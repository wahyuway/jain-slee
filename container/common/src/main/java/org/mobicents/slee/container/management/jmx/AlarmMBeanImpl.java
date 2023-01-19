/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/***************************************************
 *                                                 *
 *  Restcomm: The Open Source JSLEE Platform      *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************
 *
 * Created on Nov 18, 2004
 *
 * AlarmMBeanImpl.java
 * 
 */
package org.mobicents.slee.container.management.jmx;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;
import javax.slee.ComponentID;
import javax.slee.SbbID;
import javax.slee.UnrecognizedComponentException;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.AlarmLevel;
import javax.slee.facilities.Level;
import javax.slee.management.Alarm;
import javax.slee.management.AlarmNotification;
import javax.slee.management.ManagementException;
import javax.slee.management.NotificationSource;
import javax.slee.management.UnrecognizedNotificationSourceException;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.facilities.NotificationSourceWrapper;
import org.mobicents.slee.container.management.AlarmManagement;
import org.mobicents.slee.container.transaction.TransactionalAction;
import org.mobicents.slee.runtime.facilities.DefaultAlarmFacilityImpl;

/**
 * Implementation of the Alarm MBean: The implementation of the JMX interface to
 * the SLEE alarm facility
 * 
 * @author baranowb
 * @author Tim
 * 
 */
@SuppressWarnings("deprecation")
public class AlarmMBeanImpl extends MobicentsServiceMBeanSupport implements AlarmManagement,AlarmMBeanImplMBean {

	private static Logger log = Logger.getLogger(AlarmMBeanImpl.class);

	private Map<AlarmPlaceHolder, NotificationSource> placeHolderToNotificationSource = new ConcurrentHashMap<AlarmPlaceHolder, NotificationSource>();
	private Map<String, AlarmPlaceHolder> alarmIdToAlarm = new ConcurrentHashMap<String, AlarmPlaceHolder>();

	private final TraceMBeanImpl traceMBean;
		
	/**
	 * @param traceMBean
	 */
	public AlarmMBeanImpl(TraceMBeanImpl traceMBean) {
		super();
		this.traceMBean = traceMBean;
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.slee.container.management.AlarmManagement#newAlarmFacility(javax.slee.management.NotificationSource)
	 */
	public AlarmFacility newAlarmFacility(NotificationSource notificationSource) {
		return new DefaultAlarmFacilityImpl(notificationSource, this);	
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.slee.container.SleeContainerModule#setSleeContainer(org.mobicents.slee.container.SleeContainer)
	 */
	public void setSleeContainer(SleeContainer sleeContainer) {
		this.sleeContainer = sleeContainer; 
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#clearAlarm(java.lang.String)
	 */
	public boolean clearAlarm(String alarmID) throws NullPointerException, ManagementException {
		if (alarmID == null) {
			throw new NullPointerException("AlarmID must not be null");
		}

		AlarmPlaceHolder aph = alarmIdToAlarm.remove(alarmID);
		placeHolderToNotificationSource.remove(aph);
		if (aph == null) {
			return false;
		} else {
			// we clear?
			try {
				generateNotification(aph, true);
			} catch (Exception e) {
				throw new ManagementException("Failed to clear alarm due to: " + e);
			}
			return true;
		}

	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#clearAlarms(javax.slee.management.NotificationSource)
	 */
	public int clearAlarms(NotificationSource notificationSource) throws NullPointerException, UnrecognizedNotificationSourceException, ManagementException {
		if (notificationSource == null) {
			throw new NullPointerException("NotificationSource must not be null");
		}

		mandateSource(notificationSource);

		int count = 0;
		try {

			for (Map.Entry<AlarmPlaceHolder, NotificationSource> e : placeHolderToNotificationSource.entrySet()) {
				if (e.getValue().equals(notificationSource)) {
					if (clearAlarm(e.getKey().getAlarm().getAlarmID())) {
						count++;
					}
				}
			}

		} catch (Exception e) {
			throw new ManagementException("Failed to get alarm id list due to: ", e);
		}

		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#clearAlarms(javax.slee.management.NotificationSource, java.lang.String)
	 */
	public int clearAlarms(NotificationSource notificationSource, String alarmType) throws NullPointerException, UnrecognizedNotificationSourceException, ManagementException {
		if (notificationSource == null) {
			throw new NullPointerException("NotificationSource must not be null");
		}

		if (alarmType == null) {
			throw new NullPointerException("AlarmType must not be null");
		}

		mandateSource(notificationSource);

		int count = 0;
		try {

			for (Map.Entry<AlarmPlaceHolder, NotificationSource> e : placeHolderToNotificationSource.entrySet()) {
				if (e.getValue().equals(notificationSource) && e.getKey().getAlarmType().equals(alarmType)) {
					if (clearAlarm(e.getKey().getAlarm().getAlarmID())) {
						count++;
					}
				}
			}

		} catch (Exception e) {
			throw new ManagementException("Failed to get alarm id list due to: ", e);
		}

		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#getAlarms()
	 */
	public String[] getAlarms() throws ManagementException {
		try {
			Set<String> ids = new HashSet<String>();
			ids.addAll(alarmIdToAlarm.keySet());
			return ids.toArray(new String[ids.size()]);
		} catch (Exception e) {
			throw new ManagementException("Failed to get list of active alarms due to.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#getAlarms(javax.slee.management.NotificationSource)
	 */
	public String[] getAlarms(NotificationSource notificationSource) throws NullPointerException, UnrecognizedNotificationSourceException, ManagementException {
		if (notificationSource == null) {
			throw new NullPointerException("NotificationSource must not be null");
		}

		mandateSource(notificationSource);

		try {
			Set<String> ids = new HashSet<String>();
			for (Map.Entry<AlarmPlaceHolder, NotificationSource> e : placeHolderToNotificationSource.entrySet()) {
				if (e.getValue().equals(notificationSource)) {
					ids.add(e.getKey().getAlarm().getAlarmID());
				}
			}
			return ids.toArray(new String[ids.size()]);
		} catch (Exception e) {
			throw new ManagementException("Failed to get alarm id list due to: ", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#getDescriptor(java.lang.String)
	 */
	public Alarm getDescriptor(String alarmID) throws NullPointerException, ManagementException {
		if (alarmID == null) {
			throw new NullPointerException("AlarmID must not be null");
		}
		AlarmPlaceHolder aph = this.alarmIdToAlarm.get(alarmID);
		if (aph == null)
			return null;
		return aph.getAlarm();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#getDescriptors(java.lang.String[])
	 */
	public Alarm[] getDescriptors(String[] alarmIDs) throws NullPointerException, ManagementException {
		if (alarmIDs == null) {
			throw new NullPointerException("AlarmID[] must not be null");
		}

		Set<Alarm> alarms = new HashSet<Alarm>();

		try {
			for (String id : alarmIDs) {
				Alarm a = getDescriptor(id);
				if (a != null)
					alarms.add(a);
			}
			return alarms.toArray(new Alarm[alarms.size()]);

		} catch (Exception e) {
			throw new ManagementException("Failed to get desciptors.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.slee.management.AlarmMBean#isActive(java.lang.String)
	 */
	public boolean isActive(String alarmID) throws NullPointerException, ManagementException {
		return this.alarmIdToAlarm.containsKey(alarmID);
	}

	// NON MBEAN - used only internal, those methods are not exposed via jmx

	public boolean isSourceOwnerOfAlarm(NotificationSourceWrapper notificationSource, String alarmID) {
		AlarmPlaceHolder aph = this.alarmIdToAlarm.get(alarmID);
		if (aph == null)
			return false;

		return aph.getNotificationSource().getNotificationSource().equals(notificationSource.getNotificationSource());
	}

	public boolean isAlarmAlive(String alarmID) {
		return this.alarmIdToAlarm.containsKey(alarmID);
	}

	public boolean isAlarmAlive(NotificationSourceWrapper notificationSource, String alarmType, String instanceID) {

		AlarmPlaceHolder aph = new AlarmPlaceHolder(notificationSource, alarmType, instanceID);
		// return this.placeHolderToAlarm.containsKey(aph);

		return this.alarmIdToAlarm.containsValue(aph);
	}

	public String getAlarmId(NotificationSourceWrapper notificationSource, String alarmType, String instanceID) {
		AlarmPlaceHolder localAPH = new AlarmPlaceHolder(notificationSource, alarmType, instanceID);
		Alarm a = null;
		for (Map.Entry<String, AlarmPlaceHolder> e : this.alarmIdToAlarm.entrySet()) {
			if (e.getValue().equals(localAPH)) {
				a = e.getValue().getAlarm();
				break;
			}

		}
		if (a != null)
			return a.getAlarmID();
		else
			return null;
	}

	/**
	 * THis methods raises alarm. It MUST not receive AlarmLevel.CLEAR, it has
	 * to be filtered.
	 * 
	 * @param notificationSource
	 * @param alarmType
	 * @param instanceID
	 * @param level
	 * @param message
	 * @param cause
	 * @return - AlarmId
	 */
	public String raiseAlarm(NotificationSourceWrapper notificationSource, String alarmType, String instanceID, AlarmLevel level, String message, Throwable cause) {

		synchronized (notificationSource) {
			if (isAlarmAlive(notificationSource, alarmType, instanceID)) {
				// Alarm a = this.placeHolderToAlarm.get(new
				// AlarmPlaceHolder(notificationSource, alarmType, instanceID));

				Alarm a = null;
				// unconveniant....
				try {
					AlarmPlaceHolder localAPH = new AlarmPlaceHolder(notificationSource, alarmType, instanceID);
					for (Map.Entry<String, AlarmPlaceHolder> e : this.alarmIdToAlarm.entrySet()) {
						if (e.getValue().equals(localAPH)) {
							a = e.getValue().getAlarm();
							break;
						}
					}
				} catch (Exception e) {
					// ignore
				}

				if (a != null) {

					return a.getAlarmID();
				} else {
					return this.raiseAlarm(notificationSource, alarmType, instanceID, level, message, cause);
				}
			} else {
				Alarm a = new Alarm(UUID.randomUUID().toString(), notificationSource.getNotificationSource(), alarmType, instanceID, level, message, cause, System.currentTimeMillis());
				AlarmPlaceHolder aph = new AlarmPlaceHolder(notificationSource, alarmType, instanceID, a);
				this.alarmIdToAlarm.put(a.getAlarmID(), aph);
				// this.placeHolderToAlarm.put(aph, a);
				this.placeHolderToNotificationSource.put(aph, aph.getNotificationSource().getNotificationSource());
				generateNotification(aph, false);
				return a.getAlarmID();
			}
		}

	}

	private void generateNotification(AlarmPlaceHolder aph, boolean isCleared) {
		Alarm alarm = aph.getAlarm();
		AlarmLevel generalLevel = isCleared ? AlarmLevel.CLEAR : alarm.getAlarmLevel();

		AlarmNotification notification = new AlarmNotification(aph.getNotificationSource().getNotificationSource().getAlarmNotificationType(), this, alarm.getAlarmID(), aph.getNotificationSource()
				.getNotificationSource(), alarm.getAlarmType(), alarm.getInstanceID(), generalLevel, alarm.getMessage(), alarm.getCause(), aph.getNotificationSource().getNextSequence(), System
				.currentTimeMillis());
		super.sendNotification(notification);
	}

	/**
	 * This method is requried - in case component is removed on call to method with its noti source we must throw unknown notification source exception - even thought alarms MAY be present?
	 * @throws UnrecognizedNotificationSourceException 
	 */
	private void mandateSource(NotificationSource src) throws UnrecognizedNotificationSourceException
	{
		if(!traceMBean.isNotificationSourceDefined(src))
		{
			throw new UnrecognizedNotificationSourceException("Notification source is not present: "+src);
		}
	}
	
	
	class AlarmPlaceHolder {
		private NotificationSourceWrapper notificationSource;
		private String alarmType;
		private String instanceID;
		private Alarm alarm;

		public AlarmPlaceHolder(NotificationSourceWrapper notificationSource, String alarmType, String instanceID) {
			super();
			this.notificationSource = notificationSource;
			this.alarmType = alarmType;
			this.instanceID = instanceID;
		}

		public AlarmPlaceHolder(NotificationSourceWrapper notificationSource, String alarmType, String instanceID, Alarm a) {
			this.notificationSource = notificationSource;
			this.alarmType = alarmType;
			this.instanceID = instanceID;
			this.alarm = a;
		}

		public NotificationSourceWrapper getNotificationSource() {
			return notificationSource;
		}

		public String getAlarmType() {
			return alarmType;
		}

		public String getInstanceID() {
			return instanceID;
		}

		public Alarm getAlarm() {
			return alarm;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((alarmType == null) ? 0 : alarmType.hashCode());
			result = prime * result + ((instanceID == null) ? 0 : instanceID.hashCode());
			result = prime * result + ((notificationSource == null) ? 0 : notificationSource.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AlarmPlaceHolder other = (AlarmPlaceHolder) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (alarmType == null) {
				if (other.alarmType != null)
					return false;
			} else if (!alarmType.equals(other.alarmType))
				return false;
			if (instanceID == null) {
				if (other.instanceID != null)
					return false;
			} else if (!instanceID.equals(other.instanceID))
				return false;
			if (notificationSource == null) {
				if (other.notificationSource != null)
					return false;
			} else if (!notificationSource.equals(other.notificationSource))
				return false;
			return true;
		}

		private AlarmMBeanImpl getOuterType() {
			return AlarmMBeanImpl.this;
		}

	}

	// 1.0 part, required for compatibility.
	/**
	 * Represents a component registered with the alarm facility. Basically just
	 * stores notification sequence number
	 * 
	 * @author Tim
	 */
	static class RegisteredComp {
		public AtomicLong seqNo = new AtomicLong(0);

		public long getSeqNo() {
			return seqNo.getAndIncrement();
		}
	}

	private Map<ComponentID, RegisteredComp> registeredComps = new ConcurrentHashMap<ComponentID, RegisteredComp>();

	// 1.0 methods

	public boolean isRegisteredAlarmComponent(ComponentID alarmSource) {
		// FIXME: in 1.0 we allow only SbbID as componetns
		return this.registeredComps.containsKey(alarmSource);
	}

	public void createAlarm(ComponentID alarmSource, Level alarmLevel, String alarmType, String message, Throwable cause, long timestamp) throws UnrecognizedComponentException {
		if (log.isDebugEnabled()) {
			log.debug("alarmSource:" + alarmSource + " alarmLevel:" + alarmLevel + " alarmType:" + alarmType + " message:" + message + " cause:" + cause + " timeStamp:" + timestamp);
		}
		if (alarmSource == null || alarmLevel == null || alarmType == null || message == null)
			throw new NullPointerException("Null parameter");
		if (alarmLevel.isOff())
			throw new IllegalArgumentException("Invalid alarm level");

		RegisteredComp comp = registeredComps.get(alarmSource);
		if (comp == null)
			throw new UnrecognizedComponentException("Component not registered");

		// TODO I'm not sure if we should log the alarm too

		// Add the notication type if not already in set. See note in
		// declaration about why we are using a map, not a set
		// if (!this.notificationTypes.containsKey(alarmType))
		// this.notificationTypes.put(alarmType, alarmType);

		// Create the alarm notification and propagate to the Alarm MBean

		AlarmNotification notification = new AlarmNotification(this, alarmType, alarmSource, alarmLevel, message, cause, comp.getSeqNo(), timestamp);
		super.sendNotification(notification);

	}

	public void registerComponent(final SbbID sbbID) {
		if (log.isDebugEnabled()) {
			log.debug("Registering component with alarm facility: " + sbbID);
		}

		registeredComps.put(sbbID, new RegisteredComp());

		TransactionalAction action = new TransactionalAction() {
			public void execute() {
				registeredComps.remove(sbbID);
			}
		};
		sleeContainer.getTransactionManager().getTransactionContext().getAfterRollbackActions().add(action);

	}

	public void unRegisterComponent(final SbbID sbbID) {
		final RegisteredComp registeredComp = this.registeredComps.remove(sbbID);
		if (registeredComp != null) {
			TransactionalAction action = new TransactionalAction() {
				public void execute() {
					registeredComps.put(sbbID, registeredComp);
				}
			};
			sleeContainer.getTransactionManager().getTransactionContext().getAfterRollbackActions().add(action);
		}

	}

	/* (non-Javadoc)
	 * @see org.mobicents.slee.container.management.AlarmManagement#getAlarmMBeanObjectName()
	 */
	public ObjectName getAlarmMBeanObjectName() {
		return getObjectName();
	}
	
	// NOTIFICATION PART
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	public MBeanNotificationInfo[] getNotificationInfo() {

		return null;
	}

	@Override
	public void sleeInitialization() {
	}
	
	@Override
	public void sleeStarting() {
		
	}
	
	@Override
	public void sleeRunning() {
		
	}
	
	@Override
	public void sleeStopping() {
		
	}
	
	@Override
	public void sleeStopped() {
		
	}
	
	@Override
	public void sleeShutdown() {
		
	}
	
}
