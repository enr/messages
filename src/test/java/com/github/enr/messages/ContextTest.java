package com.github.enr.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class ContextTest {

  @Test
  void nullLocaleThrowsIllegalArgumentException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Context(null));
    assertEquals("Locale must not be null.", ex.getMessage());
  }

  @Test
  void getLocaleReturnsConstructorValue() {
    Locale locale = Locale.FRENCH;
    Context context = new Context(locale);
    assertEquals(locale, context.getLocale());
  }
}
