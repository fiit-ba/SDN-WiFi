package net.floodlightcontroller.odin.master;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class OdinClientSerializer extends JsonSerializer<OdinClient> {

	
	@Override
	public void serialize(OdinClient client, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {
		jgen.writeStartObject();
		jgen.writeStringField("macAddress", client.getMacAddress().toString());
		String clientIpAddr = client.getIpAddress().getHostAddress();
		jgen.writeStringField("ipAddress", clientIpAddr);
		jgen.writeStringField("lvapBssid", client.getLvap().getBssid().toString());
		jgen.writeStringField("lvapSsid", client.getLvap().getSsids().get(0)); // FIXME: assumes single SSID
		IOdinAgent agent = client.getLvap().getAgent();
		if (agent != null) {
			String agentIpAddr = agent.getIpAddress().getHostAddress();
			jgen.writeStringField("agent", agentIpAddr);
		}
		else {
			jgen.writeStringField("agent", null);
		}
			
		
		jgen.writeEndObject();
	}

}
