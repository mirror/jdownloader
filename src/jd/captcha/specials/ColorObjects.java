//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import jd.captcha.JAntiCaptcha;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;
import jd.plugins.DownloadLink;

/**
 * 
 * 
 * @author JD-Team
 */
public class ColorObjects {





    private static ArrayList<PixelObject> getObjects(PixelGrid grid, int tollerance) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getGrid()[x][y] == 0xffffff) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new ArrayList<PixelObject>();
                for (Iterator<PixelObject> it = ret.iterator(); it.hasNext();) {
                    PixelObject o = it.next();

                    if (o.isTouching(x, y, true, 10, 10) && UTILITIES.getColorDifference(grid.getGrid()[x][y], o.getAverage()) < tollerance) {

                        merge.add(o);
                        // n.add(o);
                        //                     
                        // it.remove();
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

                // BasicWindow.showImage(grid.getImage(3), x+"-"+y);
             
            }
            // BasicWindow.showImage(grid.getImage(6), x+"-");
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public static Letter[] getLetters(Captcha captcha) {
      
        ArrayList<PixelObject> os = getObjects(captcha, 10);

        Collections.sort(os, new Comparator<PixelObject>() {
            public int compare(PixelObject a, PixelObject b) {
            if(a.getSize()>b.getSize())return -1;
            if(a.getSize()<b.getSize())return 1;
            return 0;
            }

        });
        
//        for(PixelObject po:os){
//           if(po.getSize()>20) BasicWindow.showImage(po.toLetter().getImage(3));
//        }
        List<PixelObject> ret = os.subList(0, captcha.owner.getLetterNum());
        Collections.sort(ret);
       
Letter[] let= new Letter[ret.size()];

int i=0;
for(PixelObject po:ret){
    let[i++]=po.toLetter();
    BasicWindow.showImage(let[i-1].getImage(3));
    let[i-1].toBlackAndWhite(1.1);
    let[i-1].removeSmallObjects(0.99, 0.99);
    let[i-1].clean();
 
 
}
      
    
        return let;

    }

 

}