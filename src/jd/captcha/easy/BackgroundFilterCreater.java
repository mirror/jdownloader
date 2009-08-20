package jd.captcha.easy;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import jd.controlling.JDLogger;

import jd.captcha.pixelgrid.Captcha;

import jd.captcha.JAntiCaptcha;

import jd.captcha.utils.Utilities;

public class BackgroundFilterCreater {
    /**
     * Erstellt eine Backgroundimage im MethodenOrdner aus den Captchas im Captchaordner der Methode
     * @param files
     * @param methode
     * @return
     */
    public static File create(EasyMethodeFile methode)
    {
        return create(methode.getCaptchaFolder().listFiles(), methode);
    }
    /**
     * Erstellt ein Hintergrundbild im MethodenOrdner aus einer Liste von Dateien
     * @param files
     * @param methode
     * @return
     */
    @SuppressWarnings("unchecked")
    public static File create(File[] files, EasyMethodeFile methode) {
        JAntiCaptcha jac = new JAntiCaptcha(Utilities.getMethodDir(), methode.getName());
        Image image = Utilities.loadImage(files[0]);
        Captcha firstCaptcha = jac.createCaptcha(image);
        HashMap<Integer, Integer>[][] grid = new HashMap[firstCaptcha.getWidth()][firstCaptcha.getHeight()];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                grid[x][y] = new HashMap<Integer, Integer>();
                grid[x][y].put(firstCaptcha.getPixelValue(x, y), 0);
            }
        }
        int i =0;
        for (File file : files) {
            image = Utilities.loadImage(file);
            Captcha captcha = jac.createCaptcha(image);
            if (captcha == null || captcha.getWidth() != firstCaptcha.getWidth() || captcha.getHeight() != firstCaptcha.getHeight()) {
                if (Utilities.isLoggerActive()) {
                    JDLogger.getLogger().info("ERROR Maske und Bild passen nicht zusammmen");
                }
                continue;
            }
            if(i++==100)break;

            for (int x = 0; x < captcha.getWidth(); x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    HashMap<Integer, Integer> map = grid[x][y];
                    int val = captcha.getPixelValue(x, y);
                    if (map.containsKey(val)) {
                        map.put(val, map.get(val) + 1);
                    } else
                        map.put(val, 0);
                }
            }
        }
        for (int x = 0; x < firstCaptcha.getWidth(); x++) {
            for (int y = 0; y < firstCaptcha.getHeight(); y++) {
                Set<Entry<Integer, Integer>> map = grid[x][y].entrySet();
                Entry<Integer, Integer> best = new Entry<Integer, Integer>() {

                    public Integer getKey() {
                        // TODO Auto-generated method stub
                        return -1;
                    }

                    public Integer getValue() {
                        // TODO Auto-generated method stub
                        return -1;
                    }

                    public Integer setValue(Integer value) {
                        // TODO Auto-generated method stub
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
            ImageIO.write((RenderedImage) firstCaptcha.getImage(), "png", ret);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

}
