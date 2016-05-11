package edu.umd.lib.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
  public WufooProcessor() {
    this.loadConfiguration("configuration.properties");
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
    Map<String, List<String>> parameters = getQueryParams(message);

    checkHandshake(parameters);
    Map<String, String> fields = getFields(parameters);
    JSONArray fieldsList = getFieldStructure(parameters.get("FieldStructure"), fields);
    HashMap<String, String> values = extractParameters(fieldsList);

    exchange.getOut().setBody(values);
    log.info("Total Number of Parameters from the request:" + parameters.size());

    // SysAidConnector sysaid = new SysAidConnector();
    // sysaid.createServiceRequest(values);
  }

  /***
   * From response parse the string to form hash map of parameters
   *
   * @param parameters
   *          from the WuFoo Request @return Hash map with parameter name as key
   *          and field value as value @exception
   */
  public Map<String, List<String>> getQueryParams(String queryString) {

    try {

      Map<String, List<String>> params = new HashMap<String, List<String>>();

      for (String param : queryString.split("&")) {
        String[] pair = param.split("=");
        String key = URLDecoder.decode(pair[0], "UTF-8");
        String value = "";
        if (pair.length > 1) {
          value = URLDecoder.decode(pair[1], "UTF-8");
        }
        List<String> values = params.get(key);
        if (values == null) {
          values = new ArrayList<String>();
          params.put(key, values);
        }
        values.add(value);
      }
      // printingMap(params);
      return params;

    } catch (UnsupportedEncodingException ex) {
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
  public void checkHandshake(Map<String, List<String>> parameters) throws CamelHandShakeException {

    String handshake = parameters.get("HandshakeKey").get(0);
    log.info("Wufoo handshake:" + handshake);
    log.info("camel handshake:" + handShakeKey);
    if (handshake == null) {
      throw new CamelHandShakeException("Wufoo Handshake key is empty.");
    } else if (this.handShakeKey == null) {
      throw new CamelHandShakeException("Camel Handshake key is empty.");
    } else if (!this.handShakeKey.equalsIgnoreCase(handshake)) {
      throw new CamelHandShakeException("Camel Handshake key and Wufoo Handshake key does not match.");
    }

  }

  /***
   * From the list of all parameters filter only the fields required for SysAid
   *
   * @param Map
   *          contains all fields
   * @return Map Contains only fields related to SysAid Ticket
   */
  public Map<String, String> getFields(Map<String, List<String>> parameters) {
    Map<String, String> fields = new HashMap<String, String>();
    Set<String> parameterNames = parameters.keySet();
    for (String name : parameterNames) {
      if (name.contains("Field")) {
        fields.put(name, parameters.get(name).get(0));
      }
    }
    /* Removes field structure from fields map */
    fields.remove("FieldStructure");
    return fields;
  }

  /***
   * WuFoo provides the field structure in each response. Use the field
   * Structure to construct what fields the form contains in the request
   *
   * @param List
   *          contains all fields
   * @param Map
   *          Contains fields that has values
   */
  public JSONArray getFieldStructure(List<String> fieldStructure,
      Map<String, String> fields) throws JSONException {

    JSONArray json = new JSONArray(fieldStructure);
    JSONArray fieldsList = new JSONArray();

    for (int m = 0; m < json.length(); m++) {
      JSONObject jsonObject = new JSONObject(json.get(m).toString());
      fieldsList = (JSONArray) jsonObject.get("Fields");
    }
    for (int i = 0; i < fieldsList.length(); i++) {
      JSONObject field = fieldsList.getJSONObject(i);

      if (field.has("SubFields")) {
        String combined_value = "";
        JSONArray subfields = (JSONArray) field.get("SubFields");
        for (int j = 0; j < subfields.length(); j++) {
          JSONObject subfield = subfields.getJSONObject(j);
          if (j == 0) {
            combined_value = fields.get(subfield.getString("ID"));
          } else {
            combined_value = combined_value + " " + fields.get(subfield.getString("ID"));
          }
        }
        field.put("Value", combined_value);
      } else {
        field.put("Value", fields.get(field.get("ID")));
      }

    }
    return fieldsList;
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
   * The JSON from WuFoo contains details about each field. Get the field name
   * and corresponding value from the JSON array and create a map with just
   * field name as key and corresponding value as value.
   *
   * @param map
   */
  public HashMap<String, String> extractParameters(JSONArray values) {

    HashMap<String, String> paramaters = new HashMap<String, String>();
    try {
      for (int i = 0; i < values.length(); i++) {
        JSONObject value = (JSONObject) values.get(i);
        paramaters.put(value.getString("Title"), value.getString("Value"));
      }
    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "execute POST request.", e);
    }
    return paramaters;
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
      log.error("IOException occured while attempting to "
          + "execute POST request. Authentication Failed ", e);
    }
  }

  /***
   * Custom Exception to check if the request is valid request from WuFoo
   */
  class CamelHandShakeException extends Exception {

    private static final long serialVersionUID = 1L;

    public CamelHandShakeException() {
    }

    // Constructor that accepts a message
    public CamelHandShakeException(String message) {
      super(message);
    }
  }

  /****
   * Convert fields in HasHmap to JSONArray
   *
   * @throws JSONException
   */
  public JSONArray convertMaptoJSON(Map<String, String> mp) throws JSONException {

    JSONArray fields = new JSONArray();
    Iterator<Entry<String, String>> it = mp.entrySet().iterator();
    while (it.hasNext()) {

      Entry<String, String> pair = it.next();

      JSONObject obj = new JSONObject();
      obj.put("value", pair.getValue());
      obj.put("key", pair.getKey());
      fields.put(obj);
    }
    return fields;
  }

}
