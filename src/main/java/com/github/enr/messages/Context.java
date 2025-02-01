package com.github.enr.messages;

import java.util.Locale;

public class Context {
  private final Locale locale;

  public Context(Locale locale) {
    if (locale == null) {
      throw new IllegalArgumentException("Locale must not be null.");
    }
    this.locale = locale;
  }

  public Locale getLocale() {
    return locale;
  }
}
