package com.github.enr.messages;

public interface MessageSource {
  String msg(String key, Context context, Object... args);

  String msg(String key, Object... args);
}
