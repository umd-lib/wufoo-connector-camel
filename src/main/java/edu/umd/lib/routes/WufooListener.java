package edu.umd.lib.routes;

import org.apache.camel.Exchange;

import edu.umd.lib.process.WufooProcessor;

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
    from("jetty:" + this.getEndpoint()).streamCaching()
        .routeId("WufooListener")// Load from properties file
        .process(new WufooProcessor())
        .onException(Exception.class)
        .maximumRedeliveries("3")// Load from properties file
        .log("Index Routing Error: WufooListener")
        .handled(true)
        .transform(constant("Something went wrong"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500));
  }

}
