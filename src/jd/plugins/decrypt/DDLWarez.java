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

package jd.plugins.decrypt;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jd.PluginWrapper;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.GifDecoder;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class DDLWarez extends PluginForDecrypt {
    static class DDLWarez_Linkgrabber extends Thread {
        public final static int THREADFAIL = 1;
        public final static int THREADPASS = 0;
        int _status;
        private String downloadlink;
        private Form form;
        private boolean gotjob;
        protected ProgressController progress;
        private int Worker_ID;
        private Browser br;

        public DDLWarez_Linkgrabber(ProgressController progress, int id, Browser br) {
            downloadlink = null;
            gotjob = false;
            _status = THREADFAIL;
            Worker_ID = id;
            this.br = br;
            this.progress = progress;
        }

        public String getlink() {
            return downloadlink;
        }

        @Override
        public void run() {
            if (gotjob == true) {
                logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " started!");
                String base = br.getBaseURL();
                String action = form.getAction(base);

                if (action.contains("get_file")) {
                    Browser clone = br.cloneBrowser();
                    for (int retry = 1; retry <= 10; retry++) {
                        try {
                            clone.submitForm(form);

                            downloadlink = clone.getRegex(Pattern.compile("<frame\\s.*?src=\"(.*?)\r?\n?\" (?=(NAME=\"second\"))", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            break;
                        } catch (Exception e) {
                            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " PostRequest-Error, try again!");
                            synchronized (DDLWarez.Worker_Delay) {
                                DDLWarez.Worker_Delay = 1000;
                            }
                        }
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " finished! (NO DOWNLOAD FORM!)");
                    _status = THREADFAIL;
                    progress.increase(1);
                    return;
                }
            }
            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " finished!");
            _status = THREADPASS;
            progress.increase(1);
        }

        public void setjob(Form form) {
            this.form = form;

            gotjob = true;
        }

        public int status() {
            return _status;
        }
    }

    public static Integer Worker_Delay = 250;

    public DDLWarez(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.setReadTimeout(5 * 60 * 1000);
        br.setConnectTimeout(5 * 60 * 1000);
        for (int retry = 1; retry <= 10; retry++) {
            try {
                br.clearCookies(this.getHost());
                br.getPage(parameter);

                String pass = br.getRegex(Pattern.compile("<td>Passwort:</td>.*?<td style=\"padding-left:10px;\">(.*?)</td>.*?</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                Vector<String> passwords = new Vector<String>();
                passwords.add("ddl-warez");
                if (pass != null && !pass.equals("kein Passwort")) {
                    passwords.add(pass);
                }

                Form form = br.getForm(1);
                if (form != null && form.containsHTML("AnimCaptcha")) {
                    File file;
                    int i=10;
                    while (true) {
                        i--;
                        if(i<=0){
                            logger.severe("Could not download Captcha");
                            return null;
                        }
                        file = this.getLocalCaptchaFile(this);
                        Browser.download(file, br.cloneBrowser().openGetConnection("http://www.ddl-warez.org/captcha2/captcha.inc.php"));
                        try {
                            File f = convert(file);
                            if (f == null || !f.exists()) continue;
                            file=f;
                            break;
                        } catch (Exception e) {
                            continue;
                        }

                    }
                    String captcha = getCaptchaCode(file, this, param);
                    form.put("AnimCaptcha", captcha);
                    br.submitForm(form);
                    if (br.containsHTML("Captcha-Fehler")) continue;
                }

                Form[] forms = br.getForms();
                progress.setRange(forms.length);
                DDLWarez_Linkgrabber DDLWarez_Linkgrabbers[] = new DDLWarez_Linkgrabber[forms.length];
                for (int i = 0; i < forms.length; ++i) {
                    synchronized (Worker_Delay) {
                        Thread.sleep(Worker_Delay);
                    }
                    DDLWarez_Linkgrabbers[i] = new DDLWarez_Linkgrabber(progress, i, br.cloneBrowser());
                    DDLWarez_Linkgrabbers[i].setjob(forms[i]);
                    DDLWarez_Linkgrabbers[i].start();
                }
                for (int i = 0; i < forms.length; ++i) {
                    try {
                        DDLWarez_Linkgrabbers[i].join();
                        if (DDLWarez_Linkgrabbers[i].status() == DDLWarez_Linkgrabber.THREADPASS) {
                            DownloadLink link = createDownloadlink(DDLWarez_Linkgrabbers[i].getlink());
                            link.setSourcePluginPasswords(passwords);
                            decryptedLinks.add(link);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return decryptedLinks;
            } catch (DecrypterException e2) {
                throw e2;
            } catch (Exception e) {
                e.printStackTrace();
                logger.finest("DDLWarez: PostRequest-Error, try again!");
            }
        }
        return null;
    }

    public static File convert(File file) {
        try {
            JAntiCaptcha jas = new JAntiCaptcha("DUMMY", "DUMMY");
            jas.getJas().setColorType("RGB");
            GifDecoder d = new GifDecoder();
            d.read(file.getAbsolutePath());
            int n = d.getFrameCount();

            int width = (int) d.getFrameSize().getWidth();
            int height = (int) d.getFrameSize().getHeight();

            Captcha tmp;
            Captcha[] frames = new Captcha[n];
            for (int i = 0; i < n; i++) {
                BufferedImage frame = d.getFrame(i);
                tmp = new Captcha(width, height);
                tmp.setOwner(jas);
                PixelGrabber pg = new PixelGrabber(frame, 0, 0, width, height, false);
                try {
                    pg.grabPixels();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ColorModel cm = pg.getColorModel();
                tmp.setColorModel(cm);
                frames[i] = tmp;
                if (!(cm instanceof IndexColorModel)) {

                    tmp.setPixel((int[]) pg.getPixels());
                } else {
                    tmp.setPixel((byte[]) pg.getPixels());
                }
                // BasicWindow.showImage(tmp.getFullImage(), "img " + i);
            }
if(n<4)return null;
            frames[n - 4].crop(10, 20, 10, 10);
            frames[n - 3].crop(10, 20, 10, 10);
            frames[n - 2].crop(10, 20, 10, 10);
            frames[n - 1].crop(10, 20, 10, 10);

            Captcha merged = new Captcha(frames[n - 4].getWidth() * 4 + 3, frames[n - 4].getHeight());
            merged.setOwner(jas);
            merged.addAt(0, 0, frames[n - 4]);
            merged.addAt(frames[n - 4].getWidth() + 1, 0, frames[n - 3]);
            merged.addAt(frames[n - 4].getWidth() * 2 + 2, 0, frames[n - 2]);
            merged.addAt(frames[n - 4].getWidth() * 3 + 3, 0, frames[n - 1]);
            // merged.toBlackAndWhite(0.01);
            // BasicWindow.showImage(merged.getImage());
            ImageIO.write((BufferedImage) merged.getImage(), "png", new File(file.getAbsoluteFile() + ".png"));
            return new File(file.getAbsoluteFile() + ".png");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
