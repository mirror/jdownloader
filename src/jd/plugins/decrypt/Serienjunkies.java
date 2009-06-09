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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.nutils.Formatter;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class Serienjunkies extends PluginForDecrypt {

    private static String lastHtmlCode = "";

    private static final int saveScat = 1;

    private static final int sCatGrabb = 2;

    private static final int sCatNewestDownload = 1;

    private static final int sCatNoThing = 0;

    private static int[] useScat = new int[] { 0, 0 };
    private static String[] mirrorManagement = new String[] { JDLocale.L("plugins.decrypt.serienjunkies.usePremiumLinks", "use premiumlinks if possible"), JDLocale.L("plugins.decrypt.serienjunkies.automaticMirrorManagment", "automatic mirror managment"), JDLocale.L("plugins.decrypt.serienjunkies.noMirrorManagment", "no mirror managment"), JDLocale.L("plugins.decrypt.serienjunkies.RsComOnly", "nur Rapidshare.com"), JDLocale.L("plugins.decrypt.serienjunkies.RsDeOnly", "nur Rapidshare.de"), JDLocale.L("plugins.decrypt.serienjunkies.NetloadOnly", "nur Netload.in"), JDLocale.L("plugins.decrypt.serienjunkies.UlOnly", "nur Uploaded.to"), JDLocale.L("plugins.decrypt.serienjunkies.FFOnly", "nur FileFactory.com") };

    private static String mirror = mirrorManagement[0];
    private String dynamicCaptcha = "(?s)<FORM ACTION=\".*?\" METHOD=\"post\".*?<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"(.*?)\">.*?<IMG SRC=\"([^\"]*)\"";
    private Pattern patternCaptcha = null;
    private String subdomain = "download.";
    private static int active = 0;
    private ProgressController progress;

    private boolean scatChecked = false;

    private ArrayList<String> passwords = new ArrayList<String>();
    private static boolean rc = false;

    private static URLConnectionAdapter openGetConnection(Browser capbr, String captchaAdress) throws IOException {
        while (rc) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                return null;
            }
        }
        return capbr.openGetConnection(captchaAdress);

    }

    private static String getPage(Browser br3, Object url) throws IOException {
        while (rc) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                return null;
            }
        }
        return br3.getPage(url.toString());

    }

    private static String postPage(Browser br3, String url, String string) throws IOException {
        while (rc) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                return null;
            }
        }
        return br3.postPage(url, string);

    }

    public Serienjunkies(PluginWrapper wrapper) {
        super(wrapper);
        passwords.add("serienjunkies.dl.am");
        passwords.add("serienjunkies.org");
        passwords.add("dokujunkies.org");
    }

    /**
     * Für Links die bei denen die Parts angezeigt werden
     */
    private ArrayList<String> containerLinks(String url, CryptedLink downloadLink) throws PluginException {
        final ArrayList<String> links = new ArrayList<String>();
        final Browser br3 = getBrowser();
        if (url.matches("http://[\\w\\.]*?.serienjunkies.org/..\\-.*")) {
            url = url.replaceFirst("serienjunkies.org", "serienjunkies.org/frame");
        }
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            String htmlcode = getPage(br3, url);
            File captchaFile = null;
            String capTxt = null;
            while (true) {
                htmlcode = htmlcode.replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", "");
                Matcher matcher = patternCaptcha.matcher(htmlcode);
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(captchaFile, capTxt, false);
                    }
                    String gif = new Regex(htmlcode, patternCaptcha).getMatch(1);

                    String captchaAdress = "http://" + subdomain + "serienjunkies.org" + gif;
                    Browser capbr = br3.cloneBrowser();
                    capbr.setFollowRedirects(true);
                    URLConnectionAdapter con = openGetConnection(capbr, captchaAdress);

                    if (con.getResponseCode() < 0) {
                        captchaAdress = "http://" + subdomain + "serienjunkies.org" + gif;
                        capbr.setFollowRedirects(true);
                        con.disconnect();
                        con = openGetConnection(capbr, captchaAdress);

                    }
                    if (con.getLongContentLength() < 1000) {
                        con.disconnect();
                        logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                        progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.decryptlimit", "SJ Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)"));
                        new Thread(new Runnable() {
                            public void run() {
                                for (int i = 0; i < 100; i++) {
                                    try {
                                        Thread.sleep(1200);
                                    } catch (InterruptedException e) {
                                        logger.log(Level.SEVERE, "Exception occurred", e);
                                    }
                                    progress.increase(1);
                                }
                            }
                        }).start();
                        rc = true;
                        if (!Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                            rc = false;
                            progress.setColor(Color.red);
                            progress.setStatus(0);
                            progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.downloadlimit", "Error: SerienJunkies Downloadlimit"));
                            for (int i = 0; i < 100; i++) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    logger.log(Level.SEVERE, "Exception occurred", e);
                                }
                                progress.increase(1);
                            }

                            return new ArrayList<String>();
                        }
                        rc = false;
                        htmlcode = getPage(br3, url);

                        continue;
                    }
                    captchaFile = getLocalCaptchaFile(".png");
                    try {
                        br3.downloadConnection(captchaFile, con);

                    } catch (Exception e) {

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        htmlcode = getPage(br3, url);

                        continue;
                    }

                    logger.info("captchafile: " + captchaFile);
                    active++;
                    try {
                        capTxt = getCaptchaCode(captchaFile, downloadLink);
                    } catch (Exception e) {
                        active--;
                        logger.log(Level.SEVERE, "Exception occurred", e);
                        progress.setColor(Color.red);
                        progress.setStatus(0);
                        progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.captcha", "Error: Captcha"));
                        return new ArrayList<String>();
                    }
                    active--;

                    htmlcode = postPage(br3, url, "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");

                } else {
                    break;
                }
            }
            if (br3.getRedirectLocation() != null) {
                links.add(br.getRedirectLocation());
            }
            Form[] forms = br3.getForms();
            final ArrayList<Thread> threads = new ArrayList<Thread>();
            final Browser[] br2 = new Browser[] { br3.cloneBrowser(), br3.cloneBrowser(), br3.cloneBrowser(), br3.cloneBrowser() };
            progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.getLinks", "get links"));
            progress.setStatus(0);

            ArrayList<String> actions = new ArrayList<String>();
            for (Form form : forms) {
                if (form.getAction().contains("download.serienjunkies.org") && !form.getAction().contains("firstload") && !form.getAction().equals("http://mirror.serienjunkies.org")) {
                    actions.add(form.getAction());
                }
            }
            final int inc = 100 / actions.size();
            for (int i = 0; i < actions.size(); i++) {
                try {
                    final String action = actions.get(i);
                    final int bd = i % 4;
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            String action2 = action;
                            Browser brd = br2[bd];
                            int errors = 0;
                            for (int j = 0; j < 2000; j++) {
                                try {
                                    Thread.sleep(300 * j);
                                } catch (InterruptedException e) {
                                    return;
                                }
                                try {

                                    String tx = null;
                                    synchronized (brd) {
                                        try {
                                            tx = getPage(brd, action2);
                                        } catch (Exception e) {
                                            if (errors == 3) {
                                                links.clear();
                                                for (Thread thread : threads) {
                                                    try {
                                                        thread.notify();
                                                        thread.interrupt();
                                                    } catch (Exception e2) {
                                                    }
                                                }
                                                threads.clear();
                                                links.clear();
                                                return;
                                            }
                                            errors++;
                                            continue;
                                        }
                                        if (tx != null) {
                                            String link = brd.getRegex(Pattern.compile("SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                                            if (link != null) {
                                                try {
                                                    getPage(brd, link);
                                                } catch (Exception e) {

                                                }
                                            }

                                            String loc = brd.getRedirectLocation();
                                            if (loc != null) {
                                                links.add(loc);
                                                synchronized (this) {
                                                    notify();
                                                }
                                                progress.increase(inc);
                                                return;
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
                progress.finalize();
                return null;
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occurred", e);
        }
        return links;
    }

    /**
     * Für Links die gleich auf den Hoster relocaten
     */
    private String einzelLinks(String url, CryptedLink downloadLink) throws PluginException {
        String links = "";
        Browser br3 = getBrowser();
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            if (!url.matches(".*sa[fv]e/f.*")) {
                url = url.replaceAll("safe/", "safe/f");
                url = url.replaceAll("save/", "save/f");
            }
            String htmlcode = getPage(br3, url);
            File captchaFile = null;
            String capTxt = null;
            while (true) {
                htmlcode = htmlcode.replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", "");
                Matcher matcher = patternCaptcha.matcher(htmlcode);
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(captchaFile, capTxt, false);
                    }
                    String captchaAdress = "http://serienjunkies.org" + matcher.group(2);
                    captchaFile = getLocalCaptchaFile(".png");
                    try {
                        Browser.download(captchaFile, captchaAdress);

                    } catch (Exception e) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        htmlcode = getPage(br3, url);

                        continue;
                    }
                    active++;
                    try {
                        capTxt = getCaptchaCode("einzellinks.serienjunkies.org", captchaFile, downloadLink);
                    } catch (Exception e) {
                        active--;
                        logger.log(Level.SEVERE, "Exception occurred", e);
                        break;
                    }
                    active--;

                    htmlcode = postPage(br3, url, "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                } else {
                    break;
                }
            }

            links = br3.getRedirectLocation();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occurred", e);
        }
        return links;
    }

    private ArrayList<DownloadLink> getDLinks(String parameter, CryptedLink cryptedLink) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Browser br3 = getBrowser();

        try {
            URL url = new URL(parameter);
            subdomain = new Regex(parameter, "http://(.*?)serienjunkies.org.*").getMatch(0);
            String modifiedURL = Encoding.htmlDecode(url.toString());
            modifiedURL = modifiedURL.replaceAll("safe/", "safe/f");
            modifiedURL = modifiedURL.replaceAll("save/", "save/f");
            modifiedURL = modifiedURL.substring(modifiedURL.lastIndexOf("/"));

            patternCaptcha = Pattern.compile(dynamicCaptcha);
            logger.fine("using patternCaptcha:" + patternCaptcha);
            br3.setFollowRedirects(true);
            getPage(br3, url);
            if (br3.getRedirectLocation() != null) {
                br3.setFollowRedirects(true);
                getPage(br3, url);
            }
            if (br3.containsHTML("Du hast zu oft das Captcha falsch")) {
                rc = true;
                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    rc = false;
                    logger.info("Reconnect successfull. try again");
                    br3.setFollowRedirects(true);
                    getPage(br3, url);
                    if (br3.getRedirectLocation() != null) {
                        br3.setFollowRedirects(true);
                        getPage(br3, url);
                    }
                } else {
                    rc = false;
                    logger.severe("Reconnect failed. abort.");
                    return decryptedLinks;
                }

            }
            if (br3.containsHTML("Download-Limit")) {
                logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.decryptlimit", "SJ Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)"));
                new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < 100; i++) {
                            try {
                                Thread.sleep(1200);
                            } catch (InterruptedException e) {
                                logger.log(Level.SEVERE, "Exception occurred", e);
                            }
                            progress.increase(1);
                        }
                    }
                }).start();
                rc = true;
                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    rc = false;
                    logger.info("Reconnect successfull. try again");
                    br3.setFollowRedirects(true);
                    getPage(br3, url);
                    if (br3.getRedirectLocation() != null) {
                        br3.setFollowRedirects(true);
                        getPage(br3, url);
                    }
                } else {
                    rc = false;
                    progress.setColor(Color.red);
                    progress.setStatus(0);
                    progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.downloadlimit", "Error: SerienJunkies Downloadlimit"));
                    for (int i = 0; i < 100; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            logger.log(Level.SEVERE, "Exception occurred", e);
                        }
                        progress.increase(1);
                    }
                    logger.severe("Reconnect failed. abort.");
                    return decryptedLinks;
                }
            }
            String furl = br3.getRegex(Pattern.compile("<FRAME SRC=\"(.*?)" + modifiedURL.replaceAll("[^0-1a-zA-Z]", ".") + "\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (furl != null) {
                url = new URL(furl + modifiedURL);
                logger.info("Frame found. frame url: " + furl + modifiedURL);
                br3.setFollowRedirects(true);
                getPage(br3, url.toString());
                parameter = furl + modifiedURL;

            }

            String[] links = br3.getRegex(Pattern.compile(" <a href=\"http://(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            ArrayList<String> helpvector = new ArrayList<String>();
            String helpstring = "";

            // Einzellink
            if (parameter.indexOf("/safe/") >= 0 || parameter.indexOf("/save/") >= 0) {
                logger.info("safe link");
                helpstring = einzelLinks(parameter, cryptedLink);

                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpstring)));
            } else if (parameter.indexOf(subdomain + "serienjunkies.org") >= 0 || parameter.indexOf("/sjsafe/") >= 0) {
                logger.info("sjsafe link");
                helpvector = containerLinks(parameter, cryptedLink);
                if (helpvector == null) return null;
                for (int j = 0; j < helpvector.size(); j++) {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpvector.get(j))));
                }
            } else {
                logger.info("else link");
                // Kategorien
                for (String link : links) {
                    if (link.indexOf("/safe/") >= 0) {
                        helpstring = einzelLinks(link, cryptedLink);
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpstring)));
                    } else if (link.indexOf("/sjsafe/") >= 0) {
                        helpvector = containerLinks(link, cryptedLink);
                        if (helpvector == null) return null;
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpvector.get(j))));
                        }
                    } else {
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred", e);
        }
        return decryptedLinks;
    }

    private Browser getBrowser() {
        Browser br = new Browser();

        return br;
    }

    private DownloadLink createdl(String parameter, String[] info) {
        int size = 100;
        String name = null, linkName = null, title = null;
        String[] mirrors = null;
        if (info != null) {
            name = Encoding.htmlDecode(info[1]);
            if (info[0] != null) size = Integer.parseInt(info[0]);
            title = Encoding.htmlDecode(info[3]);
            mirrors = getMirrors(parameter, info[2]);
        }
        if (title == null) title = "";
        try {
            linkName = ((title.length() > 10 ? title.substring(0, 10) : title) + "#" + name).replaceAll("\\.", " ").replaceAll("[^\\w \\#]", "").trim() + ".rar";
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred", e);
        }
        if (linkName == null || parameter.matches("http://serienjunkies.org/sa[fv]e/.*") || parameter.matches("http://download.serienjunkies.org/..\\-.*")) {
            size = 100;
            linkName = parameter.replaceFirst(".*/..[\\_\\-]", "").replaceFirst("\\.html?", "");
        }
        String hostname = getHostname(parameter);
        DownloadLink dlink = new DownloadLink(null, name, hostname, "http://SerienJunkiesError.org/" + linkName, false);
        dlink.setName(linkName);
        dlink.setProperty("link", parameter);
        dlink.setProperty("mirrors", mirrors);
        dlink.addSourcePluginPasswordList(passwords);
        if (name != null) {
            dlink.setDownloadSize(size * 1024 * 1024);
        }
        dlink.getLinkStatus().setStatusText("SerienJunkies");
        return dlink;
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        Browser.setRequestIntervalLimitGlobal("serienjunkies.org", 400);
        Browser.setRequestIntervalLimitGlobal("download.serienjunkies.org", 400);

        br = getBrowser();
        final ArrayList<DownloadLink> ar2 = decryptItMain(param);
        ArrayList<DownloadLink> ar = ar2;
        if (ar2.size() > 1) {

            ar = new GuiRunnable<ArrayList<DownloadLink>>() {

                // @Override
                public ArrayList<DownloadLink> runSave() {
                    SerienjunkiesSJTable sjt = new SerienjunkiesSJTable(SimpleGUI.CURRENTGUI, ar2);

                    return sjt.dls;
                }
            }.getReturnValue();

        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        SerienjunkiesThread[] threads = new SerienjunkiesThread[ar.size()];
        this.progress = progress;
        for (int i = 0; i < threads.length; i++) {
            DownloadLink downloadLink = ar.get(i);
            threads[i] = new SerienjunkiesThread(downloadLink, param);
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {

            if (ar.get(i) != null) {
                while (threads[i].isAlive()) {
                    synchronized (threads[i]) {
                        threads[i].wait();
                    }
                }
                if (threads[i].result != null) decryptedLinks.addAll(threads[i].result);
            }
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptItMain(CryptedLink param) throws Exception {
        String parameter = param.toString().trim();
        this.setBrowserExclusive();
        getPage(br, "http://serienjunkies.org/enter/");

        if (parameter.contains("/\\?cat=")) {
            if (getSerienJunkiesCat() == sCatNoThing) return new ArrayList<DownloadLink>();
        }

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (parameter.matches(".*\\?(cat|p)\\=[\\d]+.*")) {
            boolean isP = parameter.contains("/?p=");
            int catst = sCatGrabb;
            if (!isP) catst = getSerienJunkiesCat();
            scatChecked = false;
            int cat = Integer.parseInt(parameter.replaceFirst(".*\\?(cat|p)\\=", "").replaceFirst("[^\\d].*", ""));
            if (sCatNewestDownload == catst) {
                getPage(br, "http://serienjunkies.org/");

                Pattern pattern = Pattern.compile("<a href=\"http://serienjunkies.org/\\?cat\\=" + cat + "\">(.*?)</a><br", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(br.toString());
                ArrayList<String> names = new ArrayList<String>();
                while (matcher.find()) {
                    names.add(matcher.group(1).toLowerCase());
                }
                if (names.size() == 0) { return decryptedLinks; }
                getPage(br, parameter);
                lastHtmlCode = br.toString();
                for (String name : names) {
                    name += " ";
                    String[] bet = null;
                    while (bet == null) {
                        name = name.substring(0, name.length() - 1);
                        if (name.length() == 0) { return decryptedLinks; }
                        try {
                            bet = br.getRegex("<p><strong>(" + name + ".*?)</strong>(.*?)</p>").getMatches()[0];
                        } catch (Exception e) {
                        }
                    }

                    String[] links = HTMLParser.getHttpLinks(bet[1], br.getRequest().getUrl().toString());
                    String wh = getHostWishedHost();
                    if (wh != null) {
                        for (String element : links) {
                            if (getHostname(element).equals(wh)) {
                                String[] info = getLinkName(element, lastHtmlCode);
                                decryptedLinks.add(createdl(element, info));
                            }
                        }
                    } else if (mirror.equals(mirrorManagement[2])) {
                        for (String element : links) {
                            String[] info = getLinkName(element, lastHtmlCode);
                            decryptedLinks.add(createdl(element, info));
                        }
                    } else {

                        boolean got = false;
                        for (String element : links) {
                            String[] info = getLinkName(element, lastHtmlCode);
                            DownloadLink dl_link = createdl(element, info);

                            if (JDUtilities.getPluginForHost(getHostname(element)).getMaxSimultanDownloadNum(dl_link) > 1) {

                                decryptedLinks.add(dl_link);

                                got = true;
                                break;
                            }

                        }
                        if (!got) {
                            for (String element : links) {
                                String[] info = getLinkName(element, lastHtmlCode);
                                DownloadLink dl_link = createdl(element, info);
                                decryptedLinks.add(dl_link);
                                break;

                            }
                        }
                    }
                }

            } else if (catst == sCatGrabb) {
                String htmlcode = "";
                if (isP) {
                    getPage(br, parameter);
                    htmlcode = br.toString();
                } else {
                    getPage(br, "http://serienjunkies.org/?cat=" + cat);
                    htmlcode = br.toString();
                    try {
                        int pages = Integer.parseInt(br.getRegex("<p align=\"center\">  Pages \\(([\\d]+)\\):").getMatch(0));
                        for (int i = 2; i < pages + 1; i++) {
                            htmlcode += "\n" + getPage(br, "http://serienjunkies.org/?cat=" + cat + "&paged=" + i);
                        }
                    } catch (Exception e) {
                    }
                }
                HashMap<String, Integer> mirrors = new HashMap<String, Integer>();
                String wh = getHostWishedHost();
                String[] titles = htmlcode.replaceFirst("(?is).*?(<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*>)", "$1").split("<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*?>");
                for (String element : titles) {

                    String title = new Regex(element, "([^><]*?)</a>").getMatch(0);
                    String[] sp = element.split("(?is)<strong>Gr(ö|oe)(ß|ss)e:?</strong>:?[\\s]*");
                    int b = 1;
                    for (String element2 : sp) {

                        String size = "0";
                        try {
                            String[] dsize = new Regex(element2, "([\\d\\,]+)[\\s]*(..)?").getMatches()[0];

                            double si = Double.parseDouble(dsize[0].replaceAll("\\,", "."));
                            if (dsize.length > 1 && dsize[1].equalsIgnoreCase("gb")) {
                                si = si * 1024;
                            }
                            size = "" + si;
                            size = size.substring(0, size.indexOf("."));
                        } catch (Exception e) {
                        }
                        FilePackage fp = FilePackage.getInstance();
                        fp.setName(title + (b > 1 ? " " + b : ""));
                        b++;
                        fp.setPassword(JDUtilities.passwordArrayToString(passwords.toArray(new String[passwords.size()])));
                        String[][] links = new Regex(element2, "<p><strong>(.*?)</strong>(.*?)</p>").getMatches();
                        for (String[] element3 : links) {
                            String[] sp2 = element3[1].split("<strong>.*?</strong>");
                            // boolean rscom = (Boolean) getPluginConfig()
                            // .getProperty("USE_RAPIDSHARE_V2", true);

                            if (wh != null) {

                                outer: for (String bb : sp2) {
                                    String[] links2 = HTMLParser.getHttpLinks(bb, parameter);
                                    for (String element4 : links2) {
                                        if (canHandle(element4) && getHostname(element4).equals(wh)) {
                                            DownloadLink dl = createdl(element4, new String[] { size, element3[0], element3[1], title });
                                            dl.setFilePackage(fp);
                                            decryptedLinks.add(dl);
                                            break outer;
                                        }
                                    }
                                }
                            } else if (mirror.equals(mirrorManagement[2])) {
                                for (String bb : sp2) {
                                    String[] links2 = HTMLParser.getHttpLinks(bb, parameter);
                                    for (String element4 : links2) {
                                        if (canHandle(element4)) {
                                            DownloadLink dl = createdl(element4, new String[] { size, element3[0], element3[1], title });
                                            dl.setFilePackage(fp);

                                            decryptedLinks.add(dl);
                                        }

                                    }
                                }
                            } else {
                                boolean isOk = false;
                                boolean breakit = false;

                                if (mirror.equals(mirrorManagement[0])) {
                                    for (String bb : sp2) {
                                        String[] links2 = HTMLParser.getHttpLinks(bb, parameter);
                                        for (String element4 : links2) {
                                            if (canHandle(element4)) {
                                                DownloadLink dl = createdl(element4, new String[] { size, element3[0], element3[1], title });
                                                if (JDUtilities.getPluginForHost(getHostname(element4)).getMaxSimultanDownloadNum(dl) > 1) {
                                                    dl.setFilePackage(fp);
                                                    decryptedLinks.add(dl);
                                                    breakit = true;
                                                }
                                            }
                                        }
                                        if (breakit) {
                                            isOk = true;
                                            break;
                                        }
                                    }
                                }
                                if (!isOk) {
                                    String[] link = null;
                                    String lastHost = null;
                                    Integer lastint = Integer.MAX_VALUE;
                                    out: for (String bb : sp2) {
                                        String[] links2 = HTMLParser.getHttpLinks(bb, parameter);
                                        for (String element4 : links2) {
                                            if (canHandle(element4)) {
                                                String hostn = getHostname(element4);
                                                if (!mirrors.containsKey(hostn)) {
                                                    mirrors.put(hostn, 1);
                                                    link = null;
                                                    DownloadLink dl = createdl(element4, new String[] { size, element3[0], element3[1], title });
                                                    dl.setFilePackage(fp);
                                                    decryptedLinks.add(dl);
                                                    break out;
                                                } else {
                                                    Integer currentInt = mirrors.get(hostn);
                                                    if (currentInt < lastint) {
                                                        lastint = currentInt;
                                                        lastHost = hostn;
                                                        link = links2;
                                                    }
                                                    break;
                                                }

                                            }

                                        }
                                    }
                                    if (link != null) {
                                        mirrors.put(lastHost, (mirrors.get(lastHost) + 1));
                                        for (String element4 : link) {
                                            DownloadLink dl = createdl(element4, new String[] { size, element3[0], element3[1], title });
                                            dl.setFilePackage(fp);
                                            decryptedLinks.add(dl);
                                            break;
                                        }

                                    }
                                }

                            }
                        }
                    }
                }
            }
            return decryptedLinks;
        }

        String[] info = getLinkName(parameter, lastHtmlCode);
        decryptedLinks.add(createdl(parameter, info));
        return decryptedLinks;
    }

    private String getHostWishedHost() {
        if (mirror == null) {
            return null;
        } else if (mirror.equals(mirrorManagement[3])) {
            return "rapidshare.com";
        } else if (mirror.equals(mirrorManagement[4])) {
            return "rapidshare.de";
        } else if (mirror.equals(mirrorManagement[5])) {
            return "netload.in";
        } else if (mirror.equals(mirrorManagement[6])) {
            return "uploaded.to";
        } else if (mirror.equals(mirrorManagement[7])) {
            return "filefactory.com";
        } else {
            return null;
        }
    }

    private String getHostname(String link) {
        if (link.matches(".*rc[\\_\\-].*")) {
            return "rapidshare.com";
        } else if (link.matches(".*rs[\\_\\-].*")) {
            return "rapidshare.de";
        } else if (link.matches(".*nl[\\_\\-].*")) {
            return "netload.in";
        } else if (link.matches(".*u[tl][\\_\\-].*")) {
            return "uploaded.to";
        } else if (link.matches(".*ff[\\_\\-].*")) {
            return "filefactory.com";
        } else {
            return "rapidshare.com";
        }
    }

    private String[] getLinkName(String link, String htmlcode) {
        if (htmlcode == null) return null;
        String[] titles = htmlcode.replaceFirst("(?is).*?(<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*>)", "$1").split("<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*?>");
        for (String element : titles) {

            String title = new Regex(element, "([^><]*?)</a>").getMatch(0);
            String[] sp = element.split("(?is)<strong>Gr(ö|oe)(ß|ss)e:?</strong>:?[\\s]*");
            for (String element2 : sp) {
                String size = new Regex(element2, "(\\d+)").getMatch(0);
                String[][] links = new Regex(element2.replaceAll("<a href=\"http://vote.serienjunkies.org.*?</a>", ""), "<p><strong>(.*?)</strong>(.*?)</p>").getMatches();

                for (String[] element3 : links) {
                    try {
                        if (element3[1].toLowerCase().contains(Encoding.UTF8Decode(link).toLowerCase())) { return new String[] { size, element3[0], element3[1], title }; }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Exception occurred", e);
                    }

                }
            }
        }

        return null;
    }

    private String[] getMirrors(String link, String htmlcode) {
        String[] sp = htmlcode.split("<strong>.*?</strong>");
        ArrayList<String> ret = new ArrayList<String>();
        int c = -1;
        for (int i = 0; i < sp.length; i++) {
            if (sp[i].contains(link)) {

                String[] links = HTMLParser.getHttpLinks(sp[i], link);
                sp[i] = null;
                for (int j = 0; j < links.length; j++) {
                    if (links[j].equals(link)) {
                        c = j;
                        break;
                    }
                }
                break;
            }
        }
        if (c == -1) { return null; }
        for (String element : sp) {
            String mirror = null;
            try {
                mirror = HTMLParser.getHttpLinks(element, link)[c];
            } catch (Exception e) {
            }
            if (mirror != null && !mirror.matches("[\\s]*")) {
                ret.add(mirror);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    private int getSerienJunkiesCat() {
        sCatDialog();
        return useScat[0];
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    private void sCatDialog() {
        if (scatChecked || useScat[1] == saveScat) return;

        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                final JDialog dialog = new JDialog(SimpleGUI.CURRENTGUI);

                SerienjunkiesMeth[] meths = new SerienjunkiesMeth[3];
                meths[0] = new SerienjunkiesMeth(JDLocale.L("plugins.SerienJunkies.CatDialog.sCatNoThing", "Kategorie nicht hinzufügen"), sCatNoThing);
                meths[1] = new SerienjunkiesMeth(JDLocale.L("plugins.SerienJunkies.CatDialog.sCatGrabb", "Alle Serien in dieser Kategorie hinzufügen"), sCatGrabb);
                meths[2] = new SerienjunkiesMeth(JDLocale.L("plugins.SerienJunkies.CatDialog.sCatNewestDownload", "Den neusten Download dieser Kategorie hinzufügen"), sCatNewestDownload);

                final JComboBox methods = new JComboBox(meths);
                final JComboBox settings = new JComboBox(mirrorManagement);
                final JCheckBox checkScat = new JCheckBox("Einstellungen für diese Sitzung beibehalten?", true);
                JButton btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
                btnOK.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        useScat = new int[] { ((SerienjunkiesMeth) methods.getSelectedItem()).var, checkScat.isSelected() ? saveScat : 0 };
                        mirror = (String) settings.getSelectedItem();
                        dialog.dispose();
                    }

                });
                JButton btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
                btnCancel.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        useScat = new int[] { sCatNoThing, 0 };
                        dialog.dispose();
                    }

                });

                dialog.setLayout(new MigLayout("wrap 1"));
                dialog.setModal(true);
                dialog.setTitle(JDLocale.L("plugins.SerienJunkies.CatDialog.title", "SerienJunkies ::CAT::"));
                dialog.setAlwaysOnTop(true);
                dialog.addWindowListener(new WindowAdapter() {

                    public void windowClosing(WindowEvent e) {
                        useScat = new int[] { sCatNoThing, 0 };
                        dialog.dispose();
                    }

                });
                dialog.add(new JLabel(JDLocale.L("plugins.SerienJunkies.CatDialog.action", "Wählen sie eine Aktion aus:")));
                dialog.add(methods);
                dialog.add(new JLabel(JDLocale.L("plugins.SerienJunkies.CatDialog.mirror", "Wählen sie eine Mirrorverwalung:")));
                dialog.add(settings);
                dialog.add(checkScat);
                dialog.add(btnOK, "split 2, center");
                dialog.add(btnCancel);
                dialog.pack();
                dialog.setLocation(Screen.getCenterOfComponent(null, dialog));
                dialog.setVisible(true);

                return null;
            }
        }.waitForEDT();

    }

    public class SerienjunkiesThread extends Thread {
        private DownloadLink downloadLink;
        public ArrayList<DownloadLink> result = null;
        private CryptedLink cryptedLink;

        public SerienjunkiesThread(DownloadLink downloadLink, CryptedLink cryptedLink) {
            this.downloadLink = downloadLink;
            this.cryptedLink = cryptedLink;
        }

        // @Override
        public void run() {
            try {
                LinkStatus linkStatus = downloadLink.getLinkStatus();
                String link = (String) downloadLink.getProperty("link");
                String[] mirrors = (String[]) downloadLink.getProperty("mirrors");
                int c = 0;
                while (active > 2) {
                    if (c++ == 120) break;

                    // downloadLink.getLinkStatus().setStatusText(
                    // "waiting for decryption"
                    // );
                    Thread.sleep(100);

                }
                ArrayList<DownloadLink> dls = getDLinks(link, cryptedLink);
                if (dls != null && dls.size() < 1) {
                    linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                    if (linkStatus.getErrorMessage() == null) linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.pageerror", "SJ liefert keine Downloadlinks"));
                    logger.warning("SJ returned no Downloadlinks");
                } else {
                    ArrayList<DownloadLink> finaldls = null;
                    if (dls != null) {
                        finaldls = new ArrayList<DownloadLink>();
                        for (DownloadLink dls2 : dls) {
                            DistributeData distributeData = new DistributeData(dls2.getDownloadURL());
                            finaldls.addAll(distributeData.findLinks());
                        }
                        if (finaldls.size() > 0) {
                            try {
                                DownloadLink[] linksar = finaldls.toArray(new DownloadLink[finaldls.size()]);
                                progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.checkLinks", "check links"));
                                progress.setStatus(0);
                                int inc = 100 / linksar.length;
                                linksar[0].getPlugin().checkLinks(linksar);
                                for (DownloadLink downloadLink2 : linksar) {
                                    if (!downloadLink2.isAvailable()) {
                                        finaldls = null;
                                        break;
                                    }
                                    progress.increase(inc);
                                }
                            } catch (Exception e) {
                                finaldls = null;
                            }
                        }
                    }
                    if (finaldls == null) {
                        if (mirrors == null) {
                            Browser br2k = getBrowser();
                            getPage(br2k, "http://serienjunkies.org/?s=" + cryptedLink.getCryptedUrl().replaceFirst(".*/", "").replaceFirst("\\.html?$", ""));
                            String[] info = getLinkName(cryptedLink.getCryptedUrl(), br2k.toString());
                            logger.warning("use Mirror");
                            try {
                                mirrors = getMirrors(cryptedLink.getCryptedUrl(), info[2]);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }

                        }
                        if (mirrors != null) {
                            for (String element : mirrors) {
                                try {
                                    dls = getDLinks(element, cryptedLink);
                                    finaldls = new ArrayList<DownloadLink>();

                                    for (DownloadLink dls2 : dls) {
                                        DistributeData distributeData = new DistributeData(dls2.getDownloadURL());
                                        finaldls.addAll(distributeData.findLinks());
                                    }
                                    if (finaldls.size() > 0) {
                                        try {
                                            DownloadLink[] linksar = finaldls.toArray(new DownloadLink[finaldls.size()]);
                                            progress.setStatusText(JDLocale.L("plugins.decrypt.serienjunkies.progress.checkMirror", "check mirror"));
                                            progress.setStatus(0);
                                            int inc = 100 / linksar.length;
                                            linksar[0].getPlugin().checkLinks(linksar);
                                            for (DownloadLink downloadLink2 : linksar) {
                                                if (!downloadLink2.isAvailable()) {
                                                    finaldls = null;
                                                    break;
                                                }
                                                progress.increase(inc);
                                            }
                                        } catch (Exception e) {
                                            finaldls = null;
                                        }
                                    }
                                } catch (Exception e) {
                                    finaldls = null;
                                    logger.log(Level.SEVERE, "Exception occurred", e);
                                }
                                if (finaldls != null) break;
                            }
                        }
                    }
                    if (finaldls == null) {
                        linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                        linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.archiveincomplete", "Archiv nicht komplett"));

                    } else {
                        for (DownloadLink downloadLink2 : finaldls) {
                            downloadLink2.addSourcePluginPasswordList(passwords);
                        }
                    }
                    result = finaldls;
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception occurred", e);
            }

            if (result == null) {
                ArrayList<DownloadLink> ar = new ArrayList<DownloadLink>();
                ar.add(downloadLink);
                result = ar;
            }
            synchronized (this) {
                this.notify();
            }

        }
    }

    private class SerienjunkiesMeth {
        public String name;

        public int var;

        public SerienjunkiesMeth(String name, int var) {
            this.name = name;
            this.var = var;
        }

        // @Override
        public String toString() {
            return name;
        }
    }

}

class SerienjunkiesSJTable extends JDialog {
    private static final long serialVersionUID = 4525944250937805028L;

    public ArrayList<DownloadLink> dls;

    public SerienjunkiesSJTable(JFrame owner, ArrayList<DownloadLink> dLinks) {
        super(owner);

        dls = dLinks;

        JLabel m_title = new JLabel(JDLocale.L("plugin.serienjunkies.manager.dllinks", "Unerwünschte Links einfach löschen"), new ImageIcon(JDImage.getImage(JDTheme.V("gui.images.config.addons"))), SwingConstants.LEFT);

        final JTable m_table = new JTable(new SerienjunkiesTM(dls));

        TableColumn column = null;

        for (int c = 0; c < m_table.getColumnCount(); c++) {
            column = m_table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(200);
                break;
            case 1:
                column.setPreferredWidth(160);
                break;
            case 2:
                column.setPreferredWidth(100);
                break;
            }
        }

        JButton del = new JButton(JDLocale.L("gui.component.textarea.context.delete", "Löschen"));
        addWindowListener(new WindowAdapter() {

            public void windowClosed(WindowEvent e) {
                dispose();
            }

            public void windowClosing(WindowEvent e) {
                dls = new ArrayList<DownloadLink>();
            }

        });
        del.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int[] rows = m_table.getSelectedRows();
                ArrayList<DownloadLink> delDls = new ArrayList<DownloadLink>();
                for (int j : rows) {
                    delDls.add(dls.get(j));
                }
                dls.removeAll(delDls);
                m_table.tableChanged(new TableModelEvent(m_table.getModel()));
            }

        });

        final JButton insertButton = new JButton(JDLocale.L("gui.component.textarea.context.paste", "Einfügen"));
        insertButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }

        });

        setTitle(JDLocale.L("plugin.serienjunkies.manager.title", "SerienJunkies Linkverwaltung"));
        setModal(true);
        setLayout(new MigLayout("ins 5, wrap 1", "[center]"));
        add(m_title, "left");
        add(new JScrollPane(m_table), "growx, spanx");
        add(del, "split 2");
        add(insertButton);
        pack();
        setLocation(Screen.getCenterOfComponent(null, this));
        setVisible(true);
    }

    private class SerienjunkiesTM extends AbstractTableModel {

        private static final long serialVersionUID = 5068062216039834333L;

        private String m_columns[] = { JDLocale.L("gui.packageinfo.name", "Name"), JDLocale.L("gui.treetable.header_3.hoster", "Anbieter"), JDLocale.L("gui.linkgrabber.packagetab.table.column.size", "Größe") };

        private ArrayList<DownloadLink> dls;

        public SerienjunkiesTM(ArrayList<DownloadLink> dls) {
            this.dls = dls;
        }

        public int getRowCount() {
            return dls == null ? 0 : dls.size();
        }

        public int getColumnCount() {
            return m_columns.length;
        }

        // @Override
        public String getColumnName(int column) {
            return m_columns[column];
        }

        // @Override
        public boolean isCellEditable(int nRow, int nCol) {
            return false;
        }

        public Object getValueAt(int nRow, int nCol) {
            switch (nCol) {
            case 0:
                return dls.get(nRow).getName();
            case 1:
                return dls.get(nRow).getHost();
            case 2:
                return Formatter.formatReadable(dls.get(nRow).getDownloadSize());
            }
            return "";
        }

    }

}
