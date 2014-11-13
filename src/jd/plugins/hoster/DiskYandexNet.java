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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "disk.yandex.net", "video.yandex.ru" }, urls = { "http://yandexdecrypted\\.net/\\d+", "http://video\\.yandex\\.ru/(iframe/[A-Za-z0-9]+/[A-Za-z0-9]+\\.\\d+|users/[A-Za-z0-9]+/view/\\d+)" }, flags = { 2, 0 })
public class DiskYandexNet extends PluginForHost {

    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://passport.yandex.ru/passport?mode=register&from=cloud&retpath=https%3A%2F%2Fdisk.yandex.ru%2F%3Fauth%3D1&origin=face.en");
        setConfigElements();
        this.setStartIntervall(10 * 1000);
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
    private final String          CLIENT_ID                          = "883aacd8d0b882b2e379506a55fb6b0f";
    private final String          VERSION                            = "2.0.102";
    private static final String   STANDARD_FREE_SPEED                = "64 kbit/s";

    /* Different languages == different 'downloads' directory names */
    private static final String[] downloaddirs                       = { "%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8", "Downloads" };

    /* Connection limits */
    private final boolean         FREE_RESUME                        = false;
    private final int             FREE_MAXCHUNKS                     = 1;
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

    private static AtomicInteger  totalMaxSimultanFreeAccDownload    = new AtomicInteger(ACCOUNT_FREE_MAXDOWNLOADS);
    /* don't touch the following! */
    private static AtomicInteger  maxFreeAcc                         = new AtomicInteger(1);

    private Account               currAcc                            = null;

    /* Make sure we always use our main domain */
    private String getMainLink(final DownloadLink dl) {
        String mainlink = dl.getStringProperty("mainlink", null);
        mainlink = "https://disk.yandex.com/" + new Regex(mainlink, "yandex\\.[a-z]+/(.+)").getMatch(0);
        return mainlink;
    }

    private void setConstants(final Account acc) {
        currAcc = acc;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie("http://disk.yandex.com/", "ys", "");
        br.setFollowRedirects(true);
        String filename;
        if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            br.getPage(link.getDownloadURL());
            if (link.getDownloadURL().matches(TYPE_VIDEO_USER)) {
                /* offline|empty|enything else (e.g. abuse) */
                if (br.containsHTML("<title>Ролик не найден</title>|>Здесь пока пусто<|class=\"error\\-container\"")) {
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
                br.getPage(iframe_url);
            }
            if (br.containsHTML("<title>Яндекс\\.Видео</title>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?) — Яндекс\\.Видео</title>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
            filename = encodeUnicode(filename);
        } else {
            if (link.getBooleanProperty("offline", false)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!link.getDownloadURL().matches(TYPE_DISK)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(getMainLink(link));
            if (br.containsHTML("(<title>The file you are looking for could not be found\\.|>Nothing found</span>|<title>Nothing found \\— Yandex\\.Disk</title>)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = link.getStringProperty("plain_filename", null);
            final String filesize = link.getStringProperty("plain_size", null);
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        link.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(TYPE_DISK)) {
            checkDiskFeatureDialog();
        }
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks) throws Exception, PluginException {
        if (br.containsHTML("class=\"text text_download\\-blocked\"")) {
            /*
             * link is only downloadable via account because the public overall download limit (traffic limit) is exceeded. In this case the
             * user can only download the link by importing it into his account and downloading it "from there".
             */
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        String dllink;
        if (downloadLink.getDownloadURL().matches(TYPE_VIDEO)) {
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
            br.getPage("http://static.video.yandex.net/get-token/" + linkpart + "?nc=0." + System.currentTimeMillis());
            final String token = br.getRegex("<token>([^<>\"]*?)</token>").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("http://streaming.video.yandex.ru/get-location/" + linkpart + "/" + file + "?token=" + token + "&ref=video.yandex.ru");
            dllink = br.getRegex("<video\\-location>(http://[^<>\"]*?)</video\\-location>").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(dllink);
            resumable = true;
            maxchunks = 0;
        } else {
            final String hash = downloadLink.getStringProperty("hash_plain", null);
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final Browser br2 = br.cloneBrowser();
            br2.postPage("https://disk.yandex.com/secret-key.jsx", "");
            String ckey = br2.getRegex("\"([a-z0-9]+)\"").getMatch(0);
            if (ckey == null) {
                logger.info("Getting ckey via html code --> Could lead to problems");
                ckey = getCkey();
            }
            br.postPage("https://disk.yandex.com/handlers.jsx", "tld=com&_ckey=" + ckey + "&_name=getLinkFileDownload&hash=" + hash);
            if (br.containsHTML("\"title\":\"invalid ckey\"")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'invalid ckey'", 5 * 60 * 1000l);
            } else if (br.containsHTML("\"code\":69")) {
                /* Usually this does not happen. Happens also if you actually try to download a "premiumonly" link via this method. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\"code\":88")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 88 'Decryption error'", 5 * 60 * 1000l);
            }
            dllink = parse("url", this.br);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.startsWith("//")) {
                dllink = "http:" + dllink;
            }
            /* Don't do htmldecode because the link will be invalid then */
            dllink = HTMLEntities.unhtmlentities(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors(downloadLink);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getCkey() throws PluginException {
        final String ckey = br.getRegex("\"ckey\":\"([^\"]+)\"").getMatch(0);
        if (ckey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ckey;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBr();
                setConstants(account);
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
                br.getPage("https://disk.yandex.com/?auth=1");
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
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("https://beta.disk.yandex.com/client/disk/Downloads/");
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
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account);
        ACCOUNT_SK = account.getStringProperty("saved_sk", null);
        requestFileInformation(link);
        login(this.currAcc, false);
        String dllink = checkDirectLink(link, "directlink_account");

        if (dllink == null) {
            br.getPage(getMainLink(link));
            final String hash = link.getStringProperty("hash_plain", null);
            /* This should never happen */
            if (ACCOUNT_SK == null) {
                logger.warning("ACCOUNT_SK is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /*
             * TODO: Maybe force this handling if a link is only downloadable via account because the public overall download limit (traffic
             * limit) is exceeded. In this case the user can only download the link by importing it into his account and downloading it
             * "from there".
             */
            if (this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, false)) {
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
                        postPage("https://disk.yandex.com/models/?_m=do-save-resource-public", "_model.0=do-save-resource-public&id.0=%2Fpublic%2F" + Encoding.urlEncode(hash) + "&async.0=0&idClient=" + this.CLIENT_ID + "&version=" + this.VERSION + "&sk=" + ACCOUNT_SK);
                        /* TODO: Maybe add/find a way to verify if the file really has been moved to the account. */
                        if (br.containsHTML("\"code\":85")) {
                            logger.info("No free space available, failed to move file to account");
                            throw new PluginException(LinkStatus.ERROR_FATAL, "No free space available, failed to move file to account");
                        }
                        file_moved = true;
                        link.setProperty("file_moved", true);
                        dllink = getLinkFromFileInAccount(link, br);
                        if (dllink == null) {
                            /*
                             * Possible errors (which should never occur:
                             * "id":"HTTP_404 == File could not be found in the account --> Probably move handling failed or is broken"
                             */
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (this.getPluginConfig().getBooleanProperty(DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, false)) {
                            try {
                                logger.info("Successfully grabbed dllink via move_to_Account_handling -> Move file to trash -> Trying to delete file from account");
                                moveFileToTrash(link);
                                logger.info("Successfully grabbed dllink via move_to_Account_handling -> Empty trash -> Trying to empty trash inside account");
                                emptyTrash();
                            } catch (final Throwable e) {
                            }
                        }
                    }

                } catch (final PluginException e) {
                    if (file_moved) {
                        logger.info("MoveToAccount download-handling failed (dllink == null) -> Deleting moved file and emptying trash, then falling back to free download handling");
                        moveFileToTrash(link);
                        emptyTrash();
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                logger.info("MoveToAccount handling is inactive -> Starting free account download handling");
                br.getPage(getMainLink(link));
                br.postPage("https://disk.yandex.ru/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=%2Fpublic%2F" + Encoding.urlEncode(hash) + "&idClient=" + this.CLIENT_ID + "&version=" + this.VERSION + "&sk=" + this.ACCOUNT_SK);
                dllink = br.getRegex("\"file\":\"(http[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }

        boolean resume = ACCOUNT_FREE_RESUME;
        if (link.getBooleanProperty(DiskYandexNet.NORESUME, false)) {
            logger.info("Resume is disabled for this try");
            resume = false;
            link.setProperty(DiskYandexNet.NORESUME, Boolean.valueOf(false));
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, ACCOUNT_FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors(link);
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directlink_account", dllink);
        dl.startDownload();
    }

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
        final String urlencodedfname = Encoding.urlTotalEncode(dl.getName());
        String dllink = null;
        for (final String downloaddir : downloaddirs) {
            try {
                br.setFollowRedirects(false);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Referer", "https://disk.yandex.com/client/disk/Downloads");
                postPage("https://disk.yandex.com/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=%2Fdisk%2F" + downloaddir + "%2F" + urlencodedfname + "&idClient=" + this.CLIENT_ID + "&version=" + this.VERSION + "&sk=" + this.ACCOUNT_SK);
                /* 28 = file not found, 70 = folder not found */
                if (br.containsHTML("\"code\":28") || br.containsHTML("\"code\":70")) {
                    logger.info("getLinkFromFileInAccount: Moved file was not found in directory: " + downloaddir);
                    continue;
                }
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    dllink = br.getRegex("\"file\":\"(http[^<>\"]*?)\"").getMatch(0);
                }
                br.setFollowRedirects(true);
                /* Fix links - cookies sit on the other domain */
                if (dllink != null) {
                    dllink = dllink.replace("disk.yandex.ru/", "disk.yandex.com/");
                    dllink = dllink.replace("\\", "");
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
        for (final String downloaddir : downloaddirs) {
            try {
                postPage("https://disk.yandex.com/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=%2Fdisk%2F" + downloaddir + "%2F" + Encoding.urlEncode(dl.getName()) + "&idClient=" + CLIENT_ID + "&sk=" + this.ACCOUNT_SK + "&version=" + VERSION);
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

    private String parse(final String var, final Browser srcbr) {
        if (var == null) {
            return null;
        }
        String result = srcbr.getRegex("<" + var + ">([^<>\"]*?)</" + var + ">").getMatch(0);
        if (result == null) {
            result = srcbr.getRegex("\"" + var + "\":\"([^\"]+)").getMatch(0);
        }
        return result;
    }

    private void prepBr() {
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private void getPage(final String url) throws Exception {
        br.getPage(url);
        if (br.containsHTML("\"id\":\"WRONG_SK\"")) {
            logger.info("Refreshing ACCOUNT_SK");
            this.login(this.currAcc, true);
        }
    }

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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DOWNLOAD_ZIP, JDL.L("plugins.hoster.DiskYandexNet.DownloadZip", "Folders: Download .zip file of all files in the folder?")).setDefaultValue(false));
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