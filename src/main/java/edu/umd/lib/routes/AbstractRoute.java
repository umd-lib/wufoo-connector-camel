package edu.umd.lib.routes;

import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.core.env.Environment;

public abstract class AbstractRoute extends SpringRouteBuilder {

  /**
   * The domain of this route
   */
  private String domain = "{{default.domain}}";
  /**
   * The name of this route
   */
  private String name = "{{default.routeName}}";
  /**
   * The endpoint exposed by Camel
   */
  private String endpoint;
  /**
   * The service name as defined in the respective properties file.
   */
  private String serviceName = "{{default.serviceName}}";

  @Override
  public void configure() throws Exception {

    this.createEndPoint();
    this.defineRoute();
  }

  private void createEndPoint() {
    this.endpoint = this.domain + this.name + "/" + this.serviceName;
  }

  /**
   * Defines a route REST based services have to specify.
   *
   * @throws Exception
   *           All thrown exceptions while executing the route
   */
  protected abstract void defineRoute() throws Exception;

  /**
   * Returns a String representation of the domain Camel should handle for this
   * route.
   *
   * @return The domain definition for this route
   */
  public String getDomain() {
    return this.domain;
  }

  /**
   * Sets the name of the route
   *
   * @param name
   *          The domain for this route
   */
  public void setDomain(String domain) {
    this.domain = domain;
  }

  /**
   * Returns a String representation of the endpoint Camel should handle for
   * this route.
   *
   * @return The endpoint definition for this route
   */
  public String getEndpoint() {
    return this.endpoint;
  }

  /**
   * Sets the name of the route
   *
   * @param name
   *          The new name for this route
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the name of this route.
   *
   * @return The name of this route
   */
  public String getName() {
    return this.name;
  }

  /**
   * Specifies the service name as set in the respective properties file which
   * is loaded through the injected {@link Environment}.
   *
   * @param serviceName
   *          The name of the servie this route will handle
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}
