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
import java.io.File;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.GifDecoder;

public class Rbkprr {

    public static void prepareCaptcha(File file) {
        try {
            JAntiCaptcha jac = new JAntiCaptcha("rbkprr");
            jac.getJas().setColorType("RGB");
            GifDecoder d = new GifDecoder();
            d.read(file.getAbsolutePath());
            int n = d.getFrameCount();
            Captcha[] frames = new Captcha[d.getFrameCount()];
            for (int i = 0; i < n; i++) {
                BufferedImage frame = d.getFrame(i);
                frames[i] = jac.createCaptcha(frame);
            }

            int[][] colorGrid = new int[frames[0].getWidth()][frames[0].getHeight()];
            int[][] counterGrid = new int[frames[0].getWidth()][frames[0].getHeight()];

            for (int i = 0; i < frames.length; i++) {
                for (int x = 0; x < colorGrid.length; x++) {
                    for (int y = 0; y < colorGrid[0].length; y++) {
                        int color = frames[i].getGrid()[x][y];
                        if (colorGrid[x][y] == color && colorGrid[x][y] != 16777215)
                            counterGrid[x][y]++;
                        else {
                            if (color != 16777215) colorGrid[x][y] = color;
                            counterGrid[x][y]--;
                        }
                    }
                }
            }

            for (int x = 0; x < colorGrid.length; x++) {
                for (int y = 0; y < colorGrid[0].length; y++) {
                    if (counterGrid[x][y] < 1)
                        colorGrid[x][y] = 16777215;
                    else
                        colorGrid[x][y] = 0;
                }
            }

            frames[0].setGrid(colorGrid);
            // File fileOut = new File(file.getParent() + "\\out\\" +
            // file.getName().replace("gif", "png"));
            ImageIO.write(frames[0].getImage(1), "png", file);
        } catch (Exception e) {

        }
    }
}
