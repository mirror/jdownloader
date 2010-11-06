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

import java.awt.Image;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;
import jd.utils.JDUtilities;

public class Recaptcha {

    private static void autoRemoveCurves(final Captcha captcha) {

        captcha.clean();
        final int[][] orgGrid = captcha.getGridCopy();
        final int height = captcha.getHeight();
        final int width = captcha.getWidth();
        int[] midCurve = new int[width];
        int average = 0;
        for (int x = 0; x < width; x++) {
            // get topmax and bottommax
            int topMax = -1;
            int bottomMax = -1;
            for (int y = 0; y < height; y++) {
                if (topMax == -1 && orgGrid[x][y] < 0x111111) {
                    topMax = y;
                }
                if (orgGrid[x][y] < 0x111111) {
                    bottomMax = y;
                }
            }
            if (bottomMax < 0) {
                bottomMax = height - 1;
            }
            if (topMax < 0) {
                topMax = 0;
            }
            // captcha.grid[x][bottomMax] = 0x00ff00;
            // captcha.grid[x][topMax] = 0x0000ff;
            midCurve[x] = (bottomMax + topMax) / 2;
            average += midCurve[x];
        }
        average /= midCurve.length;
        midCurve = Recaptcha.smooth(midCurve, 8);
        for (int x = 0; x < midCurve.length; x++) {
            captcha.grid[x][midCurve[x]] = 0xff0000;
            captcha.grid[x][average] = 0x00ff00;

        }

        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {
                final int newY = y + midCurve[x] - average;
                if (newY >= 0 && newY < height) {
                    captcha.grid[x][y] = orgGrid[x][newY];
                } else {
                    captcha.grid[x][y] = 0xffffff;
                }

            }
        }

    }

    private static File handle(final Captcha captcha, final File file) throws IOException {
        file.deleteOnExit();
        ImageIO.write(captcha.getImage(), "jpg", file);

        return file;
    }

    public static void main(final String args[]) throws IOException {

        final File[] list = JDUtilities.getResourceFile("/captchas/").listFiles(new FilenameFilter() {

            public boolean accept(final File dir, final String name) {
                return name.startsWith("filesonic.com");
            }

        });
        final int id = (int) (Math.random() * (list.length - 1));

        final File f = list[id];
        Recaptcha.prepareCaptcha(f);

    }

    public static File[] prepareCaptcha(final File file) {
        try {
            final JAntiCaptcha jac = new JAntiCaptcha("recaptcha");
            jac.getJas().setColorType("RGB");
            final Image captchaImage = Utilities.loadImage(file);
            final Captcha captcha = jac.createCaptcha(captchaImage);
            if (captcha != null) {
                captcha.setCaptchaFile(file);
            }
            captcha.toBlackAndWhite(0.6);
            captcha.clean();

            //

            Recaptcha.autoRemoveCurves(captcha);
            captcha.resizetoHeight(100);
            int white = 0;
            int maxWhite = 0;
            int maxWhiteX = 0;
            columns: for (int x = 0; x < captcha.getWidth(); x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    if (captcha.grid[x][y] <= 0x111111) {
                        if (white > maxWhite) {
                            maxWhite = white;
                            maxWhiteX = x;
                        }
                        white = 0;
                        continue columns;
                    }
                }
                white++;
            }

            final Captcha[] captchas = new Captcha[2];
            captchas[0] = new Captcha(maxWhiteX - maxWhite / 2, captcha.getHeight());
            captchas[0].setOwner(jac);
            for (int x = 0; x < captchas[0].getWidth(); x++) {
                captchas[0].grid[x] = captcha.grid[x];
            }
            captchas[1] = new Captcha(captcha.getWidth() - (maxWhiteX - maxWhite / 2), captcha.getHeight());
            captchas[1].setOwner(jac);
            for (int x = 0; x < captchas[1].getWidth(); x++) {
                captchas[1].grid[x] = captcha.grid[x + captchas[0].getWidth()];
            }

            captchas[0].clean();
            captchas[1].clean();

            final File[] ret = new File[2];
            ret[0] = Recaptcha.handle(captchas[0], new File(file.getAbsolutePath() + "_0.jpg"));
            ret[1] = Recaptcha.handle(captchas[1], new File(file.getAbsolutePath() + "_1.jpg"));

            // Recaptcha.autoRemoveCurves(captcha);
            //
            // Recaptcha.autoRemoveCurves(captcha);
            return ret;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int[] smooth(final int input[], final int radius) {
        final int output[] = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            int count = 0;
            int value = 0;
            for (int j = -radius; j <= radius; j++) {
                final int c = i + j;
                if (c >= 0 && c < input.length) {
                    value += input[c];
                    count++;
                }
            }
            output[i] = value / count;
        }
        return output;
    }
}
