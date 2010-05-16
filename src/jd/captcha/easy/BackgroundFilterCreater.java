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

package jd.captcha.easy;

import java.awt.Image;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;
import jd.controlling.JDLogger;

public class BackgroundFilterCreater {
    /**
     * Erstellt eine Backgroundimage im MethodenOrdner aus den Captchas im
     * Captchaordner der Methode
     * 
     * @param files
     * @param methode
     * @return
     */
    public static File create(EasyMethodFile methode) {
        return create(methode.getCaptchaFolder().listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().matches("(?is).*\\.(jpg|png|gif)");
            }
        }), methode);
    }

    /**
     * Erstellt ein Hintergrundbild im MethodenOrdner aus einer Liste von
     * Dateien
     * 
     * @param files
     * @param methode
     * @return
     */
    @SuppressWarnings("unchecked")
    public static File create(File[] files, EasyMethodFile methode) {
        boolean ignoreBlack = false;
        JAntiCaptcha jac = new JAntiCaptcha(methode.getName());
        Image image = Utilities.loadImage(files[0]);
        Captcha firstCaptcha = jac.createCaptcha(image);
        HashMap<Integer, Integer>[][] grid = new HashMap[firstCaptcha.getWidth()][firstCaptcha.getHeight()];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                grid[x][y] = new HashMap<Integer, Integer>();
                grid[x][y].put(firstCaptcha.getPixelValue(x, y), 0);
            }
        }
        int i = 0;
        for (File file : files) {
            image = Utilities.loadImage(file);
            Captcha captcha = jac.createCaptcha(image);
            if (captcha == null || captcha.getWidth() != firstCaptcha.getWidth() || captcha.getHeight() != firstCaptcha.getHeight()) {
                if (Utilities.isLoggerActive()) {
                    JDLogger.getLogger().info("ERROR Maske und Bild passen nicht zusammmen");
                }
                continue;
            }
            if (i++ == 100) break;

            for (int x = 0; x < captcha.getWidth(); x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    HashMap<Integer, Integer> map = grid[x][y];
                    int val = captcha.getPixelValue(x, y);
                    if (!ignoreBlack || val != 0x000000) {
                        if (map.containsKey(val)) {
                            map.put(val, map.get(val) + 1);
                        } else
                            map.put(val, 0);
                    }
                }
            }
        }
        for (int x = 0; x < firstCaptcha.getWidth(); x++) {
            for (int y = 0; y < firstCaptcha.getHeight(); y++) {
                Set<Entry<Integer, Integer>> map = grid[x][y].entrySet();
                Entry<Integer, Integer> best = new Entry<Integer, Integer>() {

                    public Integer getKey() {
                        return -1;
                    }

                    public Integer getValue() {
                        return -1;
                    }

                    public Integer setValue(Integer value) {
                        return -1;
                    }
                };
                for (Entry<Integer, Integer> entry : map) {
                    if (entry.getValue() > best.getValue()) best = entry;
                }
                firstCaptcha.setPixelValue(x, y, best.getKey());
            }
        }
        File ret = new File(methode.file, "mask_" + System.currentTimeMillis() + ".png");
        try {
            ImageIO.write(firstCaptcha.getImage(), "png", ret);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

}
