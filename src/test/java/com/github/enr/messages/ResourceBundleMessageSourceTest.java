package com.github.enr.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class ResourceBundleMessageSourceTest {

  private static final String RESOURCE_BUNDLE_NAME = "messages.test";
  private static final String FALLBACK_BUNDLE_NAME = "messages.fallback";


  private ResourceBundle mockMainBundle = Mockito.mock(ResourceBundle.class);


  private ResourceBundle mockFallbackBundle = Mockito.mock(ResourceBundle.class);

  private ResourceBundleMessageSource messageSource;

  @BeforeEach
  void setUp() {
    // A default message source for convenience in some tests
    messageSource = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withFallbackResource(FALLBACK_BUNDLE_NAME).withDefaultLocale(Locale.ENGLISH).build();
  }

  @Test
  void testValidMessage() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();
    Context context = new Context(Locale.ENGLISH);
    String result = source.msg("welcome.message", context, "John");
    assertEquals("Welcome John!", result, "Message should be formatted correctly.");
  }

  @Test
  void testValidMessageWithFallback() {
    Context context = new Context(Locale.ENGLISH);
    String result = messageSource.msg("test.message.both", context);
    assertEquals("messages", result, "Message should be taken from the main resource bundle.");
    result = messageSource.msg("test.message.only-fallback", context);
    assertEquals("fallback", result, "Message should be taken from the fallback resource bundle.");
  }

  @Test
  void testValidMessageWithFallbackAndCustomLocale() {
    Locale customLocale = Locale.forLanguageTag("it");

    ResourceBundleMessageSource source =
        ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).withDefaultLocale(customLocale)
            .withFallbackResource(FALLBACK_BUNDLE_NAME).build();

    String result = source.msg("test.message.both");
    assertEquals("messages italiano", result, "Message should be taken from the main resource bundle.");
    result = source.msg("test.message.only-fallback");
    assertEquals("fallback italiano", result, "Message should be taken from the fallback resource bundle.");

    Context context = new Context(customLocale);
    Map<String, String> allMessages = source.getAllMessagesKeyAndValue(context);
    assertThat(allMessages).as("get all messages result").hasSize(4)
        .containsEntry("welcome.message", "Benvenuto {0}!").containsEntry("test.foo", "bar")
        .containsEntry("test.message.both", "messages italiano")
        .containsEntry("test.message.only-fallback", "fallback italiano");
  }

  @Test
  void testMissingKeyWithDefaultMissingKeyStrategy() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource("messages.test").build();
    Context context = new Context(Locale.ENGLISH);
    String result = source.msg("non.existent.key", context);
    assertEquals("{non.existent.key}", result, "Default missing key strategy should handle this.");
  }

  @Test
  void testGetAllMessages() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();
    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result").hasSize(3)
        .containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages");
  }

  @Test
  void testGetAllMessagesWithFallback() {
    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = messageSource.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result").hasSize(4)
        .containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages")
        .containsEntry("test.message.only-fallback", "fallback");
  }

  @Test
  void testGetAllMessagesWithMainBundleNotFound() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource("messages.nonexistent")
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build();
    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result when main bundle not found").hasSize(2)
        .containsEntry("test.message.both", "fallback")
        .containsEntry("test.message.only-fallback", "fallback");
  }

  @Test
  void testGetAllMessagesWithFallbackNotFound() {
    ResourceBundleMessageSource source =
        ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).withFallbackResource("messages.nonexistent")
            .build();
    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result when fallback bundle not found").hasSize(3)
        .containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages");
  }

  @Test
  void testNullKey() {
    Context context = new Context(Locale.ENGLISH);
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> messageSource.msg(null, context),
            "Passing a null key should throw an exception.");
    assertEquals("Key and Context must not be null.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource(value = {"welcome.message,John,Welcome John!", "non.existent.key,,{non.existent.key}"})
  void testParameterizedMessages(String key, String argument, String expected) {
    Context context = new Context(Locale.ENGLISH);
    String result = messageSource.msg(key, context, argument);
    assertEquals(expected, result);
  }

  // New tests based on issue description

  @Test
  void testPreloadingDefaultLocaleInConstructor() {
    // Since the bundles are loaded from files, we can't easily mock ResourceBundle.getBundle
    // called inside the constructor. We'll verify by checking if the cache is populated.
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withDefaultLocale(Locale.ENGLISH).build();
    // Access cache via reflection to verify pre-loading
    assertNotNull(getCache(source, "mainBundleCache").get(Locale.ENGLISH));
  }

  @Test
  void testCacheMainBundle_FirstAccess() {
    // This test relies on the actual file-based resource bundles
    ResourceBundleMessageSource spiedSource = spy(messageSource);
    Context italianContext = new Context(Locale.ITALIAN);

    // First access for Italian
    spiedSource.msg("test.foo", italianContext);

    // Can't spy on computeIfAbsent's mapping function directly without powermock.
    // So we check if the cache now contains the bundle.
    assertNotNull(getCache(spiedSource, "mainBundleCache").get(Locale.ITALIAN));
  }


  @Test
  void testPriorityMainOverFallback_KeyExistsInBoth() throws Exception {
    ResourceBundleMessageSource spiedSource = spy(new ResourceBundleMessageSource.Builder(RESOURCE_BUNDLE_NAME)
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build());

    Context frenchContext = new Context(Locale.FRENCH);

    doReturn(mockMainBundle).when(spiedSource).getMainBundle(frenchContext);
    doReturn(mockFallbackBundle).when(spiedSource).getFallbackBundle(frenchContext);

    when(mockMainBundle.getString("duplicate.key")).thenReturn("Main Value");
    // fallback bundle is not mocked to return a value for this key, but we verify it's not called.

    String result = spiedSource.getMessageTemplate("duplicate.key", frenchContext);

    assertEquals("Main Value", result);
    verify(mockMainBundle, times(1)).getString("duplicate.key");
    verify(mockFallbackBundle, never()).getString("duplicate.key");
  }

  @Test
  void testFallbackToFallbackBundle_KeyNotInMain() throws Exception {
    ResourceBundleMessageSource spiedSource = spy(new ResourceBundleMessageSource.Builder(RESOURCE_BUNDLE_NAME)
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build());
    Context germanContext = new Context(Locale.GERMAN);

    doReturn(mockMainBundle).when(spiedSource).getMainBundle(germanContext);
    doReturn(mockFallbackBundle).when(spiedSource).getFallbackBundle(germanContext);

    when(mockMainBundle.getString("only.in.fallback"))
        .thenThrow(new MissingResourceException("", "", ""));
    when(mockFallbackBundle.getString("only.in.fallback")).thenReturn("Fallback Value");

    String result = spiedSource.getMessageTemplate("only.in.fallback", germanContext);

    assertEquals("Fallback Value", result);
    verify(mockMainBundle, times(1)).getString("only.in.fallback");
    verify(mockFallbackBundle, times(1)).getString("only.in.fallback");
  }

  @Test
  void testReturnsNullIfKeyNotFoundInAnyBundle() throws Exception {
    ResourceBundleMessageSource spiedSource = spy(new ResourceBundleMessageSource.Builder(RESOURCE_BUNDLE_NAME)
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build());

    Context spanishContext = new Context(Locale.forLanguageTag("es"));

    doReturn(mockMainBundle).when(spiedSource).getMainBundle(spanishContext);
    doReturn(mockFallbackBundle).when(spiedSource).getFallbackBundle(spanishContext);


    when(mockMainBundle.getString("nonexistent.key"))
        .thenThrow(new MissingResourceException("", "", ""));
    when(mockFallbackBundle.getString("nonexistent.key"))
        .thenThrow(new MissingResourceException("", "", ""));

    // The current design returns null from getMessageTemplate, and the MessageSourceBase handles it
    String result = spiedSource.msg("nonexistent.key", spanishContext);
    assertEquals("{nonexistent.key}", result);

    verify(mockMainBundle, times(1)).getString("nonexistent.key");
    verify(mockFallbackBundle, times(1)).getString("nonexistent.key");
  }

  @Test
  void testCacheIsSeparateForDifferentLocales() {
    ResourceBundleMessageSource spiedSource = spy(messageSource);
    spiedSource.clearCache();

    Context italianContext = new Context(Locale.ITALIAN);
    Context frenchContext = new Context(Locale.FRENCH);

    // Access for Italian
    spiedSource.msg("key1", italianContext);
    // Access for French
    spiedSource.msg("key2", frenchContext);
    // Second access for Italian
    spiedSource.msg("key3", italianContext);

    assertTrue(getCache(spiedSource, "mainBundleCache").containsKey(Locale.ITALIAN));
    assertTrue(getCache(spiedSource, "mainBundleCache").containsKey(Locale.FRENCH));
    // Cannot reliably verify count of ResourceBundle.getBundle calls without PowerMock,
    // but we can see that both caches are populated.
  }

  @Test
  void testThreadSafety_ConcurrentAccess() throws InterruptedException {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withDefaultLocale(Locale.ENGLISH).build();
    source.clearCache();

    int numberOfThreads = 20;
    int iterationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    Context italianContext = new Context(Locale.ITALIAN);

    for (int i = 0; i < numberOfThreads; i++) {
      executor.submit(() -> {
        try {
          for (int j = 0; j < iterationsPerThread; j++) {
            source.msg("test.foo", italianContext);
          }
        } finally {
          latch.countDown();
        }
      });
    }

    boolean allThreadsFinished = latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertTrue(allThreadsFinished, "Threads did not complete in time");
    // Test passes if no exceptions (like ConcurrentModificationException) are thrown.
    // And cache should contain exactly one entry for Italian locale
    assertEquals(1, getCache(source, "mainBundleCache").size());
  }

  @Test
  void testClearCacheEmptiesCaches() {
    Context italianContext = new Context(Locale.ITALIAN);
    messageSource.msg("key1", italianContext); // Populate cache

    assertFalse(getCache(messageSource, "mainBundleCache").isEmpty());
    assertFalse(getCache(messageSource, "fallbackBundleCache").isEmpty());

    messageSource.clearCache();

    assertTrue(getCache(messageSource, "mainBundleCache").isEmpty());
    assertTrue(getCache(messageSource, "fallbackBundleCache").isEmpty());
  }

  @SuppressWarnings("unchecked")
  private ConcurrentHashMap<Locale, ResourceBundle> getCache(
      ResourceBundleMessageSource source, String cacheName) {
    try {
      Field cacheField = ResourceBundleMessageSource.class.getDeclaredField(cacheName);
      cacheField.setAccessible(true);
      return (ConcurrentHashMap<Locale, ResourceBundle>) cacheField.get(source);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}