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
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.Colors;
import jd.nutils.JDHash;
import jd.nutils.zip.Zip;
import jd.parser.html.Form;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class MegaUpload {
    private static final String[] STATICMATCH = new String[] { "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "1234567890" };;

    public static Letter[] getLetters(Captcha captcha) {

        LetterComperator.MATCH_TABLE = STATICMATCH;

        getBorders(captcha);

        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (int i = 0; i < 4; i++) {
            int averageWidth = Math.min(captcha.getWidth(), (int) (captcha.getWidth() / (4 - i)) + 20);
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

            first = first.getSimplified(captcha.owner.getJas().getDouble("simplifyFaktor"));
            LetterComperator r = captcha.owner.getLetter(first);
            if (r == null) return null;

            first.detected = r;
            Letter b = r.getB();

            int[] offset = r.getPosition();

            if (offset == null) return null;
            offset[0] *= captcha.owner.getJas().getDouble("simplifyFaktor");
            offset[1] *= captcha.owner.getJas().getDouble("simplifyFaktor");

            ret.add(first);
            System.out.println(r.getDecodedValue() + "");
            if (i < 3) {
                captcha.crop(offset[0] + b.getWidth() / 2, 0, 0, 0);
                captcha.removeSmallObjects(0.95, 0.95, 15);
                captcha.clean();
            }
        }
        if (ret.size() < 4) return null;
        return ret.toArray(new Letter[] {});
    }

    public static void evaluate(LetterComperator comp, Double value) {
        int[] pos = comp.getPosition();
        if (pos == null) return;
        comp.setTmpExtensionError(pos[0] / 15.0);
    }

    private static void getBorders(Captcha captcha) {
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

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        return org;
    }

    public static void saveBoders(File file) throws IOException {
        String methodsPath = JDUtilities.getResourceFile("jd/captcha/methods").getAbsolutePath();
        String hoster = "megaupload.com2";

        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        Image captchaImage = ImageIO.read(file);

        Captcha captcha = Captcha.getCaptcha(captchaImage, jac);

        getBorders(captcha);
        try {
            file = JDUtilities.getResourceFile("caps/mu/borders/" + System.currentTimeMillis() + ".png");
            file.mkdirs();
            ImageIO.write((RenderedImage) captcha.getImage(1), "png", file);

            System.out.println(file);
        } catch (IOException e1) {
            JDLogger.exception(e1);
        }
    }

    public static void main(String args[]) throws IOException {
        if (true) {
            go();
        }

        if (false) {
            int i = 0;
            HashMap<String, File> map = new HashMap<String, File>();
            File dir = JDUtilities.getResourceFile("caps/megaupload.com/captchas/");
            for (File f : dir.listFiles()) {

                map.put(JDHash.getMD5(f), f);
            }
            int ii = 0;
            int zips = 0;
            while (true) {
                i++;
                File file = downloadCaptcha();
                String hash = JDHash.getMD5(file);

                if (map.containsKey(hash)) {
                    ii++;

                    System.out.println("Rem " + ii);
                    file.delete();
                } else {
                    map.put(hash, file);

                    saveBoders(file);
                }
                System.out.println("List: " + JDUtilities.getResourceFile("caps/mu/borders").listFiles().length);
                if (JDUtilities.getResourceFile("caps/mu/borders").listFiles().length >= 10) {
                    System.out.println("ZIPPY");
                    Zip zip = new Zip(JDUtilities.getResourceFile("caps/mu/borders"), JDUtilities.getResourceFile("caps/mu/mu_caps_" + System.currentTimeMillis() + ".zip"));

                    try {
                        zip.zip();

                        for (File cap : JDUtilities.getResourceFile("caps/mu/borders").listFiles()) {
                            System.out.println("DEL " + cap + " : " + cap.delete());
                        }
                        System.out.println("List: " + JDUtilities.getResourceFile("caps/mu/borders").listFiles().length);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private static File downloadCaptcha() throws IOException {
        Browser br = new Browser();
        br.setCookie("http://megaupload.com", "l", "en");
        br.getPage("http://www.megaupload.com/?d=ML38NV20");
        Form form = br.getForm(0);
        if (form == null) {

            System.exit(1);

        }
        String captcha = form.getRegex("Enter this.*?src=\"(.*?gencap.*?)\"").getMatch(0);

        br.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
        URLConnectionAdapter con = br.openGetConnection(captcha);
        File file = JDUtilities.getResourceFile("caps/megaupload.com/captchas/caps_" + System.currentTimeMillis() + ".png");
        Browser.download(file, con);
        return file;
    }

    private static void go() throws IOException {
        String methodsPath = JDUtilities.getResourceFile("jd/captcha/methods").getAbsolutePath();
        String hoster = "megaupload.com2";

        final JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);

        File f = downloadCaptcha();

        System.out.println(f + "");
        jac.showPreparedCaptcha(f);
    }

}