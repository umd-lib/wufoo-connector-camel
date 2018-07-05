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
import org.stringtemplate.v4.ST;

import edu.umd.lib.core.FieldsRenderer;
import edu.umd.lib.exception.FormMappingException;
import edu.umd.lib.exception.SysAidConnectorException;

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

    private final Logger log = Logger.getLogger(SysAidConnector.class);

    private final String SYSAID_FIELD_PREFIX = "sysaid.";
    private final String MAPPED_SYSAID_FIELD_PREFIX = "mapped_sysaid.";
    private final String MAPPED_SYSAID_KEY_PREFIX = "mapped_sysaid_key.";

    private final String sysaid_formID;
    private final String sysaid_accountID;
    private final String sysaid_webformurl;

    private PropertiesConfiguration configuration;

    private FieldsRenderer renderer;

    private boolean useYamlConfig = false;

    public SysAidConnector(String url, String accountid, String formID) {
        this.sysaid_webformurl = url;
        this.sysaid_accountID = accountid;
        this.sysaid_formID = formID;
    }

    /***
     * Load the mapping from SysAid and WuFoo forms in the properties file into the
     * map to find the mappings easily
     * 
     * @param resourceName properties file name
     * @throws FormMappingException
     */
    public void LoadWufooSysaidMapping(String resource, Map<String, String> wufooValues) throws FormMappingException {
        if (resource.endsWith(".yml") || resource.endsWith(".yaml")) {
            useYamlConfig = true;
            this.renderer = new FieldsRenderer(resource, wufooValues);
        } else {
            try {
                this.configuration = new PropertiesConfiguration();
                this.configuration.load(new FileInputStream(resource));
            } catch (IOException | ConfigurationException e) {

                log.error("IOException occured in Method : LoadwufooSysaidMapping of Class :SysAidConnector.java"
                        + "  while attempting access Resource Stream ", e);
                throw new FormMappingException("Mapping file :" + resource + " Not found.");
            }
        }
    }

    /***
     * Create Service Request in SysAid using the value map sent from WuFoo. The map
     * is compared with the field mapping map
     * 
     * @return ServiceRequest_ID
     * @throws FormMappingException
     * @throws SysAidConnectorException
     */
    public void createServiceRequest(HashMap<String, String> values, String resource)
            throws FormMappingException, SysAidConnectorException {

        verifyConfiguration();

        this.LoadWufooSysaidMapping(resource, values);

        try {

            DefaultHttpClient client = new DefaultHttpClient();
            Map<String, String> fieldMap;
            if (useYamlConfig) {
                fieldMap = renderer.getRenderedFields();
            } else {
                fieldMap = fieldMapping(values);
            }
            fieldMap.put("accountID", sysaid_accountID);
            fieldMap.put("formID", sysaid_formID);
            List<NameValuePair> fields = getNameValuePairList(fieldMap);

            HttpPost httpPost = new HttpPost(this.sysaid_webformurl);
            httpPost.setEntity(new UrlEncodedFormEntity(fields, "UTF-8"));
            HttpResponse response = client.execute(httpPost);

            if (response != null) {
                log.info("Response: \n" + response.toString());
            } else {
                log.info("Unable to execute POST request.\nRequest parameters: \n" + fields);
            }
        } catch (ParseException e) {
            log.error("ParseException occured while attempting to "
                    + "create service request. Method:createServiceRequest.", e);
        } catch (IOException e) {
            log.error(
                    "IOException occured while attempting to" + " create service request. Method:createServiceRequest",
                    e);
        } catch (JSONException e) {
            log.error("JSONException occured while attempting to"
                    + " create service request. Method:createServiceRequest", e);
        } catch (IllegalStateException e) {
            log.error("IllegalStateException occured while attempting to"
                    + " create service request. Method:createServiceRequest", e);
        }

    }

    private void verifyConfiguration() throws SysAidConnectorException {
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
    }

    /****
     * Mapping WuFoo fields with SysAid fields based on the mapping specified in the
     * mapping configuration and default values configuration.
     * 
     * This method exists for backwards compatibility and can be removed after all
     * `.properties` configuration are migrated to YAML configuration.
     * 
     * @param values
     * @return
     * @throws JSONException
     */
    @Deprecated
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
            String wufooValue = "";
            String sysaidKey = "";

            if (key.startsWith(MAPPED_SYSAID_FIELD_PREFIX)) {
                wufooValue = getMappedValue(key, nonEmptyArgs);
                sysaidKey = key.substring(MAPPED_SYSAID_FIELD_PREFIX.length());
            }
            if (key.startsWith(SYSAID_FIELD_PREFIX)) {
                wufooValue = renderedTemplate(configuration.getString(key), nonEmptyArgs, blankFields);
                sysaidKey = key.substring(SYSAID_FIELD_PREFIX.length());
            }
            if (wufooValue != null && !wufooValue.isEmpty()) {
                sysaidValues.put(sysaidKey, wufooValue);
            }
        }
        return sysaidValues;
    }

    protected String getMappedValue(String key, Map<String, String> args) {
        String wufooKey = configuration.getString(key);
        if (args.containsKey(wufooKey)) {
            String wufooValue = args.get(wufooKey);
            String sysaidKey = key.substring(MAPPED_SYSAID_FIELD_PREFIX.length());
            List<Object> mappingConfig = configuration.getList(MAPPED_SYSAID_KEY_PREFIX + sysaidKey);
            for (Object listItemObj : mappingConfig) {
                String listItem = listItemObj.toString();
                if (listItem.startsWith(wufooValue + "|")) {
                    return listItem.substring(wufooValue.length() + 1);
                }
            }
        }
        return null;
    }

    /**
     * Render the template using the StringTemplate library. This replace the
     * <FieldID> values in the template with the corresponding value from the args
     * map. The <FieldID> of the arguments with empty values will be stripped from
     * the template string prior to the rendering to correctly remove the
     * punctuations associated with those <FieldID> without values.
     * 
     * This method exists for backwards compatibility and can be removed after all
     * `.properties` configuration are migrated to YAML configuration.
     * 
     * @param template    - The template string.
     * @param args        - The parameters to render the template.
     * @param emptyFields - Fields (and associated punctuations) to be stripped from
     *                    the template.
     * 
     * @return Rendered template string.
     */
    @Deprecated
    protected String renderedTemplate(String template, Map<String, String> args, List<String> emptyFields) {
        log.debug("Template string: " + template);
        if (!template.contains("<Field")) {
            // No fields to be rendered so return template as the rendered string.
            return template;
        }
        for (String field : emptyFields) {
            // Remove Fields (and associated punctuations) that does not have a value in
            // this submission.
            template = template.replaceAll("<" + field + ">([,;] )?|([,;] )?<" + field + ">", "");
        }
        String renderedString = "";
        ST st = new ST(template);
        for (String key : args.keySet()) {
            // Add all the arguments to the StringTemplate
            st.add(key, args.get(key));
        }
        renderedString = st.render();
        log.debug("Rendered string: " + renderedString);
        return renderedString;
    }

    /**
     * Convert a Map to List of NameValuePair
     * 
     * @param map - A map with string key and string value
     * @return list of name value pairs
     */
    protected List<NameValuePair> getNameValuePairList(Map<String, String> map) throws SysAidConnectorException {
        List<NameValuePair> fields = new ArrayList<NameValuePair>();
        map.keySet().forEach(k -> fields.add(new BasicNameValuePair(k, map.get(k))));
        return fields;
    }

}
