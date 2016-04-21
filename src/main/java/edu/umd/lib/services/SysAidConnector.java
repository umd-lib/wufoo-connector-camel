package edu.umd.lib.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

public class SysAidConnector {

  private static Logger log = Logger.getLogger(SysAidConnector.class);

  /*** Read from properties file ***/
  private String sysaid_URL = "https://libticketingdev.umd.edu/api/v1/";
  private String sysaid_Username = "*******";
  private String sysaid_Password = "*******";
  private String session_id;

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
   * If session_id is already present pass information to skip authentication
   * process
   ***/
  public SysAidConnector(String session_id) {
    this.session_id = session_id;
  }

  /*** Authenticate the user while creating object ***/
  public SysAidConnector() {
    this.authenticate();
  }

  /***
   * Authenticate the user id with SysAid and get session id to be used for
   * further request process
   ***/
  public void authenticate() {

    try {

      JSONObject loginCredentials = new JSONObject();
      loginCredentials.put("user_name", this.sysaid_Username);
      loginCredentials.put("password", this.sysaid_Password);

      HttpResponse response = this.postRequest(loginCredentials, this.sysaid_URL + "login");
      this.session_id = parseSessionID(response);

      HttpEntity entity = response.getEntity();
      String responseString = EntityUtils.toString(entity, "UTF-8");
      log.info("Result" + responseString);

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
   * Get Session ID from cookies from HTTP response
   *
   * @param response
   * @return
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
   * Dummy Data
   *
   * @return
   */
  public HashMap<String, String> testData() {

    HashMap<String, String> fields = new HashMap<String, String>();
    fields.put("due_date", "1461384000000");
    fields.put("status", "5");
    fields.put("priority", "1");
    fields.put("description", "This is created from Rest API");
    fields.put("cust_notes", " Notes from CUST");
    fields.put("change_category", "1");
    fields.put("responsibility", "1222");
    fields.put("cust_text1", "Cust text");
    fields.put("request_user", "1222");
    fields.put("resolution", "hello Res");
    fields.put("title", " created from Rest api");
    fields.put("followup_user", "1199");
    fields.put("cust_list1", "14");
    fields.put("problem_sub_type", "3D Print Request");

    return fields;
  }

  /***
   * Create Service Request in SYSaid with following information
   *
   * @return
   */
  public String createServiceRequest() {

    try {
      JSONObject infoFields = new JSONObject();
      infoFields.put("info", this.convertMaptoJSON(testData()));

      HttpResponse response = this.postRequest(infoFields, this.sysaid_URL + "/sr");
      HttpEntity entity = response.getEntity();
      String responseString = EntityUtils.toString(entity, "UTF-8");

      JSONObject json_result = new JSONObject(responseString);

      log.info("ID:" + json_result.getString("id"));
      System.out.println("ID:" + json_result.getString("id"));
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
      log.error("IOException occured while attempting to "
          + "execute POST request.", e);
      return null;
    }

  }

  /***
   * HTTP post request method if session id already present include in the
   * request
   ***/
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
   * HTTP post request method if session id already present include in the
   * request
   ***/
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
      e.printStackTrace();
      return null;

    }

  }

  /***
   * Get all List SysAid API and store for further processing
   *
   * @throws JSONException
   */
  public void getAllList() throws JSONException {
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
      log.info("Map >>>: " + dropdownList.size());
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
   * @return
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

  /***
   * Main method for testing the application
   *
   * @param args
   */
  public static void main(String args[]) {

    try {
      SysAidConnector sysaid = new SysAidConnector();
      sysaid.getAllList();
      // sysaid.createServiceRequest();

      log.info(sysaid.getDropdownValues("responsibility", "Ben Wallberg"));
    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "execute POST request.", e);
    }

  }

  /**
   * Method to print values in hash map for debugging
   *
   * @param parameters
   */
  @SuppressWarnings("unchecked")
  public void printingMap(Map<String, ?> parameters) {

    log.info("Printing");
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

  /**
   * Search the Hash map for a particular key under a list
   *
   * @param parameters
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

}
