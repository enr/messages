package com.github.enr.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceBundleMessageSourceTest {

  private static final String RESOURCE_BUNDLE_NAME = "messages.test";
  private static final String FALLBACK_BUNDLE_NAME = "messages.fallback";

  private ResourceBundleMessageSource messageSource;

  @BeforeEach
  void setUp() {
    messageSource = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withFallbackResource(FALLBACK_BUNDLE_NAME).withDefaultLocale(Locale.ENGLISH).build();
  }

  @Test
  void testValidMessage() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();
    String result = source.msg("welcome.message", new Context(Locale.ENGLISH), "John");
    assertEquals("Welcome John!", result);
  }

  @Test
  void testValidMessageWithFallback() {
    Context context = new Context(Locale.ENGLISH);
    assertEquals("messages", messageSource.msg("test.message.both", context));
    assertEquals("fallback", messageSource.msg("test.message.only-fallback", context));
  }

  @Test
  void testValidMessageWithFallbackAndCustomLocale() {
    Locale customLocale = Locale.forLanguageTag("it");
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withDefaultLocale(customLocale).withFallbackResource(FALLBACK_BUNDLE_NAME).build();

    assertEquals("messages italiano", source.msg("test.message.both"));
    assertEquals("fallback italiano", source.msg("test.message.only-fallback"));

    Map<String, String> allMessages = source.getAllMessagesKeyAndValue(new Context(customLocale));
    assertThat(allMessages).as("get all messages result").hasSize(4).containsEntry("welcome.message", "Benvenuto {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages italiano")
        .containsEntry("test.message.only-fallback", "fallback italiano");
  }

  @Test
  void testGetAllMessages() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();
    Map<String, String> result = source.getAllMessagesKeyAndValue(new Context(Locale.ENGLISH));
    assertThat(result).as("get all messages result").hasSize(4).containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages")
        .containsEntry("english.only.key", "Only in English");
  }

  @Test
  void testGetAllMessagesWithFallback() {
    Map<String, String> result = messageSource.getAllMessagesKeyAndValue(new Context(Locale.ENGLISH));
    assertThat(result).as("get all messages result").hasSize(5).containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages")
        .containsEntry("test.message.only-fallback", "fallback").containsEntry("english.only.key", "Only in English");
  }

  @Test
  void testGetAllMessagesWithMainBundleNotFound() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource("messages.nonexistent")
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build();
    Map<String, String> result = source.getAllMessagesKeyAndValue(new Context(Locale.ENGLISH));
    assertThat(result).as("get all messages result when main bundle not found").hasSize(2)
        .containsEntry("test.message.both", "fallback").containsEntry("test.message.only-fallback", "fallback");
  }

  @Test
  void testGetAllMessagesWithFallbackNotFound() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withFallbackResource("messages.nonexistent").build();
    Map<String, String> result = source.getAllMessagesKeyAndValue(new Context(Locale.ENGLISH));
    assertThat(result).as("get all messages result when fallback bundle not found").hasSize(4)
        .containsEntry("welcome.message", "Welcome {0}!").containsEntry("test.foo", "bar")
        .containsEntry("test.message.both", "messages").containsEntry("english.only.key", "Only in English");
  }

  @Test
  void testNullKey() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> messageSource.msg(null, new Context(Locale.ENGLISH)));
    assertEquals("Key and Context must not be null.", ex.getMessage());
  }

  @Test
  void nullContextThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> messageSource.msg("any.key", (Context) null));
  }

  @Test
  void getAllMessagesWithNullContextThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> messageSource.getAllMessagesKeyAndValue(null));
  }

  @Test
  void testNullResourceThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> new ResourceBundleMessageSource.Builder(null));
  }

  @Test
  void mainValueWinsOverFallbackForDuplicateKeys() {
    assertEquals("messages", messageSource.msg("test.message.both", new Context(Locale.ENGLISH)));
  }

  @Test
  void fallbackBundleUsedWhenKeyMissingInMain() {
    assertEquals("fallback", messageSource.msg("test.message.only-fallback", new Context(Locale.ENGLISH)));
  }

  @Test
  void keyNotFoundInAnyBundleReturnsMissingKeyPlaceholder() {
    assertEquals("{nonexistent.key}", messageSource.msg("nonexistent.key", new Context(Locale.ENGLISH)));
  }

  @Test
  void customMissingKeyStrategyIsUsed() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withMissingKeyStrategy(key -> "MISSING:" + key).build();
    assertEquals("MISSING:no.such.key", source.msg("no.such.key", new Context(Locale.ENGLISH)));
  }

  @Test
  void testUseDefaultLocaleFallback_flagWiredInBuilder() {
    ResourceBundleMessageSource withFallback = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withDefaultLocale(Locale.ENGLISH).withUseDefaultLocaleFallback(true).build();
    assertTrue(withFallback.isUseDefaultLocaleFallback());

    ResourceBundleMessageSource withoutFallback = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withDefaultLocale(Locale.ENGLISH).withUseDefaultLocaleFallback(false).build();
    assertFalse(withoutFallback.isUseDefaultLocaleFallback());
  }

  @Test
  void testUseDefaultLocaleFallback_keyMissingInRequestedLocale() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withDefaultLocale(Locale.ENGLISH).withUseDefaultLocaleFallback(true).build();
    // "english.only.key" exists in test_en.properties but not in test_it.properties
    assertEquals("Only in English", source.msg("english.only.key", new Context(Locale.ITALIAN)));
  }

  @Test
  void useDefaultLocaleFallback_sameLocaleAsDefault_returnsDirectly() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withDefaultLocale(Locale.ENGLISH).withUseDefaultLocaleFallback(true).build();
    assertEquals("Only in English", source.msg("english.only.key", new Context(Locale.ENGLISH)));
  }

  @Test
  void cacheStoresBundlesForMultipleLocales_behaviorVerified() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();
    source.clearCache();

    assertEquals("Benvenuto Mario!", source.msg("welcome.message", new Context(Locale.ITALIAN), "Mario"));
    assertEquals("Welcome John!", source.msg("welcome.message", new Context(Locale.ENGLISH), "John"));
    // Second calls use cache — values must be stable
    assertEquals("Benvenuto Mario!", source.msg("welcome.message", new Context(Locale.ITALIAN), "Mario"));
    assertEquals("Welcome John!", source.msg("welcome.message", new Context(Locale.ENGLISH), "John"));
  }

  @Test
  void messageBundleRemainsAccessibleAfterClearCache() {
    assertEquals("Welcome Alice!", messageSource.msg("welcome.message", new Context(Locale.ENGLISH), "Alice"));
    messageSource.clearCache();
    assertEquals("Welcome Alice!", messageSource.msg("welcome.message", new Context(Locale.ENGLISH), "Alice"));
  }

  @Test
  void negativeCaching_consecutiveCallsForMissingBundleReturnConsistentResult() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource("messages.nonexistent").build();
    Context context = new Context(Locale.ENGLISH);
    assertEquals("{any.key}", source.msg("any.key", context));
    assertEquals("{any.key}", source.msg("any.key", context));
  }

  @Test
  void concurrentAccessReturnsConsistentResults() throws InterruptedException {
    ResourceBundleMessageSource source =
        ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).withDefaultLocale(Locale.ENGLISH).build();
    source.clearCache();

    int numberOfThreads = 20;
    int iterationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    Context context = new Context(Locale.ENGLISH);
    ConcurrentHashMap<String, Boolean> seenValues = new ConcurrentHashMap<>();

    for (int i = 0; i < numberOfThreads; i++) {
      executor.submit(() -> {
        try {
          for (int j = 0; j < iterationsPerThread; j++) {
            seenValues.put(source.msg("test.foo", context), Boolean.TRUE);
          }
        } finally {
          latch.countDown();
        }
      });
    }

    assertTrue(latch.await(10, TimeUnit.SECONDS), "Threads did not complete in time");
    executor.shutdown();
    assertThat(seenValues).as("all threads must see the same value").hasSize(1).containsKey("bar");
  }
}
