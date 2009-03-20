/**
 * Start time:16:00:31 2009-01-25<br>
 * Project: mobicents-jainslee-server-core<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.slee.container.component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.slee.ActivityContextInterface;
import javax.slee.ComponentID;
import javax.slee.EventTypeID;
import javax.slee.SbbID;
import javax.slee.management.ComponentDescriptor;
import javax.slee.management.DependencyException;
import javax.slee.management.DeploymentException;
import javax.slee.management.LibraryID;
import javax.slee.management.SbbDescriptor;
import javax.slee.profile.ProfileSpecificationID;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.slee.container.component.deployment.jaxb.descriptors.SbbDescriptorImpl;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.common.references.MLibraryRef;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.common.references.MProfileSpecRef;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.common.references.MSbbRef;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.sbb.MEventEntry;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.sbb.MResourceAdaptorEntityBinding;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.sbb.MResourceAdaptorTypeBinding;
import org.mobicents.slee.container.component.validator.SbbComponentValidator;

/**
 * Start time:16:00:31 2009-01-25<br>
 * Project: mobicents-jainslee-server-core<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class SbbComponent extends SleeComponentWithUsageParametersInterface {

	/**
	 * the sbb descriptor
	 */
	private final SbbDescriptorImpl descriptor;

	/**
	 * the sbb abstract class
	 */
	private Class abstractSbbClass;

	/**
	 * the concrete sbb class, generated by SLEE
	 */
	private Class concreteSbbClass;

	/**
	 * the sbb local interface
	 */
	private Class sbbLocalInterfaceClass;

	/**
	 * the concrete sbb local interface class, generated by SLEE
	 */
	private Class sbbLocalInterfaceConcreteClass;

	/**
	 * the sbb own activity context interface
	 */
	private Class activityContextInterface;

	/**
	 * the concrete sbb own activity context interface class, generated by SLEE
	 */
	private Class activityContextInterfaceConcreteClass;

	/**
	 * the JAIN SLEE specs descriptor
	 */
	private SbbDescriptor specsDescriptor = null;

	/**
	 * the event handler methods for this sbb component
	 */
	private Map<EventTypeID, EventHandlerMethod> eventHandlerMethods = null;

	/**
	 * 
	 * @param descriptor
	 */
	public SbbComponent(SbbDescriptorImpl descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * Retrieves the sbb descriptor
	 * 
	 * @return
	 */
	public SbbDescriptorImpl getDescriptor() {
		return descriptor;
	}

	/**
	 * Retrieves the sbb id
	 * 
	 * @return
	 */
	public SbbID getSbbID() {
		return descriptor.getSbbID();
	}

	/**
	 * Retrieves the sbb abstract class
	 * 
	 * @return
	 */
	public Class getAbstractSbbClass() {
		return abstractSbbClass;
	}

	/**
	 * Retrieves the concrete sbb class, generated by SLEE
	 * 
	 * @return
	 */
	public Class getConcreteSbbClass() {
		return concreteSbbClass;
	}

	/**
	 * This must never return null, if no custom interface is defined, this has
	 * to return generic javax.slee.SbbLocalObject FIXME emmartins: this should
	 * return null, since in runtime it will avoid some instanceof for sure
	 * 
	 * @return
	 */
	public Class getSbbLocalInterfaceClass() {
		return sbbLocalInterfaceClass;
	}

	/**
	 * Retrieves the concrete sbb local interface class, generated by SLEE
	 * 
	 * @return
	 */
	public Class getSbbLocalInterfaceConcreteClass() {
		return sbbLocalInterfaceConcreteClass;
	}

	/**
	 * Retrieves the sbb own activity context interface
	 * 
	 * @return
	 */
	public Class getActivityContextInterface() {
		return activityContextInterface;
	}

	/**
	 * Retrieves the concrete sbb own activity context interface class,
	 * generated by SLEE
	 * 
	 * @return
	 */
	public Class getActivityContextInterfaceConcreteClass() {
		return activityContextInterfaceConcreteClass;
	}

	/**
	 * Sets the sbb abstract class
	 * 
	 * @param abstractSbbClass
	 */
	public void setAbstractSbbClass(Class abstractSbbClass) {
		this.abstractSbbClass = abstractSbbClass;
	}

	/**
	 * Sets the concrete sbb class, generated by SLEE
	 * 
	 * @param concreteSbbClass
	 */
	public void setConcreteSbbClass(Class concreteSbbClass) {
		this.concreteSbbClass = concreteSbbClass;
		// build the map of event handler methods
		eventHandlerMethods = new HashMap<EventTypeID, EventHandlerMethod>();
		for (MEventEntry eventEntry : getDescriptor().getEventEntries()
				.values()) {
			if (eventEntry.isReceived()) {
				String eventHandlerMethodName = "on"
						+ eventEntry.getEventName();
				for (Method method : concreteSbbClass.getMethods()) {
					if (method.getName().equals(eventHandlerMethodName)) {
						EventHandlerMethod eventHandlerMethod = new EventHandlerMethod(
								method);
						if (method.getParameterTypes().length == 3) {
							eventHandlerMethod.setHasEventContextParam(true);
						}
						if (method.getParameterTypes()[1]
								.equals(ActivityContextInterface.class)) {
							eventHandlerMethod.setHasCustomACIParam(false);
						}
						eventHandlerMethods.put(eventEntry.getEventReference()
								.getComponentID(), eventHandlerMethod);
						break;
					}
				}
			}
		}
	}

	/**
	 * Sets the sbb local interface
	 * 
	 * @param sbbLocalInterfaceClass
	 */
	public void setSbbLocalInterfaceClass(Class sbbLocalInterfaceClass) {
		this.sbbLocalInterfaceClass = sbbLocalInterfaceClass;
	}

	/**
	 * Sets the concrete sbb local interface class, generated by SLEE
	 * 
	 * @param sbbLocalInterfaceConcreteClass
	 */
	public void setSbbLocalInterfaceConcreteClass(
			Class sbbLocalInterfaceConcreteClass) {
		this.sbbLocalInterfaceConcreteClass = sbbLocalInterfaceConcreteClass;
	}

	/**
	 * Sets the sbb own activity context interface
	 * 
	 * @param activityContextInterface
	 */
	public void setActivityContextInterface(Class activityContextInterface) {
		this.activityContextInterface = activityContextInterface;
	}

	/**
	 * Sets the concrete sbb own activity context interface class, generated by
	 * SLEE
	 * 
	 * @param activityContextInterfaceConcreteClass
	 */
	public void setActivityContextInterfaceConcreteClass(
			Class activityContextInterfaceConcreteClass) {
		this.activityContextInterfaceConcreteClass = activityContextInterfaceConcreteClass;
	}

	@Override
	public boolean isSlee11() {
		return this.descriptor.isSlee11();
	}

	@Override
	void addToDeployableUnit() {
		getDeployableUnit().getSbbComponents().put(getSbbID(), this);
	}

	@Override
	public Set<ComponentID> getDependenciesSet() {
		return descriptor.getDependenciesSet();
	}

	@Override
	public ComponentID getComponentID() {
		return getSbbID();
	}

	@Override
	public boolean validate() throws DependencyException, DeploymentException {
		SbbComponentValidator validator = new SbbComponentValidator();
		validator.setComponent(this);
		validator.setComponentRepository(getDeployableUnit()
				.getDeployableUnitRepository());
		return validator.validate();
	}

	/**
	 * Retrieves the JAIN SLEE specs descriptor
	 * 
	 * @return
	 */
	public SbbDescriptor getSpecsDescriptor() {
		if (specsDescriptor == null) {
			Set<LibraryID> libraryIDSet = new HashSet<LibraryID>();
			for (MLibraryRef mLibraryRef : getDescriptor().getLibraryRefs()) {
				libraryIDSet.add(mLibraryRef.getComponentID());
			}
			LibraryID[] libraryIDs = libraryIDSet
					.toArray(new LibraryID[libraryIDSet.size()]);

			Set<SbbID> sbbIDSet = new HashSet<SbbID>();
			for (MSbbRef mSbbRef : getDescriptor().getSbbRefs()) {
				sbbIDSet.add(mSbbRef.getComponentID());
			}
			SbbID[] sbbIDs = sbbIDSet.toArray(new SbbID[sbbIDSet.size()]);

			Set<ProfileSpecificationID> profileSpecSet = new HashSet<ProfileSpecificationID>();
			for (MProfileSpecRef mProfileSpecRef : getDescriptor()
					.getProfileSpecRefs()) {
				profileSpecSet.add(mProfileSpecRef.getComponentID());
			}
			ProfileSpecificationID[] profileSpecs = profileSpecSet
					.toArray(new ProfileSpecificationID[profileSpecSet.size()]);

			Set<EventTypeID> eventTypeSet = new HashSet<EventTypeID>();
			for (MEventEntry mEventEntry : getDescriptor().getEventEntries()
					.values()) {
				eventTypeSet.add(mEventEntry.getEventReference()
						.getComponentID());
			}
			EventTypeID[] eventTypes = eventTypeSet
					.toArray(new EventTypeID[eventTypeSet.size()]);

			Set<ResourceAdaptorTypeID> raTypeIDSet = new HashSet<ResourceAdaptorTypeID>();
			Set<String> raLinksSet = new HashSet<String>();
			for (MResourceAdaptorTypeBinding mResourceAdaptorTypeBinding : getDescriptor()
					.getResourceAdaptorTypeBindings()) {
				raTypeIDSet.add(mResourceAdaptorTypeBinding
						.getResourceAdaptorTypeRef());
				for (MResourceAdaptorEntityBinding mResourceAdaptorEntityBinding : mResourceAdaptorTypeBinding
						.getResourceAdaptorEntityBinding()) {
					raLinksSet.add(mResourceAdaptorEntityBinding
							.getResourceAdaptorEntityLink());
				}
			}
			ResourceAdaptorTypeID[] raTypeIDs = raTypeIDSet
					.toArray(new ResourceAdaptorTypeID[raTypeIDSet.size()]);
			String[] raLinks = raLinksSet
					.toArray(new String[raLinksSet.size()]);

			specsDescriptor = new SbbDescriptor(getSbbID(), getDeployableUnit()
					.getDeployableUnitID(), getDeploymentUnitSource(),
					libraryIDs, sbbIDs, eventTypes, profileSpecs,
					getDescriptor().getAddressProfileSpecRef(), raTypeIDs,
					raLinks);
		}
		return specsDescriptor;
	}

	@Override
	public ComponentDescriptor getComponentDescriptor() {
		return getSpecsDescriptor();
	}

	/**
	 * Retrieves the evetn handler methods for this sbb component, mapped by
	 * event type id
	 * 
	 * @return
	 */
	public Map<EventTypeID, EventHandlerMethod> getEventHandlerMethods() {
		return eventHandlerMethods;
	}

	/**
	 * Sbb event handler method wrapper to deliver an event to the sbb
	 * component.
	 * 
	 * @author martins
	 * 
	 */
	public class EventHandlerMethod {

		private final Method eventHandlerMethod;
		private boolean hasCustomACIParam;
		private boolean hasEventContextParam;

		public EventHandlerMethod(Method eventHandlerMethod) {
			this.eventHandlerMethod = eventHandlerMethod;
		}

		public Method getEventHandlerMethod() {
			return eventHandlerMethod;
		}

		public boolean getHasCustomACIParam() {
			return hasCustomACIParam;
		}

		public void setHasCustomACIParam(boolean hasCustomACIParam) {
			this.hasCustomACIParam = hasCustomACIParam;
		}

		public boolean getHasEventContextParam() {
			return hasEventContextParam;
		}

		public void setHasEventContextParam(boolean hasEventContextParam) {
			this.hasEventContextParam = hasEventContextParam;
		}
	}
}
