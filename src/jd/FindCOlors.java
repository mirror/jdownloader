package jd;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.controlling.JDLogger;
import jd.nutils.Colors;

public class FindCOlors {
    private static JAntiCaptcha JAC;

    public static void main(String[] args) {
        Color darkBack = new Color(0x113E47);
        Color land = new Color(0x93C03D);
        Color arrowlight = new Color(0xFFD423);
        Color arrowdarker = new Color(0xFAA42F);
        JAC = new JAntiCaptcha("linksave.in");
        JDLogger.getLogger().setLevel(Level.WARNING);
        System.out.println("Pfeil Dunkel: " + getBestColor(arrowdarker));
        System.out.println("Meer: " + getBestColor(darkBack));
        System.out.println("Kontinente: " + getBestColor(land));
        System.out.println("Pfeil Hell: " + getBestColor(arrowlight));
    }

    private static String getBestColor(Color arrowdarker) {
        File dir = new File("c:/colors/bm40");
        File bestmatch = null;
        int bestColor = 0;

        double dif = 100000000000.0;
        for (File f : dir.listFiles()) {
            try {

                // BasicWindow.showImage(img);
                BufferedImage img = ImageIO.read(f);
                if (img != null && img.getWidth() > 30) {
                    Captcha cap = JAC.createCaptcha(img);
                    int color = cap.getAverage();

                    double tmp = Colors.getColorDifference(arrowdarker.getRGB(), color);
                    if (tmp < dif) {
                        bestColor = color;
                        bestmatch = f;
                        dif = tmp;

                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return bestColor + " - " + dif + " = " + bestmatch;
    }
}