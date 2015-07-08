/*
   Copyright 2015 IBM Corporation.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ibm.sr.idgen;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.serviceregistry.ServiceRegistryException;
import com.ibm.serviceregistry.ServiceRegistryInvalidPropertyException;
import com.ibm.serviceregistry.ServiceRegistryModifier;
import com.ibm.serviceregistry.ServiceRegistryStatus;
import com.ibm.serviceregistry.delegate.DelegateFactory;
import com.ibm.serviceregistry.delegate.RepositoryDelegate;
import com.ibm.serviceregistry.sdo.BaseObject;
import com.ibm.serviceregistry.sdo.GenericObject;
import com.ibm.serviceregistry.sdo.OriginalObject;
import com.ibm.serviceregistry.sdo.helper.BSRSDOHelper;

/**
 * WebSphere Service Registry and Repository Modifier plugin which
 * generates a context identifier for an SLA and generates
 * a consumer identifier for a service or application version. 
 * The generated IDs are UUIDs.
 *
 */
public class ContextConsumerIDGen implements ServiceRegistryModifier {

    // Logger
    private static final Logger logger = Logger.getLogger("com.ibm.sr.idgen");

    // Class name constant for logging
    private static final String CLASS_NAME = ContextConsumerIDGen.class.getName();

    // model URI base
	private static final String GEP_BASE = "http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceEnablementModel#";
	private static final String XGEP_BASE = "http://www.ibm.com/xmlns/prod/serviceregistry/profile/v6r3/GovernanceProfileExtensions#";
	
	// model properties
	private static final String PROP_CONSUMER = "gep63_consumerIdentifier";
	private static final String PROP_CONTEXT = "gep63_contextIdentifier";

	/**
	 * Create method is called when any object is created. It checks
	 * that the object is a business model, and is either a version
	 * or an SLA. If either, it generates the UUID and sets the
	 * appropriate property, then saves the object.
	 */
	@Override
	public ServiceRegistryStatus create(OriginalObject arg0) {
        final String METHOD_NAME = "create";

        // log entry to the method
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASS_NAME, METHOD_NAME, arg0);
        }

		ServiceRegistryStatus status = new ServiceRegistryStatus();

		// check type of thing created using the passed-in object
		if(arg0 instanceof GenericObject) {
			boolean isVersion = false;
			boolean isSLA = false;
			String primaryType = ((GenericObject)arg0).getPrimaryType();
			// check model type
			if(primaryType != null) {
				if(primaryType.equals(GEP_BASE + "ServiceVersion") || primaryType.equals(GEP_BASE + "ApplicationVersion")) {
					isVersion = true;
				} else if(primaryType.equals(GEP_BASE + "ServiceLevelAgreement") || primaryType.equals(XGEP_BASE + "ServiceLevelAgreement")) {
					isSLA = true;
				}
			}
			
			String id = null;
			BaseObject bo = null;
			RepositoryDelegate sr = null;
			if(isVersion || isSLA) {
				// generate ID
				UUID uuid = UUID.randomUUID();
				id = uuid.toString();
				
				// Retrieve object to depth 1 for making changes.
				// It is very important to always re-retrieve an object if changes are being made.
				// Changing the passed-in object can lead to trouble.
				try {
					sr = DelegateFactory.createRepositoryDelegate();
					bo = sr.retrieve(arg0.getBsrURI(), 1);
				} catch(ServiceRegistryException ex){
					logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "exception retrieving object ", ex);
					status.addException(ex);
					status.setReturnCode(ServiceRegistryStatus.ERROR);
				}
			}

			if(isVersion) {
				// add to version as consumer ID
				try {
					// if property exists, set value, else add it
					if(BSRSDOHelper.INSTANCE.isPropertySet(bo, PROP_CONSUMER)) {
						BSRSDOHelper.INSTANCE.setPropertyValue(bo, PROP_CONSUMER, id);
					} else {
						BSRSDOHelper.INSTANCE.addProperty(bo, PROP_CONSUMER, id);
					}
				}catch(ServiceRegistryInvalidPropertyException ex) {
					logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "exception updating property " + PROP_CONSUMER, ex);
					status.addException(ex);
					status.setReturnCode(ServiceRegistryStatus.ERROR);
				}
			}
			
			if(isSLA) {
				// add to SLA as context ID
				try {
					if(BSRSDOHelper.INSTANCE.isPropertySet(bo, PROP_CONTEXT)) {
						BSRSDOHelper.INSTANCE.setPropertyValue(bo, PROP_CONTEXT, id);
					} else {
						BSRSDOHelper.INSTANCE.addProperty(bo, PROP_CONTEXT, id);
					}
				}catch(ServiceRegistryInvalidPropertyException ex) {
					logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "exception updating property " + PROP_CONTEXT, ex);
					status.addException(ex);
					status.setReturnCode(ServiceRegistryStatus.ERROR);
				}
			}
			
			if(isVersion || isSLA) {
				// update the re-retrieved object
				try {
					sr.update(bo);
				} catch(ServiceRegistryException ex){
					logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, "exception saving object ", ex);
					status.addException(ex);
					status.setReturnCode(ServiceRegistryStatus.ERROR);
				}
			}
		}

		// log exit
        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(CLASS_NAME, METHOD_NAME, status);
        }

		return status;
	}

	@Override
	public ServiceRegistryStatus delete(OriginalObject arg0) {
		return new ServiceRegistryStatus();
	}

	@Override
	public ServiceRegistryStatus update(BaseObject arg0, BaseObject arg1) {
		return new ServiceRegistryStatus();
	}

}
