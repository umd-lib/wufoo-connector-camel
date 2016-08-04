package edu.umd.lib.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONException;

import edu.umd.lib.exception.FormMappingException;

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

  private String sysaid_formID;
  private String sysaid_accountID;
  private String sysaid_webformurl;

  HashMap<String, String> configuration = new HashMap<String, String>();

  public SysAidConnector(String url, String accountid, String formID) {
    this.sysaid_webformurl = url;
    this.sysaid_accountID = accountid;
    this.sysaid_formID = formID;
  }

  /***
   * Load the mapping from SysAid and WuFoo forms in the properties file into
   * the map to find the mappings easily
   *
   * @param resourceName
   *          properties file name
   * @throws FormMappingException
   */
  public void LoadWufooSysaidMapping(String resource) throws FormMappingException {

    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(resource));
      Set<Object> keys = properties.keySet();
      for (Object k : keys) {
        String key = (String) k;
        configuration.put(key, properties.getProperty(key));
      }
    } catch (IOException e) {

      log.error("IOException occured in Method : LoadwufooSysaidMapping of Class :SysAidConnector.java"
          + "  while attempting access Resource Stream ", e);
      throw new FormMappingException("Mapping file :" + resource + " Not found.");
    }
  }

  /***
   * Create Service Request in SysAid using the value map sent from WuFoo. The
   * map is compared with the field mapping map
   *
   * @return ServiceRequest_ID
   * @throws FormMappingException
   */
  public void createServiceRequest(HashMap<String, String> values, String resource) throws FormMappingException {

    this.LoadWufooSysaidMapping(resource);

    try {

      DefaultHttpClient client = new DefaultHttpClient();
      HashMap<String, String> fieldMappings = fieldMapping(values);
      List<NameValuePair> fields = extractFields_SysAid(fieldMappings);

      HttpPost httpPost = new HttpPost(this.sysaid_webformurl);
      httpPost.setEntity(new UrlEncodedFormEntity(fields, "UTF-8"));
      HttpResponse response = client.execute(httpPost);

      if (response != null) {
        log.info("Response: \n" + response.toString());
      } else {
        log.info("Unable to execute POST request.\nRequest parameters: \n"
            + fields);
      }
    } catch (ParseException e) {
      log.error("ParseException occured while attempting to "
          + "create service request. Method:createServiceRequest.", e);
    } catch (IOException e) {
      log.error("IOException occured while attempting to"
          + " create service request. Method:createServiceRequest", e);
    } catch (JSONException e) {
      log.error("JSONException occured while attempting to"
          + " create service request. Method:createServiceRequest", e);
    }

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

    String sysAidDefaultField = configuration.get("sysaid.defaultfield");

    // From the List of WuFoo Fields from mapping file,
    // Map the field to SysAid fields using the Mapping Configuration
    for (String wufooField : wufooFields) {

      String sysAidField = configuration.get(wufooField + ".fieldmapping");

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

    // From the List of SysAid Fields from mapping file
    // Verify and Populate defaults specified in the Mapping file
    String sysAidFieldsProperty = configuration.get("sysaid.fields");
    String[] sysAidFields = sysAidFieldsProperty.split(",");

    for (String sysAidField : sysAidFields) {

      // If the SysAid Field already has mapping value check if the mapping
      // value is not empty if empty populate the field with default value
      // specified
      if (finalValues.containsKey(sysAidField)) {

        if (finalValues.get(sysAidField).toString().equalsIgnoreCase("")) {
          if (configuration.containsKey("sysaid.defaults." + sysAidField)) {
            finalValues.put(sysAidField, configuration.get("sysaid.defaults." + sysAidField));
          }
        }

        // If the SysAid field is not mapped use the defaults value to map to
        // the sysAid field
      } else {

        if (configuration.containsKey("sysaid.defaults." + sysAidField)) {
          finalValues.put(sysAidField, configuration.get("sysaid.defaults." + sysAidField));
        }
      }

    }

    // Return the Mapped SysAid field and Values
    return finalValues;

  }

  /****
   * Extract Fields for SysAid web form Format. Constructs a list of parameters
   * from a Request element. These parameters follow the format used by SysAid
   * to submit web forms and can be used to create an UrlEncodedFormEntity.
   *
   * @param fieldMappings
   * @return
   */
  protected List<NameValuePair> extractFields_SysAid(HashMap<String, String> fieldMappings) {

    List<NameValuePair> fields = new ArrayList<NameValuePair>();
    fields.add(new BasicNameValuePair("accountID", sysaid_accountID));
    fields.add(new BasicNameValuePair("formID", sysaid_formID));

    for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
      fields.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
    }
    return fields;

  }

}
