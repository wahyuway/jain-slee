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

package org.mobicents.slee.resource;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import javax.slee.EventTypeID;
import javax.slee.InvalidArgumentException;
import javax.slee.InvalidStateException;
import javax.slee.SLEEException;
import javax.slee.ServiceID;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.facilities.AlarmFacility;
import javax.slee.management.NotificationSource;
import javax.slee.management.ResourceAdaptorEntityNotification;
import javax.slee.management.ResourceAdaptorEntityState;
import javax.slee.management.SleeState;
import javax.slee.resource.ActivityFlags;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorID;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.CacheType;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.activity.ActivityContextHandle;
import org.mobicents.slee.container.activity.ActivityType;
import org.mobicents.slee.container.component.ra.ResourceAdaptorComponent;
import org.mobicents.slee.container.component.ratype.ResourceAdaptorTypeComponent;
import org.mobicents.slee.container.management.ResourceManagementImpl;
import org.mobicents.slee.container.management.jmx.ResourceUsageMBean;
import org.mobicents.slee.container.resource.ResourceAdaptorActivityContextHandle;
import org.mobicents.slee.container.resource.ResourceAdaptorEntity;
import org.mobicents.slee.container.resource.ResourceAdaptorObjectState;
import org.mobicents.slee.resource.cluster.FaultTolerantResourceAdaptor;
import org.mobicents.slee.resource.cluster.FaultTolerantResourceAdaptorContextImpl;

/**
 * 
 * Implementation of the logical Resource Adaptor Entity and its life cycle.
 * 
 * @author Eduardo Martins
 */
public class ResourceAdaptorEntityImpl implements ResourceAdaptorEntity {

	private static final Logger logger = Logger
			.getLogger(ResourceAdaptorEntity.class);

	/**
	 * the ra entity name
	 */
	private final String name;

	/**
	 * the ra component related to this entity
	 */
	private final ResourceAdaptorComponent component;

	/**
	 * the ra entity state
	 */
	private ResourceAdaptorEntityState state;

	/**
	 * the ra object
	 */
	private final ResourceAdaptorObjectImpl object;

	/**
	 * the slee container
	 */
	private final SleeContainer sleeContainer;
	
	/**
	 * 
	 */
	private final ResourceManagementImpl resourceManagement;
	
	/**
	 * Notification source of this RA Entity
	 */
	private ResourceAdaptorEntityNotification notificationSource;

	/**
	 * Alarm facility serving this RA entity(notification source)
	 */
	private AlarmFacility alarmFacility;

	/**
	 * the resource usage mbean for this ra, may be null
	 */
	private final ResourceUsageMBean usageMbean;

	/**
	 * the ra context for this entity
	 */
	private final ResourceAdaptorContextImpl resourceAdaptorContext;

	/**
	 * the ra allowed event types, cached here for optimal runtime performance
	 */
	private final Set<EventTypeID> allowedEventTypes;
	
	private boolean setFTContext = true;
	
	@SuppressWarnings("rawtypes")
	private FaultTolerantResourceAdaptorContextImpl ftResourceAdaptorContext;
	
	/**
	 * Creates a new entity with the specified name, for the specified ra
	 * component and with the provided entity config properties. The entity
	 * creation is complete after instantianting the ra object, and then setting
	 * its ra context and configuration.
	 * 
	 * @param name
	 * @param component
	 * @param entityProperties
	 * @param resourceManagement
	 * @throws InvalidConfigurationException
	 * @throws InvalidArgumentException
	 */
	public ResourceAdaptorEntityImpl(String name,
			ResourceAdaptorComponent component,
			ConfigProperties entityProperties, ResourceManagementImpl resourceManagement,
			ResourceAdaptorEntityNotification notificationSource, ResourceUsageMBean usageMbean)
			throws InvalidConfigurationException, InvalidArgumentException {
		this.name = name;
		this.component = component;
		this.resourceManagement = resourceManagement;
		this.sleeContainer = resourceManagement.getSleeContainer();
		this.notificationSource = notificationSource;
		this.usageMbean = usageMbean;
		this.alarmFacility = sleeContainer.getAlarmManagement().newAlarmFacility(notificationSource);
		// create ra object
		ClassLoader currentClassLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					component.getClassLoader());
			ResourceAdaptor ra = (ResourceAdaptor) this.component.getResourceAdaptorClass().newInstance();
			object = new ResourceAdaptorObjectImpl(this,ra, component
					.getDefaultConfigPropertiesInstance());
		} catch (Exception e) {
			throw new SLEEException(
					"unable to create instance of ra object for " + component,e);
		} finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
		// create ra context
		resourceAdaptorContext = new ResourceAdaptorContextImpl(this,
				sleeContainer);
		// cache runtime data
		if (!component.getDescriptor().getIgnoreRaTypeEventTypeCheck()) {
			// the slee endpoint will filter events fired, build a set with the
			// event types allowed
			allowedEventTypes = new HashSet<EventTypeID>();
			for (ResourceAdaptorTypeID raTypeID : resourceAdaptorContext
					.getResourceAdaptorTypes()) {
				ResourceAdaptorTypeComponent raTypeComponent = sleeContainer
						.getComponentRepository()
						.getComponentByID(raTypeID);
				for (EventTypeID eventTypeRef : raTypeComponent
						.getDescriptor().getEventTypeRefs()) {
					allowedEventTypes.add(eventTypeRef);
				}
			}
		} else {
			allowedEventTypes = null;
		}
		// set ra context
		try {
			object.setResourceAdaptorContext(resourceAdaptorContext);
		} catch (InvalidStateException e) {
			logger
					.error(
							"should not happen, setting ra context on ra entity creation",
							e);
			throw new SLEEException(e.getMessage(), e);
		}		
		// configure
		object.raConfigure(entityProperties);
		// process to inactive state
		this.state = ResourceAdaptorEntityState.INACTIVE;
	}

	/**
	 * @return the sleeContainer
	 */
	public SleeContainer getSleeContainer() {
		return sleeContainer;
	}
	
	/**
	 * Retrieves ra component related to this entity
	 * 
	 * @return
	 */
	public ResourceAdaptorComponent getComponent() {
		return component;
	}

	/**
	 * Retrieves the ra entity name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the ra entity state
	 * 
	 * @return
	 */
	public ResourceAdaptorEntityState getState() {
		return state;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == this.getClass()) {
			return ((ResourceAdaptorEntityImpl) obj).name.equals(this.name);
		} else {
			return false;
		}
	}

	// --------- ra entity/object logic

	/**
	 * Updates the ra entity config properties
	 */
	public void updateConfigurationProperties(ConfigProperties properties)
			throws InvalidConfigurationException, InvalidStateException {
		if (!component.getDescriptor().getSupportsActiveReconfiguration()
				&& (sleeContainer.getSleeState() != SleeState.STOPPED)
				&& (state == ResourceAdaptorEntityState.ACTIVE || state == ResourceAdaptorEntityState.STOPPING)) {
			throw new InvalidStateException(
					"the value of the supports-active-reconfiguration attribute of the resource-adaptor-class element in the deployment descriptor of the Resource Adaptor of the resource adaptor entity is False and the resource adaptor entity is in the Active or Stopping state and the SLEE is in the Starting, Running, or Stopping state");
		} else {
			object.raConfigurationUpdate(properties);
		}
	}

	/**
	 * Signals that the container is in RUNNING state
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void sleeRunning() throws InvalidStateException {		
		// if entity is active then activate the ra object
		if (this.state.isActive()) {
			if (setFTContext) {
				setFTContext = false;
				if (object.isFaultTolerant()) {
					// set fault tolerant context, it is a ft ra
					try {
						this.ftResourceAdaptorContext = new FaultTolerantResourceAdaptorContextImpl(name,sleeContainer,(FaultTolerantResourceAdaptor) object.getResourceAdaptorObject());
						object.setFaultTolerantResourceAdaptorContext(ftResourceAdaptorContext);						
					}
					catch (Throwable t) {
						logger.error("Got exception invoking setFaultTolerantResourceAdaptorContext(...) for entity "+name, t);
					}					
				}
			}
			try {
				object.raActive();
			}
			catch (Throwable t) {
				logger.error("Got exception invoking raActive() for entity "+name, t);
			}
		}
	}

	/**
	 * Signals that the container is in STOPPING state
	 * @throws TransactionRequiredLocalException 
	 */
	public void sleeStopping() throws InvalidStateException, TransactionRequiredLocalException {
		if (state != null && state.isActive()) {
			try {
				object.raStopping();
			}
			catch (Throwable t) {
				logger.error("Got exception from RA object",t);
			}
			scheduleAllActivitiesEnd();
		}
	}

	public void allActivitiesEnded() {
		
		logger.info("All activities ended for ra entity "+name);
		
		if (timerTask != null) {
			timerTask = null;
		}
		if (!this.state.isInactive()) {
			if (object.getState() == ResourceAdaptorObjectState.STOPPING) {
				try {
					object.raInactive();
				}
				catch (Throwable t) {
					logger.error("Got exception invoking raInactive() for entity "+name, t);
				}
			}
			if (state != null && state.isStopping()) {
				state = ResourceAdaptorEntityState.INACTIVE;
			}
		}
	}

	/**
	 * Activates the ra entity
	 * 
	 * @throws InvalidStateException
	 *             if the entity is not in INACTIVE state
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void activate() throws InvalidStateException {
		if (!this.state.isInactive()) {
			throw new InvalidStateException("entity " + name + " is in state: "
					+ this.state);
		}
		this.state = ResourceAdaptorEntityState.ACTIVE;
		// if slee is running then activate ra object
		if (sleeContainer.getSleeState() == SleeState.RUNNING) {
			if (setFTContext) {
				setFTContext = false;
				if (object.isFaultTolerant()) {
					// set fault tolerant context, it is a ft ra
					try {						
						this.ftResourceAdaptorContext = new FaultTolerantResourceAdaptorContextImpl(name,sleeContainer,(FaultTolerantResourceAdaptor) object.getResourceAdaptorObject());
						object.setFaultTolerantResourceAdaptorContext(ftResourceAdaptorContext);
					}
					catch (Throwable t) {
						logger.error("Got exception invoking setFaultTolerantResourceAdaptorContext(...) for entity "+name, t);
					}					
				}
			}
			try {
				object.raActive();
			}
			catch (Throwable t) {
				logger.error("Got exception invoking raActive() for entity "+name, t);
			}
		}
	}

	/**
	 * Deactivates the ra entity
	 * 
	 * @throws InvalidStateException
	 *             if the entity is not in ACTIVE state
	 * @throws TransactionRequiredLocalException 
	 */
	public void deactivate() throws InvalidStateException, TransactionRequiredLocalException {
		if (!this.state.isActive()) {
			throw new InvalidStateException("entity " + name + " is in state: "
					+ this.state);
		}
		this.state = ResourceAdaptorEntityState.STOPPING;
		if (object.getState() == ResourceAdaptorObjectState.ACTIVE) {
			object.raStopping();
		}
		// tck requires that the method returns with stopping state so do
		// all deactivation logic half a sec later
		TimerTask t = new TimerTask() {
			@Override
			public void run() {
				try {
					cancel();
					if (state == ResourceAdaptorEntityState.STOPPING) {
						if (object.getState() == ResourceAdaptorObjectState.STOPPING) {	
							scheduleAllActivitiesEnd();
						}
						else {
							allActivitiesEnded();
						}	
					}
				}
				catch (Throwable e) {
					logger.error(e.getMessage(),e);
				}
			}
		};
		resourceAdaptorContext.getTimer().schedule(t,500);
	}	
	
	/**
	 * schedules the ending of all the entity activities, this is needed on ra
	 * entity deactivation or slee container stop, once the process ends it will
	 * invoke allActivitiesEnded to complete those processes
	 * @throws TransactionRequiredLocalException 
	 */
	private void scheduleAllActivitiesEnd() throws TransactionRequiredLocalException {

		// schedule the end of all activities if the node is the single member of the cluster
		boolean skipActivityEnding = !sleeContainer.getCluster(CacheType.ACTIVITIES).isSingleMember();
		
		if (!skipActivityEnding && hasActivities()) {
			logger.info("RA entity "+name+" activities end scheduled.");
			timerTask = new EndAllActivitiesRAEntityTimerTask(this,sleeContainer);
		}
		else {
			allActivitiesEnded();
		}
	}

	/**
	 * Checks if the entity has activities besides the one passed as parameter (if not null).
	 * @param exceptHandle
	 * @return
	 */
	private boolean hasActivities() {
		try {	
			for (ActivityContextHandle handle : sleeContainer
					.getActivityContextFactory()
					.getAllActivityContextsHandles()) {
				if (handle.getActivityType() == ActivityType.RA) {
					ResourceAdaptorActivityContextHandle raHandle = (ResourceAdaptorActivityContextHandle) handle;
					if (raHandle.getResourceAdaptorEntity().equals(this)) {

						logger.debug("**** AllActivityContextsHandles: "+sleeContainer
								.getActivityContextFactory()
								.getAllActivityContextsHandles());

						if (logger.isDebugEnabled()) {
							logger.debug("RA entity "+name+" has (at least) activity "+handle.getActivityHandle());
						}

						return true;
					}
				}
			}			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		} 
		
		return false;
	}

	private EndAllActivitiesRAEntityTimerTask timerTask;
	
	/**
	 * Removes the entity, it will unconfigure and unset the ra context, the
	 * entity object can not be reused
	 * 
	 * @throws InvalidStateException
	 */
	public void remove() throws InvalidStateException {
		if (!this.state.isInactive()) {
			throw new InvalidStateException("entity " + name + " is in state: "
						+ this.state);
		}
		object.raUnconfigure();
		if (object.isFaultTolerant()) {
			object.unsetFaultTolerantResourceAdaptorContext();
			ftResourceAdaptorContext.shutdown();
		}
		object.unsetResourceAdaptorContext();
		this.sleeContainer.getTraceManagement()
				.deregisterNotificationSource(this.getNotificationSource());
		state = null;
	}

	/**
	 * Retrieves the active config properties for the entity
	 * 
	 * @return
	 */
	public ConfigProperties getConfigurationProperties() {
		return object.getConfigProperties();
	}

	/**
	 * Retrieves the id of the resource adaptor for this entity
	 * 
	 * @return
	 */
	public ResourceAdaptorID getResourceAdaptorID() {
		return component.getResourceAdaptorID();
	}

	/**
	 * Retrieves the ra object
	 * 
	 * @return
	 */
	public ResourceAdaptorObjectImpl getResourceAdaptorObject() {
		return object;
	}

	/**
	 * Retrieves the ra interface for this entity and the specified ra type
	 * 
	 * @param raType
	 * @return
	 */
	public Object getResourceAdaptorInterface(ResourceAdaptorTypeID raType) {
		return object.getResourceAdaptorInterface(sleeContainer
				.getComponentRepository().getComponentByID(raType)
				.getDescriptor().getResourceAdaptorInterface());
	}

	/**
	 * Retrieves the marshaller from the ra object, if exists
	 * 
	 * @return
	 */
	public Marshaler getMarshaler() {
		return object.getMarshaler();
	}

	/**
	 * Indicates a service was activated, the entity will forward this
	 * notification to the ra object.
	 * 
	 * @param serviceInfo
	 */
	public void serviceActive(ServiceID serviceID) {
		try {
			ReceivableService receivableService = resourceAdaptorContext
					.getServiceLookupFacility().getReceivableService(serviceID);
			if (receivableService.getReceivableEvents().length > 0) {
				object.serviceActive(receivableService);
			}
		} catch (Throwable e) {
			logger.warn("invocation resulted in unchecked exception", e);
		}
	}

	/**
	 * Indicates a service is stopping, the entity will forward this
	 * notification to the ra object.
	 * 
	 * @param serviceInfo
	 */
	public void serviceStopping(ServiceID serviceID) {
		try {
			ReceivableService receivableService = resourceAdaptorContext
					.getServiceLookupFacility().getReceivableService(serviceID);
			if (receivableService.getReceivableEvents().length > 0) {
				object.serviceStopping(receivableService);
			}
		} catch (Throwable e) {
			logger.warn("invocation resulted in unchecked exception", e);
		}
	}

	/**
	 * Indicates a service was deactivated, the entity will forward this
	 * notification to the ra object.
	 * 
	 * @param serviceInfo
	 */
	public void serviceInactive(ServiceID serviceID) {
		try {
			ReceivableService receivableService = resourceAdaptorContext
					.getServiceLookupFacility().getReceivableService(serviceID);
			if (receivableService.getReceivableEvents().length > 0) {
				object.serviceInactive(receivableService);
			}
		} catch (Throwable e) {
			logger.warn("invocation resulted in unchecked exception", e);
		}
	}

	/**
	 * Return Notification source representing this RA Entity
	 * 
	 * @return
	 */
	public NotificationSource getNotificationSource() {
		return this.notificationSource;
	}

	public AlarmFacility getAlarmFacility() {
		return alarmFacility;
	}

	/**
	 * Retrieves the resource usage mbean for this ra, may be null
	 * 
	 * @return
	 */
	public ResourceUsageMBean getResourceUsageMBean() {
		return usageMbean;
	}

	/**
	 * Retrieves a set containing event types allowed to be fire by this entity
	 * 
	 * @return null if the ra ignores event type checking
	 */
	public Set<EventTypeID> getAllowedEventTypes() {
		return allowedEventTypes;
	}

	public FireableEventType getFireableEventType(EventTypeID eventTypeID) {
		FireableEventType eventType = null;
		try {
			eventType = resourceAdaptorContext.getEventLookupFacility()
					.getFireableEventType(eventTypeID);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		return eventType;
	}

	public ReceivableService getReceivableService(ServiceID serviceID) {
		ReceivableService receivableService = null;
		if (serviceID != null) {
			try {
				receivableService = resourceAdaptorContext
						.getServiceLookupFacility().getReceivableService(serviceID);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
		return receivableService;
	}

	/**
	 * if it is a handle reference it gets the referred handle
	 * @param handle
	 * @return
	 */
	ActivityHandle derreferActivityHandle(ActivityHandle handle) {
		ActivityHandle ah = null;
		if (resourceManagement.getHandleReferenceFactory() != null && handle.getClass() == ActivityHandleReference.class) {
			ActivityHandleReference ahReference = (ActivityHandleReference) handle;
			ah = resourceManagement.getHandleReferenceFactory().getActivityHandle(ahReference);
		}
		else {
			ah = handle;
		}
		return ah;
	}

	/**
	 * Callback to notify the entity and possibly the ra object, informing activity handled ended.
	 * @param handle
	 * @param activityFlags
	 */
	public void activityEnded(final ActivityHandle handle, int activityFlags) {
		logger.trace("activityEnded( handle = " + handle + " )");
		ActivityHandle ah = null;
		if (handle instanceof ActivityHandleReference) {
			// handle is a ref, derrefer and remove the ref
			ah = resourceManagement.getHandleReferenceFactory().removeActivityHandleReference((ActivityHandleReference) handle);			
		}
		else {
			// handle is not a reference
			ah = handle;
		}
		if (ah != null && ActivityFlags.hasRequestEndedCallback(activityFlags)) {
			object.activityEnded(ah);
		}
		if (object.getState() == ResourceAdaptorObjectState.STOPPING) {
			synchronized (this) {
				// the ra object is stopping, check if the timer task is still
				// needed
				if (!hasActivities()) {
					if (timerTask != null) {
						timerTask.cancel();
					}
					allActivitiesEnded();				
				}
			}			
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mobicents.slee.resource.ResourceAdaptorEntity#getActivityContextHandle(javax.slee.resource.ActivityHandle)
	 */
	public ActivityContextHandle getActivityContextHandle(
			ActivityHandle activityHandle) {
		return new ResourceAdaptorActivityContextHandleImpl(this, activityHandle);
	}
	
	/**
	 * 
	 * @return
	 */
	public ActivityHandleReferenceFactory getHandleReferenceFactory() {
		return resourceManagement.getHandleReferenceFactory();
	}
}
