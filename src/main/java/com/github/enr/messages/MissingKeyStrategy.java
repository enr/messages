package com.github.enr.messages;

@FunctionalInterface
public interface MissingKeyStrategy {
  /**
   * Handles the situation where a key is missing from the resource.
   *
   * @param key The missing key.
   * @return A fallback string to use when the key is not found.
   */
  String handleMissingKey(String key);

  /**
   * Factory method for creating a default implementation of MissingKeyStrategy.
   *
   * @return A default MissingKeyStrategy that returns a placeholder for the missing key.
   */
  static MissingKeyStrategy defaultStrategy() {
    return key -> String.format("{%s}", key);
  }
}
