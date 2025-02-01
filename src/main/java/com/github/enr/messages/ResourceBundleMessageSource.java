package com.github.enr.messages;

import java.util.Locale;
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
}
