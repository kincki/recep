package stuckat.cep;

import com.espertech.esper.client.*;
import java.util.Date;

public class ReCEP {	 
	 
    private EPRuntime cepRT;
    
	//This event class represents Service Composition Calls amongst RESTful embedded systems
    public static class NumberEvent {
        int moteId;
        int myNumber; 
        int eventNumber; 
        Date timeStamp;
 
        public NumberEvent(int id, int number, long t, int eNumber) {
            moteId = id;
            myNumber = number;
            timeStamp = new Date(t);
            eventNumber = eNumber;
        }
        
        public int getMoteId() {return moteId;}
        public int getEventNumber() {return eventNumber;}
        public Date getTimeStamp() {return timeStamp;}
        public int getMyNumber() {return myNumber;}
 
        @Override
        public String toString() {
            return "Token was Released by mote: " + moteId + " @time: " + timeStamp.toString();
        }
    }
    
    public void send(int id, int number, int eNumber) {
        long timeStamp = System.currentTimeMillis();

        System.out.println("ReCEP>> Sending event token_" + id + ".");
    	NumberEvent ne = new NumberEvent(id, number, timeStamp, eNumber);
    	cepRT.sendEvent(ne);
 
    }
 
    public static class CEPListener implements UpdateListener {
 
        public void update(EventBean[] newData, EventBean[] oldData) {
            System.out.println("ReCEP>> Event received: " + newData[0].getUnderlying());
        }
    }
 
    public ReCEP() {
 
    	//The Configuration is meant only as an initialization-time object.
        Configuration cepConfig = new Configuration();
        cepConfig.addEventType("NumberEvent", NumberEvent.class.getName());
        EPServiceProvider cep = EPServiceProviderManager.getProvider("myCEPEngine", cepConfig);
        cepRT = cep.getEPRuntime();
        
        EPAdministrator cepAdm = cep.getEPAdministrator();

      //---------version-2----------//
        /*cepAdm.createEPL("create window OrderedTokensWin.win:keepall() "
        		+ "as select moteId, eventTime, timeStamp from TokenEvent");
        
        
        cepAdm.createEPL("insert into OrderedTokensWin select moteId, eventTime, timeStamp from TokenEvent order by eventTime");
        
        EPStatement cepStatement = cepAdm.createEPL("select a.moteId as aId, b.moteId as bId "
        		+ "from pattern [every a=OrderedTokensWin -> b=OrderedTokensWin] where (b.moteId - a.moteId) != 1");
        
 */       

        cepAdm.createEPL("create variable integer m3Count = 0");
        cepAdm.createEPL("create variable integer m4Count = 0");
        cepAdm.createEPL("create variable integer m5Count = 0");
        
        cepAdm.createEPL("create schema M3Event () copyfrom NumberEvent");
        cepAdm.createEPL("create schema M4Event () copyfrom NumberEvent");
        cepAdm.createEPL("create schema M5Event () copyfrom NumberEvent");
        
        //the event number has to be sent from server side; client side calculation causes misleading results
        /*cepAdm.createEPL("create schema M3Event (eventCount integer) copyfrom NumberEvent");
        cepAdm.createEPL("create schema M4Event (eventCount integer) copyfrom NumberEvent");
        cepAdm.createEPL("create schema M5Event (eventCount integer) copyfrom NumberEvent");
        
        cepAdm.createEPL("on NumberEvent(moteId=3) set m3Count = m3Count+1");
        cepAdm.createEPL("on NumberEvent(moteId=4) set m4Count = m4Count+1");
        cepAdm.createEPL("on NumberEvent(moteId=5) set m5Count = m5Count+1");*/
        
        cepAdm.createEPL("on NumberEvent "
        		+ "insert into M3Event select moteId, myNumber, eventNumber, timeStamp where moteId = 3 "
        		+ "insert into M4Event select moteId, myNumber, eventNumber, timeStamp where moteId = 4 "
        		+ "insert into M5Event select moteId, myNumber, eventNumber, timeStamp where moteId = 5"
        		);
        
        //this pattern statement tries to catch those event stream patterns where myNumber is different at either two of M events for an eventNumber of M3Event 
        /*EPStatement cepStatement = cepAdm.createEPL("select m3.myNumber, m3.eventNumber, m4.eventNumber, m5.eventNumber " 
        		+ "from pattern [every-distinct(m3.eventNumber, m3.myNumber) m3 = M3Event and every-distinct(m4.eventNumber, m4.myNumber) m4 = M4Event(eventNumber = m3.eventNumber) and every-distinct(m5.myNumber, m5.eventNumber) m5 = M5Event(eventNumber = m4.eventNumber)] "
        		+ "where (m3.myNumber != m4.myNumber) or "
        		+ "(m3.myNumber != m5.myNumber) or "
        		+ "(m4.myNumber != m5.myNumber)"
        		);*/
        
        cepAdm.createEPL("create context M3Context start pattern[every (m1 = M3Event -> m2 = M3Event(myNumber = m1.myNumber))]@inclusive "
        		+ " end pattern [M3Event(myNumber != m1.myNumber) or timer:interval(60 seconds)] ");
        
        EPStatement cepStatement = cepAdm.createEPL("select moteId, eventNumber from M3Event.win:time_batch(60 seconds)" );
        
        cepStatement.addListener(new CEPListener());
 
    }
}
