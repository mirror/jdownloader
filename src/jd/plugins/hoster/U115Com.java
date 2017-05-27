//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;
import org.appwork.utils.formatter.SizeFormatter;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u.115.com" }, urls = { "http://(www\\.)?(u\\.)?115\\.com/file/[a-z0-9]+" })
public class U115Com extends PluginForHost {

    private final String        ua                                 = RandomUserAgent.generate();
    private static final String MAINPAGE                           = "http://115.com/";
    private static final String UNDERMAINTENANCEURL                = "http://u.115.com/weihu.html";
    private static final String UNDERMAINTENANCETEXT               = "The servers are under maintenance";
    private static final String ACCOUNTNEEDEDUSERTEXT              = "Account is needed to download this link";
    private static Object       LOCK                               = new Object();
    private boolean             downloadCompleted                  = false;
    private String              DELETEFILEFROMACCOUNTAFTERDOWNLOAD = "DELETEFILEFROMACCOUNTAFTERDOWNLOAD";

    public U115Com(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("u.115.com/file/", "115.com/file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals(UNDERMAINTENANCEURL)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.U115Com.undermaintenance", UNDERMAINTENANCETEXT));
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("(id=\"pickcode_error\">很抱歉，文件不存在。</div>|很抱歉，文件不存在。|>很抱歉，该文件提取码不存在。<|<title>115网盘\\|网盘\\|115,我的网盘\\|免费网络硬盘 \\- 爱分享，云生活</title>|/resource\\?r=404|>视听类文件暂时不支持分享，给您带来的不便深表歉意。<)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"/]*?) · 互联我</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"Download\"></a><a id=\"Download(.*?)\"></a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("file_name: \\'(.*?)\\',").getMatch(0);
            }
        }
        String filesize = br.getRegex("文件大小：(.*?)<div class=\"share-url\"").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("u6587\\\\u4ef6\\\\u5927\\\\u5c0f\\\\uff1a(.*?)\\\\r\\\\n\\\\").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("file_size: \\'(.*?)\\'").getMatch(0);
                if (filesize == null) {
                    filesize = br.getRegex("<li>文件大小：(.*?)</li>").getMatch(0);
                }
            }
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filesize = filesize.replace(",", "");
        link.setProperty("plainfilename", filename);
        link.setFinalFileName(Encoding.htmlDecode(filename));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        parseSHA1(link, br);
        if (AccountController.getInstance().getValidAccount(this) == null) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.u115com.only4registered", ACCOUNTNEEDEDUSERTEXT));
        }
        return AvailableStatus.TRUE;
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://u.115.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void parseSHA1(DownloadLink link, Browser br) {
        String sh1 = br.getRegex("<li>SHA1：(.*?) <a href=\"").getMatch(0);
        if (sh1 == null) {
            sh1 = br.getRegex("sha1: \"(.*?)\",").getMatch(0);
        }
        if (sh1 != null && sh1.matches(("^[a-fA-F0-9]+$"))) {
            link.setSha1Hash(sh1.trim());
        } else {
            link.setSha1Hash(null);
        }
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) {
                return;
            }
            br.setReadTimeout(2 * 60 * 1000);
            br.setCookie("http://115.com/", "lang", "zh");
            br.getHeaders().put("User-Agent", ua);
            br.setCustomCharset("utf-8");
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (true) {
            throw new PluginException(LinkStatus.ERROR_FATAL, ACCOUNTNEEDEDUSERTEXT);
        }
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
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
            br.setFollowRedirects(true);
            br.getPage("http://passport.115.com/?ct=login&ac=ajax&is_ssl=1");
            String key = "abcdef0123456789";
            String vcode = key.toUpperCase();
            String sh1pw = JDHash.getSHA1(account.getPass());
            String sh1user = JDHash.getSHA1(account.getUser());

            // final byte[] md5bytespw = MessageDigest.getInstance("MD5").digest(sh1pw.getBytes());
            // final byte[] md5bytesuser = MessageDigest.getInstance("MD5").digest(sh1user.getBytes());
            // final byte[] concatBytes = appendData(md5bytespw, md5bytesuser);
            //
            // String one = SHAsum(concatBytes);
            // String two = new String(MessageDigest.getInstance("MD5").digest(one.getBytes())) + vcode;
            // String three = JDHash.getSHA1(two);
            // String four = new String(MessageDigest.getInstance("MD5").digest(three.getBytes()));

            // String post_data = "login[ssoent]=B1&login[version]=2.0&login[ssoext]=" + key + "&login[ssoln]=" + account.getUser() +
            // "&login[ssopw]=" + four + "&login[ssovcode]=" + key +
            // "&login[safe]=1&login[time]=1&login[safe_login]=0&login[goto]=http://www.115.com/";
            // br.postPageRaw("http://passport.115.com/?ct=login&ac=ajax&is_ssl=1", post_data);

            // sh1pw = JDHash.getSHA1(Encoding.UTF8Encode(account.getPass()));
            // sh1user = JDHash.getSHA1(Encoding.UTF8Encode(account.getUser()));
            // sh1pw = toHex(sh1pw.getBytes());
            // sh1user = toHex(sh1user.getBytes());
            //
            // one = JDHash.getSHA1(Encoding.UTF8Encode(sh1pw + sh1user));
            // two = toHex(one.getBytes());
            // three = JDHash.getSHA1(Encoding.UTF8Encode(two));
            // four = toHex(three.getBytes());
            // post_data = "login[ssoent]=A1&login[version]=2.0&login[ssoext]=" + key + "&login[ssoln]=" + account.getUser() +
            // "&login[ssopw]=" + four + "&login[ssovcode]=" + key +
            // "&login[safe]=1&login[time]=0&login[safe_login]=0&login[goto]=http://www.115.com/";
            // br.postPageRaw("http://passport.115.com/?ct=login&ac=ajax&is_ssl=1", post_data);
            // post_data = "login[ssoent]=A1&login[version]=2.0&login[ssoext]=" + key + "&login[ssoln]=" + account.getUser() +
            // "&login[ssopw]=" + four + "&login[ssovcode]=" + key +
            // "&login[safe]=1&login[time]=0&login[safe_login]=0&login[goto]=http://www.115.com/";
            // br.postPageRaw("http://passport.115.com/?ct=login&ac=ajax&is_ssl=1", post_data);
            // String wtf = unescape(br.toString());

            if (br.getCookie(MAINPAGE, "OOFL") == null || br.containsHTML("\"err_code\":")) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    // /* http://stackoverflow.com/questions/5683486/how-to-combine-two-byte-arrays */
    // protected byte[] appendData(byte[] firstObject, byte[] secondObject) {
    // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // try {
    // if (firstObject != null && firstObject.length != 0) {
    // outputStream.write(firstObject);
    // }
    // if (secondObject != null && secondObject.length != 0) {
    // outputStream.write(secondObject);
    // }
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // return outputStream.toByteArray();
    // }
    //
    // public static String toHex(byte[] a) {
    // StringBuilder sb = new StringBuilder(a.length * 2);
    // for (int i = 0; i < a.length; i++) {
    // sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
    // sb.append(Character.forDigit(a[i] & 0x0f, 16));
    // }
    // return sb.toString();
    // }
    //
    // public static String SHAsum(byte[] convertme) throws NoSuchAlgorithmException {
    // MessageDigest md = MessageDigest.getInstance("SHA-1");
    // return byteArray2Hex(md.digest(convertme));
    // }
    //
    // private static String byteArray2Hex(final byte[] hash) {
    // Formatter formatter = new Formatter();
    // for (byte b : hash) {
    // formatter.format("%02x", b);
    // }
    // return formatter.toString();
    // }

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
        ai.setUnlimitedTraffic();
        account.setValid(true);
        /**
         * Doesn't have a premium login, plugin is only designed for free accounts!
         */
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        if (!br.containsHTML("onclick=\"MoveMyFile\\.Show\\(true\\);")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is not downloadable");
        }
        final boolean plugin_broken = true;
        if (plugin_broken) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String plainfilename = link.getStringProperty("plainfilename", null);
        final String userID = br.getRegex("user_id:   \\'(\\d+)\\'").getMatch(0);
        final String fileID = br.getRegex("file_id:   \\'(\\d+)\\'").getMatch(0);
        if (userID == null || fileID == null || plainfilename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // Check if we find the new id, maybe the file has already been
        // added to
        // the account
        String[] fileIDs = findIdsByFilename(plainfilename);
        if (fileIDs[1] == null) {
            // Add file to account
            br.getPage("http://115.com/?ct=pickcode&ac=collect&user_id=" + userID + "&tid=" + fileID + "&is_temp=0&aid=1&cid=0");
            if (!br.containsHTML("\"state\":true")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            fileIDs = findIdsByFilename(plainfilename);
            if (fileIDs[1] == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        // Finally...download it
        br.getPage("http://115.com/?ct=pickcode&ac=download&pickcode=" + fileIDs[1] + "&_t=" + System.currentTimeMillis());
        String dllink = findLink();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
            try {
                if (!dl.externalDownloadStop()) {
                    this.downloadCompleted = true;
                }
            } catch (final Throwable e) {
            }
        } catch (Exception e) {
            this.downloadCompleted = false;
            throw e;
        } finally {
            if (this.downloadCompleted && this.getPluginConfig().getBooleanProperty(DELETEFILEFROMACCOUNTAFTERDOWNLOAD, false)) {
                logger.info("Download finished, trying to delete the file from the account...");
                boolean fileDeleted = true;
                try {
                    br.postPage("http://115.com/?ct=file&ac=delete", "aid=1&pid=0&tid=" + fileIDs[0]);
                } catch (final Exception e) {
                    fileDeleted = false;
                }
                if (!br.containsHTML("\"data\":\\{\"f\":true\\}")) {
                    fileDeleted = false;
                }
                if (fileDeleted) {
                    logger.info("File successfully deleted from account...");
                } else {
                    logger.info("Warning, file could not be deleted from account...");
                }
            }
        }
        // TODO: Maybe delete file from account after successfull download?!
    }

    private String[] findIdsByFilename(final String plainfilename) throws IOException {
        // Access filelist in account
        // http://web.api.115.com/files?aid=1&cid=0&offset=0&limit=50&show_dir=1&o=user_ptime&asc=0&nf=1&qid=0&source=&format=json
        // br.getPage("http://115.com//?ct=file&ac=userfile&ajax=1&qid=&select=1&nf=1&filter=&select_dir=0&aid=1&cid=0&nsf=&_t=" +
        // System.currentTimeMillis());
        // br.getPage("http://web.api.115.com/files?aid=1&cid=0&o=user_ptime&asc=0&offset=0&show_dir=1&limit=40&code=&scid=&snap=0&natsort=1&source=&format=json");
        final String[] fileIDs = new String[2];
        final String dataText = br.getRegex("\"data\":\\[(.*?\\})\\]").getMatch(0);
        if (dataText == null) {
            return fileIDs;
        }
        // Search new ID of the added file via filename
        final String[] files = dataText.split("\\}");
        for (final String file : files) {
            String currentFilename = new Regex(file, "\"n\":\"([^<>\"]*?)\"").getMatch(0);
            if (currentFilename == null) {
                continue;
            }
            currentFilename = Encoding.unicodeDecode(currentFilename);
            if (currentFilename.equals(plainfilename)) {
                fileIDs[0] = new Regex(file, "\"fid\":\"([a-z0-9]+)\"").getMatch(0);
                if (fileIDs[0] == null) {
                    fileIDs[0] = new Regex(file, "\"fid\":([0-9]+)").getMatch(0);
                }
                fileIDs[1] = new Regex(file, "\"pc\":\"([a-z0-9]+)\"").getMatch(0);
                break;
            }
        }
        return fileIDs;
    }

    public String findLink() throws Exception {
        String linkToDownload = br.getRegex("\"(https?://(\\d+\\.\\d+\\.\\d+\\.\\d+|[a-z0-9\\.]+\\.115\\.com)/gdown_group[^<>\"]*?)\"").getMatch(0);
        return linkToDownload;
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DELETEFILEFROMACCOUNTAFTERDOWNLOAD, JDL.L("plugins.hoster.u115com.deleteFileFromAccountAfterSuccessfulDownload", "Delete file from account after successful download.")).setDefaultValue(false));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

}