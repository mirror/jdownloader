//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;
import jd.controlling.DistributeData;
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

    private static int active = 0;

    public Serienjunkies(PluginWrapper wrapper) {
        super(wrapper);

    }

    public boolean collectCaptchas() {
        return false;
    }

    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url) throws PluginException {
        Vector<String> links = new Vector<String>();
        Browser br = new Browser();
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
                        if (!Reconnecter.waitForNewIP(5 * 60l)) { return null; }

                        htmlcode = br.getPage(url);

                        continue;
                    }

                    captchaFile = Plugin.getLocalCaptchaFile(this, ".gif");

                    try {
                        Browser.download(captchaFile, con);
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
            for (int i = 0; i < forms.length; i++) {
                if (!forms[i].action.contains("firstload") && !forms[i].action.equals("http://mirror.serienjunkies.org")) {
                    try {

                        for (int j = 0; j < 4; j++) {
                            if (i > 0) {
                                Thread.sleep(1000 * i);
                            }
                            br.getPage(forms[i].action);

                            String loc = br.openGetConnection(new Regex(br.toString(), Pattern.compile("SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0)).getHeaderField("Location");
                            if (loc != null) {

                                links.add(loc);
                                break;
                            }
                        }

                    } catch (Exception e) {
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        handle0(downloadLink);
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
        return;
    }

    public ArrayList<DownloadLink> getAvailableDownloads(DownloadLink downloadLink, int activeCaptchas) throws Exception {

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        this.downloadLink = downloadLink;
        String link = (String) downloadLink.getProperty("link");
        String[] mirrors = (String[]) downloadLink.getProperty("mirrors");
        int c = 0;
        while (active > activeCaptchas) {
            if (c++ == 120) break;

            //downloadLink.getLinkStatus().setStatusText("waiting for decryption"
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

        // int index = fp.indexOf(downloadLink);
        // fp.remove(downloadLink);
        Vector<Integer> down = new Vector<Integer>();
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        ArrayList<DownloadLink> fp = new ArrayList<DownloadLink>();
        for (int i = dls.size() - 1; i >= 0; i--) {
            DistributeData distributeData = new DistributeData(dls.get(i).getDownloadURL());
            Vector<DownloadLink> links = distributeData.findLinks();
            boolean online = false;
            DownloadLink[] it2 = links.toArray(new DownloadLink[links.size()]);
            if (it2.length > 0) {
                boolean[] re = it2[0].getPlugin().checkLinks(it2);
                if (re == null || re.length != it2.length) {
                    re = new boolean[it2.length];
                    for (int j = 0; j < re.length; j++) {
                        re[j] = it2[j].isAvailable();
                    }
                }
                for (int j = 0; j < it2.length; j++) {
                    if (re[j]) {
                        fp.add(it2[j]);
                        online = true;
                    } else
                        down.add(j);
                }
                if (online) {
                    ret.addAll(links);
                }
            }
        }

        if (mirrors != null) {
            for (String element : mirrors) {
                if (down.size() > 0) {
                    try {
                        dls = getDLinks(element);

                        Iterator<Integer> iter = down.iterator();
                        while (iter.hasNext()) {
                            Integer integer = (Integer) iter.next();
                            DistributeData distributeData = new DistributeData(dls.get(integer).getDownloadURL());
                            Vector<DownloadLink> links = distributeData.findLinks();
                            DownloadLink[] it2 = links.toArray(new DownloadLink[links.size()]);
                            if (it2.length > 0) {
                                boolean online = false;
                                boolean[] re = it2[0].getPlugin().checkLinks(it2);
                                if (re == null || re.length != it2.length) {
                                    re = new boolean[it2.length];
                                    for (int j = 0; j < re.length; j++) {
                                        re[j] = it2[j].isAvailable();
                                    }
                                }
                                for (int i = 0; i < it2.length; i++) {
                                    if (re[i]) {
                                        fp.add(it2[i]);
                                        online = true;
                                        iter.remove();
                                    }
                                }
                                if (online) {
                                    ret.addAll(links);
                                }
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    break;
                }
            }
        }
        if (down.size() > 0) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.archiveincomplete", "Archiv nicht komplett"));
            return null;
        }
        return fp;
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
