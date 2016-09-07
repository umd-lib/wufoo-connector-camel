package edu.umd.lib.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
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
import edu.umd.lib.exception.SysAidConnectorException;

/**
 * SysAidConnector connects to SysAid using Login credentials from Configuration file. The fields required to create
 * service request is mapped to a configuration file to map to SysAid field and then processed to create a service
 * request
 * <p>
 * SysAid requires session id in each request to validate the user. When SysAidConnector class object is created session
 * id is created by validating the user and the session id is used for other request
 * 
 * @since 1.0
 */
public class SysAidConnector {

  private final Logger log = Logger.getLogger(SysAidConnector.class);

  private final String FIELD_MAPPING_SUFFIX = ".fieldmapping";
  private final String INCLUDE_LABEL_INFIX = ".includelabel";
  private final String SYSAID_DEFAULTS_PREFIX = "sysaid.defaults";
  private final String SYSAID_JOINER_PREFIX = "sysaid.joiner.";
  private final String SYSAID_DESC_SUFFIX = "sysaid.desc.suffix";
  private final String SYSAID_TITLE_PREFIX = "sysaid.title.prefix";
  private final String SYSAID_DEFAULT_JOINER = " ";
  private final String TITLE_FIELD = "title";
  private final String DESC_FIELD = "desc";

  private final String sysaid_formID;
  private final String sysaid_accountID;
  private final String sysaid_webformurl;

  private PropertiesConfiguration configuration;

  public SysAidConnector(String url, String accountid, String formID) {
    this.sysaid_webformurl = url;
    this.sysaid_accountID = accountid;
    this.sysaid_formID = formID;
  }

  /***
   * Load the mapping from SysAid and WuFoo forms in the properties file into the map to find the mappings easily
   * 
   * @param resourceName
   *          properties file name
   * @throws FormMappingException
   */
  public void LoadWufooSysaidMapping(String resource) throws FormMappingException {
    try {
      this.configuration = new PropertiesConfiguration();
      this.configuration.load(new FileInputStream(resource));
    } catch (IOException | ConfigurationException e) {

      log.error("IOException occured in Method : LoadwufooSysaidMapping of Class :SysAidConnector.java"
          + "  while attempting access Resource Stream ", e);
      throw new FormMappingException("Mapping file :" + resource + " Not found.");
    }
  }

  /***
   * Create Service Request in SysAid using the value map sent from WuFoo. The map is compared with the field mapping
   * map
   * 
   * @return ServiceRequest_ID
   * @throws FormMappingException
   * @throws SysAidConnectorException
   */
  public void createServiceRequest(HashMap<String, String> values, String resource)
      throws FormMappingException, SysAidConnectorException {

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
    } catch (IllegalStateException e) {
      log.error("IllegalStateException occured while attempting to"
          + " create service request. Method:createServiceRequest", e);
    }

  }

  /**
   * Returns either the value or the concatenated "label: value" based on the presence of the INCLUDE_LABEL_INFIX in the
   * fieldmapping key.
   * 
   * @param wufooValues
   *          - HashMap containing the key-values from wufoo form submission
   * @param key
   *          - The key whose formatted value to be returned
   * @return
   */
  private String getFormattedWufooValue(HashMap<String, String> wufooValues, String key) {
    int wufooKeyLength = key.length() - FIELD_MAPPING_SUFFIX.length();
    boolean includeLabel = key.contains(INCLUDE_LABEL_INFIX);
    if (includeLabel) {
      wufooKeyLength = wufooKeyLength - INCLUDE_LABEL_INFIX.length();
    }
    String wufooKey = key.substring(0, wufooKeyLength);
    String wufooValue = wufooValues.get(wufooKey);
    if (wufooValue != null && !wufooValue.isEmpty()) {
      if (includeLabel) {
        return wufooKey + ": " + wufooValue;
      }
      return wufooValue;
    } else {
      return "";
    }
  }

  /**
   * Concatenated value of the current and new sysaid value joined using either the default or the configured joiner
   * string.
   * 
   * @param sysaidKey
   *          - Key for which joiner configuration needs to checked.
   * @param currentValue
   * @param newValue
   * @return
   */
  private String getJoinedSysaidValue(String sysaidKey, String currentValue, String newValue) {
    String joiner = configuration.getString(SYSAID_JOINER_PREFIX + sysaidKey, SYSAID_DEFAULT_JOINER);
    return currentValue + joiner + newValue;
  }

  /**
   * Add or append value based on existence of value for the key in the given map.
   * 
   * @param map
   * @param key
   * @param value
   * @return
   */
  private HashMap<String, String> addOrAppend(HashMap<String, String> map, String key, String value) {
    if (map.containsKey(key)) {
      String currentValue = map.get(key);
      map.put(key, getJoinedSysaidValue(key, currentValue, value));
    } else {
      map.put(key, value);
    }
    return map;
  }

  /****
   * Mapping WuFoo fields with SysAid fields based on the mapping specified in the mapping configuration and default
   * values configuration.
   * 
   * @param values
   * @return
   * @throws JSONException
   */
  public HashMap<String, String> fieldMapping(HashMap<String, String> wufooValues) throws JSONException {

    HashMap<String, String> sysaidValues = new HashMap<String, String>();

    if (configuration.containsKey(SYSAID_TITLE_PREFIX)) {
      String titlePrefix = configuration.getString(SYSAID_TITLE_PREFIX, "");
      if (!titlePrefix.isEmpty()) {
        sysaidValues.put(TITLE_FIELD, titlePrefix);
      }
    }

    // Populate values from the wufoo form submission
    Iterator<String> mappingKeys = configuration.getKeys();
    while (mappingKeys.hasNext()) {
      String key = mappingKeys.next();
      if (key.endsWith(FIELD_MAPPING_SUFFIX)) {
        String wufooValue = getFormattedWufooValue(wufooValues, key);
        if (!wufooValue.isEmpty()) {
          // wufoo key => sysaidKey1,sysaidKey2,sysaidKey3...
          for (String sysaidKey : configuration.getStringArray(key)) {
            addOrAppend(sysaidValues, sysaidKey, wufooValue);
          }
        }
      }
    }

    // Populate default values
    Iterator<String> sysaidDefaultsKeys = configuration.getKeys(SYSAID_DEFAULTS_PREFIX);
    while (sysaidDefaultsKeys.hasNext()) {
      String defaultKey = sysaidDefaultsKeys.next();
      String key = defaultKey.substring(SYSAID_DEFAULTS_PREFIX.length() + 1);
      if (sysaidValues.containsKey(key)) {
        // Sysaid field already set from a wufoo form value
        // Don't need to assign the default value
        continue;
      }
      String defaultValue = configuration.getString(defaultKey, "");
      if (!defaultValue.isEmpty()) {
        sysaidValues.put(key, defaultValue);
      }
    }

    if (configuration.containsKey(SYSAID_DESC_SUFFIX)) {
      String descSuffix = configuration.getString(SYSAID_DESC_SUFFIX, "");
      if (!descSuffix.isEmpty()) {
        addOrAppend(sysaidValues, DESC_FIELD, descSuffix);
      }
    }

    // Return the Mapped SysAid field and Values
    return sysaidValues;

  }

  /****
   * Extract Fields for SysAid web form Format. Constructs a list of parameters from a Request element. These parameters
   * follow the format used by SysAid to submit web forms and can be used to create an UrlEncodedFormEntity.
   * 
   * @param fieldMappings
   * @return
   * @throws SysAidConnectorException
   */
  protected List<NameValuePair> extractFields_SysAid(HashMap<String, String> fieldMappings)
      throws SysAidConnectorException {

    List<NameValuePair> fields = new ArrayList<NameValuePair>();

    if (this.sysaid_accountID == null || this.sysaid_accountID.equals("")) {
      throw new SysAidConnectorException(
          "SysAid Account ID information not found. Please verify wufooConnector configuration");
    }
    if (this.sysaid_formID == null || this.sysaid_formID.equals("")) {
      throw new SysAidConnectorException(
          "SysAid Form ID information not found. Please verify wufooConnector configuration");
    }
    if (this.sysaid_webformurl == null || this.sysaid_webformurl.equals("")) {
      throw new SysAidConnectorException(
          "SysAid Webform url not found. Please verify wufooConnector configuration");
    }

    fields.add(new BasicNameValuePair("accountID", sysaid_accountID));
    fields.add(new BasicNameValuePair("formID", sysaid_formID));

    for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
      fields.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
    }
    return fields;

  }

}
