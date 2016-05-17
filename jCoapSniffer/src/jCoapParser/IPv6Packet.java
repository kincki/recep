/**
 * 
 */
package jCoapParser;

import net.sourceforge.jpcap.net.RawPacket;
import java.nio.ByteBuffer;
import java.net.InetAddress;

/**
 * @author root
 *
 */
public class IPv6Packet {
	
	private final short RAW_FRAME_HEADER_LENGTH = 16; /* this much data will be discarded */
	private final short IP_CLASS_LENGTH = 4;
	private final short IP6_ADDR_LENGTH = 16;
	
	private byte[] rawData; //original raw packet arrived
	private byte[] macData; //mac layer data, not used for now
	private byte[] ipClassData;
	private short payloadLength; // length of the IPv6 Payload
	private byte nextHdr; // what is the protocol in payload
	private byte hopLimit; // what is the hop limit for this packet
	private byte [] sourceIP; // 16byte long source IPv6 address
	private byte [] destIP; // 16byte long source IPv6 address
	private long timeOfArrival; // time of arrival for this packet
	
	UDPacket payload; //the whole UDP packet
	
	/*
	 * Parse incoming RawPacket into this IPv6 packet format
	 * 
	 */
	public IPv6Packet(RawPacket packet) {
		this.rawData = packet.getData();		
	}
	
	public void parseRawData() {
		
		//This wrapper is used over and over again to retrieve packet details
		ByteBuffer wrapper = ByteBuffer.wrap(rawData); 
		
		this.timeOfArrival = System.currentTimeMillis();
		
		try {
			//strip-off mac data
			this.macData = new byte[RAW_FRAME_HEADER_LENGTH];
			wrapper.get(this.macData);
			
			//strip-off ipclass data
			this.ipClassData = new byte[IP_CLASS_LENGTH];
			wrapper.get(this.ipClassData);
			
			//payload_length is represented with two (2) bytes
			this.payloadLength = wrapper.getShort(); 
			
			//next header is represented with one (1) byte
			//add 2 to the beginning offset to account for 2 bytes of payload field 
			this.nextHdr = wrapper.get();
			
			//hop limit is represented with one (1) byte 
			this.hopLimit = wrapper.get();
			
			//source address is 16 bytes
			this.sourceIP = new byte[IP6_ADDR_LENGTH];
			wrapper.get(this.sourceIP);
			
			//destination address is 16 bytes
			this.destIP = new byte[IP6_ADDR_LENGTH];
			wrapper.get(this.destIP);
			
			//UDP payload is payloadLength length
			byte[] udpPayload = new byte[this.payloadLength];
			wrapper.get(udpPayload);
			this.payload = new UDPacket(udpPayload);
			
			//If the UDP packet is parsed correctly, then we can setup coap parser for this source IP
			if (this.payload.isCoap()) {
				
				if (this.payload.getCOAP_PORT() == this.payload.getSourcePort())
					this.payload.getCoapPacket().setRawData(InetAddress.getByAddress(this.getSourceIPAddress().toString(), this.getSourceIPAddress()), this.payload.getSourcePort());
				else
					this.payload.getCoapPacket().setRawData(InetAddress.getByAddress(this.getDestIPAddress().toString(), this.getDestIPAddress()), this.payload.getDestPort());
				
				this.payload.getCoapPacket().parseMessage();
				
				if (this.payload.getCoapPacket().isEmptyMessage())
					System.out.println("Empty Message");
				else if (this.payload.getCoapPacket().isRequest())
					System.out.println("Request: " + this.payload.getCoapPacket().getRequest().getPayloadString());
				else if (this.payload.getCoapPacket().isResponse())
					System.out.println("Response: " + this.payload.getCoapPacket().getResponse().getPayloadString());
				else
					System.out.println("Bu NE MESAJI BOYLE?");
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
	}
	
	public final byte [] getSourceIPAddress() {
		return sourceIP;
	}
	
	public void setSourceIPAddress(byte [] source) {
		this.sourceIP = source;
	}
	
	public final byte [] getDestIPAddress () {
		return destIP;
	}
	
}
