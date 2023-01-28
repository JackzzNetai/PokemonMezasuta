package util;

import index.StarTitle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateDatabase {
    private static final String DOMAIN = "https://pokemonmezastar.com";
    private static final String[] PAGES = {"/new/special/", "/new/1.html", "/new/2.html", "/new/3.html", "/new/4.html",
            "/new/st1.html", "/new/st2.html", "/new/st3.html", "/new/st4.html", "/new/st5.html",
            "/new/dc1.html", "/new/dc2.html", "/new/dc3.html", "/new/dc4.html"};

    private static final String DAN_URL_IDENTIFIER = "(?s)(?<=url\" content=\").*?(?=\">)";
    private static final String DAN_NAME_IDENTIFIER = "(?s)(?<=\"[page]\">).*?(?=</a>)";
    private static final String TAG_AREA_IDENTIFIER = "(?s)(?<=タグいちらん</h3>).*?(?=<div class=\"select_box)";
    private static final String TAG_WRAP_DELIMITER = "(?<=</div>)\\s*?(?=<div)";
    private static final String H4_TITLE_IDENTIFIER = "(?s)(?<=<span>).*?(?=</span>)";
    private static final String TAG_DELIMITER = "(?s)(?<=</li>).*?(?=<li)";
    private static final String TAG_CLASS_IDENTIFIER = "(?s)(?<=<li).*?(?=<a)";
    private static final String IMAGE_URI_IDENTIFIER = "(?s)(?<=href=\").*?(?=\")";
    private static final String SERIAL_NUMBER_IDENTIFIER = "(?s)(?<=/)(?:.(?!/))*?(?=\\.)";
    private static final String TAG_NAME_IDENTIFIER = "(?s)(?<=<br>).*?(?=\\v)";
    private static final String EXTENSION_IDENTIFIER = "(?<=\\.)[a-zA-Z]*";

    private static final List<String[]> IMAGE_URI_AND_NAMES = new LinkedList<>();

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final boolean INITIALIZE = true;

    public static void main(String[] args) throws IOException {
        if (INITIALIZE) {
            Files.createDirectories(Constants.IMAGE_DIRECTORY_PATH);
            Files.writeString(Constants.DAN_FILE_PATH, Constants.DAN_FILE_HEADER);
            Files.writeString(Constants.TAG_FILE_PATH, Constants.TAG_FILE_HEADER);
        }

        for (String page : PAGES) {
            System.out.println(page);
            parseHTML(fetchContent(DOMAIN + page));
        }

        System.out.println();
        downloadAllImages();
    }

    // Read the HTML content at the specified url and return the content as a single String
    private static String fetchContent(String url) throws IOException {
        InputStream is = (new URL(url)).openStream();
        byte[] content = is.readAllBytes();
        is.close();
        return new String(content, StandardCharsets.UTF_8);
    }

    private static void parseHTML(String html) throws IOException {
        String danName = extractDanInfo(html);
        extractTagInfo(html, danName);
    }

    /**
     * Extract the name and url of the current Dan from the given {@code html} and write the information into the
     * database
     * @return the name of the Dan
     */
    private static String extractDanInfo(String html) throws IOException {
        String url = getFound(DAN_URL_IDENTIFIER, html);

        String page = url.substring(url.indexOf("/new"));
        String name = getFound(DAN_NAME_IDENTIFIER.replace("[page]", page), html);

        String lineToWrite = name + "," + url + System.lineSeparator();
        Files.writeString(Constants.DAN_FILE_PATH, lineToWrite, StandardOpenOption.WRITE, StandardOpenOption.APPEND);

        return name;
    }

    private static void extractTagInfo(String html, String danName) throws IOException {
        String tagArea = getFound(TAG_AREA_IDENTIFIER, html);
        for (String tagWrap : tagArea.split(TAG_WRAP_DELIMITER)) {
            parseTagWrap(tagWrap, danName);
        }
    }

    private static void parseTagWrap(String tagWrap, String danName) throws IOException {
        String h4Title = getFound(H4_TITLE_IDENTIFIER, tagWrap);

        StarTitle starTitle;
        String starNumber;

        if (h4Title.equals("スーパースターポケモン")) {
            starTitle = StarTitle.スーパースター;
            starNumber = "6★★★★★★";
        } else if (h4Title.equals("スターポケモン")) {
            starTitle = StarTitle.スター;
            starNumber = "5★★★★★";
        } else if (h4Title.equals("★2〜4ポケモン")) { // "〜" is U+301C (used from 1弾 to 4弾)
            starTitle = StarTitle.無し;
            starNumber = "?";
        } else if (h4Title.equals("★2～4ポケモン")) { // "～" is U+FF5E (used from スーパータッグ1弾 onwards)
            starTitle = StarTitle.無し;
            starNumber = "?";
        } else if (h4Title.contains("スペシャルタグ")) {
            starTitle = StarTitle.unknown;
            starNumber = "スペシャル";
        } else {
            return;
        }

        for (String tagContent : tagWrap.split(TAG_DELIMITER)) {
            String name = getFound(TAG_NAME_IDENTIFIER, tagContent);
            if (name.contains("？？？？？")) {  // details not released yet
                return;
            }

            boolean isNew = getFound(TAG_CLASS_IDENTIFIER, tagContent).contains("new");
            String imageUri = getFound(IMAGE_URI_IDENTIFIER, tagContent);
            String serialNumber = getFound(SERIAL_NUMBER_IDENTIFIER, imageUri).replaceFirst("^0+(?!$)", "");  // strip leading zeros

            IMAGE_URI_AND_NAMES.add(new String[] {imageUri, serialNumber});  // serialNumber will be used as the name of local image
            String lineToWrite = serialNumber + "," + name + "," + isNew + "," + danName + "," + starTitle + "," +
                    starNumber + System.lineSeparator();
            Files.writeString(Constants.TAG_FILE_PATH, lineToWrite, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        }
    }

    private static void downloadAllImages() {
        IMAGE_URI_AND_NAMES.parallelStream().forEach(uriAndName -> {
            String extension = getFound(EXTENSION_IDENTIFIER, uriAndName[0]);
            try {
                downloadImage(DOMAIN + uriAndName[0], Constants.IMAGE_DIRECTORY_PATH.resolve(uriAndName[1] + "." + extension));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private static void downloadImage(String uri, Path savePath) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();

        if (savePath.toFile().createNewFile()) {
            CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(savePath));
            System.out.println(savePath);
        }
    }

    private static String getFound(String regex, String content) {
        Matcher matcher = Pattern.compile(regex).matcher(content);
        if (!matcher.find()) {
            throw new IllegalStateException();
        }
        return matcher.group();
    }
}
