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

package jd.plugins.host;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class Serienjunkies extends PluginForHost {

    private String dynamicCaptcha = "(?s)<FORM ACTION=\".*?\" METHOD=\"post\".*?<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"(.*?)\">.*?<IMG SRC=\"([^\"]*)\"";
    private Pattern patternCaptcha = null;
    private String subdomain = "download.";
    private DownloadLink downloadLink;
    private static Vector<String> passwords = new Vector<String>();
    private static int active = 0;
    private ProgressController progress;

    public Serienjunkies(PluginWrapper wrapper) {
        super(wrapper);
        passwords.add("serienjunkies.dl.am");
        passwords.add("serienjunkies.org");
    }

    public boolean collectCaptchas() {
        return false;
    }

    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url) throws PluginException {
        final Vector<String> links = new Vector<String>();
        final Browser br = new Browser();
        if (url.matches("http://[\\w\\.]*?.serienjunkies.org/..\\-.*")) {
            url = url.replaceFirst("serienjunkies.org", "serienjunkies.org/frame");
        }
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            String htmlcode = br.getPage(url);

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
                    Browser capbr = br.cloneBrowser();
                    capbr.setFollowRedirects(true);
                    HTTPConnection con = capbr.openGetConnection(captchaAdress);

                    if (con.getResponseCode() < 0) {
                        captchaAdress = "http://" + subdomain + "serienjunkies.org" + gifs[0][1];
                        capbr.setFollowRedirects(true);
                        con = capbr.openGetConnection(captchaAdress);

                    }
                    if (con.getContentLength() < 1000) {
                        logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                        downloadLink.getLinkStatus().setStatusText("Reconnect required");
                        downloadLink.requestGuiUpdate();
                        progress.setProgressText("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                        new Thread(new Runnable() {
                            public void run() {
                                for (int i = 0; i < 100; i++) {
                                    try {
                                        Thread.sleep(1200);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    progress.increase(1);
                                }
                            }
                        }).start();
                        if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                            progress.setColor(Color.red);
                            progress.setStatus(0);
                            progress.setProgressText("Error: SerienJunkies Downloadlimit");
                            for (int i = 0; i < 100; i++) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                progress.increase(1);
                            }

                            return null;
                        }

                        htmlcode = br.getPage(url);

                        continue;
                    }

                    captchaFile = Plugin.getLocalCaptchaFile(this, ".gif");

                    try {
                        br.downloadConnection(captchaFile,con);
                  
                    } catch (Exception e) {

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        htmlcode = br.getPage(url);

                        continue;
                    }

                    logger.info("captchafile: " + captchaFile);
                    active++;
                    try {
                        capTxt = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
                    } catch (Exception e) {
                        active--;
                        e.printStackTrace();
                        break;
                    }
                    active--;

                    htmlcode = br.postPage(url, "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");

                } else {
                    captchaMethod(captchaFile, capTxt);
                    break;
                }
            }
            if (br.getRedirectLocation() != null) {
                links.add(br.getRedirectLocation());
            }
            Form[] forms = br.getForms();
            Vector<Thread> threads = new Vector<Thread>();
            final Browser[] br2 = new Browser[] { br.cloneBrowser(), br.cloneBrowser(), br.cloneBrowser(), br.cloneBrowser() };
            progress.setProgressText("getLinks");
            progress.setStatus(0);

            ArrayList<String> actions = new ArrayList<String>();
            for (Form form : forms) {
                if (form.action.contains("download.serienjunkies.org") && !form.action.contains("firstload") && !form.action.equals("http://mirror.serienjunkies.org")) {
                    actions.add(form.action);
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
                            for (int j = 0; j < 20; j++) {
                                try {
                                    Thread.sleep(300 * j);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                try {

                                    String tx = null;
                                    synchronized (brd) {
                                        try {
                                            tx = brd.getPage(action2);
                                        } catch (Exception e) {

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
                                    // TODO Auto-generated catch block
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

            for (Thread t : threads) {
                while (t.isAlive()) {
                    synchronized (t) {
                        try {
                            t.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    // Für Links die gleich auf den Hoster relocaten
    private String EinzelLinks(String url) throws PluginException {
        String links = "";
        Browser br = new Browser();
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        new Thread(new Runnable() {

            public void run() {
                // TODO Auto-generated method stub

            }
        }).start();
        try {
            if (!url.matches(".*sa[fv]e/f.*")) {
                url = url.replaceAll("safe/", "safe/f");
                url = url.replaceAll("save/", "save/f");
            }
            String htmlcode = br.getPage(url);
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
                        htmlcode = br.getPage(url);

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

                    htmlcode = br.postPage(url, "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                } else {
                    captchaMethod(captchaFile, capTxt);
                    break;
                }
            }

            links = br.getRedirectLocation();
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

    public String getAGBLink() {
        return "http://serienjunkies.org/?page_id=35";
    }

    public ArrayList<DownloadLink> getDLinks(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Browser br = new Browser();
        try {
            URL url = new URL(parameter);
            subdomain = new Regex(parameter, "http://(.*?)serienjunkies.org.*").getMatch(0);
            String modifiedURL = Encoding.htmlDecode(url.toString());
            modifiedURL = modifiedURL.replaceAll("safe/", "safe/f");
            modifiedURL = modifiedURL.replaceAll("save/", "save/f");
            modifiedURL = modifiedURL.substring(modifiedURL.lastIndexOf("/"));

            patternCaptcha = Pattern.compile(dynamicCaptcha);
            logger.fine("using patternCaptcha:" + patternCaptcha);
            br.setFollowRedirects(true);
            br.getPage(url);
            if (br.getRedirectLocation() != null) {
                br.setFollowRedirects(true);
                br.getPage(url);
            }
            if (br.containsHTML("Du hast zu oft das Captcha falsch")) {
                downloadLink.getLinkStatus().setStatusText("Reconnect required");
                downloadLink.requestGuiUpdate();
                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    logger.info("Reconnect successfull. try again");
                    br.setFollowRedirects(true);
                    br.getPage(url);
                    if (br.getRedirectLocation() != null) {
                        br.setFollowRedirects(true);
                        br.getPage(url);
                    }
                    downloadLink.getLinkStatus().setStatusText("Decrypt");
                    downloadLink.requestGuiUpdate();
                } else {
                    logger.severe("Reconnect failed. abort.");
                    downloadLink.getLinkStatus().setErrorMessage("Error Reconnect failed");
                    return decryptedLinks;
                }

            }
            if (br.containsHTML("Download-Limit")) {
                logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                downloadLink.getLinkStatus().setStatusText("Reconnect required");
                downloadLink.requestGuiUpdate();
                progress.setProgressText("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 2 min)");
                new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < 100; i++) {
                            try {
                                Thread.sleep(1200);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            progress.increase(1);
                        }
                    }
                }).start();

                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    logger.info("Reconnect successfull. try again");
                    br.setFollowRedirects(true);
                    br.getPage(url);
                    if (br.getRedirectLocation() != null) {
                        br.setFollowRedirects(true);
                        br.getPage(url);
                    }
                    downloadLink.getLinkStatus().setStatusText("Decrypt");
                    downloadLink.requestGuiUpdate();
                } else {
                    progress.setColor(Color.red);
                    progress.setStatus(0);
                    progress.setProgressText("Error: SerienJunkies Downloadlimit");
                    for (int i = 0; i < 100; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        progress.increase(1);
                    }
                    downloadLink.getLinkStatus().setErrorMessage("Error Reconnect failed");
                    logger.severe("Reconnect failed. abort.");
                    return decryptedLinks;
                }
            }
            String furl = br.getRegex(Pattern.compile("<FRAME SRC=\"(.*?)" + modifiedURL.replaceAll("[^0-1a-zA-Z]", ".") + "\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (furl != null) {
                url = new URL(furl + modifiedURL);
                logger.info("Frame found. frame url: " + furl + modifiedURL);
                br.setFollowRedirects(true);
                br.getPage(url);
                parameter = furl + modifiedURL;

            }

            String[][] links = br.getRegex(Pattern.compile(" <a href=\"http://(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
            Vector<String> helpvector = new Vector<String>();
            String helpstring = "";

            // Einzellink
            if (parameter.indexOf("/safe/") >= 0 || parameter.indexOf("/save/") >= 0) {
                logger.info("safe link");
                helpstring = EinzelLinks(parameter);
                decryptedLinks.add(new DownloadLink(this, null, getHost(), Encoding.htmlDecode(helpstring), true));
            } else if (parameter.indexOf(subdomain + "serienjunkies.org") >= 0) {
                logger.info("sjsafe link");
                helpvector = ContainerLinks(parameter);
                for (int j = 0; j < helpvector.size(); j++) {
                    decryptedLinks.add(new DownloadLink(this, null, getHost(), Encoding.htmlDecode(helpvector.get(j)), true));
                }
            } else if (parameter.indexOf("/sjsafe/") >= 0) {
                logger.info("sjsafe link");
                helpvector = ContainerLinks(parameter);
                for (int j = 0; j < helpvector.size(); j++) {
                    decryptedLinks.add(new DownloadLink(this, null, getHost(), Encoding.htmlDecode(helpvector.get(j)), true));
                }
            } else {
                logger.info("else link");
                // Kategorien
                for (int i = 0; i < links.length; i++) {
                    if (links[i][0].indexOf("/safe/") >= 0) {
                        helpstring = EinzelLinks(links[i][0]);
                        decryptedLinks.add(new DownloadLink(this, null, getHost(), Encoding.htmlDecode(helpstring), true));
                    } else if (links[i][0].indexOf("/sjsafe/") >= 0) {
                        helpvector = ContainerLinks(links[i][0]);
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(new DownloadLink(this, null, getHost(), Encoding.htmlDecode(helpvector.get(j)), true));
                        }
                    } else {
                        decryptedLinks.add(new DownloadLink(this, null, getHost(), Encoding.htmlDecode(links[i][0]), true));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedLinks;
    }

    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        linkStatus.setStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
        return false;
    }

    public String getVersion() {

        return getVersion("$Revision$");
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        handle0(downloadLink);
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
        return;
    }

    public ArrayList<DownloadLink> getAvailableDownloads(DownloadLink downloadLink, int activeCaptchas, ProgressController progress) throws Exception {
        this.progress = progress;
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        this.downloadLink = downloadLink;
        String link = (String) downloadLink.getProperty("link");
        String[] mirrors = (String[]) downloadLink.getProperty("mirrors");
        int c = 0;
        while (active > activeCaptchas) {
            if (c++ == 120) break;

            // downloadLink.getLinkStatus().setStatusText("waiting for decryption"
            // );
            Thread.sleep(100);

        }
        downloadLink.getLinkStatus().setStatusText("decrypt");
        downloadLink.requestGuiUpdate();
        ArrayList<DownloadLink> dls = getDLinks(link);

        if (dls.size() < 1) {
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            if (linkStatus.getErrorMessage() == null || linkStatus.getErrorMessage().endsWith("")) linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.pageerror", "SJ liefert keine Downloadlinks"));
            logger.warning("SJ returned no Downloadlinks");
            return null;
        }
        ArrayList<DownloadLink> finaldls = new ArrayList<DownloadLink>();

        for (DownloadLink dls2 : dls) {
            DistributeData distributeData = new DistributeData(dls2.getDownloadURL());
            finaldls.addAll(distributeData.findLinks());
        }
        if (finaldls.size() > 0) {
            try {
                DownloadLink[] linksar = finaldls.toArray(new DownloadLink[finaldls.size()]);
                progress.setProgressText("check links");
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

        if (mirrors != null && finaldls == null) {
            for (String element : mirrors) {
                try {
                    dls = getDLinks(element);
                    finaldls = new ArrayList<DownloadLink>();

                    for (DownloadLink dls2 : dls) {
                        DistributeData distributeData = new DistributeData(dls2.getDownloadURL());
                        finaldls.addAll(distributeData.findLinks());
                    }
                    if (finaldls.size() > 0) {
                        try {
                            DownloadLink[] linksar = finaldls.toArray(new DownloadLink[finaldls.size()]);
                            progress.setProgressText("check mirror");
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
            return null;
        }
        for (DownloadLink downloadLink2 : finaldls) {
            downloadLink2.addSourcePluginPasswords(passwords);
        }
        return finaldls;
    }

    public void handle0(DownloadLink downloadLink) throws Exception {
        /*
         * ArrayList<DownloadLink> fr = getAvailableDownloads(downloadLink,
         * true); if (fr != null) { FilePackage fp = new FilePackage();
         * fp.setDownloadDirectory
         * (downloadLink.getFilePackage().getDownloadDirectory());
         * fp.setExtractAfterDownload
         * (downloadLink.getFilePackage().isExtractAfterDownload());
         * fp.setProperties(downloadLink.getFilePackage().getProperties());
         * fp.setPassword(downloadLink.getFilePackage().getPassword());
         * fp.setName(downloadLink.getName()); for (DownloadLink downloadLink2 :
         * fr) { fp.add(downloadLink2); }
         * JDUtilities.getController().removeDownloadLink(downloadLink);
         * JDUtilities.getController().addPackage(fp); } active = false;
         */
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public boolean useUserinputIfCaptchaUnknown() {
        return false;
    }
}
