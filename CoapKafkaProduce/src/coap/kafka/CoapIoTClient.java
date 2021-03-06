package coap.kafka;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

public class CoapIoTClient implements Runnable {
	
	private Thread coapThread;
	private String url;
	private String moteId;
	private Request request;
	private static ReCEP recep;
	
	public CoapIoTClient(String uri, String id, ReCEP cep) {
		this.request = Request.newGet();
		this.url = uri;
		this.moteId = id;
		this.recep = cep;
		
		System.out.println("CoAPClient init to: " + url);
			
		request.setURI(url);
		request.setObserve();
		
	}
	
	private void startObserve() {
		
		try {
			while (true) {
				request.send();
				//what should be the optimum value for waiting response
				Response response = request.waitForResponse(1000);
				
				if (null != response) {
					//parse the input string to obtain mote-id and lux value
					String responseString = response.toString();
					System.out.println("RESPONSE: " + responseString);
					String delim = "[;]";
					String[] res = responseString.split("\"");
					String[] tokens = res[res.length-1].split(delim);
					
					int mote_id = Integer.parseInt(tokens[0]);
					int val = Integer.parseInt(tokens[1]);
					recep.send(mote_id, val);
					
				} //if (null...)
				else {
					System.out.println("No Response ");
				}
			} //while
		} catch (InterruptedException e) {
            System.err.println("Receiving of response interrupted: " + e.getMessage());
            System.exit(-1);
        }
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Running CoAPClient-" + moteId);
		startObserve();	
		System.out.println("CoAPClient-" +  moteId + " exiting.");
	}
	
	public void start() {
		System.out.println("Starting CoAPClient-" + moteId);
		if (null == coapThread) {
			coapThread = new Thread(this, moteId);
			coapThread.start();
		}
	}
}
