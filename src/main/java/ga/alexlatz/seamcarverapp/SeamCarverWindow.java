package ga.alexlatz.seamcarverapp;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class SeamCarverWindow extends Application {
    ImageView imageView;
    SeamCarver seamCarver;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws FileNotFoundException {
        Image img = new Image(new FileInputStream("stretchb.jpg"));
        WritableImage writeImg = new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight());
        imageView = new ImageView(writeImg);
        Pane pane = new Pane(imageView);
        Scene scene = new Scene(pane, imageView.getImage().getWidth(), imageView.getImage().getHeight());
        seamCarver = new SeamCarver(writeImg);
        ChangeListener<Number> resizeListener = resizePrep(scene, primaryStage);
        createMenu(pane, primaryStage, resizeListener);
        primaryStage.setTitle("SeamCarver");
        primaryStage.setScene(scene);
        primaryStage.show();
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
        openFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Choose an image");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
                File file = fileChooser.showOpenDialog(primaryStage);
                try {
                    imageView.setImage(new Image(new FileInputStream(file)));
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
        saveImage.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            }
        });
        MenuItem saveAsImage = new MenuItem("Save As...");
        menuFile.getItems().addAll(openFile, saveImage, saveAsImage);
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
                    seamCarver.removeHorizontalSeam(seamCarver.height() - Integer.parseInt(result.get()));
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
                    seamCarver.removeVerticalSeam(seamCarver.width() - Integer.parseInt(result.get()));
                    imageView.setImage(seamCarver.image());
                    primaryStage.widthProperty().removeListener(resizeListener);
                    primaryStage.setWidth(seamCarver.width());
                    primaryStage.widthProperty().addListener(resizeListener);
                }
            }
        });
        menuEdit.getItems().addAll(changeHeight, changeWidth);
        Menu menuView = new Menu("View");
        menuBar.getMenus().addAll(menuFile, menuEdit, menuView);
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Mac"))
            menuBar.useSystemMenuBarProperty().set(true);
        pane.getChildren().addAll(menuBar);
    }
}
