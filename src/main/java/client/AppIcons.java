package client;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AppIcons {
    private record PathData(String content, FillRule fillRule) {}
    private record IconData(double width, double height, List<PathData> paths) {}

    private static final Map<String, IconData> CACHE = new ConcurrentHashMap<>();

    private AppIcons() {}

    public static Node icon(String filename, double size, String color) {
        return icon(filename, size, Color.web(color));
    }

    public static Node icon(String filename, double size, Paint color) {
        IconData data = CACHE.computeIfAbsent(filename, AppIcons::loadIcon);
        Group paths = new Group();
        for (PathData pathData : data.paths()) {
            SVGPath path = new SVGPath();
            path.setContent(pathData.content());
            path.setFillRule(pathData.fillRule());
            path.setFill(color);
            paths.getChildren().add(path);
        }

        double scale = size / Math.max(data.width(), data.height());
        paths.setScaleX(scale);
        paths.setScaleY(scale);
        Bounds bounds = paths.getBoundsInParent();
        paths.setTranslateX((size - bounds.getWidth()) / 2 - bounds.getMinX());
        paths.setTranslateY((size - bounds.getHeight()) / 2 - bounds.getMinY());

        Pane container = new Pane(paths);
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        return container;
    }

    private static IconData loadIcon(String filename) {
        try (InputStream input = AppIcons.class.getResourceAsStream("/icons/" + filename)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing icon resource: " + filename);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Element svg = factory.newDocumentBuilder().parse(input).getDocumentElement();
            String[] viewBox = svg.getAttribute("viewBox").trim().split("\\s+");
            double width = viewBox.length == 4 ? Double.parseDouble(viewBox[2]) : 24;
            double height = viewBox.length == 4 ? Double.parseDouble(viewBox[3]) : 24;

            List<PathData> paths = new ArrayList<>();
            NodeList pathElements = svg.getElementsByTagName("path");
            for (int index = 0; index < pathElements.getLength(); index++) {
                Element path = (Element) pathElements.item(index);
                FillRule fillRule = "evenodd".equalsIgnoreCase(path.getAttribute("fill-rule"))
                        ? FillRule.EVEN_ODD
                        : FillRule.NON_ZERO;
                paths.add(new PathData(path.getAttribute("d"), fillRule));
            }
            return new IconData(width, height, List.copyOf(paths));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load SVG icon " + filename, exception);
        }
    }
}
