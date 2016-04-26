package edu.umd.lib.routes;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import edu.umd.lib.services.SysAidConnector;
import edu.umd.lib.services.WufooListenerImpl;

public class WufooListener extends AbstractRoute {

  /**
   * Initializes a new instance of this class which defines a Camel route which
   * listens for incoming service invocations.
   */
  public WufooListener() {
    // sets the name of this bean
    this.setName("wufoo");
    // defines the service-name as set in the properties file
    this.setServiceName("wufoo-listener");

  }

  @Override
  protected void defineRoute() throws Exception {
    from("jetty:" + this.getEndpoint()).streamCaching().process(
        new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {

            WufooListenerImpl wufooProcessor = new WufooListenerImpl();
            HashMap<String, String> values = wufooProcessor.processRequest(exchange);

            SysAidConnector sysaid = new SysAidConnector();
            sysaid.createServiceRequest(values);
          }
        });
  }

}
