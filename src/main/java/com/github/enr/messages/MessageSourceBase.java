package com.github.enr.messages;

import java.lang.System.Logger;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.Locale;

public abstract class MessageSourceBase implements MessageSource {

  private static final Logger LOG = System.getLogger(MethodHandles.lookup().lookupClass().getName());

  private final MissingKeyStrategy missingKeyStrategy;
  private final ErrorHandler errorHandler;
  private final Context defaultContext;
  private final boolean useDefaultLocaleFallback;

  protected MessageSourceBase(MissingKeyStrategy missingKeyStrategy, ErrorHandler errorHandler, Locale defaultLocale,
      boolean useDefaultLocaleFallback) {
    this.missingKeyStrategy = missingKeyStrategy != null ? missingKeyStrategy : MissingKeyStrategy.defaultStrategy();
    this.errorHandler = errorHandler != null ? errorHandler : ErrorHandler.defaultHandler();
    this.defaultContext = new Context(defaultLocale);
    this.useDefaultLocaleFallback = useDefaultLocaleFallback;
  }

  protected MessageSourceBase(MissingKeyStrategy missingKeyStrategy, ErrorHandler errorHandler, Locale defaultLocale) {
    this(missingKeyStrategy, errorHandler, defaultLocale, false);
  }

  protected abstract String getMessageTemplate(String key, Context context) throws Exception;

  @Override
  public String msg(String key, Context context, Object... args) {
    if (key == null || context == null) {
      throw new IllegalArgumentException("Key and Context must not be null.");
    }

    try {
      String template = getMessageTemplate(key, context);
      LOG.log(Logger.Level.DEBUG, "template = {0}", template);
      if (template == null) {
        // Fallback to default locale if flag is enabled
        if (useDefaultLocaleFallback && !context.getLocale().equals(defaultContext.getLocale())) {
          LOG.log(Logger.Level.DEBUG,
              "Message key ''{0}'' not found in locale ''{1}'', falling back to default locale ''{2}''", key,
              context.getLocale(), defaultContext.getLocale());
          template = getMessageTemplate(key, defaultContext);
        }
      }
      if (template == null) {
        return missingKeyStrategy.handleMissingKey(key);
      }
      LOG.log(Logger.Level.DEBUG, "resolve msg from template {0}", template);
      if (args == null) {
        return template;
      }
      MessageFormat mf = new MessageFormat(template, context.getLocale());
      return mf.format(args);
    } catch (Exception e) {
      return errorHandler.handleError(key, e);
    }
  }

  @Override
  public String msg(String key, Object... args) {
    return msg(key, defaultContext, args);
  }

  public boolean isUseDefaultLocaleFallback() {
    return useDefaultLocaleFallback;
  }
}
