package jCoapParser;

import net.sourceforge.jpcap.client.*;
import net.sourceforge.jpcap.capture.*;
import net.sourceforge.jpcap.net.*;
import net.sourceforge.jpcap.util.*;

public class Parser 
{
  private static final int INFINITE = -1;
  private static final int PACKET_COUNT = 100; 

  // BPF filter for capturing any packet
  private static final String FILTER = "";

  private PacketCapture m_pcap;
  //private String m_device;

  public Parser() throws Exception {
    // Step 1:  Instantiate Capturing Engine
    m_pcap = new PacketCapture();

    // Step 2:  Check for devices 
    //m_device = m_pcap.findDevice();
    String[] devices = PacketCapture.lookupDevices();

    int tunId = -1; //network interface id for coap comm
    
    for (int i = 0; i < devices.length; i++ )
    {
    	if (devices[i].contains("tun") )
    		tunId = i;
    }
    
    // Step 3:  Open Device for Capturing (requires root)
    //m_pcap.open(m_device, true);
    
    if ( -1 != tunId )
    	m_pcap.open(devices[tunId], true);
    
    // Step 4:  Add a BPF Filter (see tcpdump documentation)
    m_pcap.setFilter(FILTER, true);
    
    // Step 5:  Register a Listener for Raw Packets
    m_pcap.addRawPacketListener(new RawPacketHandler());

    // Step 6:  Capture Data (max. PACKET_COUNT packets)
    m_pcap.capture(INFINITE);
    //m_pcap.capture(PACKET_COUNT);
  }

  public static void main(String[] args) {
    try {
      Parser example = new Parser();
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}


class RawPacketHandler implements RawPacketListener 
{
  private static int m_counter = 0;

  public void rawPacketArrived(RawPacket data) {
    m_counter++;

    try {
    	IPv6Packet packet = new IPv6Packet(data);
        packet.parseRawData();	
    } catch (Exception e) {
    	System.out.println(e.getMessage());
    }
    
    
    //System.out.println("Packet " + m_counter + "\n" + AsciiHelper.toText(data.getData()) + "\n" + "length: " + data.toString().length() + "\n");
    //System.out.println("Packet " + m_counter + "\n" + data + "\n" + "length: " + data.toString().length() + "\n");    
    
  }
}
