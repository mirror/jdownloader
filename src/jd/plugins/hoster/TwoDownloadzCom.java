//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "2downloadz.com" }, urls = { "https?://(www\\.)?2downloadz\\.com/[a-z0-9]+" }, flags = { 0 })
public class TwoDownloadzCom extends PluginForHost {

    private String               correctedBR                  = "";
    private String               passCode                     = null;
    private static final String  PASSWORDTEXT                 = "<br><b>Passwor(d|t):</b> <input";
    // primary website url, take note of redirects
    private static final String  COOKIE_HOST                  = "http://2downloadz.com";
    // domain names used within download links.
    private static final String  DOMAINS                      = "(2downloadz\\.com)";
    private static final String  MAINTENANCE                  = ">This server is in maintenance mode";
    private static final String  MAINTENANCEUSERTEXT          = JDL.L("hoster.xfilesharingprobasic.errors.undermaintenance", "This server is under Maintenance");
    private static final String  ALLWAIT_SHORT                = JDL.L("hoster.xfilesharingprobasic.errors.waitingfordownloads", "Waiting till new downloads can be started");
    private static final String  PREMIUMONLY1                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly1", "Max downloadable filesize for free users:");
    private static final String  PREMIUMONLY2                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly2", "Only downloadable via premium or registered");
    private static final boolean VIDEOHOSTER                  = false;
    private static final boolean SUPPORTSHTTPS                = false;
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections
    // fail. .:. use [1-20]
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);
    // don't touch the following!
    private static AtomicInteger maxFree                      = new AtomicInteger(1);

    // DEV NOTES
    // non account: 1 * 20
    // free account: chunks * maxdls
    // premium account: chunks * maxdls
    // protocol: no https
    // captchatype: custom 3 digit captcha
    // other:

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // link cleanup, but respect users protocol choosing.
        if (!SUPPORTSHTTPS) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://"));
        }
        // output the hostmask as we wish based on COOKIE_HOST url!
        String desiredHost = new Regex(COOKIE_HOST, "https?://([^/]+)").getMatch(0);
        String importedHost = new Regex(link.getDownloadURL(), "https?://([^/]+)").getMatch(0);
        link.setUrlDownload(link.getDownloadURL().replaceAll(importedHost, desiredHost));
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    public TwoDownloadzCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    public void prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        prepBrowser(br);
        getPage(link.getDownloadURL());
        if (new Regex(correctedBR, ">There is no file for this link").matches()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String[] fileInfo = new String[3];
        scanInfo(fileInfo);
        if (fileInfo[0] == null || fileInfo[0].equals("")) {
            logger.warning("filename equals null, throwing \"plugin defect\"");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fileInfo[2] != null && !fileInfo[2].equals("")) link.setMD5Hash(fileInfo[2].trim());
        fileInfo[0] = fileInfo[0].replaceAll("(</b>|<b>|\\.html)", "");
        link.setName(fileInfo[0].trim());
        if (fileInfo[1] != null && !fileInfo[1].equals("")) link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
        return AvailableStatus.TRUE;
    }

    private String[] scanInfo(final String[] fileInfo) {
        final String[] info = new Regex(correctedBR, "<font style=\"font\\-size:12px\" color=\"#848484\" face=\"Arial\"><b>([^<>\"]*?)</b></font></div>").getColumn(0);
        if (info != null && info.length == 2) {
            fileInfo[0] = info[0];
            fileInfo[1] = info[1];
        }
        return fileInfo;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, false, 1, "freelink");
    }

    @SuppressWarnings("unused")
    public void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = this.checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            final String id = new Regex(correctedBR, "getElementById\\(\\'mmm\\'\\)\\.hh =\"([a-z0-9]+)\"").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String continueLink = "http://www.2downloadz.com/download.php?h=" + fid + "&id=" + id;
            getPage(continueLink);

            continueLink = new Regex(correctedBR, "\\'(download\\.php\\?id=[a-z0-9]+\\&s=\\d+)\\'").getMatch(0);
            if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            continueLink = "http://www.2downloadz.com/" + continueLink + "&c=";
            getPage(continueLink);

            continueLink = new Regex(correctedBR, "\\'(download\\.php\\?id=[a-z0-9]+\\&s=[^<>\"]*?)\\'").getMatch(0);
            if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            continueLink = "http://www.2downloadz.com/" + continueLink + "&c=";
            getPage(continueLink);

            continueLink = new Regex(correctedBR, "\\'(download\\.php\\?id=[a-z0-9]+\\&s=[^<>\"]*?)\\'").getMatch(0);
            if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            continueLink = "http://www.2downloadz.com/" + continueLink + "&c=";
            br.cloneBrowser().getPage("http://www.2downloadz.com/preCaptcha.php?id=" + id);
            final String captchaAdress = "http://www.2downloadz.com/Captcha.php?id=" + id;
            for (int i = 1; i <= 3; i++) {
                final String code = getCaptchaCode(captchaAdress, downloadLink);
                br.cloneBrowser().getPage("http://www.2downloadz.com/antibots.php?id=" + id);
                getPage(continueLink + Encoding.urlEncode(code));
                if (correctedBR.contains("preCaptcha.php?id=")) {
                    continue;
                }
                break;
            }
            if (correctedBR.contains("preCaptcha.php?id=")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

            if (correctedBR.contains("src=\"images/ready5.png\"")) {
                final String seconds = br.getRegex("count_down\\((\\d+)\\);").getMatch(0);
                if (seconds != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(seconds) * 1001l); }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }

            dllink = new Regex(correctedBR, "\\'(get_file1\\.php\\?id=[a-z0-9]+\\&h=[a-z0-9]+)\\'").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = "http://www.2downloadz.com/" + dllink;
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            correctBR();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        fixFilename(downloadLink);
        try {
            // add a download slot
            controlFree(+1);
            // start the dl
            dl.startDownload();
        } finally {
            // remove download slot
            controlFree(-1);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    /**
     * Prevents more than one free download from starting at a given time. One
     * step prior to dl.startDownload(), it adds a slot to maxFree which allows
     * the next singleton download to start, or at least try.
     * 
     * This is needed because xfileshare(website) only throws errors after a
     * final dllink starts transferring or at a given step within pre download
     * sequence. But this template(XfileSharingProBasic) allows multiple
     * slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20)
     * captcha events all at once and only allows one download to start. This
     * prevents wasting peoples time and effort on captcha solving and|or
     * wasting captcha trading credits. Users will experience minimal harm to
     * downloading as slots are freed up soon as current download begins.
     * 
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    /** Remove HTML code which could break the plugin */
    public void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();

        // remove custom rules first!!! As html can change because of generic
        // cleanup rules.

        // generic cleanup
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");

        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
    }

    private String decodeDownloadLink(final String s) {
        String decoded = null;

        try {
            Regex params = new Regex(s, "\\'(.*?[^\\\\])\\',(\\d+),(\\d+),\\'(.*?)\\'");

            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");

            while (c != 0) {
                c--;
                if (k[c].length() != 0) p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
            }

            decoded = p;
        } catch (Exception e) {
        }

        String finallink = null;
        if (decoded != null) {
            finallink = new Regex(decoded, "name=\"src\"value=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = new Regex(decoded, "type=\"video/divx\"src=\"(.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = new Regex(decoded, "\\.addVariable\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
                }
            }
        }
        return finallink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void getPage(final String page) throws Exception {
        br.getPage(page);
        correctBR();
    }

    @SuppressWarnings("unused")
    private void postPage(final String page, final String postdata) throws Exception {
        br.postPage(page, postdata);
        correctBR();
    }

    private void sendForm(final Form form) throws Exception {
        br.submitForm(form);
        correctBR();
    }

    /**
     * @param downloadLink
     */
    private void fixFilename(final DownloadLink downloadLink) {
        String oldName = downloadLink.getFinalFileName();
        if (oldName == null) oldName = downloadLink.getName();
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        String newExtension = null;
        // some streaming sites do not provide proper file.extension within
        // headers (Content-Disposition or the fail over getURL()).
        if (serverFilename == null) {
            logger.info("Server filename is null, keeping filename: " + oldName);
        } else {
            if (serverFilename.contains(".")) {
                newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
            } else {
                logger.info("HTTP headers don't contain filename.extension information");
            }
        }
        if (newExtension != null && !oldName.endsWith(newExtension)) {
            String oldExtension = null;
            if (oldName.contains(".")) oldExtension = oldName.substring(oldName.lastIndexOf("."));
            if (oldExtension != null && oldExtension.length() <= 5) {
                downloadLink.setFinalFileName(oldName.replace(oldExtension, newExtension));
            } else {
                downloadLink.setFinalFileName(oldName + newExtension);
            }
        }
    }

    public void checkErrors(final DownloadLink theLink, final boolean checkAll) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(correctedBR, PASSWORDTEXT).matches() && correctedBR.contains("Wrong password")) {
                // handle password has failed in the past, additional try
                // catching / resetting values
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                passCode = null;
                theLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (correctedBR.contains("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (correctedBR.contains("\">Skipped countdown<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
        }
        /** Wait time reconnect handling */
        if (new Regex(correctedBR, "(You have reached the download(\\-| )limit|You have to wait)").matches()) {
            // adjust this regex to catch the wait time string for COOKIE_HOST
            String WAIT = new Regex(correctedBR, "((You have reached the download(\\-| )limit|You have to wait)[^<>]+)").getMatch(0);
            String tmphrs = new Regex(WAIT, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs == null) tmphrs = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = new Regex(WAIT, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin == null) tmpmin = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = new Regex(WAIT, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(WAIT, "\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                logger.info("Waittime regexes seem to be broken");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                if (tmpdays != null) days = Integer.parseInt(tmpdays);
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                /** Not enough wait time to reconnect->Wait and try again */
                if (waittime < 180000) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.xfilesharingprobasic.allwait", ALLWAIT_SHORT), waittime); }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (correctedBR.contains("You're using all download slots for IP")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        if (correctedBR.contains("Error happened when generating Download Link")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 10 * 60 * 1000l);
        /** Error handling for only-premium links */
        if (new Regex(correctedBR, "( can download files up to |Upgrade your account to download bigger files|>Upgrade your account to download larger files|>The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit)").matches()) {
            String filesizelimit = new Regex(correctedBR, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.info("As free user you can download files up to " + filesizelimit + " only");
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLY1 + " " + filesizelimit);
            } else {
                logger.info("Only downloadable via premium");
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLY2);
            }
        }
        if (br.getURL().contains("/?op=login&redirect=")) {
            logger.info("Only downloadable via premium");
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLY2);
        }
        if (new Regex(correctedBR, MAINTENANCE).matches()) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, MAINTENANCEUSERTEXT, 2 * 60 * 60 * 1000l);
    }

    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(correctedBR, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        if (new Regex(correctedBR, "(File Not Found|<h1>404 Not Found</h1>)").matches()) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}