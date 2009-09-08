package jd.captcha.specials;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.nutils.Colors;

public class UgtF {
    static void clearlines(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    int w = captcha.grid[x + 1][y] != 0xffffff ? 1 : 0;
                    w += captcha.grid[x + 1][y + 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x][y + 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x - 1][y + 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x - 1][y - 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x + 1][y - 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x - 1][y] != 0xffffff ? 1 : 0;
                    w += captcha.grid[x][y - 1] != 0xffffff ? 2 : 0;
                    if (w < 4) grid[x][y] = 0xffffff;

                }
            }
        }
        captcha.grid = grid;
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < 5; y++) {
                captcha.grid[x][y] = 0xffffff;
            }
        }
    }

    private static void toBlack(Captcha captcha) {
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    captcha.grid[x][y] = 0x000000;
                }

            }
        }
    }
    public static Letter[] getLetters(Captcha captcha) {

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                float[] cmyk = Colors.rgb2cmyk(captcha.grid[x][y]);
                int add = 0;
                for (float f : cmyk) {
                    add += f;
                }
                if (add < 35) captcha.grid[x][y] = 0xffffff;
            }
        }
        clearlines(captcha);
        toBlack(captcha);
        return EasyCaptcha.getLetters(captcha);
    }
}
