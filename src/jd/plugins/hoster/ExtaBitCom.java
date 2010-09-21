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
import java.util.Random;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extabit.com" }, urls = { "http://[\\w\\.]*?extabit\\.com/file/[a-z0-9]+" }, flags = { 2 })
public class ExtaBitCom extends PluginForHost {

    public ExtaBitCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://extabit.com/premium.jsp");
    }

    private static final String NOTAVAILABLETEXT = ">File is temporary unavailable<";
    private static final String NOMIRROR         = ">No download mirror<";
    private static final String PREMIUMONLY      = ">Only premium users can download files of this size";

    @Override
    public String getAGBLink() {
        return "http://extabit.com/static/terms.jsp";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        // To get the english version of the page
        br.setCookie("http://extabit.com", "language", "en");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(File not found|Such file doesn't exsist)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)download Extabit.com - file hosting</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("download_filename\".*?>(.*?)</div").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("extabit\\.com/file/.*?'>(.*?)</a>").getMatch(0);
            }
        }
        String filesize = br.getRegex("class=\"download_filesize(_en)\">.*?\\[(.*?)\\]").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        if (br.containsHTML(PREMIUMONLY))
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ExtaBitCom.errors.Only4Premium", "This file is only available for premium users"));
        else if (br.containsHTML(NOTAVAILABLETEXT))
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ExtaBitCom.errors.TempUnavailable", "This file is temporary unavailable"));
        else if (br.containsHTML(NOMIRROR)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ExtaBitCom.errors.NoMirror", "Extabit error: \"No download mirror\", contact extabit support."));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.containsHTML(PREMIUMONLY))
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.ExtaBitCom.errors.Only4Premium", "This file is only available for premium users"));
        else if (br.containsHTML(NOTAVAILABLETEXT))
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.ExtaBitCom.errors.TempUnavailable", "This file is temporary unavailable"));
        else if (br.containsHTML(NOMIRROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.ExtaBitCom.errors.NoMirror", "Extabit error: \"No download mirror\", contact extabit support."), 120 * 60 * 1000l);
        String addedlink = br.getURL();
        if (!addedlink.equals(link.getDownloadURL())) link.setUrlDownload(addedlink);
        if (br.containsHTML("The daily downloads limit from your IP is exceeded")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        if (br.containsHTML("Next free download from your ip will be available")) {
            String wait = br.getRegex("will be available in <b>(.*?)minutes").getMatch(0);
            int minutes = 15;
            if (wait != null) minutes = Integer.parseInt(wait.trim());
            int waitTime = minutes * 60 * 1001;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
        }
        // If the waittime was forced it yould be here but it isn't!
        // Re Captcha handling

        Browser xmlbrowser = br.cloneBrowser();
        xmlbrowser.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (br.containsHTML("api.recaptcha.net")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, link);
            rc.getForm().put("capture", "1");
            rc.getForm().setAction(addedlink);
            rc.setCode(c);
        } else {
            for (int i = 0; i <= 5; i++) {
                // *Normal* captcha handling
                // Form dlform = br.getFormbyProperty("id", "cmn_form");
                String captchaurl = br.getRegex("(/capture\\.gif\\?\\d+)").getMatch(0);
                if (captchaurl == null) captchaurl = br.getRegex("<div id=\"reload_captcha\">.*?<img src=\"(/.*?)\"").getMatch(0);
                if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                captchaurl = "http://extabit.com" + captchaurl;
                String code = getCaptchaCode(captchaurl, link);
                // dlform.put("capture", code);
                // br.submitForm(dlform);
                xmlbrowser.getPage(link.getDownloadURL() + "?capture=" + code);
                if (!xmlbrowser.containsHTML("\"ok\":true")) {
                    br.getPage(br.getURL());
                    continue;
                }
                br.getPage(link.getDownloadURL());
                break;
            }
        }
        if (br.containsHTML("api.recaptcha.net") || !xmlbrowser.containsHTML("\"ok\":true")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("Turn your download manager off and[ ]+<a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://guest\\d+\\.extabit\\.com/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if ((dl.getConnection().getContentType().contains("html"))) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
            if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://extabit.com", "language", "en");
        br.setFollowRedirects(false);
        br.postPage("http://extabit.com/login.jsp", "email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&remember=1&auth_submit_login.x=" + new Random().nextInt(10) + "&auth_submit_login.y=" + new Random().nextInt(10) + "&auth_submit_login=Enter");
        if (br.getCookie("http://extabit.com/", "auth_uid") == null || br.getCookie("http://extabit.com/", "auth_hash") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://extabit.com/");
        String expire = br.getRegex("Premium is active till <span class=\"green\"><strong>(.*?)</strong>").getMatch(0);
        String downloadsLeft = br.getRegex("You have <span class=\"green\"><strong>(\\d+) downloads</strong>").getMatch(0);
        if (downloadsLeft != null) {
            ai.setStatus("Premium User" + downloadsLeft);
        } else if (expire != null) {
            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd.MM.yyyy", null));
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Account invalid");
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (br.getRedirectLocation() != null && br.getRedirectLocation().length() <= (link.getDownloadURL().length() + 5)) {
            link.setUrlDownload(br.getRedirectLocation());
            logger.info("New link was set, retrying!");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dllink == null) {
            if (br.containsHTML(NOTAVAILABLETEXT) || br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.ExtaBitCom.errors.TempUnavailable", "This file is temporary unavailable"));
            dllink = br.getRegex("<div id=\"download_filename\" class=\"df_archive\">3 g i.part1.rar</div>[\n\r\t ]+<a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://p\\d+\\.extabit\\.com/[a-z0-9]+/.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -10);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
