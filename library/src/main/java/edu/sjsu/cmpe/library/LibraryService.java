package edu.sjsu.cmpe.library;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import edu.sjsu.cmpe.library.api.resources.BookResource;
import edu.sjsu.cmpe.library.api.resources.RootResource;
import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.repository.BookRepository;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;
import edu.sjsu.cmpe.library.ui.resources.HomeResource;

public class LibraryService extends Service<LibraryServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
	new LibraryService().run(args);
	
        
        
    }

    @Override
    public void initialize(Bootstrap<LibraryServiceConfiguration> bootstrap) {
	bootstrap.setName("library-service");
	bootstrap.addBundle(new ViewBundle());
	bootstrap.addBundle(new AssetsBundle());
    }
    
	
	
	@Override
	public void run(final LibraryServiceConfiguration configuration,
		    Environment environment) throws Exception {
		// This is how you pull the configurations from library_x_config.yml
		String queueName = configuration.getStompQueueName();
		String topicName = configuration.getStompTopicName();
	   
		log.debug("{} - Queue name is {}. Topic name is {}",
			configuration.getLibraryName(), queueName,
			topicName);
	   
		/** Root API */
		environment.addResource(RootResource.class);
		/** Books APIs */
		final BookRepositoryInterface bookRepository = new BookRepository();
		environment.addResource(new BookResource(bookRepository));
		/** UI Resources */
		 bookRepository.config(configuration);
		
	    environment.addResource(new HomeResource(bookRepository));
	   // bookRepository.listener(configuration);
	    int numThreads = 1;
	    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

	    Runnable backgroundTask = new Runnable() {
	    	
	    
		@Override
		public void run() {
			String user ="admin";
	    	String password ="password";
	    	String host ="54.215.210.214";
	    	int port =61613;
	    
	    	String destination = configuration.getStompTopicName();
	    	

	    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	    	factory.setBrokerURI("tcp://" + host + ":" + port);

	    	Connection connection;
			try{
				connection = factory.createConnection(user, password);
			
	    	connection.start();
	    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    	Destination dest = new StompJmsDestination(destination);

	    	MessageConsumer consumer = session.createConsumer(dest);
	    	System.currentTimeMillis();
	    	System.out.println("Waiting for messages...");
	    	while(true) {
	    	    Message msg = consumer.receive();
	    	    if( msg instanceof  TextMessage ) {
	    		String body = ((TextMessage) msg).getText();
	    		if( "SHUTDOWN".equals(body)) {
	    		    break;
	    		}
	    		System.out.println("Received message = " + body);
	    		String[] bookinstance=body.split("\"");
	    		System.out.println("bookinstance is"+bookinstance);
	    		Long isbn=Long.parseLong(bookinstance[0].split(":")[0]);
	    		Book book=new Book();
	    		book.setIsbn(isbn);
	    		book.setTitle(bookinstance[1]);
	    		book.setCategory(bookinstance[3]);
	    		book.setCoverimage(new URL(bookinstance[5]));
	    		bookRepository.statusUpdate(book);
	    		
	    	    } else if (msg instanceof StompJmsMessage) {
	    		StompJmsMessage smsg = ((StompJmsMessage) msg);
	    		String body = smsg.getFrame().contentAsString();
	    		if ("SHUTDOWN".equals(body)) {
	    		    break;
	    		}
	    		System.out.println("Received message = " + body);
	    		
	    		
	    		
	    		
	    		
	    	    } else {
	    		System.out.println("Unexpected message type: "+msg.getClass());
	    	    }
	    	
	    	   
	    		   
	    	  
	    	}
	    	
				connection.close();
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        } };
	        executor.execute(backgroundTask);
	    	executor.shutdown();
	    }

    }
