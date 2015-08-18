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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "openload.co", "openload.io" }, urls = { "https?://(?:www\\.)?openload\\.(?:io|co)/(?:f|embed)/[A-Za-z0-9_\\-]+", "/null/void" }, flags = { 2, 0 })
public class OpenLoadIo extends antiDDoSForHost {

    public OpenLoadIo(PluginWrapper wrapper) {
        super(wrapper);
        /* Server doesn't like it when we open too many connections in a short time */
        this.setStartIntervall(2000);
        this.enablePremium("https://openload.co/register");
    }

    @Override
    public String rewriteHost(final String host) {
        if ("openload.io".equals(host)) {
            return "openload.co";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "https://openload.co/tos";
    }

    /* Constants */
    /* Status 20.06.15: free API seems to be broken, returns response 500 when usually it should return final downloadurl */
    private static final boolean enable_api_free = false;
    private static final String  api_base        = "https://api.openload.co/1";

    /* Connection stuff */
    private static final boolean          FREE_RESUME       = true;
    private static final int              FREE_MAXCHUNKS    = 0;
    private static final int              FREE_MAXDOWNLOADS = 20;
    private int                           api_responsecode  = 0;
    private LinkedHashMap<String, Object> api_data          = null;

    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    /* don't touch the following! */
    private static AtomicInteger maxPrem = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* Force https & correct embedded urls */
        link.setUrlDownload("https://openload.co/f" + link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/")));
    }

    /*
     * Using API: http://docs.ol1.apiary.io/
     *
     * TODO: Check if we can use the mass linkchecker with this API. Add account support, get an API key and use that as well.
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        final String fid = getFID(link);
        if (link.getName() == null) {
            link.setName(fid);
        }
        this.setBrowserExclusive();
        getPage(api_base + "/file/info?file=" + fid);
        api_data = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        api_data = (LinkedHashMap<String, Object>) api_data.get("result");
        api_data = (LinkedHashMap<String, Object>) api_data.get(fid);
        api_responsecode = ((Number) api_data.get("status")).intValue();
        if (api_responsecode == 403) {
            link.getLinkStatus().setStatusText("Private files can only be downloaded by their owner/uploader");
            return AvailableStatus.TRUE;
        }
        if (api_responsecode != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = (String) api_data.get("name");
        final String filesize = (String) api_data.get("size");
        final String sha1 = (String) api_data.get("sha1");
        if (filename == null || filesize == null || sha1 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Trust API */
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        link.setSha1Hash(sha1);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (api_responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private files can only be downloaded by their owner/uploader");
        }
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String fid = getFID(downloadLink);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        dllink = null;
        if (dllink == null) {
            String ticket;
            String waittime;
            if (enable_api_free) {
                getPage(api_base + "/file/dlticket?file=" + fid);
                api_data = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                api_data = (LinkedHashMap<String, Object>) api_data.get("result");
                ticket = (String) api_data.get("ticket");
                waittime = Integer.toString(((Number) api_data.get("wait_time")).intValue());
                if (((Boolean) api_data.get("captcha_url")).booleanValue()) {
                    logger.warning("Failed - captcha handling does not yet exist!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (ticket == null) {
                    logger.warning("Ticket is null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                this.sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
                getPage(api_base + "/file/dl?file=" + fid + "&ticket=" + Encoding.urlEncode(ticket) + "&captcha_response=null");
                api_data = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                api_data = (LinkedHashMap<String, Object>) api_data.get("result");
                dllink = (String) api_data.get("url");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                br.addAllowedResponseCodes(500);
                br.setFollowRedirects(true);
                getPage(downloadLink.getDownloadURL());
                if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String rwlink = br.getRegex("var token = \"([^<>\"]*?)\";").getMatch(0);
                if (rwlink != null) {
                    try {
                        getPage(br.cloneBrowser(), "https://openload.co/reward/" + rwlink + "?adblock=0");
                    } catch (final Throwable e) {
                        /* Don't fail here! */
                    }
                }
                dllink = br.getRegex("\"(https?://[a-z0-9\\.\\-]+\\.dl\\.openload\\.io/[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                int seconds = 5;
                String wait = br.getRegex("id=\"secondsleft\">(\\d+)</span>").getMatch(0);
                if (wait != null) {
                    seconds = Integer.parseInt(wait);
                }
                this.sleep(seconds * 1001l, downloadLink);
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

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return dl.getDownloadURL().substring(dl.getDownloadURL().lastIndexOf("/") + 1);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://openload.co";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean followsRedirect = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                getPage("https://openload.co/login");
                final String csrftoken = br.getRegex("name=\"csrf\\-token\" content=\"([^<>\"]*?)\"").getMatch(0);
                if (csrftoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                postPage("/login", "_csrf=" + Encoding.urlEncode(csrftoken) + "&LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1");
                if (!this.br.containsHTML("href=\"/logout\"")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(this.getHost()));
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(followsRedirect);
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
        ai.setUnlimitedTraffic();
        /* At the moment we only support free accounts */
        if (account.getBooleanProperty("free", false) || true) {
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Free Account");
        } else {
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        if (account.getBooleanProperty("free", false)) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                getPage(link.getDownloadURL());
                dllink = br.getRegex("").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}