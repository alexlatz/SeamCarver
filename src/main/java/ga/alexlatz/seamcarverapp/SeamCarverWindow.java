package ga.alexlatz.seamcarverapp;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

public class SeamCarverWindow extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws FileNotFoundException {
        Image img = new Image(new FileInputStream("stretch.jpg"));
        WritableImage writeImg = new WritableImage(img.getPixelReader(), (int)img.getWidth(), (int)img.getHeight());
        ImageView imageView = new ImageView(writeImg);
        Pane pane = new Pane(imageView);
        Scene scene = new Scene(pane, imageView.getImage().getWidth(), imageView.getImage().getHeight());
        SeamCarver carver = new SeamCarver(writeImg);
        final ChangeListener<Number> resizeListener = new ChangeListener<Number>() {
            final Timer timer = new Timer();
            TimerTask task = null;
            final long delayTime = 200;
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if (task != null) task.cancel();
                task = new TimerTask() {
                    @Override
                    public void run() {
                        int horizontalDiff = (int) (imageView.getImage().getWidth() - scene.getWidth());
                        int verticalDiff = (int) (imageView.getImage().getHeight() - scene.getHeight());
                        if (horizontalDiff != 0) {
                            if (horizontalDiff > 0) for (int i = 0; i < horizontalDiff; i++) {
                                carver.removeVerticalSeam(carver.findVerticalSeam());
                            } else carver.addVerticalSeams(carver.findVerticalSeams(Math.abs(horizontalDiff) + 1));
                            System.out.println("changed " + horizontalDiff + " vertical seams");
                        }
                        if (verticalDiff != 0){
                            if (verticalDiff > 0) for (int i = 0; i < verticalDiff; i++) {
                                carver.removeHorizontalSeam(carver.findHorizontalSeam());
                            } else carver.addHorizontalSeams(carver.findHorizontalSeams(Math.abs(verticalDiff) + 1));
                            System.out.println("changed " + verticalDiff + " horizontal seams");
                        }
                        imageView.setImage(carver.image());
                    }
                };
                timer.schedule(task, delayTime);
            }
        };
        primaryStage.setTitle("SeamCarver");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.widthProperty().addListener(resizeListener);
        primaryStage.heightProperty().addListener(resizeListener);
    }
}
