package jd.captcha.specials;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;

public class Cbrsharsk {
    static void clearlines(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xb2b2b2) {
                    int w = captcha.grid[x + 1][y] != 0xb2b2b2 ? 1 : 0;
                    w += captcha.grid[x + 1][y + 1] != 0xb2b2b2 ? 1 : 0;
                    w += captcha.grid[x][y + 1] != 0xb2b2b2 ? 1 : 0;
                    w += captcha.grid[x - 1][y + 1] != 0xb2b2b2 ? 1 : 0;
                    w += captcha.grid[x - 1][y - 1] != 0xb2b2b2 ? 1 : 0;
                    w += captcha.grid[x + 1][y - 1] != 0xb2b2b2 ? 1 : 0;
                    w += captcha.grid[x - 1][y] != 0xb2b2b2 ? 1 : 0;
                    w += captcha.grid[x][y - 1] != 0xb2b2b2 ? 1 : 0;
                    if (w < 3) grid[x][y] = 0xb2b2b2;

                }
            }
        }
        captcha.grid=grid;
    }

    static void toBlack(Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xb2b2b2) {
                    captcha.grid[x][y] = 0x000000;
                }
                else
                    captcha.grid[x][y]=0xffffff;

            }
        }
    }

    public static Letter[] getLetters(Captcha captcha) {
//        BasicWindow.showImage(captcha.getImage());


        clearlines(captcha);
        clearlines(captcha);
        clearlines(captcha);

         toBlack(captcha);

        captcha.removeSmallObjects(1, 1, 3);
//        BasicWindow.showImage(captcha.getImage());
        Letter[] lets = EasyCaptcha.getLetters(captcha);
        for (Letter letter : lets) {
            letter.resizetoHeight(30);
            letter.betterAlign(10, 10);
        }
        return lets;
    }
}
