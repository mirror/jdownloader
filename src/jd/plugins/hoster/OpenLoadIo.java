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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "openload.io" }, urls = { "https?://(www\\.)?openload\\.io/(f|embed)/[A-Za-z0-9_\\-]+" }, flags = { 0 })
public class OpenLoadIo extends PluginForHost {

    public OpenLoadIo(PluginWrapper wrapper) {
        super(wrapper);
        /* Server doesn't like it when we open too many connections in a short time */
        this.setStartIntervall(2000);
        // this.enablePremium("https://openload.io/register");
    }

    @Override
    public String getAGBLink() {
        return "https://openload.io/tos";
    }

    /* Constants */
    private static final boolean          enable_api_free   = true;
    private static final String           api_base          = "https://api.openload.io/1";

    /* Connection stuff */
    private static final boolean          FREE_RESUME       = true;
    private static final int              FREE_MAXCHUNKS    = 0;
    private static final int              FREE_MAXDOWNLOADS = 20;
    private int                           api_responsecode  = 0;
    private LinkedHashMap<String, Object> api_data          = null;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* Force https & correct embedded urls */
        link.setUrlDownload("https://openload.io/f" + link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/")));
    }

    /*
     * Using API: http://docs.ol1.apiary.io/
     * 
     * TODO: Check if we can use the mass linkchecker with this API. Add account support, get an API key and use that as well.
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = getFID(link);
        link.setName(fid);
        this.setBrowserExclusive();
        br.getPage(api_base + "/file/info?file=" + fid);
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
                br.getPage(api_base + "/file/dlticket?file=" + fid);
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
                br.getPage(api_base + "/file/dl?file=" + fid + "&ticket=" + Encoding.urlEncode(ticket) + "&captcha_response=null");
                api_data = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                api_data = (LinkedHashMap<String, Object>) api_data.get("result");
                dllink = (String) api_data.get("url");
            } else {
                try {
                    br.getPage(downloadLink.getDownloadURL());
                } catch (final BrowserException e) {
                    if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 500) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    throw e;
                }
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String rwlink = br.getRegex("\"(/reward/[a-z0-9]+\\?adblock=)\"").getMatch(0);
                if (rwlink != null) {
                    try {
                        br.cloneBrowser().getPage(rwlink + "0");
                    } catch (final Throwable e) {
                        /* Don't fail here! */
                    }
                }
                dllink = br.getRegex("\"(https?://[a-z0-9\\.\\-]+\\.dl\\.openload\\.io/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
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
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
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

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return dl.getDownloadURL().substring(dl.getDownloadURL().lastIndexOf("/") + 1);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    // private static final String MAINPAGE = "http://openload.io";
    // private static Object LOCK = new Object();
    //
    // @SuppressWarnings("unchecked")
    // private void login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // // Load cookies
    // br.setCookiesExclusive(true);
    // final Object ret = account.getProperty("cookies", null);
    // boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name",
    // Encoding.urlEncode(account.getUser())));
    // if (acmatch) {
    // acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
    // }
    // if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
    // final HashMap<String, String> cookies = (HashMap<String, String>) ret;
    // if (account.isValid()) {
    // for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
    // final String key = cookieEntry.getKey();
    // final String value = cookieEntry.getValue();
    // br.setCookie(MAINPAGE, key, value);
    // }
    // return;
    // }
    // }
    // br.setFollowRedirects(false);
    // br.getPage("");
    // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
    // if (br.getCookie(MAINPAGE, "") == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // // Save cookies
    // final HashMap<String, String> cookies = new HashMap<String, String>();
    // final Cookies add = br.getCookies(MAINPAGE);
    // for (final Cookie c : add.getCookies()) {
    // cookies.put(c.getKey(), c.getValue());
    // }
    // account.setProperty("name", Encoding.urlEncode(account.getUser()));
    // account.setProperty("pass", Encoding.urlEncode(account.getPass()));
    // account.setProperty("cookies", cookies);
    // } catch (final PluginException e) {
    // account.setProperty("cookies", Property.NULL);
    // throw e;
    // }
    // }
    // }
    //
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
    // maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
    // try {
    // account.setType(AccountType.FREE);
    // /* free accounts can still have captcha */
    // account.setMaxSimultanDownloads(maxPrem.get());
    // account.setConcurrentUsePossible(false);
    // } catch (final Throwable e) {
    // /* not available in old Stable 0.9.581 */
    // }
    // ai.setStatus("Registered (free) user");
    // } else {
    // final String expire = br.getRegex("").getMatch(0);
    // if (expire == null) {
    // final String lang = System.getProperty("user.language");
    // if ("de".equalsIgnoreCase(lang)) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // } else {
    // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
    // }
    // maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
    // try {
    // account.setType(AccountType.PREMIUM);
    // account.setMaxSimultanDownloads(maxPrem.get());
    // account.setConcurrentUsePossible(true);
    // } catch (final Throwable e) {
    // /* not available in old Stable 0.9.581 */
    // }
    // ai.setStatus("Premium Account");
    // }
    // account.setValid(true);
    // return ai;
    // }
    //
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // login(account, false);
    // br.setFollowRedirects(false);
    // br.getPage(link.getDownloadURL());
    // if (account.getBooleanProperty("free", false)) {
    // doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    // } else {
    // String dllink = this.checkDirectLink(link, "premium_directlink");
    // if (dllink == null) {
    // dllink = br.getRegex("").getMatch(0);
    // if (dllink == null) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
    // if (dl.getConnection().getContentType().contains("html")) {
    // logger.warning("The final dllink seems not to be a file!");
    // br.followConnection();
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // link.setProperty("premium_directlink", dllink);
    // dl.startDownload();
    // }
    // }
    //
    // @Override
    // public int getMaxSimultanPremiumDownloadNum() {
    // /* workaround for free/premium issue on stable 09581 */
    // return maxPrem.get();
    // }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}