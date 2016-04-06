package edu.umd.lib.routes;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.XML;

public class WofooListener extends AbstractRoute {

  /**
   * Initializes a new instance of this class which defines a Camel route which
   * listens for incoming service invocations.
   */
  public WofooListener() {
    // sets the name of this bean
    this.setName("wofoo");
    // defines the service-name as set in the properties file
    this.setServiceName("wofoo-listener");

  }

  @Override
  protected void defineRoute() throws Exception {
    System.out.println("Endpoint:" + this.getEndpoint());
    from("jetty:http://localhost:8080/myapp/mytestservice").streamCaching().process(
        new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            String message = exchange.getIn().getBody(String.class);
            System.out.println("Hello world Mr " + message);
            Map<String, List<String>> parameters = getQueryParams(message);
            exchange.getOut().setBody("Hello world Mr " + message);
            System.out.println("Hello world Mr " + parameters.size());

            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
              String s = "";
              for (int i = 0; i < entry.getValue().size(); i++) {
                s = s + entry.getValue().get(i);
              }
              System.out.println(entry.getKey() + ":" + s);
            }
            // http://stackoverflow.com/questions/23307707/how-to-get-and-set-parameters-in-camel-http-proxy
            System.out.println("json" + parameters.get("FieldStructure"));
            System.out.println("IP" + parameters.get("IP"));
            JSONArray json = new JSONArray(parameters.get("FieldStructure"));
            System.out.println("data" + json);
            String xml = XML.toString(json);
            System.out.println("Xml" + xml);
          }
        });

  }

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

      return params;
    } catch (UnsupportedEncodingException ex) {
      throw new AssertionError(ex);
    }
  }
}
