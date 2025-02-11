package com.github.enr.messages;

import java.util.Map;

public interface MessageSource {
  String msg(String key, Context context, Object... args);

  String msg(String key, Object... args);

  Map<String, String> getAllMessagesKeyAndValue(Context context);
}
