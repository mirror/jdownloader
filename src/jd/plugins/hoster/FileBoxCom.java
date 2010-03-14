//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebox.com" }, urls = { "(http://|https://)[\\w\\.]*?filebox\\.com/(.*?/[0-9a-z]{12}|[0-9a-z]{12})" }, flags = { 2 })
public class FileBoxCom extends PluginForHost {

    public FileBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.filebox.com/premium.html");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    public boolean registered = false;

    @Override
    public String getAGBLink() {
        return "http://www.filebox.com/tos.html";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        registered = false;
        br.setFollowRedirects(true);
        br.setCookie("http://www.filebox.com", "lang", "english");
        br.getPage("http://www.filebox.com/");
        Form loginform = br.getFormbyProperty("name", "FL");
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put("login", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        if (!br.containsHTML("You Premium membership expires on")) {
            registered = true;
        }
        if (br.getCookie("http://www.filebox.com/", "login") == null || br.getCookie("http://www.filebox.com/", "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex(Pattern.compile("Files Stored.*?\\((.*?)\\)<br>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        String hostedFiles = br.getRegex("Files Stored :.*?(\\d+).*?\\(").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        // TODO: Implement handle free for registered users!
        if (registered) {
            ai.setStatus("Registered User");
        } else {
            ai.setStatus("Premium User");
            String expire = br.getRegex("You Premium membership expires on (.*?[0-9]{4})").getMatch(0);
            if (expire == null) expire = br.getRegex("Expires.*?: (.*?[0-9]{4}).*?<br>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(Regex.getMilliSeconds(expire, "MMMM dd, yyyy", null));
            }
        }
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.filebox.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("fname\" value=\"(.*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("Filename</label>.*?title=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("File size</label>.*?\\((.*?)\\)").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setFinalFileName(filename);
        if (filesize != null) {
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (!registered) {
            String dllink = br.getRedirectLocation();
            if (dllink == null) dllink = br.getRegex("direct_link\".*?value=\"(http.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://media[0-9]+\\.filebox\\.com/files/.*?/[a-z0-9]+/.*?)\"").getMatch(0);
            if (dllink == null) {
                Form pwform = null;
                if (br.containsHTML("splash_Pasword")) {
                    pwform = br.getFormbyKey("fname");
                    if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    if (link.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", link);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = link.getStringProperty("pass", null);
                    }
                    pwform.put("password", passCode);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.submitForm(pwform);
                if (br.containsHTML("Wrong password")) {
                    logger.warning("Wrong password!");
                    link.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                Form dlform = br.getFormbyProperty("name", "F1");
                if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.submitForm(dlform);
                dllink = br.getRedirectLocation();
            }
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("<title>404 Not Found</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            doFree2(link);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    // Handles registered accounts and downloads without accounts!
    public void doFree2(DownloadLink downloadLink) throws Exception, PluginException {
        br.setFollowRedirects(true);
        String passCode = null;
        // Out commented stuff is the captcha handling which isn't needed right
        // now because their captcha system is buggy so you don't have
        // to enter anything and still you can start downloading...in case they
        // repair that just "activate" the following outcommented lines!
        // String captchaurl = br.getRegex("\"(/captchas/.*?)\"").getMatch(0);
        // if (captchaurl == null ) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // captchaurl = "https://www.filebox.com" + captchaurl;
        Form DLForm = br.getFormbyKey("fname");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML("splash_Pasword")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            DLForm.put("password", passCode);
        }
        // String code = getCaptchaCode(captchaurl, downloadLink);
        // logger.info("Entered captcha code is||" + code +
        // "|| from captchalink||" + captchaurl + "||");
        // DLForm.put("code", code);
        br.submitForm(DLForm);
        if (br.containsHTML("Wrong captcha")) {
            logger.warning("Wrong captcha or wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (br.containsHTML("Wrong password")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        // If the user wants to download a picture, he can only do this by using
        // this link...
        String piclink = br.getRegex("\"/cgi-bin/proxy\\.cgi\\?url=(http.*?)\"").getMatch(0);
        if (piclink == null) {
            Form finalform = br.getFormbyProperty("name", "F1");
            if (finalform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // Waittime...is skipable right now but in case they change it just
            // use
            // the following code (tested)
            // String ttt =
            // br.getRegex("countdown\">(\\d+)</span>").getMatch(0);
            // if (ttt != null) {
            // int tt = Integer.parseInt(ttt);
            // sleep(tt * 1001, downloadLink);
            // }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalform, false, 1);
        } else {
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, piclink, false, 1);
        }
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            if (br.containsHTML("<title>404 Not Found</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 60 * 60 * 1000l);
            if (br.containsHTML(">Free Registration<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Register for free and add your free filebox account like a premium account to download this file!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree2(downloadLink);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}