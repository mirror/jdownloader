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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.appwork.utils.formatter.TimeFormatter;
import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tenlua.vn" }, urls = { "https?://(?:www\\.)?tenlua\\.vn/[^/]+/[a-f0-9]{18}/[^<>\"]+" })
public class TenluaVn extends PluginForHost {

    public TenluaVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://tenlua.vn/#payment");
    }

    @Override
    public String getAGBLink() {
        return "http://tenlua.vn/#terms";
    }

    private Browser ajax    = null;
    private long    req_num = 0;
    private String  sid     = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String fid = get_fid(link);
        /* No fid --> We have no downloadable link */
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!link.isNameSet()) {
            link.setName(fid);
        }
        br.setRequest(br.createGetRequest(link.getDownloadURL()));
        postPageRaw("//api2.tenlua.vn/", "[{\"a\":\"filemanager_builddownload_getinfo\",\"n\":\"" + fid + "\",\"r\":0." + System.currentTimeMillis() + "}]");
        if ("none".equals(PluginJSonUtils.getJson(ajax, "type"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = PluginJSonUtils.getJson(ajax, "n");
        final String filesize = PluginJSonUtils.getJson(ajax, "real_size");
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(Long.parseLong(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private String get_fid(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/([a-f0-9]{18})/").getMatch(0);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            dllink = PluginJSonUtils.getJson(ajax, "url");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            postPageRaw("//api2.tenlua.vn/", "[{\"a\":\"filemanager_builddownload_getwaittime\"}]");
            final String waittime = PluginJSonUtils.getJson(ajax, "t");
            int wait = 5;
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            this.sleep(wait * 1001l, downloadLink);
        }

        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("Finallink does not lead to a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private static final String MAINPAGE = "http://tenlua.vn";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
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
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                postPageRaw("https://api2.tenlua.vn/", "[{\"a\":\"user_login\",\"user\":\"" + PluginJSonUtils.escape(account.getUser()) + "\",\"password\":\"" + PluginJSonUtils.escape(account.getPass()) + "\",\"permanent\":true}]");
                if ("-9".equals(ajax.toString())) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                sid = ajax.getRegex("\\[\"([^<>\"/]*?)\"\\]").getMatch(0);
                if (sid == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br = ajax.cloneBrowser();
                postPageRaw("/", "[{\"a\":\"user_info\",\"r\":0." + System.currentTimeMillis() + "}]");
                /* So far, only free accounts are supported */
                final String type = ajax.getRegex("\"utype\":\"(\\d+)\"").getMatch(0);
                if (type == null || !(type.equals("2") || type.equals("3"))) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = ajax.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        String accstatus;
        final String acctype = PluginJSonUtils.getJson(ajax, "utype");
        if (acctype.equals("1")) {
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
            accstatus = "Free Account";
        } else if (acctype.equals("2")) {
            account.setProperty("free", false);
            accstatus = "Unknown / unsupported account type";
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (acctype.equals("3")) {
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.PREMIUM);
            final String expiredate = PluginJSonUtils.getJson(br, "endGold");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "dd-MM-yyyy", Locale.ENGLISH));
            accstatus = "Gold Account";
        } else {
            accstatus = "Unknown / unsupported account type";
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        account.setValid(true);
        ai.setStatus(accstatus);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* Always do a full login to get the sid value */
        login(account, true);
        br.setFollowRedirects(false);
        if (account.getType() == AccountType.FREE) {
            postPageRaw("//api2.tenlua.vn/", "[{\"a\":\"filemanager_builddownload_getinfo\",\"n\":\"" + get_fid(link) + "\",\"r\":0." + System.currentTimeMillis() + "}]");
            doFree(link);
        } else {
            String dllink = checkDirectLink(link, "account_premium_directlink");
            if (dllink == null) {
                // br.getHeaders().put("Referer", getDownloadlink(link));
                postPageRaw("//api2.tenlua.vn/", "[{\"a\":\"filemanager_builddownload_getinfo\",\"n\":\"" + get_fid(link) + "\",\"r\":0." + System.currentTimeMillis() + "}]");
                dllink = PluginJSonUtils.getJson(ajax, "dlink");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("Finallink does not lead to a file...");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("account_premium_directlink", dllink);
            dl.startDownload();
        }
    }

    private void postPageRaw(final String url, final String postData) throws IOException {
        if (req_num == 0) {
            req_num = (long) Math.ceil(Math.random() * 1000000000);
        } else {
            req_num++;
        }
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("TENLUA-Chrome-Antileak", "/?id=" + req_num);
        ajax.postPageRaw(url, postData);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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