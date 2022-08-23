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

import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.IO;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.dcm.Dcm;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "digitalcomicmuseum.com" }, urls = { "https?://(?:www\\.)?digitalcomicmuseum\\.com/.*\\?dl?id=\\d+" })
public class DigitalcomicmuseumCom extends PluginForHost {
    public DigitalcomicmuseumCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://digitalcomicmuseum.com/forum/index.php?action=register");
    }

    @Override
    public String getAGBLink() {
        return "http://digitalcomicmuseum.com/support/index.php";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME               = false;
    private static final int     FREE_MAXCHUNKS            = 1;
    private static final int     FREE_MAXDOWNLOADS         = -1;
    private static final boolean ACCOUNT_FREE_RESUME       = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS = -1;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        try {
            final UrlQuery query = UrlQuery.parse(link.getPluginPatternMatcher());
            String fid = query.get("did");
            if (fid == null) {
                fid = query.get("dlid");
            }
            return fid;
        } catch (final Throwable e) {
        }
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            /* Set fallback name */
            link.setName(fid);
        }
        this.setBrowserExclusive();
        br.getPage("https://" + this.getHost() + "/index.php?dlid=" + fid);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("/images/error\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<a href='index\\.php\\?dlid=\\d+'>([^<>]*?)</a>").getMatch(0);
        final String filesize = br.getRegex(">Filesize:</td>[\t\n\r ]+<td colspan='2'>([^<>\"]*?)</td>").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename.trim());
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new AccountRequiredException();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("http://digitalcomicmuseum.com/forum/index.php?action=login2");
                final Form login = br.getFormbyActionRegex(".*action=login2.*");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // login has hashed values, derived from javascript
                final String charset = br.getRegex("var smf_charset\\s*=\\s*\"(.*?)\";").getMatch(0);
                final String cur_session_id = login.getRegex("onsubmit=\"hashLoginPassword\\(this,\\s*'([a-f0-9]{32})'").getMatch(0);
                final String myEn = br.getRegex("myElement\\.name\\s*=\\s'(.*?)';").getMatch(0);
                final String myEv = br.getRegex("myElement\\.value\\s*=\\s'(.*?)';").getMatch(0);
                String result = null;
                try {
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    engine.eval(IO.readURLToString(Dcm.class.getResource("dcm.js")));
                    engine.eval(IO.readURLToString(Dcm.class.getResource("sha1.js")));
                    engine.put("user", account.getUser());
                    engine.put("pass", account.getPass());
                    engine.put("smf_charset", charset);
                    engine.put("cur_session_id", cur_session_id);
                    engine.eval("var res = hex_sha1(hex_sha1(user.php_to8bit().php_strtolower() + pass.php_to8bit()) + cur_session_id);");
                    result = engine.get("res").toString();
                } catch (final Exception e) {
                    // e.printStackTrace();
                    throw e;
                }
                login.put(myEn, Encoding.urlEncode(myEv));
                login.put("user", Encoding.urlEncode(account.getUser()));
                login.put("passwrd", "");
                login.put("hash_passwrd", result);
                login.put("cookielength", "-1");
                br.submitForm(login);
                if (br.getCookie(this.getHost(), "rwd_password", Cookies.NOTDELETEDPATTERN) == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = this.checkDirectLink(link, "premium_directlink");
        if (dllink == null) {
            dllink = br.getRegex("'(index\\.php\\?ACT=dl[^<>\"']+)").getMatch(0);
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "/" + Encoding.htmlOnlyDecode(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}