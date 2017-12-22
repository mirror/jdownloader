//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "disk.yandex.net", "video.yandex.ru" }, urls = { "http://yandexdecrypted\\.net/\\d+", "http://video\\.yandex\\.ru/(iframe/[A-Za-z0-9]+/[A-Za-z0-9]+\\.\\d+|users/[A-Za-z0-9]+/view/\\d+)" })
public class DiskYandexNet extends PluginForHost {
    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://passport.yandex.ru/passport?mode=register&from=cloud&retpath=https%3A%2F%2Fdisk.yandex.ru%2F%3Fauth%3D1&origin=face.en");
        setConfigElements();
        this.setStartIntervall(1 * 1000);
    }

    @Override
    public String getAGBLink() {
        return "https://disk.yandex.net/";
    }

    /* Settings values */
    private final String          MOVE_FILES_TO_ACCOUNT              = "MOVE_FILES_TO_ACCOUNT";
    private final String          DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD = "EMPTY_TRASH_AFTER_DOWNLOAD";
    private final String          DOWNLOAD_ZIP                       = "DOWNLOAD_ZIP_2";
    private static final String   NORESUME                           = "NORESUME";
    /* Some constants which they used in browser */
    public static final String    CLIENT_ID                          = "6214e1ac6b579eb984b716151bcb5143";
    public static String          VERSION                            = "8.5";
    private static final String   STANDARD_FREE_SPEED                = "64 kbit/s";
    /* Different languages == different 'downloads' directory names */
    private static final String[] downloaddirs                       = { "Downloads", "%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8" };
    /* Connection limits */
    private final boolean         FREE_RESUME                        = true;
    private final int             FREE_MAXCHUNKS                     = 0;
    private static final int      FREE_MAXDOWNLOADS                  = 20;
    private final boolean         ACCOUNT_FREE_RESUME                = true;
    private final int             ACCOUNT_FREE_MAXCHUNKS             = 0;
    private static final int      ACCOUNT_FREE_MAXDOWNLOADS          = 20;
    /* Domain & other login stuff */
    private final String          MAIN_DOMAIN                        = "https://yandex.com";
    private final String[]        domains                            = new String[] { "https://yandex.ru", "https://yandex.com", "https://disk.yandex.ru/", "https://disk.yandex.com/", "https://disk.yandex.net/" };
    private static Object         LOCK                               = new Object();
    /* Other constants */
    /* Important constant which seems to be unique for every account. It's needed for most of the requests when logged in. */
    private String                ACCOUNT_SK                         = null;
    private static final String   TYPE_VIDEO                         = "http://video\\.yandex\\.ru/(iframe/[A-Za-z0-9]+/[A-Za-z0-9]+\\.\\d+|users/[A-Za-z0-9]+/view/\\d+)";
    private static final String   TYPE_VIDEO_USER                    = "http://video\\.yandex\\.ru/users/[A-Za-z0-9]+/view/\\d+";
    private static final String   TYPE_DISK                          = "http://yandexdecrypted\\.net/\\d+";
    private static final String   ACCOUNTONLYTEXT                    = "class=\"nb-panel__warning aside\\-public__warning\\-speed\"|>File download limit exceeded";
    private Account               currAcc                            = null;
    private String                currHash                           = null;
    private String                currPath                           = null;
    /* 2017-02-08: Disabled API usage due to issues/ especially the download API request seems not to work anymore. */
    private static final boolean  use_api_file_free_availablecheck   = false;
    private static final boolean  use_api_file_free_download         = false;

    /* Make sure we always use our main domain */
    private String getMainLink(final DownloadLink dl) throws PluginException {
        String mainlink = dl.getStringProperty("mainlink", null);
        mainlink = null;
        if (mainlink == null && this.currHash != null) {
            mainlink = String.format("https://yadi.sk/public/?hash=%s", Encoding.urlEncode(this.currHash));
        }
        if (mainlink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!StringUtils.contains(mainlink, "yadi")) {
            mainlink = "https://disk.yandex.com/" + new Regex(mainlink, "yandex\\.[^/]+/(.+)").getMatch(0);
        }
        return mainlink;
    }

    private void setConstants(final DownloadLink dl, final Account acc) {
        currAcc = acc;
        currHash = this.getHash(dl);
        currPath = this.getPath(dl);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setConstants(link, null);
        setBrowserExclusive();
        this.br = prepBR(this.br);
        br.setFollowRedirects(true);
        String filename;
        String filesize_str;
        long filesize_long = -1;
        String final_filename = link.getStringProperty("plain_filename", null);
        if (final_filename == null) {
            final_filename = link.getFinalFileName();
        }
        if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            getPage(link.getDownloadURL());
            if (link.getDownloadURL().matches(TYPE_VIDEO_USER)) {
                /* offline|empty|enything else (e.g. abuse) */
                if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>Ролик не найден</title>|>Здесь пока пусто<|class=\"error\\-container\"")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String iframe_url = br.getRegex("property=\"og:video:ifrаme\" content=\"(http://video\\.yandex\\.ru/iframe/[^<>\"]*?)\"").getMatch(0);
                if (iframe_url == null) {
                    iframe_url = br.getRegex("class=\"video\\-frame\"><iframe src=\"(//video\\.yandex\\.ru/[^<>\"]*?)\"").getMatch(0);
                }
                if (iframe_url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!iframe_url.startsWith("http:")) {
                    iframe_url = "http:" + iframe_url;
                }
                link.setUrlDownload(iframe_url);
                getPage(iframe_url);
            }
            if (br.containsHTML("<title>Яндекс\\.Видео</title>") || this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?) — Яндекс\\.Видео</title>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
            filename = encodeUnicode(filename);
            link.setName(filename);
        } else {
            if (link.getBooleanProperty("offline", false)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.currHash == null || this.currPath == null) {
                /* Errorhandling for old urls */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (use_api_file_free_availablecheck) {
                getPage("https://cloud-api.yandex.net/v1/disk/public/resources?public_key=" + Encoding.urlEncode(this.currHash) + "&path=" + Encoding.urlEncode(this.currPath));
                if (apiAvailablecheckIsOffline(br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                link.setProperty("premiumonly", false);
                if (isZippedFolder(link)) {
                    /*
                     * We cannot get filename/size from zipped root folders - we already set these information in the decrypter so now we
                     * know it is online!
                     */
                    return AvailableStatus.TRUE;
                }
                return parseInformationAPIAvailablecheck(this, link, (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString()));
            } else {
                getPage(getMainLink(link));
                if (br.containsHTML("(<title>The file you are looking for could not be found\\.|>Nothing found</span>|<title>Nothing found \\— Yandex\\.Disk</title>)") || br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = this.br.getRegex("class=\"info\\-public__name\" title=\"([^<>\"]+)\"").getMatch(0);
                filesize_str = link.getStringProperty("plain_size", null);
                if (filesize_str == null) {
                    filesize_str = this.br.getRegex("class=\"item-details__name\">Size:</span> ([^<>\"]+)</div>").getMatch(0);
                }
                if (filesize_str == null) {
                    /* Language independant */
                    filesize_str = this.br.getRegex("class=\"item-details__name\">[^<>\"]+</span> ([\\d\\.]+ (?:B|KB|MB|GB))</div>").getMatch(0);
                }
                if (filesize_str != null) {
                    filesize_str = filesize_str.replace(",", ".");
                    filesize_long = SizeFormatter.getSize(filesize_str);
                }
                if (final_filename == null) {
                    final_filename = filename;
                }
                /* Important for account download handling */
                if (br.containsHTML(ACCOUNTONLYTEXT)) {
                    link.setProperty("premiumonly", true);
                } else {
                    link.setProperty("premiumonly", false);
                }
            }
            if (final_filename == null && filename != null) {
                link.setFinalFileName(filename);
            }
        }
        if (filesize_long > -1) {
            link.setDownloadSize(filesize_long);
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationAPIAvailablecheck(final Plugin plugin, final DownloadLink dl, final LinkedHashMap<String, Object> entries) throws Exception {
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
        final String error = (String) entries.get("error");
        final String hash = (String) entries.get("public_key");
        String filename = (String) entries.get("name");
        final String path = (String) entries.get("path");
        final String md5 = (String) entries.get("md5");
        final String sha256 = (String) entries.get("sha256");
        if (error != null || filename == null || path == null || hash == null || filesize == -1) {
            /* Whatever - our link is probably offline! */
            dl.setAvailable(false);
            return AvailableStatus.FALSE;
        }
        filename = plugin.encodeUnicode(filename);
        if (md5 != null) {
            dl.setMD5Hash(md5);
        }
        if (sha256 != null) {
            dl.setSha256Hash(sha256);
        }
        dl.setProperty("path", path);
        dl.setProperty("hash_main", hash);
        dl.setFinalFileName(filename);
        dl.setDownloadSize(filesize);
        dl.setAvailable(true);
        return AvailableStatus.TRUE;
    }

    public static boolean apiAvailablecheckIsOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie("http://disk.yandex.com/", "ys", "");
        return br;
    }

    private void getPage(final String url) throws IOException {
        getPage(this.br, url);
    }

    public static void getPage(final Browser br, final String url) throws IOException {
        br.getPage(url);
        /* 2017-03-30: New */
        final String jsRedirect = br.getRegex("(https?://[^<>\"]+force_show=1[^<>\"]*?)").getMatch(0);
        if (jsRedirect != null) {
            br.getPage(jsRedirect);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink;
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(TYPE_DISK)) {
            boolean resume;
            int maxchunks;
            checkDiskFeatureDialog();
            if (downloadableViaAccountOnly(downloadLink)) {
                /*
                 * link is only downloadable via account because the public overall download limit (traffic limit) is exceeded. In this case
                 * the user can only download the link by importing it into his account and downloading it "from there".
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            if (use_api_file_free_download) {
                /**
                 * Download API:
                 *
                 * https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=public_key&path=/
                 */
                /* Free API download. */
                getPage("https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=" + Encoding.urlEncode(this.currHash) + "&path=" + Encoding.urlEncode(this.currPath));
                if (this.br.containsHTML("DiskNotFoundError")) {
                    /* Inside key 'error' */
                    /*
                     * Usually this would mean that the file is offline but we checked it before so it has probably simply reached the
                     * traffic limit.
                     */
                    downloadLink.setProperty("premiumonly", true);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                }
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                dllink = (String) entries.get("href");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                /* Free website download. */
                this.br = this.prepbrWebsite(new Browser());
                getPage(getMainLink(downloadLink));
                String sk = getSK(this.br);
                /* 2017-02-09: Required */
                getPage("https://yadi.sk/tns.html");
                if (sk == null) {
                    logger.warning("sk in account handling (without move) is null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=" + getID0ForPostFree(isPartOfAFolder(downloadLink)) + "&idClient=" + getIdClient() + "&version=" + VERSION + "&sk=" + sk);
                /** TODO: Find out why we have the wrong SK here and remove this workaround! */
                if (br.containsHTML("\"id\":\"WRONG_SK\"")) {
                    sk = getSK(this.br);
                    if (sk == null || sk.equals("")) {
                        logger.warning("sk in account handling (without move) is null");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.postPage("https://disk.yandex.com/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=" + getID0ForPostFree(isPartOfAFolder(downloadLink)) + "&idClient=" + getIdClient() + "&version=" + VERSION + "&sk=" + sk);
                }
                handleErrorsFree();
                dllink = PluginJSonUtils.getJsonValue(br, "file");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* Don't do htmldecode because the link will be invalid then */
                dllink = HTMLEntities.unhtmlentities(dllink);
            }
            if (isZippedFolder(downloadLink)) {
                resume = false;
                maxchunks = 1;
            } else {
                resume = FREE_RESUME;
                maxchunks = FREE_MAXCHUNKS;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors(downloadLink);
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            handleDownloadVideo(downloadLink);
        }
    }

    @SuppressWarnings("deprecation")
    private void handleDownloadVideo(final DownloadLink downloadLink) throws Exception {
        final String linkpart = new Regex(downloadLink.getDownloadURL(), "/iframe/(.+)").getMatch(0);
        final String width = br.getRegex("width\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        final String height = br.getRegex("width\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        String file = br.getRegex("\\&quot;file\\&quot;:\\&quot;([a-z0-9]+)\\&quot;").getMatch(0);
        if (file == null) {
            file = br.getRegex("name=\"twitter:image\" content=\"https?://static\\.video\\.yandex.ru/get/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_\\.]+/([A-Za-z0-9]+)\\.jpg\"").getMatch(0);
        }
        if (file == null && (width != null && height != null)) {
            file = "m" + width + "x" + height + ".flv";
            downloadLink.setFinalFileName(downloadLink.getName().replace(".mp4", ".flv"));
        } else if (file == null) {
            file = "0.flv";
            downloadLink.setFinalFileName(downloadLink.getName().replace(".mp4", ".flv"));
        } else {
            file += ".mp4";
            downloadLink.setFinalFileName(downloadLink.getName().replace(".flv", ".mp4"));
        }
        getPage("http://static.video.yandex.net/get-token/" + linkpart + "?nc=0." + System.currentTimeMillis());
        final String token = br.getRegex("<token>([^<>\"]*?)</token>").getMatch(0);
        if (token == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage("http://streaming.video.yandex.ru/get-location/" + linkpart + "/" + file + "?token=" + token + "&ref=video.yandex.ru");
        String dllink = br.getRegex("<video\\-location>(http://[^<>\"]*?)</video\\-location>").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors(downloadLink);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleErrorsFree() throws PluginException {
        if (br.containsHTML("\"title\":\"invalid ckey\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'invalid ckey'", 5 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":21")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 21 'bad formed path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":69")) {
            /* Usually this does not happen. Happens also if you actually try to download a "premiumonly" link via this method. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"code\":71")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 71 'Wrong path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":77")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 77 'resource not found'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":88")) {
            /* Happens when we send a wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 88 'Decryption error'", 10 * 60 * 1000l);
        }
    }

    private String getIdClient() {
        return String.format("undefined", System.currentTimeMillis());
    }

    private String getHash(final DownloadLink dl) {
        final String hash = dl.getStringProperty("hash_main", null);
        return hash;
    }

    private String getPath(final DownloadLink dl) {
        return dl.getStringProperty("path", null);
    }

    private String getCkey() throws PluginException {
        final String ckey = PluginJSonUtils.getJsonValue(br, "ckey");
        if (ckey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ckey;
    }

    private boolean isZippedFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("is_zipped_folder", false);
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepbrWebsite(this.br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final String domain : domains) {
                            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                                final String key = cookieEntry.getKey();
                                final String value = cookieEntry.getValue();
                                br.setCookie(domain, key, value);
                            }
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                getPage("https://disk.yandex.com/?auth=1");
                br.postPage("https://passport.yandex.com/passport?mode=auth&from=cloud&origin=facelogin.en", "twoweeks=yes&retpath=&login=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAIN_DOMAIN, "yandex_login") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAIN_DOMAIN);
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
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        getPage("https://disk.yandex.com/client/disk/");
        ACCOUNT_SK = br.getRegex("\"sk\":\"([a-z0-9]+)\"").getMatch(0);
        if (ACCOUNT_SK == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        account.setProperty("saved_sk", ACCOUNT_SK);
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Free Account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        ACCOUNT_SK = account.getStringProperty("saved_sk", null);
        requestFileInformation(link);
        login(account, false);
        setConstants(link, account);
        String dllink = checkDirectLink(link, "directlink_account");
        if (dllink == null) {
            final String hash = getHash(link);
            /* This should never happen */
            if (ACCOUNT_SK == null) {
                logger.warning("ACCOUNT_SK is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final boolean moveIntoAccHandlingActive = this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, false);
            final boolean forcedmoveIntoAccHandling = downloadableViaAccountOnly(link);
            final boolean forcedmoveIntoAccHandlingAgainstUserSelection = downloadableViaAccountOnly(link) && !moveIntoAccHandlingActive;
            if (moveIntoAccHandlingActive || forcedmoveIntoAccHandling) {
                if (forcedmoveIntoAccHandling) {
                    logger.info("forcedmoveIntoAccHandling active");
                }
                boolean file_moved = link.getBooleanProperty("file_moved", false);
                try {
                    logger.info("MoveToAccount handling is active -> Starting account download handling");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    if (file_moved) {
                        logger.info("Seems like the file has already been moved to the account -> Trying to get it");
                        dllink = getLinkFromFileInAccount(link, br);
                        if (dllink == null) {
                            logger.info("Seems like the file is not in the account -> Trying to move it");
                            file_moved = false;
                            link.setProperty("file_moved", false);
                        }
                    }
                    if (dllink == null) {
                        postPage("https://disk.yandex.com/models/?_m=do-save-resource-public", "_model.0=do-save-resource-public&id.0=" + getID0ForPostFree(isPartOfAFolder(link)) + "&async.0=0&idClient=" + CLIENT_ID + "&version=" + VERSION + "&sk=" + ACCOUNT_SK);
                        /* TODO: Maybe add/find a way to verify if the file really has been moved to the account. */
                        if (br.containsHTML("\"code\":85")) {
                            logger.info("No free space available, failed to move file to account");
                            throw new PluginException(LinkStatus.ERROR_FATAL, "No free space available, failed to move file to account");
                        }
                        dllink = getLinkFromFileInAccount(link, br);
                        if (dllink == null) {
                            /*
                             * Possible errors (which should never occur: "id":"HTTP_404 == File could not be found in the account -->
                             * Probably move handling failed or is broken
                             */
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        file_moved = true;
                        link.setProperty("file_moved", true);
                        if (this.getPluginConfig().getBooleanProperty(DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, false)) {
                            try {
                                logger.info("Successfully grabbed dllink via move_to_Account_handling -> Move file to trash -> Trying to delete file from account");
                                moveFileToTrash(link);
                                /* Only empty trash if desired by user */
                                if (!forcedmoveIntoAccHandlingAgainstUserSelection) {
                                    logger.info("Successfully grabbed dllink via move_to_Account_handling -> Empty trash -> Trying to empty trash inside account");
                                    emptyTrash();
                                }
                            } catch (final Throwable e) {
                            }
                        }
                    }
                } catch (final PluginException e) {
                    if (file_moved) {
                        logger.info("MoveToAccount download-handling failed (dllink == null) -> Deleting moved file and emptying trash, then falling back to free download handling");
                        moveFileToTrash(link);
                        /* Only empty trash if desired by user */
                        if (!forcedmoveIntoAccHandlingAgainstUserSelection) {
                            emptyTrash();
                        }
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                logger.info("MoveToAccount handling is inactive -> Starting free account download handling");
                getPage(getMainLink(link));
                br.postPage("https://disk.yandex.com/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=%2Fpublic%2F" + hash + "&idClient=" + CLIENT_ID + "&version=" + VERSION + "&sk=" + this.ACCOUNT_SK);
                handleErrorsFree();
                dllink = siteGetDllink(link);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        boolean resume = ACCOUNT_FREE_RESUME;
        int maxchunks = ACCOUNT_FREE_MAXCHUNKS;
        if (link.getBooleanProperty(DiskYandexNet.NORESUME, false)) {
            logger.info("Resume is disabled for this try");
            resume = false;
            link.setProperty(DiskYandexNet.NORESUME, Boolean.valueOf(false));
        }
        if (isZippedFolder(link)) {
            resume = false;
            maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors(link);
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directlink_account", dllink);
        dl.startDownload();
    }

    private boolean downloadableViaAccountOnly(final DownloadLink dl) {
        return dl.getBooleanProperty("premiumonly", false);
    }

    private boolean isPartOfAFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("is_part_of_a_folder", false);
    }

    @SuppressWarnings("deprecation")
    private void handleServerErrors(final DownloadLink link) throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(DiskYandexNet.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private String getLinkFromFileInAccount(final DownloadLink dl, final Browser br2) {
        final String filepath = siteGetInternalFilePath(dl);
        String dllink = null;
        for (final String downloaddir : downloaddirs) {
            try {
                br.setFollowRedirects(false);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Referer", "https://disk.yandex.com/client/disk/Downloads");
                postPage("https://disk.yandex.com/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=%2Fdisk%2F" + downloaddir + "%2F" + filepath + "&idClient=" + CLIENT_ID + "&version=" + VERSION + "&sk=" + this.ACCOUNT_SK);
                /* 28 = file not found, 70 = folder not found */
                if (br.containsHTML("\"code\":28") || br.containsHTML("\"code\":70")) {
                    logger.info("getLinkFromFileInAccount: Moved file was not found in directory: " + downloaddir);
                    continue;
                }
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    dllink = siteGetDllink(dl);
                }
                br.setFollowRedirects(true);
                /* Fix links - cookies sit on the other domain */
                if (dllink != null) {
                    dllink = dllink.replace("disk.yandex.ru/", "disk.yandex.com/");
                }
                break;
            } catch (final Throwable e) {
                logger.warning("Failed to create dllink of link in account - Exception!");
                break;
            }
        }
        return dllink;
    }

    private void moveFileToTrash(final DownloadLink dl) {
        final String filepath = siteGetInternalFilePath(dl);
        for (final String downloaddir : downloaddirs) {
            try {
                postPage("https://disk.yandex.com/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=%2Fdisk%2F" + downloaddir + "%2F" + filepath + "&idClient=" + CLIENT_ID + "&sk=" + this.ACCOUNT_SK + "&version=" + VERSION);
                /* 28 = file not found, 70 = folder not found */
                if (br.containsHTML("\"code\":28") || br.containsHTML("\"code\":70")) {
                    logger.info("moveFileToTrash: ");
                    continue;
                }
                logger.info("Successfully moved file to trash");
                break;
            } catch (final Throwable e) {
                logger.warning("Failed to move file to trash - Exception!");
                break;
            }
        }
    }

    private String siteGetDllink(final DownloadLink dl) {
        final String dllink;
        if (isZippedFolder(dl)) {
            dllink = PluginJSonUtils.getJsonValue(br, "folder");
        } else {
            dllink = PluginJSonUtils.getJsonValue(br, "file");
        }
        return dllink;
    }

    /* Returns URL-encoded internal path for download/move/file POST-requests */
    private String siteGetInternalFilePath(final DownloadLink dl) {
        final String plain_filename = dl.getStringProperty("plain_filename", null);
        String filepath = null;
        if (isZippedFolder(dl)) {
            /* Rare case */
            filepath = plain_filename.substring(0, plain_filename.lastIndexOf("."));
            filepath = Encoding.urlEncode(filepath);
        } else {
            filepath = Encoding.urlEncode(plain_filename);
        }
        if (filepath == null) {
            filepath = PluginJSonUtils.getJsonValue(br, "path");
        }
        return filepath;
    }

    private void emptyTrash() {
        try {
            postPage("https://disk.yandex.com/models/?_m=do-clean-trash", "_model.0=do-clean-trash&idClient=" + CLIENT_ID + "&sk=" + this.ACCOUNT_SK + "&version=" + VERSION);
            logger.info("Successfully emptied trash");
        } catch (final Throwable e) {
            logger.warning("Failed to empty trash");
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
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

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    private String getID0ForPostFree(final boolean isPartOfAFolder) {
        final String hash = this.currHash.replace("/", "_").replace("+", "-");
        final String path = this.currPath;
        final String postValue;
        if (isPartOfAFolder) {
            postValue = "%2Fpublic%2F" + Encoding.urlEncode(hash + ":" + path);
        } else {
            postValue = "%2Fpublic%2F" + Encoding.urlEncode(hash);
        }
        return postValue;
    }

    public static String getSK(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    private Browser prepbrWebsite(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.setFollowRedirects(true);
        return br;
    }

    public static Browser prepbrAPI(final Browser br) {
        return br;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private void postPage(final String url, final String data) throws Exception {
        br.postPage(url, data);
        if (br.containsHTML("\"id\":\"WRONG_SK\"")) {
            logger.info("Refreshing ACCOUNT_SK");
            this.login(this.currAcc, true);
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's Yandex.ru Plugin helps downloading files from Yandex.ru. It provides some settings for downloads via account.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DOWNLOAD_ZIP, JDL.L("plugins.hoster.DiskYandexNet.DownloadZip", "Folders: Download .zip file of all files in the folder [works only for root folders]?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Account settings:"));
        final ConfigEntry moveFilesToAcc = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MOVE_FILES_TO_ACCOUNT, JDL.L("plugins.hoster.DiskYandexNet.MoveFilesToAccount", "1. Move files to account before downloading them to get higher download speeds?")).setDefaultValue(false);
        getConfig().addEntry(moveFilesToAcc);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, JDL.L("plugins.hoster.DiskYandexNet.EmptyTrashAfterSuccessfulDownload", "2. Delete moved files & empty trash after downloadlink-generation?")).setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(false));
    }

    private void checkDiskFeatureDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog_Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog_Shown2") == null) {
                    showDiskFeatureDialogAll();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_Shown2", "shown");
                config.save();
            }
        }
    }

    private static void showDiskFeatureDialogAll() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Disk.yandex.net Plugin";
                        final String lang = System.getProperty("user.language");
                        if ("de".equalsIgnoreCase(lang)) {
                            message += "Du benutzt disk.yandex.net zum ersten mal in JDownloader.\r\n";
                            message += "Momentan lädst du ohne Account und somit begrenzt yandex.net deine Downloadgeschwindigkeit auf nur " + STANDARD_FREE_SPEED + ".\r\n";
                            message += "Indem du dir einen KOSTENLOSEN Account (das ist KEINE Werbung!) anlegst und ihn in JDownloader einträgst,\r\n";
                            message += "kannst du ohne Limits von diesem Hoster laden.\r\n";
                            message += "\r\n";
                            message += "Wir wünschen dir weiterhin viel Spaß mit JDownloader.\r\n";
                            message += "Fragen oder Probleme? Melde dich in unserem Support Forum!";
                        } else {
                            message += "You're using disk.yandex.net for the frist time in JDownloader.\r\n";
                            message += "Because you're downloading without Account, yandex.net limits your speed to only " + STANDARD_FREE_SPEED + ".\r\n";
                            message += "By creating a FREE account (this is NOT advertising!) and adding it to JDownloader\r\n";
                            message += "you will be able to download without any limits from this host.\r\n";
                            message += "\r\n";
                            message += "Furthermore have fun using JDownloader.\r\n";
                            message += "In case there are any questions or problems, you can contact us via our support forum!";
                        }
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}