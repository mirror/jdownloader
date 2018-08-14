//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ishare.iask.sina.com.cn" }, urls = { "https?://(?:www\\.)?ishare\\.iask\\.sina\\.com\\.cn/f/\\d+\\.html" })
public class IshareIaskSinaComCn extends PluginForHost {
    public IshareIaskSinaComCn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://ishare.iask.sina.com.cn/");
    }

    @Override
    public String getAGBLink() {
        return "http://iask.com/help/mzsm.html";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME                  = false;
    private final int     FREE_MAXCHUNKS               = 1;
    private final int     FREE_MAXDOWNLOADS            = 3;
    private final boolean ACCOUNT_FREE_RESUME          = true;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title>共享资料</title>|<br>5秒钟后跳转到首页</div>|近期共享资料正在配合有关部门进行淫秽、色情、|无法查看！<") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>(.*?) - 免费高速下载 - 共享资料</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("name=\"file_title\" id=\"file_des\" value=\"(.*?)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("name=\"hiddenfile_title\" id=\"hiddenfile_title\" value=\"(.*?)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<h1 class=\"f14\" style=\"display:inline;\">(.*?)</h1></div>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<input type=\"hidden\" name=\"title\" value=\"(.*?)\">").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<h2 title=\"([^<>\"]+)\">").getMatch(0);
        }
        String filesize = br.getRegex("class=\"f10\">0分<br>(.*?)</span></td>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            if (br.containsHTML("<a class=\"btn-download btn-m-not\"><i class=\"icon-iShare\"></i>下载</a>")) {
                throw new AccountRequiredException();
            }
            br.setFollowRedirects(false);
            this.br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://ishare.iask.sina.com.cn/f/download/" + new Regex(downloadLink.getDownloadURL(), "/(\\d+)\\.html").getMatch(0));
            final String code = PluginJSonUtils.getJsonValue(this.br, "code");
            if ("102".equals(code)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            dllink = PluginJSonUtils.getJsonValue(this.br, "data");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                final String username_b64 = Encoding.urlEncode(Encoding.Base64Encode(Encoding.urlEncode(account.getUser())));
                br.getPage("http://" + this.getHost() + "/login?loginType=SINA&location=http://ishare.iask.sina.com.cn/");
                final String pagerefer = Encoding.urlEncode(this.br.getURL());
                br.getPage("https://login.sina.com.cn/sso/prelogin.php?entry=openapi&callback=sinaSSOController.preloginCallBack&su=" + username_b64 + "&rsakt=mod&checkpin=1&client=ssologin.js(v1.4.18)&_=1483727712973");
                final String is_openlock = PluginJSonUtils.getJsonValue(this.br, "is_openlock");
                String pcid = PluginJSonUtils.getJsonValue(this.br, "pcid");
                final String servertime = PluginJSonUtils.getJsonValue(this.br, "servertime");
                final String rsakv = PluginJSonUtils.getJsonValue(this.br, "rsakv");
                final String nonce = PluginJSonUtils.getJsonValue(this.br, "nonce");
                final String pubkey = PluginJSonUtils.getJsonValue(this.br, "pubkey");
                if (!"0".equals(is_openlock)) {
                    /* Wrong username */
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (pcid == null || pcid.equals("") || servertime == null || servertime.equals("") || rsakv == null || rsakv.equals("") || nonce == null || nonce.equals("") || pubkey == null || pubkey.equals("")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                pcid = Encoding.urlEncode(pcid);
                final String captchaurl = "https://login.sina.com.cn/cgi/pin.php?r=" + rsakv + "&s=0&p=" + Encoding.urlEncode(pcid);
                final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), "http://" + this.getHost(), true);
                final String c = getCaptchaCode(captchaurl, dummyLink);
                String postdata = "entry=openapi&gateway=1&from=&savestate=0&useticket=1&pagerefer=" + pagerefer + "&wsseretry=servertime_error&ct=1800&s=1&vsnf=1&vsnval=&door=fBubv&appkey=1t4elW&pcid=" + pcid + "&su=" + username_b64 + "&service=miniblog&servertime=" + servertime + "&nonce=" + Encoding.urlEncode(nonce) + "&pwencode=rsa2&rsakv=" + Encoding.urlEncode(rsakv) + "&sp=" + pubkey.toLowerCase() + "&sr=1920*1080&encoding=UTF-8&cdult=2&domain=weibo.com&prelt=370&returntype=TEXT";
                this.br.postPage("https://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)&_=" + System.currentTimeMillis() + "&openapilogin=qrcode", postdata);
                this.br.postPage("https://api.weibo.com/oauth2/authorize", "");
                br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIn()) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        /* 2017-01-06: At the moment we only support free accounts. */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    private boolean isLoggedIn() throws IOException {
        this.br.getPage("http://" + this.getHost() + "/user/checkLogin");
        final String success = PluginJSonUtils.getJsonValue(this.br, "succ");
        return success != null && success.equals("Y") ? true : false;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}