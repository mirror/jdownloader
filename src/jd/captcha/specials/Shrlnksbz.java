package jd.captcha.specials;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.easy.CPoint;
import jd.captcha.easy.ColorTrainer;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;

public class Shrlnksbz {
    public static Letter[] getLetters(Captcha captcha) {
        int[][] backup = Captcha.getGridCopy(captcha.grid);
        File file = captcha.owner.getResourceFile("CPointsback.xml");
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
                    if (!cpBestDist2.isForeground())
                        captcha.setPixelValue(x, y, 0xffffff);
                    else
                        captcha.setPixelValue(x, y, 0x000000);
                } else if (cpBestDist1 != null) {
                    if (!cpBestDist1.isForeground())
                        captcha.setPixelValue(x, y, 0xffffff);
                    else
                        captcha.setPixelValue(x, y, 0x000000);
                }
            }
        }
        captcha.blur(3, 3, 7);
        captcha.toBlackAndWhite(0.8);
//        BasicWindow.showImage(captcha.getImage());

        captcha.resizetoHeight(100);
        captcha.setOrgGrid(captcha.grid);
//        BasicWindow.showImage(captcha.getImage().getScaledInstance(600, 400, 1));
        captcha.owner.setLetterNum(3);
        Vector<PixelObject> obs = captcha.getObjects(0.8, 0.8);
        Collections.sort(obs);
        Letter[] lets = new Letter[obs.size()];
        for (int i = 0; i < lets.length; i++) {
            lets[i]=obs.get(i).toLetter();
            lets[i].resizetoHeight(30);
        }
        return lets;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        JAntiCaptcha jac = new JAntiCaptcha("shrlnksbz");
        Image ret;
        try {
            ret = ImageIO.read(new File("/home/dwd/.jd_home/captchas/captcha.gif"));
            Captcha captcha = jac.createCaptcha(ret);
            getLetters(captcha);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
