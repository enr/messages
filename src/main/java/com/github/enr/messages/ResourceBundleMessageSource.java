package com.github.enr.messages;

import java.lang.System.Logger;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleMessageSource extends MessageSourceBase {

  private static final Logger LOG = System.getLogger(MethodHandles.lookup().lookupClass().getName());

  private final String resource;

  private String fallbackResource;

  private ResourceBundleMessageSource(Builder builder) {
    super(builder.missingKeyStrategy, builder.errorHandler, builder.defaultLocale);
    this.resource = builder.resource;
    this.fallbackResource = builder.fallbackResource;
  }

  public static class Builder {
    private final String resource;
    private MissingKeyStrategy missingKeyStrategy = MissingKeyStrategy.defaultStrategy();
    private ErrorHandler errorHandler = ErrorHandler.defaultHandler();
    private Locale defaultLocale = Locale.getDefault();
    private String fallbackResource;

    public Builder(String resource) {
      this.resource = resource;
    }

    public Builder withMissingKeyStrategy(MissingKeyStrategy missingKeyStrategy) {
      this.missingKeyStrategy = missingKeyStrategy;
      return this;
    }

    public Builder withErrorHandler(ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
      return this;
    }

    public Builder withDefaultLocale(Locale defaultLocale) {
      this.defaultLocale = defaultLocale;
      return this;
    }

    public Builder withFallbackResource(String fallbackResource) {
      this.fallbackResource = fallbackResource;
      return this;
    }

    public ResourceBundleMessageSource build() {
      return new ResourceBundleMessageSource(this);
    }
  }

  public static Builder forResource(String resource) {
    return new Builder(resource);
  }

  @Override
  protected String getMessageTemplate(String key, Context context) throws Exception {
    String template = null;
    try {
      ResourceBundle mainLabels = ResourceBundle.getBundle(resource, context.getLocale());
      template = mainLabels.getString(key);
      if (template != null) {
        return template;
      }
    } catch (MissingResourceException ignored) {
    }
    try {
      if (fallbackResource != null) {
        ResourceBundle fallbackLabels = ResourceBundle.getBundle(fallbackResource, context.getLocale());
        template = fallbackLabels.getString(key);
      }
      return template;
    } catch (MissingResourceException ignored) {
    }
    return template;
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
      if (fallbackResource != null) {
        ResourceBundle fallbackBundle = ResourceBundle.getBundle(fallbackResource, context.getLocale());
        Enumeration<String> fallbackKeys = fallbackBundle.getKeys();
        while (fallbackKeys.hasMoreElements()) {
          String key = fallbackKeys.nextElement();
          messages.putIfAbsent(key, fallbackBundle.getString(key));
        }
      }
      return messages;
    } catch (MissingResourceException e) {
      LOG.log(Logger.Level.ERROR, "Error loading resource {0}: {1}", resource, e);
      return Collections.emptyMap(); // Return an empty map if the resource is not found
    }
  }

}
