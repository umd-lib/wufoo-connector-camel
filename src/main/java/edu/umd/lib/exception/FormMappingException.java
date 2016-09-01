package edu.umd.lib.exception;

/***
 * Custom Exception to check if the request is valid request from WuFoo
 */
public class FormMappingException extends Exception {

  private static final long serialVersionUID = 1L;

  public FormMappingException() {
  }

  // Constructor that accepts a message
  public FormMappingException(String message) {
    super(message);
  }
}
