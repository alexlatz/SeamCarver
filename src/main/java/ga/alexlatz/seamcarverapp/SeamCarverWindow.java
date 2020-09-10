package ga.alexlatz.seamcarverapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class SeamCarverWindow extends Application {
    ImageView imageView;
    SeamCarver seamCarver;
    File path;
    ArrayList<ArrayList<Integer>> removalMarked;
    ArrayList<ArrayList<Integer>> preserveMarked;
    final Color preserveColor = new Color(0f, 0f, 1.0f, 0.5f);
    final Color removeColor = new Color(1.0f, 0f, 0f, 0.5f);
    Canvas preserveCanvas;
    Canvas removeCanvas;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Image img = new Image(SeamCarverWindow.class.getResource("/splash.png").toExternalForm());
        WritableImage writeImg = new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight());
        imageView = new ImageView(writeImg);
        Pane pane = new Pane(imageView);
        seamCarver = new SeamCarver(writeImg);
        StackPane stackPane = new StackPane(pane);
        Scene scene = new Scene(stackPane, imageView.getImage().getWidth(), imageView.getImage().getHeight());
        ChangeListener<Number> resizeListener = resizePrep(scene, primaryStage);
        pane.getChildren().add(createMenu(pane, primaryStage, resizeListener));
        primaryStage.setTitle("SeamCarver");
        primaryStage.getIcons().add(new Image(SeamCarverWindow.class.getResource("/icon.png").toExternalForm()));
        primaryStage.setScene(scene);
        primaryStage.show();
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
                        if (removalMarked != null) {
                            removalMarked = seamCarver.getRemovalMarked();
                            resizeCanvas(removeCanvas, removalMarked, removeColor);
                        }
                        if (preserveMarked != null) {
                            preserveMarked = seamCarver.getPreserveMarked();
                            resizeCanvas(preserveCanvas, preserveMarked, preserveColor);
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

    public MenuBar createMenu(Pane pane, Stage primaryStage, ChangeListener<Number> resizeListener) {
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
        CheckMenuItem selectRemoveArea = new CheckMenuItem("Enable Area Selection for Removal");
        selectRemoveArea.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (selectRemoveArea.isSelected()) {
                    if (removeCanvas == null) removeCanvas = new Canvas(seamCarver.width(), seamCarver.height());
                    else if (removeCanvas.getHeight() != seamCarver.height() || removeCanvas.getWidth() != seamCarver.width())
                        resizeCanvas(removeCanvas, removalMarked, removeColor);
                    GraphicsContext gc = removeCanvas.getGraphicsContext2D();
                    pane.getChildren().add(removeCanvas);
                    removeCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent mouseEvent) {
                            createMouseEvent(mouseEvent, removalMarked, removeColor, removeCanvas.getGraphicsContext2D());
                            seamCarver.setRemovalMarked(removalMarked);
                        }
                    });
                    if (removalMarked == null) {
                        removalMarked = new ArrayList<>();
                        for (int y = 0; y < seamCarver.height(); y++) {
                            removalMarked.add(new ArrayList<>());
                        }
                    }
                } else {
                    pane.getChildren().removeAll(removeCanvas);
                }
            }
        });
        CheckMenuItem selectPreserveArea = new CheckMenuItem("Enable Area Selection for Preservation");
        selectPreserveArea.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (selectPreserveArea.isSelected()) {
                    if (preserveCanvas == null) preserveCanvas = new Canvas(seamCarver.width(), seamCarver.height());
                    else if (preserveCanvas.getHeight() != seamCarver.height() || preserveCanvas.getWidth() != seamCarver.width())
                        resizeCanvas(preserveCanvas, preserveMarked, preserveColor);
                    GraphicsContext gc = preserveCanvas.getGraphicsContext2D();
                    pane.getChildren().add(preserveCanvas);
                    preserveCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent mouseEvent) {
                            createMouseEvent(mouseEvent, preserveMarked, preserveColor, preserveCanvas.getGraphicsContext2D());
                            seamCarver.setPreserveMarked(preserveMarked);
                        }
                    });
                    if (preserveMarked == null) {
                        preserveMarked = new ArrayList<>();
                        for (int y = 0; y < seamCarver.height(); y++) {
                            preserveMarked.add(new ArrayList<>());
                        }
                    }
                } else {
                    pane.getChildren().removeAll(preserveCanvas);
                }
            }
        });
        menuEdit.getItems().addAll(changeHeight, changeWidth, selectRemoveArea, selectPreserveArea);
        menuBar.getMenus().addAll(menuFile, menuEdit);
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Mac"))
            menuBar.useSystemMenuBarProperty().set(true);
        return menuBar;
    }

    private void createMouseEvent(MouseEvent mouseEvent, ArrayList<ArrayList<Integer>> marked, Color color, GraphicsContext gc) {
        gc.setFill(color);
        for (int y = 0; y < 10; y++) {
            int newY = (int) (mouseEvent.getY() - 10 + y);
            ArrayList<Integer> yList = marked.get(newY);
            for (int x = 0; x < 10; x++) {
                int newX = (int) (mouseEvent.getX() - 10 + x);
                if (!yList.contains(newX)) {
                    gc.fillRect(newX, newY, 1, 1);
                    yList.add(newX);
                }
            }
        }
    }

    private void resizeCanvas(Canvas canvas, ArrayList<ArrayList<Integer>> marked, Color color) {
        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(color);
        for (int y = 0; y < marked.size(); y++) {
            for (int x : marked.get(y)) {
                gc.fillRect(x, y, 1, 1);
            }
        }
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
