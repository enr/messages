package com.github.enr.messages;

@FunctionalInterface
public interface ErrorHandler {
  /**
   * Handles errors that occur during message retrieval or formatting.
   *
   * @param key The required key.
   * @param throwable The exception or error that occurred.
   * @return A fallback string to use in case of an error.
   */
  String handleError(String key, Throwable throwable);

  /**
   * Factory method for creating a default implementation of ErrorHandler.
   *
   * @return A default ErrorHandler that returns a generic error message.
   */
  static ErrorHandler defaultHandler() {
    return (key, throwable) -> String.format("{%s}: %s", key, throwable.getMessage());
  }
}
