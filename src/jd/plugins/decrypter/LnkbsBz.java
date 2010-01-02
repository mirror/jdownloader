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

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.pluginUtils.Recaptcha;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkbase.biz" }, urls = { "http://[\\w\\.]*?linkbase\\.biz/{1,}\\?v=[\\w]+" }, flags = { 0 })
public class LnkbsBz extends PluginForDecrypt {

    public static Integer Worker_Delay = 250;

    static class LnkbsBz_Linkgrabber extends Thread {
        public final static int THREADFAIL = 1;
        public final static int THREADPASS = 0;
        int _status;
        private String downloadlink;
        private boolean gotjob;

        private int Worker_ID;
        private Browser br;
        private String link;

        public LnkbsBz_Linkgrabber(int id, Browser br) {
            this.downloadlink = null;
            this.link = null;
            this.gotjob = false;
            this._status = THREADFAIL;
            this.Worker_ID = id;
            this.br = br;
            this.br.setFollowRedirects(true);
        }

        public String getlink() {
            return this.downloadlink;
        }

        // @Override
        public void run() {
            if (this.gotjob == true) {
                logger.finest("LnkbsBz_Linkgrabber: id=" + new Integer(this.Worker_ID) + " started!");

                for (int retry = 1; retry <= 10; retry++) {
                    try {
                        String page = decodePage(this.br.getPage("http://linkbase.biz/?go=" + this.link));
                        String link = new Regex(page, "<iframe src='(.*?)'").getMatch(0);
                        if (link == null) {
                            link = br.getRegex("<iframe src='(.*?)'").getMatch(0);
                        }
                        this.downloadlink = link;
                        break;
                    } catch (Exception e) {
                        logger.finest("LnkbsBz_Linkgrabber: id=" + new Integer(this.Worker_ID) + " GetRequest-Error, try again!");
                        synchronized (LnkbsBz.Worker_Delay) {
                            LnkbsBz.Worker_Delay = 1000;
                        }
                    }
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
                }
            }
            logger.finest("LnkbsBz_Linkgrabber: id=" + this.Worker_ID + " finished!");
            this._status = THREADPASS;
        }

        public void setjob(String link) {
            this.link = link;
            this.gotjob = true;
        }

        public int status() {
            return this._status;
        }
    }

    public LnkbsBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final Object LOCK = new Object();

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        synchronized (LOCK) {
            for (int retry = 1; retry <= 20; retry++) {
                try {
                    br.clearCookies(getHost());
                    br.getPage(parameter);
                    if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
                    if (br.getRegex("Du hast.*?Du musst noch").matches()) {
                        param.getProgressController().setRange(30);
                        param.getProgressController().setStatusText("Wrong captcha, please wait 30 secs!");
                        for (int i = 0; i < 30; i++) {
                            Thread.sleep(1000);
                            param.getProgressController().increase(1);
                        }
                        param.getProgressController().setStatusText(null);
                        param.getProgressController().setStatus(0);
                        continue;
                    }
                    String captchaurl = br.getRegex("<img src='(.*?captcha.*?)'").getMatch(0);
                    if (captchaurl != null) {
                        File captchaFile = this.getLocalCaptchaFile();
                        try {
                            Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://linkbase.biz/" + captchaurl));
                        } catch (Exception e) {
                            logger.severe("Captcha Download fehlgeschlagen: " + "http://linkbase.biz/" + captchaurl);
                            throw new DecrypterException(DecrypterException.CAPTCHA);
                        }

                        String captchaCode = getCaptchaCode(captchaFile, param);
                        if (captchaCode == null || captchaCode.contains("-")) continue;
                        Form form = br.getForm(0);
                        form.put("captcha", captchaCode);
                        br.submitForm(form);
                        if (br.containsHTML("Das war leider Falsch")) {
                            continue;
                        }
                    }
                    if (br.containsHTML("api.recaptcha.net")) {
                        Recaptcha rc = new Recaptcha(br);
                        rc.parse();
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(null, cf, param);
                        rc.setCode(c);
                        if (br.containsHTML("Das war leider Falsch")) continue;
                    }
                    String links[] = br.getRegex("window.open\\('\\?go=(.*?)','.*?'\\)").getColumn(0);
                    progress.setRange(links.length);
                    LnkbsBz_Linkgrabber LnkbsBz_Linkgrabbers[] = new LnkbsBz_Linkgrabber[links.length];
                    for (int i = 0; i < links.length; ++i) {
                        synchronized (Worker_Delay) {
                            Thread.sleep(Worker_Delay);
                        }
                        LnkbsBz_Linkgrabbers[i] = new LnkbsBz_Linkgrabber(i, br.cloneBrowser());
                        LnkbsBz_Linkgrabbers[i].setjob(links[i]);
                        LnkbsBz_Linkgrabbers[i].start();
                    }
                    for (int i = 0; i < links.length; ++i) {
                        try {
                            LnkbsBz_Linkgrabbers[i].join();
                            if (LnkbsBz_Linkgrabbers[i].status() == LnkbsBz_Linkgrabber.THREADPASS) {
                                decryptedLinks.add(createDownloadlink(LnkbsBz_Linkgrabbers[i].getlink()));
                            }
                            progress.increase(1);
                        } catch (InterruptedException e) {
                            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                        }
                    }
                    progress.doFinalize();
                    return decryptedLinks;
                } catch (DecrypterException e2) {
                    logger.severe("LinkBaseBiz: Captcha Error");
                    throw e2;
                } catch (Exception e) {
                    logger.finest("LnkbsBz: GetRequest-Error, try again!");
                }
            }
        }
        return null;
    }

    private static String decodePage(String page) {
        if (page == null) return null;
        StringBuffer sb = new StringBuffer();
        String pattern = "(document\\.write\\(\".*?\"\\);)";
        Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(page);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                String content = r.group(1).replaceAll("^document\\.write\\(\"", "").replaceAll("\"\\);$", "");
                r.appendReplacement(sb, content);
            }
        }
        r.appendTail(sb);
        return sb.toString();
    }

}
