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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "disk.yandex.net" }, urls = { "http://yandexdecrypted\\.net/\\d+" }, flags = { 2 })
public class DiskYandexNet extends PluginForHost {

    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://passport.yandex.ru/passport?mode=register&from=cloud&retpath=https%3A%2F%2Fdisk.yandex.ru%2F%3Fauth%3D1&origin=face.en");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://disk.yandex.net/";
    }

    /* Settings values */
    private static final String   MOVE_FILES_TO_ACCOUNT              = "MOVE_FILES_TO_ACCOUNT";
    private static final String   MOVE_FILES_TO_TRASH_AFTER_DOWNLOAD = "MOVE_FILES_TO_TRASH_AFTER_DOWNLOAD";
    private static final String   EMPTY_TRASH_AFTER_DOWNLOAD         = "EMPTY_TRASH_AFTER_DOWNLOAD";

    /* Some contants which they used in browser */
    private static final String   CLIENT_ID                          = "24f549192f9f2fac2d80c71dd7969442";
    private static final String   VERSION                            = "1.0.20";
    private static final String   STANDARD_FREE_SPEED                = "64 kbit/s";

    /* Connection limits */
    private static final boolean  FREE_RESUME                        = false;
    private static final int      FREE_MAXCHUNKS                     = 1;
    private static final boolean  ACCOUNT_RESUME                     = true;
    private static final int      ACCOUNT_MAXCHUNKS                  = 0;

    /* Domain & other login stuff */
    private static final String   MAIN_DOMAIN                        = "https://yandex.com";
    private static final String[] domains                            = new String[] { "https://yandex.ru", "https://yandex.com", "https://disk.yandex.ru/", "https://disk.yandex.com/", "https://disk.yandex.net/" };
    private static Object         LOCK                               = new Object();

    /* Important constant which seems to be unique for every account. It's needed for most of the requests when logged in. */
    private String                ACCOUNT_SK                         = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (!link.getDownloadURL().matches("http://yandexdecrypted\\.net/\\d+")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        setBrowserExclusive();
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setFollowRedirects(true);
        br.getPage(link.getStringProperty("mainlink", null));
        if (br.containsHTML("(<title>The file you are looking for could not be found\\.|>Nothing found</span>|<title>Nothing found \\— Yandex\\.Disk</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = link.getStringProperty("plain_filename", null);
        final String filesize = link.getStringProperty("plain_size", null);

        link.setName(filename);
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        checkFeatureDialog();
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, null);
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, String ckey) throws Exception, PluginException {
        final String hash = downloadLink.getStringProperty("hash_plain", null);
        if (ckey == null) ckey = getCkey();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("https://disk.yandex.com/handlers.jsx", "_ckey=" + ckey + "&_name=getLinkFileDownload&hash=" + Encoding.urlEncode(hash));
        String dllink = parse("url", this.br);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.startsWith("//")) dllink = "http:" + dllink;
        /* Don't do htmldecode because the link will be invalid then */
        dllink = dllink.replace("amp;", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getCkey() throws PluginException {
        final String ckey = br.getRegex("\"ckey\":\"([^\"]+)\"").getMatch(0);
        if (ckey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return ckey;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBr();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ACCOUNT_SK = account.getStringProperty("saved_sk", null);
        if (ACCOUNT_SK == null) {
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
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        String dllink = checkDirectLink(link, "directlink_account");

        if (dllink == null) {
            br.getPage(link.getStringProperty("mainlink", null));
            final String hash = link.getStringProperty("hash_plain", null);
            final String ckey = getCkey();
            ACCOUNT_SK = account.getStringProperty("saved_sk", null);
            /* This should never happen */
            if (ACCOUNT_SK == null) {
                logger.warning("ACCOUNT_SK is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, false)) {
                boolean file_moved = link.getBooleanProperty("file_moved", false);
                try {
                    logger.info("MoveToAccount handling is active -> Starting account download handling");
                    final Browser br2 = br.cloneBrowser();
                    br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    if (file_moved) {
                        logger.info("Seems like the file has already been moved to the account -> Trying to get it");
                        dllink = getLinkFromFileInAccount(link, br2);
                        if (dllink == null) {
                            logger.info("Seems like the file is not in the account -> Trying to move it");
                            file_moved = false;
                            link.setProperty("file_moved", false);
                        }
                    }
                    if (dllink == null) {
                        br2.postPage("https://disk.yandex.com/handlers.jsx", "_ckey=" + ckey + "&_name=copyToSelf&hash=" + Encoding.urlEncode(hash) + "&source=copy-public");
                        if (!br2.containsHTML("\"success\":true")) {
                            if (br2.containsHTML("\"code\":85")) {
                                logger.info("No free space available, failed to move file to account");
                                throw new PluginException(LinkStatus.ERROR_FATAL, "No free space available, failed to move file to account");
                            }
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        file_moved = true;
                        link.setProperty("file_moved", true);
                        dllink = getLinkFromFileInAccount(link, br2);
                        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }

                } catch (final PluginException e) {

                    if (file_moved) {
                        logger.info("MoveToAccount handling failed -> Deleting moved file and emptying trash, then falling back to free download handling");
                        moveFileToTrash(link);
                        emptyTrash();
                    }

                    logger.info("MoveToAccount handling failed -> Falling back to free download handling");
                    doFree(link, ACCOUNT_RESUME, ACCOUNT_MAXCHUNKS, ckey);
                    return;
                }

                br.setFollowRedirects(false);
                br.getPage(dllink);
                dllink = br.getRedirectLocation();
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                logger.info("MoveToAccount handling is inactive -> Starting free download handling");
                doFree(link, ACCOUNT_RESUME, ACCOUNT_MAXCHUNKS, ckey);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directlink_account", dllink);
        if (this.dl.startDownload() && this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_TRASH_AFTER_DOWNLOAD, false)) {
            try {
                logger.info("Download successful -> DELETE_MOVED_FILES_AFTER_DOWNLOAD handling is active -> Trying to delete file from account");
                moveFileToTrash(link);
            } catch (final Throwable e) {
            }
            try {
                if (this.getPluginConfig().getBooleanProperty(EMPTY_TRASH_AFTER_DOWNLOAD, false)) {
                    logger.info("Download successful -> EMPTY_TRASH_AFTER_DOWNLOAD handling is active -> Trying to empty trash inside account");
                    emptyTrash();
                }
            } catch (final Throwable e) {
            }
        }
    }

    private String getLinkFromFileInAccount(final DownloadLink dl, final Browser br2) {
        String dllink = null;
        try {
            br2.postPage("https://beta.disk.yandex.com/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=%2Fdisk%2FDownloads%2F" + Encoding.urlEncode(dl.getName()) + "&inline.0=true&idClient=" + CLIENT_ID + "&sk=" + ACCOUNT_SK + "&version=" + VERSION);
            dllink = parse("file", br2);
            /* Fix links - cookies sit on the other domain */
            if (dllink != null) dllink = dllink.replace("disk.yandex.ru/", "disk.yandex.com/");
        } catch (final Throwable e) {
        }
        return dllink;
    }

    private void moveFileToTrash(final DownloadLink dl) {
        try {
            br.postPage("https://beta.disk.yandex.com/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=%2Fdisk%2FDownloads%2F" + Encoding.urlEncode(dl.getName()) + "&idClient=" + CLIENT_ID + "&sk=" + ACCOUNT_SK + "&version=" + VERSION);
            logger.info("Successfully moved file to trash");
        } catch (final Throwable e) {
            logger.warning("Failed to move file to trash");
        }
    }

    private void emptyTrash() {
        try {
            br.postPage("https://beta.disk.yandex.com/models/?_m=do-clean-trash", "_model.0=do-clean-trash&idClient=" + CLIENT_ID + "&sk=" + ACCOUNT_SK + "&version=" + VERSION);
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
        return -1;
    }

    private String parse(final String var, final Browser srcbr) {
        if (var == null) return null;
        String result = srcbr.getRegex("<" + var + ">([^<>\"]*?)</" + var + ">").getMatch(0);
        if (result == null) result = srcbr.getRegex("\"" + var + "\":\"([^\"]+)").getMatch(0);
        return result;
    }

    private void prepBr() {
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
    }

    @Override
    public String getDescription() {
        return "JDownloader's Yandex.ru Plugin helps downloading files from Yandex.ru. It provides some settings for downloads via account.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Account settings:"));
        final ConfigEntry moveFilesToAcc = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DiskYandexNet.MOVE_FILES_TO_ACCOUNT, JDL.L("plugins.hoster.DiskYandexNet.MoveFilesToAccount", "1.Move files to account before downloading them to get higher download speeds?")).setDefaultValue(false);
        getConfig().addEntry(moveFilesToAcc);
        final ConfigEntry moveFilesToTrash = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DiskYandexNet.MOVE_FILES_TO_TRASH_AFTER_DOWNLOAD, JDL.L("plugins.hoster.DiskYandexNet.MoveFilesToTrashAfterSuccessfulDownload", "2.Move successfully downloaded files to trash after download?")).setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(false);
        getConfig().addEntry(moveFilesToTrash);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DiskYandexNet.EMPTY_TRASH_AFTER_DOWNLOAD, JDL.L("plugins.hoster.DiskYandexNet.EmptyTrashAfterSuccessfulDownload", "3.Empty trash after successful download?\r\n[Can only be used if setting #1 is active!]")).setEnabledCondidtion(moveFilesToTrash, true).setDefaultValue(false));
    }

    private void checkFeatureDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog_Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog_Shown2") == null) {
                    showFeatureDialogAll();
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

    private static void showFeatureDialogAll() {
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}