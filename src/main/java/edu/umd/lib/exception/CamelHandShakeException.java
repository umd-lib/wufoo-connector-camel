package edu.umd.lib.exception;

/***
 * Custom Exception to check if the request is valid request from WuFoo
 */
public class CamelHandShakeException extends Exception {

  private static final long serialVersionUID = 1L;

  public CamelHandShakeException() {
  }

  // Constructor that accepts a message
  public CamelHandShakeException(String message) {
    super(message);
  }
}
