package ga.alexlatz.seamcarverapp;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class SeamCarver {
    private WritableImage image;
    private double[][] energy;
    private double[][] energyTo;

    public SeamCarver(final WritableImage image) {
        this.image = image;
        energy = new double[width()][height()];
        for (int x = 0; x < width(); x++) {
            for (int y = 0; y < height(); y++) {
                energy[x][y] = energy(x, y);
            }
        }
    }

    public int width() {
        return (int) image.getWidth();
    }

    public int height() {
        return (int) image.getHeight();
    }

    public WritableImage image() {
        return image;
    }

    public double energy(final int x, final int y) {
        checkCoords(x, y);
        if (x == 0 || x == width() - 1 || y == 0 || y == height() - 1)
            return 1000;
        PixelReader reader = image.getPixelReader();
        return Math.sqrt(colorDiff(reader.getArgb(x + 1, y), reader.getArgb(x - 1, y))
                + colorDiff(reader.getArgb(x, y + 1), reader.getArgb(x, y - 1)));
    }
    public int[][] findVerticalSeams(int num) {
        int[][] seams = new int[num][height()];
        WritableImage original = new WritableImage(image.getPixelReader(), width(), height());
        if (num > 0) seams[0] = findVerticalSeam();
        for (int i = 1; i < num; i++) {
            seams[i] = findVerticalSeam();
            removeVerticalSeam(seams[i]);
            for (int j = 0; j < seams[0].length; j++) {
                if (seams[i-1][j] <= seams[i][j]) seams[i][j] += 2;
            }
        }
        image = original;
        return seams;
    }
    public int[][] findHorizontalSeams(int num) {
        transpose();
        int[][] seams = findVerticalSeams(num);
        transpose();
        return seams;
    }

    public int[] findVerticalSeam() {
        energyTo = new double[width()][height()];
        final int[] seam = new int[height()];
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                if (y == 0)
                    energyTo[x][y] = 0;
                else
                    energyTo[x][y] = Double.POSITIVE_INFINITY;
            }
        }
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                relax(x, y, x, y + 1);
                relax(x, y, x + 1, y + 1);
                relax(x, y, x - 1, y + 1);
            }
        }
        double min = Double.POSITIVE_INFINITY;
        if (height() > 1) {
            for (int x = 0; x < width(); x++) {
                if (energyTo[x][height() - 2] < min) {
                    min = energyTo[x][height() - 2];
                    seam[height() - 2] = x;
                }
            }
            seam[height() - 1] = seam[height() - 2];
        } else {
            seam[height() - 1] = 0;
            return seam;
        }
        for (int y = height() - 2; y > 0; y--) {
            double minSeam = Double.POSITIVE_INFINITY;
            if (seam[y] - 1 > 0) {
                if (energyTo[seam[y] - 1][y - 1] < minSeam) {
                    minSeam = energyTo[seam[y] - 1][y - 1];
                    seam[y - 1] = seam[y] - 1;
                }
            }
            if (energyTo[seam[y]][y - 1] < minSeam) {
                minSeam = energyTo[seam[y]][y - 1];
                seam[y - 1] = seam[y];
            }
            if (seam[y] + 1 < width()) {
                if (energyTo[seam[y] + 1][y - 1] < minSeam) {
                    seam[y - 1] = seam[y] + 1;
                }
            }
        }
        return seam;
    }

    public int[] findHorizontalSeam() {
        transpose();
        final int[] seam = findVerticalSeam();
        transpose();
        return seam;
    }

    public void removeVerticalSeam(final int[] seam) {
        if (seam == null || width() <= 1 || seam.length != height())
            throw new IllegalArgumentException();
        checkSeam(seam);
        final WritableImage newPic = new WritableImage(width() - 1, height());
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = newPic.getPixelWriter();
        for (int y = 0; y < height(); y++) {
            int curRow = 0;
            for (int x = 0; x < width(); x++) {
                if (x != seam[y]) writer.setArgb(curRow++, y, reader.getArgb(x, y));
            }
        }
        image = newPic;
        seamEnergy(seam, true);
    }

    public void removeHorizontalSeam(final int[] seam) {
        if (height() <= 1)
            throw new IllegalArgumentException();
        transpose();
        removeVerticalSeam(seam);
        transpose();
    }

    public void addVerticalSeam(final int[] seam) {
        final WritableImage newPic = new WritableImage(width() + 1, height());
        PixelWriter writer = newPic.getPixelWriter();
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height(); y++) {
            int curRow = 0;
            for (int x = 0; x < width(); x++) {
                if (x != seam[y]) writer.setArgb(curRow++, y, reader.getArgb(x, y));
                else {
                    int seamColor = reader.getArgb(seam[y], y);
                    int nextColor;
                    if (x < width()-1) nextColor = reader.getArgb(seam[y] + 1, y);
                    else nextColor = reader.getArgb(seam[y] - 1, y);
                    int a = ((((seamColor >> 24) & 0xff) + ((nextColor >> 24) & 0xff)) / 2);
                    int r = ((((seamColor >> 16) & 0xff) + ((nextColor >> 16) & 0xff)) / 2);
                    int g = ((((seamColor >> 8) & 0xff) + ((nextColor >> 8) & 0xff)) / 2);
                    int newColor = ((((seamColor) & 0xff) + ((nextColor) & 0xff)) / 2);
                    newColor = newColor + (g << 8);
                    newColor = newColor + (r << 16);
                    newColor = newColor + (a << 24);
                    if (x < width() - 1) {
                        writer.setArgb(curRow++, y, seamColor);
                        writer.setArgb(curRow++, y, newColor);
                    } else {
                        writer.setArgb(curRow++, y, newColor);
                        writer.setArgb(curRow++, y, seamColor);
                    }
                }
            }
        }
        image = newPic;
        seamEnergy(seam, false);
    }

    public void addHorizontalSeam(final int[] seam) {
        transpose();
        addVerticalSeam(seam);
        transpose();
    }

    public void addVerticalSeams(int[][] seams) {
        for (int[] seam : seams) {
            addVerticalSeam(seam);
        }
    }

    public void addHorizontalSeams(int[][] seams) {
        transpose();
        addVerticalSeams(seams);
        transpose();
    }

    private void seamEnergy(int[] seam, boolean delete) {
        double[][] newEnergy = new double[width()][height()];
        for (int y = 0; y < height(); y++) {
            System.arraycopy(energy, 0, newEnergy, 0, seam[y]);
            if (!delete) {
                newEnergy[seam[y]][y] = energy(seam[y], y);
                if (seam[y] < width() - 1) {
                    newEnergy[seam[y] + 1][y] = energy(seam[y] + 1, y);
                    newEnergy[seam[y] + 2][y] = energy(seam[y] + 2, y);
                    if (energy.length - (seam[y] + 2) > -1)
                        System.arraycopy(energy, seam[y] + 2, newEnergy, seam[y] + 3, energy.length - (seam[y] + 2));
                } else {
                    newEnergy[seam[y]-1][y] = energy(seam[y]-1, y);
                    newEnergy[seam[y]+1][y] = energy(seam[y]+1, y);
                }
            } else {
                if (seam[y] > 0) newEnergy[seam[y] - 1][y] = energy(seam[y] - 1, y);
                if (seam[y] < width() - 1) {
                    if (seam[y] < width() - 1) newEnergy[seam[y]][y] = energy(seam[y], y);
                    if (seam[y] + 2 < energy.length)
                        System.arraycopy(energy, seam[y] + 2, newEnergy, seam[y] + 1, newEnergy.length - (seam[y] + 1));
                }
            }
        }
        energy = newEnergy;
    }

    private void checkSeam(final int[] seam) {
        for (int i = 1; i < seam.length; i++) {
            if (Math.abs(seam[i] - seam[i - 1]) > 1)
                throw new IllegalArgumentException();
        }
    }

    private void checkCoords(final int x, final int y) {
        if (x < 0 || x >= width() || y < 0 || y >= height())
            throw new IllegalArgumentException();
    }


    private void transpose() {
        final WritableImage flipped = new WritableImage(height(), width());
        final double[][] newEnergy = new double[height()][width()];
        PixelWriter writer = flipped.getPixelWriter();
        PixelReader reader = image.getPixelReader();
        for (int x = 0; x < width(); x++) {
            for (int y = 0; y < height(); y++) {
                writer.setArgb(y, x, reader.getArgb(x, y));
                if (x >= energy.length || y >= energy[0].length) newEnergy[y][x] = energy(x, y);
                else newEnergy[y][x] = energy[x][y];
            }
        }
        image = flipped;
        energy = newEnergy;
    }

    private void relax(final int x, final int y, final int x2, final int y2) {
        if (x2 < 0 || y2 < 0 || x2 > width() - 1 || y2 > height() - 1)
            return;
        if (energyTo[x2][y2] > energyTo[x][y] + energy[x2][y2])
            energyTo[x2][y2] = energyTo[x][y] + energy[x2][y2];
    }

    private double colorDiff(final int c1, final int c2) {
        final double r = ((c1 >> 16) & 0xff) - ((c2 >> 16) & 0xff);
        final double g = ((c1 >> 8) & 0xff) - ((c2 >> 8) & 0xff);
        final double b = ((c1) & 0xff) - ((c2) & 0xff);
        return r * r + g * g + b * b;
    }
}