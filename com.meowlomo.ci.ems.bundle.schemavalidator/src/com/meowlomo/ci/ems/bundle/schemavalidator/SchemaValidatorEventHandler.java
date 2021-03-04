package com.meowlomo.ci.ems.bundle.schemavalidator;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class SchemaValidatorEventHandler implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		String topic = event.getTopic();
		if ("com/meowlomo/bundle/schemavalidator/dotest" == topic){
			//TODO
		}
	}

}
