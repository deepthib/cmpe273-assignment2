package edu.sjsu.cmpe.library.repository;

import static com.google.common.base.Preconditions.checkArgument;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;

public class BookRepository implements BookRepositoryInterface {
    /** In-memory map to store books. (Key, Value) -> (ISBN, Book) */
    private final ConcurrentHashMap<Long, Book> bookInMemoryMap;

    /** Never access this key directly; instead use generateISBNKey() */
    private long isbnKey;
    private String user="";
    private String password="";
    private String host="";
    private String port="";
    private String queue="";
    private String libraryName="";

    public BookRepository() {
	bookInMemoryMap = seedData();
	isbnKey = 0;
    }

    private ConcurrentHashMap<Long, Book> seedData(){
	ConcurrentHashMap<Long, Book> bookMap = new ConcurrentHashMap<Long, Book>();
	Book book = new Book();
	book.setIsbn(1);
	book.setCategory("computer");
	book.setTitle("Java Concurrency in Practice");
	try {
	    book.setCoverimage(new URL("http://goo.gl/N96GJN"));
	} catch (MalformedURLException e) {
	    // eat the exception
	}
	bookMap.put(book.getIsbn(), book);

	book = new Book();
	book.setIsbn(2);
	book.setCategory("computer");
	book.setTitle("Restful Web Services");
	try {
	    book.setCoverimage(new URL("http://goo.gl/ZGmzoJ"));
	} catch (MalformedURLException e) {
	    // eat the exception
	}
	bookMap.put(book.getIsbn(), book);

	return bookMap;
    }

    /**
     * This should be called if and only if you are adding new books to the
     * repository.
     * 
     * @return a new incremental ISBN number
     */
    private final Long generateISBNKey() {
	// increment existing isbnKey and return the new value
	return Long.valueOf(++isbnKey);
    }

    /**
     * This will auto-generate unique ISBN for new books.
     */
    @Override
    public Book saveBook(Book newBook) {
	checkNotNull(newBook, "newBook instance must not be null");
	// Generate new ISBN
	Long isbn = generateISBNKey();
	newBook.setIsbn(isbn);
	// TODO: create and associate other fields such as author

	// Finally, save the new book into the map
	bookInMemoryMap.putIfAbsent(isbn, newBook);

	return newBook;
    }

    /**
     * @see edu.sjsu.cmpe.library.repository.BookRepositoryInterface#getBookByISBN(java.lang.Long)
     */
    @Override
    public Book getBookByISBN(Long isbn) {
	checkArgument(isbn > 0,
		"ISBN was %s but expected greater than zero value", isbn);
	return bookInMemoryMap.get(isbn);
    }

    @Override
    public List<Book> getAllBooks() {
	return new ArrayList<Book>(bookInMemoryMap.values());
    }

    /*
     * Delete a book from the map by the isbn. If the given ISBN was invalid, do
     * nothing.
     * 
     * @see
     * edu.sjsu.cmpe.library.repository.BookRepositoryInterface#delete(java.
     * lang.Long)
     */
    @Override
    public void delete(Long isbn) {
	bookInMemoryMap.remove(isbn);
    }
    @Override
    public void config(LibraryServiceConfiguration configuration){
     user=configuration.getApolloUser();
	 password=configuration.getApolloPassword();
	  host=configuration.getApolloHost();
	  port= configuration.getApolloPort();
	 queue=configuration.getStompQueueName();
	 libraryName=configuration.getLibraryName();
  }
    @Override
    public void producer(Long isbn) throws JMSException{
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
    	factory.setBrokerURI("tcp://" + host + ":" + port);
    	Connection connection = factory.createConnection(user, password);
    	connection.start();
    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    	Destination dest = new StompJmsDestination(queue);
    	MessageProducer producer = session.createProducer(dest);
    	producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

    	System.out.println("Sending messages to " + queue + "...");
    	String data = libraryName+":"+isbn;
    	TextMessage msg = session.createTextMessage(data);
    	msg.setLongProperty("id", System.currentTimeMillis());
    	producer.send(msg);
    	connection.close();

    	}
    
@Override 
public void statusUpdate(Book book){
	Long isbn=book.getIsbn();
	List<Book> availablebooks=new ArrayList<Book>();
	availablebooks=getAllBooks();
	for(Book a: availablebooks){
		if(a.getIsbn()==isbn){
			book.setStatus(Book.Status.available);
			System.out.println("updated status for "+isbn);
		}
	}
	bookInMemoryMap.put(isbn, book);
	System.out.println("book added in map");
		
}
}
