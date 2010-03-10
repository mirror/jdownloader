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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharedzip.com" }, urls = { "http://[\\w\\.]*?(sharedzip)\\.com/[a-z0-9]{12}" }, flags = { 2 })
public class SharedZipCom extends PluginForHost {

    public SharedZipCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.sharedzip.com/free132.html");
    }

    public String getAGBLink() {
        return "http://www.sharedzip.com/tos.html";
    }

    public void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.sharedzip.com", "lang", "english");
        br.postPage("http://www.sharedzip.com/", "op=login&redirect=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&x=15&y=13");
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String passCode = null;
        // direct download or not?
        if (br.getRedirectLocation() != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 1);
        } else {
            br.loadConnection(null);
            br.forceDebug(true);
            Form download = br.getFormBySubmitvalue("Download+File");
            if (download == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input)")) {

                logger.info("The downloadlink seems to be password protected.");
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                download.put("password", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download, true, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input|Wrong password)")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.sharedzip.com", "lang", "english");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<Title>File Not Found</Title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileName = br.getRegex("You have requested <font color=\"red\">http://www.sharedzip.com/(.*?)/(.*?)</font> \\((.*?)\\)</font>").getMatch(1);
        long fileSize = Regex.getSize(br.getRegex("You have requested <font color=\"red\">http://www.sharedzip.com/(.*?)/(.*?)</font> \\((.*?)\\)</font>").getMatch(2));
        if (fileName == null || fileSize <= 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(fileName.trim());
        downloadLink.setDownloadSize(fileSize);
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.forceDebug(true);
        login(account);
        br.getPage("http://www.sharedzip.com/?op=my_account");
        String space = br.getRegex(Pattern.compile("<td>Used space:</td>.*?<td.*?b>(.*?)of.*?Mb</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim() + " Mb");
        String points = br.getRegex(Pattern.compile("<td>You have collected:</td.*?b>(.*?)premium points", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) {
            // Who needs half points ? If we have a dot in the points, just
            // remove it
            if (points.contains(".")) {
                String dot = new Regex(points, ".*?(\\.(\\d+))").getMatch(0);
                points = points.replace(dot, "");
            }
            ai.setPremiumPoints(Long.parseLong(points.trim()));
        }
        String validUntil = br.getRegex("Premium-Account expire:</TD><TD><b>(.*?)</b><").getMatch(0);
        if (validUntil == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd MMMM yyyy", Locale.GERMANY));
        String trafficAvail = br.getRegex("Traffic available today:</TD><TD><b>(.*?)<").getMatch(0);
        if (trafficAvail != null) {
            /* only unlimited accounts dont have a limit */
            ai.setTrafficLeft(trafficAvail);
        }
        return ai;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form form = br.getFormBySubmitvalue("Free+Download");
        br.forceDebug(true);
        br.submitForm(form);
        checkErrors(downloadLink);
        Form download = br.getFormBySubmitvalue("Download+File");
        if (download == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt == null) ttt = br.getRegex("id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        if (br.containsHTML("/captchas/")) {
            logger.info("Detected captcha method \"Standard captcha\" for this host");
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            String captchaurl = null;
            if (sitelinks == null || sitelinks.length == 0) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String link : sitelinks) {
                if (link.contains("/captchas/")) {
                    captchaurl = link;
                    break;
                }
            }
            if (captchaurl == null) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String code = getCaptchaCode(captchaurl, downloadLink);
            download.put("code", code);
            logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
        }
        // Booleans are for other implementations
        String passCode = null;
        boolean password = false;
        if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input)")) {
            password = true;
            logger.info("The downloadlink seems to be password protected.");
        }
        if (password == true) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            download.put("password", passCode);
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input|Wrong password)")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            checkErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    public void checkErrors(DownloadLink theLink) throws NumberFormatException, PluginException {
        // Some waittimes...
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            logger.info("Detected waittime #1, waiting " + waittime + "milliseconds");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        if (br.containsHTML("You have reached the download-limit")) {
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = br.getRegex("\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                if (tmpdays != null) days = Integer.parseInt(tmpdays);
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        // Errorhandling for only-premium links
        if (br.containsHTML("(You can download files up to.*?only|Upgrade your account to download bigger files|This file reached max downloads)")) {
            String filesizelimit = br.getRegex("You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.warning("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can only download files up to " + filesizelimit);
            } else {
                logger.warning("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
            }
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
