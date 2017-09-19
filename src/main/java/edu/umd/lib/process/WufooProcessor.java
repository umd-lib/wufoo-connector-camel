package edu.umd.lib.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.umd.lib.exception.CamelHandShakeException;

/**
 * WufooProcessor process the request from WuFoo by parsing the request then
 * creating a Hash map of fields and values from the form. The map is then
 * passed to SysAidConnector for creating a request in SysAid
 * <p>
 * Before the WuFoo request is processed the request should be validated to make
 * sure the request is not a spam request. This is done by comparing the
 * handshake key from WuFoo and configuration file. If the handshake key does
 * not match a custom exception is thrown.
 *
 * @since 1.0
 */
public class WufooProcessor implements Processor {

  private String handShakeKey;

  private static Logger log = Logger.getLogger(WufooProcessor.class);

  /***
   * Load the configuration file while creating the object and populate the
   * handshake key from the properties file
   */
  public WufooProcessor(String handshakeKey) {
    this.handShakeKey = handshakeKey;
    log.info("Setting handshake key: " + this.handShakeKey);
  }

  /***
   * Process method is the main method of WufooProcessor. The Exchange from the
   * route is parsed and parameters from WuFoo form is created as Hash map. The
   * hash map created is used to create SysAid Ticket
   *
   * @exception CamelHandshakeKeyException
   * @exception SysAidLoginException
   */
  @Override
  public void process(Exchange exchange) throws Exception {

    String message = exchange.getIn().getBody(String.class);

    Map<String, String> parameters = getQueryParams(message);
    checkHandshake(parameters);
    String formName = getHashvalue(parameters.get("FormStructure"));
    parameters.remove("FieldStructure");
    ArrayList<String> nonFieldKeys = new ArrayList<String>();
    for (String key : parameters.keySet()) {
      if(!key.startsWith("Field")) {
        nonFieldKeys.add(key);
      }
    }
    parameters.keySet().removeAll(nonFieldKeys);
    parameters.put("Hash", formName);
    exchange.getOut().setBody(parameters);

  }

  /****
   * Get Form Name from the Wufoo Request
   *
   * @param formStructureString
   * @return
   */
  public String getHashvalue(String formStructureString) {
    try {
      JSONObject formStructure = new JSONObject(formStructureString);
      return formStructure.getString("Hash");
    } catch (JSONException e) {
      log.error("JSONException occured while extracting form Hash value from Form Structure " +
          ".", e);
    }
    return "";
  }

  /***
   * From response parse the string to form hash map of parameters
   *
   * @param parameters
   *          from the WuFoo Request @return Hash map with parameter name as key
   *          and field value as value @exception
   */
  public Map<String, String> getQueryParams(String queryString) {

    try {

      Map<String, String> params = new HashMap<String, String>();

      for (String param : queryString.split("&")) {
        String[] pair = param.split("=");
        String key = URLDecoder.decode(pair[0], "UTF-8");
        String value = "";
        if (pair.length > 1) {
          value = URLDecoder.decode(pair[1], "UTF-8");
        }
        String previousValue = params.get(key);
        if (previousValue != null) {
          value = previousValue + ", " + value;
        }
        params.put(key, value);
      }
      // printingMap(params);
      return params;

    } catch (UnsupportedEncodingException ex) {
      log.error("UnsupportedEncodingException occured while parsing query parameters " +
          ".", ex);
      throw new AssertionError(ex);
    }
  }

  /***
   * Compare handshake key from WuFoo and Camel and throw exception if the key
   * does not match
   *
   * @param map
   *          Contains all the fields from request from the WuFoo Request
   * @exception CamelHandShakeException
   */
  public void checkHandshake(Map<String, String> parameters) throws CamelHandShakeException {

    String handshake = parameters.get("HandshakeKey");
    if (handshake == null) {
      throw new CamelHandShakeException("Wufoo Handshake key is empty.");
    } else if (this.handShakeKey == null) {
      throw new CamelHandShakeException("Camel Handshake key is empty.");
    } else if (!this.handShakeKey.equalsIgnoreCase(handshake)) {
      throw new CamelHandShakeException("Camel Handshake key and Wufoo Handshake key does not match.");
    } else {
      log.info("Wufoo handshake and Camel HandShake Key Matches");
    }

  }

  /***
   * Utility function to print maps, Loop through each key set and value and
   * print the contain. If the value is a collection again the collection is
   * converted into a single string and the key value pair is printed
   *
   * @param map
   */
  public void printingMap(Map<String, ?> parameters) {

    for (Map.Entry<String, ?> entry : parameters.entrySet()) {
      if (entry.getValue() instanceof List<?>) {

        List<?> list = (List<?>) entry.getValue();
        String value = "";
        for (int i = 0; i < list.size(); i++) {
          value = value + list.get(i);
        }
        log.info(entry.getKey() + ":" + value);
      } else {
        log.info(entry.getKey() + ":" + entry.getValue());
      }

    }
  }

  /***
   * Method to load the configuration from properties file
   *
   * @param resourceName
   *          properties file name
   */
  public void loadConfiguration(String resourceName) {

    Properties properties = new Properties();

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try (InputStream resourceStream = loader.getResourceAsStream(resourceName)) {

      properties.load(resourceStream);
      this.handShakeKey = properties.getProperty("wufoo.handshake_key");

    } catch (IOException e) {
      log.error("IOException occured while accessing the configuration file " +
          resourceName + ".", e);
    }
  }
}
