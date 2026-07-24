package client.ui;

import client.AppFonts;
import client.AppIcons;
import client.media.MediaLibrary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import shared.models.User;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/** Responsive X-style Edit Profile modal with fixed header and scrolling fields. */
public final class EditProfileDialog {
    public record Result(String name, String bio, String location, String website,
                         String birthDate, boolean professional, String avatarUri, String bannerUri) {}

    private EditProfileDialog() {}

    public static Optional<Result> show(Window owner, User user) {
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        if (owner != null) modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Edit profile");

        Result[] result = new Result[1];
        String[] avatarUri = { user.getAvatarUrl() };
        String[] bannerUri = { user.getBannerUrl() };

        Button close = new Button("×");
        close.getStyleClass().add("compose-close-button");
        Label title = new Label("Edit profile");
        title.setFont(AppFonts.fontFor(title.getText(), 21, FontWeight.BOLD));
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button save = new Button("Save");
        save.getStyleClass().add("composer-post-button");
        HBox header = new HBox(12, close, title, headerSpacer, save);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("edit-profile-header");

        VBox fields = new VBox(14);
        fields.setPadding(new Insets(0, 16, 24, 16));

        StackPane banner = new StackPane();
        banner.setMinHeight(194);
        banner.setPrefHeight(194);
        Region bannerFallback = new Region();
        bannerFallback.getStyleClass().add("profile-banner");
        ImageView bannerImage = new ImageView();
        bannerImage.setFitWidth(568);
        bannerImage.setFitHeight(194);
        bannerImage.setPreserveRatio(false);
        applyImage(bannerImage, bannerUri[0]);
        bannerImage.setVisible(bannerImage.getImage() != null);
        bannerFallback.setVisible(bannerImage.getImage() == null);
        Button chooseBanner = roundToolButton("edit-icon.svg", "Change banner");
        Button removeBanner = new Button("×");
        removeBanner.getStyleClass().add("photo-overlay-button");
        HBox bannerTools = new HBox(10, chooseBanner, removeBanner);
        bannerTools.setAlignment(Pos.CENTER);
        banner.getChildren().addAll(bannerFallback, bannerImage, bannerTools);

        Circle avatar = new Circle(55, Color.web("#1d9bf0"));
        avatar.setStroke(Color.WHITE);
        avatar.setStrokeWidth(4);
        applyCircleImage(avatar, avatarUri[0]);
        Button chooseAvatar = roundToolButton("edit-icon.svg", "Change profile photo");
        StackPane avatarPane = new StackPane(avatar, chooseAvatar);
        avatarPane.setMaxSize(116, 116);
        avatarPane.setTranslateY(-34);

        Label imagineTitle = new Label("Edit your photo with Imagine");
        imagineTitle.setFont(AppFonts.fontFor(imagineTitle.getText(), 15, FontWeight.BOLD));
        Label imagineDetail = new Label("Customize yourself in seconds");
        imagineDetail.setTextFill(Color.web("#536471"));
        VBox imagineWords = new VBox(2, imagineTitle, imagineDetail);
        Region imagineSpacer = new Region();
        HBox.setHgrow(imagineSpacer, Priority.ALWAYS);
        Button editPhoto = new Button("Edit Photo");
        editPhoto.setGraphic(AppIcons.icon("edit-icon.svg", 18, "#0f1419"));
        editPhoto.getStyleClass().add("edit-photo-button");
        HBox imagine = new HBox(10, imagineWords, imagineSpacer, editPhoto);
        imagine.setAlignment(Pos.CENTER_LEFT);
        imagine.getStyleClass().add("imagine-card");

        HBox avatarAndImagine = new HBox(18, avatarPane, imagine);
        avatarAndImagine.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(imagine, Priority.ALWAYS);

        TextField name = new TextField(value(user.getDisplayName()));
        TextArea bio = new TextArea(value(user.getBio()));
        bio.setWrapText(true);
        bio.setPrefHeight(96);
        TextField location = new TextField(value(user.getLocation()));
        TextField website = new TextField(value(user.getWebsite()));
        TextField birthDate = new TextField(value(user.getBirthDate()));
        birthDate.setPromptText("Add your birth date");
        Label professionalTitle = new Label("Professional account");
        professionalTitle.setFont(AppFonts.fontFor(professionalTitle.getText(), 16, FontWeight.BOLD));
        Label professionalDescription = new Label(
                "Show professional tools and account information on your profile.");
        professionalDescription.setWrapText(true);
        professionalDescription.setTextFill(Color.web("#536471"));
        professionalDescription.setFont(AppFonts.fontFor(professionalDescription.getText(), 13));
        VBox professionalCopy = new VBox(3, professionalTitle, professionalDescription);
        HBox.setHgrow(professionalCopy, Priority.ALWAYS);

        ToggleButton professional = new ToggleButton();
        professional.setSelected(user.isProfessional());
        professional.setGraphic(new Circle(9, Color.WHITE));
        professional.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        professional.setAccessibleText("Professional account");
        professional.getStyleClass().add("professional-toggle");
        updateProfessionalSwitch(professional);
        professional.selectedProperty().addListener((observable, oldValue, selected) ->
                updateProfessionalSwitch(professional));

        HBox professionalSetting = new HBox(18, professionalCopy, professional);
        professionalSetting.setAlignment(Pos.CENTER_LEFT);
        professionalSetting.getStyleClass().add("professional-setting-row");

        fields.getChildren().addAll(
                banner,
                avatarAndImagine,
                field("Name", name),
                field("Bio", bio),
                field("Location", location),
                field("Website", website),
                field("Birth date", birthDate),
                professionalSetting);

        ScrollPane scroll = new ScrollPane(fields);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("edit-profile-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox card = new VBox(header, scroll);
        card.setMinWidth(600);
        card.setMaxWidth(600);
        card.getStyleClass().addAll("compose-dialog", "edit-profile-dialog");
        StackPane shell = new StackPane(card);
        shell.setPadding(new Insets(10));
        shell.getStyleClass().add("compose-modal-shell");
        Scene scene = new Scene(shell, Color.TRANSPARENT);
        var stylesheet = EditProfileDialog.class.getResource("/styles/twitter.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
        modal.setScene(scene);
        modal.setWidth(620);
        modal.setMinWidth(620);
        modal.setMaxWidth(620);
        double availableHeight = owner == null ? 720 : owner.getHeight() - 50;
        modal.setHeight(Math.max(520, Math.min(720, availableHeight)));

        chooseBanner.setOnAction(event -> chooseImage(modal).ifPresent(uri -> {
            bannerUri[0] = uri;
            applyImage(bannerImage, uri);
            bannerImage.setVisible(true);
            bannerFallback.setVisible(false);
        }));
        removeBanner.setOnAction(event -> {
            bannerUri[0] = null;
            bannerImage.setImage(null);
            bannerImage.setVisible(false);
            bannerFallback.setVisible(true);
        });
        Runnable chooseAvatarAction = () -> chooseImage(modal).ifPresent(uri -> {
            avatarUri[0] = uri;
            applyCircleImage(avatar, uri);
        });
        chooseAvatar.setOnAction(event -> chooseAvatarAction.run());
        editPhoto.setOnAction(event -> chooseAvatarAction.run());

        name.textProperty().addListener((observable, oldText, newText) -> save.setDisable(newText.trim().isEmpty() || newText.length() > 50));
        close.setOnAction(event -> modal.close());
        scene.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ESCAPE) modal.close(); });
        save.setOnAction(event -> {
            result[0] = new Result(name.getText().trim(), bio.getText().trim(), location.getText().trim(),
                    website.getText().trim(), birthDate.getText().trim(), professional.isSelected(),
                    avatarUri[0], bannerUri[0]);
            modal.close();
        });
        modal.setOnShown(event -> {
            if (owner != null) {
                modal.setX(owner.getX() + (owner.getWidth() - modal.getWidth()) / 2);
                modal.setY(owner.getY() + Math.max(20, (owner.getHeight() - modal.getHeight()) / 2));
            }
        });
        modal.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static VBox field(String label, Node control) {
        Label caption = new Label(label);
        caption.getStyleClass().add("edit-field-caption");
        VBox box = new VBox(2, caption, control);
        box.getStyleClass().add("edit-field-box");
        return box;
    }

    private static Button roundToolButton(String icon, String accessibleText) {
        Button button = new Button();
        button.setGraphic(AppIcons.icon(icon, 20, "#ffffff"));
        button.setAccessibleText(accessibleText);
        button.getStyleClass().add("photo-overlay-button");
        return button;
    }

    private static void updateProfessionalSwitch(ToggleButton toggle) {
        toggle.setAlignment(toggle.isSelected() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    }

    private static Optional<String> chooseImage(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selected = chooser.showOpenDialog(owner);
        if (selected == null) return Optional.empty();
        try {
            return Optional.of(MediaLibrary.importFile(selected));
        } catch (IOException exception) {
            System.err.println("Could not import profile image: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private static void applyImage(ImageView view, String uri) {
        if (uri == null || uri.isBlank()) return;
        try { view.setImage(new Image(uri, true)); } catch (IllegalArgumentException ignored) { }
    }

    private static void applyCircleImage(Circle circle, String uri) {
        if (uri == null || uri.isBlank()) return;
        try {
            Image image = new Image(uri, 110, 110, true, true);
            if (!image.isError() && image.getWidth() > 0 && image.getHeight() > 0) {
                circle.setFill(new ImagePattern(image));
            }
        }
        catch (IllegalArgumentException ignored) { }
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
