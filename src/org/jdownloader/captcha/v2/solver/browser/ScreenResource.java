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
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public abstract class ScreenResource {

    protected int x;
    private int   blockSize;

    public ScreenResource(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        blockSize = 50;

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

    protected int y;
    protected int width;
    protected int height;
    private Robot robot;

    public Rectangle getRectangleByColor(int rgb, double tollerance, int xstart, int ystart) {
        long start = System.currentTimeMillis();
        int xstartBlock = xstart / blockSize;
        int ystartBlock = ystart / blockSize;
        int blockRadius = 0;
        Point point = null;
        step: while (true) {
            blockRadius++;

            int xblockmax = xstartBlock + blockRadius - 1;
            int yblockmax = ystartBlock + blockRadius - 1;
            boolean hasBlock = false;
            Block block;
            for (int xblock = xstartBlock; xblock <= xblockmax; xblock++) {

                block = getBlock(xblock * blockSize, yblockmax * blockSize);
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

                block = getBlock(xblockmax * blockSize, yblock);
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
            // showImage(xStrip);
            for (int x = 0; x < xStrip.getWidth(); x++) {
                if (Colors.getColorDifference(rgb, xStrip.getRGB(x, 0)) > tollerance) {
                    break;
                } else {
                    width = x + 1;
                }
            }
            BufferedImage yStrip = getRobot().createScreenCapture(new Rectangle(this.x + point.x, this.y + point.y, 1, getHeight() - point.y));
            // showImage(yStrip);
            for (int y = 0; y < yStrip.getHeight(); y++) {
                if (Colors.getColorDifference(rgb, yStrip.getRGB(0, y)) > tollerance) {
                    break;
                } else {
                    height = y + 1;
                }
            }
            Rectangle ret = new Rectangle(this.x + point.x, this.y + point.y, width, height);
            // showImage(getRobot().createScreenCapture(ret));
            System.out.println(System.currentTimeMillis() - start);
            return ret;
        }
        // rectWidth = dwidth - dx + 1;
        // rectHeight = dheight - dy + 1;
        // rectX = x + dx;
        // rectY = y + dy;
        // return null;
        return null;
    }

    private Point scanBlock(Block block, int rgb, double tollerance, int xstart, int ystart) {

        try {

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
                    if (dif < tollerance) {

                        px = new Point(block.x + x, block.y + ymax);
                        break;

                    }
                }

                for (int y = 0; y < ymax; y++) {
                    int pixelColor = block.getImage().getRGB(xmax, y);

                    double dif = Colors.getColorDifference(rgb, pixelColor);
                    if (dif < tollerance) {
                        // block.getImage().setRGB(xmax, y, Color.blue.getRGB());
                        // showImage(block.getImage());
                        py = new Point(block.x + xmax, block.y + y);
                        break;
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

        public int getRGB(int x2, int y2) {
            return getImage().getRGB(x2 - x, y2 - y);
        }

        private BufferedImage getImage() {
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
            Rectangle rec;
            BufferedImage img = getRobot().createScreenCapture(rec = new Rectangle(ScreenResource.this.x + this.x, ScreenResource.this.y + y, blockSize, blockSize));
            System.out.println("Create Screen shot " + rec);
            // showImage(img);
            image = new SoftReference<BufferedImage>(img);
            return img;
        }

        /**
         * @param img
         */

    }

    private HashMap<Integer, HashMap<Integer, Block>> blocks = new HashMap<Integer, HashMap<Integer, Block>>();

    private int getRGB(int x, int y) {
        robot = getRobot();
        Block block = getBlock(x, y);
        return block.getRGB(x, y);
    }

    public void showImage(BufferedImage img) {
        try {
            Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(img), null, null);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    private Block getBlock(int x, int y) {
        if (x * blockSize >= getWidth()) {
            return null;
        }
        if (y * blockSize >= getHeight()) {
            return null;
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

    private Robot getRobot() {
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
