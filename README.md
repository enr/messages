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
