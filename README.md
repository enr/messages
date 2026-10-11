# Library messages

![CI](https://github.com/enr/messages/workflows/CI/badge.svg)

[![](https://jitpack.io/v/enr/messages.svg)](https://jitpack.io/#enr/messages)

Java library to manage messages in your app.

## Usage

To get this project into your build:

Add the JitPack repository to your build file

```xml
<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Add the dependency

```xml
<dependency>
  <groupId>com.github.enr</groupId>
  <artifactId>messages</artifactId>
  <version>0.2.0</version>
</dependency>
```

Declare messages:

```properties
welcome.message=Welcome {0}!
```

Usage:

```java
// create the source
ResourceBundleMessageSource source = ResourceBundleMessageSource
    .forResource(RESOURCE_BUNDLE_NAME)
    .withFallbackResource(FALLBACK_BUNDLE_NAME).build();
// create a context
Context context = new Context(Locale.ENGLISH);
// get message
String message = source.msg("welcome.message", context, "John");
```

Messages with arguments are formatted with `java.text.MessageFormat`, where an apostrophe must be written as `''`
(e.g. `user.named=L''utente {0}`). By default, messages without arguments are returned as-is: use
`.withAlwaysUseMessageFormat(true)` to apply the same rules in both cases.

Locales without a bundle are not resolved against the JVM default locale: enable
`.withUseDefaultLocaleFallback(true)` to fall back to the locale set with `.withDefaultLocale(...)`.

## Development

Build:

```
mvn install
```

Full check (test and formatting):

```
mvn -Pci
```

Repair formatting:

```
mvn -Pfmt
```

Fast build (skip any check and file generation):

```
mvn -Pfast
```
