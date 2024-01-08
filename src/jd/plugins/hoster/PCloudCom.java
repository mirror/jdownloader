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
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.PCloudComFolder;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcloud.com" }, urls = { "https?://pclouddecrypted\\.com/\\d+" })
public class PCloudCom extends PluginForHost {
    @SuppressWarnings("deprecation")
    public PCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://my.pcloud.com/#page=register");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        PCloudComFolder.prepBR(br);
        return br;
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        String folderid = null;
        try {
            folderid = this.getFolderID(link);
        } catch (final PluginException ignore) {
        }
        final String parentfolderid = link.getStringProperty("plain_parentfolderid");
        if (folderid != null && parentfolderid != null) {
            return "https://u.pcloud.link/publink/show?code=" + folderid + "#folder=" + parentfolderid + "&tpl=publicfoldergrid";
        } else {
            return super.getPluginContentURL(link);
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        String id = null;
        try {
            final String fileid = getFID(link);
            final String folderID = getFolderID(link);
            if (fileid != null) {
                id = folderID + "_" + fileid;
            }
        } catch (final Throwable e) {
        }
        if (id != null) {
            return "pcloud://" + id;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getAGBLink() {
        return "https://my.pcloud.com/#page=policies&tab=terms-of-service";
    }

    private static final String NICE_HOST                                       = "pcloud.com";
    /* Plugin Settings */
    private static final String DOWNLOAD_ZIP                                    = "DOWNLOAD_ZIP_2";
    private static final String MOVE_FILES_TO_ACCOUNT                           = "MOVE_FILES_TO_ACCOUNT";
    private static final String DELETE_FILE_AFTER_DOWNLOADLINK_CREATION         = "DELETE_FILE_AFTER_DOWNLOADLINK_CREATION";
    private static final String DELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION = "DELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION";
    private static final String EMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION         = "EMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION";
    /* Errorcodes */
    private static final int    STATUS_CODE_OKAY                                = 0;
    public static final int     STATUS_CODE_DOWNLOAD_PASSWORD_INVALID           = 1125;
    private static final int    STATUS_CODE_INVALID_LOGIN                       = 2000;
    private static final int    STATUS_CODE_MAYBE_OWNER_ONLY                    = 2003;
    public static final int     STATUS_CODE_DOWNLOAD_PASSWORD_REQUIRED          = 2258;
    private static final int    STATUS_CODE_WRONG_LOCATION                      = 2321;
    private static final int    STATUS_CODE_PREMIUMONLY                         = 7005;

    public static String getAPIDomain(final String linkDomain) {
        if (linkDomain.matches("(?i).*e\\d*\\.pcloud\\.(com|link)")) {
            // europe data center
            return "eapi.pcloud.com";
        } else {
            // us data center
            return "api.pcloud.com";
        }
    }

    public static String getAPIDomain(final DownloadLink link) throws Exception {
        final String mainLink = link.getStringProperty("mainlink");
        if (mainLink != null) {
            return getAPIDomain(new URL(mainLink).getHost());
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (this.isCompleteFolder(link)) {
            return false;
        } else {
            return true;
        }
    }

    private int getMaxChunks(final DownloadLink link, final Account account) {
        if (this.isCompleteFolder(link)) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws IOException, Exception {
        this.setBrowserExclusive();
        final String filename = link.getStringProperty("plain_name");
        final String filesize = link.getStringProperty("plain_size");
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        getDownloadURL(link, account);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String directLinkProperty = account != null ? "account_dllink" : "free_dllink";
        final String storedDirectlink = link.getStringProperty(directLinkProperty);
        String downloadURL = null;
        if (storedDirectlink != null) {
            logger.info("Attempting to re-use stored directurl: " + storedDirectlink);
            downloadURL = storedDirectlink;
        } else {
            downloadURL = getDownloadURL(link, account);
            if (downloadURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directLinkProperty, dl.getConnection().getURL().toExternalForm());
        } catch (final Exception e) {
            if (storedDirectlink != null) {
                link.removeProperty(directLinkProperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    private String getDownloadURL(final DownloadLink link, final Account account) throws Exception {
        final String folderID = getFolderID(link);
        String account_auth = null;
        String account_api = null;
        if (account != null) {
            synchronized (account) {
                account_auth = login(account, false);
                account_api = account.getStringProperty("account_api");
                if (account_api == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        if (isCompleteFolder(link)) {
            if (account_auth != null) {
                br.getPage("https://" + getAPIDomain(link) + "/showpublink?code=" + folderID + "&auth=" + account_auth);
            } else {
                br.getPage("https://" + getAPIDomain(link) + "/showpublink?code=" + folderID);
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            handleAPIErrors(br);
            /* Select all IDs of the folder to download all as .zip */
            final String[] fileids = br.getRegex("\"fileid\": (\\d+)").getColumn(0);
            if (fileids == null || fileids.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String dllink = "https://" + getAPIDomain(link) + "/getpubzip?fileids=";
            for (int i = 0; i < fileids.length; i++) {
                final String currentID = fileids[i];
                if (i == fileids.length - 1) {
                    dllink += currentID;
                } else {
                    dllink += currentID + "%2C";
                }
            }
            dllink += "&filename=" + link.getStringProperty("plain_name", null) + "&code=" + folderID;
            if (account_auth != null) {
                dllink += "&auth=" + account_auth;
            }
            return dllink;
        } else {
            final String fileID = getFID(link);
            if (StringUtils.equals(account_api, getAPIDomain(link)) && this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, defaultMOVE_FILES_TO_ACCOUNT)) {
                /*
                 * Only possible to copy files on same data center region!
                 *
                 * not yet implemented for complete folder(zip)
                 */
                /* tofolderid --> 0 = root */
                final String fileid = getFID(link);
                getAPISafe("https://" + getAPIDomain(link) + "/copypubfile?fileid=" + fileid + "&tofolderid=0&code=" + folderID + "&auth=" + account_auth);
                final String new_fileid = PluginJSonUtils.getJsonValue(br, "fileid");
                final String new_hash = PluginJSonUtils.getJsonValue(br, "hash");
                final String api_filename = PluginJSonUtils.getJsonValue(br, "name");
                if (new_fileid == null || new_hash == null || api_filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                getAPISafe("/getfilelink?fileid=" + new_fileid + "&hashCache=" + new_hash + "&forcedownload=1&auth=" + account_auth);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Object> ressourcelist = (List) entries.get("hosts");
                final String path = PluginJSonUtils.getJsonValue(br, "path");
                if (ressourcelist == null || path == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String download_host = (String) ressourcelist.get(new Random().nextInt(ressourcelist.size() - 1));
                final String directurl = "https://" + download_host + path;
                if (this.getPluginConfig().getBooleanProperty(DELETE_FILE_AFTER_DOWNLOADLINK_CREATION, defaultDELETE_DELETE_FILE_AFTER_DOWNLOADLINK_CREATION)) {
                    /*
                     * It sounds crazy but we'll actually delete the file before we download it as the directlink will still be valid and
                     * this way we avoid filling up the space of our account.
                     */
                    getAPISafe("/deletefile?fileid=" + new_fileid + "&name=" + Encoding.urlEncode(api_filename) + "&id=000-0&auth=" + account_auth);
                    if (this.getPluginConfig().getBooleanProperty(DELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION, defaultDELETE_FILE_FOREVER_AFTER_DOWNLOADLINK_CREATION)) {
                        /* Delete file inside trash (FOREVER) in case user wants that. */
                        getAPISafe("/trash_clear?fileid=" + new_fileid + "&id=000-0&auth=" + account_auth);
                    }
                }
                if (this.getPluginConfig().getBooleanProperty(EMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION, defaultEMPTY_TRASH_AFTER_DOWNLOADLINK_CREATION)) {
                    /* Let's empty the trash in case the user wants this. */
                    getAPISafe("/trash_clear?folderid=0&auth=" + account_auth);
                }
                return directurl;
            }
            final UrlQuery query = new UrlQuery();
            query.add("code", folderID);
            query.add("forcedownload", "1");
            query.add("fileid", fileID);
            if (account_auth != null) {
                query.add("auth", account_auth);
            }
            String passCode = link.getDownloadPassword();
            Map<String, Object> resp = null;
            int passwordAttempts = 0;
            do {
                if (passCode != null) {
                    query.addAndReplace("linkpassword", Encoding.urlEncode(passCode));
                }
                br.getPage("https://" + getAPIDomain(link) + "/getpublinkdownload?" + query.toString());
                resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final int errorcode = ((Number) resp.get("result")).intValue();
                if (errorcode == STATUS_CODE_MAYBE_OWNER_ONLY && account_auth != null) {
                    br.getPage("/getfilelink?" + query.toString());
                    resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                }
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final int statusCode = ((Number) resp.get("result")).intValue();
                if (statusCode == STATUS_CODE_DOWNLOAD_PASSWORD_REQUIRED || statusCode == STATUS_CODE_DOWNLOAD_PASSWORD_INVALID) {
                    logger.info("Password invalid/required | password = " + passCode);
                    if (passwordAttempts >= 3) {
                        link.setDownloadPassword(null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                    }
                    link.setPasswordProtected(true);
                    passCode = getUserInput("Password?", link);
                    passwordAttempts++;
                    continue;
                } else {
                    if (passCode == null) {
                        link.setPasswordProtected(false);
                    }
                    break;
                }
            } while (!this.isAbort());
            if (passCode != null) {
                /* Save for later */
                link.setDownloadPassword(passCode);
            }
            final List<String> hosts = (List<String>) resp.get("hosts");
            String dllink = resp.get("path").toString();
            if (dllink == null || hosts == null || hosts == null || hosts.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "https://" + hosts.get(new Random().nextInt(hosts.size() - 1)) + dllink;
            return dllink;
        }
    }

    private boolean isCompleteFolder(final DownloadLink link) {
        return link.getBooleanProperty("complete_folder", false);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            if (!account.getUser().matches(".+@.+\\..+")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new AccountInvalidException("\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!");
                } else {
                    throw new AccountInvalidException("\r\nPlease enter your e-mail address in the username field!");
                }
            }
            final Cookies cookies = account.loadCookies("");
            String account_auth = account.getStringProperty("account_auth");
            String account_api = account.getStringProperty("account_api");
            if (cookies != null && StringUtils.isAllNotEmpty(account_auth, account_api)) {
                br.setCookies(cookies);
                if (!force) {
                    logger.info("Trust token without checking");
                    return account_auth;
                }
                br.getPage("https://" + account_api + "/userinfo?auth=" + Encoding.urlEncode(account_auth) + "&getlastsubscription=1");
                try {
                    final Map<String, Object> resp = this.handleAPIErrors(br);
                    logger.info("Token login successful");
                    updateAccountInfo(account, resp);
                    return account_auth;
                } catch (final PluginException e) {
                    /* Wrong token = Will typically fail with errorcode 2000 */
                    logger.info("Token login failed");
                    logger.log(e);
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            /* Depending on which selection the user met when he registered his account, a different endpoint is required for login. */
            logger.info("Trying to login via US-API endpoint");
            br.postPage("https://api.pcloud.com/login", "logout=1&getauth=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&_t=" + System.currentTimeMillis());
            Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final int statusCode = ((Number) resp.get("result")).intValue();
            if (statusCode == STATUS_CODE_WRONG_LOCATION || statusCode == STATUS_CODE_INVALID_LOGIN) {
                logger.info("Trying to login via EU-API endpoint");
                br.postPage("https://eapi.pcloud.com/login", "logout=1&getauth=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&_t=" + System.currentTimeMillis());
                resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                this.handleAPIErrors(resp);
            }
            updateAccountInfo(account, resp);
            account_auth = resp.get("auth").toString();
            if (StringUtils.isEmpty(account_auth)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account_api = br.getHost(true);
            getAPISafe("https://" + account_api + "/userinfo?auth=" + Encoding.urlEncode(account_auth) + "&getlastsubscription=1");
            account.setProperty("account_api", account_api);
            account.setProperty("account_auth", account_auth);
            account.saveCookies(br.getCookies(br.getURL()), "");
            return account_auth;
        }
    }

    private void updateAccountInfo(final Account account, final Map<String, Object> resp) throws PluginException {
        if (Boolean.FALSE.equals(resp.get("emailverified"))) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new AccountInvalidException("\r\nDein Account ist noch nicht verifiziert!\r\nPrüfe deine E-Mails und verifiziere deinen Account!");
            } else {
                throw new AccountInvalidException("\r\nYour account is not yet verified!\r\nCheck your mails and verify it!");
            }
        }
        if (Boolean.TRUE.equals(resp.get("premiumlifetime"))) {
            account.setType(AccountType.LIFETIME);
            account.setMaxSimultanDownloads(20);
        } else if (Boolean.TRUE.equals(resp.get("premium"))) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(20);
        } else {
            account.setType(AccountType.FREE);
            /* Last checked: 2020-10-06 */
            account.setMaxSimultanDownloads(20);
        }
        account.setConcurrentUsePossible(true);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    private String getFolderID(final DownloadLink dl) throws PluginException {
        final String ret = dl.getStringProperty("plain_code");
        if (ret == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return ret;
        }
    }

    private String getFID(final DownloadLink dl) throws PluginException {
        final String ret = dl.getStringProperty("plain_fileid");
        if (ret == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return ret;
        }
    }

    private Map<String, Object> getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        return handleAPIErrors(this.br);
    }

    private Map<String, Object> postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        br.postPage(accesslink, postdata);
        return handleAPIErrors(this.br);
    }

    private Map<String, Object> handleAPIErrors(final Browser br) throws PluginException {
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        return handleAPIErrors(entries);
    }

    private Map<String, Object> handleAPIErrors(final Map<String, Object> entries) throws PluginException {
        final int statusCode = ((Number) entries.get("result")).intValue();
        final String errormessage = (String) entries.get("error");
        try {
            switch (statusCode) {
            case STATUS_CODE_OKAY:
                /* Everything ok */
                break;
            case 1029:
                throw new AccountRequiredException();
            case STATUS_CODE_DOWNLOAD_PASSWORD_INVALID:
                throw new PluginException(LinkStatus.ERROR_RETRY, "Download password invalid");
            case STATUS_CODE_INVALID_LOGIN:
                throw new AccountInvalidException(errormessage);
            case 2008:
                /* Account is out of storage space */
                throw new AccountInvalidException(errormessage);
            case 2009:
                /* { "result": 2009, "error": "File not found."} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case STATUS_CODE_DOWNLOAD_PASSWORD_REQUIRED:
                throw new PluginException(LinkStatus.ERROR_RETRY, "Download password required");
            case STATUS_CODE_WRONG_LOCATION:
                // wrong location
                /*
                 * {"result": 2321, "error": "This user is on another location.", "location": { "label": "US", "id": 1, "binapi":
                 * "binapi.pcloud.com", "api": "api.pcloud.com" }}
                 */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 5002:
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 'Internal error, no servers available. Try again later.'", 5 * 60 * 1000l);
            case 7002:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case STATUS_CODE_PREMIUMONLY:
                throw new AccountRequiredException();
            case STATUS_CODE_MAYBE_OWNER_ONLY:
                /* file might be set to preview only download */
                /* "error": "Access denied. You do not have permissions to perform this operation." */
                throw new AccountRequiredException();
            case 7014:
                /*
                 * 2016-08-31: Added support for this though I'm not sure about this - I guess some kind of account traffic limit has been
                 * reached!
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            default:
                /* Unknown error */
                throw new PluginException(LinkStatus.ERROR_FATAL, errormessage);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statusCode + " statusMessage: " + errormessage);
            throw e;
        }
        return entries;
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