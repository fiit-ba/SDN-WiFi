package net.floodlightcontroller.odin.master;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.odin.master.IOdinMasterToApplicationInterface;
import net.floodlightcontroller.odin.master.NotificationCallback;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.odin.master.OdinEventSubscription;
import org.projectfloodlight.openflow.types.MacAddress;


/**
 * Base class for all Odin applications. They are
 * expected to run as a thread provided by the master. * 
 * 
 * @author Lalith Suresh <suresh.lalith@gmail.com>
 *
 */
public abstract class OdinApplication implements Runnable {

	private IOdinMasterToApplicationInterface odinApplicationInterfaceToMaster;
	private String pool;
	
	
	/**
	 * Set the OdinMaster to use
	 */
	final void setOdinInterface (IOdinMasterToApplicationInterface om) {
		odinApplicationInterfaceToMaster = om;
	}
	
	
	/**
	 * Sets the pool to use for the application
	 * @param pool
	 */
	final void setPool (String pool) {
		this.pool = pool;
	}
	
	
	/**
	 * Needed to wrap OdinApplications into a thread, and is
	 * implemented by the specific application
	 */
	public abstract void run();

	
	/**
	 * VAP-Handoff a client to a new AP. This operation is idempotent.
	 * 
	 * @param newApIpAddr IPv4 address of new access point
	 * @param hwAddrSta Ethernet address of STA to be handed off
	 */
	protected final void handoffClientToAp (MacAddress staHwAddr, InetAddress newApIpAddr) {
		odinApplicationInterfaceToMaster.handoffClientToAp(pool, staHwAddr, newApIpAddr);
	}

	
	/**
	 * Get the list of clients currently registered with Odin
	 * 
	 * @return a map of OdinClient objects keyed by HW Addresses
	 */
	protected final Set<OdinClient> getClients () {
		return odinApplicationInterfaceToMaster.getClients(pool);		
	}
	
	
	/**
	 * Get the OdinClient type from the client's MacAddress
	 * 
	 * @return a OdinClient instance corresponding to clientHwAddress
	 */
	protected final OdinClient getClientFromHwAddress (MacAddress clientHwAddress) {
		return odinApplicationInterfaceToMaster.getClientFromHwAddress(pool, clientHwAddress);
	}
	
	  /** Retreive LastHeard from the agent
    *
    * @param agentAddr InetAddress of the agent
    *
    * @return timestamp of the last ping heard from the agent
    */
    protected final long getLastHeardFromAgent (InetAddress agentAddr) {
      return odinApplicationInterfaceToMaster.getLastHeardFromAgent(pool, agentAddr);
  }
					
	/**
	 * Retreive TxStats from the agent
	 * 
	 * @param agentAddr InetAddress of the agent
	 * 
	 * @return Key-Value entries of each recorded statistic for each client 
	 */
	protected final Map<MacAddress, Map<String, String>> getTxStatsFromAgent (InetAddress agentAddr) {
		return odinApplicationInterfaceToMaster.getTxStatsFromAgent(pool, agentAddr);
	}
	
	/**
	 * Retreive RxStats from the agent
	 * 
	 * @param agentAddr InetAddress of the agent
	 * 
	 * @return Key-Value entries of each recorded statistic for each client 
	 */
	protected final Map<MacAddress, Map<String, String>> getRxStatsFromAgent (InetAddress agentAddr) {
		return odinApplicationInterfaceToMaster.getRxStatsFromAgent(pool, agentAddr);
	}
	
	/**
	 * Get a list of Odin agents from the agent tracker
	 * @return a map of OdinAgent objects keyed by Ipv4 addresses
	 */
	protected final Set<InetAddress> getAgents () {
		return odinApplicationInterfaceToMaster.getAgentAddrs(pool);
	}
	
	
	/**
	 * Add a subscription for a particular event defined by oes. cb is
	 * defines the application specified callback to be invoked during
	 * notification. If the application plans to delete the subscription,
	 * later, the onus is upon it to keep track of the subscription
	 * id for removal later.
	 * 
	 * @param oes the susbcription
	 * @param cb the callback
	 */
	protected final long registerSubscription (OdinEventSubscription oes, NotificationCallback cb){
		return odinApplicationInterfaceToMaster.registerSubscription(pool, oes, cb);
	}
	
	
	/**
	 * Remove a subscription from the list
	 * 
	 * @param id subscription id to remove
	 * @return
	 */
	protected final void unregisterSubscription (long id) {
		odinApplicationInterfaceToMaster.unregisterSubscription(pool, id);
	}
	
	
	/**
	 * Add an SSID to the Odin network.
	 * 
	 * @param networkName
	 * @return true if the network could be added, false otherwise
	 */
	protected final boolean addNetwork (String ssid) {
		return odinApplicationInterfaceToMaster.addNetwork(pool, ssid);
	}
	
	
	/**
	 * Remove an SSID from the Odin network.
	 * 
	 * @param networkName
	 * @return true if the network could be removed, false otherwise
	 */
	protected final boolean removeNetwork (String ssid) {
		return odinApplicationInterfaceToMaster.removeNetwork(pool, ssid);
	}
	
	/**
	 * Change the Wi-Fi channel of an specific agent (AP)
	 * 
	 * @param Agent InetAddress
	 * @param Channel
	 */
	protected final void setChannelToAgent (InetAddress agentAddr, int channel){
		odinApplicationInterfaceToMaster.setChannelToAgent(pool, agentAddr, channel);
	}
	
	
	/**
	 * Get channel from and specific agent (AP)
	 * 
	 * @param Agent InetAddress
	 * @return Channel number
	 */
	protected final int getChannelFromAgent (InetAddress agentAddr){
		return odinApplicationInterfaceToMaster.getChannelFromAgent(pool, agentAddr); 
	}
	
	
	/**
	 * Channel Switch Announcement, to the clients of an specific agent (AP)
	 * 
	 * @param Agent InetAddress
	 * @param Client MAC
	 * @param SSID
	 * @param Channel
	 */
	/*protected final void sendChannelSwitchToClient (InetAddress agentAddr, MacAddress clientHwAddr, String ssid, int channel){
		odinApplicationInterfaceToMaster.sendChannelSwitchToClient(pool, agentAddr, clientHwAddr, ssid, channel);
	}*/
	
	/**
	 * Scanning for a client in a specific agent (AP)
	 * 
	 * @param Agent InetAddress
	 * @param Client MAC
	 * @param Channel
	 * @param Scanning time
	 * @return Signal power
	 */
	protected final int scanClientFromAgent (InetAddress agentAddr, MacAddress clientHwAddr, int channel, int time){
		return odinApplicationInterfaceToMaster.scanClientFromAgent(pool, agentAddr, clientHwAddr, channel, time);
	}

}
