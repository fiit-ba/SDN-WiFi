package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.odin.master.OdinApplication;

import net.floodlightcontroller.odin.master.NotificationCallback;
import net.floodlightcontroller.odin.master.NotificationCallbackContext;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.odin.master.OdinEventSubscription;
import net.floodlightcontroller.odin.master.OdinMaster;
import net.floodlightcontroller.odin.master.OdinEventSubscription.Relation;
import org.projectfloodlight.openflow.types.MacAddress;

public class MobilityManager extends OdinApplication {
	protected static Logger log = LoggerFactory.getLogger(MobilityManager.class);
	/* A table including each client and its mobility statistics */
	private ConcurrentMap<MacAddress, MobilityStats> clientMap = new ConcurrentHashMap<MacAddress, MobilityStats> ();
	private final long HYSTERESIS_THRESHOLD; 		// milliseconds
	private final long IDLE_CLIENT_THRESHOLD; 		// Must to be bigger than HYSTERESIS_THRESHOLD (milliseconds)
	private final long SIGNAL_STRENGTH_THRESHOLD; 	// Signal strength threshold
	private final long SIGNAL_THRESHOLD; 			// Signal threshold (milliseconds)
	private final int SCANNING_TIME; 				// Time (milliseconds) for scanning in another agent
	private final String STA; 						// Handle a mac or all STAs ("*")
	private final String VALUE; 					// Parameter to measure (signal, noise, rate, etc.)
	private boolean scan; 							// For testing only once

	public MobilityManager () {
		this.HYSTERESIS_THRESHOLD = 15000;
		this.IDLE_CLIENT_THRESHOLD = 180000;
		this.SIGNAL_STRENGTH_THRESHOLD = 0;
		this.SIGNAL_THRESHOLD = 215;
		this.SCANNING_TIME = 1000; 
		this.STA = "*";
		this.VALUE = "signal";
		this.scan = true;
	}

	/**
	 * Register subscriptions
	 */
	private void init () {
		OdinEventSubscription oes = new OdinEventSubscription();
		/* FIXME: Add something in order to subscribe more than one STA */
		oes.setSubscription(this.STA, this.VALUE, Relation.LESSER_THAN, this.SIGNAL_THRESHOLD); 
		NotificationCallback cb = new NotificationCallback() {
			@Override
			public void exec(OdinEventSubscription oes, NotificationCallbackContext cntx) {
				if (scan == true) // For testing only once
					handler(oes, cntx);
			}
		};
		/* Before executing this line, make sure the agents declared in poolfile are started */	
		registerSubscription(oes, cb);
	}

	@Override
	public void run() {
		/* When the application runs, you need some time to start the agents */
		this.giveTime(30000);
		//this.channelAssignment();
		//this.giveTime(10000);
		//setAgentTimeout(10000);
		init (); 
	}
	
	/**
	 * This method will handoff a client in the event of its
	 * agent having failed.
	 *
	 * @param oes
	 * @param cntx
	 */
	private void handler (OdinEventSubscription oes, NotificationCallbackContext cntx) {
		OdinClient client = getClientFromHwAddress(cntx.clientHwAddress);
		long lastScanningResult = 0;
		/* The client is not registered in Odin, exit */
		if (client == null)
			return;
		long currentTimestamp = System.currentTimeMillis();
		// Assign mobility stats object if not already done
		// add an entry in the clientMap table for this client MAC
		// put the statistics in the table: value of the parameter, timestamp, timestamp, agent, scanning result
		if (!clientMap.containsKey(cntx.clientHwAddress)) {
			clientMap.put(cntx.clientHwAddress, new MobilityStats(cntx.value, currentTimestamp, currentTimestamp, cntx.agent.getIpAddress(), cntx.value));
		}
		// get the statistics of that client
		MobilityStats stats = clientMap.get(cntx.clientHwAddress);
				
		/* Now, handoff */
		
		// The client is associated to Odin (it has an LVAP), but it does not have an associated agent
		// If client hasn't been assigned an agent, associate it to the current AP
		if (client.getLvap().getAgent() == null) {
			log.info("MobilityManager: client hasn't been asigned an agent: handing off client " + cntx.clientHwAddress
					+ " to agent " + stats.agentAddr + " at " + System.currentTimeMillis());
			handoffClientToAp(cntx.clientHwAddress, stats.agentAddr);
			updateStatsWithReassignment (stats, cntx.value, currentTimestamp, stats.agentAddr, stats.scanningResult);
			return;
		}
		
		// Check for out-of-range client
		// a client has sent nothing during a certain time
		if ((currentTimestamp - stats.lastHeard) > IDLE_CLIENT_THRESHOLD) {
			log.info("MobilityManager: client with MAC address " + cntx.clientHwAddress
					+ " was idle longer than " + IDLE_CLIENT_THRESHOLD/1000 + " sec -> Reassociating it to agent " + stats.agentAddr);
			handoffClientToAp(cntx.clientHwAddress, stats.agentAddr);
			updateStatsWithReassignment (stats, cntx.value, currentTimestamp, stats.agentAddr, stats.scanningResult);
			return;
		}
		
		// If this notification is from the agent that's hosting the client's LVAP scan, update MobilityStats and handoff.
		if (client.getLvap().getAgent().getIpAddress().equals(cntx.agent.getIpAddress())) {
			// Don't bother if we're not within hysteresis period
			if (currentTimestamp - stats.assignmentTimestamp < HYSTERESIS_THRESHOLD)
				return;
			/* Scan and update statistics */
			for (InetAddress agentAddr: getAgents()) { // FIXME: scan for nearby agents only 
				if (cntx.agent.getIpAddress().equals(agentAddr)) {
					log.info("MobilityManager: Do not Scan client " + cntx.clientHwAddress + " in agent " + agentAddr + " and channel " + getChannelFromAgent(agentAddr));
					continue; // Skip same AP
				}
				else {
					log.info("MobilityManager: Scanning client " + cntx.clientHwAddress + " in agent " + agentAddr + " and channel " + getChannelFromAgent(cntx.agent.getIpAddress()));
					lastScanningResult = scanClientFromAgent(agentAddr, cntx.clientHwAddress, getChannelFromAgent(cntx.agent.getIpAddress()), this.SCANNING_TIME);
					//scan = false; // For testing only once
					if (lastScanningResult >= stats.signalStrength) {
					//if (lastScanningResult >= 50) { // testing
						updateStatsWithReassignment(stats, lastScanningResult, currentTimestamp, agentAddr, lastScanningResult);
					}
					else {
						updateStatsWithReassignment(stats, cntx.value, currentTimestamp, stats.agentAddr, stats.scanningResult);
					}
					log.info("MobilityManager: Scaned client " + cntx.clientHwAddress + " in agent " + agentAddr + " and channel " + getChannelFromAgent(cntx.agent.getIpAddress()) + " with power " + lastScanningResult);
				}
			}
			log.info("MobilityManager: signal strengths: new = " + lastScanningResult + " old = " + stats.signalStrength + " + " + SIGNAL_STRENGTH_THRESHOLD + " :" + "handing off client " + cntx.clientHwAddress
						+ " to agent " + stats.agentAddr);
			handoffClientToAp(cntx.clientHwAddress, stats.agentAddr);
			return;
		}
		
	}
	
	/**
	 * This method will update statistics
	 *
	 * @param stats
	 * @param signalValue
	 * @param now
	 * @param agentAddr
	 * @param scanningResult
	 */
	private void updateStatsWithReassignment (MobilityStats stats, long signalValue, long now, InetAddress agentAddr, long scanningResult) {
		stats.signalStrength = signalValue;
		stats.lastHeard = now;
		stats.assignmentTimestamp = now;
		stats.agentAddr = agentAddr;
		stats.scanningResult = scanningResult;
	}
	
	/**
	 * Sleep
	 *
	 * @param time
	 */
	private void giveTime (int time) {
		try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	}
		
	/**
	 * It will be a method for channel assignment
	 *
	 * FIXME: Do it in a suitable way
	 */
	private void channelAssignment () {
		for (InetAddress agentAddr: getAgents()) {
			log.info("MobilityManager: Agent IP: " + agentAddr.getHostAddress());
			if (agentAddr.getHostAddress().equals("192.168.1.9")){
				log.info ("MobilityManager: Agent channel: " + getChannelFromAgent(agentAddr));
				setChannelToAgent(agentAddr, 1);
				log.info ("MobilityManager: Agent channel: " + getChannelFromAgent(agentAddr));
			}
			if (agentAddr.getHostAddress().equals("192.168.1.10")){
				log.info ("MobilityManager: Agent channel: " + getChannelFromAgent(agentAddr));
				setChannelToAgent(agentAddr, 11);
				log.info ("MobilityManager: Agent channel: " + getChannelFromAgent(agentAddr));
			}
			
		}
	}

	private class MobilityStats {
		public long signalStrength;
		public long lastHeard;				// timestamp where it was heard the last time
		public long assignmentTimestamp;	// timestamp it was assigned
		public InetAddress agentAddr;
		public long scanningResult;

		public MobilityStats (long signalStrength, long lastHeard, long assignmentTimestamp, InetAddress agentAddr, long scanningResult) {
			this.signalStrength = signalStrength;
			this.lastHeard = lastHeard;
			this.assignmentTimestamp = assignmentTimestamp;
			this.agentAddr = agentAddr;
			this.scanningResult = scanningResult;
		}
	}
	
}
