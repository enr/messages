package com.github.enr.messages;

import java.lang.System.Logger;
import java.lang.invoke.MethodHandles;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceBundleMessageSource extends MessageSourceBase {

  private static final Logger LOG = System.getLogger(MethodHandles.lookup().lookupClass().getName());

  private final String resource;

  private String fallbackResource;

  private ClassLoader classLoader;

  private final ConcurrentHashMap<Locale, ResourceBundle> mainBundleCache;
  private final ConcurrentHashMap<Locale, ResourceBundle> fallbackBundleCache;

  private ResourceBundleMessageSource(Builder builder) {
    super(builder.missingKeyStrategy, builder.errorHandler, builder.defaultLocale);
    this.resource = builder.resource;
    this.fallbackResource = builder.fallbackResource;
    this.classLoader = builder.classLoader;
    this.mainBundleCache = new ConcurrentHashMap<>();
    this.fallbackBundleCache = new ConcurrentHashMap<>();
    loadBundlesForLocale(builder.defaultLocale);
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
      ResourceBundle mainBundle = getMainBundle(context);
      if (mainBundle != null) {
        template = mainBundle.getString(key);
      }
      if (template != null) {
        return template;
      }
    } catch (MissingResourceException ignored) {
    }
    try {
      if (fallbackResource != null) {
        ResourceBundle fallbackLabels = getFallbackBundle(context);
        if (fallbackLabels != null) {
          template = fallbackLabels.getString(key);
        }
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
      ResourceBundle bundle = getMainBundle(context);
      if (bundle != null) {
        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement();
          messages.put(key, bundle.getString(key));
        }
      }
    } catch (MissingResourceException ignored) {
    }

    if (fallbackResource == null) {
      return messages;
    }
    try {
      ResourceBundle fallbackBundle = getFallbackBundle(context);
      if (fallbackBundle != null) {
        Enumeration<String> fallbackKeys = fallbackBundle.getKeys();
        while (fallbackKeys.hasMoreElements()) {
          String key = fallbackKeys.nextElement();
          messages.putIfAbsent(key, fallbackBundle.getString(key));
        }
      }
    } catch (MissingResourceException ignored) {
    }
    return messages;
  }

  private void loadBundlesForLocale(Locale locale) {
    // Carica main bundle
    try {
      ResourceBundle mainBundle = loadBundle(resource, locale);
      mainBundleCache.put(locale, mainBundle);
    } catch (MissingResourceException e) {
      LOG.log(Logger.Level.WARNING, "Main bundle not found for locale " + locale);
    }

    // Carica fallback bundle
    if (fallbackResource != null) {
      try {
        ResourceBundle fallbackBundle = loadBundle(fallbackResource, locale);
        fallbackBundleCache.put(locale, fallbackBundle);
      } catch (MissingResourceException e) {
        LOG.log(Logger.Level.WARNING, "Fallback bundle not found for locale " + locale);
      }
    }
  }

  protected ResourceBundle getMainBundle(Context context) {
    return mainBundleCache.computeIfAbsent(context.getLocale(), loc -> {
      try {
        return loadBundle(resource, loc);
      } catch (MissingResourceException e) {
        return null;
      }
    });
  }

  protected ResourceBundle getFallbackBundle(Context context) {
    if (fallbackResource == null) {
      return null;
    }
    return fallbackBundleCache.computeIfAbsent(context.getLocale(), loc -> {
      try {
        return loadBundle(fallbackResource, loc);
      } catch (MissingResourceException e) {
        return null;
      }
    });
  }

  private ResourceBundle loadBundle(String res, Locale locale) {
    if (classLoader != null) {
      return ResourceBundle.getBundle(res, locale, classLoader);
    }
    return ResourceBundle.getBundle(res, locale);
  }

  protected void clearCache() {
    mainBundleCache.clear();
    fallbackBundleCache.clear();
  }
}
