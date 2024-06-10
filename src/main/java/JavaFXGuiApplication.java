import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

public class JavaFXGuiApplication extends Application {
    private TextArea openedFileTextArea;
    private Label countedDurationLabel;
    private TextArea durationTextArea;
    private Button showErrorsButton;
    private final String openedFile = "Opened file:\n";

    private File selectedFile;
    private SimpleStringProperty timeFormat = new SimpleStringProperty("%02d:%02d:%02d"); // Default time format
    private SimpleStringProperty exampleFormat = new SimpleStringProperty("01:20:05"); // Default example format

    private final String LAST_OPENED_FILE_KEY = "lastOpenedFile";

    private Stage errorStage;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Subtitle Duration Counter");

        // Set the application icon
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon.png")));
        primaryStage.getIcons().add(icon);

        GridPane mainPane = new GridPane();
        mainPane.setPadding(new Insets(10));
        mainPane.setHgap(10);
        mainPane.setVgap(10);

        // Column constraints for 45-55% ratio
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(45);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(55);
        mainPane.getColumnConstraints().addAll(col1, col2);

        // First column components
        Button openFileButton = new Button("Open file");
        openFileButton.setStyle("-fx-font-weight: bold;");
        openFileButton.setOnAction(e -> openFile(primaryStage));

        openedFileTextArea = new TextArea();
        openedFileTextArea.setEditable(false);
        openedFileTextArea.setText(openedFile + "null" + "\n");
        openedFileTextArea.setPrefRowCount(4);

        ScrollPane openedFileScrollPane = new ScrollPane(openedFileTextArea);
        openedFileScrollPane.setFitToWidth(true);
        openedFileScrollPane.setFitToHeight(true);
        openedFileScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Disable vertical scrollbar
        openedFileScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Button countDurationButton = new Button("Count duration");
        countDurationButton.setStyle("-fx-font-weight: bold;");
        countDurationButton.setOnAction(e -> countDuration());

        VBox firstColumn = new VBox(10, openFileButton, openedFileScrollPane, countDurationButton);
        firstColumn.setPadding(new Insets(10));
        firstColumn.setStyle("-fx-border-color: gray; -fx-border-radius: 5px;");

        // Second column components
        countedDurationLabel = new Label("Counted duration:");
        countedDurationLabel.setStyle("-fx-font-weight: bold;");

        durationTextArea = new TextArea();
        durationTextArea.setEditable(false);
        durationTextArea.setPrefRowCount(5);

        showErrorsButton = new Button("Show Errors");
        showErrorsButton.setDisable(true);
        showErrorsButton.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
        showErrorsButton.setOnAction(e -> showErrorsWindow(primaryStage));

        ScrollPane durationTextScrollPane = new ScrollPane(durationTextArea);
        durationTextScrollPane.setFitToWidth(true);
        durationTextScrollPane.setFitToHeight(true);
        durationTextScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Disable vertical scrollbar
        durationTextScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox secondColumn = new VBox(10, countedDurationLabel, durationTextScrollPane, showErrorsButton);
        secondColumn.setPadding(new Insets(10));
        secondColumn.setStyle("-fx-border-color: gray; -fx-border-radius: 5px;");

        mainPane.add(firstColumn, 0, 0);
        mainPane.add(secondColumn, 1, 0);

        // Menu Bar
        MenuBar menuBar = new MenuBar();
        Menu optionsMenu = new Menu("Options");
        optionsMenu.setStyle("-fx-border-color: gray; -fx-border-radius: 2px;");
        Menu timeFormatMenu = new Menu("Time Format");

        RadioMenuItem formatOption1 = new RadioMenuItem("01:20:05");
        formatOption1.setSelected(true);
        formatOption1.setOnAction(e -> {
            timeFormat.set("%02d:%02d:%02d");
            exampleFormat.set("01:20:05");
        });

        RadioMenuItem formatOption2 = new RadioMenuItem("01h 20m 05s");
        formatOption2.setOnAction(e -> {
            timeFormat.set("%02dh %02dm %02ds");
            exampleFormat.set("01h 20m 05s");
        });

        ToggleGroup formatGroup = new ToggleGroup();
        formatOption1.setToggleGroup(formatGroup);
        formatOption2.setToggleGroup(formatGroup);

        timeFormatMenu.getItems().addAll(formatOption1, formatOption2);
        optionsMenu.getItems().add(timeFormatMenu);
        menuBar.getMenus().add(optionsMenu);

        VBox root = new VBox(menuBar, mainPane);
        Scene scene = new Scene(root, 560, 250);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Handle the closing of the main window
        primaryStage.setOnCloseRequest(event -> {
            if (errorStage != null) {
                errorStage.close();
            }
        });

        // Restore the last opened file location
        String lastOpenedFile = getLastOpenedFileLocation();
        if (lastOpenedFile != null) {
            selectedFile = new File(lastOpenedFile);
        }
    }

    private void openFile(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");
        fileChooser.setInitialDirectory(new File(getLastOpenedFileLocation()));

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Word files (*.docx, *.doc)", "*.docx", "*.doc");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            selectedFile = file;
            openedFileTextArea.setText(openedFile + selectedFile.getName());
            durationTextArea.setText("");
            showErrorsButton.setDisable(true);
            showErrorsButton.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            saveLastOpenedFileLocation(selectedFile.getParent());
        }
    }

    private void countDuration() {
        if (selectedFile != null) {
            durationTextArea.setText("In progress...");
            new Thread(() -> {
                WordColorParser.ParseResult parseResult = WordColorParser.getDuration(selectedFile.getPath(), timeFormat.get());
                Map<String, String> colorDurationMap = parseResult.getColorDurationMap();
                List<String> colorFormattingErrors = parseResult.getColorFormattingErrors();
                List<String> otherErrors = parseResult.getOtherErrors();

                if (!colorDurationMap.isEmpty()) {
                    // Calculate the maximum width of the color strings
                    int maxColorLength = colorDurationMap.keySet().stream()
                            .mapToInt(color -> ("Color: " + color).length())
                            .max()
                            .orElse(0);

                    StringBuilder builder = new StringBuilder();
                    for (Map.Entry<String, String> entry : colorDurationMap.entrySet()) {
                        String colorText = "Color: " + entry.getKey();
                        String formattedColorText = String.format("%-" + maxColorLength + "s", colorText);
                        builder.append(String.format("%s  ->  %s\n", formattedColorText, entry.getValue()));
                    }

                    if (!colorFormattingErrors.isEmpty() || !otherErrors.isEmpty()) {
                        showErrorsButton.setDisable(false);
                        showErrorsButton.setStyle("-fx-border-color: red; -fx-border-radius: 5px; -fx-font-weight: bold; -fx-text-fill: black; -fx-background-color: #e8c1a7");

                        showErrorsButton.setUserData(getAllErrorMessages(colorFormattingErrors, otherErrors));
                    }

                    durationTextArea.setText(builder.toString().trim());
                } else
                    durationTextArea.setText("Ooopss... File is empty!\nOr reading error occurred...\nPlease, check the formatting\nof the selected file.");
            }).start();
        } else {
            durationTextArea.setText("Select file first!");
        }
    }

    private static String getAllErrorMessages(List<String> colorFormattingErrors, List<String> otherErrors) {
        StringBuilder errorsBuilder = new StringBuilder();
        if (!colorFormattingErrors.isEmpty()) {
            errorsBuilder.append("Text matches pattern, but has no color:\n");
            for (String colorText : colorFormattingErrors) {
                errorsBuilder.append(colorText);
                errorsBuilder.append("\n");
            }
        }

        if (!otherErrors.isEmpty()) {
            errorsBuilder.append("Other found errors:\n");
            for (String error : otherErrors) {
                errorsBuilder.append(error);
                errorsBuilder.append("\n");
            }
        }
        return errorsBuilder.toString();
    }

    private void showErrorsWindow(Stage owner) {
        Image errorIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/error-icon.png")));
        String errorMessages = (String) showErrorsButton.getUserData();

        if (errorMessages != null && !errorMessages.isEmpty()) {
            errorStage = new Stage();
            errorStage.setTitle("Error Messages");
            errorStage.initOwner(owner);

            TextArea errorTextArea = new TextArea();
            errorTextArea.setEditable(false);
            errorTextArea.setText(errorMessages);

            VBox errorPane = new VBox(10, new ScrollPane(errorTextArea));
            errorPane.setPadding(new Insets(10));

            Scene errorScene = new Scene(errorPane, 400, 300);
            errorStage.getIcons().add(errorIcon);
            errorStage.setScene(errorScene);
            errorStage.show();
        }
    }

    private String getLastOpenedFileLocation() {
        Preferences preferences = Preferences.userNodeForPackage(JavaFXGuiApplication.class);
        return preferences.get(LAST_OPENED_FILE_KEY, System.getProperty("user.home"));
    }

    private void saveLastOpenedFileLocation(String filePath) {
        Preferences preferences = Preferences.userNodeForPackage(JavaFXGuiApplication.class);
        preferences.put(LAST_OPENED_FILE_KEY, filePath);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
