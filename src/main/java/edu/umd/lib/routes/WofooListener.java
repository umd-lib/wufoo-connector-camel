package edu.umd.lib.routes;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import edu.umd.lib.services.WofooListenerImpl;

public class WofooListener extends AbstractRoute {

  /**
   * Initializes a new instance of this class which defines a Camel route which
   * listens for incoming service invocations.
   */
  public WofooListener() {
    // sets the name of this bean
    this.setName("wofoo");
    // defines the service-name as set in the properties file
    this.setServiceName("wofoo-listener");

  }

  @Override
  protected void defineRoute() throws Exception {
    from("jetty:" + this.getEndpoint()).streamCaching().process(
        new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            WofooListenerImpl wofooProcessor = new WofooListenerImpl();
            wofooProcessor.processRequest(exchange);
          }
        });
  }

}
