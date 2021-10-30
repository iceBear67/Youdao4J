# Youdao4J

Translating texts via [Youdao Translator](https://fanyi.youdao.com/) by one class.  
Need GSON.

# Usage
Youdao4J did lots of works in the background. **DO NOT** create morr Youdao4J Object.   
Token expiration or cookie expiration are held automatically.

```java
public class Showcase {
    {
        var yd = Youdao4J.fromDefault(); // lazy-loaded and cached. Recommended.
        var advancedYd = Youdao4J.from(HttpClient.newBuilder(), Duration.ofMinutes(30), "Firefox UA"); // If you need to specific User-Agent, HTTP Proxy, Cache-Control etc.
        yd.translate(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Nullcat sb"); // Translate synchronously.
        yd.translateAsync(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, translated -> { // Translate Asynchronously. Threads are held by Http Clients
            //...
        }, "Nullcat sb");
        Assertions.assertEquals(Youdao4J.fromDefault(), Youdao4J.fromDefault()); // They're equal.
    }
}
```
