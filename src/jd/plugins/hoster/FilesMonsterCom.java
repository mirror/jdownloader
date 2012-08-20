//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

/** Is similar to BitBonusCom */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "http://[\\w\\.\\d]*?filesmonsterdecrypted\\.com/(download.php\\?id=|dl/.*?/free/2/).+" }, flags = { 2 })
public class FilesMonsterCom extends PluginForHost {

    private static final String POSTTHATREGEX        = "\"(http://filesmonster\\.com/dl/.*?/free/.*?)\"";
    private static final String POSTTHATREGEX2       = "(http://(www\\.)?filesmonster\\.com/dl/.*?/free/.+)";
    private static final String TEMPORARYUNAVAILABLE = "Download not available at the moment";
    private static final String REDIRECTFNF          = "DL_FileNotFound";
    private static final String PREMIUMONLYUSERTEXT  = "Only downloadable via premium";
    private static final Object LOCK                 = new Object();
    private static boolean      loaded               = false;

    public FilesMonsterCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filesmonster.com/service.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("filesmonsterdecrypted.com/", "filesmonster.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://filesmonster.com/rules.php";
    }

    // @Override to keep compatible to stable
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (downloadLink.getStringProperty("PREMIUMONLY") != null && account == null) {
            /* premium only */
            return false;
        }
        if (downloadLink.getDownloadURL().contains("/free/2/") && account != null) {
            /* free only */
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().contains("/free/2/")) {
            br.getPage(downloadLink.getStringProperty("mainlink"));
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains(REDIRECTFNF)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setName(downloadLink.getName());
            downloadLink.setDownloadSize(downloadLink.getDownloadSize());
        } else {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("(>File was deleted by owner or it was deleted for violation of copyrights<|>File not found<|>The link could not be decoded<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains(REDIRECTFNF)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (loaded == false) {
                synchronized (LOCK) {
                    if (loaded == false) {
                        /*
                         * we only have to load this once, to make sure its
                         * loaded
                         */
                        JDUtilities.getPluginForDecrypt("filesmonster.comDecrypt");
                    }
                    loaded = true;
                }
            }
            String filename = br.getRegex(jd.plugins.decrypter.FilesMonsterDecrypter.FILENAMEREGEX).getMatch(0);
            String filesize = br.getRegex(jd.plugins.decrypter.FilesMonsterDecrypter.FILESIZEREGEX).getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            }
        }
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"));
        if (downloadLink.getStringProperty("PREMIUMONLY") != null) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filesmonstercom.only4premium", PREMIUMONLYUSERTEXT));
        return AvailableStatus.TRUE;

    }

    private String getNewTemporaryLink(String mainlink, String originalfilename) throws IOException, PluginException {
        // Find a new temporary link
        String mainlinkpart = new Regex(mainlink, "filesmonster\\.com/download\\.php\\?id=(.+)").getMatch(0);
        String temporaryLink = null;
        br.getPage(mainlink);
        String[] allInfo = jd.plugins.decrypter.FilesMonsterDecrypter.getTempLinks(br);
        if (allInfo != null && allInfo.length != 0) {
            for (String singleInfo : allInfo)
                if (singleInfo.contains("\"name\":\"" + originalfilename + "\"")) temporaryLink = new Regex(singleInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
        }
        if (temporaryLink != null) temporaryLink = "http://filesmonster.com/dl/" + mainlinkpart + "/free/2/" + temporaryLink + "/";
        return temporaryLink;
    }

    private void handleErrors() throws PluginException {
        logger.info("Handling errors...");
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"), 120 * 60 * 1000l);
        String wait = br.getRegex("You can wait for the start of downloading (\\d+)").getMatch(0);
        if (wait == null) {
            wait = br.getRegex("is already in use (\\d+)").getMatch(0);
            if (wait == null) {
                wait = br.getRegex("You can start new download in (\\d+)").getMatch(0);
                if (wait == null) {
                    if (wait == null) {
                        wait = br.getRegex("will be available for free download in (\\d+) min\\.").getMatch(0);
                        if (wait == null) {
                            wait = br.getRegex("<br>Next free download will be available in (\\d+) min").getMatch(0);
                            if (wait == null) {
                                wait = br.getRegex("will be available for free download in (\\d+) min").getMatch(0);
                            }
                        }
                    }
                }
            }

        }
        if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(wait) * 60 * 1001l);
        if (br.containsHTML("Minimum interval between free downloads is 45 minutes")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 45 * 60 * 1001l);
        if (br.containsHTML("Respectfully yours Adminstration of Filesmonster\\.com")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (br.containsHTML("You need Premium membership to download files")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesmonstercom.only4premium", PREMIUMONLYUSERTEXT));
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getStringProperty("PREMIUMONLY") != null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesmonstercom.only4premium", PREMIUMONLYUSERTEXT));
        handleErrors();
        br.setFollowRedirects(true);
        String postThat = br.getRegex(POSTTHATREGEX).getMatch(0);
        if (postThat == null) postThat = new Regex(downloadLink.getDownloadURL(), POSTTHATREGEX2).getMatch(0);
        if (postThat == null) {
            logger.warning("The following string could not be found: postThat");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!downloadLink.getDownloadURL().contains("/free/2/")) {
            br.postPage(postThat, "");
            if (br.containsHTML("Free download links:")) {
                downloadLink.setProperty("PREMIUMONLY", "true");
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesmonstercom.only4premium", PREMIUMONLYUSERTEXT));
            }
        } else {
            downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
            String newTemporaryLink = getNewTemporaryLink(downloadLink.getStringProperty("mainlink"), downloadLink.getStringProperty("origfilename"));
            if (newTemporaryLink == null) {
                logger.warning("Failed to find a new temporary link for this link...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(newTemporaryLink);
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
        handleErrors();
        if (br.containsHTML("(Captcha number error or expired|api\\.recaptcha\\.net)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (!downloadLink.getDownloadURL().contains("/free/2/")) {
            String finalPage = br.getRegex("reserve_ticket\\(\\'(/dl/.*?)\\'\\)").getMatch(0);
            if (finalPage == null) {
                logger.warning("The following string could not be found: finalPage");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* request ticket for this file */
            br.getPage("http://filesmonster.com" + finalPage);
            String linkPart = br.getRegex("dlcode\":\"(.*?)\"").getMatch(0);
            String firstPart = new Regex(postThat, "(http://filesmonster\\.com/dl/.*?/free/)").getMatch(0);
            if (linkPart == null || firstPart == null) {
                logger.warning("The following string could not be found: linkPart or firstPart");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String nextLink = firstPart + "2/" + linkPart + "/";
            br.getPage(nextLink);
        }
        String strangeLink = br.getRegex("get_link\\('(/dl/.*?)'\\)").getMatch(0);
        if (strangeLink == null) {
            logger.warning("The following string could not be found: strangeLink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        strangeLink = "http://filesmonster.com" + strangeLink;
        String regexedwaittime = br.getRegex("id=\\'sec\\'>(\\d+)</span>").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        int shortWaittime = 45;
        if (regexedwaittime != null) {
            shortWaittime = Integer.parseInt(regexedwaittime);
        } else {
            logger.warning("Waittime regex doesn't work, using default waittime...");
        }
        sleep(shortWaittime * 1100l, downloadLink);
        try {
            br.getPage(strangeLink);
        } catch (Exception e) {
        }
        handleErrors();
        String dllink = br.getRegex("url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("The following string could not be found: dllink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The downloadlink doesn't seem to refer to a file, following the connection...");
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

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.postPage("http://filesmonster.com/login.php", "act=login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&login=Login");
        if (br.getRegex("Your membership type: <span class=\"[A-Za-z0-9 ]+\">(Premium)</span>").getMatch(0) == null || br.containsHTML("Username/Password can not be found in our database") || br.containsHTML("Try to recover your password by 'Password reminder'")) throw new PluginException(LinkStatus.ERROR_PREMIUM);
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
        ai.setUnlimitedTraffic();
        String expires = br.getRegex("\">Valid until: <span class=\\'green\\'>(.*?)</span>").getMatch(0);
        long ms = 0;
        if (expires != null) {
            ms = TimeFormatter.getMilliSeconds(expires, "MM/dd/yy HH:mm", null);
            if (ms <= 0) {
                ms = TimeFormatter.getMilliSeconds(expires, "MM/dd/yy", null);
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
        if (!downloadLink.getDownloadURL().contains("download.php?id=")) {
            logger.info(downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesmonstercom.only4freeusers", "This file is only available for freeusers"));
        }
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

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}