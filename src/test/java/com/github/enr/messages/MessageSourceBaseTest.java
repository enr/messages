package com.github.enr.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageSourceBaseTest {

  private static final Locale ITALIAN = Locale.ITALIAN;
  private static final Locale ENGLISH = Locale.ENGLISH;

  private MockMessageSource messageSource;

  private Map<Locale, Map<String, String>> messages = new HashMap<>();

  @BeforeEach
  void setUp() {
    messages.clear();
    Map<String, String> italianMessages = new HashMap<>();
    italianMessages.put("test.key", "Ciao mondo");
    messages.put(ITALIAN, italianMessages);

    Map<String, String> englishMessages = new HashMap<>();
    englishMessages.put("test.key", "Hello world");
    englishMessages.put("fallback.key", "This is a fallback");
    messages.put(ENGLISH, englishMessages);
  }

  private MockMessageSource.Builder builder() {
    return new MockMessageSource.Builder(messages).defaultLocale(ENGLISH);
  }

  @Test
  @DisplayName("Flag Disabled - Message Does NOT Exist in Requested Locale")
  void flagDisabledMessageDoesNotExistInRequestedLocale() {
    messageSource = builder().useDefaultLocaleFallback(false).build();
    assertEquals("{fallback.key}", messageSource.msg("fallback.key", new Context(ITALIAN)));
  }

  @Test
  @DisplayName("Flag Enabled - Message Exists in Requested Locale")
  void flagEnabledMessageExistsInRequestedLocale() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    assertEquals("Ciao mondo", messageSource.msg("test.key", new Context(ITALIAN)));
  }

  @Test
  @DisplayName("Flag Enabled - Message Only Exists in Default Locale")
  void flagEnabledMessageOnlyExistsInDefaultLocale() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    assertEquals("This is a fallback", messageSource.msg("fallback.key", new Context(ITALIAN)));
  }

  @Test
  @DisplayName("Flag Enabled - Message Does Not Exist Anywhere")
  void flagEnabledMessageDoesNotExistAnywhere() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    assertEquals("{nonexistent.key}", messageSource.msg("nonexistent.key", new Context(ITALIAN)));
  }

  @Test
  @DisplayName("Flag Enabled - Requested Locale IS Default Locale")
  void flagEnabledRequestedLocaleIsDefaultLocale() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    assertEquals("Hello world", messageSource.msg("test.key", new Context(ENGLISH)));
  }

  @Test
  @DisplayName("3-arg Constructor Defaults useDefaultLocaleFallback to False")
  void legacyConstructorDefaultsFlagToFalse() {
    messageSource = MockMessageSource.withLegacyConstructor(messages, ENGLISH);
    assertEquals("{fallback.key}", messageSource.msg("fallback.key", new Context(ITALIAN)));
    assertFalse(messageSource.isUseDefaultLocaleFallback());
  }

  @Test
  @DisplayName("Null Context Throws IllegalArgumentException")
  void contextNullThrowsIllegalArgumentException() {
    messageSource = builder().build();
    assertThrows(IllegalArgumentException.class, () -> messageSource.msg("test.key", (Context) null));
  }

  @Test
  @DisplayName("Empty Key Returns Missing Key Placeholder")
  void emptyKeyReturnsMissingKeyPlaceholder() {
    messageSource = builder().build();
    assertEquals("{}", messageSource.msg("", new Context(ENGLISH)));
  }

  @Test
  @DisplayName("Args With Null Element Produces 'null' Literal in Output")
  void argsWithNullElementProducesNullLiteralInOutput() {
    Map<String, String> en = new HashMap<>();
    en.put("greeting", "Hello {0}");
    messages.put(ENGLISH, en);
    messageSource = builder().build();

    assertEquals("Hello null", messageSource.msg("greeting", new Context(ENGLISH), (Object) null));
  }

  @Test
  @DisplayName("Explicit Null Args Array Returns Raw Template")
  void explicitNullArgsArrayReturnsRawTemplate() {
    Map<String, String> en = new HashMap<>();
    en.put("json.example", "Use {\"key\": value} syntax");
    messages.put(ENGLISH, en);
    messageSource = builder().build();

    assertEquals("Use {\"key\": value} syntax",
        messageSource.msg("json.example", new Context(ENGLISH), (Object[]) null));
  }

  @Test
  @DisplayName("Custom Missing Key Strategy Returning Null Propagates Null")
  void customMissingKeyStrategyReturningNullPropagatesNull() {
    messageSource =
        new MockMessageSource.Builder(messages).defaultLocale(ENGLISH).missingKeyStrategy(key -> null).build();
    assertNull(messageSource.msg("nonexistent.key", new Context(ENGLISH)));
  }

  @Test
  @DisplayName("Custom Error Handler Is Invoked With Correct Key on Template Error")
  void customErrorHandlerIsInvokedWithCorrectKey() {
    Map<String, String> en = new HashMap<>();
    en.put("bad.template", "Broken {0");
    messages.put(ENGLISH, en);

    AtomicReference<String> capturedKey = new AtomicReference<>();
    messageSource = new MockMessageSource.Builder(messages).defaultLocale(ENGLISH).errorHandler((key, ex) -> {
      capturedKey.set(key);
      return "ERROR";
    }).build();

    assertEquals("ERROR", messageSource.msg("bad.template", new Context(ENGLISH), "arg"));
    assertEquals("bad.template", capturedKey.get());
  }

  @Test
  @DisplayName("Malformed Template Invokes Default Error Handler")
  void malformedTemplateInvokesDefaultErrorHandler() {
    Map<String, String> en = new HashMap<>();
    en.put("bad.template", "Broken {0");
    messages.put(ENGLISH, en);
    messageSource = builder().build();

    String result = messageSource.msg("bad.template", new Context(ENGLISH), "arg");
    assertTrue(result.startsWith("{bad.template}:"),
        "Expected error handler output starting with {bad.template}: but was: " + result);
  }

  @Test
  @DisplayName("Zero-arg Call Returns Template as-is, No MessageFormat Parsing")
  void zeroArgCallReturnsTemplateWithoutMessageFormatParsing() {
    Map<String, String> en = new HashMap<>();
    en.put("json.example", "Use {\"key\": value} syntax");
    messages.put(ENGLISH, en);
    messageSource = new MockMessageSource.Builder(messages).defaultLocale(ENGLISH).build();

    assertEquals("Use {\"key\": value} syntax", messageSource.msg("json.example", new Context(ENGLISH)));
  }

  @Test
  @DisplayName("Flag Enabled - Fallback Template Formatted With Args")
  void flagEnabledFallbackTemplateFormattedWithArgs() {
    Map<String, String> en = new HashMap<>();
    en.put("greeting.key", "Hello {0}");
    messages.put(ENGLISH, en);
    messageSource = builder().useDefaultLocaleFallback(true).build();

    assertEquals("Hello World", messageSource.msg("greeting.key", new Context(ITALIAN), "World"));
  }

  @Test
  @DisplayName("Flag Enabled - Fallback Uses Default Locale for MessageFormat")
  void flagEnabledFallbackUsesDefaultLocaleForMessageFormat() {
    Map<String, String> en = new HashMap<>();
    en.put("amount.key", "{0,number,integer}");
    messages.put(ENGLISH, en);
    messageSource = builder().useDefaultLocaleFallback(true).build();

    String result = messageSource.msg("amount.key", new Context(ITALIAN), 1234567);
    String expected = new java.text.MessageFormat("{0,number,integer}", ENGLISH).format(new Object[] {1234567});
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Always Use MessageFormat - Escaped Apostrophe Is Consistent With and Without Args")
  void alwaysUseMessageFormatHandlesApostrophesConsistently() {
    Map<String, String> it = new HashMap<>();
    it.put("user", "L''utente");
    it.put("user.named", "L''utente {0}");
    messages.put(ITALIAN, it);
    messageSource = builder().alwaysUseMessageFormat(true).build();

    assertEquals("L'utente", messageSource.msg("user", new Context(ITALIAN)));
    assertEquals("L'utente", messageSource.msg("user", new Context(ITALIAN), (Object[]) null));
    assertEquals("L'utente Mario", messageSource.msg("user.named", new Context(ITALIAN), "Mario"));
    assertTrue(messageSource.isAlwaysUseMessageFormat());
  }

  @Test
  @DisplayName("Default - Zero-arg Call Does Not Unescape Apostrophes")
  void defaultZeroArgCallKeepsEscapedApostrophes() {
    Map<String, String> it = new HashMap<>();
    it.put("user", "L''utente");
    messages.put(ITALIAN, it);
    messageSource = builder().build();

    assertEquals("L''utente", messageSource.msg("user", new Context(ITALIAN)));
    assertFalse(messageSource.isAlwaysUseMessageFormat());
  }

  @Test
  @DisplayName("Always Use MessageFormat - Malformed Template Without Args Invokes Error Handler")
  void alwaysUseMessageFormatMalformedTemplateWithoutArgsInvokesErrorHandler() {
    Map<String, String> en = new HashMap<>();
    en.put("bad.template", "Broken {0");
    messages.put(ENGLISH, en);
    messageSource = builder().alwaysUseMessageFormat(true).errorHandler((key, ex) -> "ERROR").build();

    assertEquals("ERROR", messageSource.msg("bad.template", new Context(ENGLISH)));
  }

  private static class MockMessageSource extends MessageSourceBase {

    private final Map<Locale, Map<String, String>> messages;

    MockMessageSource(MissingKeyStrategy missingKeyStrategy, ErrorHandler errorHandler, Locale defaultLocale,
        boolean useDefaultLocaleFallback, boolean alwaysUseMessageFormat, Map<Locale, Map<String, String>> messages) {
      super(missingKeyStrategy, errorHandler, defaultLocale, useDefaultLocaleFallback, alwaysUseMessageFormat);
      this.messages = messages;
    }

    MockMessageSource(MissingKeyStrategy missingKeyStrategy, ErrorHandler errorHandler, Locale defaultLocale,
        Map<Locale, Map<String, String>> messages) {
      super(missingKeyStrategy, errorHandler, defaultLocale);
      this.messages = messages;
    }

    static MockMessageSource withLegacyConstructor(Map<Locale, Map<String, String>> messages, Locale defaultLocale) {
      return new MockMessageSource(MissingKeyStrategy.defaultStrategy(), ErrorHandler.defaultHandler(), defaultLocale,
          messages);
    }

    @Override
    protected String getMessageTemplate(String key, Context context) {
      Map<String, String> localeMessages = messages.get(context.getLocale());
      if (localeMessages != null) {
        return localeMessages.get(key);
      }
      return null;
    }

    @Override
    public Map<String, String> getAllMessagesKeyAndValue(Context context) {
      Map<String, String> localeMessages = messages.get(context.getLocale());
      return localeMessages != null ? Collections.unmodifiableMap(localeMessages) : Collections.emptyMap();
    }

    static class Builder {
      private final Map<Locale, Map<String, String>> messages;
      private Locale defaultLocale = Locale.ROOT;
      private boolean useDefaultLocaleFallback = false;
      private boolean alwaysUseMessageFormat = false;
      private MissingKeyStrategy missingKeyStrategy = MissingKeyStrategy.defaultStrategy();
      private ErrorHandler errorHandler = ErrorHandler.defaultHandler();

      Builder(Map<Locale, Map<String, String>> messages) {
        this.messages = messages;
      }

      public Builder defaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
        return this;
      }

      public Builder useDefaultLocaleFallback(boolean useDefaultLocaleFallback) {
        this.useDefaultLocaleFallback = useDefaultLocaleFallback;
        return this;
      }

      public Builder alwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
        this.alwaysUseMessageFormat = alwaysUseMessageFormat;
        return this;
      }

      public Builder missingKeyStrategy(MissingKeyStrategy strategy) {
        this.missingKeyStrategy = strategy;
        return this;
      }

      public Builder errorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
        return this;
      }

      public MockMessageSource build() {
        return new MockMessageSource(missingKeyStrategy, errorHandler, defaultLocale, useDefaultLocaleFallback,
            alwaysUseMessageFormat, messages);
      }
    }
  }
}
