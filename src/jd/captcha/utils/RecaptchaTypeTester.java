//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.captcha.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class RecaptchaTypeTester {
    public static enum RecaptchaType {
        DOTS_TWO_WORDS,
        TWO_WORDS,
        NUMBER,
        SPLIT_WITH_NUMBER,
        SINGLE_WORD,
        BANNED
    }

    private static final boolean SYSOUT      = false;

    private static final boolean DEBUG_IMAGE = false;

    private static double        TRESHOLD    = 0.6;
    private static int           RADIUS      = 16;

    public static void main(String[] args) throws IOException {

        File folder = new File("G:\\recaptchas");
        File sub = new File(folder, "null");
        if (sub.exists()) {
            Files.deleteRecursiv(sub);
        }
        for (RecaptchaType t : RecaptchaType.values()) {
            sub = new File(folder, t.name());
            if (sub.exists()) {
                Files.deleteRecursiv(sub);
            }
        }

        RADIUS = 16;
        TRESHOLD = 0.6;
        int bad = 0;
        int good = 0;

        // getType(new File(folder, "NUMBER_2db3a62ac4f4f271b2981ba41f716967.jpg"));
        // getType(new File(folder, "NUMBER_9ad55fcdf51704dd43002a3298b75fa4.jpg"));
        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jpg")) {
                RecaptchaType type = getType(file);
                // System.out.println(type);
                if (file.getName().startsWith(type.name())) {
                    // System.out.println("OK");
                    good++;
                } else {
                    // System.out.println("BAD");
                    bad++;

                }
                boolean hasName = false;
                for (RecaptchaType t : RecaptchaType.values()) {
                    if (file.getName().startsWith(t.name())) {
                        hasName = true;
                        break;
                    }
                }
                if (!hasName) {
                    File copyTp = new File(file.getParentFile(), type + "/" + type + "_" + file.getName());
                    copyTp.getParentFile().mkdirs();
                    IO.copyFile(file, copyTp);
                } else {
                    File copyTp = new File(file.getParentFile(), type + "/" + file.getName());
                    copyTp.getParentFile().mkdirs();
                    IO.copyFile(file, copyTp);
                }

            }

        }
    }

    public static void main22(String[] args) {
        File folder = new File("G:\\recaptchas");

        RADIUS = 16;
        TRESHOLD = 0.6d;
        int bad = 0;
        int good = 0;

        for (File file : folder.listFiles()) {
            RecaptchaType type = null;
            for (RecaptchaType tt : RecaptchaType.values()) {
                if (file.getName().startsWith(tt.name())) {
                    type = tt;
                    break;

                }
            }
            if (type == null) {
                continue;
            }
            if (file.isFile() && file.getName().endsWith(".jpg") && file.getName().contains("TWO_")) {
                RecaptchaType detectedType = getType(file);
                // System.out.println(type);
                if (detectedType == type) {
                    // System.out.println("OK");
                    good++;
                } else {
                    // System.out.println("BAD");
                    bad++;
                    System.out.println(file);
                }
                // File copyTp = new File(file.getParentFile(), type + "/" + type + "_" + file.getName());
                // copyTp.getParentFile().mkdirs();
                // IO.copyFile(file, copyTp);
                // Dialog.getInstance().showConfirmDialog(0, file + "", "Test " + type, new ImageIcon(ImageIO.read(file)), null,
                // null);

            }

        }

        double quality = (double) good / (good + bad);
        System.out.println("Run t \t" + TRESHOLD + " r \t" + RADIUS + "  \t" + quality + " \t Bad: " + bad);

    }

    public static void main321(String[] args) throws IOException, DialogClosedException, DialogCanceledException {
        File folder = new File("G:\\recaptchas");

        double bestq = 0;
        for (int r = 15; r < 18; r += 1) {

            for (int t = 600; t < 1000; t += 25) {
                RADIUS = r;
                TRESHOLD = (double) t / 1000;
                int bad = 0;
                int good = 0;

                for (File file : folder.listFiles()) {
                    RecaptchaType type = null;
                    for (RecaptchaType tt : RecaptchaType.values()) {
                        if (file.getName().startsWith(tt.name())) {
                            type = tt;
                            break;

                        }
                    }
                    if (type == null) {
                        continue;
                    }
                    if (file.isFile() && file.getName().endsWith(".jpg") && file.getName().contains("TWO_")) {
                        RecaptchaType detectedType = getType(file);
                        // System.out.println(type);
                        if (detectedType == type) {
                            // System.out.println("OK");
                            good++;
                        } else {
                            // System.out.println("BAD");
                            bad++;
                        }
                        // File copyTp = new File(file.getParentFile(), type + "/" + type + "_" + file.getName());
                        // copyTp.getParentFile().mkdirs();
                        // IO.copyFile(file, copyTp);
                        // Dialog.getInstance().showConfirmDialog(0, file + "", "Test " + type, new ImageIcon(ImageIO.read(file)), null,
                        // null);

                    }

                }

                double quality = (double) good / (good + bad);
                System.out.println("Run t \t" + TRESHOLD + " r \t" + RADIUS + "  \t" + quality);
                if (quality > bestq) {
                    bestq = quality;
                    System.out.println("New Best QUality Bad: " + bad);
                }

            }
        }

    }

    public static RecaptchaType getType(File file) {
        try {

            BufferedImage buf = ImageIO.read(file);

            int width = buf.getWidth();
            int height = buf.getHeight();

            if (buf.getType() == 5) {
                BufferedImage newImage = DEBUG_IMAGE ? IconIO.createEmptyImage(width, height * 2) : null;
                Graphics2D g = DEBUG_IMAGE ? (Graphics2D) newImage.getGraphics() : null;
                int whiteCounterLeft = 0;
                int whiteCounterRight = 0;
                int whiteCounterLeft14 = 0;
                int whiteCounterRight14 = 0;
                int regDistLeft = 0;
                int rgbDistRight = 0;
                for (int x = 0; x < width; x++) {

                    for (int y = 0; y < height; y++) {
                        boolean white = jd.nutils.Colors.getColorDifference(buf.getRGB(x, y), 0xffffff) < 5;
                        if (white) {
                            if (x < width / 4) {
                                whiteCounterLeft14++;
                            } else if (x > ((3 * width) / 4)) {
                                whiteCounterRight14++;
                            }

                            if (x < width / 2) {
                                whiteCounterLeft++;
                            } else {
                                whiteCounterRight++;
                            }
                            if (g != null) {
                                g.setColor(Color.WHITE);
                                g.drawLine(x, y, x, y);
                            }
                        } else {
                            if (g != null) {
                                g.setColor(Color.BLACK);
                                g.drawLine(x, y, x, y);
                            }
                        }
                        Color c = new Color(buf.getRGB(x, y));
                        int br = Math.abs(c.getBlue() - c.getRed());
                        int bg = Math.abs(c.getBlue() - c.getGreen());
                        int rg = Math.abs(c.getGreen() - c.getRed());

                        int v = (br + bg + rg) / 3;
                        if (x < width / 2) {
                            regDistLeft += v;
                        } else {
                            rgbDistRight += v;
                        }
                    }
                }
                double whiteLeft14 = (double) whiteCounterLeft14 / ((width / 4) * height);
                double whiteRight14 = (double) whiteCounterRight14 / ((width / 4) * height);

                double whiteLeft = (double) whiteCounterLeft / ((width / 2) * height);
                double whiteRight = (double) whiteCounterRight / ((width / 2) * height);
                double rgbLeft = (double) regDistLeft / ((width / 2) * height);
                double rgbRight = (double) rgbDistRight / ((width / 2) * height);
                if (g != null) {
                    g.drawImage(buf, 0, height, null);
                    g.dispose();
                }
                boolean textLeft = whiteLeft > 0.5 && rgbLeft < 5d && whiteLeft14 < 0.95;
                boolean textRight = whiteRight > 0.5 && rgbRight < 5d && whiteRight14 < 0.95;
                if (whiteLeft > 0.5d && whiteRight > 0.5d) {
                    if (g != null) {
                        Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(newImage), null, null);

                    }
                    return RecaptchaType.NUMBER;
                }
                if (textLeft && !textRight) {
                    if (g != null) {
                        Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(newImage), null, null);

                    }

                    return RecaptchaType.SPLIT_WITH_NUMBER;
                }
                if (textRight && !textLeft) {
                    if (g != null) {
                        Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(newImage), null, null);

                    }
                    return RecaptchaType.SPLIT_WITH_NUMBER;
                }
                if (g != null) {
                    Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(newImage), null, null);

                }
                return RecaptchaType.NUMBER;

            }

            // buf = IconIO.blur(buf);
            boolean[][] pixel = new boolean[width][height];
            long t = System.currentTimeMillis();
            // BufferedImage newImage = IconIO.createEmptyImage(width, height * 2);
            // Graphics2D g = (Graphics2D) newImage.getGraphics();

            loop: for (int x = 0; x < width; x++) {

                for (int y = 0; y < height; y++) {
                    pixel[x][y] = jd.nutils.Colors.getColorDifference(buf.getRGB(x, y), 0xffffff) > 20;
                    // if (pixel[x][y]) {
                    // g.setColor(Color.BLACK);
                    // g.drawLine(x, y, x, y);
                    // } else {
                    // g.setColor(Color.YELLOW);
                    // g.drawLine(x, y, x, y);
                    // }
                }
            }
            if (SYSOUT) {
                System.out.println("Create Grid: " + (System.currentTimeMillis() - t));
            }
            t = System.currentTimeMillis();

            boolean isBanned = true;
            loop: for (int x = 0; x < width; x++) {
                if (pixel[x][15]) {
                    isBanned = false;
                    break;
                }
                if (pixel[x][29]) {
                    isBanned = false;
                    break;
                }
                if (pixel[x][43]) {
                    isBanned = false;
                    break;
                }
            }
            // g.drawImage(buf, 0, height, null);
            // g.dispose();
            if (SYSOUT) {
                System.out.println("Banned: " + (System.currentTimeMillis() - t));
            }
            t = System.currentTimeMillis();

            if (isBanned) {
                return RecaptchaType.BANNED;
            }
            // Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(newImage), null, null);

            int words = 0;
            int lastBlackColumn = -1;

            boolean isWord = false;
            int blackMin = -1;

            // int middle = newImage.getWidth() / 2;
            for (int x = 0; x < width; x++) {
                boolean white = true;
                for (int y = 0; y < height; y++) {

                    if (pixel[x][y]) {
                        // black
                        white = false;
                        break;
                    }
                }

                if (white) {
                    if (isWord && lastBlackColumn >= 0) {
                        if (x - lastBlackColumn > 5) {
                            words++;
                            isWord = false;
                        }
                    }
                    // lastWhiteColumn = x;
                } else {
                    // black
                    isWord = true;
                    if (blackMin < 0) {
                        blackMin = x;
                    }
                    lastBlackColumn = x;
                }
            }
            if (lastBlackColumn == width - 1) {
                words++;
            }
            if (words == 1) {
                if (width - (lastBlackColumn - blackMin) < width / 5) {
                    words++;

                }
            }

            if (SYSOUT) {
                System.out.println("Count words: " + (System.currentTimeMillis() - t));
            }
            t = System.currentTimeMillis();
            if (words <= 1) {
                return RecaptchaType.SINGLE_WORD;
            }

            // if (!isEmptyMiddle) {
            // return RecaptchaType.SINGLE_WORD;
            // }
            // Graphics2D g = (Graphics2D) newImage.getGraphics();
            // g.setColor(Color.black);
            // g.drawImage(buf, 0, height, null);
            double maxBWRationLeft = 0;
            double maxBWRationRight = 0;
            double tresh = TRESHOLD;
            for (int x = 0; x < width; x += 1) {
                for (int y = 0; y < height; y += 1) {
                    int radius = RADIUS;
                    boolean black = true;
                    int whitePX = 0;
                    int blackPX = 0;
                    int max = (radius * 2 + 1) * (radius * 2 + 1);
                    block: for (int xx = -radius; xx < radius; xx++) {
                        for (int yy = -radius; yy < radius; yy++) {
                            int currentX = x + xx;
                            int currentY = y + yy;
                            if (currentX < 0 || currentY < 0 || currentX > width - 1 || currentY > height - 1) {
                                continue;
                            }

                            if (!pixel[currentX][currentY]) {
                                whitePX++;
                                black = false;
                                // break block;
                            } else {
                                blackPX++;
                            }
                            // int highestPossibleBlack = max - whitePX;
                            // if (((double) highestPossibleBlack / max) <= maxBWRation) {
                            // break block;
                            // }
                        }
                    }

                    double rat = (double) blackPX / max;
                    if (x <= width / 2) {
                        maxBWRationLeft = Math.max(rat, maxBWRationLeft);
                    } else {
                        maxBWRationRight = Math.max(rat, maxBWRationRight);
                    }

                    // if (black) {
                    // g.drawLine(x, y, x, y);
                    // }
                }
            }
            // System.out.println();
            // g.dispose();

            if (SYSOUT) {
                System.out.println("Find Dots: " + (System.currentTimeMillis() - t));
            }
            t = System.currentTimeMillis();
            if (SYSOUT) {
                System.out.println("words " + words + "Test maxBWRation " + maxBWRationLeft + "-" + maxBWRationRight);
            }
            if (maxBWRationLeft > tresh || maxBWRationRight > tresh) {
                return RecaptchaType.DOTS_TWO_WORDS;
            } else {
                return RecaptchaType.TWO_WORDS;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}