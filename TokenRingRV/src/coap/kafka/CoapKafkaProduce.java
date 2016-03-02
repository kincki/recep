package coap.kafka;

import java.util.*;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class CoapKafkaProduce {
	
	Properties props;
    ProducerConfig config;
    static Producer producer;


	public CoapKafkaProduce()
	{
		props = new Properties();

        props.put("metadata.broker.list", "localhost:9092,localhost:9093 ");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("partitioner.class", "coap.kafka.SimplePartitioner");
        props.put("request.required.acks", "1");
 
        config = new ProducerConfig(props);

		producer = new Producer<String, String>(config);
		
	}
	
	public void send(int input)
	{		
		long runtime = new Date().getTime();

		String ip = "192.168.2." + input;
		
		String msg = runtime + ", www.example.com, " + ip;
		
		KeyedMessage<String, String> data = new KeyedMessage<String, String>("page-visit", ip, msg);
		
		producer.send(data);				 
	}

}
