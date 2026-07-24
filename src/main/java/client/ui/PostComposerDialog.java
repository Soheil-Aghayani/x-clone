package client.ui;

import client.AppFonts;
import client.AppIcons;
import client.UserSession;
import client.media.MediaLibrary;
import client.timeline.Post;
import client.timeline.PostStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Line;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import shared.models.User;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.Optional;

/** Borderless X-style modal composer shared by replies and quote posts. */
public final class PostComposerDialog {
    private static final int MAX_LENGTH = 280;

    public enum Mode { POST, REPLY, QUOTE }

    public record Composition(String text, String mediaUri) {}

    private PostComposerDialog() {}

    public static Optional<Composition> showPost(Window owner) {
        return show(owner, null, Mode.POST);
    }

    public static Optional<Composition> show(Window owner, Post original, Mode mode) {
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        if (owner != null) modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(switch (mode) {
            case REPLY -> "Reply";
            case QUOTE -> "Quote";
            default -> "Post";
        });

        Composition[] result = new Composition[1];
        String[] mediaUri = new String[1];

        VBox card = new VBox(12);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.getStyleClass().add("compose-dialog");
        card.setMinWidth(600);
        card.setMaxWidth(600);

        Button close = new Button("×");
        close.getStyleClass().add("compose-close-button");
        close.setAccessibleText("Discard and close");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        Button drafts = new Button("Drafts");
        drafts.getStyleClass().add("compose-drafts-button");
        HBox top = new HBox(close, topSpacer, drafts);
        top.setAlignment(Pos.CENTER_LEFT);

        User user = UserSession.getInstance().getCurrentUser();
        String displayName = user == null || user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? (user == null ? "User" : user.getUsername()) : user.getDisplayName();
        Node avatar = user == null
                ? ProfileHoverCard.avatarNode(client.profile.AccountDirectory.find("user"), 44)
                : ProfileHoverCard.avatarNode(client.profile.AccountDirectory.find(user.getUsername()), 44);

        Label audience = new Label("Everyone");
        audience.setGraphic(AppIcons.icon("chevron-down-icon.svg", 11, "#1d9bf0"));
        audience.setContentDisplay(ContentDisplay.RIGHT);
        audience.setGraphicTextGap(5);
        audience.getStyleClass().add("audience-pill");
        TextArea editor = new TextArea();
        editor.setPromptText(switch (mode) {
            case REPLY -> "Post your reply";
            case QUOTE -> "Add a comment";
            default -> "What’s happening?";
        });
        editor.setWrapText(true);
        editor.setPrefHeight(82);
        editor.setMinHeight(64);
        editor.setMaxHeight(380);
        editor.getStyleClass().add("modal-compose-area");
        if (mode == Mode.POST && user != null) {
            List<DraftItem> list = loadDrafts(user.getUsername());
            if (!list.isEmpty()) {
                DraftItem lastDraft = list.get(list.size() - 1);
                editor.setText(lastDraft.text());
                mediaUri[0] = lastDraft.mediaUri();
            }
        }
        VBox editorColumn = new VBox(5, audience, editor);
        HBox.setHgrow(editorColumn, Priority.ALWAYS);
        HBox editorRow = new HBox(12, avatar, editorColumn);
        editorRow.setAlignment(Pos.TOP_LEFT);

        card.getChildren().add(top);
        if (mode == Mode.REPLY) {
            card.getChildren().addAll(createReplyContext(original), editorRow);
        } else if (mode == Mode.QUOTE) {
            card.getChildren().addAll(editorRow, createPostPreview(original));
        } else {
            card.getChildren().add(editorRow);
        }

        Label attachment = new Label();
        attachment.setTextFill(Color.web("#536471"));
        attachment.getStyleClass().add("compose-attachment");
        attachment.setManaged(false);
        attachment.setVisible(false);
        if (mediaUri[0] != null) {
            attachment.setText(MediaLibrary.displayName(mediaUri[0]));
            attachment.setManaged(true);
            attachment.setVisible(true);
        }

        Button image = toolButton("image-off-icon.svg");
        Button gif = toolButton("gif-in-square-icon.svg");
        Button poll = toolButton("list-filter-icon.svg");
        Button idea = toolButton("lightbulb-icon.svg");
        Button location = toolButton("location-pin-icon.svg");
        Button submit = new Button(mode == Mode.REPLY ? "Reply" : "Post");
        submit.getStyleClass().add("composer-post-button");
        submit.setDisable(mode != Mode.QUOTE);

        Circle progress = new Circle(15, Color.TRANSPARENT);
        progress.setStrokeWidth(2);
        progress.setStroke(Color.web("#fddd3e"));
        progress.getStyleClass().add("character-progress");
        Label count = new Label("20");
        count.getStyleClass().add("character-count");
        StackPane counter = new StackPane(progress, count);
        counter.getStyleClass().add("character-counter-stack");
        counter.setManaged(false);
        counter.setVisible(false);

        image.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Attach an image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File selected = chooser.showOpenDialog(modal);
            if (selected != null) {
                try {
                    mediaUri[0] = MediaLibrary.importFile(selected);
                    attachment.setText(selected.getName());
                    attachment.setStyle("");
                } catch (IOException exception) {
                    mediaUri[0] = null;
                    attachment.setText("Could not attach " + selected.getName());
                    attachment.setStyle("-fx-text-fill: #f4212e;");
                }
                attachment.setManaged(true);
                attachment.setVisible(true);
                submit.setDisable((mode != Mode.QUOTE && editor.getText().isBlank() && mediaUri[0] == null)
                        || editor.getText().length() > MAX_LENGTH);
            }
        });
        gif.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Attach a GIF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GIF images", "*.gif"));
            File selected = chooser.showOpenDialog(modal);
            if (selected != null) {
                try {
                    mediaUri[0] = MediaLibrary.importFile(selected);
                    attachment.setText(selected.getName());
                    attachment.setStyle("");
                } catch (IOException exception) {
                    mediaUri[0] = null;
                    attachment.setText("Could not attach " + selected.getName());
                    attachment.setStyle("-fx-text-fill: #f4212e;");
                }
                attachment.setManaged(true);
                attachment.setVisible(true);
                submit.setDisable((mode != Mode.QUOTE && editor.getText().isBlank() && mediaUri[0] == null)
                        || editor.getText().length() > MAX_LENGTH);
            }
        });

        editor.textProperty().addListener((observable, oldText, newText) -> {
            int remaining = MAX_LENGTH - newText.length();
            count.setText(Integer.toString(remaining));
            boolean showCounter = remaining <= 20;
            boolean showCircle = showCounter && remaining >= -9;
            counter.setManaged(showCounter);
            counter.setVisible(showCounter);
            progress.setManaged(showCircle);
            progress.setVisible(showCircle);
            String warningColor = remaining > 0 ? "#fddd3e" : "#f42330";
            count.setStyle("-fx-text-fill: " + warningColor + ";");
            progress.setStroke(Color.web(warningColor));
            boolean emptyRequiredPost = mode != Mode.QUOTE && newText.isBlank() && mediaUri[0] == null;
            submit.setDisable(emptyRequiredPost || newText.length() > MAX_LENGTH);
            editor.setFont(AppFonts.fontFor(newText, 20));
            editor.setPrefHeight(composerHeight(newText));
            Platform.runLater(modal::sizeToScene);
        });

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        HBox toolbar = new HBox(5, image, gif, poll, idea, location, toolbarSpacer, counter, submit);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("modal-compose-toolbar");
        if (mode == Mode.POST) {
            Label replyPermission = new Label("Everyone can reply");
            replyPermission.setGraphic(globeIcon());
            replyPermission.setGraphicTextGap(6);
            replyPermission.getStyleClass().add("reply-permission-label");
            card.getChildren().add(replyPermission);
        }
        card.getChildren().addAll(attachment, toolbar);

        StackPane shell = new StackPane(card);
        shell.setPadding(new Insets(10));
        shell.setAlignment(Pos.CENTER);
        shell.getStyleClass().add("compose-modal-shell");
        card.setMaxHeight(Region.USE_PREF_SIZE);
        Scene scene = new Scene(shell, Color.TRANSPARENT);
        var stylesheet = PostComposerDialog.class.getResource("/styles/twitter.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
        modal.setScene(scene);
        modal.setMinWidth(620);
        modal.setMaxWidth(620);

        Runnable handleClose = () -> {
            String textContent = editor.getText();
            String media = mediaUri[0];
            boolean hasContent = (textContent != null && !textContent.trim().isEmpty()) || media != null;
            if (mode == Mode.POST && hasContent) {
                Window dialogOwner = shell.getScene() == null ? modal : shell.getScene().getWindow();
                XDialog.Result response = XDialog.show(
                        dialogOwner,
                        "Save post?",
                        "You can save this to send later from your drafts.",
                        "Save",
                        false,
                        "Discard",
                        true,
                        "Cancel"
                );
                if (response == XDialog.Result.PRIMARY) {
                    if (user != null) {
                        List<DraftItem> list = loadDrafts(user.getUsername());
                        list.add(new DraftItem(textContent, media, System.currentTimeMillis()));
                        saveDrafts(user.getUsername(), list);
                    }
                    modal.close();
                } else if (response == XDialog.Result.SECONDARY) {
                    modal.close();
                }
            } else {
                modal.close();
            }
        };

        close.setOnAction(event -> handleClose.run());
        drafts.setOnAction(event -> {
            if (user != null) {
                showDraftsModal(user, editor, mediaUri, attachment, submit);
            }
        });
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) handleClose.run();
        });
        submit.setOnAction(event -> {
            result[0] = new Composition(editor.getText().trim(), mediaUri[0]);
            if (user != null && mode == Mode.POST) {
                List<DraftItem> list = loadDrafts(user.getUsername());
                if (!list.isEmpty()) {
                    list.remove(list.size() - 1);
                    saveDrafts(user.getUsername(), list);
                }
            }
            modal.close();
        });
        modal.setOnShown(event -> {
            modal.sizeToScene();
            modal.setWidth(620);
            if (owner != null) {
                modal.setX(owner.getX() + (owner.getWidth() - modal.getWidth()) / 2);
                modal.setY(owner.getY() + Math.max(24, (owner.getHeight() - modal.getHeight()) / 3));
            }
            editor.requestFocus();
        });
        modal.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static double composerHeight(String text) {
        if (text == null || text.isEmpty()) return 82;
        int visualLines = 0;
        for (String line : text.split("\\R", -1)) visualLines += Math.max(1, (int) Math.ceil(line.length() / 48.0));
        return Math.max(82, Math.min(380, 42 + visualLines * 25.0));
    }

    private static Group globeIcon() {
        Color blue = Color.web("#1d9bf0");
        Circle outline = new Circle(6, Color.TRANSPARENT);
        outline.setStroke(blue);
        outline.setStrokeWidth(1.4);
        Line equator = new Line(-5, 0, 5, 0);
        equator.setStroke(blue);
        equator.setStrokeWidth(1.1);
        Arc meridian = new Arc(0, 0, 3, 6, 90, 180);
        meridian.setType(ArcType.OPEN);
        meridian.setFill(Color.TRANSPARENT);
        meridian.setStroke(blue);
        meridian.setStrokeWidth(1.1);
        Arc meridianRight = new Arc(0, 0, 3, 6, 270, 180);
        meridianRight.setType(ArcType.OPEN);
        meridianRight.setFill(Color.TRANSPARENT);
        meridianRight.setStroke(blue);
        meridianRight.setStrokeWidth(1.1);
        return new Group(outline, equator, meridian, meridianRight);
    }

    private static VBox createReplyContext(Post post) {
        VBox context = new VBox(4);
        context.setPadding(new Insets(0, 0, 2, 56));
        Label author = new Label(post.getAuthorName() + "  @" + post.getAuthorUsername()
                + " · " + PostStore.getInstance().relativeTime(post));
        author.setFont(AppFonts.fontFor(author.getText(), 15, FontWeight.BOLD));
        Label body = new Label(post.getContent());
        body.setWrapText(true);
        body.setMaxWidth(500);
        body.setFont(AppFonts.fontFor(body.getText(), 15));
        Label replying = new Label("Replying to @" + post.getAuthorUsername());
        replying.setTextFill(Color.web("#1d9bf0"));
        replying.setPadding(new Insets(8, 0, 0, 0));
        context.getChildren().addAll(author, body, replying);
        return context;
    }

    public static VBox createPostPreview(Post post) {
        VBox preview = new VBox(7);
        preview.getStyleClass().add("quoted-post-card");
        preview.setMaxWidth(548);
        Label author = new Label(post.getAuthorName() + "  @" + post.getAuthorUsername()
                + " · " + PostStore.getInstance().relativeTime(post));
        author.setFont(AppFonts.fontFor(author.getText(), 14, FontWeight.BOLD));
        preview.getChildren().add(author);
        if (!post.getContent().isBlank()) {
            Label body = new Label(post.getContent());
            body.setWrapText(true);
            body.setMaxWidth(520);
            body.setFont(AppFonts.fontFor(body.getText(), 15));
            preview.getChildren().add(body);
        }
        if (post.getMediaUri() != null) {
            Image mediaImage = MediaLibrary.loadImage(post.getMediaUri());
            if (mediaImage != null) {
                ImageView media = new ImageView(mediaImage);
                media.setFitWidth(520);
                media.setFitHeight(230);
                media.setPreserveRatio(true);
                media.setSmooth(true);
                preview.getChildren().add(media);
            } else {
                Label unavailable = new Label("Media unavailable · " + MediaLibrary.displayName(post.getMediaUri()));
                unavailable.setWrapText(true);
                unavailable.setMaxWidth(520);
                unavailable.setStyle("-fx-background-color: #eff3f4; -fx-background-radius: 12;"
                        + "-fx-text-fill: #536471; -fx-padding: 12;");
                preview.getChildren().add(unavailable);
            }
        }
        return preview;
    }

    private static Button toolButton(String icon) {
        Button button = new Button();
        button.setGraphic(AppIcons.icon(icon, 20, "#1d9bf0"));
        button.getStyleClass().add("tool-icon");
        return button;
    }

    private static final class DraftItem {
        private final String text;
        private final String mediaUri;
        private final long timestamp;
        public DraftItem(String text, String mediaUri, long timestamp) {
            this.text = text;
            this.mediaUri = mediaUri;
            this.timestamp = timestamp;
        }
        public String text() { return text; }
        public String mediaUri() { return mediaUri; }
        public long timestamp() { return timestamp; }
    }

    private static List<DraftItem> loadDrafts(String username) {
        PostStore.DraftData draft = PostStore.getInstance().getDraft(username);
        List<DraftItem> list = new ArrayList<>();
        if (draft != null && draft.text() != null) {
            String txt = draft.text();
            if (txt.startsWith("[") && txt.endsWith("]")) {
                try {
                    DraftItem[] items = new Gson().fromJson(txt, DraftItem[].class);
                    if (items != null) {
                        list.addAll(java.util.Arrays.asList(items));
                    }
                } catch (Exception e) {
                    list.add(new DraftItem(txt, draft.mediaUri(), System.currentTimeMillis()));
                }
            } else if (!txt.isBlank() || draft.mediaUri() != null) {
                list.add(new DraftItem(txt, draft.mediaUri(), System.currentTimeMillis()));
            }
        }
        return list;
    }

    private static void saveDrafts(String username, List<DraftItem> list) {
        if (list.isEmpty()) {
            PostStore.getInstance().clearDraft(username);
        } else {
            String json = new Gson().toJson(list);
            PostStore.getInstance().saveDraft(username, json, null);
        }
    }

    private static void showDraftsModal(User user, TextArea editor, String[] mediaUri, Label attachment, Button submit) {
        Stage draftsStage = new Stage(StageStyle.TRANSPARENT);
        draftsStage.initModality(Modality.APPLICATION_MODAL);
        draftsStage.initOwner(editor.getScene().getWindow());

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16));
        layout.getStyleClass().add("premium-modal");
        var stylesheet = PostComposerDialog.class.getResource("/styles/twitter.css");
        if (stylesheet != null) layout.getStylesheets().add(stylesheet.toExternalForm());

        Label title = new Label("Drafts");
        title.setFont(AppFonts.fontFor("Drafts", 18, FontWeight.BOLD));
        title.setStyle("-fx-text-fill: #0f1419;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("compose-close-button");
        closeBtn.setOnAction(e -> draftsStage.close());

        HBox header = new HBox(title, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox listContainer = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("modal-scroll-pane");
        scrollPane.setPrefHeight(250);

        List<DraftItem> draftsList = loadDrafts(user.getUsername());
        if (draftsList.isEmpty()) {
            Label empty = new Label("No drafts saved yet");
            empty.setStyle("-fx-text-fill: #536471; -fx-alignment: center;");
            empty.setFont(AppFonts.fontFor("No drafts", 15));
            empty.setMinHeight(200);
            empty.setPrefWidth(360);
            listContainer.getChildren().add(empty);
        } else {
            for (int i = 0; i < draftsList.size(); i++) {
                final int idx = i;
                DraftItem item = draftsList.get(i);
                
                HBox row = new HBox(10);
                row.getStyleClass().add("modal-account-row");
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 12, 10, 12));

                VBox details = new VBox(3);
                HBox.setHgrow(details, Priority.ALWAYS);
                
                String snippet = item.text();
                if (snippet.isBlank() && item.mediaUri() != null) {
                    snippet = "[Attached Media]";
                }
                Label snippetLabel = new Label(snippet.length() > 60 ? snippet.substring(0, 60) + "…" : snippet);
                snippetLabel.setFont(AppFonts.fontFor(snippet, 14));
                snippetLabel.setStyle("-fx-text-fill: #0f1419;");
                
                Label timeLabel = new Label("Saved on " + java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
                        .withZone(java.time.ZoneId.systemDefault()).format(Instant.ofEpochMilli(item.timestamp())));
                timeLabel.setFont(AppFonts.fontFor("Time", 11));
                timeLabel.setStyle("-fx-text-fill: #536471;");
                
                details.getChildren().addAll(snippetLabel, timeLabel);

                Button deleteBtn = new Button();
                deleteBtn.getStyleClass().add("icon-button");
                deleteBtn.setGraphic(AppIcons.icon("image-off-icon.svg", 16, "#f4212e"));
                deleteBtn.setOnAction(e -> {
                    draftsList.remove(idx);
                    saveDrafts(user.getUsername(), draftsList);
                    draftsStage.close();
                    showDraftsModal(user, editor, mediaUri, attachment, submit);
                });

                row.getChildren().addAll(details, deleteBtn);
                row.setOnMouseClicked(e -> {
                    if (e.getTarget() == deleteBtn || e.getTarget() == deleteBtn.getGraphic()) return;
                    editor.setText(item.text());
                    mediaUri[0] = item.mediaUri();
                    
                    if (mediaUri[0] != null) {
                        try {
                            attachment.setText(Path.of(java.net.URI.create(mediaUri[0])).getFileName().toString());
                            attachment.setManaged(true);
                            attachment.setVisible(true);
                        } catch (Exception ex) {
                            attachment.setManaged(false);
                            attachment.setVisible(false);
                        }
                    } else {
                        attachment.setManaged(false);
                        attachment.setVisible(false);
                    }
                    
                    draftsList.remove(idx);
                    saveDrafts(user.getUsername(), draftsList);
                    draftsStage.close();
                });

                listContainer.getChildren().add(row);
            }
        }

        layout.getChildren().addAll(header, scrollPane);

        StackPane shell = new StackPane(layout);
        shell.setPadding(new Insets(10));
        shell.setAlignment(Pos.CENTER);
        shell.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(shell, Color.TRANSPARENT);
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
        draftsStage.setScene(scene);
        draftsStage.setWidth(400);
        draftsStage.setHeight(360);
        
        Window owner = editor.getScene().getWindow();
        draftsStage.setOnShown(e -> {
            draftsStage.setX(owner.getX() + (owner.getWidth() - draftsStage.getWidth()) / 2);
            draftsStage.setY(owner.getY() + (owner.getHeight() - draftsStage.getHeight()) / 2);
        });

        draftsStage.showAndWait();
    }
}
