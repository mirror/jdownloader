package jd.captcha.specials;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.GifDecoder;
import jd.nutils.Colors;
import jd.utils.JDUtilities;

public class Linksave {
    public static void main(String args[]) throws IOException {
        File file = new File("C:\\Users\\coalado\\.jd_home\\captchas\\linksave.in\\").listFiles()[7];
        prepareCaptcha(file);
    }

    public static void prepareCaptcha(File file) {
        try {
            JAntiCaptcha jac = new JAntiCaptcha(JDUtilities.getJACMethodsDirectory(), "linksave.in");
            jac.getJas().setColorType("RGB");
            GifDecoder d = new GifDecoder();
            d.read(file.getAbsolutePath());
            int n = d.getFrameCount();
            Captcha[] frames = new Captcha[d.getFrameCount()];
            for (int i = 0; i < n; i++) {
                BufferedImage frame = d.getFrame(i);
                frames[i] = jac.createCaptcha(frame);

            }
            int[][] grid = new int[frames[0].getWidth()][frames[0].getHeight()];

            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[0].length; y++) {
                    int max = 0;
                    HashMap<Integer, Integer> colors = new HashMap<Integer, Integer>();
                    int colorsCount = 0;
                    for (int i = 0; i < frames.length; i++) {
                        float[] hsb = Colors.rgb2hsb(frames[i].getGrid()[x][y]);
                        int distance = Colors.getRGBDistance(frames[i].getGrid()[x][y]);
                        if (!colors.containsKey(frames[i].getGrid()[x][y])) {
                            colors.put(frames[i].getGrid()[x][y], 1);
                            colorsCount++;
                        } else {
                            colors.put(frames[i].getGrid()[x][y], colors.get(frames[i].getGrid()[x][y]) + 1);
                        }
                        if (hsb[2] < 0.8 && distance < 100) continue;

                        max = Math.max(max, frames[i].getGrid()[x][y]);
                    }
                    int mainColor = 0;
                    int mainCount = 0;
                    for (Iterator<Entry<Integer, Integer>> it = colors.entrySet().iterator(); it.hasNext();) {
                        Entry<Integer, Integer> col = it.next();
                        if (col.getValue() > mainCount && col.getKey() > 10) {
                            mainCount = col.getValue();
                            mainColor = col.getKey();
                        }
                    }
                    grid[x][y] = mainColor;
                }
            }
            frames[0].setGrid(grid);
            // BasicWindow.showImage(frames[0].getImage(1));
            ImageIO.write((BufferedImage) (frames[0].getImage(1)), "png", file);
        } catch (Exception e) {

        }

    }
}
