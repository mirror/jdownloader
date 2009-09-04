package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class FlDRR {
    static void clearlines(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] == 0x008000) {
                    int co;
                    int w = (co = captcha.grid[x + 1][y]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    w += (co = captcha.grid[x + 1][y + 1]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    w += (co = captcha.grid[x][y + 1]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y + 1]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y - 1]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    w += (co = captcha.grid[x + 1][y - 1]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    w += (co = captcha.grid[x][y - 1]) != 0x008000 && co != 0xffffff ? 1 : 0;
                    if (w < 1) grid[x][y] = 0xffffff;
                }
            }
        }
        captcha.grid = grid;
    }

    static void toBlack(Captcha captcha) {
        try {
            int color = captcha.getAverage();

            for (int x = 0; x < captcha.getWidth(); x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    // System.out.println(Colors.getRGBColorDifference2(captcha.grid[x][y],
                    // color));
                    if (Colors.getRGBColorDifference2(captcha.grid[x][y], color) > 25) {
                        captcha.grid[x][y] = 0x000000;
                    } else
                        captcha.grid[x][y] = 0xffffff;

                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    private static void merge(Vector<PixelObject> objects) {
        for (PixelObject pixelObject : objects) {
            if (pixelObject.getArea() < 100) {
                EasyCaptcha.mergeos(pixelObject, objects);
                merge(objects);
                break;
            }
        }
    }
    private static void split(Vector<PixelObject> objects, Captcha captcha)
    {
        if(objects.size()<4)
        EasyCaptcha.getRightletters(objects, captcha, new int[]{70}, new int[] {10,10});
    }
    public static Letter[] getLetters(Captcha captcha) {

        // BasicWindow.showImage(captcha.getImage());

        clearlines(captcha);
        captcha.crop(1, 2, 1, 2);
        
        Vector<PixelObject> objects = captcha.getObjects(0.3, 0.3);
        merge(objects);
        split(objects, captcha);
        Collections.sort(objects);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : objects) {
            Letter let = pixelObject.toLetter();
            let.toBlackAndWhite();
            ret.add(let);
//            BasicWindow.showImage(let.getImage());
        }
        // toBlack(captcha);
        // BasicWindow.showImage(captcha.getImage());
        return ret.toArray(new Letter[] {});

    }
}
