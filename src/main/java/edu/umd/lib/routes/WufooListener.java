package edu.umd.lib.routes;

import org.apache.camel.spring.SpringRouteBuilder;

import edu.umd.lib.process.ExceptionProcessor;
import edu.umd.lib.process.SysAidProcessor;
import edu.umd.lib.process.WufooProcessor;

public class WufooListener extends SpringRouteBuilder {

  /**
   * Handshake key for security
   */
  private String handshake;

  /**
   * SysAid URL
   */
  private String sysaidwebformurl;

  /**
   * SysAid User id for login
   */
  private String sysaidaccountid;

  /**
   * SysAid Password for login
   */
  private String sysaidformid;

  /**
   * Map containing form mapping configuration
   */
  private String formmapping;

  /**
   * The domain of this route
   */
  private String domain = "{{default.domain}}";
  /**
   * The name of this route
   */
  private String name = "{{wufoo.routename}}";
  /**
   * The service name as defined in the respective properties file.
   */
  private String serviceName = "{{wufoo.servicename}}";

  /**
   * The endpoint exposed by Camel
   */
  private String endpoint = this.domain + this.name + "/" + this.serviceName;

  @Override
  public void configure() throws Exception {

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
    from("jetty:" + this.endpoint).streamCaching()
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
        .process(
            new SysAidProcessor(this.sysaidwebformurl, this.sysaidaccountid, this.sysaidformid, this.formmapping))
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

  public String getFormmapping() {
    return formmapping;
  }

  public void setFormmapping(String formmapping) {
    this.formmapping = formmapping;
  }

  public String getSysaidwebformurl() {
    return sysaidwebformurl;
  }

  public void setSysaidwebformurl(String sysaidwebformurl) {
    this.sysaidwebformurl = sysaidwebformurl;
  }

  public String getSysaidaccountid() {
    return sysaidaccountid;
  }

  public void setSysaidaccountid(String sysaidaccountid) {
    this.sysaidaccountid = sysaidaccountid;
  }

  public String getSysaidformid() {
    return sysaidformid;
  }

  public void setSysaidformid(String sysaidformid) {
    this.sysaidformid = sysaidformid;
  }

  public void setHandshake(String key) {
    this.handshake = key;
  }

  public String getHandshake() {
    return this.handshake;
  }

}
