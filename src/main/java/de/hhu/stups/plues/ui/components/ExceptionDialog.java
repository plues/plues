package de.hhu.stups.plues.ui.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;


// Based on http://code.makery.ch/blog/javafx-dialogs-official/
public class ExceptionDialog extends Alert {

    private final StringProperty exceptionText;

    public ExceptionDialog() {
        super(AlertType.ERROR);

        this.exceptionText = new SimpleStringProperty(this, "exceptionText");

        this.setup();
    }

    private void setup() {
        // Create expandable Exception.

        final Label label = new Label("The exception stacktrace was:");

        final TextArea textArea = new TextArea();
        textArea.textProperty().bind(this.exceptionText);

        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        this.getDialogPane().setExpandableContent(expContent);
    }

    public final void setException(final Throwable exception) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        exception.printStackTrace(pw);

        this.setContentText(exception.getMessage());
        this.exceptionText.set(sw.toString());
    }
}
