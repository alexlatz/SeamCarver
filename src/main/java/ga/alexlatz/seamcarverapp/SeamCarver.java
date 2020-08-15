package ga.alexlatz.seamcarverapp;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class SeamCarver {
    private WritableImage image;
    private double[][] energy;

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
        if (x == 0 || x == width() - 1 || y == 0 || y == height() - 1)
            return 1000;
        PixelReader reader = image.getPixelReader();
        return Math.sqrt(colorDiff(reader.getArgb(x + 1, y), reader.getArgb(x - 1, y))
                + colorDiff(reader.getArgb(x, y + 1), reader.getArgb(x, y - 1)));
    }

    public int[][] findVerticalSeam(int num) {
        int[][] seams = new int[height()][num];
        boolean[][] usedPixels = new boolean[height()][width()];
        double[][] energyTo = new double[width()][height()];
        ArrayList<HashSet<Integer>> possibleMoves = new ArrayList<>();
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
                relax(energyTo, x, y, x, y + 1);
                relax(energyTo, x, y, x + 1, y + 1);
                relax(energyTo, x, y, x - 1, y + 1);
            }
        }
        for (int i = 0; i < num; i++) {
            double min = Double.POSITIVE_INFINITY;
            if (height() > 1) {
                for (int x = 0; x < width(); x++) {
                    if (energyTo[x][height() - 2] < min && !usedPixels[height() - 2][x]) {
                        min = energyTo[x][height() - 2];
                        seams[height() - 2][i] = x;
                    }
                }
                seams[height() - 1][i] = seams[height() - 2][i];
                usedPixels[height() - 2][seams[height() - 2][i]] = true;
                usedPixels[height() - 1][seams[height() - 2][i]] = true;
            } else {
                seams[height() - 1][i] = 0;
            }
            possibleMoves.add(new HashSet<>());
            possibleMoves.get(i).add(seams[height() - 2][i]);
            if (seams[height() - 2][i] < width() - 1) possibleMoves.get(i).add(seams[height() - 2][i] + 1);
            if (seams[height() - 2][i] > 0) possibleMoves.get(i).add(seams[height() - 2][i] - 1);
        }
        for (int y = height() - 2; y > 0; y--) {
            for (int i = 0; i < num; i++) {
                double minSeam = Double.POSITIVE_INFINITY;
                if (seams[y][i] - 1 > 0) {
                    if (energyTo[seams[y][i] - 1][y - 1] < minSeam && !usedPixels[y - 1][seams[y][i] - 1] && allowedMove(possibleMoves, i, seams[y][i] - 1)) {
                        minSeam = energyTo[seams[y][i] - 1][y - 1];
                        seams[y - 1][i] = seams[y][i] - 1;
                    }
                }
                if (energyTo[seams[y][i]][y - 1] < minSeam && !usedPixels[y - 1][seams[y][i]] && allowedMove(possibleMoves, i, seams[y][i])) {
                    minSeam = energyTo[seams[y][i]][y - 1];
                    seams[y - 1][i] = seams[y][i];
                }
                if (seams[y][i] + 1 < width()) {
                    if (energyTo[seams[y][i] + 1][y - 1] < minSeam && !usedPixels[y - 1][seams[y][i] + 1] && allowedMove(possibleMoves, i, seams[y][i] + 1)) {
                        seams[y - 1][i] = seams[y][i] + 1;
                    }
                }
                if (seams[y - 1][i] == 0) {
                    System.out.println("bad");
                    int add = 0, sub = 0;
                    while (seams[y][i] + add < width() || !usedPixels[y - 1][seams[y][i] + add]) add++;
                    while (seams[y][i] - sub >= 0 || !usedPixels[y - 1][seams[y][i] - sub]) sub++;
                    if (add > sub) seams[y - 1][i] = seams[y - 1][i] + add;
                    else seams[y - 1][i] = seams[y - 1][i] - sub;
                }
                usedPixels[y - 1][seams[y - 1][i]] = true;
                for (int j = i + 1; j < num; j++) {
                    possibleMoves.get(j).remove(seams[y - 1][i]);
                }
                possibleMoves.get(i).clear();
                possibleMoves.get(i).add(seams[y - 1][i]);
                if (seams[y - 1][i] < width() - 1) possibleMoves.get(i).add(seams[y - 1][i] + 1);
                if (seams[y - 1][i] > 0) possibleMoves.get(i).add(seams[y - 1][i] - 1);
            }
        }
        return seams;
    }

    private boolean allowedMove(ArrayList<HashSet<Integer>> possibleMoves, int i, int x) {
        for (int j = i + 1; j < possibleMoves.size(); j++) {
            if (possibleMoves.get(j).contains(x) && possibleMoves.get(j).size() == 1) return false;
        }
        return true;
    }

    public int[][] findHorizontalSeam(int num) {
        transpose();
        final int[][] seam = findVerticalSeam(num);
        transpose();
        return seam;
    }

    public void removeVerticalSeam(final int[][] seam) {
        final WritableImage newPic = new WritableImage(width() - seam[0].length, height());
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = newPic.getPixelWriter();
        double[][] newEnergy = new double[width() - seam[0].length][height()];
        for (int y = 0; y < height(); y++) {
            Arrays.sort(seam[y]);
            int curRow = 0;
            for (int x = 0; x < width(); x++) {
                if (Arrays.binarySearch(seam[y], x) < 0) {
                    writer.setArgb(curRow, y, reader.getArgb(x, y));
                    if (newEnergy[curRow][y] != 0) newEnergy[curRow++][y] = energy[x][y];
                } else {
                    newEnergy[curRow][y] = energy(curRow, y);
                    if (curRow > 0) newEnergy[curRow - 1][y] = energy(curRow - 1, y);
                }
            }
        }
        image = newPic;
        energy = newEnergy;
    }

    public void removeHorizontalSeam(final int[][] seam) {
        if (height() <= 1)
            throw new IllegalArgumentException();
        transpose();
        removeVerticalSeam(seam);
        transpose();
    }

    public void addVerticalSeam(final int[][] seam) {
        final WritableImage newPic = new WritableImage(width() + seam[0].length, height());
        PixelWriter writer = newPic.getPixelWriter();
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height(); y++) {
            Arrays.sort(seam[y]);
            int curRow = 0;
            for (int x = 0; x < width(); x++) {
                int i = Arrays.binarySearch(seam[y], x);
                if (i < 0) writer.setArgb(curRow++, y, reader.getArgb(x, y));
                else {
                    int seamColor = reader.getArgb(seam[y][i], y);
                    int nextColor;
                    if (x < width() - 1) nextColor = reader.getArgb(seam[y][i] + 1, y);
                    else nextColor = reader.getArgb(seam[y][i] - 1, y);
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

    public void addHorizontalSeam(final int[][] seam) {
        transpose();
        addVerticalSeam(seam);
        transpose();
    }

    private void seamEnergy(int[][] seam, boolean delete) {
        double[][] newEnergy = new double[width()][height()];
        for (int y = 0; y < height(); y++) {
            System.arraycopy(energy, 0, newEnergy, 0, seam[y][0]);
            for (int i = 0; i < seam[0].length; i++) {
                if (!delete) {
                    newEnergy[seam[y][i]][y] = energy(seam[y][i], y);
                    if (seam[y][i] < width() - 1) {
                        newEnergy[seam[y][i] + 1][y] = energy(seam[y][i] + 1, y);
                        newEnergy[seam[y][i] + 2][y] = energy(seam[y][i] + 2, y);
                        if (i < seam[0].length - 1)
                            System.arraycopy(energy, seam[y][i] + 2, newEnergy, seam[y][i] + 3, (seam[y][i + 1] - seam[y][i]));
                        try {
                            System.arraycopy(energy, seam[y][i] + 2, newEnergy, seam[y][i] + 3, energy.length - (seam[y][i]));
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println("bad");
                        }
                    } else {
                        newEnergy[seam[y][i] - 1][y] = energy(seam[y][i] - 1, y);
                        newEnergy[seam[y][i] + 1][y] = energy(seam[y][i] + 1, y);
                    }
                } else {
                    if (seam[y][i] > 0) newEnergy[seam[y][i] - 1][y] = energy(seam[y][i] - 1, y);
                    if (seam[y][i] < width() - 1) {
                        if (seam[y][i] < width() - 1) newEnergy[seam[y][i]][y] = energy(seam[y][i], y);
                        if (i < seam[0].length - 1)
                            System.arraycopy(energy, seam[y][i] + 2, newEnergy, seam[y][i] + 1, (seam[y][i + 1] - seam[y][i]));
                        else
                            System.arraycopy(energy, seam[y][i] + 2, newEnergy, seam[y][i] + 1, newEnergy.length - (seam[y][i]));
                    }
                }
            }
            /*
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
             */
        }
        energy = newEnergy;
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

    private void relax(double[][] energyTo, final int x, final int y, final int x2, final int y2) {
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

    private class Node {
        Node left;
        Node down;
        Node right;
        double energy;
        int x;
        int y;
    }
}