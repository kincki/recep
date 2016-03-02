package coap.kafka;

import com.espertech.esper.client.*;
import java.util.Date;

public class ReCEP {	 
	 
    private EPRuntime cepRT;
    
	//This event class represents Service Composition Calls amongst RESTful embedded systems
    public static class TokenEvent {
        int moteId;
        long eventTime; 
        Date timeStamp;
 
        public TokenEvent(int id, long t, long time) {
            moteId = id;
            timeStamp = new Date(t);
            eventTime = time;
        }
        
        public int getMoteId() {return moteId;}
        public long getEventTime() {return eventTime;}
        public Date getTimeStamp() {return timeStamp;}
 
        @Override
        public String toString() {
            return "Token was Released by mote: " + moteId + " @time: " + timeStamp.toString();
        }
    }
    
    public void send(int id, long eventTime) {
        long timeStamp = System.currentTimeMillis();

        System.out.println("ReCEP>> Sending event token_" + id + ".");
    	TokenEvent te = new TokenEvent(id, timeStamp, eventTime);
    	cepRT.sendEvent(te);
 
    }
 
    public static class CEPListener implements UpdateListener {
 
        public void update(EventBean[] newData, EventBean[] oldData) {
            System.out.println("ReCEP>> Event received: " + newData[0].getUnderlying());
        }
    }
 
    public ReCEP() {
 
    	//The Configuration is meant only as an initialization-time object.
        Configuration cepConfig = new Configuration();
        cepConfig.addEventType("TokenEvent", TokenEvent.class.getName());
        EPServiceProvider cep = EPServiceProviderManager.getProvider("myCEPEngine", cepConfig);
        cepRT = cep.getEPRuntime();
        
        EPAdministrator cepAdm = cep.getEPAdministrator();
        //--------version-1-----------//
        /*EPStatement cepStatement = cepAdm.createEPL(
        		"select * from TokenEvent match_recognize ( " + 
        		" partition by moteId " +
        		" measures A.moteId as a_id, B.moteId as b_id " + 
        		" pattern (A B) " +  
        		" define " + 
        		" A as A.eventTime < B.eventTime, " +
        		" B as (B.moteId - A.moteId) != 1 )"
        		"select moteId eventTime from TokenEvent.win:length(2) having (moteId - prev(1, moteId)) != 1 & eventTime > prev(1, eventTime)"
        		);
 */
        //---------version-2----------//
        cepAdm.createEPL("create window OrderedTokensWin.win:keepall() "
        		+ "as select moteId, eventTime, timeStamp from TokenEvent");
        
        
        cepAdm.createEPL("insert into OrderedTokensWin select moteId, eventTime, timeStamp from TokenEvent order by eventTime");
        
        EPStatement cepStatement = cepAdm.createEPL("select a.moteId as aId, b.moteId as bId "
        		+ "from pattern [every a=OrderedTokensWin -> b=OrderedTokensWin] where (b.moteId - a.moteId) != 1");
        
        
      //---------version-3----------//
       /* cepAdm.createEPL("create schema OrderedTokens () copyfrom TokenEvent");
        
        cepAdm.createEPL("on TokenEvent insert into OrderedTokens select * order by eventTime");
        
        EPStatement cepStatement = cepAdm.createEPL("select a.moteId as aId, b.moteId as bId "
        		+ "from pattern [every a=OrderedTokens -> b=OrderedTokens] where (b.moteId - a.moteId) != 1");
        */
        cepStatement.addListener(new CEPListener());
 
    }
}