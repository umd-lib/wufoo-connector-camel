package edu.umd.lib.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.stringtemplate.v4.ST;

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

  private final String SYSAID_FIELD_PREFIX = "sysaid.";

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
    HashMap<String, String> nonEmptyArgs = new HashMap<String, String>();
    List<String> blankFields = new ArrayList<String>();

    for (String key : wufooValues.keySet()) {
      if (wufooValues.get(key).isEmpty()) {
        blankFields.add(key);
      } else {
        nonEmptyArgs.put(key, wufooValues.get(key));
      }
    }

    // Populate values from the wufoo form submission
    Iterator<String> mappingKeys = configuration.getKeys();
    while (mappingKeys.hasNext()) {
      String key = mappingKeys.next();
      if (key.startsWith(SYSAID_FIELD_PREFIX)) {
        String wufooValue = renderedTemplate(configuration.getString(key), nonEmptyArgs, blankFields);
        if (!wufooValue.isEmpty()) {
          sysaidValues.put(key.substring(SYSAID_FIELD_PREFIX.length()), wufooValue);
        }
      }
    }
    // Return the Mapped SysAid field and Values
    return sysaidValues;
  }

  /**
   * Render the template using the StringTemplate library. This replace the <FieldID> values
   * in the template with the corresponding value from the args map. The <FieldID> of the
   * arguments with empty values will be stripped from the template string prior to the rendering
   * to correctly remove the punctuations associated with those <FieldID> without values.
   * 
   * @param template - The template string.
   * @param args - The parameters to render the template.
   * @param emptyFields - Fields (and associated punctuations) to be stripped from the template.
   * 
   * @return Rendered template string.
   */
  protected String renderedTemplate(String template, Map<String, String> args, List<String> emptyFields) {
    log.debug("Template string: " + template);
    if (!template.contains("<Field")) {
      // No fields to be rendered so return template as the rendered string.
      return template;
    }
    for (String field : emptyFields) {
      // Remove Fields (and associated punctuations) that does not have a value in this submission.
      template = template.replaceAll("<" + field + ">([,;] )?|([,;] )?<" + field + ">", "");
    }
    String renderedString = "";
    ST st = new ST(template);
    for(String key : args.keySet()) {
      // Add all the arguments to the StringTemplate
      st.add(key, args.get(key));
    }
    renderedString = st.render();
    log.debug("Rendered string: " + renderedString);
    return renderedString;
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
