package com.github.enr.messages;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleMessageSource extends MessageSourceBase {

  private final String resource;

  public ResourceBundleMessageSource(String resource) {
    super(MissingKeyStrategy.defaultStrategy(), ErrorHandler.defaultHandler(), Locale.getDefault());
    this.resource = resource;

  }

  public ResourceBundleMessageSource(String resource, MissingKeyStrategy missingKeyStrategy) {
    super(missingKeyStrategy, ErrorHandler.defaultHandler(), Locale.getDefault());
    this.resource = resource;

  }

  public ResourceBundleMessageSource(String resource, MissingKeyStrategy missingKeyStrategy,
      ErrorHandler errorHandler) {
    super(missingKeyStrategy, errorHandler, Locale.getDefault());
    this.resource = resource;

  }

  public ResourceBundleMessageSource(String resource, MissingKeyStrategy missingKeyStrategy, ErrorHandler errorHandler,
      Locale defaultLocale) {
    super(missingKeyStrategy, errorHandler, defaultLocale);
    this.resource = resource;

  }

  @Override
  protected String getMessageTemplate(String key, Context context) throws Exception {
    try {
      ResourceBundle labels = ResourceBundle.getBundle(resource, context.getLocale());
      System.err.println("resource bundle get message template");
      return labels.getString(key);
    } catch (MissingResourceException e) {
      // return missingKeyStrategy.handleMissingKey(key);
    }
    return null;
  }

  /**
   * Retrieves all keys and values from the resource bundle for a given locale.
   *
   * @param context The context containing the locale.
   * @return A map with all keys and their corresponding message values.
   */
  @Override
  public Map<String, String> getAllMessagesKeyAndValue(Context context) {
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(resource, context.getLocale());
      Map<String, String> messages = new HashMap<>();

      Enumeration<String> keys = bundle.getKeys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        messages.put(key, bundle.getString(key));
      }
      return messages;
    } catch (MissingResourceException e) {
      return Collections.emptyMap(); // Return an empty map if the resource is not found
    }
  }

}
