package jd.captcha.specials;
import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.Utilities;
import jd.nutils.Colors;

/**
 * Captcha Recognition for gmd,uploadr
 * 
 * @author JTB
 */
public class GmdMscCm {
    Comparator<Integer> isElementColor = new Comparator<Integer>() {

        public int compare(Integer o1, Integer o2) {
            int c = o1;
            int c2 = o2;
            if (c < 0 || c == 0xffffff) return 0;
            int[] hsvC = Colors.rgb2hsv(c);
            int[] hsvC2 = Colors.rgb2hsv(c2);
            if(hsvC[0]==0&&hsvC2[0]==0&&hsvC[1]==0&&hsvC2[1]==0)return 1;
            if(hsvC[0]==hsvC2[0]&&hsvC[2]==hsvC2[2])return 1;
            return 0;
        }

    };


    public static void main(String[] args) {
        File[] list = new File("/home/dwd/.jd_home/captchas/lnkcrptwsCircles").listFiles();
       for (int i = 20; i < 30; i++) {
        File file = list[i];
        new GmdMscCm(file).getResult();

    }
    }
    /**
     * get objects with different color
     * 
     * @param grid
     * @return
     */
    public ArrayList<PixelObject> getObjects(Captcha grid) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                int c = grid.getGrid()[x][y];
                if (c < 0 || c == 0xffffff) continue;
                PixelObject n = new PixelObject(grid);
                n.add(x, y, c);
                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (o.isTouching(x, y, true, 26, 26) && isElementColor.compare(o.getMostcolor(), c)==1) {
                        merge.add(o);
                    }
                }
                if (merge.size() == 0) {
                    ret.add(n);
                } else if (merge.size() == 1) {
                    merge.get(0).add(n);
                } else {
                    for (PixelObject po : merge) {
                        ret.remove(po);
                        n.add(po);
                    }
                    ret.add(n);
                }
            }
        }
        return ret;
    }
    private File captchafile = null;
    private JAntiCaptcha jac = new JAntiCaptcha("EasyCaptcha");
    public GmdMscCm(File file) {
        this.captchafile = file;
    }
    public int[] getResult() {
        
        Image captchaImage = Utilities.loadImage(captchafile);
        Captcha captcha = jac.createCaptcha(captchaImage);
        captcha.setCaptchaFile(captchafile);
        captcha.crop(2, 2, 2, 2);
        ArrayList<PixelObject> ob = getObjects(captcha);
//        merge(ob);
        // delete the lines

        for (Iterator<PixelObject> iterator = ob.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = (PixelObject) iterator.next();
            int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
            if (ratio > 115 || ratio < 85||pixelObject.getSize()<35) iterator.remove();
        }
        Circle circle = new Circle(captcha, ob);
        circle.inBorder = 3;
        circle.outBorder = 4;
        circle.isElementColor = isElementColor;
//         BasicWindow.showImage(captcha.getImage());
//         BasicWindow.showImage(circle.getOpenCircle().getImage());
        Letter openCircle = circle.getOpenCircle();
        if (openCircle == null) return null;
        int x = openCircle.getLocation()[0] + (openCircle.getWidth() / 2);
        int y = openCircle.getLocation()[1] + (openCircle.getHeight() / 2);
        return new int[] {x, y};
    }
}
