package ga.alexlatz.seamcarverapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class SeamCarverWindow extends Application {
    ImageView imageView;
    SeamCarver seamCarver;
    File path;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws FileNotFoundException {
        try {
            Image img = new Image(SeamCarverWindow.class.getResource("/splash.png").toExternalForm());
            WritableImage writeImg = new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight());
            imageView = new ImageView(writeImg);
            Pane pane = new Pane(imageView);
            Scene scene = new Scene(pane, imageView.getImage().getWidth(), imageView.getImage().getHeight());
            seamCarver = new SeamCarver(writeImg);
            ChangeListener<Number> resizeListener = resizePrep(scene, primaryStage);
            createMenu(pane, primaryStage, resizeListener);
            primaryStage.setTitle("SeamCarver");
            primaryStage.getIcons().add(new Image(SeamCarverWindow.class.getResource("/icon.png").toExternalForm()));
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public ChangeListener<Number> resizePrep(Scene scene, Stage primaryStage) {
        final ChangeListener<Number> resizeListener = new ChangeListener<Number>() {
            final Timer timer = new Timer();
            final long delayTime = 200;
            TimerTask task = null;

            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if (task != null) task.cancel();
                task = new TimerTask() {
                    @Override
                    public void run() {
                        int horizontalDiff = (int) (imageView.getImage().getWidth() - scene.getWidth());
                        int verticalDiff = (int) (imageView.getImage().getHeight() - scene.getHeight());
                        if (horizontalDiff != 0) {
                            if (horizontalDiff > 0) seamCarver.removeVerticalSeam(horizontalDiff);
                            else seamCarver.addVerticalSeam(Math.abs(horizontalDiff) + 1);
                            System.out.println("changed " + horizontalDiff + " vertical seams");
                        }
                        if (verticalDiff != 0) {
                            if (verticalDiff > 0) seamCarver.removeHorizontalSeam(verticalDiff);
                            else seamCarver.addHorizontalSeam(Math.abs(verticalDiff) + 1);
                            System.out.println("changed " + verticalDiff + " horizontal seams");
                        }
                        imageView.setImage(seamCarver.image());
                    }
                };
                timer.schedule(task, delayTime);
            }
        };
        primaryStage.widthProperty().addListener(resizeListener);
        primaryStage.heightProperty().addListener(resizeListener);
        return resizeListener;
    }

    public void createMenu(Pane pane, Stage primaryStage, ChangeListener<Number> resizeListener) {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem openFile = new MenuItem("Open...");
        openFile.setAccelerator(KeyCombination.keyCombination("SHORTCUT+O"));
        openFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Choose an image");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
                File file = fileChooser.showOpenDialog(primaryStage);
                try {
                    imageView.setImage(new Image(new FileInputStream(file)));
                    path = file;
                    Image img = imageView.getImage();
                    seamCarver = new SeamCarver(new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight()));
                    primaryStage.widthProperty().removeListener(resizeListener);
                    primaryStage.heightProperty().removeListener(resizeListener);
                    primaryStage.setHeight(seamCarver.height());
                    primaryStage.setWidth(seamCarver.width());
                    primaryStage.widthProperty().addListener(resizeListener);
                    primaryStage.heightProperty().addListener(resizeListener);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        MenuItem saveImage = new MenuItem("Save");
        saveImage.setAccelerator(KeyCombination.keyCombination("SHORTCUT+S"));
        saveImage.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (path != null) {
                    saveFile(path);
                }
            }
        });
        MenuItem saveAsImage = new MenuItem("Save As...");
        saveAsImage.setAccelerator(KeyCombination.keyCombination("SHORTCUT+SHIFT+S"));
        saveAsImage.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
                File save = fileChooser.showSaveDialog(primaryStage);
                saveFile(save);
            }
        });
        MenuItem quit = new MenuItem("Quit");
        quit.setAccelerator(KeyCombination.keyCombination("SHORTCUT+Q"));
        quit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                stop();
            }
        });
        menuFile.getItems().addAll(openFile, saveImage, saveAsImage, quit);
        Menu menuEdit = new Menu("Edit");
        MenuItem changeHeight = new MenuItem("Change Height...");
        changeHeight.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog dialog = new TextInputDialog(String.valueOf(seamCarver.height()));
                dialog.initStyle(StageStyle.UNDECORATED);
                dialog.setTitle("Set Height");
                dialog.setHeaderText("Change the height of the image by entering any number");
                dialog.setContentText("Enter the new height here:");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    int s = seamCarver.height() - Integer.parseInt(result.get());
                    if (s < 0) seamCarver.addHorizontalSeam(Math.abs(s));
                    else seamCarver.removeHorizontalSeam(s);
                    imageView.setImage(seamCarver.image());
                    primaryStage.heightProperty().removeListener(resizeListener);
                    primaryStage.setHeight(seamCarver.height());
                    primaryStage.heightProperty().addListener(resizeListener);
                }
            }
        });
        MenuItem changeWidth = new MenuItem("Change Width...");
        changeWidth.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog dialog = new TextInputDialog(String.valueOf(seamCarver.width()));
                dialog.initStyle(StageStyle.UNDECORATED);
                dialog.setTitle("Set Width");
                dialog.setHeaderText("Change the width of the image by entering any number");
                dialog.setContentText("Enter the new width here:");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    int s = seamCarver.width() - Integer.parseInt(result.get());
                    if (s < 0) seamCarver.addVerticalSeam(Math.abs(s));
                    else seamCarver.removeVerticalSeam(s);
                    imageView.setImage(seamCarver.image());
                    primaryStage.widthProperty().removeListener(resizeListener);
                    primaryStage.setWidth(seamCarver.width());
                    primaryStage.widthProperty().addListener(resizeListener);
                }
            }
        });
        MenuItem preserveSection = new MenuItem("Preserve Section...");
        preserveSection.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //TODO: functionality
            }
        });
        menuEdit.getItems().addAll(changeHeight, changeWidth);
        menuBar.getMenus().addAll(menuFile, menuEdit);
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Mac"))
            menuBar.useSystemMenuBarProperty().set(true);
        pane.getChildren().addAll(menuBar);
    }

    private void saveFile(File save) {
        BufferedImage original = SwingFXUtils.fromFXImage(seamCarver.image(), null);
        BufferedImage bufferedImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        bufferedImage.getGraphics().drawImage(original, 0, 0, null);
        try {
            String ext = save.getName().substring(save.getName().lastIndexOf(".") + 1);
            ImageIO.write(bufferedImage, ext, save);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
