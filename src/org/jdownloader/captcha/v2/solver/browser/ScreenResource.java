package org.jdownloader.captcha.v2.solver.browser;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.HashMap;

import javax.swing.ImageIcon;

import jd.nutils.Colors;

import org.appwork.exceptions.WTFException;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;

public abstract class ScreenResource {
    protected int x;
    private int   blockSize = 100;

    public ScreenResource(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public ScreenResource() {
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    protected int    y;
    protected int    width;
    protected int    height;
    private Robot    robot;
    protected double scale = 1d;

    public Rectangle getRectangleByColor(int rgb, int wmin, int hmin, double tollerance, int xstart, int ystart) {
        long start = System.currentTimeMillis();
        int xstartBlock = xstart / blockSize;
        int ystartBlock = ystart / blockSize;
        int blockRadius = 0;
        try {
            Point point = null;
            step: while (true) {
                blockRadius++;
                if (blockRadius * blockSize > 300) {
                    break;
                }
                int xblockmax = xstartBlock + blockRadius - 1;
                int yblockmax = ystartBlock + blockRadius - 1;
                boolean hasBlock = false;
                Block block;
                for (int xblock = xstartBlock; xblock <= xblockmax; xblock++) {
                    block = getBlock(xblock * blockSize, yblockmax * blockSize);
                    // showImage(block.getImage(), blockRadius + ":" + xblock + "x" + yblockmax + "=" + block);
                    if (block != null) {
                        hasBlock = true;
                    } else {
                        break;
                    }
                    point = scanBlock(block, rgb, tollerance, xstart, ystart);
                    if (point != null) {
                        break step;
                    }
                }
                for (int yblock = ystartBlock; yblock < yblockmax; yblock++) {
                    block = getBlock(xblockmax * blockSize, yblock * blockSize);
                    // showImage(block.getImage(), blockRadius + ":" + xblockmax + "x" + yblock + "=" + block);
                    if (block != null) {
                        hasBlock = true;
                    } else {
                        break;
                    }
                    point = scanBlock(block, rgb, tollerance, xstart, ystart);
                    if (point != null) {
                        break step;
                    }
                }
                if (!hasBlock) {
                    break;
                }
            }
            int width = 0;
            int height = 0;
            if (point != null) {
                BufferedImage xStrip = getRobot().createScreenCapture(new Rectangle(this.x + point.x, this.y + point.y, getWidth() - point.x, 1));
                // showImage(xStrip, null);
                for (int x = wmin; x < xStrip.getWidth(); x++) {
                    int col = xStrip.getRGB(x, 0);
                    double dif = Colors.getColorDifference(rgb, col);
                    if (dif > tollerance) {
                        break;
                    } else {
                        width = x + 1;
                    }
                }
                BufferedImage yStrip = getRobot().createScreenCapture(new Rectangle(this.x + point.x, this.y + point.y, 1, getHeight() - point.y));
                // showImage(yStrip, null);
                for (int y = hmin; y < yStrip.getHeight(); y++) {
                    int col = yStrip.getRGB(0, y);
                    double dif = Colors.getColorDifference(rgb, col);
                    // System.out.println(Long.toHexString(col) + "\t" + dif);
                    if (dif > tollerance) {
                        break;
                    } else {
                        height = y + 1;
                    }
                }
                Rectangle ret = new Rectangle(this.x + point.x, this.y + point.y, width, height);
                // showImage(getRobot().createScreenCapture(ret));
                System.out.println("Found Rectangle in " + (System.currentTimeMillis() - start) + "ms");
                return ret;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            clearBlocks();
        }
        // return null;
        return null;
    }

    private void clearBlocks() {
        blocks = new HashMap<Integer, HashMap<Integer, Block>>();
    }

    private Point scanBlock(Block block, int rgb, double tollerance, int xstart, int ystart) {
        try {
            // block.image = null;
            int pixelRadius = 0;
            while (true) {
                pixelRadius++;
                if (pixelRadius >= blockSize) {
                    break;
                }
                int xmax = pixelRadius - 1;
                int ymax = pixelRadius - 1;
                Point px = null;
                Point py = null;
                for (int x = 0; x <= xmax; x++) {
                    int pixelColor = block.getImage().getRGB(x, ymax);
                    double dif = Colors.getColorDifference(rgb, pixelColor);
                    // System.out.println(Long.toHexString(pixelColor) + "\r\n" + x + "\tx\t" + ymax + "\t " + dif);
                    // if (rgb == 0xcccccc) {
                    // block.getImage().setRGB(x, ymax, 0x00ff00);
                    // }
                    if (dif < tollerance) {
                        // if (rgb == 0xcccccc) {
                        // block.getImage().setRGB(x, ymax, 0Xff0000);
                        // }
                        // if (rgb == 0xcccccc) {
                        // showImage(block.getImage());
                        // }
                        try {
                            if (checkColor(rgb, tollerance, block.x + x + scale(1), block.y + ymax) && checkColor(rgb, tollerance, block.x + x + scale(48), block.y + ymax)) {
                                if (checkColor(rgb, tollerance, block.x + x, block.y + ymax + scale(1)) && checkColor(rgb, tollerance, block.x + x, block.y + ymax + scale(48))) {
                                    px = new Point(block.x + x, block.y + ymax);
                                    break;
                                }
                            }
                        } catch (NoBlockException e) {
                        }
                    }
                }
                for (int y = 0; y < ymax; y++) {
                    int pixelColor = block.getImage().getRGB(xmax, y);
                    double dif = Colors.getColorDifference(rgb, pixelColor);
                    // if (rgb == 0xcccccc) {
                    // block.getImage().setRGB(xmax, y, 0X00fff0);
                    // }
                    // System.out.println(xmax + "\tx\t" + y + "\t " + dif);
                    if (dif < tollerance) {
                        // if (rgb == 0xcccccc) {
                        // block.getImage().setRGB(xmax, y, 0Xff0000);
                        // }
                        // if (rgb == 0xcccccc) {
                        // showImage(block.getImage());
                        // }
                        try {
                            if (checkColor(rgb, tollerance, block.x + xmax + scale(1), block.y + y) && checkColor(rgb, tollerance, block.x + xmax + scale(48), block.y + y)) {
                                if (checkColor(rgb, tollerance, block.x + xmax, block.y + y + scale(1)) && checkColor(rgb, tollerance, block.x + xmax, block.y + y + scale(48))) {
                                    px = new Point(block.x + xmax, block.y + y);
                                    break;
                                }
                            }
                        } catch (NoBlockException e) {
                        }
                    }
                }
                if (px != null) {
                    if (py == null) {
                        return px;
                    }
                    if (px.x < py.x) {
                        return px;
                    }
                    if (px.y < py.y) {
                        return px;
                    }
                    return py;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    protected boolean checkColor(int rgb, double tollerance, int x, int y) throws NoBlockException {
        int col = getRGB(x, y);
        double dif = Colors.getColorDifference(rgb, col);
        return dif < tollerance;
    }

    protected int scale(Number i) {
        return Math.max(1, (int) (scale * i.doubleValue()));
    }

    public class Block {
        private int                          x;
        private int                          y;
        private SoftReference<BufferedImage> image;

        public Block(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getHeight() {
            return getImage().getHeight();
        }

        public int getWidth() {
            return getImage().getWidth();
        }

        @Override
        public String toString() {
            return "Block " + x + "." + y + " " + getWidth() + "x" + getHeight();
        }

        public int getRGB(int x2, int y2) {
            return getImage().getRGB(x2 - x, y2 - y);
        }

        public BufferedImage getImage() {
            BufferedImage img;
            if (image == null) {
                return updateImage();
            }
            if ((img = image.get()) == null) {
                System.out.println("Threw away");
                return updateImage();
            }
            return img;
        }

        private BufferedImage updateImage() {
            final Rectangle rec = new Rectangle(ScreenResource.this.x + this.x, ScreenResource.this.y + y, blockSize, blockSize);
            final BufferedImage img = getRobot().createScreenCapture(rec);
            System.out.println("Create screenshot: " + rec);
            if (System.getProperty("rc2debug") != null) {
                showImage(img, rec.toString());
            }
            image = new SoftReference<BufferedImage>(img);
            return img;
        }
        /**
         * @param img
         */
    }

    private HashMap<Integer, HashMap<Integer, Block>> blocks = new HashMap<Integer, HashMap<Integer, Block>>();

    private int getRGB(int x, int y) throws NoBlockException {
        Block block = getBlock(x, y);
        return block.getRGB(x, y);
    }

    public void showImage(BufferedImage img, String title) {
        ConfirmDialog d = new ConfirmDialog(0, title, "", new ImageIcon(img), null, null);
        d.setTimeout(5000);
        UIOManager.I().show(null, d);
    }

    private Block getBlock(int x, int y) throws NoBlockException {
        if (x >= getWidth()) {
            throw new NoBlockException(x, y);
        }
        if (y >= getHeight()) {
            throw new NoBlockException(x, y);
        }
        x /= blockSize;
        y /= blockSize;
        x *= blockSize;
        y *= blockSize;
        HashMap<Integer, Block> column = blocks.get(x);
        if (column == null) {
            column = new HashMap<Integer, ScreenResource.Block>();
            blocks.put(x, column);
        }
        Block block = column.get(y);
        if (block == null) {
            block = new Block(x, y);
            column.put(y, block);
        }
        return block;
    }

    protected Robot getRobot() {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                throw new WTFException(e);
            }
        }
        return robot;
    }
}
