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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebase.to" }, urls = { "http://[\\w\\.]*?filebase\\.to/(files|download)/\\d{1,}/.*" }, flags = { 2 })
public class FileBaseTo extends PluginForHost {

    public FileBaseTo(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://filebase.to/buypremium/");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(/files|/download)", "/files"));
    }

    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }

    private static final String DIRECTLINKREGEX = "=\"(http://[0-9]+\\..*?/download/ticket.*?)\"";

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://filebase.to", "fb_language", "de");
        br.setFollowRedirects(false);
        br.postPage("http://filebase.to/index.php", "fb_username=" + Encoding.urlEncode(account.getUser()) + "&fb_password=" + Encoding.urlEncode(account.getPass()) + "&login_submit=login");
        if (br.getCookie("http://filebase.to/", "fb_username") == null || br.getCookie("http://filebase.to/", "fb_passwort") == null || br.containsHTML("Falscher Username/Passwort")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://filebase.to/user/premium/");
        String type = br.getRegex("Account Typ:[\n\r\t ]+</td>[\n\r\t ]+<td width=\"\\d+%\">[\n\r\t ]+<font color=\"green\"><b>(Premium)</b></font>").getMatch(0);
        if (type == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String points = br.getRegex("Ihr Punktestand:[\r\t\n ]+</td>[\r\t\n ]+<td>(.*?)</td>").getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        String expires = null;
        Regex dateAndTime = br.getRegex("<b>(\\d+\\.\\d+\\.\\d+) um (\\d+:\\d+) Uhr");
        String date = dateAndTime.getMatch(0);
        String time = dateAndTime.getMatch(1);
        if (date != null || time != null) {
            expires = date.trim() + "|" + time.trim();
            ai.setValidUntil(Regex.getMilliSeconds(expires, "dd.MM.yyyy|HH:mm", null));
            account.setValid(true);
            return ai;
        }
        String daysleft = br.getRegex("\\(([0-9]+).*?Tage\\)").getMatch(0);
        if (daysleft != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(daysleft) * 24 * 60 * 60 * 1000));
            account.setValid(true);
            ai.setStatus("Premium User");
            return ai;
        }
        account.setValid(false);
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        // Workaround for streaming files
        br.getPage(downloadLink.getDownloadURL().replace("/files/", "/download/"));
        Form dlForm = br.getFormbyKey("wait");
        dlForm = null;
        // in case they rename the form we can maybe still get the link of the
        // page, if not the plugin is defect
        if (dlForm == null) {
            String dllink = br.getRegex("\"(http://[0-9]+\\..*?/premium/.*?/.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
        // FIXME dlForm will always be null and would cause a
        // NullPointerException
        dl = BrowserAdapter.openDownload(br, downloadLink, dlForm, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        // br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setCookie("http://filebase.to", "fb_language", "de");
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        downloadLink.setName(Plugin.extractFileNameFromURL(url).replaceAll("&dl=1", ""));
        if (br.containsHTML("eider\\s+nicht\\s+gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("<b>Downloading: </b>.*? <b>\\((.*?)\\)</b><br").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<center><strong>.*?</strong></center>[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+<center><strong>.*?</strong></center>[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+<center><strong>.*?</strong></center>[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+<center><strong>(.*?)</strong></center>").getMatch(0);
            if (filesize == null) filesize = br.getRegex("<h1><b>Download: </b>.*? <b>\\((.*?)\\)</b>").getMatch(0);
        }
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;

    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form forms = null;
        String directLink = br.getRegex(DIRECTLINKREGEX).getMatch(0);
        if (directLink == null) {
            String uidValue = br.getRegex("id=\"uid\" value=\"(.*?)\"").getMatch(0);
            if (uidValue == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.postPage(br.getURL(), "dl_free3=Normal+Download&uid=" + uidValue);
            if (br.containsHTML("lädt bereits ein Datei runter\\. Warten Sie bitte, bis der Download abgeschlossen ist")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            if (br.containsHTML("(Deine Stundenlimit von 300 MB ist bereits erreicht|oder kaufen Sie ein Premium Account, Premium Nutzer haben diese Limit nicht)")) {
                int waitHours = 1;
                String waittime = br.getRegex("Bitte warten Sie (\\d+) Stunde oder kaufen Sie ein Premium Account").getMatch(0);
                if (waittime != null) {
                    logger.info("Found regexed waittime..." + waittime + " hours.");
                    waitHours = Integer.parseInt(waittime);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitHours * 60 * 60 * 1001l);
            }
            uidValue = br.getRegex("id=\"uid\" name=\"uid\" value=\"(.*?)\"").getMatch(0);
            if (uidValue == null) {
                logger.warning("uidValue #2 is null...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // // Ticket Time is skippable atm.
            // String ttt = br.getRegex("var sec = (\\d+);").getMatch(0);
            // int tt = 10;
            // if (ttt != null) {
            // logger.info("Waittime detected, waiting " + ttt.trim() +
            // " seconds from now on...");
            // tt = Integer.parseInt(ttt);
            // }
            // sleep(tt * 1001, downloadLink);
            br.postPage(br.getURL(), "submit=Download&captcha=ok&uid=" + uidValue + "&filetype=file");
            directLink = br.getRegex(DIRECTLINKREGEX).getMatch(0);
        }
        if (directLink != null) {
            dl = BrowserAdapter.openDownload(br, downloadLink, directLink);
        } else {
            String formact = downloadLink.getDownloadURL();
            // I'm not sure if we have captchas in new version
            if (br.containsHTML("/captcha/CaptchaImage")) {
                for (int i = 0; i <= 5; i++) {
                    File captchaFile = getLocalCaptchaFile(".png");
                    String captchaFileURL = br.getRegex("src=\"(/captcha/CaptchaImage\\.php.*?)\"").getMatch(0);
                    String filecid = br.getRegex("cid\"\\s+value=\"(.*?)\"").getMatch(0);
                    Browser.download(captchaFile, br.openGetConnection("http://filebase.to" + captchaFileURL));
                    String capTxt = getCaptchaCode(captchaFile, downloadLink);
                    br.postPage(formact, "uid=" + capTxt + "&cid=" + Encoding.urlEncode(filecid) + "&submit=+++Best%E4tigung+++&session_code=");
                    // if captcha error
                    if (br.containsHTML("Code wurde falsch")) {
                        br.getPage(downloadLink.getDownloadURL());
                        continue;
                    }
                    break;
                }
                // if captcha error after loop
                if (br.containsHTML("Code wurde falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            forms = br.getForm(0);
            if (forms == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = BrowserAdapter.openDownload(br, downloadLink, forms);
        }
        try {
            URLConnectionAdapter con = dl.getConnection();
            if (con.getContentType().contains("html")) {
                br.getPage(forms.getAction());
                if (br.containsHTML("error")) {
                    con.disconnect();
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.filebaseto.servererror", "Server error"));
                } else {
                    con.disconnect();
                    logger.warning("Unsupported error:");
                    logger.warning(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, JDL.L("plugins.hoster.filebaseto.unsupportederror", "Unsupported error"));
                }
            }
            dl.startDownload();
        } catch (IOException e) {

            if (e.getCause() instanceof NullPointerException) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("jd.plugins.hoster.filebaseto.serversideerror", "Server Error. Retry later"), 10 * 60 * 1000l); }
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
