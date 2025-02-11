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

  @Test
  public void testValidMessage() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource(RESOURCE_BUNDLE_NAME,
        MissingKeyStrategy.defaultStrategy(), ErrorHandler.defaultHandler());

    Context context = new Context(Locale.ENGLISH);
    String result = source.msg("welcome.message", context, "John");
    assertEquals("Welcome John!", result, "Message should be formatted correctly.");
  }

  @Test
  public void testMissingKeyWithDefaultMissingKeyStrategy() {
    ResourceBundle mockBundle = Mockito.mock(ResourceBundle.class);
    Mockito.when(mockBundle.getString("non.existent.key"))
        .thenThrow(new MissingResourceException("Key not found", "messages", "non.existent.key"));

    ResourceBundleMessageSource source = new ResourceBundleMessageSource("messages.test",
        MissingKeyStrategy.defaultStrategy(), ErrorHandler.defaultHandler());

    Context context = new Context(Locale.ENGLISH);
    String result = source.msg("non.existent.key", context);
    assertEquals("{non.existent.key}", result, "Default missing key strategy should handle this.");
  }

  @Test
  public void testGetAllMessages() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource(RESOURCE_BUNDLE_NAME,
        MissingKeyStrategy.defaultStrategy(), ErrorHandler.defaultHandler());

    Context context = new Context(Locale.ENGLISH);
    Map<String, String> result = source.getAllMessagesKeyAndValue(context);
    assertThat(result).as("get all messages result").hasSize(2).containsEntry("welcome.message", "Welcome {0}!")
        .containsEntry("test.foo", "bar");
  }

  @Test
  public void testNullKey() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource(RESOURCE_BUNDLE_NAME,
        MissingKeyStrategy.defaultStrategy(), ErrorHandler.defaultHandler());

    Context context = new Context(Locale.ENGLISH);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> source.msg(null, context),
        "Passing a null key should throw an exception.");

    assertEquals("Key and Context must not be null.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource(value = {"welcome.message,John,Welcome John!", "non.existent.key,,{non.existent.key}"})
  public void testParameterizedMessages(String key, String argument, String expected) {
    // ResourceBundle mockBundle = Mockito.mock(ResourceBundle.class);
    // Mockito.when(mockBundle.getString("welcome.message")).thenReturn("Welcome,
    // {0}!");
    // Mockito.when(mockBundle.getString("non.existent.key"))
    // .thenThrow(new MissingResourceException("Key not found", "messages",
    // "non.existent.key"));

    ResourceBundleMessageSource source = new ResourceBundleMessageSource(RESOURCE_BUNDLE_NAME,
        MissingKeyStrategy.defaultStrategy(), ErrorHandler.defaultHandler());

    Context context = new Context(Locale.ENGLISH);
    String result = source.msg(key, context, argument);
    assertEquals(expected, result);
  }
}
