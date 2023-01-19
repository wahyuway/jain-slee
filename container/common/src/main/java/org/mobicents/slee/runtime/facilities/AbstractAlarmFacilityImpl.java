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

package org.mobicents.slee.runtime.facilities;

import javax.slee.ComponentID;
import javax.slee.UnrecognizedComponentException;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.AlarmLevel;
import javax.slee.facilities.FacilityException;
import javax.slee.facilities.Level;

import org.mobicents.slee.container.facilities.NotificationSourceWrapper;
import org.mobicents.slee.container.management.AlarmManagement;

/**
 * Implementation of the SLEE Alarm facility. This is statefull part of
 * TraceFacility framework. Its has assigned source. All no statefull alarm are
 * delegated to MBean impl. Since those are common for ALL components. MBean is
 * responsible for maintainng life cycle of alarms.
 * 
 * @author baranowb
 * @see javax.slee.facilities.AlarmFacility
 * 
 */
@SuppressWarnings("deprecation")
public abstract class AbstractAlarmFacilityImpl implements AlarmFacility {

	private AlarmManagement mBean;

	public AbstractAlarmFacilityImpl(AlarmManagement aMBean) {

		if (aMBean == null) {
			throw new NullPointerException("Parameters must not be null.");
		}
		this.mBean = aMBean;
	}

	public abstract NotificationSourceWrapper getNotificationSource();

	public boolean clearAlarm(String alarmID) throws NullPointerException, FacilityException {
		
		if (alarmID == null) {
			throw new NullPointerException("AllarmID must not be null.");
		}
		if(!this.mBean.isAlarmAlive(alarmID))
			return false;
		if (!this.mBean.isSourceOwnerOfAlarm(getNotificationSource(), alarmID)) {
			
			throw new FacilityException("Source: " + getNotificationSource() + ", is not owner of alarm with id: " + alarmID);
		}

		try {

			return this.mBean.clearAlarm(alarmID);

		} catch (Exception e) {
			throw new FacilityException("Failed to clear alarm: " + alarmID + ", for source: " + getNotificationSource(), e);
		}

	}

	public int clearAlarms() throws FacilityException {
		try {
			return this.mBean.clearAlarms(getNotificationSource().getNotificationSource());
		} catch (Exception e) {
			throw new FacilityException("Failed to clear alarms for source: " + getNotificationSource(), e);
		}
	}

	public int clearAlarms(String alarmType) throws NullPointerException, FacilityException {
		if (alarmType == null) {
			throw new NullPointerException("AlarmType must not be null.");
		}
		try {
			return this.mBean.clearAlarms(getNotificationSource().getNotificationSource(), alarmType);
		} catch (Exception e) {
			throw new FacilityException("Failed to clear alarms for source: " + getNotificationSource(), e);
		}
	}

	public String raiseAlarm(java.lang.String alarmType, java.lang.String instanceID, AlarmLevel level, String message) throws NullPointerException, IllegalArgumentException, FacilityException {

		return this.raiseAlarm(alarmType, instanceID, level, message, null);
	}

	public String raiseAlarm(java.lang.String alarmType, java.lang.String instanceID, AlarmLevel level, String message, Throwable cause) throws NullPointerException, IllegalArgumentException,
			FacilityException {
		if (alarmType == null) {
			throw new NullPointerException("AlarmType must not be null");
		}

		if (instanceID == null) {
			throw new NullPointerException("InstanceID must not be null");
		}

		if (level == null) {
			throw new NullPointerException("AlarmLevel must not be null");
		}
		if (message == null) {
			throw new NullPointerException("Message must not be null");
		}

		if (level.isClear()) {
			throw new IllegalArgumentException("Raised alarm must not have level equal to AlarmLevel.CLEAR");
		}

		try {
			//if (this.mBean.isAlarmAlive(getNotificationSource(), alarmType, instanceID)) {
			//	return this.mBean.getAlarmId(getNotificationSource(), alarmType, instanceID);
			//} else {
				return this.mBean.raiseAlarm(getNotificationSource(), alarmType, instanceID, level, message, cause);
			//}
		} catch (Exception e) {
			throw new FacilityException("Failed to raise alarm for source: " + getNotificationSource(), e);
		}
	}

	public void createAlarm(ComponentID alarmSource, Level alarmLevel, java.lang.String alarmType, java.lang.String message, long timestamp) throws NullPointerException, IllegalArgumentException,
			UnrecognizedComponentException, FacilityException {
		this.createAlarm(alarmSource, alarmLevel, alarmType, message, null, timestamp, true);

	}

	public void createAlarm(ComponentID alarmSource, Level alarmLevel, java.lang.String alarmType, java.lang.String message, java.lang.Throwable cause, long timestamp) throws NullPointerException,
			IllegalArgumentException, UnrecognizedComponentException, FacilityException {
		this.createAlarm(alarmSource, alarmLevel, alarmType, message, null, timestamp, false);
	}

	public void createAlarm(ComponentID alarmSource, Level alarmLevel, java.lang.String alarmType, java.lang.String message, java.lang.Throwable cause, long timestamp, boolean allowCauseNull)
			throws NullPointerException, IllegalArgumentException, UnrecognizedComponentException, FacilityException {
		
		if (alarmSource == null) {
			throw new NullPointerException("AlarmSource must not be null");
		}

		if (alarmLevel == null) {
			throw new NullPointerException("AlarmLevel must not be null");
		}

		if (alarmType == null) {
			throw new NullPointerException("AlarmType must not be null");
		}

		if (message == null) {
			throw new NullPointerException("Message must not be null");
		}

		if (!allowCauseNull && cause == null) {
			throw new NullPointerException("Cause must nto be null");
		}

		if (!this.mBean.isRegisteredAlarmComponent(alarmSource)) {
			throw new UnrecognizedComponentException("Declared alarm source is not valid compoenent. Either it is nto able to create alarms or has been uninstalled");
		}
		
		try {
			this.mBean.createAlarm(alarmSource, alarmLevel, alarmType, message, cause, timestamp);
		} catch (Exception e) {

		}

	}

}
