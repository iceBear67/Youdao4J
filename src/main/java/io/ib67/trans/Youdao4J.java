package io.ib67.trans;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Translator from YouDao DEMO
 *
 * @author iceBear
 */
public final class Youdao4J {
    private static final String API_URL = "https://fanyi.youdao.com/translate_o?smartresult=dict&smartresult=rule";
    private static final String CLIENT = "fanyideskweb";
    private static final String VERSION = "2.1";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Pattern FANYIJS_REGEX = Pattern.compile("(https:\\/\\/shared.ydstatic.com\\/.*fanyi.min.js)");
    private static final Pattern TOKEN_REGEX = Pattern.compile("(n.md5\\(\"fanyideskweb\".+?\")(.+?)(\"\\) } )");
    private final HttpClient httpClient;
    private final String userAgent;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private String token; // lesser thread-safe

    private Youdao4J(HttpClient httpClient, long cachingTime, String userAgent) {
        this.httpClient = httpClient;
        this.userAgent = userAgent;
        var crawler = new Crawler();
        crawler.run();
        executorService.scheduleAtFixedRate(crawler, 1000L, cachingTime, TimeUnit.MILLISECONDS);

        // fetch cookies
        try {
            httpClient.send(HttpRequest.newBuilder(URI.create("https://fanyi.youdao.com")).GET().build(), HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException t) {
            t.printStackTrace();
        }
    }

    /**
     * Getting a translator from default arguments.
     *
     * @return
     */
    public static Youdao4J fromDefault() {
        return LazyLoading.translator;
    }

    /**
     * Build a translator
     *
     * @param client      specified http client
     * @param cachingTime how long should we cache the token
     * @param userAgent   userAgent used for client
     * @return translator
     */
    public static Youdao4J from(HttpClient.Builder client, Duration cachingTime, String userAgent) {
        return new Youdao4J(client.cookieHandler(new CookieManager()).build(), cachingTime.toMillis(), userAgent);
    }

    /**
     * Build a translator. You will need to configure cookiehandlers by yourself.
     *
     * @param client      specified http client
     * @param cachingTime how long should we cache the token
     * @return translator
     */
    public static Youdao4J from(HttpClient client, Duration cachingTime) {
        return new Youdao4J(client, cachingTime.toMillis(), "Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0");
    }

    private static String processResponse(String resp, int stringBuilderCapacity) {
        var jo = JsonParser.parseString(resp);
        var result = new StringBuilder(stringBuilderCapacity);
        for (JsonElement _translateResult : jo.getAsJsonObject().getAsJsonArray("translateResult")) {
            var arr = _translateResult.getAsJsonArray();
            for(JsonElement translateResult :arr){
                var tran = GSON.fromJson(translateResult, Translation.class);
                result.append(tran.translated.isEmpty() ? "\n" : tran.translated);
            }
        }
        return result.toString();
    }

    private static String escapeChars(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * Translating texts.
     *
     * @param from         from which language. AUTO is available.
     * @param to           to which language.
     * @param originalText text to be translated.
     * @return Translation. NotNull
     */
    public String translate(LanguageType from, LanguageType to, String originalText) {
        synchronized (this) {
            if (token == null) {
                throw new TranslationException("Token is null.");
            }
        }
        try {
            var resp = httpClient.send(buildRequest(from, to, originalText), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new TranslationException("Our expectation is 200 but it's " + resp.statusCode());
            }
            return processResponse(resp.body(), originalText.length());
        } catch (IOException | InterruptedException e) {
            var exc = new TranslationException("Can't translate");
            exc.initCause(e);
            throw exc;
        }
    }

    /**
     * Translate Asynchronously with a default timeout (10s)
     *
     * @param from         from which language. AUTO is available.
     * @param to           to which language. AUTO is available.
     * @param originalText text to be translated.
     */
    public CompletableFuture<String> translateAsync(LanguageType from, LanguageType to, String originalText) {
        return translateAsync(from, to, 10, originalText);
    }

    /**
     * Translate Asynchronously.
     *
     * @param from         from which language. AUTO is available.
     * @param to           to which language.
     * @param timeOutSec   time to be timeout
     * @param originalText text to be translated.
     */
    public CompletableFuture<String> translateAsync(LanguageType from, LanguageType to, int timeOutSec, String originalText) {
        synchronized (this) {
            if (token == null) {
                throw new TranslationException("Token is null.");
            }
        }
        return httpClient.sendAsync(buildRequest(from, to, originalText), HttpResponse.BodyHandlers.ofString())
                .orTimeout(timeOutSec, TimeUnit.SECONDS)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        return;
                    }
                    if (response.statusCode() != 200) {
                        throw new TranslationException("Our expectation is 200 but it's " + response.statusCode());
                    }
                })
                .thenApply(HttpResponse::body)
                .thenApply(body -> processResponse(body, body.length()));
    }

    private HttpRequest buildRequest(LanguageType from, LanguageType to, String originalText) {
        var payload = new StringBuilder(originalText.length() + 256);
        synchronized (this) {
            payload.append("i=").append(escapeChars(originalText))
                    .append("&from=").append(from.data)
                    .append("&to=").append(to.data)
                    .append("&smartresult=dict&client=").append(CLIENT)
                    .append('&').append(Credential.of(token, originalText))
                    .append("&doctype=json&version=").append(VERSION)
                    .append("&keyfrom=fanyi.web&action=FY_BY_CLICKBUTTION");
        }
        var str = payload.toString();
        //System.out.println(str.replaceAll("\\&","\n"));
        return HttpRequest
                .newBuilder(URI.create(API_URL))
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Referer", "https://fanyi.youdao.com/")
                .header("Origin", "https://fanyi.youdao.com")
                .POST(HttpRequest.BodyPublishers.ofString(str))
                .build();
    }

    /**
     * Language types supported by Youdao
     */
    public enum LanguageType {
        AUTO("AUTO"),

        CHINESE("zh-CNS"),
        ENGLISH("en"),
        KOREAN("ko"),
        JAPANESE("ja"),
        FRENCH("fr"),
        RUSSIA("ru"),
        SPANISH("es"),
        PORTUGUESE("pt"),
        HINDI("hi"),
        ARAB("ar"),
        DANISH("da"),
        GERMAN("de"),
        GREECE("el"),
        FINLAND("fi"),
        ITALY("it"),
        MALAY("ms"),
        VIETNAM("vi"),
        INDONESIA("id"),
        NETHERLAND("nl"),
        THAI("th");

        private String data;

        LanguageType(String data) {
            this.data = data;
        }
    }

    /**
     * Throws when text can't be translated.
     */
    public static class TranslationException extends RuntimeException {
        public TranslationException(String msg) {
            super(msg);
        }
    }

    private static class Translation {
        @SerializedName("tgt")
        private String translated;
        @SerializedName("src")
        private String source;
    }

    public static class Credential {
        private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        // BV : md5(navigator.version)
        private final String bv = "1de9313c44872e4c200c577f99d4c09e"; // constant data. (24/10/2021)
        private String salt;
        private String sign;
        private long time; // lts

        public static Credential of(String token, String translatingText) {
            var cred = new Credential();
            long time = System.currentTimeMillis();
            //long time = 1635049636700L;
            cred.salt = String.valueOf(time) + ThreadLocalRandom.current().nextInt(10);
            //cred.salt = String.valueOf(time)+ "6";
            cred.time = time;
            // Calculate sign
            var msg = CLIENT + translatingText + cred.salt + token;
            //   System.out.println(msg);
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(msg.getBytes(StandardCharsets.UTF_8));
                cred.sign = bytesToHex(digest.digest()).toLowerCase(Locale.ROOT);
            } catch (NoSuchAlgorithmException impossible) {

            }
            return cred;
        }

        private static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("salt=").append(salt)
                    .append("&sign=").append(sign)
                    .append("&lts=").append(time)
                    .append("&bv=").append(bv);
            return sb.toString();
        }
    }

    private static class LazyLoading {
        public static final Youdao4J translator = from(HttpClient.newBuilder(), Duration.ofMinutes(10), "Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0");
    }

    private class Crawler implements Runnable {
        @Override
        public void run() {
            var request = HttpRequest.newBuilder(URI.create("https://fanyi.youdao.com/"))
                    .setHeader("User-Agent", userAgent)
                    .GET().build();
            try {
                var resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    throw new IllegalStateException("Youdao Fanyi's webpage status code isn't 200.");
                }
                var match = FANYIJS_REGEX.matcher(resp.body());
                if (!match.find()) {
                    throw new IllegalStateException("Can't match fanyi.js");
                }
                String fanyiJs = match.group();
                var jsReq = HttpRequest.newBuilder(URI.create(fanyiJs))
                        .setHeader("User-Agent", userAgent)
                        .GET().build();
                var js = httpClient.send(jsReq, HttpResponse.BodyHandlers.ofString()).body();
                var mch = TOKEN_REGEX.matcher(js);
                if (!mch.find()) {
                    throw new IllegalStateException("Can't match token");
                }
                synchronized (this) {
                    token = mch.group(2);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
