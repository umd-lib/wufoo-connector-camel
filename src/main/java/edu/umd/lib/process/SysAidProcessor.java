package edu.umd.lib.process;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.google.common.base.Splitter;

import edu.umd.lib.exception.FormMappingException;
import edu.umd.lib.exception.SysAidLoginException;
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
   * SysAidProcessor Constructor which connects to SysAid using URL, login and
   * password parameters
   *
   * @param url
   * @param login
   * @param password
   * @param formmapping
   * @throws SysAidLoginException
   */
  public SysAidProcessor(String url, String login, String password, String formmapping) throws SysAidLoginException {
    sysaid = new SysAidConnector(url, login, password);
    sysaid.sysAidLogin();
    wufoo_sysaid_map = this.parseMap(formmapping);
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

    if (wufoo_sysaid_map.containsKey(message.get("Hash"))) {
      String formMappingProperties = wufoo_sysaid_map.get(message.get("Hash"));
      log.info("Property File Name :" + formMappingProperties);
      sysaid.createServiceRequest(message, formMappingProperties);
    } else {
      log.error("Mapping file for the form:" + message.get("Hash") + " not found.");
      throw new FormMappingException("Mapping file for the form :" + message.get("Hash") + " not found.");
    }

  }

}
