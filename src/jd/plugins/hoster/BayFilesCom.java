//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bayfiles.com" }, urls = { "https?://(?:www\\.)?bayfiles\\.(?:com|net)/[a-z0-9]+(?:/.+)?" })
public class BayFilesCom extends PluginForHost {
    public BayFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://bayfiles.com/terms";
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                  = true;
    private final int            FREE_MAXCHUNKS               = 0;
    private final int            FREE_MAXDOWNLOADS            = 20;
    private final boolean        ACCOUNT_FREE_RESUME          = true;
    private final int            ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int            ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final boolean use_filecheck_API            = true;
    private static final String  API_BASE                     = "https://bayfiles.com/api/v2";

    @Override
    public String getLinkID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/([a-z0-9]+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        String filename;
        long filesize = 0;
        if (use_filecheck_API) {
            /* https://bayfiles.com/docs/api */
            br.getPage(API_BASE + "/file/" + getLinkID(link) + "/info");
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/file/metadata");
            filename = (String) entries.get("name");
            filesize = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "size/bytes"), 0);
        } else {
            br.getPage(link.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("").getMatch(0);
            final String filesizeStr = br.getRegex("").getMatch(0);
            if (filesizeStr != null) {
                filesize = SizeFormatter.getSize(filesizeStr);
            }
        }
        if (StringUtils.isEmpty(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(filesize);
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
            if (use_filecheck_API) {
                br.getPage(downloadLink.getDownloadURL());
            }
            dllink = br.getRegex("id=\"download\\-url\" class=\"btn btn\\-primary btn\\-block\" href=\"(http[^<>\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("\"(https?://cdn\\-\\d+\\.bayfiles\\.com/[^<>\"]+)\"").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
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

    // private static Object LOCK = new Object();
    //
    // private void login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // br.setFollowRedirects(true);
    // br.setCookiesExclusive(true);
    // final Cookies cookies = account.loadCookies("");
    // if (cookies != null && !force) {
    // this.br.setCookies(this.getHost(), cookies);
    // return;
    // }
    // br.getPage("");
    // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
    // if (br.getCookie(this.getHost(), "") == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu
    // bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es
    // und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username
    // and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // account.saveCookies(this.br.getCookies(this.getHost()), "");
    // } catch (final PluginException e) {
    // account.clearCookies("");
    // throw e;
    // }
    // }
    // }
    //
    // @SuppressWarnings("deprecation")
    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws Exception {
    // final AccountInfo ai = new AccountInfo();
    // try {
    // login(account, true);
    // } catch (PluginException e) {
    // account.setValid(false);
    // throw e;
    // }
    // String space = br.getRegex("").getMatch(0);
    // if (space != null) {
    // ai.setUsedSpace(space.trim());
    // }
    // ai.setUnlimitedTraffic();
    // if (account.getBooleanProperty("free", false)) {
    // account.setType(AccountType.FREE);
    // /* free accounts can still have captcha */
    // account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
    // account.setConcurrentUsePossible(false);
    // ai.setStatus("Registered (free) user");
    // } else {
    // final String expire = br.getRegex("").getMatch(0);
    // if (expire == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account
    // Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort
    // Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick
    // help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change
    // it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // } else {
    // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
    // }
    // account.setType(AccountType.PREMIUM);
    // account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
    // account.setConcurrentUsePossible(true);
    // ai.setStatus("Premium account");
    // }
    // account.setValid(true);
    // return ai;
    // }
    //
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // login(account, false);
    // br.getPage(link.getDownloadURL());
    // if (account.getType() == AccountType.FREE) {
    // doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    // } else {
    // String dllink = this.checkDirectLink(link, "premium_directlink");
    // if (dllink == null) {
    // dllink = br.getRegex("").getMatch(0);
    // if (StringUtils.isEmpty(dllink)) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
    // if (dl.getConnection().getContentType().contains("html")) {
    // if (dl.getConnection().getResponseCode() == 403) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
    // } else if (dl.getConnection().getResponseCode() == 404) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
    // }
    // logger.warning("The final dllink seems not to be a file!");
    // br.followConnection();
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // link.setProperty("premium_directlink", dllink);
    // dl.startDownload();
    // }
    // }
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