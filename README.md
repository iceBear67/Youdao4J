# Youdao4J

Translating texts via [Youdao Translator](https://fanyi.youdao.com/) by one class.  
Need GSON.

# Usage

```java
public class Showcase {
    {
        var yd = Youdao4J.fromDefault();
        var advancedYd = Youdao4J.from(HttpClient.newBuilder(), Duration.ofMinutes(30), "Firefox UA");
        yd.translate(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Nullcat sb");
        yd.translateAsync(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, translated -> {
            //...
        }, "Nullcat sb");
        Assertions.assertEquals(Youdao4J.fromDefault(), Youdao4J.fromDefault());
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
