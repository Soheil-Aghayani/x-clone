package client;

import javafx.application.Application;

public class launcher {

    public static void main(String[] args) {
        CrashReporter.install();
        EmbeddedBackend.ensureStarted();
        Application.launch(MainApp.class, args);
    }
}
