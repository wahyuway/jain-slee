/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.slee.runtime.facilities;

import java.util.concurrent.CountDownLatch;

import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;
import javax.slee.facilities.FacilityException;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.AbstractSleeContainerModule;
import org.mobicents.slee.container.activity.ActivityContext;
import org.mobicents.slee.container.facilities.TimerFacility;
import org.mobicents.slee.container.management.jmx.TimerFacilityConfiguration;
import org.mobicents.slee.container.transaction.SleeTransactionManager;
import org.mobicents.slee.container.transaction.TransactionContext;
import org.mobicents.slee.container.transaction.TransactionalAction;
import org.mobicents.slee.util.concurrent.SleeThreadFactory;
import org.restcomm.timers.FaultTolerantScheduler;

/**
 * Implementation of the SLEE timer facility. timer is the timer object
 * currently being examined. timer.scheduleTime is time that the Timer Event is
 * scheduled to fire. timer.timeout is timeout for this timer (from
 * TimerOptions). timer.numRepetitions is the total repetitions for this timer,
 * 0 if infinite, 1 if non-periodic. timer.remainingRepetiton is the remaining
 * repetition count, initially Long.MAX_VALUE for infinite periodic timers,
 * timer.numRepetitions otherwise. timer.period is the timer period
 * (Long.MAX_VALUE if non-periodic). timer.missed is the counter of undelivered
 * late events.
 * 
 * @author Tim
 * @author M. Ranganathan
 * @author Ivelin Ivanov
 * @author martins
 * 
 */
public class TimerFacilityImpl extends AbstractSleeContainerModule implements TimerFacility {

    private final static SleeThreadFactory SLEE_THREAD_FACTORY = new SleeThreadFactory("SLEE-TimerFacility");

    private static Logger logger = Logger.getLogger(TimerFacilityImpl.class);
	
	private static final int DEFAULT_TIMEOUT = 1000;

	// this is supposed to be the timer resolution in ms of the hosting
	// OS/hardware
	private int timerResolution = 10;
			
	private FaultTolerantScheduler scheduler;
	
	private final TimerFacilityConfiguration configuration;
	
	/**
	 * 
	 */
	public TimerFacilityImpl(TimerFacilityConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public void sleeInitialization() {
	}
	
	@Override
	public void sleeStarting() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
		scheduler = new FaultTolerantScheduler("timer-facility",configuration.getTimerThreads(),sleeContainer.getClusterFactory(),(byte)10, sleeContainer.getTransactionManager().getRealTransactionManager(),new TimerFacilityTimerTaskFactory(),configuration.getPurgePeriod(), SLEE_THREAD_FACTORY);
	}
	
	/**
	 * Retrieves 
	 * @return the scheduler
	 */
	public FaultTolerantScheduler getScheduler() {
		return scheduler;
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.slee.facilities.TimerFacility#setTimer(javax.slee.ActivityContextInterface, javax.slee.Address, long, javax.slee.facilities.TimerOptions)
	 */
	public TimerID setTimer(ActivityContextInterface aci, Address address,
			long startTime, TimerOptions timerOptions)
			throws NullPointerException, IllegalArgumentException,
			FacilityException {
		
		return setTimer(aci, address, startTime, Long.MAX_VALUE, 1,
				timerOptions);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.facilities.TimerFacility#setTimer(javax.slee.ActivityContextInterface, javax.slee.Address, long, long, int, javax.slee.facilities.TimerOptions)
	 */
	public TimerID setTimer(ActivityContextInterface aci, Address address,
			long startTime, long period, int numRepetitions,
			TimerOptions timerOptions) throws NullPointerException,
			IllegalArgumentException, TransactionRolledbackLocalException,
			FacilityException {

		if (aci == null)
			throw new NullPointerException("Null ActivityContextInterface");
		if (startTime < 0)
			throw new IllegalArgumentException("startTime < 0");
		if (period <= 0)
			throw new IllegalArgumentException("period <= 0");
		if (timerOptions == null)
			throw new NullPointerException("Null TimerOptions");
		if (timerOptions.getTimeout() > period)
			throw new IllegalArgumentException("timeout > period"); 
		if (timerOptions.getTimeout() < this.getResolution())
			timerOptions.setTimeout(Math.min(period, this.getResolution()));
		
		if (period == Long.MAX_VALUE && numRepetitions == 1) {
			// non periodic value, the framework expects it to be negative instead
			period = -1;
		}
		
		// when numRepetitions == 0 the timer repeats infinitely or until
		// canceled
		if (numRepetitions < 0)
			throw new IllegalArgumentException("numRepetitions < 0");
		
		SleeTransactionManager txMgr = sleeContainer.getTransactionManager();
		boolean startedTx = txMgr.requireTransaction();

		TimerIDImpl timerID = new TimerIDImpl(sleeContainer.getUuidGenerator().createUUID());
		
		if (logger.isDebugEnabled()) {
			logger.debug("setTimer: timerID = "+timerID+" , startTime = " + startTime + " period = "
					+ period + " numRepetitions = " + numRepetitions
					+ " timeroptions =" + timerOptions);
		}

		// Attach to activity context
		org.mobicents.slee.container.activity.ActivityContextInterface aciImpl = (org.mobicents.slee.container.activity.ActivityContextInterface) aci;
		aciImpl.getActivityContext().attachTimer(timerID);
		
		// schedule timer task
		TimerFacilityTimerTaskData taskData = new TimerFacilityTimerTaskData(timerID, aciImpl.getActivityContext().getActivityContextHandle(), address, startTime, period, numRepetitions, timerOptions);
    	final TimerFacilityTimerTask task = new TimerFacilityTimerTask(taskData);
    	if(configuration.getTaskExecutionWaitsForTxCommitConfirmation()) {
    		final CountDownLatch countDownLatch = new CountDownLatch(1);
    		task.setCountDownLatch(countDownLatch);
    		TransactionalAction action = new TransactionalAction() {			
    			@Override
    			public void execute() {
    				countDownLatch.countDown();				
    			}
    		};
    		TransactionContext txContext = txMgr.getTransactionContext();
    		txContext.getAfterCommitActions().add(action);
    		txContext.getAfterRollbackActions().add(action);
    	}
		scheduler.schedule(task);				

		// If we started a tx for this operation, we commit it now
		if (startedTx) {
			try {
				txMgr.commit();
			} catch (Exception e) {
				throw new TransactionRolledbackLocalException(
						"Failed to commit transaction");
			}
		}

		return timerID;
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.slee.facilities.TimerFacility#cancelTimer(javax.slee.facilities.TimerID)
	 */
	public void cancelTimer(TimerID timerID) throws NullPointerException,
			TransactionRolledbackLocalException, FacilityException {
		
		if (logger.isDebugEnabled()) {
			logger.debug("cancelTimer: timerID = "+timerID);
		}
		
		if (timerID == null)
			throw new NullPointerException("Null TimerID");

		SleeTransactionManager txMgr = sleeContainer.getTransactionManager();

		boolean terminateTx = txMgr.requireTransaction();
		boolean doRollback = true;
		try {
			cancelTimer(timerID,true);
			doRollback = false;
		} finally {
			try {
				txMgr.requireTransactionEnd(terminateTx, doRollback);
			} catch (Throwable e) {
				throw new TransactionRolledbackLocalException(e.getMessage(),e);
			}
		}
	}
	
	public void cancelTimer(TimerID timerID, boolean detachAC) {
		// cancel task in scheduler
		final TimerFacilityTimerTask task = (TimerFacilityTimerTask) scheduler.cancel(timerID);
		if (detachAC && task != null) {
			// detach this timer from the ac
			ActivityContext ac = sleeContainer.getActivityContextFactory()
					.getActivityContext(task.getTimerFacilityTimerTaskData().getActivityContextHandle());
			if (ac != null) {					
				ac.detachTimer(timerID);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.slee.facilities.TimerFacility#getResolution()
	 */
	public long getResolution() throws FacilityException {
		return this.timerResolution;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.facilities.TimerFacility#getDefaultTimeout()
	 */
	public long getDefaultTimeout() throws FacilityException {
		return DEFAULT_TIMEOUT;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.facilities.TimerFacility#getActivityContextInterface(javax.slee.facilities.TimerID)
	 */
	public ActivityContextInterface getActivityContextInterface(TimerID timerID)
			throws NullPointerException, TransactionRequiredLocalException,
			FacilityException {
		if (timerID == null) {
			throw new NullPointerException("null timerID");
		}
		
		sleeContainer.getTransactionManager().mandateTransaction();
		
		TimerFacilityTimerTaskData taskData = (TimerFacilityTimerTaskData) scheduler.getTimerTaskData(timerID);
		if (taskData != null) {
			try {
				return sleeContainer.getActivityContextFactory().getActivityContext(taskData.getActivityContextHandle()).getActivityContextInterface();
			} catch (Exception e) {
				throw new FacilityException(e.getMessage(),e);
			}
		}
		else {
			return null;		
		}
	}
	
	@Override
	public String toString() {
		return 	"Timer Facility: " +
				"\n+-- " + scheduler.toDetailedString();
	}
}
