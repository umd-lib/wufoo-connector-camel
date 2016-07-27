package edu.umd.lib.routes;

import edu.umd.lib.process.ExceptionProcessor;
import edu.umd.lib.process.SysAidProcessor;
import edu.umd.lib.process.WufooProcessor;

public class WufooListener extends AbstractRoute {

	private String handshake;

	public void setHandshake(String key) {
		this.handshake = key;
	}

	public String getHandshake() {
		return this.handshake;
	}

  /**
   * Initializes a new instance of this class which defines a Camel route that
   * listens for incoming service invocations.
   */
  public WufooListener() {
    // sets the name of this bean
    this.setName("{{wufoo.routename}}");
    // defines the service-name as set in the properties file
    this.setServiceName("{{wufoo.servicename}}");

  }

  @Override
  protected void defineRoute() throws Exception {

    /**
     * A generic error handler (specific to this RouteBuilder)
     */
    onException(Exception.class)
        .routeId("ExceptionRoute")
        .process(new ExceptionProcessor())
        .handled(true)
        .maximumRedeliveries("{{camel.maximum_tries}}")
        .redeliveryDelay("{{camel.redelivery_delay}}")
        .backOffMultiplier("{{camel.backoff_multiplier}}")
        .useExponentialBackOff()
        .maximumRedeliveryDelay("{{camel.maximum_redelivery_delay}}")
        .to("direct:send_error_email");

    /**
     * Parse Request from WuFoo Web hooks and create hash map for SysAid Route
     */
    from("jetty:" + this.getEndpoint()).streamCaching()
        .routeId("WufooListener")
        .process(new WufooProcessor(this.handshake))
        .log("Wufoo Request Processing Complete by Wufoo listener.")
        .to("direct:connect.sysaid");

    /**
     * Connect to SysAid and create Service Request. All the Request to SysAid
     * is processed under one route Since SysAid works on Session
     */
    from("direct:connect.sysaid")
        .routeId("SysAidConnector")
        .process(new SysAidProcessor())
        .log("Request to SysAid Completed by SysAid connector.");

    /****
     * Send Email
     */
    from("direct:send_error_email")
        .doTry()
        .routeId("SendErrorEmail")
        .log("processing a email to be sent using SendErrorEmail Route.")
        .setHeader("subject", simple(
            "Exception Occured in Wufoo-SysAid Integration, Total Number of Attempts: {{camel.maximum_tries}} retries."))
        .setHeader("From", simple("{{email.from}}"))
        .setHeader("To", simple("{{email.to}}"))
        .to("{{email.uri}}")
        .doCatch(Exception.class)
        .log("Error Occurred While Sending Email to specified to address.")
        .end();

  }

}
