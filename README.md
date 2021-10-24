# Youdao4J

Translating texts via [Youdao Translator](https://fanyi.youdao.com/) by one class.  
Need GSON.

# Usage
Youdao4J did lots of works in the background. **DO NOT** create the second Youdao4J Object because it's unnecessary. You
don't have to worry about token expiration or cookie expiration, which are Youdao4J hold automatically.

```java
public class Showcase {
    {
        var yd = Youdao4J.fromDefault(); // lazy-loading and cached. Recommended.
        var advancedYd = Youdao4J.from(HttpClient.newBuilder(), Duration.ofMinutes(30), "Firefox UA"); // If you need to specific User-Agent, HTTP Proxy, Cache-Control etc.
        yd.translate(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Nullcat sb"); // Translate synchronously.
        yd.translateAsync(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, translated -> { // Translate Asynchronously. Threads are held by Http Clients
            //...
        }, "Nullcat sb");
        Assertions.assertEquals(Youdao4J.fromDefault(), Youdao4J.fromDefault()); // They're equal.
    }
}
```

# As a dependency

```groovy
repositories {
    maven {
        name = "Youdao4J"
        url = "https://maven.pkg.github.com/iceBear67/Youdao4J"
    }
}
dependencies {
    compileOnly 'io.ib67:Youdao4J:$RELEASE_VERSION'
}
```
