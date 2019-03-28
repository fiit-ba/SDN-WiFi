# SDN-WiFi
Repository for source codes used in article published in Journal

## 1. OpenFlow extensions

In this part, proposed OpenFlow extensions are described. All extensions follow the OpenFlow specification. The OpenFlow protocol links the forwarding plane with the control plane placed in the SDN controller. The OpenFlow forwarder contains one or more flow tables for packet forwarding. Flow entry puts match field, counters and set of instructions to the flow table. The instructions contain actions for packet operations. The communication between the SDN controller and the OpenFlow forwarder is performed via a control channel. In the architecture, we bring 802.11 extensions for these features:

- Packet type – provides additional information related to a received packet on a port.
- Instructions – contains information for processing of the received packet. It contains actions for discard, modification, queue, or forward the packets.
- Matches – defines fields of a packet which can be compared within flow entries. New matches were designed for 802.11 frames.
- Messages – serve to exchange information between the SDN controller and the SDN forwarder. For managing 802.11 functionalities of our architecture we propose new OpenFlow messages for control the wireless part of the network infrastructure.

### 1.1 Packet type

When a packet arrives to forwarder processing it has to be handled correctly. Therefore, it is necessary to distinguish 802.11 frames from Ethernet frames. As a result, a new packet type was defined for recognizing Ethernet frames from 802.11 frames.

The packet type fields are depicted in Figure 1. It is defined within experimenter part of the packet type which is specified by field&#39;s _namespace 0x0000_ and _ns\_type 0xFFFF_. Behind these two fields the OXM experimenter structure follows, which contains Experimenter ID for type and OXM field for subtype of a packet. The experimenter ID is set to 0x00000037 and the OXM field is set to 0x0000. Finally, additional subtypes of 802.11 frames are not necessary to define because they are recognized by 802.11 matches.

![Packet type structure](/images/Picture1.png)

### 1.2 Instruction

In order to link WTP with the SDN controller a trigger instruction was developed. This instruction generates OpenFlow messages based on received 802.11 frames. Generated OpenFlow messages include information of a client station connecting states and related context. The new instruction is depicted in Figure 2. Type of instruction is set to 0xFFFF what represents experimenter type. It is followed by fields: length, Experimenter ID, subtype and payload. The length is depending on payload size. The Experimenter ID is set to 0x00000037 and the subtype is described below with its payload parts.

![Instruction structure](/images/Picture2.png)

We have designed following subtypes of information:

- Probe information – subtype 0x0001 and contains following field:
- MAC\_STA – 6B – identifies a client station which is trying to connect to a network.
- Association information - subtype 0x0002
- MAC\_STA – 6B – identifies a client station which is associating to VAP.
- BSSID – 6B – additional identifier of VAP for the SDN controller.
- STATUS\_CODE – 1B – association result.
- Disassociation information - subtype 0x0003
- MAC\_STA – 6B – identifies a client station which is associating to VAP.
- BSSID – 6B – additional identifier of VAP for the SDN controller.

### 1.3 Matches

The OpenFlow matches are primarily proposed for Ethernet frames and they do not contain matches for the 802.11 frames. We proposed new matches for the 802.11 frames. These new matches are part of Experimenter class and are identified by the Experimenter ID. The Experimenter ID value is set to 0x00000037 like other our extensions for the 802.11 standard. The new matches are proposed based on the 802.11 frame format and they are listed in Table 2. Structure of the new matches is depicted in Figure 3.

**Table 2.** The 802.11 matches

| Matches name | Bits | Bytes | Description |
| --- | --- | --- | --- |
| OXM\_OF\_WIFI\_TYPE | 2 | - | frame type |
| OXM\_OF\_WIFI\_SUBTYPE | 4 | - | frame subtype |
| OXM\_OF\_WIFI\_TO\_DS | 1 | - | To DS flag |
| OXM\_OF\_WIFI\_FROM\_DS | 1 | - | From DS flag |
| OXM\_OF\_WIFI\_ADDR\_1 | - | 6 | Address 1 |
| OXM\_OF\_WIFI\_ADDR\_2 | - | 6 | Address 2 |
| OXM\_OF\_WIFI\_ADDR\_3 | - | 6 | Address 3 |
| OXM\_OF\_WIFI\_SSID | - | 32 | name of the wireless network - SSID |

![Matches structure](/images/Picture3.png)

### 1.4 Messages

New OpenFlow messages were developed for communication between the SDN controller, WTP and EnDeC. OpenFlow messages between the SDN controller and WTP serve for connection management. OpenFlow messages between the SDN controller and EnDeC distribute the encryption keys for accesses of the client stations into 802.11 network infrastructure.

The new OpenFlow messages (Figure 4) are defined by the field type in OpenFlow protocol (OFP) header, Experimenter ID, Experimenter type and payload. The Experimenter ID value is set to 0x00000037 and Experimenter type defines message type. The message type values ranging from 0x00000000 to 0x00000099 are reserved for SDN controller  WTP communication. Values equal to 0x00000100 and greater are reserved for SDN controller  EnDeC communication. The payload determines additional information for each subtype message. The list of all messages is depicted in Table 3 for SDN controller  WTP communication and Table 4 for SDN controller  EnDeC communication.

![Messages structure](/images/Picture4.png)

**Table 3.** Message list between WTP and SDN controller

| Message name | Experimenter type – message ID | Payload | Direction | Description |
| --- | --- | --- | --- | --- |
| Probe information | 0x00000001 | MAC\_STA = 6B | WTP  SDN controller | Information about received Probe request |
| Association information | 0x00000002 | MAC\_STA = 6B, STATUS\_CODE = 1B | WTP   SDN controller | Information about client station |
| Add VAP | 0x00000003 | BSSID = 6B,MAC\_ STA = 6B,IP\_STA = 4B,SSID\_LENGTH= 1BSSID = Variable length | SDN controller  WTP | Add VAP to the WTP. IP address can be set or not. It depends on a process when this message is used. |
| Update VAP | 0x00000004 | BSSID = 6BMAC\_ STA = 6BIP\_STA = 4B |   | Update VAP information. |
| Remove VAP | 0x00000005 | BSSID = 6BMAC\_STA = 6B | SDN controller  WTP | Remove VAP from the WTP. |
| Flush | 0x00000006 |   | SDN controller  WTP | Remove all VAPs on the WTP. |
| Statistics | 0x00000007 | Array ofMAC\_STA = 6B, RSSI = 2B | WTP  SDN controller | Statistics about client stations on WTP. |
| Get stats | 0x00000008 |   | SDN controller  WTP | Request statistics from the WTP. Reply on this message is Statistics message. |
| Disassociation | 0x00000009 | BSSID = 6B STA\_MAC = 6B | SDN controller -\&gt; WTP | Message for disconnecting a client station from the network. |
| Disassociation information | 0x00000010 | MAC\_STA = 6B | WTP  SDN controller | Information about client station disconnection |

**Table 4.** Message list between WTP and EnDeC

| Message name | Experimenter type – message ID | Payload | Direction | Description |
| --- | --- | --- | --- | --- |
| Add key | 0x00000101 | BSSID – 6BMAC\_STA 6B  MAC\_WTP 6B  PTK - CCMP 48B GTK - CCMP 32B | SDN controller  EnDeC | Add client station encryption keys to EnDeC component |
| Remove Key | 0x00000102 | MAC\_STA 6B  BSSID 6B | SDN controller  EnDeC | Remove client station encryption keys from EnDeC component |
| Update key | 0x00000103 | MAC\_STA 6B  BSSID 6BMAC\_WTP 6BPTK – CCMP 48B GTK - CCMP 32B | SDN controller  EnDeC | Update client station encryption in EnDeC component |
| Acknowledge information | 0x00000104 | MAC\_STA – 6BBSSID – 6BSTATUS\_CODE – 1B | EnDeC   SDN controller | Information about request result |
| Missing keys | 0x00000105 | MAC\_STA - 6BBSSID - 6B | EnDeC  SDN controller | Information to SDN controller about missing encryption keys in EnDeC component |
| Flush keys | 0x00000106 |   | SDN controller  EnDeC | Remove all encryption keys from the EnDeC |

OpenFlow message payload provides an additional information for the management of 802.11 network infrastructure or encryption. OpenFlow message payload usually contains information related to a client station MAC address/IP address, BSSID, SSID, encryption keys or status code. Finally, status code confirms successful or unsuccessful request execution.
