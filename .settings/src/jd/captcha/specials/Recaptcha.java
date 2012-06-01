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
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;
import jd.nutils.Executer;
import jd.utils.JDUtilities;

import org.appwork.utils.IO;

public class Recaptcha {

    private static final int SMOOTH_FACTOR = 8;

    private static void autoRemoveCurves(final Captcha captcha) {
        captcha.clean();
        // Save orginial grid
        final int[][] org = captcha.getGridCopy();
        // find curve
        captcha.blur(5, 5, 3);
        BasicWindow.showImage(captcha.getImage());
        captcha.toBlackAndWhite(0.8);
        BasicWindow.showImage(captcha.getImage());
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
            // debug:print line
            captcha.grid[x][bottomMax] = 0x00ff00;
            captcha.grid[x][topMax] = 0x0000ff;
            midCurve[x] = (bottomMax + topMax) / 2;
            average += midCurve[x];
        }
        // calulate mid line
        average /= midCurve.length;
        // smooth curve
        midCurve = Recaptcha.smooth(midCurve, Recaptcha.SMOOTH_FACTOR);
        for (int x = 0; x < midCurve.length; x++) {
            captcha.grid[x][midCurve[x]] = 0xff0000;
            captcha.grid[x][average] = 0x00ff00;

        }
        BasicWindow.showImage(captcha.getImage());

        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {
                final int newY = y + midCurve[x] - average;
                if (newY >= 0 && newY < height) {
                    captcha.grid[x][y] = org[x][newY];
                } else {
                    captcha.grid[x][y] = 0xffffff;
                }

            }
        }

    }

    public static void main(final String args[]) throws IOException {
        // get a random recaptcha image
        final File[] list = JDUtilities.getResourceFile("/captchas/").listFiles(new FilenameFilter() {

            public boolean accept(final File dir, final String name) {
                return name.startsWith("filesonic.com");
            }

        });
        final int id = (int) (Math.random() * (list.length - 1));

        final File f = list[id];
        final File input = new File("input.jpg");
        input.delete();
        IO.copyFile(f, input);
        // prepare image
        Recaptcha.prepareCaptcha(input);

        final Executer exe = new Executer("tesseract.exe");
        exe.addParameter("input.jpg");
        exe.addParameter("tessoutput");
        exe.addParameter("-l");
        exe.addParameter("eng");
        exe.addParameter("nobatch");
        exe.addParameter("+arc");
        exe.setWaitTimeout(-1);
        exe.start();
        exe.waitTimeout();
        System.out.println(exe.getErrorStream());
        System.out.println(exe.getOutputStream());

        System.out.println(IO.readFileToString(new File("tessoutput.txt")));
    }

    public static void prepareCaptcha(final File file) {
        try {
            final JAntiCaptcha jac = new JAntiCaptcha("recaptcha");
            jac.getJas().setColorType("RGB");
            final Image captchaImage = Utilities.loadImage(file);
            BasicWindow.showImage(captchaImage);
            Captcha captcha = jac.createCaptcha(captchaImage);
            // scale x2
            captcha = jac.createCaptcha(new Hq2x().scale(captcha.getImage()));
            if (captcha != null) {
                captcha.setCaptchaFile(file);
            }
            BasicWindow.showImage(captcha.getImage());

            //
            // try to remove distortion
            Recaptcha.autoRemoveCurves(captcha);
            // find gap between words
            int white = 0;
            int maxWhite = 0;
            columns: for (int x = 0; x < captcha.getWidth(); x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    if (captcha.grid[x][y] <= 0x111111) {
                        if (white > maxWhite) {
                            maxWhite = white;
                        }
                        white = 0;
                        continue columns;
                    }
                }
                white++;
            }
            // TODO: add a defined gap between two words (10 px). (space)
            // Recaptcha.autoRemoveCurves(captcha);
            captcha.toBlackAndWhite(0.6);
            captcha.clean();
            BasicWindow.showImage(captcha.getImage());
            ImageIO.write(captcha.getImage(), "jpg", file);
        } catch (final Exception e) {
            e.printStackTrace();
        }

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
