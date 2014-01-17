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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

/** Works exactly like putlocker.com */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sockshare.com" }, urls = { "http://(www\\.)?sockshare.com/(mobile/)?(file|embed)/[A-Z0-9]+" }, flags = { 2 })
public class SockShareCom extends PluginForHost {

    private static final String MAINPAGE          = "http://sockshare.com";
    private static final String SERVERUNAVAILABLE = "(>This content server has been temporarily disabled for upgrades|Try again soon\\. You can still download it below\\.<)";
    private static Object       LOCK              = new Object();
    private String              agent             = null;
    private static final String NOCHUNKS          = "NOCHUNKS";
    /** The list of quality values displayed to the user */
    private final String[]      servers           = new String[] { "Prefer Original format (bigger size, better quality)", "Prefer Stream format [.flv] (smaller size, less quality)" };
    private final String        formats           = "formats";

    public SockShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.sockshare.com/gopro.php");
        setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/mobile", "").replace("/embed/", "/file/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.sockshare.com/page.php?terms";
    }

    public void prepBrowser() {
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("sockshare.com/?404") || br.containsHTML("(>404 Not Found<|>This file doesn\\'t exist, or has been removed|<title>Share Files Easily on SockShare</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<h1>(.*?)<strong>\\( (.*?) \\)</strong></h1>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\| SockShare</title>").getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) {
            if (br.containsHTML(">You have exceeded the daily stream limit for your country")) return AvailableStatus.UNCHECKABLE;
            if (br.containsHTML("You can wait until tomorrow, or get a")) return AvailableStatus.UNCHECKABLE;
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!new Regex(filename, "\\.[0-9a-z]{0,4}$").matches()) filename = filename + ".flv";
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML("(>You have exceeded the daily stream limit for your country|You can wait until tomorrow, or get a)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have exceeded the daily download limit for your country", 4 * 60 * 60 * 1000l);
        Form freeform = getFormByHTML("value=\"Continue as Free User\"");
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        freeform.put("confirm", "Continue+as+Free+User");
        if (freeform.containsHTML("/include/captcha")) {
            final String captchaIMG = getCaptchaIMG();
            if (captchaIMG == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final String captcha = getCaptchaCode(captchaIMG.replace("&amp;", "&"), downloadLink);
            if (captcha != null) freeform.put("captcha_code", Encoding.urlEncode(captcha));
        }
        /** Can still be skipped */
        // String waittime =
        // br.getRegex("var countdownNum = (\\d+);").getMatch(0);
        // int wait = 5;
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        br.submitForm(freeform);
        if (br.containsHTML("This file failed to convert")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Download only works with an account", PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                /* not existing in old stable */
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Download only works with an account");
        }
        if (br.containsHTML(SERVERUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server temporarily disabled!", 2 * 60 * 60 * 1000l);
        if (br.containsHTML("(>You have exceeded the daily stream limit for your country|You can wait until tomorrow, or get a)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have exceeded the daily download limit for your country", 4 * 60 * 60 * 1000l);
        String passCode = downloadLink.getStringProperty("pass");
        if (br.containsHTML("This file requires a password\\. Please enter it")) {
            br.setFollowRedirects(true);
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            br.postPage(br.getURL(), "file_password=" + Encoding.urlEncode(passCode));
            if (br.containsHTML(">This password is not correct")) {
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            }
            br.setFollowRedirects(false);
        }
        final String dllink = getDllink(downloadLink);
        int chunks = 0;
        if (downloadLink.getBooleanProperty(SockShareCom.NOCHUNKS, false)) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("pass", passCode);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            /* unknown error, we disable multiple chunks */
            if (downloadLink.getBooleanProperty(SockShareCom.NOCHUNKS, false) == false) {
                downloadLink.setProperty(SockShareCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            throw e;
        }
        br.getPage("http://www.sockshare.com/profile.php?pro");
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<td>Expiring </td>[\t\n\r ]+<td>([A-Za-z]+ \\d+, \\d{4} at \\d{2}:\\d{2})</td>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.replace("at ", ""), "MMMM dd, yyyy hh:mm", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    private void login(final Account account, final boolean fetchInfo) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                prepBrowser();
                br.getHeaders().put("Accept-Charset", null);
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean cookiesSet = false;
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof Map<?, ?>) {
                    final Map<String, String> cookies = (Map<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                            cookiesSet = true;
                        }
                    }
                }
                if (!fetchInfo && cookiesSet) return;
                String proActive = null;
                if (cookiesSet) {
                    br.getPage("http://www.sockshare.com/profile.php?pro");
                    proActive = br.getRegex("Pro  ?Status</?[^>]+>[\r\n\t ]+<[^>]+>(Active)").getMatch(0);
                    if (proActive == null) {
                        logger.severe("No longer Pro-Status, try to fetch new cookie!\r\n" + br.toString());
                    } else {
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://www.sockshare.com/authenticate.php?login");
                Form login = br.getForm(0);
                if (login == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("captcha.php\\?")) {
                    String captchaIMG = getCaptchaIMG();
                    if (captchaIMG == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "sockshare.com", "http://sockshare.com", true);
                    final String captcha = getCaptchaCode(captchaIMG.replace("&amp;", "&"), dummyLink);
                    if (captcha != null) login.put("captcha_code", Encoding.urlEncode(captcha));
                }
                login.put("user", Encoding.urlEncode(account.getUser()));
                login.put("pass", Encoding.urlEncode(account.getPass()));
                login.put("remember", "1");
                br.submitForm(login);
                // no auth = not logged / invalid account.
                if (br.getCookie(MAINPAGE, "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha wrong!\r\nUngültiger Benutzername oder ungültiges Passwort oder ungültiges login Captcha!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                // finish off more code here
                br.getPage("http://www.sockshare.com/profile.php?pro");
                proActive = br.getRegex("Pro  ?Status</?[^>]+>[\r\n\t ]+<[^>]+>(Active)").getMatch(0);
                if (br.containsHTML("<td>Free Account \\- <strong><a href=\"/gopro\\.php\\?upgrade\"")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported for this host!\r\nFree Accounts werden für diesen Hoster nicht unterstützt!", PluginException.VALUE_ID_PREMIUM_DISABLE); }
                if (proActive == null) {
                    logger.severe(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnknown accounttype!\r\nUnbekannter Accounttyp!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
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
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(SERVERUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server temporarily disabled!", 2 * 60 * 60 * 1000l);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("\"(/get_file\\.php\\?id=[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("&amp;", "&");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://www.sockshare.com" + Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /** Same code for putlocker.com and sockshare.com START */
    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(formats, -1)) {
        case 0:
            logger.fine("Original format is configured");
            return 0;
        case 1:
            logger.fine("Stream format is configured");
            return 1;
        default:
            logger.fine("No format is cunfigured, returning default format (original format)");
            return 0;
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), formats, servers, JDL.L("plugins.host.putlockerandsocksharecom.preferredformats", "Format selection - select your prefered format:\r\nBy default, JDownloader will download the original format if possible.\r\nIf the desired format isn't available, JDownloader will download the other one.\r\n\rPremium users can only download the original format.")).setDefaultValue(0));
    }

    private String getDllink(final DownloadLink downloadLink) throws IOException, PluginException {
        String dllink = null;
        final int selectedFormat = getConfiguredServer();
        if (selectedFormat == 0) {
            dllink = getOriginalFormatLink();
            if (dllink == null) {
                logger.info("Failed to find chosen format");
                dllink = getStreamLink();
            }
        } else {
            dllink = getStreamLink();
            if (dllink == null) {
                logger.info("Failed to find chosen format");
                dllink = getOriginalFormatLink();
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return dllink.replace("&amp;", "&");
    }

    private String getStreamLink() throws IOException, PluginException {
        String dllink = br.getRegex("\\'(/get_file\\.php\\?stream=[^<>\"/]*?)\\'").getMatch(0);
        if (dllink != null) {
            br.getPage("http://www." + this.getHost() + dllink);
            dllink = br.getRegex("<media:content url=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    private String getOriginalFormatLink() throws IOException, PluginException {
        String dllink = br.getRegex("\"(/get_file\\.php\\?id=[^<>\"/]*?)\"").getMatch(0);
        if (dllink != null) {
            br.getPage("http://www." + this.getHost() + dllink);
            dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    private String getCaptchaIMG() {
        return br.getRegex("<img src=\"(/include/captcha\\.php\\?[^<>\"]+)\".*?/>").getMatch(0);
    }

    private void fixFilename(final DownloadLink downloadLink) {
        String oldName = downloadLink.getFinalFileName();
        if (oldName == null) oldName = downloadLink.getName();
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        String newExtension = null;
        // some streaming sites do not provide proper file.extension within
        // headers (Content-Disposition or the fail over getURL()).
        if (serverFilename.contains(".")) {
            newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
        } else {
            logger.info("HTTP headers don't contain filename.extension information");
        }
        if (newExtension != null && !oldName.endsWith(newExtension)) {
            String oldExtension = null;
            if (oldName.contains(".")) oldExtension = oldName.substring(oldName.lastIndexOf("."));
            if (oldExtension != null && oldExtension.length() <= 5)
                downloadLink.setFinalFileName(oldName.replace(oldExtension, newExtension));
            else
                downloadLink.setFinalFileName(oldName + newExtension);
        }
    }

    /** Same code for putlocker.com and sockshare.com END */
    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private Form getFormByHTML(final String regex) {
        Form[] workaround = br.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                if (f.containsHTML(regex)) return f;
            }
        }
        return null;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}