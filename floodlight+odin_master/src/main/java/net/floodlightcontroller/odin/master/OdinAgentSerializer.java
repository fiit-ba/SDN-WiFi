package net.floodlightcontroller.odin.master;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class OdinAgentSerializer extends JsonSerializer<IOdinAgent> {

	@Override
	public void serialize(IOdinAgent agent, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {
		jgen.writeStartObject();
		String blah = agent.getIpAddress().getHostAddress();
		jgen.writeStringField("ipAddress", blah);
		jgen.writeStringField("lastHeard", String.valueOf(agent.getLastHeard()));
		jgen.writeEndObject();
	}
}
