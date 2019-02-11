package net.floodlightcontroller.odin.master;

import org.projectfloodlight.openflow.types.MacAddress;

public class NotificationCallbackContext {
	public final MacAddress clientHwAddress;
	public final IOdinAgent agent;
	public final long value;
	
	public NotificationCallbackContext(final MacAddress clientHwAddress, final IOdinAgent agent, final long value) {
		this.clientHwAddress = clientHwAddress;
		this.agent = agent;
		this.value = value;
	}
}
