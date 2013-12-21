package edu.sjsu.cmpe.procurement;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.eclipse.jetty.util.ajax.JSON;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.client.JerseyClientBuilder;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import de.spinscale.dropwizard.jobs.JobsBundle;
import edu.sjsu.cmpe.procurement.api.resources.RootResource;
import edu.sjsu.cmpe.procurement.config.ProcurementServiceConfiguration;
import edu.sjsu.cmpe.procurement.domain.Book;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
public class ProcurementService extends Service<ProcurementServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * FIXME: THIS IS A HACK!
     */
    public static Client jerseyClient;
    List  ids=new ArrayList();
    String []books;
    public static void main(String[] args) throws Exception {
	new ProcurementService().run(args);
    }

    @Override
    public void initialize(Bootstrap<ProcurementServiceConfiguration> bootstrap) {
	bootstrap.setName("procurement-service");
	/**
	 * NOTE: All jobs must be placed under edu.sjsu.cmpe.procurement.jobs
	 * package
	 */
	bootstrap.addBundle(new JobsBundle("edu.sjsu.cmpe.procurement.jobs"));
    }

    @Override
    public void run(ProcurementServiceConfiguration configuration,
	    Environment environment) throws Exception {
	jerseyClient = new JerseyClientBuilder()
	.using(configuration.getJerseyClientConfiguration())
	.using(environment).build();

	/**
	 * Root API - Without RootResource, Dropwizard will throw this
	 * exception:
	 * 
	 * ERROR [2013-10-31 23:01:24,489]
	 * com.sun.jersey.server.impl.application.RootResourceUriRules: The
	 * ResourceConfig instance does not contain any root resource classes.
	 */
	environment.addResource(RootResource.class);
	
	String queueName = configuration.getStompQueueName();
	String topicName = configuration.getStompTopicPrefix();
	log.debug("Queue name is {}. Topic is {}", queueName, topicName);
	
	
    }
    
	public List<Long> consumers() throws JMSException{
		
			String user = "admin";
			String password ="password";
			String host = "54.219.156.168";
			int port =61613;
			String destination = "/queue/06830.book.orders";

			StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
			factory.setBrokerURI("tcp://" + host + ":" + port);

			Connection connection = factory.createConnection(user, password);
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination dest = new StompJmsDestination(destination);

			MessageConsumer consumer = session.createConsumer(dest);
			System.currentTimeMillis();
			System.out.println("Waiting for messages...");
			long waitUntil = 5000; //for 5 sec
			  List<Long> isbns=new ArrayList<Long>();
			while(true) {
				
			    Message msg = consumer.receive(waitUntil);
			   
			  
			    if( msg instanceof  TextMessage ) {
			    	
			    	String body = ((TextMessage) msg).getText();
			    	String test[]=body.split(":");
			    	isbns.add(Long.parseLong(test[1]));	    	
			    	System.out.println("Received message1 = " + body);
			    	System.out.println(isbns);
				} else if (msg instanceof StompJmsMessage) {
				StompJmsMessage smsg = ((StompJmsMessage) msg);
				String body = smsg.getFrame().contentAsString();
				System.out.println("Received message2 = " + body);
				}
					else if(msg == null){
					
					System.out.println("No new messages. Existing due to timeout - " + waitUntil / 1000 + " sec");
					break;
				}
				else {
				System.out.println("Unexpected message type: "+msg.getClass());
			    }
			   
			}
			connection.close();
			
			System.out.println("Done");
		
			return isbns;
		    }

		   
		    public void postHttp(List<Long> isbns){
		    	try {
		    		 
		    		Client client = Client.create();
		     
		    		WebResource webResource = client
		    		   .resource("http://54.219.156.168:9000/orders");
		     
		    		String input = "{\"id\":\"06830\",\"order_book_isbns\":"+isbns+"}";
		    		System.out.println(input);
		     
		    		ClientResponse response = webResource.type("application/json")
		    		   .post(ClientResponse.class, input);
		     
		    		if (response.getStatus() != 200) {
		    			throw new RuntimeException("Failed : HTTP error code : "
		    			     + response.getStatus());
		    		}
		     
		    		System.out.println("Message posted successfully");
		    		String output = response.getEntity(String.class);
		    		System.out.println(output);
		     
		    	  } catch (Exception e) {
		     
		    		e.printStackTrace();
		     
		    	  }
		    }
		    public void getHttp(){
		    	String output = "";
		    	try{
		    		Client client = Client.create();
		    		 
		    		WebResource webResource = client
		    		   .resource("http://54.219.156.168:9000/orders/06830" );
		     
		    		ClientResponse response = webResource.accept("application/json")
		                       .get(ClientResponse.class);
		     
		    		if (response.getStatus() != 200) {
		    		   throw new RuntimeException("Failed : HTTP error code : "
		    			+ response.getStatus());
		    		}
		     
		    		output = response.getEntity(String.class);
		     
		    		System.out.println("Output from Server .... \n");
		    		 books=output.split("\\[");
		    	  } catch (Exception e) {
		     
		    		e.printStackTrace();
		     
		    	  }
		    	
		    	
		  String[] split=books[1].split("\\}");
		 for(int i=0;i<split.length;i++){
			 split[i]=split[i]+"}";
			 if(i!=0)
			split[i]=split[i].substring(2);
		 }
			
		  
		   try {
			   JSONArray nameArray =(JSONArray) JSONSerializer.toJSON(split);
               System.out.println(split.length);
               for(Object js : nameArray){
               	JSONObject json = (JSONObject)JSONSerializer.toJSON(js);
			   String user ="admin";
				String password ="password";
				String host ="54.219.156.168";
				int port =61613;
				StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
				factory.setBrokerURI("tcp://" + host + ":" + port);
			   Connection connection = factory.createConnection(user, password);
				connection.start();
				Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    String cat=(String)json.get("category");
                    String coverImage =(String) json.get("coverimage");
					int isbn = json.getInt("isbn");
					String title = (String) json.get("title");
                    if(cat.equalsIgnoreCase("computer")){
                    String destination1 = "/topic/06830.book." + cat;
					System.out.println("destination1 is "+destination1);
					Destination dest = new StompJmsDestination(destination1);
					MessageProducer producer = session.createProducer(dest);
						String data = isbn + ":\"" + title + "\":\"" + cat + "\":\"" + coverImage+"\"";
						System.out.println(data);
						TextMessage msg = session.createTextMessage(data);
						msg.setLongProperty("id", System.currentTimeMillis());
						producer.send(msg);
						System.out.println("Data published succesfully");
					}
                    else  if(cat.equalsIgnoreCase("comics")){
						
						String destination1 = "/topic/06830.book." + json.get("category");
							
						Destination dest = new StompJmsDestination(destination1);
						MessageProducer producer = session.createProducer(dest);
						producer.setDeliveryMode(DeliveryMode.PERSISTENT);
						String data = isbn + ":\"" + title + "\":\"" + cat + "\":\"" + coverImage+"\"";
						System.out.println(data);
						TextMessage msg = session.createTextMessage(data);
						msg.setLongProperty("id", System.currentTimeMillis());
						producer.send(msg);
						System.out.println("Data published succesfully");
                    		}
                    else if(cat.equalsIgnoreCase("management")){
						
						String destination1 = "/topic/06830.book." + json.get("category");
							
						Destination dest = new StompJmsDestination(destination1);
						MessageProducer producer = session.createProducer(dest);
						producer.setDeliveryMode(DeliveryMode.PERSISTENT);				
						String data = isbn + ":\"" + title + "\":\"" + cat + "\":\"" + coverImage+"\"";
						System.out.println(data);
						TextMessage msg = session.createTextMessage(data);
						msg.setLongProperty("id", System.currentTimeMillis());
						producer.send(msg);
						System.out.println("Data published succesfully");

					}
                    else if(cat.equalsIgnoreCase("selfimprovement")){
						
						String destination1 = "/topic/06830.book." + json.get("category");
							
						Destination dest = new StompJmsDestination(destination1);
						MessageProducer producer = session.createProducer(dest);
						producer.setDeliveryMode(DeliveryMode.PERSISTENT);
						String data = isbn + ":\"" + title + "\":\"" + cat + "\":\"" + coverImage+"\"";
						System.out.println(data);
						TextMessage msg = session.createTextMessage(data);
						msg.setLongProperty("id", System.currentTimeMillis());
						producer.send(msg);
						System.out.println("Data published succesfully");
					}
                }}catch(Exception e)
                {
                	
                }
                
		    	
		  
		     
		    	}

		    
}   
		    
