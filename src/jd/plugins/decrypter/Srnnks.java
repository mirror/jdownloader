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
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.EditDistance;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "serienjunkies.org" }, urls = { "http://[\\w\\.]*?serienjunkies\\.org.*(rc[_-]|rs[_-]|nl[_-]|u[tl][_-]|ff[_-]).*" }, flags = { 0 })
public class Srnnks extends PluginForDecrypt {
    private final static String[] passwords = { "serienjunkies.dl.am", "serienjunkies.org", "dokujunkies.org" };

    public Srnnks(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink dlink = super.createDownloadlink(link);
        dlink.addSourcePluginPasswords(passwords);
        return dlink;
    }

    private boolean limitsReached() {
        if (br.containsHTML("Du hast zu oft das Captcha falsch")) {
            UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, "Sie haben zu oft das Captcha falsch eingegeben sie müssen entweder warten oder einen Reconnect durchführen");
            return true;
        }
        if (br.containsHTML("Download-Limit")) {
            UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, "Das Downloadlimit wurde erreicht sie müssen entweder warten oder einen Reconnect durchführen");
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, final ProgressController progress) throws Exception {
        Browser.setRequestIntervalLimitGlobal("serienjunkies.org", 400);
        Browser.setRequestIntervalLimitGlobal("download.serienjunkies.org", 400);
        final Vector<DownloadLink> ret = new Vector<DownloadLink>();
        // progress.setStatusText("Lade Downloadseite");

        br.getPage(parameter.getCryptedUrl());
        if (limitsReached()) return new ArrayList<DownloadLink>(ret);

        if (br.containsHTML("<FRAME SRC")) {
            // progress.setStatusText("Lade Downloadseitenframe");
            br.getPage(br.getRegex("<FRAME SRC=\"(.*?)\"").getMatch(0));
        }
        if (limitsReached()) return new ArrayList<DownloadLink>(ret);
        progress.increase(30);

        // linkendung kommt auch im action der form vor
        String sublink = parameter.getCryptedUrl().substring(parameter.getCryptedUrl().indexOf("org/") + 3);
        for (int i = 0; i < 5; i++) {
            // suche wahrscheinlichste form
            // progress.setStatusText("Suche Captcha Form");
            Form form = null;
            {
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
            }
            if (form == null) throw new Exception("Serienjunkies Captcha Form konnte nicht gefunden werden!");
            progress.increase(30);

            String captchaLink = null;
            // das bild in der Form ist das captcha
            {
                captchaLink = new Regex(form.getHtmlCode(), "<IMG SRC=\"(.*?)\"").getMatch(0);
                if (captchaLink == null) throw new Exception("Serienjunkies Captcha konnte nicht gefunden werden!");
                if (!captchaLink.toLowerCase().startsWith("http://")) captchaLink = "http://" + br.getHost() + captchaLink;
            }
            File captcha = getLocalCaptchaFile(".png");
            // captcha laden
            {
                URLConnectionAdapter urlc = br.cloneBrowser().openGetConnection(captchaLink);
                Browser.download(captcha, urlc);
                String code;
                // wenn es ein Einzellink ist soll die Captchaerkennung benutzt
                // werden
                if (captchaLink.contains(".gif"))
                    code = getCaptchaCode("einzellinks.serienjunkies.org", captcha, parameter);
                else
                    code = getCaptchaCode(captcha, parameter);
                if (code == null || code.length() != 3) {
                    progress.setStatusText("Captcha code falsch");
                    progress.setStatus(30);
                    continue;
                }
                progress.increase(39);

                form.getInputFieldByType("text").setValue(code);
                // System.out.println(code);
                br.submitForm(form);
                if (limitsReached()) return new ArrayList<DownloadLink>(ret);
                if (br.getRedirectLocation() != null) {
                    ret.add(createDownloadlink(br.getRedirectLocation()));
                    progress.doFinalize();
                    return new ArrayList<DownloadLink>(ret);
                } else {
                    progress.setStatus(0);
                    Form[] forms = br.getForms();
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
                        progress.setStatus(30);
                        // progress.setStatusText("Captcha code falsch");
                        continue;
                    }
                    // durch paralleles laden der Links wird schneller
                    // entschlüsselt
                    // ist noch von der alten SerienJunkies Klasse
                    final Vector<Thread> threads = new Vector<Thread>();
                    final Browser[] br2 = new Browser[] { br.cloneBrowser(), br.cloneBrowser(), br.cloneBrowser(), br.cloneBrowser() };
                    progress.setStatus(0);

                    for (int d = 0; d < actions.size(); d++) {
                        // fortschritt pro Link
                        final int inc = 100 / actions.size();
                        try {
                            final String action = actions.get(d);
                            final int bd = d % 4;
                            Thread t = new Thread(new Runnable() {
                                public void run() {
                                    int errors = 0;
                                    for (int j = 0; j < 2000; j++) {
                                        try {
                                            Thread.sleep(300 * j);
                                        } catch (InterruptedException e) {
                                            return;
                                        }
                                        try {

                                            String tx = null;
                                            synchronized (br2[bd]) {
                                                try {
                                                    tx = br2[bd].getPage(action);
                                                } catch (Exception e) {
                                                    if (errors == 3) {
                                                        ret.clear();
                                                        for (Thread thread : threads) {
                                                            try {
                                                                thread.notify();
                                                                thread.interrupt();
                                                            } catch (Exception e2) {
                                                            }
                                                        }
                                                        threads.clear();
                                                        ret.clear();
                                                        return;
                                                    }
                                                    errors++;
                                                    continue;
                                                }
                                                if (tx != null) {
                                                    String link = new Regex(tx, Pattern.compile("SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                                                    if (link != null) {
                                                        try {
                                                            Browser brc = br2[bd].cloneBrowser();
                                                            brc.getPage(link);
                                                            String loc = brc.getRedirectLocation();
                                                            if (loc != null) {
                                                                ret.add(createDownloadlink(loc));
                                                                synchronized (progress) {
                                                                    progress.increase(inc);
                                                                }
                                                                synchronized (this) {
                                                                    notify();
                                                                }
                                                                return;
                                                            }
                                                        } catch (Exception e) {

                                                        }
                                                    }

                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.log(Level.SEVERE, "Exception occurred", e);
                                        }

                                    }

                                }
                            });
                            t.start();
                            threads.add(t);
                        } catch (Exception e) {
                        }

                    }
                    try {
                        for (Thread t : threads) {
                            while (t.isAlive()) {
                                synchronized (t) {
                                    try {
                                        t.wait();
                                    } catch (InterruptedException e) {
                                        logger.log(Level.SEVERE, "Exception occurred", e);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        progress.doFinalize();
                        return null;
                    }
                }
                progress.doFinalize();
            }
            // wenn keine links drinnen sind ist bestimmt was mit dem captcha
            // schief gegangen einfach nochmal versuchen
            if (ret.size() != 0) break;
        }

        return new ArrayList<DownloadLink>(ret);
    }

}
