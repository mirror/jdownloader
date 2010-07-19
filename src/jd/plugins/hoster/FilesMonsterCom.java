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

import java.io.File;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "http://[\\w\\.\\d]*?(decryptedfilesmonster\\.com/download.php\\?id=|(decrypted)?filesmonster\\.com/dl/.*?/free/(2|1)/).+" }, flags = { 2 })
public class FilesMonsterCom extends PluginForHost {
    private static final String PROPERTY_NO_SLOT_WAIT_TIME = "NO_SLOT_WAIT_TIME";

    public FilesMonsterCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://filesmonster.com/service.php");
    }

    private static final String POSTTHATREGEX = "\"(http://filesmonster\\.com/dl/.*?/free/.*?)\"";
    private static final String POSTTHATREGEX2 = "(http://(www\\.)?filesmonster\\.com/dl/.*?/free/.+)";
    private static final String TEMPORARYUNAVAILABLE = "Download not available at the moment";
    private static final String REDIRECTFNF = "DL_FileNotFound";

    @SuppressWarnings("deprecation")
    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_NO_SLOT_WAIT_TIME, JDL.L("plugins.hoster.filesmonstercom.noslotwaittime", "No slot wait time (seconds)"), 30, 86400).setDefaultValue(60).setStep(30));
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("decryptedfilesmonster.com", "filesmonster.com"));
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();

        br.setFollowRedirects(true);
        br.postPage("http://filesmonster.com/login.php", "act=login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&login=");
        if (!br.containsHTML("<p>Your membership type: <span class=\"green\">Premium</span>") || br.containsHTML("Username/Password can not be found in our database") || br.containsHTML("Try to recover your password by 'Password reminder'")) throw new PluginException(LinkStatus.ERROR_PREMIUM);

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);

        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String hostedFiles = br.getRegex(">Hosted Files</span></td>.*?<td>(\\d+).*?<a").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        String trafficleft = br.getRegex("id=\"info_credit\">.*?<strong>(.*?)</strong>").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(Regex.getSize(trafficleft));
        }
        String expires = br.getRegex("Membership period ends.*?([0-9]{1,2}/[0-9]{1,2}/[0-9]{1,2} [0-9]{1,2}:[0-9]{1,2}) ").getMatch(0);

        long ms = 0;
        if (expires == null) {

            expires = br.getRegex("<span style=color:#[0-9a-z]+>.*?([0-9]{1,2}/[0-9]{1,2}/[0-9]{1,2} [0-9]{1,2}:[0-9]{1,2}).*?</span>").getMatch(0);

        }
        if (expires == null) {

            expires = br.getRegex("\\(valid until (.*?)\\)").getMatch(0);

        }

        if (expires != null) {
            ms = Regex.getMilliSeconds(expires, "MM/dd/yy HH:mm", null);
            if (ms <= 0) {
                ms = Regex.getMilliSeconds(expires, "MM/dd/yy", null);
            }

            ai.setValidUntil(ms);
            account.setValid(true);
            return ai;
        } else {
            account.setValid(false);
            return ai;
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"), 120 * 60 * 1000l);
        String premlink = br.getRegex("\"(http://filesmonster\\.com/get/.*?)\"").getMatch(0);
        if (premlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(premlink);
        if (br.containsHTML("but it has exceeded the daily limit download in total")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        String ajaxurl = br.getRegex("get_link\\(\"(.*?)\"\\)").getMatch(0);
        Browser ajax = br.cloneBrowser();
        ajax.getPage(ajaxurl);

        String dllink = ajax.getRegex("url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("\\\\/", "/");
        /* max chunks to 1 , because each chunk gets calculated full size */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().contains("/free/2/")) {
            String mainlinkpart = new Regex(downloadLink.getDownloadURL(), "filesmonster\\.com/dl/(.*?)/").getMatch(0);
            br.getPage("http://filesmonster.com/download.php?id=" + mainlinkpart);
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains(REDIRECTFNF)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setName(downloadLink.getName());
            downloadLink.setDownloadSize(downloadLink.getDownloadSize());
        } else {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains(REDIRECTFNF)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String filesize = br.getRegex("File size: <span class=\"em\">(.*?)</span>").getMatch(0);
            String filename = br.getRegex("File name: <span class=\"em\">(.*?)</span>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setName(Encoding.htmlDecode(filename.trim()));
            if (filesize != null) {
                downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
            }
        }
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"));
        return AvailableStatus.TRUE;

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"), 120 * 60 * 1000l);
        br.setFollowRedirects(true);
        String wait = br.getRegex("You can wait for the start of downloading (\\d+)").getMatch(0);
        if (wait != null && br.containsHTML("You reached your")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(wait) * 60 * 1000l);
        wait = br.getRegex("is already in use (\\d+)").getMatch(0);
        if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(wait) * 60 * 1000l);
        wait = br.getRegex("You can start new download in (\\d+)").getMatch(0);
        if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(wait) * 60 * 1000l);
        // The code below will always cause a reconnect and i don't know how
        // filesmonster detects that...
        boolean pluginBroken = true;
        if (pluginBroken) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* get file id */
        String fmurl = br.getRegex("(http://[\\w\\.\\d]*?filesmonster\\.com/)").getMatch(0);
        if (fmurl == null) {
            if (br.containsHTML("UNDER MAINTENANCE")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesmonstercom.maintenance", "Maintenance"), 60 * 1000l);
            } else if (br.containsHTML("File was deleted by")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String isPay = br.getRegex("<input type=\"hidden\" name=\"showpayment\" value=\"(1)").getMatch(0);
            Boolean isFree = br.containsHTML("slowdownload");
            if (!isFree && isPay != null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesmonstercom.premiumonly", "Only downloadable via premium"));
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

        }
        String postThat = br.getRegex(POSTTHATREGEX).getMatch(0);
        if (postThat == null) postThat = new Regex(downloadLink.getDownloadURL(), POSTTHATREGEX2).getMatch(0);
        if (postThat == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!downloadLink.getDownloadURL().contains("/free/2/")) {
            br.postPage(postThat, "");
        } else {
            downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
            // Find a new temporary link
            String mainlinkpart = new Regex(downloadLink.getDownloadURL(), "filesmonster\\.com/dl/(.*?)/").getMatch(0);
            String originalfilename = downloadLink.getStringProperty("origfilename");
            String originalfilesize = downloadLink.getStringProperty("origsize");
            String oldTemporaryLinkPart = new Regex(downloadLink.getDownloadURL(), "/free/2/(.+)").getMatch(0);
            br.getPage("http://filesmonster.com/download.php?id=" + mainlinkpart);
            postThat = br.getRegex(POSTTHATREGEX).getMatch(0);
            if (postThat == null) postThat = new Regex(downloadLink.getDownloadURL(), POSTTHATREGEX2).getMatch(0);
            if (postThat == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.postPage(postThat, "");
            String findOtherLinks = br.getRegex("reserve_ticket\\('(/dl/rft/.*?)'\\)").getMatch(0);
            if (findOtherLinks == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://filesmonster.com" + findOtherLinks);
            String regexToFindANewTemporaryLinkInfo = "\\{(\"dlcode\":\"[A-Za-z0-9_.-]+\",\"name\":\"" + originalfilename + ")\",\"size\":\\d+,\"cutted_name\":\".{38}\"\\}";
            String tempLinkInfo = br.getRegex(regexToFindANewTemporaryLinkInfo).getMatch(0);
            if (tempLinkInfo == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String newTemporaryLinkPart = new Regex(tempLinkInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
            downloadLink.setUrlDownload(downloadLink.getDownloadURL().replace(oldTemporaryLinkPart, newTemporaryLinkPart));
            br.getPage(downloadLink.getDownloadURL());
        }
        /* now we have the data page, check for wait time and data id */
        // Captcha handling
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        if (br.containsHTML("(Captcha number error or expired|api\\.recaptcha\\.net)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (!downloadLink.getDownloadURL().contains("/free/2/")) {
            String finalPage = br.getRegex("reserve_ticket\\('(/dl/.*?)'\\)").getMatch(0);
            if (finalPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            /* request ticket for this file */
            br.getPage("http://filesmonster.com" + finalPage);
            String linkPart = br.getRegex("dlcode\":\"(.*?)\"").getMatch(0);
            String firstPart = new Regex(postThat, "(http://filesmonster\\.com/dl/.*?/free/)").getMatch(0);
            if (linkPart == null || firstPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String nextLink = firstPart + "2/" + linkPart + "/";
            br.getPage(nextLink);
        }
        if (br.containsHTML("(>You've downloaded the file recently\\.<|>You've downloaded file recently\\.)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 45 * 60 * 1001l);
        String strangeLink = br.getRegex("get_link\\('(/dl/.*?)'\\)").getMatch(0);
        if (strangeLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        strangeLink = "http://filesmonster.com" + strangeLink;
        String regexedwaittime = br.getRegex("id='sec'>(\\d+)</span>").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int shortWaittime = 45;
        if (regexedwaittime != null) shortWaittime = Integer.parseInt(regexedwaittime);
        sleep(shortWaittime * 1100l, downloadLink);
        try {
            br.getPage(strangeLink);
        } catch (Exception e) {
        }
        String dllink = br.getRegex("url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final link equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getAGBLink() {

        return "http://filesmonster.com/rules.php";

    }

}
