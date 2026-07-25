package client;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.regex.Pattern;

public final class AppFonts {
    public static final String LATIN_FAMILY = "Geist";
    public static final String COMPLEX_FAMILY = "Vazirmatn";

    private static final Pattern ARABIC_SCRIPT = Pattern.compile(
            "[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]");
    private static boolean loaded;

    private AppFonts() {}

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        List<String> resources = List.of(
                "/fonts/Geist-Regular.ttf",
                "/fonts/Geist-SemiBold.ttf",
                "/fonts/Vazirmatn-Regular.ttf"
        );

        for (String resource : resources) {
            var url = AppFonts.class.getResource(resource);
            if (url == null || Font.loadFont(url.toExternalForm(), 14) == null) {
                throw new IllegalStateException("Unable to load bundled font: " + resource);
            }
        }
        loaded = true;
    }

    public static Font fontFor(String text, double size) {
        return fontFor(text, size, FontWeight.NORMAL);
    }

    public static Font fontFor(String text, double size, FontWeight weight) {
        String family = usesComplexScript(text) ? COMPLEX_FAMILY : LATIN_FAMILY;
        return Font.font(family, weight, size);
    }

    public static boolean usesComplexScript(String text) {
        return text != null && ARABIC_SCRIPT.matcher(text).find();
    }
}
