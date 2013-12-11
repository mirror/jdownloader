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

import java.io.File;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extabit.com" }, urls = { "http://(www\\.)?(u\\d+\\.extabit\\.com/go/[a-z0-9]{13}|extabit\\.com/file(_[^/]+)?/[a-z0-9]{13}(/[A-Za-z0-9]+/[^<>\"/]*?\\.htm)?)" }, flags = { 2 })
public class ExtaBitCom extends PluginForHost {

    private static final String  NOTAVAILABLETEXT = "(>File is temporary unavailable<|temporary unavailable<br/>)";
    private static final String  NOMIRROR         = ">No download mirror<";
    private static final String  PREMIUMONLY      = "(>Only premium users can download (this file|files of this size\\. <)|<h2>The file that you're trying to download is larger than \\d+ (Mb|Gb).*?Upgrade to premium</b>.*?<h2>)";

    private static final String  SKIPFREELIMITS   = "SKIPFREELIMITS";
    private static AtomicInteger maxFree          = new AtomicInteger(1);
    private static final String  NOCHUNKS         = "NOCHUNKS";

    // extabit api specs http://code.google.com/p/extabit-api/wiki/APIDocumentation

    public ExtaBitCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://extabit.com/premium.jsp");
        // Too many downloads in a short time = 503 errors for premiumusers
        this.setStartIntervall(5 * 1000l);
        this.setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().contains("/go/")) {
            br.setFollowRedirects(false);
            String finallink = null;
            try {
                br.getPage(link.getDownloadURL());
            } catch (Exception e) {

            }
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = "http://extabit.com/file/" + System.currentTimeMillis();
            link.setUrlDownload(finallink);
        } else if (link.getDownloadURL().matches(".+/file_[^/]+/.+")) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("/file_[^/]+/", "/file/"));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        /*
         * They got a Mass-Linkchecker, but it's only available for registered users and doesn't show the filesize:
         * http://extabit.com/linkchecker.jsp
         */
        // To get the english version of the page
        br.setCookie("http://extabit.com", "language", "en");
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        try {
            br.getPage(downloadLink.getDownloadURL());
        } catch (final UnknownHostException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } catch (final ConnectException ec) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(File not found|Such file doesn\\'t exsist)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)download Extabit.com \\- file hosting</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("download_filename\".*?>(.*?)</div").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("extabit\\.com/file/.*?'>(.*?)</a>").getMatch(0);
                if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        // merged 18872 and 18775
        // 18872
        String filesize = br.getRegex("Size: ([^<>\"]*?)</div>").getMatch(0);
        // 18775
        if (filesize == null) filesize = br.getRegex("class=\"download_filesize(_en)\">.*?\\[(.*?)\\]").getMatch(1);
        if (filesize == null) filesize = br.getRegex("Size:.*?class=\"col-fileinfo\">(.*?)</").getMatch(0);
        if (filename.startsWith("file ")) {
            String newFilename = br.getRegex("df_html_link\" name=\"df_html_link\" class.*?>(.*?)</").getMatch(0);
            if (newFilename != null && newFilename.length() > filename.length()) {
                filename = newFilename;
            }
        }
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML(PREMIUMONLY))
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ExtaBitCom.errors.Only4Premium", "This file is only available for premium users"));
        else if (br.containsHTML(NOTAVAILABLETEXT))
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ExtaBitCom.errors.TempUnavailable", "This file is temporary unavailable"));
        else if (br.containsHTML(NOMIRROR)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ExtaBitCom.errors.NoMirror", "Extabit error: \"No download mirror\", contact extabit support."));
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://extabit.com/");
        // merged 18872 & 18775
        // 18775
        String expire = br.getRegex("Premium is active till <span class=\"green\"><strong>(.*?)</strong>").getMatch(0);
        if (expire == null) expire = br.getRegex("Premium is active till ([\\d\\.]+) ").getMatch(0);
        // 18872
        if (expire == null) expire = br.getRegex("Storage valid until (\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);

        final String downloadsLeft = br.getRegex("You have <strong>(\\d+)</strong> downloads").getMatch(0);

        if (downloadsLeft != null) {
            ai.setStatus("Downloads left: " + downloadsLeft);
        } else if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy", null));
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Account invalid");
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://extabit.com/static/terms.jsp";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private static void showFreeDialog(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        String tab = "                        ";
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download";
                            message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = domain + " Free Download";
                            message = "You are using the " + domain + " Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkShowFreeDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                if (config.getProperty("premAdShown2") == null) {
                    File checkFile = JDUtilities.getResourceFile("tmp/extmp");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("extabit.com");
                    }
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("premAdShown", Boolean.TRUE);
                config.setProperty("premAdShown2", "shown");
                config.save();
            }
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        checkShowFreeDialog();
        int maxChunks = 1;
        boolean resume = false;
        String dllink = null;
        if (this.getPluginConfig().getBooleanProperty(ExtaBitCom.SKIPFREELIMITS)) dllink = br.getRegex("product_download_url=(http[^<>\"]*?)(\\'|\")").getMatch(0);
        if (dllink != null) {
            maxFree.set(20);
            maxChunks = -10;
            resume = true;
            br.getHeaders().put("User-Agent", "TixDll");
        } else {
            if (br.containsHTML(PREMIUMONLY))
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.ExtaBitCom.errors.Only4Premium", "This file is only available for premium users"));
            else if (br.containsHTML(NOTAVAILABLETEXT))
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.ExtaBitCom.errors.TempUnavailable", "This file is temporary unavailable"));
            else if (br.containsHTML(NOMIRROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.ExtaBitCom.errors.NoMirror", "Extabit error: \"No download mirror\", contact extabit support."), 120 * 60 * 1000l);
            String addedlink = br.getURL();
            if (!addedlink.equals(link.getDownloadURL())) link.setUrlDownload(addedlink);
            if (br.containsHTML("The daily downloads limit from your IP is exceeded")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            if (br.containsHTML("Next free download from your ip will be available")) {
                String wait = br.getRegex("will be available in <b>(.*?)minutes").getMatch(0);
                int minutes = 15;
                if (wait != null) minutes = Integer.parseInt(wait.trim());
                int waitTime = minutes * 60 * 1001;
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
            }
            // If the waittime was forced it yould be here but it isn't!
            // Re Captcha handling
            final String fileID = new Regex(link.getDownloadURL(), "extabit\\.com/file/(.+)(/.+)?").getMatch(0);
            Browser xmlbrowser = br.cloneBrowser();
            xmlbrowser.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                for (int i = 0; i <= 5; i++) {
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, link);
                    xmlbrowser.getPage("http://extabit.com/file/" + fileID + "?type=recaptcha&challenge=" + rc.getChallenge() + "&capture=" + Encoding.urlEncode(c));
                    if (xmlbrowser.containsHTML("\"Entered digits are incorrect\\.\"")) {
                        rc.reload();
                        continue;
                    }
                    if (!xmlbrowser.containsHTML("\"ok\":true")) {
                        logger.warning("Unknown captcha error...");
                        break;
                    }
                    String xmlhref = new Regex(xmlbrowser, "href\":\"(\\?.*?)\"").getMatch(0);
                    if (xmlhref != null) {
                        br.getPage(link.getDownloadURL() + xmlhref);
                        break;
                    } else {
                        br.getPage(link.getDownloadURL());
                        break;
                    }
                }
            } else {
                for (int i = 0; i <= 5; i++) {
                    // *Normal* captcha handling
                    String captchaurl = br.getRegex("(/capture\\.gif\\?\\d+)").getMatch(0);
                    if (captchaurl == null) captchaurl = br.getRegex("<div id=\"reload_captcha\">.*?<img src=\"(/.*?)\"").getMatch(0);
                    if (captchaurl == null) {
                        // Check if maybe the link is only downloadable via their downloadmanager
                        if (br.containsHTML("/img/dlbanner\\.jpg\"")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Download not possible!"); }
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }

                    captchaurl = "http://extabit.com" + captchaurl;
                    String code = getCaptchaCode(captchaurl, link);
                    // dlform.put("capture", code);
                    // br.submitForm(dlform);
                    xmlbrowser.getPage(link.getDownloadURL() + "?capture=" + code);
                    if (!xmlbrowser.containsHTML("\"ok\":true")) {
                        br.getPage(br.getURL());
                        continue;
                    }
                    String xmlhref = new Regex(xmlbrowser, "href\":\"(\\?.*?)\"").getMatch(0);
                    if (xmlhref != null) {
                        br.getPage(link.getDownloadURL() + xmlhref);
                        break;
                    } else {
                        br.getPage(link.getDownloadURL());
                        break;
                    }
                }
            }
            if (br.containsHTML("\"Entered digits are incorrect\\.\"") || !xmlbrowser.containsHTML("\"ok\":true")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // Australia 20130613
            dllink = br.getRegex("href=\"(https?://guest\\d+\\.extabit\\.com/[a-z0-9]+/[^\"]+)\"[^>]+>Download file</a>").getMatch(0);
            if (dllink == null) {
                // Russia 20130613
                dllink = br.getRegex("&product_download_url=(https?://guest\\d+\\.extabit\\.com/[a-z0-9]+/.*?)';").getMatch(0);
                if (dllink == null) {
                    // fail over 20130613
                    dllink = br.getRegex("(\"|')(https?://guest\\d+\\.extabit\\.com/[a-z0-9]+/.*?)(\"|')").getMatch(1);
                    if (dllink == null) {
                        if (br.getURL().endsWith("?af")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster error, please contact the extabit.com support!", 60 * 60 * 1000l);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        if (link.getBooleanProperty(ExtaBitCom.NOCHUNKS, false)) {
            resume = false;
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        dl.setFilenameFix(true);
        if ((dl.getConnection().getContentType().contains("html"))) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
            if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) { return; }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getLinkStatus().getErrorMessage() != null && link.getBooleanProperty(ExtaBitCom.NOCHUNKS, false) == false) {
                    link.setProperty(ExtaBitCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(ExtaBitCom.NOCHUNKS, false) == false) {
                link.setProperty(ExtaBitCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (br.getRedirectLocation() != null && br.getRedirectLocation().length() <= (link.getDownloadURL().length() + 5)) {
            link.setUrlDownload(br.getRedirectLocation());
            logger.info("New link was set, retrying!");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dllink == null) {
            if (br.containsHTML(NOTAVAILABLETEXT) || br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.ExtaBitCom.errors.TempUnavailable", "This file is temporary unavailable"));
            dllink = br.getRegex("<div id=\"download_filename\" class=\"df_archive\">3 g i.part1.rar</div>[\n\r\t ]+<a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://[a-z]+\\d+\\.extabit\\.com/[a-z0-9]+/.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -10);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://extabit.com", "language", "en");
        br.setFollowRedirects(false);
        br.postPage("http://extabit.com/login.jsp", "email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&remember=1&auth_submit_login.x=" + new Random().nextInt(10) + "&auth_submit_login.y=" + new Random().nextInt(10) + "&auth_submit_login=Enter");
        if (br.getCookie("http://extabit.com/", "auth_uid") == null || br.getCookie("http://extabit.com/", "auth_hash") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ExtaBitCom.SKIPFREELIMITS, JDL.L("plugins.hoster.ExtaBitCom.skipfreelimits", "Free Mode: Try to skip captchas, speed-limits and waittimes.\r\nWARNING: This can cause bugs/problems - if you experience bugs/problems with downloads from extabit.com with this enabled, disable it again!")).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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