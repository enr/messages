package com.github.enr.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageSourceBaseTest {

  private static final Locale ITALIAN = Locale.ITALIAN;
  private static final Locale ENGLISH = Locale.ENGLISH;

  private MockMessageSource messageSource;

  // Test Data
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
  @DisplayName("Flag Disabled - Message Exists in Requested Locale")
  void flagDisabledMessageExistsInRequestedLocale() {
    messageSource = builder().useDefaultLocaleFallback(false).build();
    String message = messageSource.msg("test.key", new Context(ITALIAN));
    assertEquals("Ciao mondo", message);
  }

  @Test
  @DisplayName("Flag Disabled - Message Does NOT Exist in Requested Locale")
  void flagDisabledMessageDoesNotExistInRequestedLocale() {
    messageSource = builder().useDefaultLocaleFallback(false).build();
    // "fallback.key" only exists in English
    String message = messageSource.msg("fallback.key", new Context(ITALIAN));
    // The default strategy returns the key in braces
    assertEquals("{fallback.key}", message);
  }

  @Test
  @DisplayName("Flag Enabled - Message Exists in Requested Locale")
  void flagEnabledMessageExistsInRequestedLocale() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    String message = messageSource.msg("test.key", new Context(ITALIAN));
    assertEquals("Ciao mondo", message);
  }

  @Test
  @DisplayName("Flag Enabled - Message Only Exists in Default Locale Primary Bundle")
  void flagEnabledMessageOnlyExistsInDefaultLocale() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    String message = messageSource.msg("fallback.key", new Context(ITALIAN));
    assertEquals("This is a fallback", message);
  }

  @Test
  @DisplayName("Flag Enabled - Message Does Not Exist Anywhere")
  void flagEnabledMessageDoesNotExistAnywhere() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    String message = messageSource.msg("nonexistent.key", new Context(ITALIAN));
    assertEquals("{nonexistent.key}", message);
  }

  @Test
  @DisplayName("Flag Enabled - Requested Locale IS Default Locale")
  void flagEnabledRequestedLocaleIsDefaultLocale() {
    messageSource = builder().useDefaultLocaleFallback(true).build();
    String message = messageSource.msg("test.key", new Context(ENGLISH));
    assertEquals("Hello world", message);
  }

  @Test
  @DisplayName("Backward Compatibility - Constructor Without Flag")
  void backwardCompatibilityConstructorWithoutFlag() {
    // This test relies on the mock's constructor correctly defaulting the flag
    messageSource = new MockMessageSource.Builder(messages).defaultLocale(ENGLISH).build();
    String message = messageSource.msg("fallback.key", new Context(ITALIAN));
    assertEquals("{fallback.key}", message); // Should not fallback
    assertFalse(messageSource.isUseDefaultLocaleFallback());
  }

  @Test
  @DisplayName("Flag Value Accessible via Getter")
  void flagValueAccessibleViaGetter() {
    MockMessageSource withFallback = builder().useDefaultLocaleFallback(true).build();
    MockMessageSource withoutFallback = builder().useDefaultLocaleFallback(false).build();

    assertTrue(withFallback.isUseDefaultLocaleFallback());
    assertFalse(withoutFallback.isUseDefaultLocaleFallback());
  }

  @Test
  @DisplayName("Zero-arg call returns template as-is, no MessageFormat parsing")
  void zeroArgCallReturnsTemplateWithoutMessageFormatParsing() {
    Map<String, String> en = new HashMap<>();
    en.put("json.example", "Use {\"key\": value} syntax");
    messages.put(ENGLISH, en);
    messageSource = new MockMessageSource.Builder(messages).defaultLocale(ENGLISH).build();

    String result = messageSource.msg("json.example", new Context(ENGLISH));
    assertEquals("Use {\"key\": value} syntax", result);
  }

  @Test
  @DisplayName("Flag Enabled - Fallback template formatted with args")
  void flagEnabledFallbackTemplateFormattedWithArgs() {
    Map<String, String> en = new HashMap<>();
    en.put("greeting.key", "Hello {0}");
    messages.put(ENGLISH, en);
    messageSource = builder().useDefaultLocaleFallback(true).build();

    String result = messageSource.msg("greeting.key", new Context(ITALIAN), "World");
    assertEquals("Hello World", result);
  }

  @Test
  @DisplayName("Flag Enabled - Fallback uses default locale for MessageFormat")
  void flagEnabledFallbackUsesDefaultLocaleForMessageFormat() {
    Map<String, String> en = new HashMap<>();
    en.put("amount.key", "{0,number,integer}");
    messages.put(ENGLISH, en);
    messageSource = builder().useDefaultLocaleFallback(true).build();

    String result = messageSource.msg("amount.key", new Context(ITALIAN), 1234567);
    String expectedWithEnglishLocale = new java.text.MessageFormat("{0,number,integer}", ENGLISH).format(new Object[]{1234567});
    assertEquals(expectedWithEnglishLocale, result);
  }

  /**
   * A mock implementation of MessageSourceBase for testing purposes.
   */
  private static class MockMessageSource extends MessageSourceBase {

    private final Map<Locale, Map<String, String>> messages;

    MockMessageSource(MissingKeyStrategy missingKeyStrategy, ErrorHandler errorHandler, Locale defaultLocale,
        boolean useDefaultLocaleFallback, Map<Locale, Map<String, String>> messages) {
      super(missingKeyStrategy, errorHandler, defaultLocale, useDefaultLocaleFallback);
      this.messages = messages;
    }

    @Override
    protected String getMessageTemplate(String key, Context context) {
      Map<String, String> localeMessages = messages.get(context.getLocale());
      if (localeMessages != null) {
        return localeMessages.get(key);
      }
      return null;
    }

    static class Builder {
      private final Map<Locale, Map<String, String>> messages;
      private Locale defaultLocale = Locale.ROOT;
      private boolean useDefaultLocaleFallback = false;
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

      public MockMessageSource build() {
        return new MockMessageSource(missingKeyStrategy, errorHandler, defaultLocale, useDefaultLocaleFallback,
            messages);
      }
    }

    @Override
    public Map<String, String> getAllMessagesKeyAndValue(Context context) {
      Map<String, String> localeMessages = messages.get(context.getLocale());
      return localeMessages != null ? Collections.unmodifiableMap(localeMessages) : Collections.emptyMap();
    }
  }
}
