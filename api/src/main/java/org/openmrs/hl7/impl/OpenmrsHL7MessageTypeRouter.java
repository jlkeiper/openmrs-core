/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.hl7.impl;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.Application;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

/**
 * This is a HL7 message type router which discriminates the messages based on the
 * type,event,sendingApplication and MessageControlId.Inspired by 
 * HAPI MessageTypeRouter. 
 */
public class OpenmrsHL7MessageTypeRouter implements Application {
	
	private HashMap<String, Application> apps;
	
	private Log log = LogFactory.getLog(OpenmrsHL7MessageTypeRouter.class);
	
	/**
	 * constructor to initialize the HashMap
	 */
	public OpenmrsHL7MessageTypeRouter() {
		apps = new HashMap<String, Application>(20);
	}
	
	/**
	 * @see ca.uhn.hl7v2.app.Application#canProcess(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public boolean canProcess(Message in) {
		boolean can = false;
		try {
			Application matches = this.getMatchingApplication(in);
			if (matches != null)
				can = true;
		}
		catch (HL7Exception e) {
			can = false;
		}
		return can;
	}
	
	/**
	 * @see ca.uhn.hl7v2.app.Application#processMessage(ca.uhn.hl7v2.model.Message)
	 * @should use the best match on the most specific rule to route a message to a single handler
	 */
	@Override
	public Message processMessage(Message in) throws ApplicationException, HL7Exception {
		Message out;
		try {
			Application matchingApp = this.getMatchingApplication(in);
			out = matchingApp.processMessage(in);
		}
		catch (HL7Exception e) {
			throw new ApplicationException("Error internally routing message: " + e.toString(), e);
		}
		return out;
	}
	
	/**
	 * Registers the given application to handle messages corresponding to the given type,
	 * trigger event and matching patter to sending application and message control id. Only 
	 * one application can be registered for a given message type trigger event
	 * combination and a matching pattern for sendingApplication and messageControlId.Otherwise it
	 * will throw an exception
	 * @should not throw exception if a handler is registered to a rule which is already registered
	 */
	public synchronized void registerApplication(String messageType, String triggerEvent, String sendingApplication,
	                                             String messageControlId, Application handler) {
		String key = getKey(messageType, triggerEvent, sendingApplication, messageControlId);
		if (apps.containsKey(key))
			log.warn("HL7 key " + key + " now handled by " + handler.getClass().getName());
		this.apps.put(key, handler);
	}
	
	/**
	 * Returns the Applications that has been registered to handle messages of the type, trigger
	 * event and matching pattern for sendingApplication and messageControlId of the given message.
	 */
	private Application getMatchingApplication(Message message) throws HL7Exception {
		Terser t = new Terser(message);
		String messageType = t.get("/MSH-9-1");
		String triggerEvent = t.get("/MSH-9-2");
		String sendingApplication = t.get("/MSH-3");
		String messageControlId = t.get("/MSH-10");
		return getMatchingApplication(messageType, triggerEvent, sendingApplication, messageControlId);
	}
	
	/**
	 * Helper method for getMatchingApplication(message).that is actually doing matching.
	 */
	private synchronized Application getMatchingApplication(String messageType, String triggerEvent,
	                                                        String sendingApplication, String messageControlId) {
		Application matchingApp = null;
		Object o = null;
		String key[] = null;
		String longestMatchKey = null;
		Integer counter = 0;
		Integer maxCount = 0;
		
		for (String keys : this.apps.keySet()) {
			key = keys.split("_");
			if (messageType.matches(key[0])) {
				counter++;
				if (triggerEvent.matches(key[1])) {
					counter++;
					if (sendingApplication.matches(key[2])) {
						counter++;
						if (messageControlId.matches(key[3])) {
							counter++;
						}
					}
				}
			}
			if (counter > maxCount) {
				maxCount = counter;
				longestMatchKey = keys;
			}
			if (counter == 4)
				break;
			counter = 0;
		}
		if (maxCount != 0)
			o = this.apps.get(longestMatchKey);
		if (o != null)
			matchingApp = (Application) o;
		else
			throw new APIException("Could not find the handler for " + messageType + triggerEvent + sendingApplication
			        + messageControlId);
		return matchingApp;
	}
	
	/**
	 * Creates reproducible hash key.
	 */
	private String getKey(String messageType, String triggerEvent, String sendingApplication, String messageControlId) {
		//create hash key string by concatenating type and trigger event
		return messageType + "_" + triggerEvent + "_" + sendingApplication + "_" + messageControlId;
	}
	
}
