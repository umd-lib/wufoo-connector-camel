package edu.umd.lib.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SysAidConnector connects to SysAid using Login credentials from Configuration
 * file. The fields required to create service request is mapped to a
 * configuration file to map to SysAid field and then processed to create a
 * service request
 * <p>
 * SysAid requires session id in each request to validate the user. When
 * SysAidConnector class object is created session id is created by validating
 * the user and the session id is used for other request
 *
 * @since 1.0
 */
public class SysAidConnector {

  private Logger log = Logger.getLogger(SysAidConnector.class);

  private String sysaid_URL;
  private String sysaid_Username;
  private String sysaid_Password;
  private String session_id;

  Properties Config_properties = new Properties();
  HashMap<String, String> configuration = new HashMap<String, String>();

  private HashMap<String, HashMap<String, String>> dropdownList = new HashMap<String, HashMap<String, String>>();

  public String getSysaid_URL() {
    return sysaid_URL;
  }

  public void setSysaid_URL(String sysaid_URL) {
    this.sysaid_URL = sysaid_URL;
  }

  public String getSysaid_Username() {
    return sysaid_Username;
  }

  public void setSysaid_Username(String sysaid_Username) {
    this.sysaid_Username = sysaid_Username;
  }

  public String getSysaid_Password() {
    return sysaid_Password;
  }

  public void setSysaid_Password(String sysaid_Password) {
    this.sysaid_Password = sysaid_Password;
  }

  public String getSession_id() {
    return session_id;
  }

  public void setSession_id(String session_id) {
    this.session_id = session_id;
  }

  /***
   * Default Constructor, While creating an object the configuration for SysAid
   * is loaded to the object. Using the credentials from the configuration
   * settings user is validated. If validation fails custom exceptions is thrown
   * since further connection with SysAid is not possible.
   * <p>
   * After authenticating the user all the Drop down list from SysAid is
   * populated so that it can be used for further request
   *
   * @throws SysAidLoginException
   */

  public SysAidConnector() throws SysAidLoginException {
    this.loadConfiguration("edu.umd.lib.wufoo-connector-camel.cfg");
    this.authenticate();
    this.getAllDropDownList();

  }

  /***
   * Default Constructor, While creating an object the configuration for SysAid
   * is loaded to the object and the field mapping configuration is also loaded.
   * If a session is already created use the same session id instead of creating
   * a new session.
   * <p>
   * After authenticating the user all the drop down list from SysAid is
   * populated so that it can be used for further request
   *
   * @throws SysAidLoginException
   */
  public SysAidConnector(String session_id) {
    this.session_id = session_id;
    this.loadConfiguration("edu.umd.lib.wufoo-connector-camel.cfg");
    this.getAllDropDownList();
  }

  /***
   * Method to load the configuration Setting from Configuration file
   *
   * @param resourceName
   *          properties file name
   */
  public void loadConfiguration(String resourceName) {

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream resourceStream = null;

    try {

      resourceStream = loader.getResourceAsStream(resourceName);
      Config_properties.load(resourceStream);
      this.sysaid_URL = Config_properties.getProperty("SysAid.url");
      this.sysaid_Username = Config_properties.getProperty("SysAid.userName");
      this.sysaid_Password = Config_properties.getProperty("SysAid.password");

    } catch (IOException e) {
      log.error("IOException occured in Method : loadConfiguration of Class :SysAidConnector.java"
          + "  while attempting access Resource Stream ", e);
    } finally {
      try {
        resourceStream.close();
      } catch (IOException e) {
        log.error("IOException occured in Method : loadConfiguration of Class :SysAidConnector.java"
            + "  while attempting to close the Resource Stream ", e);
      }
    }
  }

  /***
   * Get Configuration Properties from the Loaded Configuration Properties
   * object, Property Name is passed as parameter and corresponding value is
   * returned
   *
   * @param PropertyName
   * @return
   */
  public String getConfigProperty(String PropertyName) {
    if (Config_properties.containsKey(PropertyName)) {
      return Config_properties.getProperty(PropertyName);
    } else {
      log.info("Property with Name: " + PropertyName
          + " not found in the Configuration file, Verify the Configuration File ");
      return "";
    }

  }

  /***
   * Load the mapping from SysAid and WuFoo forms in the properties file into
   * the map to find the mappings easily
   *
   * @param resourceName
   *          properties file name
   */
  public void LoadWufooSysaidMapping(String resourceName) {

    Properties properties = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream resourceStream = null;
    try {
      resourceStream = loader.getResourceAsStream(resourceName);
      properties.load(resourceStream);
      Set<Object> keys = properties.keySet();
      for (Object k : keys) {
        String key = (String) k;
        configuration.put(key, properties.getProperty(key));
      }

    } catch (IOException e) {
      log.error("IOException occured in Method : LoadwufooSysaidMapping of Class :SysAidConnector.java"
          + "  while attempting access Resource Stream ", e);
    } finally {
      try {
        resourceStream.close();
      } catch (IOException e) {
        log.error("IOException occured in Method : LoadwufooSysaidMapping of Class :SysAidConnector.java"
            + "  while attempting to close the Resource Stream ", e);
      }
    }
  }

  /***
   * Authenticate the user id with SysAid and get session id to be used for
   * further request process. If the authentication fails throw custom
   * SysAidlogin exception.
   *
   * @throws SysAidLoginException
   ***/
  public void authenticate() throws SysAidLoginException {

    try {

      JSONObject loginCredentials = new JSONObject();
      loginCredentials.put("user_name", this.sysaid_Username);
      loginCredentials.put("password", this.sysaid_Password);

      HttpResponse response = this.postRequest(loginCredentials, this.sysaid_URL + "login");
      HttpEntity entity = response.getEntity();
      String responseString = EntityUtils.toString(entity, "UTF-8");

      JSONObject json_result = new JSONObject(responseString);
      if (json_result.has("status") && json_result.getString("status").equalsIgnoreCase("401")) {
        throw new SysAidLoginException("Invalid User. Authentication Failed");
      }
      this.session_id = parseSessionID(response);

    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "execute POST request. Authentication Failed ", e);
    } catch (ParseException e) {
      log.error("ParseException occured while attempting to "
          + "execute POST request. Authentication Failed ", e);
    } catch (IOException e) {
      log.error("IOException occured while attempting to "
          + "execute POST request. Authentication Failed ", e);
    }
  }

  /***
   * Get Session ID from cookies from HTTP response. The session id is under
   * header parameter "Set-Cookie" with key JSESSIONID
   *
   * @param response
   */
  private String parseSessionID(HttpResponse response) {

    Header header = response.getFirstHeader("Set-Cookie");
    String value = header.getValue();
    if (value.contains("JSESSIONID")) {
      int index = value.indexOf("JSESSIONID=");
      int endIndex = value.indexOf(";", index);
      String sessionID = value.substring(
          index + "JSESSIONID=".length(), endIndex);
      if (sessionID != null) {
        return sessionID;
      }
    }
    return null;

  }

  /***
   * Create Service Request in SysAid using the value map sent from WuFoo. The
   * map is compared with the field mapping map and converted to the fields
   * SysAid expects and sent to SysAid as JSON info parameters
   *
   * @return ServiceRequest_ID
   */
  public String createServiceRequest(HashMap<String, String> values, String resourceName) {

    this.LoadWufooSysaidMapping(resourceName);

    try {

      JSONObject infoFields = new JSONObject();

      HashMap<String, String> fieldMappings = fieldMapping(values);
      JSONArray sysAidFieldsinfo = this.convertMaptoJSON(fieldMappings);

      infoFields.put("info", sysAidFieldsinfo);
      HttpResponse response = this.postRequest(infoFields, this.sysaid_URL + "/sr");
      HttpEntity entity = response.getEntity();

      String responseString = EntityUtils.toString(entity, "UTF-8");
      JSONObject json_result = new JSONObject(responseString);
      log.info("Service Request Created, ID:" + json_result.getString("id"));

      return json_result.getString("id");

    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "execute POST request.", e);
      return null;
    } catch (ParseException e) {
      log.error("ParseException occured while attempting to "
          + "execute POST request.", e);
      return null;
    } catch (IOException e) {
      log.error(
          "IOException occured while attempting to " + "execute POST request.", e);
      return null;
    }

  }

  /***
   * This method is used to post a request to the SySAid end point. Before
   * posting the request the session id from the object is added to the request.
   * This acts as access token for all the post request made to SysAid
   */
  public HttpResponse postRequest(JSONObject paramaters, String endpoint) {
    try {

      HttpPost httpPost = new HttpPost(endpoint);
      if (this.session_id != null) {
        httpPost.setHeader("Cookie", "JSESSIONID=" + this.session_id + "; Path=/; Secure; HttpOnly");
      }

      StringEntity entity = new StringEntity(paramaters.toString(), HTTP.UTF_8);
      entity.setContentType("application/json");
      httpPost.setEntity(entity);

      DefaultHttpClient client = new DefaultHttpClient();
      HttpResponse response = client.execute(httpPost);
      return response;

    } catch (UnsupportedEncodingException e) {
      log.error("UnsupportedEncodingException occured while attempting to "
          + "execute POST request. Ensure the coding used is proper ", e);
      return null;

    } catch (ClientProtocolException e) {
      log.error("ClientProtocolException occured while attempting to "
          + "execute POST request. Ensure this service is properly "
          + "configured and that the server you are attempting to make "
          + "a request to is currently running.", e);
      return null;

    } catch (IOException e) {
      log.error("IOException occured while attempting to "
          + "execute POST request.", e);
      e.printStackTrace();
      return null;

    }

  }

  /***
   * This method is used to get request to the SySAid end point. Before sending
   * the request the session id from the object is added to the request. This
   * acts as access token for all the post request made to SysAid
   */
  public HttpResponse getRequest(String endpoint) {

    try {

      HttpGet httpget = new HttpGet(endpoint);
      if (this.session_id != null) {
        httpget.setHeader("Cookie", "JSESSIONID=" + this.session_id + "; Path=/; Secure; HttpOnly");
      }

      DefaultHttpClient client = new DefaultHttpClient();
      HttpResponse response = client.execute(httpget);
      return response;

    } catch (UnsupportedEncodingException e) {
      log.error("UnsupportedEncodingException occured while attempting to "
          + "execute POST request. Ensure the coding used is proper ", e);
      return null;

    } catch (ClientProtocolException e) {
      log.error("ClientProtocolException occured while attempting to "
          + "execute POST request. Ensure this service is properly "
          + "configured and that the server you are attempting to make "
          + "a request to is currently running.", e);
      return null;

    } catch (IOException e) {
      log.error("IOException occured while attempting to "
          + "execute POST request.", e);
      return null;

    }

  }

  /***
   * Connect to SysAid and get all the List that is being used. SysAid expects
   * the numerical value for many fields and this list will be used to get the
   * numerical value for the corresponding text value for each field
   */
  public void getAllDropDownList() {
    HttpResponse response = this.getRequest(this.sysaid_URL + "list");
    HttpEntity entity = response.getEntity();
    String responseString;
    try {
      responseString = EntityUtils.toString(entity, "UTF-8");
      JSONArray list_response = new JSONArray(responseString);

      for (int i = 0; i < list_response.length(); i++) {

        JSONObject list = list_response.getJSONObject(i);
        JSONArray values = (JSONArray) list.get("values");

        HashMap<String, String> list_map = new HashMap<String, String>();
        for (int j = 0; j < values.length(); j++) {
          JSONObject value = values.getJSONObject(j);
          list_map.put(value.getString("caption"), value.getString("id"));
        }
        dropdownList.put(list.getString("id"), list_map);
      }
    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "execute POST request.", e);
    } catch (ParseException e) {
      log.error("ParseException occured while attempting to "
          + "execute POST request.", e);
    } catch (IOException e) {
      log.error("IOException occured while attempting to "
          + "execute POST request.", e);
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

  /**
   * Search the Hash map for a particular key under a list
   */
  public String getDropdownValues(String listKey, String key) {

    if (dropdownList.containsKey(listKey)) {
      HashMap<String, String> map = dropdownList.get(listKey);
      if (map.containsKey(key)) {
        return map.get(key);
      }
    }
    return "";
  }

  /**
   * Search the Hash map for a particular key under a list
   */
  public boolean isDropdownValueExist(String listKey, String value) {

    if (dropdownList.containsKey(listKey)) {
      HashMap<String, String> map = dropdownList.get(listKey);
      for (Map.Entry<String, ?> entry : map.entrySet()) {
        String Dropdown_value = (String) entry.getValue();// Value from WuFoo
        if (Dropdown_value.equalsIgnoreCase(value)) {
          return true;
        }
      }

    }
    return false;
  }

  /****
   * Mapping WuFoo fields with SysAid fields based on the mapping specified in
   * the mapping configuration.
   *
   * @param values
   * @return
   * @throws JSONException
   */
  public HashMap<String, String> fieldMapping(HashMap<String, String> values) throws JSONException {

    HashMap<String, String> finalValues = new HashMap<String, String>();

    String wufooFieldsProperty = configuration.get("wufoo.fields");
    String[] wufooFields = wufooFieldsProperty.split(",");

    String sysAidDefaultField = configuration.get("SysAid.DefaultField");

    // From the List of WuFoo Fields from mapping file,
    // Map the field to SysAid fields using the Mapping Configuration
    for (String wufooField : wufooFields) {

      String sysAidField = configuration.get(wufooField + ".fieldMapping");
      String sysAidFieldType = configuration.get(wufooField + ".fieldType");

      // If the SysAidFieldType is Drop down Loop find the actual value from the
      // list of drop down values already loaded
      if (sysAidFieldType.equalsIgnoreCase("Dropdown")) {

        if (dropdownList.containsKey(sysAidField)) {
          String dropdownValue = getDropdownValues(sysAidField, values.get(wufooField));
          finalValues.put(sysAidField, dropdownValue);
        }

        // If the SysAidFieldType is User Drop down Loop find the actual value
        // by passing the field key to the List of User loaded into cache to get
        // the User ID
      } else if (sysAidFieldType.equalsIgnoreCase("UserDropdown")) {

        String wufoofieldKey = configuration.get(wufooField + ".fieldKey");
        JSONObject userObject = SysAidUsers.getInstance().getUserbyKey(wufoofieldKey, values.get(wufooField));

        if (userObject != null) {
          finalValues.put(sysAidField, userObject.getString("id"));
        }

      } else {
        // If the SysAidFieldType is Text Check if the field is a Default field.
        // If its a default field append the values, if not overwrite the values
        // in the field.
        if (sysAidDefaultField.equalsIgnoreCase(sysAidField)) {

          if (finalValues.containsKey(sysAidField)) {

            String current_value = finalValues.get(sysAidField);
            String value = wufooField + " : " + values.get(wufooField);
            current_value = current_value + "\n" + value;
            finalValues.put(sysAidField, current_value);

          } else {
            String value = wufooField + " : " + values.get(wufooField);
            finalValues.put(sysAidField, value);
          }

        } else {
          finalValues.put(sysAidField, values.get(wufooField));
        }

      }
    }

    // From the List of SysAid Fields from mapping file
    // Verify and Populate defaults specified in the Mapping file
    String sysAidFieldsProperty = configuration.get("SysAid.fields");
    String[] sysAidFields = sysAidFieldsProperty.split(",");

    for (String sysAidField : sysAidFields) {

      // If the SysAid Field already has mapping value check if the mapping
      // value is not empty if empty populate the field with default value
      // specified
      if (finalValues.containsKey(sysAidField)) {

        if (finalValues.get(sysAidField).toString().equalsIgnoreCase("")) {
          if (configuration.containsKey("SysAid.Defaults." + sysAidField)) {
            finalValues.put(sysAidField, configuration.get("SysAid.Defaults." + sysAidField));
          }
        }

        // If the SysAid field is not mapped use the defaults value to map to
        // the sysAid field
      } else {

        if (configuration.containsKey("SysAid.Defaults." + sysAidField)) {
          finalValues.put(sysAidField, configuration.get("SysAid.Defaults." + sysAidField));
        }
      }

    }

    // Return the Mapped SysAid field and Values
    return finalValues;

  }

  /***
   * Custom Exception when SysAid Authentication Fails
   */
  class SysAidLoginException extends Exception {
    private static final long serialVersionUID = 1L;

    public SysAidLoginException() {
    }

    public SysAidLoginException(String message) {
      super(message);
    }
  }

  /***
   * Main method for testing the application
   *
   * @param args
   */
  public static void main(String args[]) {

    try {
      SysAidConnector sysaid = new SysAidConnector();
      sysaid.getAllDropDownList();
      // sysaid.createServiceRequest();
    } catch (SysAidLoginException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /***
   * Dummy Data for testing purpose
   */
  public HashMap<String, String> testData() {

    HashMap<String, String> fields = new HashMap<String, String>();
    fields.put("due_date", "1461384000000");
    fields.put("status", "5");
    fields.put("priority", "1");
    fields.put("description", "This is created from Rest API");
    fields.put("responsibility", "1222");
    fields.put("request_user", "1222");
    fields.put("title", " created from Rest api");
    return fields;
  }

  /**
   * Method to print values in hash map for debugging
   *
   * @param parameters
   */
  @SuppressWarnings("unchecked")
  public void printingMap(Map<String, ?> parameters) {

    for (Map.Entry<String, ?> entry : parameters.entrySet()) {
      if (entry.getValue() instanceof List<?>) {
        String value = "";
        for (int i = 0; i < ((Map<String, List<String>>) entry.getValue()).size(); i++) {
          value = value + ((List<String>) entry.getValue()).get(i);
        }
        log.info(entry.getKey() + ":" + value);
      } else {
        log.info(entry.getKey() + ":" + entry.getValue());
      }

    }
  }

}
