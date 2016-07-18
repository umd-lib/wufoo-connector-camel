package edu.umd.lib.routes;

import org.apache.camel.LoggingLevel;

import edu.umd.lib.process.SysAidProcessor;
import edu.umd.lib.process.WufooProcessor;

public class WufooListener extends AbstractRoute {

  /**
   * Initializes a new instance of this class which defines a Camel route which
   * listens for incoming service invocations.
   */
  public WufooListener() {
    // sets the name of this bean
    this.setName("{{wufoo.routeName}}");
    // defines the service-name as set in the properties file
    this.setServiceName("{{wufoo.serviceName}}");

  }

  @Override
  protected void defineRoute() throws Exception {

    /**
     * A generic error handler (specific to this RouteBuilder)
     */

    onException(Exception.class)
        .routeId("ConnectionExceptionRoute")
        .handled(true)
        .log(LoggingLevel.ERROR, "Connection Error")
        .maximumRedeliveries("{{camel.maximum_tries}}")
        .redeliveryDelay("{{camel.redelivery_delay}}")
        .backOffMultiplier("{{camel.backOff_multiplier}}")
        .useExponentialBackOff()
        .maximumRedeliveryDelay("{{camel.maximum_redelivery_delay}}")
        .log(LoggingLevel.DEBUG, "Rolling back!")
        .to("direct:send_error_email");

    /**
     * Parse Request from WuFoo Web hooks and create hash map for SysAid Route
     */
    from("jetty:" + this.getEndpoint()).streamCaching()
        .routeId("WufooListener")
        .process(new WufooProcessor())
        .log("Wufoo Process Completed")
        .to("direct:connect.SysAid");

    /**
     * Connect to SysAid and create Service Request. All the Request to SysAid
     * is processed under one route Since SysAid works on Session
     */
    from("direct:connect.SysAid")
        .routeId("SysAidConnector")
        .process(new SysAidProcessor())
        .log("SysAid Request Created");

    /****
     * Send Email
     */
    from("direct:send_error_email")
        .routeId("SendErrorEmail")
        .log("Sending Email to SysAdmin")
        .setHeader("subject", simple("Error in Creating SysAid Ticket Even after {{camel.maximum_tries}} retries."))
        .setHeader("From", simple("{{email.from}}"))
        .setHeader("To", simple("{{email.to}}"))
        .to("{{email.uri}}");

  }

}
