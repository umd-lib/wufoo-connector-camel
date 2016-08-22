package edu.umd.lib.exception;

/***
 * Custom Exception to check if the request is valid request from WuFoo
 */
public class SysAidConnectorException extends Exception {

  private static final long serialVersionUID = 1L;

  public SysAidConnectorException() {
  }

  // Constructor that accepts a message
  public SysAidConnectorException(String message) {
    super(message);
  }
}
