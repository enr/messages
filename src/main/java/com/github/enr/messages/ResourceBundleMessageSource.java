package com.github.enr.messages;

import java.lang.System.Logger;
import java.lang.invoke.MethodHandles;
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

  private ClassLoader classLoader;

  private ResourceBundleMessageSource(Builder builder) {
    super(builder.missingKeyStrategy, builder.errorHandler, builder.defaultLocale);
    this.resource = builder.resource;
    this.fallbackResource = builder.fallbackResource;
    this.classLoader = builder.classLoader;
  }

  public static class Builder {
    private final String resource;
    private MissingKeyStrategy missingKeyStrategy = MissingKeyStrategy.defaultStrategy();
    private ErrorHandler errorHandler = ErrorHandler.defaultHandler();
    private Locale defaultLocale = Locale.getDefault();
    private String fallbackResource;
    private ClassLoader classLoader;

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

    public Builder withClassLoader(ClassLoader classLoader) {
      this.classLoader = classLoader;
      return this;
    }
  }

  public static Builder forResource(String resource) {
    return new Builder(resource);
  }

  @Override
  protected String getMessageTemplate(String key, Context context) throws Exception {
    String template = null;
    try {
      ResourceBundle mainBundle = loadBundle(resource, context);
      template = mainBundle.getString(key);
      if (template != null) {
        return template;
      }
    } catch (MissingResourceException ignored) {
    }
    try {
      if (fallbackResource != null) {
        ResourceBundle fallbackLabels = loadBundle(fallbackResource, context);
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
    Map<String, String> messages = new HashMap<>();
    try {
      ResourceBundle bundle = loadBundle(resource, context);

      Enumeration<String> keys = bundle.getKeys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        messages.put(key, bundle.getString(key));
      }
    } catch (MissingResourceException ignored) {
    }

    if (fallbackResource == null) {
      return messages;
    }
    try {
      ResourceBundle fallbackBundle = loadBundle(fallbackResource, context);
      Enumeration<String> fallbackKeys = fallbackBundle.getKeys();
      while (fallbackKeys.hasMoreElements()) {
        String key = fallbackKeys.nextElement();
        messages.putIfAbsent(key, fallbackBundle.getString(key));
      }
    } catch (MissingResourceException ignored) {
    }
    return messages;
  }

  private ResourceBundle loadBundle(String res, Context context) {
    if (classLoader != null) {
      return ResourceBundle.getBundle(res, context.getLocale(), classLoader);
    }
    return ResourceBundle.getBundle(res, context.getLocale());
  }

}
