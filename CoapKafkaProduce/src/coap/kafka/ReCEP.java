package coap.kafka;

import com.espertech.esper.client.*;
import java.util.Date;

public class ReCEP {	 
	 
    private EPRuntime cepRT;
    
	//This event class represents Service Composition Calls amongst RESTful embedded systems
    public static class LightEvent {
        int moteId;
        int luxVal;
        Date timeStamp;
 
        public LightEvent(int id, int val, long t) {
            moteId = id;
            luxVal = val;
            timeStamp = new Date(t);
        }
        
        public int getMoteId() {return moteId;}
        public int getLuxVal() {return luxVal;}
        public Date getTimeStamp() {return timeStamp;}
 
        @Override
        public String toString() {
            return "Lux value: " + luxVal + " received from " + moteId + " @time: " + timeStamp.toString();
        }
    }
    
    public void send(int id, int val) {
        long timeStamp = System.currentTimeMillis();

        System.out.println("Sending event lux_value " + val + " from mote-"+ id + ".");
    	LightEvent le = new LightEvent(id, val, timeStamp);
    	cepRT.sendEvent(le);
 
    }
 
    public static class CEPListener implements UpdateListener {
 
        public void update(EventBean[] newData, EventBean[] oldData) {
            System.out.println("Event received: " + newData[0].getUnderlying());
        }
    }
 
    public ReCEP() {
 
    	//The Configuration is meant only as an initialization-time object.
        Configuration cepConfig = new Configuration();
        cepConfig.addEventType("LightEvent", LightEvent.class.getName());
        EPServiceProvider cep = EPServiceProviderManager.getProvider("myCEPEngine", cepConfig);
        cepRT = cep.getEPRuntime();
        
        EPAdministrator cepAdm = cep.getEPAdministrator();
        EPStatement cepStatement = cepAdm.createEPL("select * from " +
                "LightEvent.win:time(4 sec) " +
                "having avg(luxVal) > 100.0");
 
        cepStatement.addListener(new CEPListener());
 
    }
}
