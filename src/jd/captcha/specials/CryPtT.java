package jd.captcha.specials;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;

public class CryPtT {

    private static int[] getColorAndPoint(Captcha captcha) {
        int y1 = 0;
        int color=0xffffff;
        for (; y1 < captcha.getHeight(); y1++) {
            if (captcha.grid[1][y1] != 0xffffff && captcha.grid[captcha.getWidth() - 2][y1] != 0xffffff) {
                color = captcha.grid[0][y1];
                break;
            }
        }
        int y2 = captcha.getHeight()-1;
        for (; y2 >= 0; y2--) {
            if (captcha.grid[1][y2] != 0xffffff && captcha.grid[captcha.getWidth() - 2][y2] != 0xffffff) break;
        }
        return new int[] {y1, y2, color};
    }
    private static void toBlack(Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    captcha.grid[x][y] = 0x000000;
                } else
                    captcha.grid[x][y] = 0xffffff;

            }
        }
    }
    private static void markColoredLayer(Captcha captcha, int[] point) {

        for (int x = 1; x < captcha.getWidth()-1; x++) {
            for (int y = point[0]; y < point[1]+1; y++) {
                if(captcha.grid[x][y]==0xffffff)
                {
                    captcha.grid[x][y-1]=0xff0000;
                    captcha.grid[x][y]=0xff0000;
                    captcha.grid[x][y+1]=0xff0000;
                    captcha.grid[x+1][y+1]=0xff0000;
                    captcha.grid[x+1][y]=0xff0000;
                    captcha.grid[x+1][y-1]=0xff0000;
                    captcha.grid[x-1][y]=0xff0000;
                    captcha.grid[x-1][y-1]=0xff0000;
                    captcha.grid[x-1][y+1]=0xff0000;


                }
                else if(captcha.grid[x][y]==point[2])captcha.grid[x][y]=0xffffff;
                
            }
        }
        for (int y = point[0]; y < point[1]+1; y++) {
            captcha.grid[0][y]=0xffffff;
            captcha.grid[captcha.getWidth()-1][y]=0xffffff;

        }
    }
    private static PixelObject[] getObjects(Captcha captcha)
    {
        PixelObject[] ret = new PixelObject[3];
        for (int i = 0; i < ret.length; i++) {
            ret[i]=new PixelObject(captcha);
            
        }
        for (int x = 0; x < (captcha.getWidth()/3+captcha.getWidth()/10); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                     ret[0].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        for (int x = (captcha.getWidth()/3-captcha.getWidth()/20); x < (captcha.getWidth()*2/3+captcha.getWidth()/20); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[1].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        for (int x = (captcha.getWidth()*2/3-captcha.getWidth()/10); x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[2].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        return ret;
    }
    public static Letter[] getLetters(Captcha captcha) {
        // BasicWindow.showImage(captcha.getImage());

//         BasicWindow.showImage(captcha.getImage());

//        captcha.crop(2, 2, 2, 2);
//        captcha.removeSmallObjects(0.5, 0.5, 7);
        int[] cp = getColorAndPoint(captcha);
        markColoredLayer(captcha, cp);
        toBlack(captcha);
        PixelObject[] os = getObjects(captcha);

        Letter[] lets = new Letter[os.length];
        for (int i = 0; i < lets.length; i++) {
            lets[i] = os[i].toLetter();
            lets[i].resizetoHeight(30);
        }
        return lets;
    }
}
