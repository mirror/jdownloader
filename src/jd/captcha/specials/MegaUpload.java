//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;

/**
 * 
 * 
 * @author JD-Team
 */
public class MegaUpload {
    public static Letter[] getLetters(Captcha captcha) {
captcha.toBlackAndWhite(0.45);
        captcha.clean();
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (int i = 0; i < 4; i++) {
            int averageWidth = Math.min(captcha.getWidth(), (int) (captcha.getWidth() / (4 - i)) + 8);
            Letter first = new Letter(averageWidth, captcha.getHeight());
            first.setOwner(captcha.owner);
            for (int x = 0; x < averageWidth; x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    try {
                        first.grid[x][y] = captcha.grid[x][y];
                    } catch (Exception e) {

                    }
                }
            }

            LetterComperator r = captcha.owner.getLetter(first);

            
            
            Letter b = r.getB();
//            int[] offset = new int[]{r.getIntersectionStartX(),r.getIntersectionStartY()};
            int[] offset = r.getPosition();
            
            for(int x=offset[0];x<offset[0]+b.getWidth();x++){
                
                for(int y=offset[1];y<offset[1]+b.getHeight();y++){
                  
                    if(x<captcha.getWidth()&&y<captcha.getHeight()&&x>0&&y>0){
                        if(x<b.getWidth()&&y<b.getHeight()&&b.getGrid()[x][y]<100){
                        captcha.getGrid()[x][y]=0xffffff;
                        }else{
                           // captcha.getGrid()[x][y]=0x00ff00; 
                        }
                    }
                    
                } 
            }
//            BasicWindow.showImage(b.getImage(3));
//            BasicWindow.showImage(r.getIntersectionLetter().getImage(3));
//            BasicWindow.showImage(captcha.getImage(3));
            ret.add(first);
            if (i < 3) {
                System.out.println(r.getDecodedValue() + "");
                captcha.crop(offset[0]+b.getWidth()/2, 0, 0, 0);
               
//                BasicWindow.showImage(captcha.getImage(3));
                captcha.removeSmallObjects(0.95, 0.95,25);
                captcha.clean();
//                BasicWindow.showImage(captcha.getImage(3));

            }
        }
        if (ret.size() < 4) return null;
        return ret.toArray(new Letter[] {});

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        return org;
    }
}