//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.VKontakteRu.JSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data.hu" }, urls = { "http://[\\w\\.]*?data.hu/get/\\d+/[^<>\"/%]+" }, flags = { 2 })
public class DataHu extends PluginForHost {

    private static final String NICE_HOST         = "offcloud.com";
    private static final String NICE_HOSTproperty = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private int                 statuscode        = 0;

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://data.hu/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://data.hu/adatvedelem.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace(".html", ""));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    private void prepBR() {
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    /** Using API: http://data.hu/api.php */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBR();
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            String checkurl = null;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once (limit = 50) */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    checkurl = "http://data.hu/get/" + getFID(dl) + "/";
                    sb.append(checkurl);
                    sb.append("%2C");
                }
                this.getAPISafe("http://data.hu/api.php?act=check_download_links&links=" + sb.toString());
                br.getRequest().setHtmlCode(JSonUtils.unescape(br.toString()));
                for (final DownloadLink dllink : links) {
                    checkurl = "http://data.hu/get/" + getFID(dllink) + "/";
                    final String thisjson = br.getRegex("\"" + checkurl + "\":\\{(.*?)\\}").getMatch(0);
                    if (thisjson == null || !"online".equals(this.getJson(thisjson, "status"))) {
                        dllink.setAvailable(false);
                    } else {
                        final String name = this.getJson(thisjson, "filename");
                        final String size = this.getJson(thisjson, "filesize");
                        final String md5 = getJson(thisjson, "md5");
                        final String sha1 = getJson(thisjson, "sha1");
                        /* Names via API are good --> Use as final filenames */
                        dllink.setFinalFileName(name);
                        dllink.setDownloadSize(SizeFormatter.getSize(size));
                        dllink.setMD5Hash(md5);
                        dllink.setSha1Hash(sha1);
                        dllink.setAvailable(true);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (!"premium".equals(getJson("type"))) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String expiredate = getJson("expiration_date");
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "yyyy-MM-dd mm:HH:ss", Locale.ENGLISH));
        ai.setStatus("Premium account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        handleSiteErrors();
        final String link = br.getRegex(Pattern.compile("(?:\"|\\')(http://ddl\\d+\\.data\\.hu/get/\\d+/\\d+/.*?)(?:\"|\\')", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            /*
             * Wait a minute for respons 503 because JD tried to start too many downloads in a short time
             */
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.datahu.toomanysimultandownloads", "Too many simultan downloads, please wait some time!"), 60 * 1000l);
            }
            br.followConnection();
            handleSiteErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        getAPISafe("http://data.hu/api.php?act=get_direct_link&link=" + JSonUtils.escape(downloadLink.getDownloadURL()) + "&username=" + JSonUtils.escape(account.getUser()) + "&password=" + JSonUtils.escape(account.getPass()));
        final String link = getJson("direct_link");
        if (link == null) {
            /* Should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        /* Max 4 connections per downloadserver so we prefer a total of 4 simultan downloads to avoid 503 server errors. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            /*
             * Wait a minute for respons 503 because JD tried to start too many downloads in a short time
             */
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.datahu.toomanysimultandownloads", "Too many simultan downloads, please wait some time!"), 60 * 1000l);
            }
            br.followConnection();
            handleSiteErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleSiteErrors() throws PluginException {
        if (br.containsHTML("Az adott fájl nem létezik\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Az adott fájl már nem elérhető\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBR();
        getAPISafe("http://data.hu/api.php?act=check_login_data&username=" + JSonUtils.escape(account.getUser()) + "&password=" + JSonUtils.escape(account.getPass()));
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "data.hu/get/(\\d+)").getMatch(0);
    }

    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        br.postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    /**
     * 0 = everything ok, 1-99 = "error"-errors, 100-199 = "not_available"-errors, 200-299 = Other (html) [download] errors, sometimes mixed
     * with the API errors.
     */
    private void updatestatuscode() {
        String error = getJson("error");
        final String msg = getJson("msg");
        if (error != null && msg != null) {
            if (msg.equals("wrong username or password")) {
                statuscode = 1;
            } else if (msg.equals("no premium")) {
                statuscode = 2;
            } else if (msg.equals("wrong link")) {
                statuscode = 3;
            } else if (msg.equals("max 50 link can check")) {
                statuscode = 4;
            } else if (msg.equals("no act param defined")) {
                statuscode = 5;
            } else {
                statuscode = 666;
            }
        } else {
            /* No way to tell that something unpredictable happened here --> status should be fine. */
            statuscode = 0;
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* Wrong username or password -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2:
                /* Account = Free account tried a download in premium mode --> (Should never happen anyways) Permanently disable */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.";
                } else {
                    statusMessage = "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3:
                /* User tried to download an invalid link via account --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Downloadlink";
                } else {
                    statusMessage = "\r\nInvalid downloadlink";
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, statusMessage);
            case 4:
                /* We tried to check more than 50 links at the same time --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nKann maximal 50 Links gleichzeitig überprüfen";
                } else {
                    statusMessage = "\r\nMax simultaneous checkable links number equals 50";
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, statusMessage);
            case 5:
                /* 'no act param defined' --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nFATALER API Fehler";
                } else {
                    statusMessage = "\r\nFATAL API error";
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, statusMessage);
            case 666:
                /* SHTF --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUnbekannter Fehler";
                } else {
                    statusMessage = "\r\nUnknown error";
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, statusMessage);
            default:
                /* Completely Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}