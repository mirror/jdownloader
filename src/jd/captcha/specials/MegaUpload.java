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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.Colors;
import jd.nutils.JDHash;
import jd.nutils.JDImage;
import jd.parser.html.Form;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class MegaUpload {
    public static Letter[] getLetters(Captcha captcha) {

        captcha.toBlackAndWhite(0.45);

        LetterComperator.MATCH_TABLE = new String[] { "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "1234567890"

        };

        captcha.clean();
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (int i = 0; i < 4; i++) {
            int averageWidth = Math.min(captcha.getWidth(), (int) (captcha.getWidth() / (4 - i)) + 6);
            Letter first = new Letter(averageWidth, captcha.getHeight());

            first.setId(i);

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
            if (r == null) return null;
            first.detected = r;
            Letter b = r.getB();
            // int[] offset = new
            // int[]{r.getIntersectionStartX(),r.getIntersectionStartY()};
            int[] offset = r.getPosition();
            if (offset == null) return null;
            for (int x = offset[0]; x < offset[0] + b.getWidth(); x++) {

                for (int y = offset[1]; y < offset[1] + b.getHeight(); y++) {

                    if (x < captcha.getWidth() && y < captcha.getHeight() && x > 0 && y > 0) {
                        if (x < b.getWidth() && y < b.getHeight() && b.getGrid()[x][y] < 100) {
                            setValue(captcha, x, y, 0xffffff);
                            // setValue(captcha,x-1,y,0xffffff);
                            // setValue(captcha,x+1,y,0xffffff);
                            // setValue(captcha,x,y-1,0xffffff);
                            // setValue(captcha,x,y+1,0xffffff);

                        } else {
                            // captcha.getGrid()[x][y]=0x00ff00;
                        }
                    }

                }
            }
            // BasicWindow.showImage(b.getImage(3));
            // BasicWindow.showImage(r.getIntersectionLetter().getImage(3));
            // BasicWindow.showImage(captcha.getImage(3));

            ret.add(first);
            if (i < 3) {
                System.out.println(r.getDecodedValue() + "");

                captcha.crop(offset[0] + b.getWidth() / 2, 0, 0, 0);

                // BasicWindow.showImage(captcha.getImage(3));
                captcha.removeSmallObjects(0.95, 0.95, 25);
                captcha.clean();
                // BasicWindow.showImage(captcha.getImage(3));

            }
        }
        if (ret.size() < 4) return null;
        return ret.toArray(new Letter[] {});

    }

    private static void setValue(Captcha captcha, int x, int y, int value) {
        if (y < 0 || x < 0) return;
        if (y >= captcha.getHeight() || x >= captcha.getWidth()) return;
        captcha.setPixelValue(x, y, value);

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        return org;
    }

    public static void saveBoders(File file) throws IOException {

        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "megaupload.com2";

        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        Image captchaImage = ImageIO.read(file);
//        captchaImage=JDImage.getScaledImage((BufferedImage)captchaImage,64, 64);
//        BasicWindow.showImage(captchaImage);

        Captcha captcha = Captcha.getCaptcha(captchaImage, jac);
//        BasicWindow.showImage(captcha.getImage(1));
        int[][] grid = new int[captcha.getWidth()][captcha.getHeight()];
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                int avg = captcha.getAverage(x, y, 2, 2);
                double dif = Math.min(Colors.getColorDifference(avg, 0), Colors.getColorDifference(avg, 0xffffff));

                if (dif > 43.0) {
                    grid[x][y] = 0;
                } else {
                    grid[x][y] = 0xffffff;
                }

            }
        }
        captcha.setGrid(grid);
        captcha.blurIt(2);
        captcha.toBlackAndWhite(0.99);
        try {
//            BasicWindow.showImage(captcha.getImage(1));
            file = JDUtilities.getResourceFile("caps/mu/borders/" + System.currentTimeMillis() + ".png");
            file.mkdirs();
            ImageIO.write((RenderedImage) captcha.getImage(1), "png", file);

            System.out.println(file);
        } catch (IOException e1) {
            JDLogger.exception(e1);
        }
    }

    public static void main(String args[]) throws IOException {

        int i = 0;
        HashMap<String, File> map = new HashMap<String, File>();
        File dir = JDUtilities.getResourceFile("caps/megaupload.com/captchas/");
        for (File f : dir.listFiles()) {

            map.put(JDHash.getMD5(f), f);
        }
        int ii = 0;
        while (true) {
            i++;
            Browser br = new Browser();
            br.setCookie("http://megaupload.com", "l", "en");
            br.getPage("http://www.megaupload.com/?d=ML38NV20");
            Form form = br.getForm(0);

            String captcha = form.getRegex("Enter this.*?src=\"(.*?gencap.*?)\"").getMatch(0);

            br.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
            URLConnectionAdapter con = br.openGetConnection(captcha);
            File file;
            Browser.download(file = JDUtilities.getResourceFile("caps/megaupload.com/captchas/caps_" + System.currentTimeMillis() + ".png"), con);

            String hash = JDHash.getMD5(file);

            if (map.containsKey(hash)) {
                ii++;

                System.out.println("Rem " + ii);
                file.delete();
            } else {
                map.put(hash, file);

                saveBoders(file);
            }

        }

        // String methodsPath = UTILITIES.getFullPath(new String[] {
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(),
        // "jd", "captcha", "methods" });
        //    
        // String hoster = "megaupload.com";
        //
        // JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        //
        // LinkedList<Letter> letterdb = jac.letterDB;
        // LinkedList<Letter> newlb = new LinkedList<Letter>();
        // for (Iterator<Letter> it1 = letterdb.iterator(); it1.hasNext();) {
        // Letter l = it1.next();
        // boolean foundinNew = false;
        // for (Iterator<Letter> it = newlb.iterator(); it.hasNext();) {
        // Letter n = it.next();
        // if (n == l) continue;
        // if (l.getDecodedValue().equalsIgnoreCase(n.getDecodedValue())) {
        //
        // LetterComperator lc = new LetterComperator(l, n);
        // lc.setOwner(jac);
        // lc.run();
        // double vp = lc.getValityPercent();
        //
        // if (vp < 1.0) {
        // foundinNew = true;
        // BasicWindow.showImage(l.getImage(), vp + "");
        // BasicWindow.showImage(n.getImage(), vp + "");
        // break;
        // }
        //
        // }
        //
        // }
        // if (!foundinNew) {
        // newlb.add(l);
        // }
        //
        // }
        //
        // jac.letterDB = newlb;
        // jac.saveMTHFile();

    }

}