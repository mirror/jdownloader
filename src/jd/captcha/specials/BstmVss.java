package jd.captcha.specials;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.easy.CPoint;
import jd.captcha.easy.ColorTrainer;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;

public class BstmVss {
    public static Letter[] getLetters(Captcha captcha) {
        File file = captcha.owner.getResourceFile("CPoints.xml");
        Vector<CPoint> ret = ColorTrainer.load(file);

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                double bestDist1 = Double.MAX_VALUE;
                CPoint cpBestDist1 = null;
                double bestDist2 = Double.MAX_VALUE;
                CPoint cpBestDist2 = null;
                for (CPoint cp : ret) {
                    double dist = cp.getColorDifference(captcha.grid[x][y]);

                    if (bestDist1 > dist) {
                        bestDist1 = dist;
                        cpBestDist1 = cp;
                    }
                    if (dist < cp.getDistance()) {
                        if (bestDist2 > dist) {
                            bestDist2 = 0;
                            cpBestDist2 = cp;
                        }
                    }
                }
                if (cpBestDist2 != null) {
                    if (!cpBestDist2.isForeground()) captcha.setPixelValue(x, y, 0xFFFFFF);

                } else if (cpBestDist1 != null) {
                    if (!cpBestDist1.isForeground()) captcha.setPixelValue(x, y, 0xFFFFFF);
                }
            }
        }
        ArrayList<PixelObject> os = PpscnRg.getObjects(captcha, 2);
        Letter[] lets = new Letter[os.size()];
        for (int i = 0; i < lets.length; i++) {
            lets[i] = os.get(i).toLetter();
        }
        return lets;
    }

    private static double getM(Letter letter) {
        return Math.atan(((double) (letter.getHeight())) / ((double) letter.getWidth())) * 10;
    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        int min = 0;
        int h = 0;
        for (Letter letter : org) {
            double m = getM(letter);
//            System.out.println("m"+m);

            double del = 0;
            // if (letter.grid[letter.getWidth()-1][0] != 0xffffff ||
            // letter.grid[0][letter.getHeight()-1] == 0xffffff) m =15-m;
            if (m > 14) del = m;
            int x = letter.getLocation()[0] + letter.getWidth();
            int y = letter.getLocation()[1] + letter.getHeight();
            if (x < 60) {
                if (letter.grid[0][0] != 0xffffff || letter.grid[letter.getWidth() - 1][letter.getHeight() - 1] != 0xffffff)
                {
                    if(y!=100&&y!=76)
                    m += 15;
                }
                else
                    m=15-m;
                m += 30;
            } else {
                if (letter.grid[0][0] != 0xffffff || letter.grid[letter.getWidth() - 1][letter.getHeight() - 1] != 0xffffff)
                    m += 15;
                else
                    m = 15 - m;
            }
//            System.out.println(m);

            if (m > 59)
                m = 0;
            else
                m -= del;
            if (letter.grid[0][0] == 0xff0000 || letter.grid[letter.getWidth() - 1][0] == 0xff0000) {
                m = (int) Math.round((double)m / (double)5);
//                System.out.println("m"+m);

                h = (int) m;
            } else
                min = (int) m;
//            BasicWindow.showImage(letter.getImage(), x + ":" + y + ":" + m);
        }
        String time = "";
        if (h < 10) time += "0";
        time += h;
        time += ":";
        if (min < 10) time += "0";
        time += min;
        char[] tx = time.toCharArray();
        Letter[] ret = new Letter[tx.length];
        for (int i = 0; i < ret.length; i++) {
            Letter re = new Letter();
            re.setDecodedValue("" + tx[i]);
            LetterComperator let = new LetterComperator(re, re);
            let.setValityPercent(0);
            re.detected = let;
            ret[i] = re;
        }
        return ret;
    }
}
