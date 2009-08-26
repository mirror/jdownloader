package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import jd.captcha.gui.BasicWindow;

import jd.captcha.pixelobject.PixelObject;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;

public class Lnkcrptws {

    static boolean equalElements(int c, int c2) {
        return c==c2;
    }

    static boolean isWhite(int c) {
        return c < 0 || c == 0xffffff;
    }
    /**
     * get objects with different color
     * @param grid
     * @return
     */
    static ArrayList<PixelObject> getObjects(Captcha grid) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                int c = grid.getGrid()[x][y];
                if (isWhite(c)) continue;
                PixelObject n = new PixelObject(grid);
                n.add(x, y, c);
                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (o.isTouching(x, y, true, 5, 5) && equalElements(c, o.getMostcolor())) {
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

    public static Letter[] getLetters(Captcha captcha) {
        ArrayList<PixelObject> ob = getObjects(captcha);
        //delete the lines
        for (Iterator<PixelObject> iterator = ob.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = (PixelObject) iterator.next();
            int ratio = pixelObject.getHeight()*100/pixelObject.getWidth();
            if(ratio>105 || ratio<95)
                iterator.remove();
        }
        Circle circle = new Circle(captcha, ob);
        circle.inBorder=3;
        circle.outBorder=2;
        circle.isElementColor=new Comparator<Integer>() {

            public int compare(Integer o1, Integer o2) {
                return o1.equals(o2)?1:0;
            }};
//            BasicWindow.showImage(captcha.getImage());
//            BasicWindow.showImage(circle.getOpenCircle().getImage());
        return circle.getOpenCirclePositionAsLetters();
    }
}
