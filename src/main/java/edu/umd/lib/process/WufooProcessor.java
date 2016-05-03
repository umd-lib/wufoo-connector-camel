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
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.umd.lib.services.SysAidConnector;

public class WufooProcessor implements Processor {

  private static Logger log = Logger.getLogger(WufooProcessor.class);
  private String handShakeKey;

  public WufooProcessor() {
    this.loadConfiguration("configuration.properties");
  }

  /*********************************************
   * process the request and parses the field names and field values.
   *
   * @return
   ***/
  @Override
  public void process(Exchange exchange) throws Exception {

    String message = exchange.getIn().getBody(String.class);
    Map<String, List<String>> parameters = getQueryParams(message);

    checkHandshake(parameters);
    Map<String, String> fields = getFields(parameters);
    JSONArray fieldsList = getFieldStructure(parameters.get("FieldStructure"), fields);
    HashMap<String, String> values = extractParameters(fieldsList);

    exchange.getOut().setBody("Thank you for the submission");
    log.info("Total Number of Parameters from the request:" + parameters.size());

    SysAidConnector sysaid = new SysAidConnector();
    sysaid.createServiceRequest(values);
  }

  /***********************************************
   * Created key value pair from the url string
   ****/
  public Map<String, List<String>> getQueryParams(String paramaters) {

    try {

      Map<String, List<String>> params = new HashMap<String, List<String>>();
      String query = paramaters;

      for (String param : query.split("&")) {
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

  /***********************************************
   * Checks for handshake key and validates if request is valid
   *
   * @throws CamelHandShakeException
   ****/
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

  /***********************************************
   * Get the fields and values
   ****/
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

  /***********************************************
   * Get the field Structure
   ***/
  public JSONArray getFieldStructure(List<String> fieldStructure, Map<String, String> fields) throws JSONException {

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

  /***********************************************
   * Method to print Hash maps
   ***/
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

  /***********************************************
   * Method extract parameters from WuFoo response
   ***/
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

  /****
   * Loading configuration File
   *
   * @param resourceName
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
   * Custom Exception to check if the request is valid request from Wufoo
   * 
   * @author rameshb
   *
   */
  class CamelHandShakeException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public CamelHandShakeException() {
    }

    // Constructor that accepts a message
    public CamelHandShakeException(String message) {
      super(message);
    }
  }

}
