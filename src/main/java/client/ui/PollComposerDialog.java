package client.ui;

import client.AppFonts;
import client.AppIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class PollComposerDialog {
    public record PollComposition(String question, List<String> choices, Duration duration) {}
    private PollComposerDialog() {}

    public static Optional<PollComposition> show(Window owner) {
        PollComposition[] result = { null };

        Stage modal = new Stage(StageStyle.TRANSPARENT);
        modal.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) modal.initOwner(owner);

        // ── Card ──────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.getStyleClass().add("compose-dialog");
        card.setMinWidth(500);
        card.setMaxWidth(500);

        // ── Header ───────────────────────────────────────────────────────
        Button close = new Button();
        close.getStyleClass().add("compose-close-button");
        close.setGraphic(AppIcons.icon("x-logo-icon.svg", 14, "#0f1419"));

        Label heading = new Label("Create a poll");
        heading.setFont(AppFonts.fontFor("Create a poll", 18, FontWeight.BOLD));
        heading.setStyle("-fx-text-fill: #0f1419;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(12, close, heading, headerSpacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 10, 16));

        // Separator
        Line sep1 = separator();

        // ── Question ─────────────────────────────────────────────────────
        VBox questionSection = new VBox(6);
        questionSection.setPadding(new Insets(14, 16, 0, 16));
        Label qLabel = new Label("Question");
        qLabel.setFont(AppFonts.fontFor("Question", 12, FontWeight.BOLD));
        qLabel.setStyle("-fx-text-fill: #536471;");
        TextField question = styledField("Ask a question…", false);
        questionSection.getChildren().addAll(qLabel, question);

        // ── Choices ──────────────────────────────────────────────────────
        VBox choicesSection = new VBox(0);
        choicesSection.setPadding(new Insets(16, 16, 0, 16));
        Label cLabel = new Label("Choices");
        cLabel.setFont(AppFonts.fontFor("Choices", 12, FontWeight.BOLD));
        cLabel.setStyle("-fx-text-fill: #536471;");
        cLabel.setPadding(new Insets(0, 0, 8, 0));

        TextField choice1 = styledField("Choice 1", false);
        TextField choice2 = styledField("Choice 2", false);
        TextField choice3 = styledField("Choice 3 (optional)", true);
        TextField choice4 = styledField("Choice 4 (optional)", true);

        VBox choiceRows = new VBox(0, choiceRow(choice1, null), choiceRow(choice2, null),
                choiceRow(choice3, null), choiceRow(choice4, null));
        choiceRows.getStyleClass().add("poll-choices-box");
        choicesSection.getChildren().addAll(cLabel, choiceRows);

        // ── Duration ─────────────────────────────────────────────────────
        VBox durationSection = new VBox(8);
        durationSection.setPadding(new Insets(16, 16, 0, 16));
        Label dLabel = new Label("Poll duration");
        dLabel.setFont(AppFonts.fontFor("Poll duration", 12, FontWeight.BOLD));
        dLabel.setStyle("-fx-text-fill: #536471;");

        String[] durations = { "1 hour", "1 day", "3 days", "7 days" };
        String[] selectedDuration = { "1 day" };
        HBox durationPills = new HBox(8);
        durationPills.setAlignment(Pos.CENTER_LEFT);
        for (String dur : durations) {
            Button pill = new Button(dur);
            pill.getStyleClass().add(dur.equals(selectedDuration[0]) ? "poll-duration-pill-selected" : "poll-duration-pill");
            pill.setOnAction(e -> {
                selectedDuration[0] = dur;
                durationPills.getChildren().forEach(n -> {
                    n.getStyleClass().remove("poll-duration-pill-selected");
                    n.getStyleClass().remove("poll-duration-pill");
                    n.getStyleClass().add(((Button) n).getText().equals(dur)
                            ? "poll-duration-pill-selected" : "poll-duration-pill");
                });
            });
            durationPills.getChildren().add(pill);
        }
        durationSection.getChildren().addAll(dLabel, durationPills);

        // ── Footer ───────────────────────────────────────────────────────
        Line sep2 = separator();
        sep2.startXProperty().bind(card.widthProperty());
        sep2.setTranslateY(0);

        Label charCount = new Label("");
        charCount.setFont(AppFonts.fontFor("", 13));
        charCount.setStyle("-fx-text-fill: #536471;");

        Label errorLabel = new Label("");
        errorLabel.setFont(AppFonts.fontFor("", 12));
        errorLabel.setStyle("-fx-text-fill: #f4212e;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("audience-pill");
        cancel.setStyle("-fx-border-color: #cfd9de; -fx-background-color: white; -fx-text-fill: #0f1419;");

        Button post = new Button("Post poll");
        post.getStyleClass().add("composer-post-button");
        post.setDisable(true);

        HBox footer = new HBox(10, errorLabel, footerSpacer, charCount, cancel, post);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 16, 14, 16));

        card.getChildren().addAll(header, sep1, questionSection, choicesSection, durationSection, sep2, footer);

        // ── Validation ───────────────────────────────────────────────────
        Runnable validate = () -> {
            String q = question.getText();
            int qLen = q == null ? 0 : q.length();
            charCount.setText(qLen + " / 280");
            boolean qOk = qLen > 0 && qLen <= 280;
            boolean c1Ok = !choice1.getText().isBlank() && choice1.getText().length() <= 25;
            boolean c2Ok = !choice2.getText().isBlank() && choice2.getText().length() <= 25;
            boolean c3Ok = choice3.getText().isBlank() || choice3.getText().length() <= 25;
            boolean c4Ok = choice4.getText().isBlank() || choice4.getText().length() <= 25;

            String err = "";
            if (qLen > 280) err = "Question too long (max 280 chars)";
            else if (choice1.getText().length() > 25 || choice2.getText().length() > 25
                    || choice3.getText().length() > 25 || choice4.getText().length() > 25)
                err = "Choices must be 25 characters or fewer";

            errorLabel.setText(err);
            boolean hasErr = !err.isBlank();
            errorLabel.setManaged(hasErr);
            errorLabel.setVisible(hasErr);

            post.setDisable(!(qOk && c1Ok && c2Ok && c3Ok && c4Ok));
        };

        List.of(question, choice1, choice2, choice3, choice4).forEach(f ->
                f.textProperty().addListener((o, oldV, newV) -> validate.run()));
        validate.run();

        // ── Actions ──────────────────────────────────────────────────────
        close.setOnAction(e -> modal.close());
        cancel.setOnAction(e -> modal.close());
        post.setOnAction(e -> {
            List<String> choices = List.of(choice1, choice2, choice3, choice4)
                    .stream().map(TextField::getText).map(String::trim).filter(v -> !v.isBlank()).toList();
            Duration dur = switch (selectedDuration[0]) {
                case "1 hour" -> Duration.ofHours(1);
                case "3 days" -> Duration.ofDays(3);
                case "7 days" -> Duration.ofDays(7);
                default -> Duration.ofDays(1);
            };
            result[0] = new PollComposition(question.getText().trim(), choices, dur);
            modal.close();
        });

        // ── Scene & stage ─────────────────────────────────────────────────
        StackPane shell = new StackPane(card);
        shell.setPadding(new Insets(10));
        shell.setAlignment(Pos.CENTER);
        shell.getStyleClass().add("compose-modal-shell");
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Scene scene = new Scene(shell, Color.TRANSPARENT);
        var stylesheet = PollComposerDialog.class.getResource("/styles/twitter.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
        scene.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ESCAPE) modal.close(); });
        modal.setScene(scene);
        modal.setMinWidth(520);
        modal.setMaxWidth(520);
        modal.setOnShown(ev -> {
            modal.sizeToScene();
            if (owner != null) {
                modal.setX(owner.getX() + (owner.getWidth() - modal.getWidth()) / 2);
                modal.setY(owner.getY() + Math.max(24, (owner.getHeight() - modal.getHeight()) / 3));
            }
            question.requestFocus();
        });
        modal.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static TextField styledField(String prompt, boolean optional) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add(optional ? "poll-field-optional" : "poll-field-required");
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private static HBox choiceRow(TextField field, Void unused) {
        HBox row = new HBox(field);
        HBox.setHgrow(field, Priority.ALWAYS);
        row.getStyleClass().add("poll-choice-row");
        return row;
    }

    private static Line separator() {
        Line line = new Line(0, 0, 500, 0);
        line.setStroke(Color.web("#eff3f4"));
        line.setStrokeWidth(1);
        return line;
    }
}
