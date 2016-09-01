package edu.umd.lib.process;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.google.common.base.Splitter;

import edu.umd.lib.exception.FormMappingException;
import edu.umd.lib.services.SysAidConnector;

/**
 * SysAidProcessor connects to SysAid using Login credentials from Configuration
 * file. The fields required to create service request is mapped to a
 * configuration file to map to SysAid field and then processed to create a
 * service request
 * <p>
 *
 * @since 1.0
 */
public class SysAidProcessor implements Processor {

  private Map<String, String> wufoo_sysaid_map;

  private static Logger log = Logger.getLogger(SysAidProcessor.class);
  SysAidConnector sysaid;

  /****
   * SysAidProcessor Constructor
   *
   * @param url
   * @param accountId
   * @param formid
   * @param formmapping
   * @throws SysAidLoginException
   */
  public SysAidProcessor(String url, String accountId, String formid, String formmapping) {
    sysaid = new SysAidConnector(url, accountId, formid);
    if (formmapping != null && !formmapping.equalsIgnoreCase("")) {
      wufoo_sysaid_map = this.parseMap(formmapping);
    }

  }

  /****
   * Parsing map from String
   *
   * @param formattedMap
   * @return
   */
  public Map<String, String> parseMap(String formattedMap) {
    return Splitter.on(",").withKeyValueSeparator("=").split(formattedMap);
  }

  /***
   * Process method to perform business logic on route
   */
  @SuppressWarnings("unchecked")
  @Override
  public void process(Exchange exchange) throws Exception {

    log.info("Processing new request to SysAid");
    HashMap<String, String> message = exchange.getIn().getBody(HashMap.class);

    if (wufoo_sysaid_map != null && wufoo_sysaid_map.containsKey(message.get("Hash"))) {
      String formMappingProperties = wufoo_sysaid_map.get(message.get("Hash"));
      log.info("Property File Name :" + formMappingProperties);
      sysaid.createServiceRequest(message, formMappingProperties);
    } else {
      log.error("Mapping file for the form:" + message.get("Hash") + " not found.");
      throw new FormMappingException("Mapping file for the form :" + message.get("Hash") + " not found.");
    }

  }

}
