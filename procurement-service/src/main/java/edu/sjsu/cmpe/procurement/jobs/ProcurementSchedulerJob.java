package edu.sjsu.cmpe.procurement.jobs;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;

/**
 * This job will run at every 5 second.
 */
@Every("5s")
public class ProcurementSchedulerJob extends Job {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void doJob() {
    	
    	ProcurementService ps=new ProcurementService();
    	List<Long> isbnids=new ArrayList<Long>();
    	try {
    		isbnids=ps.consumers();
    		
    	} catch (JMSException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    if(!isbnids.isEmpty()){
    	
    	ps.postHttp(isbnids);
    	ps.getHttp();
    }
    String strResponse = ProcurementService.jerseyClient.resource(
		"http://ip.jsontest.com/").get(String.class);
	log.debug("Response from jsontest.com: {}", strResponse);
	
	
    }
}
