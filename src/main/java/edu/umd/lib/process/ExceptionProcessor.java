package edu.umd.lib.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import edu.umd.lib.exception.CamelHandShakeException;
import edu.umd.lib.exception.FormMappingException;
import edu.umd.lib.exception.SysAidConnectorException;

/**
 * ExceptionProcessor creates the Email body content for notifying Exceptions.
 * Based on the Exception that was caught the Exception Message is created and
 * sent to System admin Email Address
 * <p>
 *
 * @since 1.0
 */
public class ExceptionProcessor implements Processor {

  private static Logger log = Logger.getLogger(ExceptionProcessor.class);

  @Override
  public void process(Exchange exchange) {

    try {

      Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
      List<?> messageHistory = (ArrayList<?>) exchange.getProperty(Exchange.MESSAGE_HISTORY);

      String exceptionClass = exception.getClass().getName();
      StringBuilder email_Content = new StringBuilder();

      // Based on the Exception Class create Custom Email Message
      if (exceptionClass.equalsIgnoreCase(CamelHandShakeException.class.getName())) {
        email_Content.append("Exception: CamelHandShakeException \n");
        email_Content.append("Exception Message: " + exception.getMessage() + " \n");
        log.info("CamelHandShakeException");
        exception.printStackTrace();
      } else if (exceptionClass.equalsIgnoreCase(FormMappingException.class.getName())) {
        email_Content.append("Exception: FormMappingException \n");
        email_Content.append("Exception Message: " + exception.getMessage() + " \n");
        log.info("FormMappingException");
        exception.printStackTrace();
      } else if (exceptionClass.equalsIgnoreCase(SysAidConnectorException.class.getName())) {
        email_Content.append("Exception: SysAidConnectorException \n");
        email_Content.append("Exception Message: " + exception.getMessage() + " \n");
        log.info("SysAidConnectorException");
        exception.printStackTrace();
      } else {
        email_Content.append("Exception: " + exceptionClass + " \n");
        email_Content.append("Exception Message: " + exception.getMessage() + " \n");
        log.info("Exception:" + exceptionClass);
        exception.printStackTrace();
      }

      email_Content.append("\nCamel Message History: \n");
      for (int i = 0; i < messageHistory.size(); i++) {
        email_Content.append(messageHistory.get(i) + " \n");
      }

      String message = exchange.getIn().getBody(String.class);
      message = java.net.URLDecoder.decode(message, "UTF-8");
      email_Content.append("\n\nOriginal Message: " + message + " \n");

      exchange.getOut().setBody(email_Content.toString()); // Email Content

    } catch (Exception e) {
      log.error("Exception occured while handling Route's Exception." + e);
    }
  }

}
