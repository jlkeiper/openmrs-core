package org.openmrs.hl7.impl;

import java.util.Properties;

import org.junit.Test;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Service;
import org.openmrs.hl7.handler.ORUR01Handler;
import org.openmrs.module.ModuleConstants;
import org.openmrs.module.ModuleUtil;
import org.openmrs.test.BaseContextSensitiveTest;

import ca.uhn.hl7v2.app.Application;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.model.Message;

public class OpenmrsHL7MessageTypeRouterTest extends BaseContextSensitiveTest {
	
	/**
	 * @see OpenmrsHL7MessageTypeRouter#processMessage(Message)
	 * @verifies use the best match on the most specific rule to route a message to a single handler
	 */
	@Test(expected = ApplicationException.class)
	public void processMessage_shouldUseTheBestMatchOnTheMostSpecificRuleToRouteAMessageToASingleHandler() throws Exception {
		executeDataSet("org/openmrs/hl7/include/ORUTest-initialData.xml");
		OpenmrsHL7MessageTypeRouter router = new OpenmrsHL7MessageTypeRouter();
		Properties props = super.getRuntimeProperties();
		
		props.setProperty(ModuleConstants.RUNTIMEPROPERTY_MODULE_LIST_TO_LOAD,
		    "org/openmrs/hl7/include/examplehl7handlers-0.1.omod");
		// the above module provides a handler for messages of type "ADR" with trigger "A19"
		
		ModuleUtil.startup(props);
		
		// the application context cannot restart here to load in the moduleApplicationContext that
		// calls the setHL7Handlers method so we're doing it manually here
		Class<Application> c = (Class<Application>) Context
		        .loadClass("org.openmrs.module.examplehl7handlers.AlternateORUR01Handler");
		Application classInstance = c.newInstance();
		router.registerApplication("ORU", "R01", "FORMENTRY", null, classInstance);
		router.registerApplication("ORU", "R01", null, null, new ORUR01Handler());
		HL7Service hl7service = Context.getHL7Service();
		Message message = hl7service
		        .parseHL7String("MSH|^~\\&|FORMENTRY|AMRS.ELD|HL7LISTENER|AMRS.ELD|20080226102656||ORU^R01|JqnfhKKtouEz8kzTk6Zo|P|2.5|1||||||||16^AMRS.ELD.FORMID\r"
		                + "PID|||3^^^^||John3^Doe^||\r"
		                + "NK1|1|Hornblower^Horatio^L|2B^Sibling^99REL||||||||||||M|19410501|||||||||||||||||1234^^^Test Identifier Type^PT||||\r"
		                + "PV1||O|1^Unknown Location||||1^Super User (1-8)|||||||||||||||||||||||||||||||||||||20080212|||||||V\r"
		                + "ORC|RE||||||||20080226102537|1^Super User\r"
		                + "OBR|1|||1238^MEDICAL RECORD OBSERVATIONS^99DCT\r"
		                + "OBX|1|NM|5497^CD4, BY FACS^99DCT||450|||||||||20080206\r"
		                + "OBX|2|DT|5096^RETURN VISIT DATE^99DCT||20080229|||||||||20080212");
		
		router.processMessage(message);
		
		ModuleUtil.shutdown();
	}
	
	/**
	 * @see OpenmrsHL7MessageTypeRouter#registerApplication(String,String,String,String,Application)
	 * @verifies not throw exception if a handler is registered to a rule which is already registered
	 */
	@Test
	public void registerApplication_shouldNotThrowExceptionIfAHandlerIsRegisteredToARuleWhichIsAlreadyRegistered()
	                                                                                                              throws Exception {
		OpenmrsHL7MessageTypeRouter router = new OpenmrsHL7MessageTypeRouter();
		router.registerApplication("ORU", "R01", null, null, new ORUR01Handler());
		router.registerApplication("ORU", "R01", null, null, new ORUR01Handler());
	}
}
