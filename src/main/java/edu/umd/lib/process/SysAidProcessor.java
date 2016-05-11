package edu.umd.lib.process;


import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import edu.umd.lib.services.SysAidConnector;

/**
 * SysAidProcessor connects to SysAid using Login credentials from Configuration
 * file. The fields required to create service request is mapped to a
 * configuration file to map to SysAid field and then processed to create a
 * service request
 * <p>
 * @since 1.0
 */
public class SysAidProcessor implements Processor {
	
    private static Logger log = Logger.getLogger(SysAidProcessor.class);

 
  @SuppressWarnings("unchecked")
  @Override
  public void process(Exchange exchange) throws Exception {

	  log.info("Processing a request to SysAid");
	  HashMap<String, String> message = exchange.getIn().getBody(HashMap.class);
	  SysAidConnector sysaid = new SysAidConnector();
	  sysaid.createServiceRequest(message);
  }

}
