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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import jd.PluginWrapper;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class Serienjunkies extends PluginForDecrypt {
    // private static final String host = "Serienjunkies.org";

    public static String lastHtmlCode = "";

    private static final int saveScat = 1;

    private static final int sCatGrabb = 2;

    private static final int sCatNewestDownload = 1;

    private static final int sCatNoThing = 0;

    private static int[] useScat = new int[] { 0, 0 };
    private String dynamicCaptcha = "(?s)<FORM ACTION=\".*?\" METHOD=\"post\".*?<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"(.*?)\">.*?<IMG SRC=\"([^\"]*)\"";
    private Pattern patternCaptcha = null;
    private String subdomain = "download.";
    private static int active = 0;
    private ProgressController progress;
    private JCheckBox checkScat;
    private JComboBox methods;
    private String[] mirrorManagement = new String[] { JDLocale.L("plugins.decrypt.serienjunkies.usePremiumLinks", "use premiumlinks if possible"), JDLocale.L("plugins.decrypt.serienjunkies.automaticMirrorManagment", "automatic mirror managment"), JDLocale.L("plugins.decrypt.serienjunkies.noMirrorManagment", "no mirror managment") };
    private boolean next = false;

    private boolean scatChecked = false;

    private static Vector<String> passwords = new Vector<String>();

    public Serienjunkies(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        passwords.add("serienjunkies.dl.am");
        passwords.add("serienjunkies.org");
    }

    public boolean[] checkLinks(DownloadLink[] urls) {
        boolean[] ret = new boolean[urls.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = false;
        }
        return ret;
    }

    public synchronized boolean canHandle(String data) {
        boolean cat = false;
        if (data == null) return false;
        data = data.replaceAll("http://vote.serienjunkies.org/?", "");
        if (data.contains("serienjunkies.org") && (data.contains("/?cat="))) {
            cat = getSerienJunkiesCat() != sCatNoThing;
        }
        boolean rscom = (Boolean) getPluginConfig().getProperty("USE_RAPIDSHARE_V2", true);
        boolean rsde = (Boolean) getPluginConfig().getProperty("USE_RAPIDSHAREDE_V2", true);
        boolean net = (Boolean) getPluginConfig().getProperty("USE_NETLOAD_V2", true);
        boolean uploaded = (Boolean) getPluginConfig().getProperty("USE_UPLOADED_V2", true);
        boolean filefactory = (Boolean) getPluginConfig().getProperty("USE_FILEFACTORY_V2", true);
        next = false;
        String hosterStr = "";
        if (rscom || rsde || net || uploaded || filefactory || cat) {
            hosterStr += "(";
            if (rscom) {
                hosterStr += isNext() + "rc[\\_\\-]";
            }
            if (rsde) {
                hosterStr += isNext() + "rs[\\_\\-]";
            }
            if (net) {
                hosterStr += isNext() + "nl[\\_\\-]";
            }
            if (uploaded) {
                hosterStr += isNext() + "ut[\\_\\-]";
            }
            if (filefactory) {
                hosterStr += isNext() + "ff[\\_\\-]";
            }
            hosterStr += isNext() + "p\\=[\\d]+";
            if (cat) {
                hosterStr += isNext() + "cat\\=[\\d]+";

            }

            hosterStr += ")";
        } else {
            hosterStr += "not";
        }
        Matcher matcher = Pattern.compile("http://[\\w\\.]{0,10}serienjunkies\\.org.*" + hosterStr + ".*", Pattern.CASE_INSENSITIVE).matcher(data);
        if (matcher.find()) {
            return true;
        } else {

            String[] links = new Regex(data, "http://[\\w\\.]{3,10}\\.serienjunkies.org/.*", Pattern.CASE_INSENSITIVE).getColumn(-1);
            Pattern pat = Pattern.compile("http://[\\w\\.]{3,10}\\.serienjunkies.org/.*(rc[\\_\\-]|rs[\\_\\-]|nl[\\_\\-]|ut[\\_\\-]|su[\\_\\-]|ff[\\_\\-]|cat\\=[\\d]+|p\\=[\\d]+).*", Pattern.CASE_INSENSITIVE);
            for (String element : links) {
                Matcher m = pat.matcher(element);

                if (!m.matches()) { return true; }
            }
        }
        return false;
    }

    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url, CryptedLink downloadLink) throws PluginException {
        final Vector<String> links = new Vector<String>();
        final Browser br3 = new Browser();
        if (url.matches("http://[\\w\\.]*?.serienjunkies.org/..\\-.*")) {
            url = url.replaceFirst("serienjunkies.org", "serienjunkies.org/frame");
        }
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            String htmlcode = br3.getPage(url);
            File captchaFile = null;
            String capTxt = null;
            while (true) {
                htmlcode = htmlcode.replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", "");
                Matcher matcher = patternCaptcha.matcher(htmlcode);
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }
                    String[][] gifs = new Regex(htmlcode, patternCaptcha).getMatches();

                    String captchaAdress = "http://" + subdomain + "serienjunkies.org" + gifs[0][1];
                    Browser capbr = br3.cloneBrowser();
                    capbr.setFollowRedirects(true);
                    URLConnectionAdapter con = capbr.openGetConnection(captchaAdress);

                    if (con.getResponseCode() < 0) {
                        captchaAdress = "http://" + subdomain + "serienjunkies.org" + gifs[0][1];
                        capbr.setFollowRedirects(true);
                        con = capbr.openGetConnection(captchaAdress);

                    }
                    if (con.getLongContentLength() < 1000) {
                        logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                        progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.decryptlimit", "SJ Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)"));
                        new Thread(new Runnable() {
                            public void run() {
                                for (int i = 0; i < 100; i++) {
                                    try {
                                        Thread.sleep(1200);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    progress.increase(1);
                                }
                            }
                        }).start();
                        if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                            progress.setColor(Color.red);
                            progress.setStatus(0);
                            progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.downloadlimit", "Error: SerienJunkies Downloadlimit"));
                            for (int i = 0; i < 100; i++) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                progress.increase(1);
                            }

                            return new Vector<String>();
                        }

                        htmlcode = br3.getPage(url);

                        continue;
                    }
                    captchaFile = Plugin.getLocalCaptchaFile(this, ".gif");
                    try {
                        br3.downloadConnection(captchaFile, con);

                    } catch (Exception e) {

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        htmlcode = br3.getPage(url);

                        continue;
                    }

                    logger.info("captchafile: " + captchaFile);
                    active++;
                    try {
                        capTxt = getCaptchaCode(captchaFile, this, downloadLink);
                    } catch (Exception e) {
                        active--;
                        e.printStackTrace();
                        progress.setColor(Color.red);
                        progress.setStatus(0);
                        progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.captcha", "Error: Captcha"));
                        return new Vector<String>();
                    }
                    active--;

                    htmlcode = br3.postPage(url, "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");

                } else {
                    captchaMethod(captchaFile, capTxt);
                    break;
                }
            }
            if (br3.getRedirectLocation() != null) {
                links.add(br.getRedirectLocation());
            }
            Form[] forms = br3.getForms();
            final Vector<Thread> threads = new Vector<Thread>();
            final Browser[] br2 = new Browser[] { br3.cloneBrowser(), br3.cloneBrowser(), br3.cloneBrowser(), br3.cloneBrowser() };
            progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.getLinks", "get links"));
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
                                            tx = brd.getPage(action2);
                                        } catch (Exception e) {
                                            if (errors == 3) {
                                                links.removeAllElements();
                                                for (Thread thread : threads) {
                                                    try {
                                                        thread.notify();
                                                        thread.interrupt();

                                                    } catch (Exception e2) {
                                                        // TODO: handle
                                                        // exception
                                                    }

                                                }
                                                threads.removeAllElements();
                                                links.removeAllElements();
                                                return;
                                            }
                                            errors++;
                                            continue;
                                        }
                                        if (tx != null) {
                                            String link = new Regex(brd.toString(), Pattern.compile("SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                                            if (link != null) {
                                                try {
                                                    brd.getPage(link);
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
                                    e.printStackTrace();
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
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                progress.finalize();
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    private void captchaMethod(File captchaFile, String capTxt) {
        if (captchaFile != null && capTxt != null) {
            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);

            if (useUserinputIfCaptchaUnknown() && getCaptchaDetectionID() == Plugin.CAPTCHA_USER_INPUT && getLastCaptcha() != null && getLastCaptcha().getLetterComperators() != null) {
                LetterComperator[] lcs = getLastCaptcha().getLetterComperators();
                getLastCaptcha().setCorrectcaptchaCode(capTxt.trim());

                if (lcs.length == capTxt.trim().length()) {
                    for (int i = 0; i < capTxt.length(); i++) {
                        if (lcs[i] != null && lcs[i].getDecodedValue() != null && capTxt.substring(i, i + 1).equalsIgnoreCase(lcs[i].getDecodedValue()) && lcs[i].getValityPercent() < 30.0) { //
                            logger.severe("OK letter: " + i + ": JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER: " + capTxt.substring(i, i + 1));
                        } else {
                            logger.severe("Unknown letter: // " + i + ":  JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER:  " + capTxt.substring(i, i + 1));
                            final String character = capTxt.substring(i, i + 1);
                            logger.info("SEND");
                            Letter letter = lcs[i].getA();
                            String captchaHash = UTILITIES.getLocalHash(captchaFile);
                            letter.setSourcehash(captchaHash);
                            letter.setOwner(getLastCaptcha().owner);
                            letter.setDecodedValue(character);
                            getLastCaptcha().owner.letterDB.add(letter);
                            getLastCaptcha().owner.saveMTHFile();
                        }
                    }

                } else {
                    logger.info("LCS not length comp");
                }
            }
        }
    }

    // Für Links die gleich auf den Hoster relocaten
    private String EinzelLinks(String url, CryptedLink downloadLink) throws PluginException {
        String links = "";
        Browser br3 = new Browser();
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            if (!url.matches(".*sa[fv]e/f.*")) {
                url = url.replaceAll("safe/", "safe/f");
                url = url.replaceAll("save/", "save/f");
            }
            String htmlcode = br3.getPage(url);
            File captchaFile = null;
            String capTxt = null;
            while (true) {
                htmlcode = htmlcode.replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", "");
                Matcher matcher = patternCaptcha.matcher(htmlcode);
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }
                    String captchaAdress = "http://serienjunkies.org" + matcher.group(2);
                    captchaFile = Plugin.getLocalCaptchaFile(this, ".gif");
                    try {
                        Browser.download(captchaFile, captchaAdress);

                    } catch (Exception e) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        htmlcode = br3.getPage(url);

                        continue;
                    }
                    active++;
                    try {
                        capTxt = Plugin.getCaptchaCode(this, "einzellinks.serienjunkies.org", captchaFile, false, downloadLink);
                    } catch (Exception e) {
                        active--;
                        e.printStackTrace();
                        break;
                    }
                    active--;

                    htmlcode = br3.postPage(url, "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                } else {
                    captchaMethod(captchaFile, capTxt);
                    break;
                }
            }

            links = br3.getRedirectLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    public ArrayList<DownloadLink> getDLinks(String parameter, CryptedLink cryptedLink) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Browser br3 = new Browser();
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
            br3.getPage(url);
            if (br3.getRedirectLocation() != null) {
                br3.setFollowRedirects(true);
                br3.getPage(url);
            }
            if (br3.containsHTML("Du hast zu oft das Captcha falsch")) {
                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    logger.info("Reconnect successfull. try again");
                    br3.setFollowRedirects(true);
                    br3.getPage(url);
                    if (br3.getRedirectLocation() != null) {
                        br3.setFollowRedirects(true);
                        br3.getPage(url);
                    }
                } else {
                    logger.severe("Reconnect failed. abort.");
                    return decryptedLinks;
                }

            }
            if (br3.containsHTML("Download-Limit")) {
                logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.decryptlimit", "SJ Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)"));
                new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < 100; i++) {
                            try {
                                Thread.sleep(1200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            progress.increase(1);
                        }
                    }
                }).start();

                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    logger.info("Reconnect successfull. try again");
                    br3.setFollowRedirects(true);
                    br3.getPage(url);
                    if (br3.getRedirectLocation() != null) {
                        br3.setFollowRedirects(true);
                        br3.getPage(url);
                    }
                } else {
                    progress.setColor(Color.red);
                    progress.setStatus(0);
                    progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.downloadlimit", "Error: SerienJunkies Downloadlimit"));
                    for (int i = 0; i < 100; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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
                br3.getPage(url);
                parameter = furl + modifiedURL;

            }

            String[][] links = br3.getRegex(Pattern.compile(" <a href=\"http://(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
            Vector<String> helpvector = new Vector<String>();
            String helpstring = "";

            // Einzellink
            if (parameter.indexOf("/safe/") >= 0 || parameter.indexOf("/save/") >= 0) {
                logger.info("safe link");
                helpstring = EinzelLinks(parameter, cryptedLink);

                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpstring)));
            } else if (parameter.indexOf(subdomain + "serienjunkies.org") >= 0 || parameter.indexOf("/sjsafe/") >= 0) {
                logger.info("sjsafe link");
                helpvector = ContainerLinks(parameter, cryptedLink);
                if (helpvector == null) return null;
                for (int j = 0; j < helpvector.size(); j++) {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpvector.get(j))));
                }
            } else {
                logger.info("else link");
                // Kategorien
                for (int i = 0; i < links.length; i++) {
                    if (links[i][0].indexOf("/safe/") >= 0) {
                        helpstring = EinzelLinks(links[i][0], cryptedLink);
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpstring)));
                    } else if (links[i][0].indexOf("/sjsafe/") >= 0) {
                        helpvector = ContainerLinks(links[i][0], cryptedLink);
                        if (helpvector == null) return null;
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(helpvector.get(j))));
                        }
                    } else {
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(links[i][0])));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedLinks;
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
            e.printStackTrace();
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
        dlink.addSourcePluginPasswords(passwords);
        if (name != null) {
            dlink.setDownloadSize(size * 1024 * 1024);
        }
        dlink.getLinkStatus().setStatusText("SerienJunkies");
        return dlink;
    }

    public String cutMatches(String data) {
        return data.replaceAll("(?i)http://[\\w\\.]*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es).*", "--CUT--");
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ar = decryptItMain(param);
        if (ar.size() > 1) {
            SJTable sjt = new SJTable(SimpleGUI.CURRENTGUI.getFrame(), ar);
            ar = sjt.dls;
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

    public ArrayList<DownloadLink> decryptItMain(CryptedLink param) throws Exception {
        String parameter = param.toString().trim();
        br.setCookiesExclusive(true);
        br.clearCookies("serienjunkies.org");
        br.getPage("http://serienjunkies.org/enter/");

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (parameter.matches(".*\\?(cat|p)\\=[\\d]+.*")) {
            boolean isP = parameter.contains("/?p=");
            int catst = sCatGrabb;
            if (!isP) catst = getSerienJunkiesCat();
            scatChecked = false;
            int cat = Integer.parseInt(parameter.replaceFirst(".*\\?(cat|p)\\=", "").replaceFirst("[^\\d].*", ""));
            if (sCatNewestDownload == catst) {
                br.getPage("http://serienjunkies.org/");

                Pattern pattern = Pattern.compile("<a href=\"http://serienjunkies.org/\\?cat\\=" + cat + "\">(.*?)</a><br", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(br + "");
                ArrayList<String> names = new ArrayList<String>();
                while (matcher.find()) {
                    names.add(matcher.group(1).toLowerCase());
                }
                if (names.size() == 0) { return decryptedLinks; }
                br.getPage(parameter);
                lastHtmlCode = br + "";
                for (String name : names) {
                    name += " ";
                    String[] bet = null;
                    while (bet == null) {
                        name = name.substring(0, name.length() - 1);
                        if (name.length() == 0) { return decryptedLinks; }
                        try {
                            bet = br.getRegex("<p><strong>(" + name + ".*?)</strong>(.*?)</p>").getMatches()[0];
                        } catch (Exception e) {
                            // TODO: handle exception
                        }

                    }

                    String[] links = HTMLParser.getHttpLinks(bet[1], br.getRequest().getUrl().toString());
                    if (getPluginConfig().getStringProperty("SJ_MIRRORMANAGEMENT", mirrorManagement[0]).equals(mirrorManagement[2])) {
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
                    br.getPage(parameter);
                    htmlcode = br + "";
                } else {
                    br.getPage("http://serienjunkies.org/?cat=" + cat);
                    htmlcode = br + "";
                    try {
                        int pages = Integer.parseInt(br.getRegex("<p align=\"center\">  Pages \\(([\\d]+)\\):").getMatch(0));
                        for (int i = 2; i < pages + 1; i++) {
                            htmlcode += "\n" + br.getPage("http://serienjunkies.org/?cat=" + cat + "&paged=" + i);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                HashMap<String, Integer> mirrors = new HashMap<String, Integer>();
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
                            // TODO: handle exception
                        }
                        FilePackage fp = new FilePackage();
                        fp.setName(title + (b > 1 ? " " + b : ""));
                        b++;
                        fp.setPassword(JDUtilities.passwordArrayToString(passwords.toArray(new String[passwords.size()])));
                        String[][] links = new Regex(element2, "<p><strong>(.*?)</strong>(.*?)</p>").getMatches();
                        for (String[] element3 : links) {
                            String[] sp2 = element3[1].split("<strong>.*?</strong>");
                            // boolean rscom = (Boolean) getPluginConfig()
                            // .getProperty("USE_RAPIDSHARE_V2", true);
                            if (getPluginConfig().getStringProperty("SJ_MIRRORMANAGEMENT", mirrorManagement[0]).equals(mirrorManagement[2])) {
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
                                if (getPluginConfig().getStringProperty("SJ_MIRRORMANAGEMENT", mirrorManagement[0]).equals(mirrorManagement[0])) {
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

    public CryptedLink[] getDecryptableLinks(String data) {
        String[] links = new Regex(data, "http://[\\w\\.]*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es)[^\"]*", Pattern.CASE_INSENSITIVE).getColumn(-1);
        ArrayList<CryptedLink> ret = new ArrayList<CryptedLink>();
        scatChecked = true;
        for (String element : links) {
            if (canHandle(element)) {
                ret.add(new CryptedLink(element));
            }
        }
        return ret.toArray(new CryptedLink[ret.size()]);
    }

    private String getHostname(String link) {
        if (link.matches(".*rc[\\_\\-].*")) {
            return "rapidshare.com";
        } else if (link.matches(".*rs[\\_\\-].*")) {
            return "rapidshare.de";
        } else if (link.matches(".*nl[\\_\\-].*")) {
            return "netload.in";
        } else if (link.matches(".*ut[\\_\\-].*")) {
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
                        e.printStackTrace();
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
                // TODO: handle exception
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

    public String getVersion() {
        return getVersion("$Revision$");
    }

    private String isNext() {
        if (next) {
            return "|";
        } else {
            next = true;
        }
        return "";

    }

    private void sCatDialog() {
        if (scatChecked || useScat[1] == saveScat) { return; }
        new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {

            /**
             * 
             */
            private static final long serialVersionUID = -5144850223169000644L;

            void init() {
                setLayout(new BorderLayout());
                setModal(true);
                setTitle(JDLocale.L("plugins.SerienJunkies.CatDialog.title", "SerienJunkies ::CAT::"));
                setAlwaysOnTop(true);
                JPanel panel = new JPanel(new GridBagLayout());
                final class meth {
                    public String name;

                    public int var;

                    public meth(String name, int var) {
                        this.name = name;
                        this.var = var;
                    }

                    public String toString() {

                        return name;
                    }
                }
                addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {

                    }

                    public void windowClosed(WindowEvent e) {

                    }

                    public void windowClosing(WindowEvent e) {
                        useScat = new int[] { ((meth) methods.getSelectedItem()).var, 0 };
                        dispose();

                    }

                    public void windowDeactivated(WindowEvent e) {

                    }

                    public void windowDeiconified(WindowEvent e) {

                    }

                    public void windowIconified(WindowEvent e) {

                    }

                    public void windowOpened(WindowEvent e) {

                    }
                });
                meth[] meths = new meth[3];
                meths[0] = new meth("Kategorie nicht hinzufügen", sCatNoThing);
                meths[1] = new meth("Alle Serien in dieser Kategorie hinzufügen", sCatGrabb);
                meths[2] = new meth("Den neusten Download dieser Kategorie hinzufügen", sCatNewestDownload);
                methods = new JComboBox(meths);
                checkScat = new JCheckBox("Einstellungen für diese Sitzung beibehalten?", true);
                Insets insets = new Insets(0, 0, 0, 0);
                JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("plugins.SerienJunkies.CatDialog.action", "Wählen sie eine Aktion aus:")), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, methods, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, checkScat, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JButton btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
                btnOK.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        useScat = new int[] { ((meth) methods.getSelectedItem()).var, checkScat.isSelected() ? saveScat : 0 };
                        dispose();
                    }

                });
                JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                add(panel, BorderLayout.CENTER);
                pack();
                setLocation(JDUtilities.getCenterOfComponent(null, this));
                setVisible(true);
            }

        }.init();
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), "SJ_MIRRORMANAGEMENT", mirrorManagement, JDLocale.L("plugins.decrypt.serienjunkies.mirrorManagement", "mirror management")));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.decrypt.general.hosterSelection", "Hoster selection")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_RAPIDSHARE_V2", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_RAPIDSHAREDE_V2", "Rapidshare.de"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_NETLOAD_V2", "Netload.in"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_UPLOADED_V2", "Uploaded.to"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_FILEFACTORY_V2", "FileFactory.com"));
        cfg.setDefaultValue(true);
    }

    public boolean useUserinputIfCaptchaUnknown() {
        return false;
    }

    public class SerienjunkiesThread extends Thread {
        private DownloadLink downloadLink;
        public ArrayList<DownloadLink> result = null;
        private CryptedLink cryptedLink;

        public SerienjunkiesThread(DownloadLink downloadLink, CryptedLink cryptedLink) {
            this.downloadLink = downloadLink;
            this.cryptedLink = cryptedLink;
        }

        @Override
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
                    if (linkStatus.getErrorMessage() == null || linkStatus.getErrorMessage().endsWith("")) linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.pageerror", "SJ liefert keine Downloadlinks"));
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
                                progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.checkLinks", "check links"));
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
                            Browser br2k = new Browser();
                            br2k.getPage("http://serienjunkies.org/?s=" + cryptedLink.getCryptedUrl().replaceFirst(".*/", "").replaceFirst("\\.html?$", ""));
                            String[] info = getLinkName(cryptedLink.getCryptedUrl(), br2k.toString());
                            logger.warning("use Mirror");
                            mirrors = getMirrors(cryptedLink.getCryptedUrl(), info[2]);
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
                                            progress.setProgressText(JDLocale.L("plugins.decrypt.serienjunkies.progress.checkMirror", "check mirror"));
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
                                    e.printStackTrace();
                                }
                                if (finaldls != null) break;
                            }
                        }
                        if (finaldls == null) {
                            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                            linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.archiveincomplete", "Archiv nicht komplett"));

                        }
                        for (DownloadLink downloadLink2 : finaldls) {
                            downloadLink2.addSourcePluginPasswords(passwords);
                        }

                    }
                    result = finaldls;
                }

            } catch (Exception e) {
                e.printStackTrace();
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
}

@SuppressWarnings("serial")
class SJTable extends JDialog {
    protected JTable m_table;
    private Thread countdownThread;
    private int countdown = 60;
    private boolean interrupted = false;
    protected SJTM m_data;
    private JButton insertButton;

    protected JLabel m_title;
    public ArrayList<DownloadLink> dls;

    public SJTable(JFrame owner, ArrayList<DownloadLink> DownloadLinks) {
        super(owner);
        this.setTitle(JDLocale.L("plugin.serienjunkies.manager.title", "SerienJunkies Linkverwaltung"));
        setSize(600, 300);
        this.setLocation(JDUtilities.getCenterOfComponent(null, this));
        this.dls = DownloadLinks;
        m_data = new SJTM(dls);
        setModal(true);
        m_title = new JLabel(JDLocale.L("plugin.serienjunkies.manager.dllinks", "Unerwünschte Links einfach löschen"), new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.config.infoFile"))), SwingConstants.LEFT);
        getContentPane().add(m_title, BorderLayout.NORTH);

        m_table = new JTable();
        m_table.setAutoCreateColumnsFromModel(false);
        m_table.setModel(m_data);

        m_table.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                interrupted = true;
            }

            public void mouseEntered(MouseEvent e) {
                interrupted = true;
            }

            public void mouseExited(MouseEvent e) {
                interrupted = true;
            }

            public void mousePressed(MouseEvent e) {
                interrupted = true;
            }

            public void mouseReleased(MouseEvent e) {
                interrupted = true;
            }
        });

        for (int k = 0; k < SJTM.m_columns.length; k++) {
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
            renderer.setHorizontalAlignment(SJTM.m_columns[k].m_alignment);
            TableColumn column = new TableColumn(k, SJTM.m_columns[k].m_width, renderer, null);
            m_table.addColumn(column);
        }
        JTableHeader header = m_table.getTableHeader();
        header.setUpdateTableInRealTime(false);
        JScrollPane ps = new JScrollPane();
        ps.getViewport().add(m_table);
        getContentPane().add(ps, BorderLayout.CENTER);
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JButton del = new JButton(JDLocale.L("gui.component.textarea.context.delete", "Löschen"));
        addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {

                dispose();

            }

            public void windowClosing(WindowEvent e) {
                dls = new ArrayList<DownloadLink>();

            }

            public void windowDeactivated(WindowEvent e) {

            }

            public void windowDeiconified(WindowEvent e) {

            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {

            }
        });
        del.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                interrupted = true;
                int[] rows = m_table.getSelectedRows();
                ArrayList<DownloadLink> delDls = new ArrayList<DownloadLink>();
                for (int j : rows) {
                    delDls.add(dls.get(j));
                }
                dls.removeAll(delDls);
                m_table.tableChanged(new TableModelEvent(m_table.getModel()));

            }
        });
        panel.add(del);
        insertButton = new JButton(JDLocale.L("gui.component.textarea.context.paste", "Einfügen"));
        insertButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();

            }
        });
        panel.add(insertButton);
        getContentPane().add(panel, BorderLayout.SOUTH);

        countdownThread = new Thread() {

            @Override
            public void run() {

                while (!isVisible() && isDisplayable()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                int c = countdown;

                while (--c >= 0) {
                    if (interrupted == true) {
                        insertButton.setText(JDLocale.L("gui.component.textarea.context.paste", "Einfügen"));
                        return;
                    }
                    if (countdownThread == null) { return; }

                    insertButton.setText(JDUtilities.formatSeconds(c) + ">>" + JDLocale.L("gui.component.textarea.context.paste", "Einfügen"));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) {

                    return; }

                }
                dispose();

            }

        };
        countdownThread.start();
        setVisible(true);
    }
}

class ColumnData {
    public String m_title;

    public int m_width;

    public int m_alignment;

    public ColumnData(String title, int width, int alignment) {
        m_title = title;
        m_width = width;
        m_alignment = alignment;
    }
}

@SuppressWarnings("serial")
class SJTM extends AbstractTableModel {
    static final public ColumnData m_columns[] = { new ColumnData(JDLocale.L("gui.packageinfo.name", "Name"), 200, JLabel.LEFT), new ColumnData(JDLocale.L("gui.treetable.header_3.hoster", "Anbieter"), 160, JLabel.LEFT), new ColumnData(JDLocale.L("gui.linkgrabber.packagetab.table.column.size", "Größe"), 100, JLabel.RIGHT) };

    ArrayList<DownloadLink> dls;

    public SJTM(ArrayList<DownloadLink> dls) {
        this.dls = dls;
    }

    public int getRowCount() {
        return dls == null ? 0 : dls.size();
    }

    public int getColumnCount() {
        return m_columns.length;
    }

    public String getColumnName(int column) {
        return m_columns[column].m_title;
    }

    public boolean isCellEditable(int nRow, int nCol) {
        return false;
    }

    public Object getValueAt(int nRow, int nCol) {
        switch (nCol) {
        case 0:
            return dls.get(nRow).getName();
        case 1:
            return dls.get(nRow).getHost();
        case 2: {
            long size = dls.get(nRow).getDownloadSize();
            if (size > 1048576)
                return size / 1048576 + " mb";
            else
                return size / 1024 + " kb";
        }
        }
        return "";
    }
}
