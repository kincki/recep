/**
 * This packet maintains COAP protocol stack.
 * The raw packet format for COAP messages is as follows:
 * 
 * 00 00 ff fe 00 00 40 b6  60 d8 0e 6b 60 d8 86 dd //MAC
 * 60 00 00 00 00 1f 11 3f  aa aa 00 00 00 00 00 00 //IPv6
 * c3 0c 00 00 00 00 00 01  aa aa 00 00 00 00 00 00 //IPv6 | UDP
 * 00 00 00 00 00 00 00 01  16 33 9a a4 00 1f 3b d7 //COAP : depends on message length
 * 52 45 1b 8a 1e 5b 61 0b  60 ff 45 56 45 4e 54 2e //COAP : depends on message length
 * 31 2e 34 32 33 35 35				 				//COAP : depends on message length
 * 
 */
package jCoapParser;

import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.CoAP.Type;
import java.net.InetAddress;


/**
 * @author root
 *
 */
public class CoapPacket {
	
	private byte[] payload;
	private RawData rawData;
	private DataParser parser;
	private Request request;
	private Response response;
	private EmptyMessage empMessage;

	public CoapPacket(byte[] coapPayload) {
		this.payload = coapPayload;
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * The IPv6Packet parser calls this method to instantiate a RawData
	 * representation of the incoming packet after parsing IP-UDP-CoAP
	 * packets.
	 * @param address is the network byte order representation of source IP address
	 * @param port is the port address of the source 
	 */
	public void setRawData(InetAddress address, int port)
	{
		this.rawData = new RawData(payload, address, port, false);
	}
	
	
	/*
	 * The endpoint's executor executes this method to convert the raw bytes
	 * into a message, look for an associated exchange and forward it to
	 * the stack of layers.
	 */
	public void parseMessage() {
		parser = new DataParser(rawData.getBytes());
		
		if (parser.isRequest()) {
			// This is a request
			try {
				setRequest(parser.parseRequest());
			} catch (IllegalStateException e) {
				StringBuffer log = new StringBuffer("message format error caused by ")
					.append(rawData.getInetSocketAddress());
				if (!parser.isReply()) {
					// manually build RST from raw information
					EmptyMessage rst = new EmptyMessage(Type.RST);
					rst.setMID(parser.getMID());
					rst.setToken(new byte[0]);
					rst.setDestination(rawData.getAddress());
					rst.setDestinationPort(rawData.getPort());
/*						for (MessageInterceptor interceptor:interceptors)
						interceptor.sendEmptyMessage(rst);
					connector.send(serializer.serialize(rst));
*/						log.append(" and reset");
				}
/*					if (LOGGER.isLoggable(Level.INFO)) {
					LOGGER.info(log.toString());
				}
*/					return;
			}
			getRequest().setSource(rawData.getAddress());
			getRequest().setSourcePort(rawData.getPort());
			getRequest().setSenderIdentity(rawData.getSenderIdentity());
			
			/* 
			 * Logging here causes significant performance loss.
			 * If necessary, add an interceptor that logs the messages,
			 * e.g., the MessageTracer.
			 */
			
/*				for (MessageInterceptor interceptor:interceptors)
				interceptor.receiveRequest(request);
*/
			// MessageInterceptor might have canceled
			if (!getRequest().isCanceled()) {
/*					Exchange exchange = matcher.receiveRequest(request);
				if (exchange != null) {
					exchange.setEndpoint(CoapEndpoint.this);
					coapstack.receiveRequest(exchange, request);
				}
	*/		}
			
		} else if (parser.isResponse()) {
			// This is a response
			setResponse(parser.parseResponse());
			getResponse().setSource(rawData.getAddress());
			getResponse().setSourcePort(rawData.getPort());
			
			/* 
			 * Logging here causes significant performance loss.
			 * If necessary, add an interceptor that logs the messages,
			 * e.g., the MessageTracer.
			 */
			
/*				for (MessageInterceptor interceptor:interceptors)
				interceptor.receiveResponse(response);
*/
			// MessageInterceptor might have canceled
			if (!getResponse().isCanceled()) {
/*					Exchange exchange = matcher.receiveResponse(response);
				if (exchange != null) {
					exchange.setEndpoint(CoapEndpoint.this);
					response.setRTT(System.currentTimeMillis() - exchange.getTimestamp());
					coapstack.receiveResponse(exchange, response);
				} else if (response.getType() != Type.ACK) {
					LOGGER.fine("Rejecting unmatchable response from " + raw.getInetSocketAddress());
					reject(response);
				}*/
			}
			
		} else if (parser.isEmpty()) {
			// This is an empty message
			setEmpMessage(parser.parseEmptyMessage());
			getEmpMessage().setSource(rawData.getAddress());
			getEmpMessage().setSourcePort(rawData.getPort());
			
			/* 
			 * Logging here causes significant performance loss.
			 * If necessary, add an interceptor that logs the messages,
			 * e.g., the MessageTracer.
			 */
			
/*				for (MessageInterceptor interceptor:interceptors)
				interceptor.receiveEmptyMessage(message);
*/
			// MessageInterceptor might have canceled
			if (!getEmpMessage().isCanceled()) {
				// CoAP Ping
				if (getEmpMessage().getType() == Type.CON || getEmpMessage().getType() == Type.NON) {
					/*LOGGER.info("Responding to ping by " + rawData.getInetSocketAddress());
					reject(message);*/
				} else {
					/*Exchange exchange = matcher.receiveEmptyMessage(message);
					if (exchange != null) {
						exchange.setEndpoint(CoapEndpoint.this);
						coapstack.receiveEmptyMessage(exchange, message);
					}*/
				}
			}
		} else {
/*				LOGGER.finest("Silently ignoring non-CoAP message from " + rawData.getInetSocketAddress());
*/			}
	}
	
	private void reject(Message message) {
		EmptyMessage rst = EmptyMessage.newRST(message);
		// sending directly through connector, not stack, thus set token
		rst.setToken(new byte[0]);
		
/*			for (MessageInterceptor interceptor:interceptors)
			interceptor.sendEmptyMessage(rst);*/
		
		// MessageInterceptor might have canceled
/*			if (!rst.isCanceled())
			connector.send(serializer.serialize(rst));*/
	}
	/**
	 * The connector uses this channel to forward messages (in form of
	 * {@link RawData}) to the endpoint. The endpoint creates a new task to
	 * process the message. The task consists of invoking the matcher to look
	 * for an associated exchange and then forwards the message with the
	 * exchange to the stack of layers.
	 */
	/*private class InboxImpl implements RawDataChannel {

		@Override
		public void receiveData(final RawData raw) {
			if (raw.getAddress() == null)
				throw new NullPointerException();
			if (raw.getPort() == 0)
				throw new NullPointerException();
			
			// Create a new task to process this message
			Runnable task = new Runnable() {
				public void run() {
					receiveMessage(raw);
				}
			};
			//runInProtocolStage(task);
		}
		
		

	}*/

	/**
	 * This method is used to retrieve a copy of response message
	 * @return Response response
	 */
	public final Response getResponse() {
		return response;
	}

	/**
	 * This method is used to set new value in the internal response message
	 * representation. 
	 * @param response
	 */
	public void setResponse(Response response) {
		this.response = response;
	}

	/**
	 * This method is user to retrieve a copy of request message
	 * @return Request request
	 */
	public final Request getRequest() {
		return request;
	}

	/**
	 * This method is used to set a new value in the internal request message
	 * @param request
	 */
	public void setRequest(Request request) {
		this.request = request;
	}

	/**
	 * This method is used to get a copy of empty message
	 * @return EmptyMessage message
	 */
	public final EmptyMessage getEmpMessage() {
		return empMessage;
	}
	
	/**
	 * This method is used to set a new value in the internal empty messsage
	 * @param empMessage
	 */
	public void setEmpMessage(EmptyMessage empMessage) {
		this.empMessage = empMessage;
	}
	
	/**
	 * This method is used to determine if the payload is a @type Response message
	 * @return @type boolean True if it is a Response message
	 */
	public boolean isResponse() {
		return parser.isResponse();
	}

	/**
	 * This method is used to determine if the payload is a @type Request message
	 * @return @type boolean True if it is a Request message
	 */
	public boolean isRequest() {
		return parser.isRequest();
	}
	
	/** 
	 * This method is used to determine if the payload is a @type EmptyMessage message
	 * @return @type boolean True if it is an EmptyMessage message
	 */
	public boolean isEmptyMessage() {
		return parser.isEmpty();
	}
	

	
	/**
	 * Execute the specified task on the endpoint's executor (protocol stage).
	 *
	 * @param task the task
	 */
	/*private void runInProtocolStage(final Runnable task) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					task.run();
				} catch (Throwable t) {
					LOGGER.log(Level.SEVERE, "Exception in protocol stage thread: "+t.getMessage(), t);
				}
			}
		});
	}*/

}
