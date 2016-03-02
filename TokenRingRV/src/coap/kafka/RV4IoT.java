package coap.kafka;

public class RV4IoT {
	
	private static String[] urls = {
			"coap://[aaaa::c30c:0:0:1]:5683/test/event",
			"coap://[aaaa::c30c:0:0:2]:5683/test/event",
			"coap://[aaaa::c30c:0:0:3]:5683/test/event",
			"coap://[aaaa::c30c:0:0:4]:5683/test/event",
			"coap://[aaaa::c30c:0:0:5]:5683/test/event"
	};
	
	public static void main(String[] args) {
		ReCEP recep = new ReCEP();
		
		CoapIoTClient[] clients = new CoapIoTClient[urls.length];
		
		for (int i=0; i < urls.length; i++) {
			clients[i] = new CoapIoTClient(urls[i], Integer.toString(i), recep);
			clients[i].start();
		}
		
		for (int i=0; i < urls.length; i++) {
			
		}
	}
}
