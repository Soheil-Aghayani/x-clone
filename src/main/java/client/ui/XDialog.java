package client.ui;

import client.AppFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Borderless modal used in place of native JavaFX alerts so confirmations
 * remain visually consistent with X on every operating system.
 */
public final class XDialog {
    public enum Result { PRIMARY, SECONDARY, CANCEL }

    private XDialog() {}

    public static boolean confirm(
            Window owner,
            String title,
            String message,
            String confirmLabel,
            boolean destructive
    ) {
        return show(owner, title, message, confirmLabel, destructive, null, false, "Cancel") == Result.PRIMARY;
    }

    public static void info(Window owner, String title, String message) {
        show(owner, title, message, "Got it", false, null, false, null);
    }

    public static Result show(
            Window owner,
            String title,
            String message,
            String primaryLabel,
            boolean primaryDestructive,
            String secondaryLabel,
            boolean secondaryDestructive,
            String cancelLabel
    ) {
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        if (owner != null) {
            modal.initOwner(owner);
        }
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title);

        Result[] result = {Result.CANCEL};
        StackPane root = createContent(
                title,
                message,
                primaryLabel,
                primaryDestructive,
                secondaryLabel,
                secondaryDestructive,
                cancelLabel,
                selected -> {
                    result[0] = selected;
                    modal.close();
                }
        );

        Scene scene = new Scene(root, Color.TRANSPARENT);
        var stylesheet = XDialog.class.getResource("/styles/twitter.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                result[0] = Result.CANCEL;
                modal.close();
                event.consume();
            }
        });
        modal.setScene(scene);
        modal.setResizable(false);
        modal.setOnShown(event -> {
            if (owner != null) {
                modal.setX(owner.getX() + (owner.getWidth() - modal.getWidth()) / 2);
                modal.setY(owner.getY() + Math.max(24, (owner.getHeight() - modal.getHeight()) / 2.5));
            }
        });
        modal.showAndWait();
        return result[0];
    }

    static StackPane createContent(
            String title,
            String message,
            String primaryLabel,
            boolean primaryDestructive,
            String secondaryLabel,
            boolean secondaryDestructive,
            String cancelLabel,
            java.util.function.Consumer<Result> onSelect
    ) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(AppFonts.fontFor(title, 22, FontWeight.BOLD));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("x-dialog-title");

        Button close = new Button("×");
        close.setAccessibleText("Close dialog");
        close.getStyleClass().add("x-dialog-close");
        close.setOnAction(event -> onSelect.accept(Result.CANCEL));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(12, titleLabel, headerSpacer, close);
        header.setAlignment(Pos.TOP_LEFT);

        Label messageLabel = new Label(message);
        messageLabel.setFont(AppFonts.fontFor(message, 15));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.getStyleClass().add("x-dialog-message");

        VBox actions = new VBox(10);
        if (primaryLabel != null) {
            Button primary = actionButton(primaryLabel, primaryDestructive);
            primary.setDefaultButton(true);
            primary.setOnAction(event -> onSelect.accept(Result.PRIMARY));
            actions.getChildren().add(primary);
        }
        if (secondaryLabel != null) {
            Button secondary = actionButton(secondaryLabel, secondaryDestructive);
            secondary.setOnAction(event -> onSelect.accept(Result.SECONDARY));
            actions.getChildren().add(secondary);
        }
        if (cancelLabel != null) {
            Button cancel = new Button(cancelLabel);
            cancel.setMaxWidth(Double.MAX_VALUE);
            cancel.setCancelButton(true);
            cancel.getStyleClass().add("x-dialog-secondary");
            cancel.setOnAction(event -> onSelect.accept(Result.CANCEL));
            actions.getChildren().add(cancel);
        }

        VBox card = new VBox(14, header, messageLabel, actions);
        card.setPadding(new Insets(22));
        card.setMinWidth(420);
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.getStyleClass().add("x-dialog-card");

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(22));
        root.getStyleClass().add("x-dialog-shell");
        return root;
    }

    private static Button actionButton(String text, boolean destructive) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add(destructive ? "x-dialog-danger" : "x-dialog-primary");
        return button;
    }
}
