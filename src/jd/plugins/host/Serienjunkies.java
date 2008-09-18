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
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class Serienjunkies extends PluginForHost {

    private String dynamicCaptcha = "(?s)<FORM ACTION=\".*?\" METHOD=\"post\".*?<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"(.*?)\">.*?<IMG SRC=\"([^\"]*)\"";
    private Pattern patternCaptcha = null;
    private String subdomain = "download.";
    private DownloadLink downloadLink;

    private static boolean active = false;

    public Serienjunkies(PluginWrapper wrapper) {
        super(wrapper);

    }

    public boolean collectCaptchas() {
        return false;
    }

    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url) throws InterruptedException {
        Vector<String> links = new Vector<String>();

        if (url.matches("http://[\\w\\.]*?.serienjunkies.org/..\\-.*")) {
            url = url.replaceFirst("serienjunkies.org", "serienjunkies.org/frame");
        }
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            RequestInfo reqinfo = HTTP.getRequest(new URL(url));

            String cookie = reqinfo.getCookie();
            File captchaFile = null;
            String capTxt = null;
            while (true) {
                reqinfo.setHtmlCode(reqinfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", ""));
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }
                    String[][] gifs = new Regex(reqinfo.getHtmlCode(), patternCaptcha).getMatches();

                    String captchaAdress = "http://" + subdomain + "serienjunkies.org" + gifs[0][1];

                    HTTPConnection con = HTTP.getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection();

                    if (con.getResponseCode() < 0) {
                        captchaAdress = "http://" + subdomain + "serienjunkies.org" + gifs[0][1];
                        con = HTTP.getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection();

                    }
                    if (con.getContentLength() < 1000) {
                        if (!Reconnecter.waitForNewIP(5 * 60l)) { return null; }

                        reqinfo = HTTP.getRequest(new URL(url));
                        cookie = reqinfo.getCookie();

                        continue;
                    }

                    captchaFile = Plugin.getLocalCaptchaFile(this, ".gif");

                    try {
                        Browser.download(captchaFile, con);
                    } catch (Exception e) {

                        Thread.sleep(1000);
                        reqinfo = HTTP.getRequest(new URL(url));
                        cookie = reqinfo.getCookie();

                        continue;
                    }

                    logger.info("captchafile: " + captchaFile);
                    capTxt = Plugin.getCaptchaCode(captchaFile, this);

                    reqinfo = HTTP.postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");

                } else {
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

                                        logger.severe("Unknown letter: // " + i + ":  JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER:  " + capTxt.substring(i, i + 1)); // Pixelstring
                                        // .
                                        // getB()
                                        // ist
                                        // immer
                                        // der
                                        // neue
                                        // letter
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
                    break;
                }
            }
            if (reqinfo.getLocation() != null) {
                links.add(reqinfo.getLocation());
            }
            Form[] forms = reqinfo.getForms();
            for (int i = 0; i < forms.length; i++) {
                if (!forms[i].action.contains("firstload")) {
                    try {
                        reqinfo = HTTP.getRequest(new URL(forms[i].action));
                        reqinfo = HTTP.getRequest(new URL(new Regex(reqinfo.getHtmlCode(), Pattern.compile("SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0)), null, null, false);
                        String loc = reqinfo.getLocation();
                        if (loc != null) {
                            links.add(loc);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    // Für Links die gleich auf den Hoster relocaten
    private String EinzelLinks(String url) throws InterruptedException, PluginException {
        String links = "";

        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            if (!url.matches(".*sa[fv]e/f.*")) {
                url = url.replaceAll("safe/", "safe/f");
                url = url.replaceAll("save/", "save/f");
            }
            RequestInfo reqinfo = HTTP.getRequest(new URL(url));
            File captchaFile = null;
            String capTxt = null;
            while (true) {
                reqinfo.setHtmlCode(reqinfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", ""));
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
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

                        Thread.sleep(1000);
                        reqinfo = HTTP.getRequest(new URL(url));

                        continue;
                    }
                    capTxt = Plugin.getCaptchaCode(this, "einzellinks.serienjunkies.org", captchaFile, false, downloadLink);
                    reqinfo = HTTP.postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                } else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                        if (useUserinputIfCaptchaUnknown() && getCaptchaDetectionID() == Plugin.CAPTCHA_USER_INPUT && getLastCaptcha() != null && getLastCaptcha().getLetterComperators() != null) {
                            LetterComperator[] lcs = getLastCaptcha().getLetterComperators();
                            getLastCaptcha().setCorrectcaptchaCode(capTxt.trim());

                            if (lcs.length == capTxt.trim().length()) {
                                for (int i = 0; i < capTxt.length(); i++) {

                                    if (lcs[i] != null && lcs[i].getDecodedValue() != null && capTxt.substring(i, i + 1).equalsIgnoreCase(lcs[i].getDecodedValue()) && lcs[i].getValityPercent() < 30.0) {
                                        logger.severe("OK letter: " + i + ": JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER: " + capTxt.substring(i, i + 1));
                                    } else {

                                        logger.severe("Unknown letter: // " + i + ":  JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER:  " + capTxt.substring(i, i + 1)); // Pixelstring
                                        // .
                                        // getB()
                                        // ist
                                        // immer
                                        // der
                                        // neue
                                        // letter
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
                    break;
                }
            }

            links = reqinfo.getLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    public String getAGBLink() {

        return "http://serienjunkies.org/?page_id=35";
    }

    public String getCoder() {
        return "JD-Team";
    }

    public ArrayList<DownloadLink> getDLinks(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        try {
            URL url = new URL(parameter);
            subdomain = new Regex(parameter, "http://(.*?)serienjunkies.org.*").getMatch(0);
            String modifiedURL = Encoding.htmlDecode(url.toString());
            modifiedURL = modifiedURL.replaceAll("safe/", "safe/f");
            modifiedURL = modifiedURL.replaceAll("save/", "save/f");
            modifiedURL = modifiedURL.substring(modifiedURL.lastIndexOf("/"));

            patternCaptcha = Pattern.compile(dynamicCaptcha);
            logger.fine("using patternCaptcha:" + patternCaptcha);
            RequestInfo reqinfo = HTTP.getRequest(url, null, null, true);
            if (reqinfo.getLocation() != null) {
                reqinfo = HTTP.getRequest(url, null, null, true);
            }
            if (reqinfo.containsHTML("Du hast zu oft das Captcha falsch")) {
                downloadLink.getLinkStatus().setStatusText("Reconnect required");
                downloadLink.requestGuiUpdate();
                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    logger.info("Reconnect successfull. try again");
                    reqinfo = HTTP.getRequest(url, null, null, true);
                    if (reqinfo.getLocation() != null) {
                        reqinfo = HTTP.getRequest(url, null, null, true);
                    }
                    downloadLink.getLinkStatus().setStatusText("Decrypt");
                    downloadLink.requestGuiUpdate();
                } else {
                    logger.severe("Reconnect failed. abort.");
                    return decryptedLinks;
                }

            }
            if (reqinfo.containsHTML("Download-Limit")) {
                logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 5 min)");
                downloadLink.getLinkStatus().setStatusText("Reconnect required");
                downloadLink.requestGuiUpdate();
                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    logger.info("Reconnect successfull. try again");
                    reqinfo = HTTP.getRequest(url, null, null, true);
                    if (reqinfo.getLocation() != null) {
                        reqinfo = HTTP.getRequest(url, null, null, true);
                    }
                    downloadLink.getLinkStatus().setStatusText("Decrypt");
                    downloadLink.requestGuiUpdate();
                } else {
                    logger.severe("Reconnect failed. abort.");
                    return decryptedLinks;
                }
            }
            String furl = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<FRAME SRC=\"(.*?)" + modifiedURL.replaceAll("[^0-1a-zA-Z]", ".") + "\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (furl != null) {
                url = new URL(furl + modifiedURL);
                logger.info("Frame found. frame url: " + furl + modifiedURL);
                reqinfo = HTTP.getRequest(url, null, null, true);
                parameter = furl + modifiedURL;

            }

            String[][] links = new Regex(reqinfo.getHtmlCode(), Pattern.compile(" <a href=\"http://(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
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
            // TODO: handle exception
        }
        return decryptedLinks;
    }

    public boolean getFileInformation(DownloadLink downloadLink) {
        return true;
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

    public void handle0(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        this.downloadLink = downloadLink;
        String link = (String) downloadLink.getProperty("link");
        String[] mirrors = (String[]) downloadLink.getProperty("mirrors");
        int c = 0;
        while (active) {
            if (c++ == 120) break;

            downloadLink.getLinkStatus().setStatusText("waiting for decryption");
            Thread.sleep(1000);

        }
        active = true;
        downloadLink.getLinkStatus().setStatusText("decrypt");
        downloadLink.requestGuiUpdate();
        ArrayList<DownloadLink> dls = getDLinks(link);

        if (dls.size() < 1) {
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.pageerror", "SJ liefert keine Downloadlinks"));
            logger.warning("SJ returned no Downloadlinks");
            active = false;
            return;
        }

        FilePackage fp = new FilePackage();
        fp.setDownloadDirectory(downloadLink.getFilePackage().getDownloadDirectory());
        fp.setExtractAfterDownload(downloadLink.getFilePackage().isExtractAfterDownload());
        fp.setProperties(downloadLink.getFilePackage().getProperties());
        fp.setPassword(downloadLink.getFilePackage().getPassword());
        fp.setName(downloadLink.getName());
        // int index = fp.indexOf(downloadLink);
        // fp.remove(downloadLink);
        Vector<Integer> down = new Vector<Integer>();
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        for (int i = dls.size() - 1; i >= 0; i--) {
            DistributeData distributeData = new DistributeData(dls.get(i).getDownloadURL());
            Vector<DownloadLink> links = distributeData.findLinks();
            Iterator<DownloadLink> it2 = links.iterator();
            boolean online = false;
            while (it2.hasNext()) {
                DownloadLink downloadLink3 = (DownloadLink) it2.next();
                // if (downloadLink3.isAvailable()) {
                fp.add(downloadLink3);

                online = true;
                // } else {
                // down.add(i);
                // }

            }
            if (online) {
                ret.addAll(links);
            }
            // ret.addAll(down);
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
                            Iterator<DownloadLink> it2 = links.iterator();
                            boolean online = false;
                            while (it2.hasNext()) {
                                DownloadLink downloadLink3 = (DownloadLink) it2.next();
                                if (downloadLink3.isAvailable()) {
                                    fp.add(downloadLink3);
                                    online = true;
                                    iter.remove();
                                }

                            }
                            if (online) {
                                ret.addAll(links);
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
            // fp.add(downloadLink);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.archiveincomplete", "Archiv nicht komplett"));
            active = false;
            return;
        } else {
            JDUtilities.getController().removeDownloadLink(downloadLink);
            JDUtilities.getController().addPackage(fp);
        }
        active = false;
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
