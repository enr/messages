package com.github.enr.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class ResourceBundleMessageSourceTest {

  private static final String RESOURCE_BUNDLE_NAME = "messages.test";
  private static final String FALLBACK_BUNDLE_NAME = "messages.fallback";

  @Test
  public void testValidMessage() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();

    Context context = new Context(Locale.ENGLISH);
    String result = source.msg("welcome.message", context, "John");
    assertEquals("Welcome John!", result, "Message should be formatted correctly.");
  }

  @Test
  public void testValidMessageWithFallback() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build();

    Context context = new Context(Locale.ENGLISH);
    String result = source.msg("test.message.both", context);
    assertEquals("messages", result, "Message should be taken from the main resource bundle.");
    result = source.msg("test.message.only-fallback", context);
    assertEquals("fallback", result, "Message should be taken from the fallback resource bundle.");

  }

  @Test
  public void testMissingKeyWithDefaultMissingKeyStrategy() {
    ResourceBundle mockBundle = Mockito.mock(ResourceBundle.class);
    Mockito.when(mockBundle.getString("non.existent.key"))
        .thenThrow(new MissingResourceException("Key not found", "messages", "non.existent.key"));

    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource("messages.test").build();

    Context context = new Context(Locale.ENGLISH);
    String result = source.msg("non.existent.key", context);
    assertEquals("{non.existent.key}", result, "Default missing key strategy should handle this.");
  }

  @Test
  public void testGetAllMessages() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();

    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result").hasSize(3).containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages");
  }

  @Test
  public void testGetAllMessagesWithFallback() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build();

    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result").hasSize(4).containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar").containsEntry("test.message.both", "messages")
        .containsEntry("test.message.only-fallback", "fallback");
  }

  @Test
  public void testGetAllMessagesWithMainBundleNotFound() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource("messages.nonexistent")
        .withFallbackResource(FALLBACK_BUNDLE_NAME).build();

    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result when main bundle not found").hasSize(2)
        .containsEntry("test.message.both", "fallback").containsEntry("test.message.only-fallback", "fallback");
  }

  @Test
  public void testGetAllMessagesWithFallbackNotFound() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME)
        .withFallbackResource("messages.nonexistent").build();

    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result when fallback bundle not found").hasSize(3)
        .containsEntry("welcome.message", "Welcome {0}!").containsEntry("test.foo", "bar")
        .containsEntry("test.message.both", "messages");
  }

  @Test
  public void testNullKey() {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();

    Context context = new Context(Locale.ENGLISH);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> source.msg(null, context),
        "Passing a null key should throw an exception.");

    assertEquals("Key and Context must not be null.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource(value = {"welcome.message,John,Welcome John!", "non.existent.key,,{non.existent.key}"})
  public void testParameterizedMessages(String key, String argument, String expected) {
    ResourceBundleMessageSource source = ResourceBundleMessageSource.forResource(RESOURCE_BUNDLE_NAME).build();

    Context context = new Context(Locale.ENGLISH);
    String result = source.msg(key, context, argument);
    assertEquals(expected, result);
  }
}
