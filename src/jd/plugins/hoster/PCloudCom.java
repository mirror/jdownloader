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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcloud.com" }, urls = { "http://pclouddecrypted\\.com/\\d+" })
public class PCloudCom extends PluginForHost {

    @SuppressWarnings("deprecation")
    public PCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://my.pcloud.com/#page=register");
    }

    @Override
    public String getAGBLink() {
        return "https://my.pcloud.com/#page=policies&tab=terms-of-service";
    }

    /* Linktypes */
    private static final String  TYPE_OLD                                        = "https?://(www\\.)?(my\\.pcloud\\.com/#page=publink\\&code=|pc\\.cd/)[A-Za-z0-9]+";

    private static final String  MAINPAGE                                        = "http://pcloud.com";
    private static final String  NICE_HOST                                       = "pcloud.com";
    private static final String  NICE_HOSTproperty                               = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String  NOCHUNKS                                        = NICE_HOSTproperty + "NOCHUNKS";

    /* Plugin Settings */
    private static final String  DOWNLOAD_ZIP                                    = "DOWNLOAD_ZIP_2";
    private static final String  MOVE_FILES_TO_ACCOUNT                           = "MOVE_FILES_TO_ACCOUNT";
    private static final String  DELETE_FILE_AFTER_DOWNLOADLINK_CREATION         = "DELETE_FILE_AFTER_DOWNLOADLINK_CREATION";
    private static final String  DELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION = "DELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION";
    private static final String  EMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION         = "EMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION";

    /* Errorcodes */
    private static final long    ERROR_OKAY                                      = 0;
    private static final long    ERROR_PREMIUMONLY                               = 7005;

    /* Connection stuff */
    private static final boolean FREE_RESUME                                     = true;
    private static final int     FREE_MAXCHUNKS                                  = 0;
    private static final int     FREE_MAXDOWNLOADS                               = 20;
    private static final boolean ACCOUNT_FREE_RESUME                             = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS                          = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS                       = 20;

    private int                  statuscode                                      = 0;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                                         = new AtomicInteger(1);
    private static Object        LOCK                                            = new Object();
    private String               account_auth                                    = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String code = getCODE(link);
        final String fileid = getFID(link);
        /* Links before big change */
        if (link.getDownloadURL().matches(TYPE_OLD)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        prepBR();
        if (isCompleteFolder(link)) {
            br.getPage("http://api.pcloud.com/showpublink?code=" + code);
            this.updatestatuscode();
            if (this.statuscode == 7002) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            br.getPage("https://api.pcloud.com/getpublinkdownload?code=" + code + "&forcedownload=1&fileid=" + fileid);
            this.updatestatuscode();
            if (this.statuscode == 7002) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String filename = link.getStringProperty("plain_name", null);
        final String filesize = link.getStringProperty("plain_size", null);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (this.statuscode == ERROR_PREMIUMONLY) {
            link.getLinkStatus().setStatusText("Only downloadable for registered/premium users");
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (this.statuscode == ERROR_PREMIUMONLY) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered/premium users");
        }
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink link) throws Exception, PluginException {
        this.handleAPIErrors(this.br);
        final String dllink = getdllink(link);
        boolean resume = FREE_RESUME;
        int maxchunks = FREE_MAXCHUNKS;
        if (isCompleteFolder(link)) {
            resume = false;
            maxchunks = 1;
        } else if (link.getBooleanProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, false)) {
            maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, false) == false) {
                    link.setProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            e.printStackTrace();
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (link.getBooleanProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, false) == false) {
                link.setProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private String getdllink(final DownloadLink dl) throws PluginException {
        String dllink = null;
        if (isCompleteFolder(dl)) {
            /* Select all IDs of the folder to download all as .zip */
            final String[] fileids = br.getRegex("\"fileid\": (\\d+)").getColumn(0);
            if (fileids == null || fileids.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "https://api.pcloud.com/getpubzip?fileids=";
            for (int i = 0; i < fileids.length; i++) {
                final String currentID = fileids[i];
                if (i == fileids.length - 1) {
                    dllink += currentID;
                } else {
                    dllink += currentID + "%2C";
                }
            }
            dllink += "&filename=" + dl.getStringProperty("plain_name", null) + "&code=" + dl.getStringProperty("plain_code", null);
        } else {
            final String hoststext = br.getRegex("\"hosts\": \\[(.*?)\\]").getMatch(0);
            if (hoststext == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String[] hosts = new Regex(hoststext, "\"([^<>\"]*?)\"").getColumn(0);
            dllink = PluginJSonUtils.getJsonValue(br, "path");
            if (dllink == null || hosts == null || hosts.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
            dllink = "https://" + hosts[new Random().nextInt(hosts.length - 1)] + dllink;
        }
        return dllink;
    }

    private boolean isCompleteFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("complete_folder", false);
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

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
                        for (final Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        this.account_auth = account.getStringProperty("account_auth", null);
                        return;
                    }
                }
                prepBR();
                postAPISafe("https://api.pcloud.com/userinfo", "logout=1&getauth=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&_t=" + System.currentTimeMillis());
                if (!"true".equals(PluginJSonUtils.getJsonValue(br, "emailverified"))) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDein Account ist noch nicht verifiziert!\r\nPrüfe deine E-Mails und verifiziere deinen Account!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account is not yet verified!\r\nCheck your mails and verify it!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                this.account_auth = PluginJSonUtils.getJsonValue(br, "auth");
                if (this.account_auth == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("account_auth", this.account_auth);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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
        final String premium = PluginJSonUtils.getJsonValue(br, "premium");
        ai.setUnlimitedTraffic();
        if ("true".equals(premium)) {
            ai.setStatus("Registered premium user");
            maxPrem.set(20);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(20);
            account.setConcurrentUsePossible(true);
        } else {
            ai.setStatus("Registered (free) user");
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        }

        return ai;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        final String code = getCODE(link);
        final String fileid = getFID(link);
        String new_fileid = null;
        String new_hash = null;
        String freeaccount_dllink = null;
        String download_host = null;
        String api_filename = null;
        requestFileInformation(link);
        if (this.statuscode == ERROR_PREMIUMONLY && this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, defaultMOVE_FILES_TO_ACCOUNT)) {
            int maxchunks = ACCOUNT_FREE_MAXCHUNKS;
            /* File has too much traffic on it --> Move it into our account so we can download it (if wished by user). */
            login(account, false);
            freeaccount_dllink = checkDirectLink(link, "freeaccount_dllink");
            if (freeaccount_dllink == null) {
                /* tofolderid --> 0 = root */
                getAPISafe("https://api.pcloud.com/copypubfile?fileid=" + fileid + "&tofolderid=0&code=" + code + "&auth=" + this.account_auth);
                new_fileid = PluginJSonUtils.getJsonValue(br, "fileid");
                new_hash = PluginJSonUtils.getJsonValue(br, "hash");
                api_filename = PluginJSonUtils.getJsonValue(br, "name");
                if (new_fileid == null || new_hash == null || api_filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                getAPISafe("/getfilelink?fileid=" + new_fileid + "&hashCache=" + new_hash + "&forcedownload=1&auth=" + this.account_auth);
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                final ArrayList<Object> ressourcelist = (ArrayList) entries.get("hosts");
                freeaccount_dllink = PluginJSonUtils.getJsonValue(br, "path");
                if (ressourcelist == null || freeaccount_dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                download_host = (String) ressourcelist.get(new Random().nextInt(ressourcelist.size() - 1));
                freeaccount_dllink = "https://" + download_host + freeaccount_dllink;
                if (this.getPluginConfig().getBooleanProperty(DELETE_FILE_AFTER_DOWNLOADLINK_CREATION, defaultDELETE_DELETE_FILE_AFTER_DOWNLOADLINK_CREATION)) {
                    /*
                     * It sounds crazy but we'll actually delete the file before we download it as the directlink will still be valid and
                     * this way we avoid filling up the space of our account.
                     */
                    getAPISafe("/deletefile?fileid=" + new_fileid + "&name=" + Encoding.urlEncode(api_filename) + "&id=000-0&auth=" + this.account_auth);
                    if (this.getPluginConfig().getBooleanProperty(DELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION, defaultDELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION)) {
                        /* Delete file inside trash (FOREVER) in case user wants that. */
                        getAPISafe("/trash_clear?fileid=" + new_fileid + "&id=000-0&auth=" + this.account_auth);
                    }
                }
                if (this.getPluginConfig().getBooleanProperty(EMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION, defaultEMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION)) {
                    /* Let's empty the trash in case the user wants this. */
                    getAPISafe("/trash_clear?folderid=0&auth=" + this.account_auth);
                }
            }
            if (link.getBooleanProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, false)) {
                maxchunks = 1;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, freeaccount_dllink, ACCOUNT_FREE_RESUME, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("freeaccount_dllink", freeaccount_dllink);
            try {
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, false) == false) {
                        link.setProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                e.printStackTrace();
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, false) == false) {
                    link.setProperty(NICE_HOSTproperty + PCloudCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            }
        } else if (this.statuscode == ERROR_PREMIUMONLY) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered/premium users");
        } else {
            doFree(link);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = openConnection(br2, dllink);
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

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    private String getCODE(final DownloadLink dl) {
        return dl.getStringProperty("plain_code", null);
    }

    private String getFID(final DownloadLink dl) {
        return dl.getStringProperty("plain_fileid", null);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private String getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
        return this.br.toString();
    }

    private String postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        br.postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
        return this.br.toString();
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 2000:
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2008:
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nDein Account hat keinen freien Speicherplatz mehr!";
                } else {
                    statusMessage = "\r\nYour account has no free space anymore!";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 5002:
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 'Internal error, no servers available. Try again later.'", 5 * 60 * 1000l);
            case 7002:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 7005:
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            case 7014:
                /*
                 * 2016-08-31: Added support for this though I'm not sure about this - I guess some kind of account traffic limit has been
                 * reached!
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            default:
                /* Unknown error */
                statusMessage = "This file can only be downloaded by registered/premium users";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "This file can only be downloaded by registered/premium users");
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    // private String postRawAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
    // br.postPageRaw(accesslink, postdata);
    // updatestatuscode();
    // handleAPIErrors(this.br);
    // return this.br.toString();
    // }

    /**
     * 0 = everything ok, 2000-??? = Normal "result" API errorcodes, 666 = hell
     */
    private void updatestatuscode() {
        final String error = PluginJSonUtils.getJsonValue(br, "result");
        if (error != null) {
            statuscode = Integer.parseInt(error);
        }
    }

    private static final boolean defaultDOWNLOAD_ZIP                                    = false;
    private static final boolean defaultMOVE_FILES_TO_ACCOUNT                           = false;
    private static final boolean defaultDELETE_DELETE_FILE_AFTER_DOWNLOADLINK_CREATION  = false;
    private static final boolean defaultDELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION = false;
    private static final boolean defaultEMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION         = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Crawler settings:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PCloudCom.DOWNLOAD_ZIP, JDL.L("plugins.hoster.PCloudCom.DownloadZip", "Download .zip file of all files in the folder?")).setDefaultValue(defaultDOWNLOAD_ZIP));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Host plugin settings:"));
        final ConfigEntry moveFilesToAcc = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MOVE_FILES_TO_ACCOUNT, JDL.L("plugins.hoster.PCloudCom.MoveFilesToAccount", "1. Move files with too high traffic to account before downloading them to avoid downloadlimits?")).setDefaultValue(defaultMOVE_FILES_TO_ACCOUNT);
        getConfig().addEntry(moveFilesToAcc);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MOVE_FILES_TO_ACCOUNT, JDL.L("plugins.hoster.PCloudCom.DeleteMovedFiles", "2. Delete moved files after downloadlink-generation?")).setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(defaultDELETE_DELETE_FILE_AFTER_DOWNLOADLINK_CREATION));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION, JDL.L("plugins.hoster.PCloudCom.DeleteMovedFilesForever", "3. Delete moved files FOREVER (inside trash) after downloadlink-generation?")).setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(defaultDELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION, JDL.L("plugins.hoster.PCloudCom.EmptyTrashAfterSuccessfulDownload", "4. Empty trash after downloadlink-generation?")).setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(defaultEMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}