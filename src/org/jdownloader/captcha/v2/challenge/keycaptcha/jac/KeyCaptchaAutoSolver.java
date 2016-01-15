package org.jdownloader.captcha.v2.challenge.keycaptcha.jac;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import jd.nutils.Colors;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaImages;
import org.jdownloader.logging.LogController;

// KeyCaptcha stuff
/**
 * Solves KeyCaptcha for us
 *
 * @author flubshi
 *
 */
public class KeyCaptchaAutoSolver {
    private final LogInterface logger;

    public KeyCaptchaAutoSolver() {
        LogInterface logger = LogController.getRebirthLogger();
        if (logger == null) {
            logger = LoggerFactory.getDefaultLogger();
        }
        this.logger = logger;
    }

    private static final int     PUNISH_LINES      = 6;

    private static final boolean COLLECT_PIECES    = !Application.isJared(null);

    private static final int     COLOR_SCAN_LENGTH = 3;

    private LinkedList<Integer>  mouseArray        = new LinkedList<Integer>();

    private void marray(Point loc) {
        if (loc != null) {
            if (mouseArray.size() == 0) {
                mouseArray.add(loc.x + 465);
                mouseArray.add(loc.y + 264);
            }
            if (mouseArray.get(mouseArray.size() - 2) != loc.x + 465 || mouseArray.get(mouseArray.size() - 1) != loc.y + 264) {
                mouseArray.add(loc.x + 465);
                mouseArray.add(loc.y + 264);
            }
            if (mouseArray.size() > 40) {
                ArrayList<Integer> tmpMouseArray = new ArrayList<Integer>();
                tmpMouseArray.addAll(mouseArray.subList(2, 40));
                mouseArray.clear();
                mouseArray.addAll(tmpMouseArray);
            }
        }
    }

    public LinkedList<Integer> getMouseArray() {
        return mouseArray;
    }

    /**
     * get the rectangle that can be used to crop the background image
     *
     * @param source
     * @param offset
     * @return
     */
    public static Rectangle getCroppedImage(BufferedImage source, int offset) {

        try {

            final int width = source.getWidth();
            final int height = source.getHeight();
            int x0 = 0;
            int y0 = 0;
            int x1 = width;
            int y1 = height; // the new corners of the trimmed image
            int i, j; // i - horizontal iterator; j - vertical iterator
            leftLoop: for (i = 0; i < width; i++) {
                for (j = 0; j < height; j++) {

                    if (Colors.getCMYKColorDifference1(source.getRGB(i, j), Color.WHITE.getRGB()) > 7.0d) {
                        break leftLoop;
                    }
                }
            }
            x0 = Math.max(i - offset, 0);

            topLoop: for (j = 0; j < height; j++) {
                for (i = 0; i < width; i++) {
                    if (Colors.getCMYKColorDifference1(source.getRGB(i, j), Color.WHITE.getRGB()) > 7.0d) {
                        break topLoop;
                    }
                }
            }
            y0 = Math.max(j - offset, 0);

            rightLoop: for (i = width - 1; i >= 0; i--) {
                for (j = 0; j < height; j++) {
                    if (Colors.getCMYKColorDifference1(source.getRGB(i, j), Color.WHITE.getRGB()) > 7.0d) {
                        break rightLoop;
                    }
                }
            }

            x1 = Math.min(i + 1 + offset, width);

            bottomLoop: for (j = height - 1; j >= 0; j--) {
                for (i = 0; i < width; i++) {
                    if (Colors.getCMYKColorDifference1(source.getRGB(i, j), Color.WHITE.getRGB()) > 7.0d) {
                        break bottomLoop;
                    }
                }
            }
            y1 = Math.min(j + 1 + offset, height);

            return new Rectangle(x0, y0, x1 - x0, y1 - y0);
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) throws IOException {
        Application.setApplication(".jd_home");
        // helper method to create masks from pieces
        // main2(args);
        File masks = Application.getResource("tmp/masks");
        if (masks.exists()) {
            Files.deleteRecursiv(masks);
        }
        masks.mkdirs();
        for (File folder : Application.getResource("tmp").listFiles()) {

            if (folder.isDirectory() && folder.getName().contains(".png_col")) {
                BufferedImage merge = IconIO.createEmptyImage(60, 60);
                Graphics2D g = (Graphics2D) merge.getGraphics();
                g.setColor(Color.BLACK);
                int files = 0;
                for (File file : folder.listFiles()) {
                    if (file.getName().endsWith(".png")) {
                        BufferedImage image = ImageIO.read(file);
                        files++;

                        for (int x = 0; x < image.getWidth(); x++) {
                            for (int y = 0; y < image.getHeight(); y++) {
                                int rgb = image.getRGB(x, y);
                                if (rgb == 0) {
                                    continue;
                                }

                                Color c = new Color(rgb, true);
                                int tol = 200;

                                if (Colors.getRGBDistance(rgb) == 0 && c.getRed() > tol && c.getGreen() > tol && c.getBlue() > tol && c.getAlpha() > tol) {

                                    g.drawLine(x, y, x, y);
                                }
                            }
                        }

                    }
                }
                g.dispose();
                if (files > 5) {
                    int m = 0;

                    File mask = new File(masks, "mask_" + m + ".png");
                    while (mask.exists()) {
                        m++;
                        mask = new File(masks, "mask_" + m + ".png");
                    }

                    ImageIO.write(merge, "png", mask);
                }

            }

        }

    }

    public static void main2(String[] args) throws IOException {
        // helper funtion to sort puzzle pieces by form
        Application.setApplication(".jd_home");

        HashSet<File> dupe = new HashSet<File>();
        orgLoop: for (File orgFile : Application.getResource("").listFiles()) {
            if (!orgFile.getName().endsWith(".png")) {
                continue;
            }
            BufferedImage org = ImageIO.read(orgFile);
            if (org == null) {
                continue;
            }
            if (org.getWidth() != 60 || org.getHeight() != 60) {
                continue;
            }
            if (!dupe.add(orgFile)) {
                continue orgLoop;
            }
            File dest = new File(orgFile.getAbsolutePath() + "_col");
            dest.mkdirs();
            File copy = new File(dest, orgFile.getName());
            copy.delete();
            IO.copyFile(orgFile, copy);
            fileloop: for (File f : Application.getResource("tmp").listFiles()) {
                if (dupe.contains(f)) {
                    continue;
                }
                if (f.getName().endsWith(".png")) {
                    BufferedImage image = ImageIO.read(f);
                    int count = 0;
                    if (image != null && image.getWidth() == org.getWidth() && image.getHeight() == org.getHeight()) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            for (int y = 0; y < image.getHeight(); y++) {

                                if (org.getRGB(x, y) == 0) {
                                    if (image.getRGB(x, y) != 0) {
                                        count++;

                                    }

                                }
                            }
                        }
                        System.out.println(f + " - " + count);
                        if (count == 0) {
                            dupe.add(f);
                            copy = new File(dest, f.getName());
                            copy.delete();
                            IO.copyFile(f, copy);
                        }

                    }

                }
            }

        }
    }

    public String solve(KeyCaptchaImages images) {
        try {
            mouseArray = new LinkedList<Integer>();
            // does not work. update required
            HashMap<BufferedImage, Point> imgPosition = new HashMap<BufferedImage, Point>();

            collectPIiecesDevOnly(images);

            int stepSize = 2;
            int stepSizeMask = 2;
            Rectangle cropping = getCroppedImage(images.backgroundImage, 45);
            BufferedImage back = images.backgroundImage.getSubimage(cropping.x, cropping.y, cropping.width, cropping.height);

            BufferedImage cropped = IconIO.createEmptyImage(back.getWidth(), back.getHeight());
            Graphics2D g = (Graphics2D) cropped.getGraphics();
            g.drawImage(back, 0, 0, null);

            back = cropped;
            int pieceID = 0;
            for (BufferedImage piece : images.pieces) {
                BufferedImage mask = getMask(piece);
                pieceID++;
                if (mask != null) {

                    BufferedImage cleanedPiece = applyMask(piece, mask);
                    // Dialog.getInstance().showImage(cleanedPiece);

                    int best = Integer.MAX_VALUE;
                    Point bestPoint = null;
                    for (int x = -cleanedPiece.getWidth() / 2; x < cropped.getWidth() - cleanedPiece.getWidth() / 2; x += stepSize) {

                        for (int y = -cleanedPiece.getHeight() / 2; y < cropped.getHeight() - cleanedPiece.getHeight() / 2; y += stepSize) {
                            int count = 0;
                            int max = 50;
                            maskloop: for (int x2 = 0; x2 < mask.getWidth(); x2 += stepSizeMask) {
                                for (int y2 = 0; y2 < mask.getHeight(); y2 += stepSizeMask) {
                                    if (mask.getRGB(x2, y2) != 0) {
                                        if (x + x2 < 0 || x + x2 >= cropped.getWidth()) {
                                            continue;
                                        }
                                        if (y + y2 < 0 || y + y2 >= cropped.getHeight()) {
                                            continue;
                                        }

                                        try {
                                            int rgb = cropped.getRGB(x + x2, y + y2);
                                            if (!isWhite(rgb)) {
                                                count++;
                                                if (count > max) {
                                                    break;
                                                }
                                            }
                                        } catch (Throwable e) {
                                            count += 10000;
                                            break maskloop;
                                        }

                                    }

                                }
                            }
                            int punLeft;
                            int punRight;
                            int punTop;
                            int punBottom;
                            int pxCount = count;
                            if (count < best) {
                                punLeft = getLeftPunish(x, y, cropped, mask, cleanedPiece);
                                punRight = getRightPunish(x, y, cropped, mask, cleanedPiece);
                                punTop = getTopPunish(x, y, cropped, mask, cleanedPiece);
                                punBottom = getBottomPunish(x, y, cropped, mask, cleanedPiece);

                                count += punLeft / 8;
                                count += punRight / 12;
                                count += punTop / 8;
                                count += punBottom / 12;

                                // if (count < best) {
                                // System.out.println("xXy " + x + "x" + y);
                                // System.out.println("Piece " + pieceID);
                                // System.out.println("pxCount " + pxCount);
                                // System.out.println("punLeft " + punLeft);
                                // System.out.println("punRight " + punRight);
                                // System.out.println("punTop " + punTop);
                                // System.out.println("punBottom " + punBottom);
                                // System.out.println("Rate " + count);
                                // }
                                if (count < best) {

                                    bestPoint = new Point(x, y);
                                    // System.out.println(bestPoint);
                                    best = count;
                                }
                            }

                        }
                    }

                    ImageAndPosition imagePos = new ImageAndPosition(piece, new Point(cropping.x + bestPoint.x, cropping.y + bestPoint.y));
                    imgPosition.put(imagePos.image, imagePos.position);
                    g.drawImage(cleanedPiece, bestPoint.x, bestPoint.y, null);

                    marray(new Point((int) (Math.random() * imagePos.position.x), (int) (Math.random() * imagePos.position.y)));
                    marray(imagePos.position);

                }

            }
            // Dialog.getInstance().showImage(cropped);
            String positions = "";
            int i = 0;
            for (int c = 0; c < images.pieces.size(); c++) {
                BufferedImage image = images.pieces.get(c);
                final Point p = imgPosition.get(image);
                if (p == null) {
                    logger.info("Could Not Map all PIeces");
                    return null;
                }
                positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
                i++;
            }
            return positions;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private void collectPIiecesDevOnly(KeyCaptchaImages images) throws IOException {
        if (COLLECT_PIECES) {
            // collect_pieces
            // write images to jd_home

            int j = 0;
            File backf = Application.getResource("tmp/background_" + Hash.getMD5(IconIO.toJpgBytes(images.backgroundImage)) + "_" + j + ".png");

            while (backf.exists()) {
                j++;
                backf = Application.getResource("tmp/background_" + Hash.getMD5(IconIO.toJpgBytes(images.backgroundImage)) + "_" + j + ".png");

            }
            backf.delete();
            ImageIO.write(images.backgroundImage, "png", backf);
            for (int i = 0; i < images.pieces.size(); i++) {
                j = 0;
                File file = Application.getResource("tmp/" + "piece_" + j + ".png");
                while (file.exists()) {
                    j++;
                    file = Application.getResource("tmp/" + "piece_" + j + ".png");
                }
                file.delete();
                ImageIO.write(images.pieces.get(i), "png", file);
            }
        }
    }

    private int getRightPunish(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece) {
        int sum = 0;
        int num = PUNISH_LINES;
        int step = cleanedPiece.getHeight() / (num + 1);
        int stepCount = 0;
        for (int i = 0; i < num; i++) {
            int of = getRightPunishByOffset(x, y, cropped, mask, cleanedPiece, (i + 1) * step);
            if (of >= 0) {
                sum += of;
                stepCount++;
            }
        }

        return sum / stepCount;
    }

    private int getTopPunish(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece) {

        int sum = 0;
        int num = PUNISH_LINES;
        int step = cleanedPiece.getWidth() / (num + 1);
        int stepCount = 0;
        for (int i = 0; i < num; i++) {
            int of = getTopPunishByOffset(x, y, cropped, mask, cleanedPiece, (i + 1) * step);
            if (of >= 0) {
                sum += of;
                stepCount++;
            }
        }

        return sum / stepCount;
    }

    private int getBottomPunish(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece) {
        int sum = 0;
        int num = PUNISH_LINES;
        int step = cleanedPiece.getWidth() / (num + 1);
        int stepCount = 0;
        for (int i = 0; i < num; i++) {
            int of = getBottomPunishByOffset(x, y, cropped, mask, cleanedPiece, (i + 1) * step);
            if (of >= 0) {
                sum += of;
                stepCount++;
            }
        }

        return sum / stepCount;
    }

    private int getLeftPunish(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece) {
        int sum = 0;
        int num = PUNISH_LINES;
        int step = cleanedPiece.getHeight() / (num + 1);
        int stepCount = 0;
        for (int i = 0; i < num; i++) {
            int of = getLeftPunishbyOffset(x, y, cropped, mask, cleanedPiece, (i + 1) * step);
            if (of >= 0) {
                sum += of;
                stepCount++;
            }
        }

        return sum / stepCount;

    }

    private int getLeftPunishbyOffset(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece, int offset) {
        int punish = 255;
        try {
            int y2 = offset;
            boolean notFoundColor = true;
            // Dialog.getInstance().showImage(cropped);
            for (int x2 = 0; x2 < mask.getWidth(); x2++) {
                if (mask.getRGB(x2, y2) != 0) {
                    notFoundColor = false;
                    int rgb = cleanedPiece.getRGB(x2, y2);
                    // Graphics2D g = (Graphics2D) cleanedPiece.getGraphics();
                    // g.setColor(Color.RED);
                    // g.drawLine(x2, y2, x2, y2);
                    int xx = x + x2 - 1;
                    int yy = y + y2;
                    int m = COLOR_SCAN_LENGTH;
                    double bestDiff = punish;
                    int bestColor = 0;
                    while (m-- > 0 && xx >= 0) {

                        int c = cropped.getRGB(xx, yy);
                        xx--;
                        // Colors.getColorDifference(color, color2)
                        double dif = Colors.getColorDifference(c, rgb);
                        Color col = new Color(c);

                        if (col.getRed() == 255 && col.getGreen() == 255 && col.getBlue() == 255) {
                            dif *= 3;
                        }
                        if (dif < bestDiff) {
                            bestDiff = dif;
                            bestColor = c;
                        }

                    }
                    Color pCol = new Color(rgb);
                    Color bCol = new Color(bestColor);
                    return (int) bestDiff;

                }
            }
            if (notFoundColor) {
                return -1;
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            return punish;
        }
        return punish;
    }

    private int getRightPunishByOffset(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece, int offset) {
        int punish = 255;
        try {
            int y2 = offset;
            boolean notFoundColor = true;
            for (int x2 = mask.getWidth() - 1; x2 >= 0; x2--) {
                if (mask.getRGB(x2, y2) != 0) {
                    notFoundColor = false;
                    int rgb = cleanedPiece.getRGB(x2, y2);

                    int xx = x + x2 - 1;
                    int yy = y + y2;
                    int m = COLOR_SCAN_LENGTH;
                    double bestDiff = punish;
                    int bestColor = 0;
                    while (m-- > 0 && xx < cropped.getWidth()) {

                        int c = cropped.getRGB(xx, yy);
                        xx++;

                        double dif = Colors.getColorDifference(c, rgb);
                        Color col = new Color(c);

                        if (col.getRed() == 255 && col.getGreen() == 255 && col.getBlue() == 255) {
                            dif *= 3;
                        }
                        if (dif < bestDiff) {
                            bestDiff = dif;
                            bestColor = c;
                        }

                    }
                    Color pCol = new Color(rgb);
                    Color bCol = new Color(bestColor);
                    return (int) bestDiff;

                }
            }
            if (notFoundColor) {
                return -1;
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            return punish;
        }
        return punish;
    }

    private int getBottomPunishByOffset(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece, int offset) {
        int punish = 255;
        try {
            boolean notFoundColor = true;
            int x2 = offset;
            for (int y2 = mask.getHeight() - 1; y2 >= 0; y2--) {
                if (mask.getRGB(x2, y2) != 0) {
                    notFoundColor = false;
                    int rgb = cleanedPiece.getRGB(x2, y2);

                    int xx = x + x2;
                    int yy = y + y2 - 1;
                    int m = COLOR_SCAN_LENGTH;
                    double bestDiff = punish;
                    int bestColor = 0;
                    while (m-- > 0 && yy < cropped.getHeight()) {

                        int c = cropped.getRGB(xx, yy);
                        yy++;

                        double dif = Colors.getColorDifference(c, rgb);
                        Color col = new Color(c);

                        if (col.getRed() == 255 && col.getGreen() == 255 && col.getBlue() == 255) {
                            dif *= 3;
                        }
                        if (dif < bestDiff) {
                            bestDiff = dif;
                            bestColor = c;
                        }

                    }
                    Color pCol = new Color(rgb);
                    Color bCol = new Color(bestColor);
                    return (int) bestDiff;

                }
            }
            if (notFoundColor) {
                return -1;
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            return punish;
        }
        return punish;
    }

    private int getTopPunishByOffset(int x, int y, BufferedImage cropped, BufferedImage mask, BufferedImage cleanedPiece, int offset) {

        int punish = 255;
        try {
            int x2 = offset;
            boolean notFoundColor = true;
            for (int y2 = 0; y2 < mask.getHeight(); y2++) {
                if (mask.getRGB(x2, y2) != 0) {
                    notFoundColor = false;
                    int rgb = cleanedPiece.getRGB(x2, y2);

                    int xx = x + x2;
                    int yy = y + y2 - 1;
                    int m = COLOR_SCAN_LENGTH;
                    double bestDiff = punish;
                    int bestColor = 0;
                    while (m-- > 0 && yy >= 0) {

                        int c = cropped.getRGB(xx, yy);
                        yy--;

                        double dif = Colors.getColorDifference(c, rgb);
                        Color col = new Color(c);

                        if (col.getRed() == 255 && col.getGreen() == 255 && col.getBlue() == 255) {
                            dif *= 3;
                        }
                        if (dif < bestDiff) {
                            bestDiff = dif;
                            bestColor = c;
                        }

                    }
                    Color pCol = new Color(rgb);
                    Color bCol = new Color(bestColor);
                    return (int) bestDiff;
                }
            }
            if (notFoundColor) {
                return -1;
            }
        } catch (Throwable e) {
            // e.printStackTrace();
            return punish;
        }
        return punish;
    }

    private boolean isWhite(int rgb) {
        int tol = 250;
        Color c = new Color(rgb);
        return Colors.getRGBDistance(rgb) == 0 && c.getRed() > tol && c.getGreen() > tol && c.getBlue() > tol;
    }

    private BufferedImage applyMask(BufferedImage piece, BufferedImage maskImage) {
        BufferedImage merge = IconIO.createEmptyImage(60, 60);
        Graphics2D g = (Graphics2D) merge.getGraphics();
        g.setColor(Color.BLACK);

        for (int x = 0; x < maskImage.getWidth(); x++) {
            for (int y = 0; y < maskImage.getHeight(); y++) {
                if (maskImage.getRGB(x, y) != 0) {
                    g.setColor(new Color(piece.getRGB(x, y), true));
                    g.drawLine(x, y, x, y);
                }
            }
        }
        g.dispose();
        return merge;
    }

    private BufferedImage getMask(BufferedImage piece) throws IOException {
        int i = 0;
        BufferedImage mask = null;
        int bestCount = Integer.MAX_VALUE;

        while (true) {

            URL url = getClass().getResource("masks/mask_" + i + ".png");
            i++;
            if (url == null) {
                return mask;
            }
            BufferedImage maskImage = ImageIO.read(url);

            int count = 0;
            for (int x = 0; x < maskImage.getWidth(); x++) {
                for (int y = 0; y < maskImage.getHeight(); y++) {

                    if (maskImage.getRGB(x, y) != 0) {

                        int rgb = piece.getRGB(x, y);
                        if (rgb == 0) {
                            count++;
                        }
                    }

                }
            }
            if (count < bestCount) {
                mask = maskImage;
                bestCount = count;
            }
        }
    }

}