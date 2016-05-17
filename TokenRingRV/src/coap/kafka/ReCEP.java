package coap.kafka;

import com.espertech.esper.client.*;
import java.util.Date;
import java.io.PrintWriter;

public class ReCEP {	 
	 
    private EPRuntime cepRT;
    
    //file operations
    private static PrintWriter rawEventFile;
    private static PrintWriter failEventFile;
    
    private static int violationCounter = 0;
    private static int eventCounter = 0;
    
	//This event class represents Service Composition Calls amongst RESTful embedded systems
    public static class TokenEvent {
    	int eventId;
        int moteId;
        long eventTime; 
 
        public TokenEvent(int eventID, int id, long time) {
        	eventId = eventID;
            moteId = id;
            eventTime = time;
        }
        
        public int getEventId() {return eventId;}
        public int getMoteId() {return moteId;}
        public long getEventTime() {return eventTime;}
 
        @Override
        public String toString() {
            return "Token was Released by mote: " + moteId + " @time: " + eventTime + ", with eventId: " + eventId;
        }
    }
    
    public void send(int evId, int id, long eventTime) {

        System.out.println("ReCEP>> Sending event token_" + evId + ".");
        
        /*
         * This pair of statements effectively inserts new events into TokenEvent schema
         */
    	TokenEvent te = new TokenEvent(evId, id, eventTime);
    	cepRT.sendEvent(te);
    	
    	/*
    	 * We log all incoming events into a file to compare the results after the simulation
    	 */
    	try {    		
    		String rawEvent = new String();
    		rawEvent = evId + "." + id + "." + eventTime;
    		
    		rawEventFile.println(rawEvent);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	System.out.println("ReCEP>> # of Events Received is " + ++eventCounter);
 
    }
 
    /*
     * This listener captures the complex-events triggered by our verification EPL statement
     */
    public static class CEPListener implements UpdateListener {
 
        public void update(EventBean[] newData, EventBean[] oldData) {
                    
        	/*
        	 * The event attributes indicate ids of motes which satisfy aId->bId where m-n != 1
        	 */
        	int aEvId = Integer.parseInt(newData[0].get("aEvId").toString());
        	int aId = Integer.parseInt(newData[0].get("aId").toString());
        	int bEvId = Integer.parseInt(newData[0].get("bEvId").toString());
        	int bId = Integer.parseInt(newData[0].get("bId").toString());
        	
        	/*
        	 * We log all incoming events into a file to compare the results after the simulation
        	 */
           	try {        		
        		String failEvent = new String();
        		failEvent = aEvId + "." + aId + "." + bEvId + "." + bId ;
        		
        		failEventFile.println(failEvent);        		
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
           	
           	
        	System.out.println("ReCEP>> FaILuRE detected: " + newData[0].getUnderlying());

        	/*
        	 * A token (circular) ring protocol will rewind back to the starting mote, in 
        	 * which case aId = 5 when bId =1, and this is not a failure; thus, we should
        	 * omit this case from failure set
        	 */
        	if (!(aId == 5 && bId == 1))
        		System.out.println("ReCEP>> # of Violations Detected is " + ++violationCounter + "\n");   
        }
    }
 
    public ReCEP() {
 
    	//The Configuration is meant only as an initialization-time object.
        Configuration cepConfig = new Configuration();
        
        try { //open the rawevents file
            rawEventFile = new PrintWriter("rawEvents_" + System.currentTimeMillis() + ".txt", "UTF-8");
            
    		String title = new String();
    		title = "EventID	MoteID	EventTime";
    		
    		rawEventFile.println(title);
    		
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        
        try { //open the failureevents file
            failEventFile = new PrintWriter("failEvents_" + System.currentTimeMillis() + ".txt", "UTF-8");	
    		
    		String title = new String();
    		title = "AEventID	AMoteID		BEventID	BMoteID";
    		
    		failEventFile.println(title);
    		
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        
        /*
         * Add a new event type TokenEvent by using addEventType of EPService API.
         * This is effectively same as creating a schema explicitly by using a createEPL(..) function.
         */
        cepConfig.addEventType("TokenEvent", TokenEvent.class.getName());
        EPServiceProvider cep = EPServiceProviderManager.getProvider("myCEPEngine", cepConfig);
        cepRT = cep.getEPRuntime();
        
        EPAdministrator cepAdm = cep.getEPAdministrator();
        
        /*
         * Create a named window consisting of TokenEvent events with unique eventTime attributes
         */
/*        cepAdm.createEPL("create window UniqueTokenWindow.std:unique(eventTime) as TokenEvent");
        cepAdm.createEPL("insert into UniqueTokenWindow select * from TokenEvent");*/
        
        /*
         * Create an ordered named UniqueTokenWindow
         */
        cepAdm.createEPL("create window OrderedUniqueTokenWin.std:unique(eventId).win:keepall() "
        		+ "as select eventId, moteId, eventTime from TokenEvent");
        
        
        /*
         * Create a named window consisting of TokenEvent events with unique eventTime attributes
         */
        cepAdm.createEPL("insert into OrderedUniqueTokenWin select eventId, moteId, eventTime from TokenEvent order by eventTime");
        
        EPStatement cepStatement = cepAdm.createEPL("select a.eventId as aEvId, a.moteId as aId, b.moteId as bId, b.eventId as bEvId "
        		+ "from pattern [every a=OrderedUniqueTokenWin -> b=OrderedUniqueTokenWin] where ( ((b.moteId - a.moteId) != 1) )");
        
        
        cepStatement.addListener(new CEPListener());
 
    } //ReCEP(..)
    
    protected void finalize() throws Throwable {
        try {
        	failEventFile.close();
        	rawEventFile.close();
        } finally {	
            super.finalize();
        }
    }
    
    
}
