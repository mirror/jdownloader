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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
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
import jd.utils.locale.JDL;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloud.mail.ru" }, urls = { "http://clouddecrypted\\.mail\\.ru/\\d+|https?://[a-z0-9]+\\.datacloudmail\\.ru/weblink/(view|get)/a13a79fc6e6f/[^<>\"/]+/[^<>\"/]+" })
public class CloudMailRu extends PluginForHost {
    public CloudMailRu(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://cloud.mail.ru/");
    }

    private static final String  TYPE_FROM_DECRYPTER          = "http://clouddecrypted\\.mail\\.ru/\\d+";
    private static final String  TYPE_HOTLINK                 = "https?://[a-z0-9]+\\.datacloudmail\\.ru/weblink/(view|get)/[a-z0-9]+/[^<>\"/]+/[^<>\"/]+";
    private static final String  NOCHUNKS                     = "NOCHUNKS";
    private static final String  DOWNLOAD_ZIP                 = "DOWNLOAD_ZIP_2";
    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public String getAGBLink() {
        return "https://cloud.mail.ru/";
    }

    private static final String BUILD = jd.plugins.decrypter.CloudMailRuDecrypter.BUILD;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        prepBR();
        if (link.getDownloadURL().matches(TYPE_HOTLINK)) {
            URLConnectionAdapter con = null;
            final String dlink = getdllink(link, "free_directlink");
            try {
                con = br.openGetConnection(dlink);
                if (!con.getContentType().contains("html")) {
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)).trim());
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /** TODO: Remove this */
            /* Check if main-folder still exists */
            if (link.getBooleanProperty("noapi", false)) {
                br.getPage(getMainlink(link));
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                br.getPage("https://cloud.mail.ru/api/v2/folder?weblink=" + Encoding.urlEncode(getID(link)) + "&sort=%7B%22type%22%3A%22name%22%2C%22order%22%3A%22asc%22%7D&offset=0&limit=500&api=2&build=" + BUILD);
                if (br.containsHTML("\"status\":400")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            final String filename = link.getStringProperty("plain_name", null);
            final String filesize = link.getStringProperty("plain_size", null);
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(filename);
            link.setDownloadSize(Long.parseLong(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, boolean resume, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = getdllink(downloadLink, directlinkproperty);
        if (isCompleteFolder(downloadLink)) {
            resume = false;
            maxchunks = 1;
        }
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("plain_directlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(CloudMailRu.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(CloudMailRu.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info("ERROR_DOWNLOAD_INCOMPLETE --> Handling it");
                if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
                    downloadLink.setProperty(NOCHUNKS, Boolean.valueOf(false));
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
                }
                downloadLink.setProperty(NOCHUNKS, Boolean.valueOf(true));
                downloadLink.setChunksProgress(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "ERROR_DOWNLOAD_INCOMPLETE");
            }
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(CloudMailRu.NOCHUNKS, false) == false) {
                downloadLink.setProperty(CloudMailRu.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    private String getdllink(final DownloadLink dl, final String directlinkproperty) throws Exception {
        final String unique_id = dl.getStringProperty("unique_id", null);
        String dllink = checkDirectLink(dl, "plain_directlink");
        if (dllink == null) {
            if (dl.getDownloadURL().matches(TYPE_HOTLINK)) {
                dllink = dl.getDownloadURL();
            } else if (isCompleteFolder(dl)) {
                final String request_id = dl.getStringProperty("plain_request_id", null);
                if (request_id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.postPage("https://cloud.mail.ru/api/v2/zip", "weblink_list=%5B%22" + Encoding.urlEncode(request_id) + "%22%5D&name=" + Encoding.urlEncode(dl.getName()) + "&cp866=false&api=2&build=" + BUILD);
                dllink = PluginJSonUtils.getJsonValue(br, "body");
            } else if (dl.getBooleanProperty("noapi", false)) {
                br.getPage(getMainlink(dl));
                final String json = br.getRegex("(\\{\\s*\"tree\":.*?)\\);").getMatch(0);
                if (json == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                final LinkedHashMap<String, Object> folder = (LinkedHashMap<String, Object>) entries.get("folder");
                final ArrayList<Object> list = (ArrayList) folder.get("list");
                for (final Object o : list) {
                    final LinkedHashMap<String, Object> filemap = (LinkedHashMap<String, Object>) o;
                    final LinkedHashMap<String, Object> url = (LinkedHashMap<String, Object>) filemap.get("url");
                    final String get_url = (String) url.get("get");
                    if (Encoding.htmlDecode(get_url).contains(dl.getName())) {
                        if (get_url.startsWith("//")) {
                            dllink = Request.getLocation(get_url, br.getRequest());
                        } else {
                            dllink = get_url;
                        }
                        break;
                    }
                }
            } else {
                logger.info("Failed to use saved dllink, trying to generate new link");
                final String mainlink = getMainlink(dl);
                String dataserver = null;
                String pageid = null;
                String linkpart = new Regex(mainlink, "/public/([^/]+/[^/]+)").getMatch(0);
                if (linkpart == null || (unique_id != null && unique_id.contains("#"))) {
                    linkpart = unique_id;
                }
                this.br.getPage(mainlink);
                final String web_json = this.br.getRegex("window\\[\"__configObject[^<>\"]+\"\\] =(\\{.*?\\});<").getMatch(0);
                if (web_json != null) {
                    // using linkedhashmap here will result in exception
                    // java.lang.ClassCastException: java.util.HashMap cannot be cast to java.util.LinkedHashMap
                    // irc report - raztoki20160619
                    final HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(web_json);
                    dataserver = (String) JavaScriptEngineFactory.walkJson(entries, "dispatcher/weblink_get/{0}/url");
                    pageid = (String) JavaScriptEngineFactory.walkJson(entries, "params/x-page-id");
                    // final LinkedHashMap<String, Object> page_info = (LinkedHashMap<String, Object>) entries.get("");
                    // final ArrayList<Object> ressourcelist = (ArrayList) entries.get("");
                }
                if (pageid == null) {
                    pageid = this.br.getRegex("\"x-page-id\"[\n\r\t ]*?:[\n\r\t ]*?\"([^<>\"]*?)\"").getMatch(0);
                }
                if (pageid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.postPage("https://cloud.mail.ru/api/v2/tokens/download", "api=2&build=" + BUILD + "&x-page-id=" + pageid);
                final String token = PluginJSonUtils.getJsonValue(br, "token");
                if (token == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dataserver == null) {
                    /* Usually this should not be needed! */
                    br.getPage("/api/v2/dispatcher?api=2&build=" + BUILD + "&_=" + System.currentTimeMillis());
                    dataserver = br.getRegex("\"url\":\"(https?://[a-z0-9]+\\.datacloudmail\\.ru/weblink/)view/\"").getMatch(0);
                }
                if (dataserver != null) {
                    /* TODO: Check for encoding problems here! */
                    String encoded_unique_id = Encoding.urlEncode(unique_id);
                    /* We need the "/" so let's encode them back. */
                    encoded_unique_id = encoded_unique_id.replace("%2F", "/");
                    encoded_unique_id = encoded_unique_id.replace("+", "%20");
                    dllink = dataserver + "/" + encoded_unique_id + "?key=" + token;
                } else {
                    logger.warning("Failed to find dataserver for finallink");
                }
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    private String getID(final DownloadLink dl) {
        return dl.getStringProperty("plain_request_id", null);
    }

    private String getMainlink(final DownloadLink dl) {
        return dl.getStringProperty("mainlink", null);
    }

    private boolean isCompleteFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("complete_folder", false);
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
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

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                final String mail_domain = account.getUser().split("@")[1];
                final String postData = "page=https%3A%2F%2Fcloud.mail.ru%2F&FailPage=&Domain=" + mail_domain + "&Login=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()) + "&new_auth_form=1&saveauth=1";
                br.postPage("https://auth.mail.ru/cgi-bin/auth?lang=ru_RU&from=authpopup", postData);
                if (br.containsHTML("\\&fail=1") || br.getCookie("http://auth.mail.ru/", "ssdc") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage("https://cloud.mail.ru/?from=authpopup");
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        account.setType(AccountType.FREE);
        ai.setStatus("Free Account");
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (AccountType.FREE.equals(account.getType())) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
            return;
        }
        String dllink = this.checkDirectLink(link, "premium_directlink");
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CloudMailRu.DOWNLOAD_ZIP, JDL.L("plugins.hoster.CloudMailRu.DownloadZip", "Download .zip file of all files in the folder?")).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}