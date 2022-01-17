package tests;

import io.ib67.trans.Youdao4J;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Translation {
    @Test
    public void testTranslation() {
        var yd = Youdao4J.fromDefault();
        Assertions.assertTrue(yd.translate(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Nullcat cant leave his sexual photos").contains("他的性照片"));
    }
}
