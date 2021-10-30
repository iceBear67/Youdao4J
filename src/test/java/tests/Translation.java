package tests;

import io.ib67.trans.Youdao4J;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Translation {
    @Test
    public void testTranslation() {
        var yd = Youdao4J.fromDefault();
        Assertions.assertEquals("Nullcat不能离开他的性照片", yd.translate(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Nullcat cant leave his sexual photos"));
        //System.out.println(yd.translate(Youdao4J.LanguageType.AUTO, Youdao4J.LanguageType.CHINESE, "Youdao4J did lots of works in the background. DO NOT create morr Youdao4J Object. You dont need to worry about token expiration or cookie expiration, which are Youdao4J held automatically."));
    }
}
