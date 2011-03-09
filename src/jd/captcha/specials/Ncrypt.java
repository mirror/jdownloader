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
import java.io.IOException;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.AnimatedGifEncoder;
import jd.captcha.utils.GifDecoder;
import jd.utils.JDUtilities;

public class Ncrypt {
    public static void main(final String args[]) throws IOException {
        final String hoster = "ncrypt.in";
        final File[] list = JDUtilities.getResourceFile("/captchas/" + hoster).listFiles();
        int id = (int) (Math.random() * (list.length - 1));
        id = 3;
        final File f = list[id];
        // prepareCaptcha(f);

        for (final File file : list) {
            prepareCaptcha(file);

        }
        System.out.println(id);
        System.out.println(f);
    }

    public static void prepareCaptcha(final File file) {
        try {
            final JAntiCaptcha jac = new JAntiCaptcha("ncrypt.in");
            jac.getJas().setColorType("RGB");
            final GifDecoder d = new GifDecoder();
            d.read(file.getAbsolutePath());
            final int n = d.getFrameCount();
            final Captcha[] frames = new Captcha[d.getFrameCount()];
            for (int i = 0; i < n; i++) {
                final BufferedImage frame = d.getFrame(i);
                frames[i] = jac.createCaptcha(frame);
            }

            // BasicWindow.showImage(frames[0].getImage(1));
            ImageIO.write(frames[0].getImage(1), "png", file);
        } catch (final Exception e) {
        }
    }

    public static void setDelay(final File file, final int delay) {
        try {
            final GifDecoder d = new GifDecoder();
            d.read(file.getAbsolutePath());
            final int n = d.getFrameCount();
            final BufferedImage[] frames = new BufferedImage[n];
            for (int j = 0; j < n; j++) {
                final BufferedImage frame = d.getFrame(j);
                frames[j] = frame;
            }
            final AnimatedGifEncoder e = new AnimatedGifEncoder();
            e.start(file.getAbsolutePath());
            e.setRepeat(0);
            e.setDelay(delay);
            for (int j = 0; j < n; j++) {
                e.addFrame(frames[j]);
            }
            e.finish();
        } catch (final Exception e) {
        }
    }
}
