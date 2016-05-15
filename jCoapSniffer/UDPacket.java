/**
 * 
 */
package jCoapParser;

import java.nio.ByteBuffer;

/**
 * @author root
 *
 */
public class UDPacket {

	short sourcePort; //sender's port address
	short destPort; //receiver's port address
	short length; //length of the payload.
	short checkSum; //message checksum
	
	CoapPacket coapPacket;
	
	public UDPacket(byte[] udpPayload) {
		// TODO Auto-generated constructor stub
		ByteBuffer wrapper = ByteBuffer.wrap(udpPayload);
		
		//get source port
		this.sourcePort = wrapper.getShort();
		
		//get destination port
		//add two (2) for source port field
		this.destPort = wrapper.getShort(2);
		
		//get length of payload
		//add 2 for source port, 2 for destination port fields
		this.length = wrapper.getShort(2+2);
		
		//get checksum
		//add 2 for source port, 2 for destination port, 2 for length
		this.checkSum = wrapper.getShort(2+2+2);
		
		//strip coap payload
		//assume that UDP payload is CoAP packet
		byte[] coapPayload = new byte[length];
		wrapper.get(coapPayload, 0, length);
		coapPacket = new CoapPacket(coapPayload);
		
	}

}