# Project Archived

Youdao has deployed a new frontend which uses Webpack and I have no time to make my crawler adapt to it.  
So, this project is permanently ARCHIVED, or I'll back if I need this again.

---

# Youdao4J

Translating texts via [Youdao Translator](https://fanyi.youdao.com/) by one class.    
Need GSON.

# Usage
Youdao4J did lots of works in the background. **DO NOT** create morr Youdao4J Object.   
Token expiration or cookie expiration are held automatically.

```java
import java.util.concurrent.CompletableFuture;

public class Showcase {
    {
        var yd = Youdao4J.fromDefault(); // The default one is lazy-loaded and cached in memory.
        var advancedYd = Youdao4J.from(HttpClient.newBuilder(), Duration.ofMinutes(30), "Firefox UA ..?"); // If you need to specific User-Agent, HTTP Proxy, Cache-Control etc.
        yd.translate(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Nullcat sb"); // Translate synchronously.
        CompletableFuture<String> future = yd.translateAsync(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Nullcat sb"); // Translate Asynchronously. Threads are held by Http Clients
        future.thenAccept(System.out::println); // Print the result.

        Assertions.assertEquals(Youdao4J.fromDefault(), Youdao4J.fromDefault()); // They're equal.
    }
}
```

# Add as dependency

Youdao4J is now on Maven Central.

```groovy
dependencies {
    implementation("io.ib67.trans:youdao4j:$VERSION")
}
```