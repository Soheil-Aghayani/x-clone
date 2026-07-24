package client.ui;

import client.AppFonts;
import client.timeline.PollData;
import client.timeline.Post;
import client.timeline.PostStore;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;

import java.time.Duration;

public final class PollView {
    private PollView() {}

    public static VBox create(Post post, Runnable refreshParent) {
        VBox container = new VBox(8);
        container.getStyleClass().add("poll-card");
        refresh(container, post, refreshParent);
        return container;
    }

    private static void refresh(VBox container, Post post, Runnable refreshParent) {
        container.getChildren().clear();
        PollData poll = post.getPoll();
        if (poll == null) return;
        int total = poll.getTotalVotes();
        boolean results = poll.hasVoted() || poll.isEnded();
        for (int index = 0; index < poll.getChoices().size(); index++) {
            String choice = poll.getChoices().get(index);
            if (!results) {
                Button option = new Button(choice);
                option.setMaxWidth(Double.MAX_VALUE);
                option.getStyleClass().add("poll-choice-button");
                int selectedIndex = index;
                option.setOnAction(event -> {
                    if (PostStore.getInstance().vote(post, selectedIndex)) {
                        refresh(container, post, refreshParent);
                        refreshParent.run();
                    }
                });
                container.getChildren().add(option);
            } else {
                int votes = poll.getVotes().get(index);
                double ratio = total == 0 ? 0 : votes / (double) total;
                Label label = new Label(choice);
                label.setFont(AppFonts.fontFor(choice, 14,
                        Integer.valueOf(index).equals(poll.activeChoice()) ? FontWeight.BOLD : FontWeight.NORMAL));
                Label percent = new Label(Math.round(ratio * 100) + "%");
                percent.setTextFill(Color.web("#536471"));
                HBox heading = new HBox(label, percent);
                heading.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(label, Priority.ALWAYS);
                ProgressBar bar = new ProgressBar(ratio);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.getStyleClass().add("poll-result-bar");
                container.getChildren().add(new VBox(3, heading, bar));
            }
        }
        Duration remaining = Duration.between(java.time.Instant.now(), poll.getEndsAt());
        String time = poll.isEnded() ? "Final results" : remaining.toHours() >= 24
                ? Math.max(1, remaining.toDays()) + "d left" : Math.max(1, remaining.toHours()) + "h left";
        Label footer = new Label(total + (total == 1 ? " vote · " : " votes · ") + time);
        footer.setTextFill(Color.web("#536471"));
        container.getChildren().add(footer);
    }
}
