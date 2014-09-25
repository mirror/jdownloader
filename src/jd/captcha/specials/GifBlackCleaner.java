//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.captcha.specials;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.GifDecoder;
import jd.nutils.Colors;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.seamless.util.io.IO;

public class GifBlackCleaner {
    public static void main(String args[]) throws IOException, InterruptedException, DialogClosedException, DialogCanceledException {

        byte[] bytes = IO.readBytes(new File("animatedBlack.gif"));
        BufferedImage image = toBufferedImage(new ByteArrayInputStream(bytes));

        Dialog.getInstance().showConfirmDialog(0, "Image", "", new ImageIcon(image), null, null);

    }

    private static void cleanBlack(int x, int y, int[][] grid) {
        for (int x1 = Math.max(x - 2, 0); x1 < Math.min(x + 2, grid.length); x1++) {
            for (int y1 = Math.max(y - 2, 0); y1 < Math.min(y + 2, grid[0].length); y1++) {
                if (grid[x1][y1] == 0x000000) {
                    grid[x1][y1] = 0xffffff;
                    cleanBlack(x1, y1, grid);
                }
            }
        }
    }

    private static BufferedImage toBufferedImage(InputStream is) throws InterruptedException {

        try {
            JAntiCaptcha jac = new JAntiCaptcha();
            jac.getJas().setColorType("RGB");
            GifDecoder d = new GifDecoder();
            d.read(is);
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
                        if (hsb[2] < 0.2 && distance < 100) {
                            continue;
                        }

                        max = Math.max(max, frames[i].getGrid()[x][y]);
                    }
                    int mainColor = 0;
                    int mainCount = 0;
                    for (Entry<Integer, Integer> col : colors.entrySet()) {
                        if (col.getValue() > mainCount && col.getKey() > 10) {
                            mainCount = col.getValue();
                            mainColor = col.getKey();
                        }
                    }
                    grid[x][y] = mainColor;
                }
            }
            int gl1 = grid[0].length - 1;
            for (int x = 0; x < grid.length; x++) {
                int bl1 = 0;
                int bl2 = 0;
                for (int i = Math.max(0, x - 6); i < Math.min(grid.length, x + 6); i++) {
                    if (grid[i][0] == 0x000000) {
                        bl1++;
                    }
                    if (grid[i][gl1] == 0x000000) {
                        bl2++;
                    }
                }
                if (bl1 == 12) {
                    cleanBlack(x, 0, grid);
                }
                if (bl2 == 12) {
                    cleanBlack(x, gl1, grid);
                }
            }
            gl1 = grid.length - 1;

            for (int y = 0; y < grid.length; y++) {
                int bl1 = 0;
                int bl2 = 0;
                for (int i = Math.max(0, y - 6); i < Math.min(grid[0].length, y + 6); i++) {
                    if (grid[0][i] == 0x000000) {
                        bl1++;
                    }
                    if (grid[gl1][i] == 0x000000) {
                        bl2++;
                    }
                }
                if (bl1 == 12) {
                    cleanBlack(0, y, grid);
                }
                if (bl2 == 12) {
                    cleanBlack(gl1, y, grid);
                }
            }
            frames[0].setGrid(grid);

            // BasicWindow.showImage(frames[0].getImage(1));
            return frames[0].getImage(1);

        } finally {

        }
    }
}
