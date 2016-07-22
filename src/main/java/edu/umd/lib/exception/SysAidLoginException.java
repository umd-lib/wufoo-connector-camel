package edu.umd.lib.exception;

/***
 * Custom Exception when SysAid Authentication Fails
 */
public class SysAidLoginException extends Exception {
  private static final long serialVersionUID = 1L;

  public SysAidLoginException() {
  }

  public SysAidLoginException(String message) {
    super(message);
  }
}