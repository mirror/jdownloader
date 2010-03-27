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
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.jobber.JDRunnable;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.EditDistance;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "serienjunkies.org" }, urls = { "http://[\\w\\.]*?serienjunkies\\.org/.*?/(rc[_-]|rs[_-]|nl[_-]|u[tl][_-]|ff[_-]).*" }, flags = { 0 })
public class Srnnks extends PluginForDecrypt {
    private final static String[] passwords = { "serienjunkies.dl.am", "serienjunkies.org", "dokujunkies.org" };
    private static long LATEST_BLOCK_DETECT = 0;
    private static long LATEST_RECONNECT = 0;

    private static Object GLOBAL_LOCK = new Object();
    // seems like sj does block ips if requestlimit is not ok
    private static final long FW_WAIT = 300;

    public Srnnks(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected String getInitials() {
        return "SJ";
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink dlink = super.createDownloadlink(link);
        dlink.addSourcePluginPasswords(passwords);
        return dlink;
    }

    private synchronized static boolean limitsReached(Browser br) throws IOException {
        int ret = -100;
        if (br == null) {
            ret = UserIO.RETURN_OK;
        } else {
            if (br.containsHTML("Error 503")) {
                UserIO.getInstance().requestMessageDialog("Serienjunkies ist überlastet. Bitte versuch es später nocheinmal!");
                return true;
            }

            if (br.containsHTML("Du hast zu oft das Captcha falsch")) {

                if (System.currentTimeMillis() - LATEST_BLOCK_DETECT < 60000) return true;
                if (System.currentTimeMillis() - LATEST_RECONNECT < 15000) {
                    // redo the request
                    br.loadConnection(br.openRequestConnection(br.getRequest().cloneRequest()));
                    return false;
                }
                ret = UserIO.getInstance().requestConfirmDialog(0, "Captchalimit", "Sie haben zu oft das Captcha falsch eingegeben sie müssen entweder warten oder einen Reconnect durchführen", null, "Reconnect", "Decrypten abbrechen");

            }
            if (br.containsHTML("Download-Limit")) {
                if (System.currentTimeMillis() - LATEST_BLOCK_DETECT < 60000) return true;
                if (System.currentTimeMillis() - LATEST_RECONNECT < 15000) {
                    // redo the request
                    br.loadConnection(br.openRequestConnection(br.getRequest().cloneRequest()));
                    return false;
                }
                ret = UserIO.getInstance().requestConfirmDialog(0, "Downloadlimit", "Das Downloadlimit wurde erreicht sie müssen entweder warten oder einen Reconnect durchführen", null, "Reconnect", "Decrypten abbrechen");

            }
        }
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                if (Reconnecter.waitForNewIP(15000, false)) {

                    // redo the request
                    br.loadConnection(br.openRequestConnection(br.getRequest().cloneRequest()));
                    LATEST_RECONNECT = System.currentTimeMillis();
                    return false;
                }
            } else {
                LATEST_BLOCK_DETECT = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, final ProgressController progress) throws Exception {
        try {
            // Browser.setRequestIntervalLimitGlobal("serienjunkies.org", 400);
            // Browser.setRequestIntervalLimitGlobal("download.serienjunkies.org",
            // 400);
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            // progress.setStatusText("Lade Downloadseite");
            progress.setRange(100);
            synchronized (GLOBAL_LOCK) {
                Thread.sleep(FW_WAIT);
                br.getPage(parameter.getCryptedUrl());
            }
            if (limitsReached(br)) return new ArrayList<DownloadLink>(ret);

            if (br.containsHTML("<FRAME SRC")) {
                // progress.setStatusText("Lade Downloadseitenframe");
                synchronized (GLOBAL_LOCK) {
                    Thread.sleep(FW_WAIT);
                    br.getPage(br.getRegex("<FRAME SRC=\"(.*?)\"").getMatch(0));
                }
            }
            if (limitsReached(br)) return new ArrayList<DownloadLink>(ret);
            progress.increase(5);

            // linkendung kommt auch im action der form vor
            String sublink = parameter.getCryptedUrl().substring(parameter.getCryptedUrl().indexOf("org/") + 3);

            // try captcha max 5 times
            for (int i = 0; i < 5; i++) {

                progress.setRange(100);
                progress.setStatus(5);

                // suche wahrscheinlichste form
                // progress.setStatusText("Suche Captcha Form");
                Form form = null;

                Form[] forms = br.getForms();
                int bestdist = Integer.MAX_VALUE;
                for (Form form1 : forms) {
                    int dist = EditDistance.damerauLevenshteinDistance(sublink, form1.getAction());
                    if (dist < bestdist) {
                        form = form1;
                        bestdist = dist;
                    }
                }
                if (bestdist > 100) form = null;

                if (form == null) throw new Exception("Serienjunkies Captcha Form konnte nicht gefunden werden!");
                progress.increase(5);

                // das bild in der Form ist das captcha
                String captchaLink = new Regex(form.getHtmlCode(), "<IMG SRC=\"(.*?)\"").getMatch(0);
                if (captchaLink == null) throw new Exception("Serienjunkies Captcha konnte nicht gefunden werden!");
                if (!captchaLink.toLowerCase().startsWith("http://")) captchaLink = "http://" + br.getHost() + captchaLink;

                File captcha = getLocalCaptchaFile(".png");
                // captcha laden
                synchronized (GLOBAL_LOCK) {
                    Thread.sleep(FW_WAIT);
                    URLConnectionAdapter urlc = br.cloneBrowser().openGetConnection(captchaLink);
                    Browser.download(captcha, urlc);
                }
                String code;
                // wenn es ein Einzellink ist soll die Captchaerkennung benutzt
                // werden
                if (captchaLink.contains(".gif")) {
                    code = getCaptchaCode("einzellinks.serienjunkies.org", captcha, parameter);
                } else {
                    code = getCaptchaCode(captcha, parameter);
                }
                if (code == null || code.length() != 3) {
                    progress.setStatusText("Captcha code falsch");
                    progress.setStatus(30);
                    continue;
                }
                progress.increase(5);

                form.getInputFieldByType("text").setValue(code);
                // System.out.println(code);
                synchronized (GLOBAL_LOCK) {
                    Thread.sleep(FW_WAIT);
                    br.submitForm(form);
                }
                if (limitsReached(br)) return new ArrayList<DownloadLink>(ret);
                if (br.getRedirectLocation() != null) {
                    ret.add(createDownloadlink(br.getRedirectLocation()));
                    progress.doFinalize();
                    return new ArrayList<DownloadLink>(ret);
                } else {
                    progress.setStatus(0);
                    forms = br.getForms();
                    // suche die downloadlinks
                    ArrayList<String> actions = new ArrayList<String>();
                    for (Form frm : forms) {
                        if (frm.getAction().contains("download.serienjunkies.org") && !frm.getAction().contains("firstload") && !frm.getAction().equals("http://mirror.serienjunkies.org")) {
                            actions.add(frm.getAction());
                        }
                    }
                    // es wurden keine Links gefunden also wurde das Captcha
                    // falsch eingegeben
                    if (actions.size() == 0) {
                        progress.setStatus(10);
                        // progress.setStatusText("Captcha code falsch");
                        continue;
                    }
                    // we need the 300 ms gap between two requests..
                    progress.setRange(10 + actions.size());
                    progress.setStatus(10);
                    synchronized (GLOBAL_LOCK) {
                        for (int d = 0; d < actions.size(); d++) {
                            this.new DecryptRunnable(actions.get(d), br.cloneBrowser(), ret).go();
                            progress.increase(1);
                        }

                    }
                    progress.doFinalize();

                    // wenn keine links drinnen sind ist bestimmt was mit dem
                    // captcha
                    // schief gegangen einfach nochmal versuchen
                    if (ret.size() != 0) { return ret; }
                }

            }
            return new ArrayList<DownloadLink>(ret);
        } catch (Exception e) {
            JDLogger.exception(e);
            throw e;
        }
    }

    class DecryptRunnable implements JDRunnable {

        private String action;
        private Browser br;
        private ArrayList<DownloadLink> results;

        public DecryptRunnable(String action, Browser br, ArrayList<DownloadLink> results) {
            this.action = action;
            this.br = br;
            this.results = results;
        }

        public void go() throws Exception {

            // sj heuristic detection.

            // this makes the jobber useless... but we have to use the 300 ms to
            // work around sj's firewall

            Thread.sleep(FW_WAIT);
            br.getPage(action);

            String link = br.getRegex("SRC=\"(http://download\\.serienjunkies\\.org.*?)\"").getMatch(0);

            if (link != null) {
                Thread.sleep(FW_WAIT);
                br.getPage(link);
                String loc = br.getRedirectLocation();
                if (loc != null) {
                    results.add(createDownloadlink(loc));
                    return;
                } else {
                    throw new Exception("no Redirect found");
                }
            } else {

                throw new Exception("no Frame found");

            }
        }

    }
}
