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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.ext.BasicBrowserEnviroment;
import jd.http.ext.ExtBrowser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.locale.Loc;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
import org.lobobrowser.html.style.AbstractCSS2Properties;
import org.w3c.dom.html2.HTMLCollection;

//import org.lobobrowser.html.domimpl.HTMLDivElementImpl;
//import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
//import org.lobobrowser.html.style.AbstractCSS2Properties;
//import org.w3c.dom.html2.HTMLCollection;
//API:  http://support.mediafire.com/index.php?_m=knowledgebase&_a=viewarticle&kbarticleid=68

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?|\\?(?!sharekey)|file/).+" }, flags = { 2 })
public class MediafireCom extends PluginForHost {
    public static abstract class PasswordSolver {

        protected Browser       br;
        protected PluginForHost plg;
        protected DownloadLink  dlink;
        private final int       maxTries;
        private int             currentTry;

        public PasswordSolver(final PluginForHost plg, final Browser br, final DownloadLink downloadLink) {
            this.plg = plg;
            this.br = br;
            this.dlink = downloadLink;
            this.maxTries = 3;
            this.currentTry = 0;
        }

        abstract protected void handlePassword(String password) throws Exception;

        abstract protected boolean isCorrect();

        public void run() throws Exception {
            while (this.currentTry++ < this.maxTries) {
                String password = null;
                if (this.dlink.getStringProperty("pass", null) != null) {
                    password = this.dlink.getStringProperty("pass", null);
                } else if (this.plg.getPluginConfig().getStringProperty("pass", null) != null) {
                    password = this.plg.getPluginConfig().getStringProperty("pass", null);

                } else {
                    password = Plugin.getUserInput(Loc.LF("PasswordSolver.askdialog", "Downloadpassword for %s/%s", this.plg.getHost(), this.dlink.getName()), this.dlink);

                }
                if (password == null) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong")); }
                this.handlePassword(password);
                if (!this.isCorrect()) {
                    this.dlink.setProperty("pass", null);
                    this.plg.getPluginConfig().setProperty("pass", null);
                    this.plg.getPluginConfig().save();
                    continue;
                } else {
                    this.dlink.setProperty("pass", password);
                    this.plg.getPluginConfig().setProperty("pass", password);
                    this.plg.getPluginConfig().save();
                    return;
                }

            }
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
    }

    private static final String                   UA                 = RandomUserAgent.generate();

    static private final String                   offlinelink        = "tos_aup_violation";

    /** The name of the error page used by MediaFire */
    private static final String                   ERROR_PAGE         = "error.php";
    /**
     * The number of retries to be performed in order to determine if a file is
     * available
     */
    private static final int                      NUMBER_OF_RETRIES  = 3;

    /**
     * Map to cache the configuration keys
     */
    private static final HashMap<Account, String> CONFIGURATION_KEYS = new HashMap<Account, String>();

    private static int covertToPixel(final String top) {
        if (top == null) { return 0; }
        if (top.toLowerCase().trim().endsWith("px")) { return Integer.parseInt(top.substring(0, top.length() - 2)); }
        final String value = new Regex(top, "([\\-\\+]?\\s*\\d+)").getMatch(0);
        if (value == null) { return 0; }
        return Integer.parseInt(value);
    }

    private static ArrayList<HTMLElementImpl> getPath(final HTMLElementImpl impl) {
        final ArrayList<HTMLElementImpl> styles = new ArrayList<HTMLElementImpl>();

        HTMLElementImpl p = impl;
        while (p != null) {
            styles.add(0, p);
            p = p.getParent("*");
        }
        return styles;
    }

    public static boolean isVisible(final HTMLElementImpl impl) {

        final ArrayList<HTMLElementImpl> styles = MediafireCom.getPath(impl);
        int x = 0;
        int y = 0;
        for (final HTMLElementImpl p : styles) {
            final AbstractCSS2Properties style = p.getComputedStyle(null);

            if ("none".equalsIgnoreCase(style.getDisplay())) {
                //
                System.out.println("NO DISPLAY");
                return false;
            }
            if ("absolute".equalsIgnoreCase(style.getPosition())) {
                x = y = 0;
            }
            if (style.getTop() != null) {
                y += MediafireCom.covertToPixel(style.getTop());
            }
            if (style.getLeft() != null) {
                x += MediafireCom.covertToPixel(style.getLeft());

            }

        }
        if (y < 0) {
            System.out.println("y<0" + " " + x + " - " + y);
            return false;
        }
        return true;
    }

    private String fileID;

    public MediafireCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.mediafire.com/register.php");

    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("http://media", "http://www.media"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        this.br.getPage("http://www.mediafire.com/myaccount.php");
        final String hostedFiles = this.br.getRegex("> From.*?(\\d+).*?total files <").getMatch(0);
        if (hostedFiles != null) {
            ai.setFilesNum(Long.parseLong(hostedFiles));
        }
        final String usedspace = this.br.getRegex("Total Storage Used </div> <div style=\".*?div style=\"font-size.*?\">(.*?)</div").getMatch(0);
        if (usedspace != null) {
            ai.setUsedSpace(usedspace.trim());
        }
        final String trafficleft = this.br.getRegex("Available Bandwidth </div> <div style=.*?<div style=\"font-size.*?\">(.*?)</div").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(Regex.getSize(trafficleft.trim()));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    private String getDownloadUrl() throws Exception {
        // if (Integer.parseInt(JDUtilities.getRevision().replace(".", "")) <
        // 10000) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT,
        // "Use Nightly"); }
        final ExtBrowser eb = new ExtBrowser();
        try {

            //
            // Set the browserenviroment. We blacklist a few urls here, because
            // we do not need css, and several other sites
            // we enable css evaluation, because we need this to find invisible
            // links
            // internal css is enough.
            eb.setBrowserEnviroment(new BasicBrowserEnviroment(new String[] { ".*pmsrvr.com.*", ".*yahoo.com.*", ".*templates/linkto.*", ".*cdn.mediafire.com/css/.*", ".*/blank.html" }, null) {

                @Override
                public boolean isInternalCSSEnabled() {
                    return true;
                }

            });
            // Start Evaluation of br
            eb.eval(this.br);
            // wait for workframe2, but max 30 seconds
            eb.waitForFrame("workframe2", 30000);
            // get all links now
            final HTMLCollection links = eb.getDocument().getLinks();

            for (int i = 0; i < links.getLength(); i++) {
                final HTMLLinkElementImpl l = (HTMLLinkElementImpl) links.item(i);
                // check if the link is visible in browser
                System.out.println(l.getOuterHTML());
                if (l.getInnerHTML().toLowerCase().contains("start download")) {
                    System.out.println("Download start");
                    if (MediafireCom.isVisible(l)) {
                        System.out.println("visible");
                        if (new Regex(l.getAbsoluteHref(), "http://.*?/[a-z0-9]+/[a-z0-9]+/.*").matches()) {
                            // we do not know yet, why there are urls with ip
                            // only, and urls with domain
                            // if (new Regex(l.getAbsoluteHref(),
                            // "http://\\d\\.\\d\\.\\d\\.\\d/[a-z0-9]+/[a-z0-9]+/.*").matches())
                            // { throw new
                            // PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
                            // "Server error", 2 * 60000l); }
                            System.out.println("contains mf");
                            return l.getAbsoluteHref();
                        }
                    }
                }
            }
        } catch (final Exception e) {
            Plugin.logger.info(eb.getHtmlText());
            e.printStackTrace();
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        String url = null;
        this.br.setDebug(true);

        this.br.getHeaders().put("User-Agent", MediafireCom.UA);
        for (int i = 0; i < MediafireCom.NUMBER_OF_RETRIES; i++) {
            if (url != null) {
                break;
            }
            this.requestFileInformation(downloadLink);
            try {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
                final Form form = this.br.getFormbyProperty("name", "form_captcha");
                final String id = this.br.getRegex("e\\?k=(.+?)\"").getMatch(0);
                if (id != null) {
                    rc.setId(id);
                    final InputField challenge = new InputField("recaptcha_challenge_field", null);
                    final InputField code = new InputField("recaptcha_response_field", null);
                    form.addInputField(challenge);
                    form.addInputField(code);
                    rc.setForm(form);
                    rc.load();
                    final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());

                    try {
                        final String c = this.getCaptchaCode(cf, downloadLink);
                        rc.setCode(c);
                    } catch (final PluginException e) {
                        /**
                         * captcha input timeout run out.. try to reconnect
                         */
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                    }
                }
            } catch (final Exception e) {
                JDLogger.exception(e);
            }

            if (downloadLink.getStringProperty("type", "").equalsIgnoreCase("direct")) {
                Plugin.logger.info("DirectDownload");
                url = this.br.getRedirectLocation();
            } else {
                this.handlePW(downloadLink);
                url = this.getDownloadUrl();
            }
        }
        if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.br.setFollowRedirects(true);
        this.br.setDebug(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
        if (!this.dl.getConnection().isContentDisposition()) {
            Plugin.logger.info("Error (3)");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.requestFileInformation(downloadLink);
        this.login(account);

        this.fileID = new Regex(downloadLink.getDownloadURL(), "\\?(.*)").getMatch(0);
        if (this.fileID == null) {
            this.fileID = new Regex(downloadLink.getDownloadURL(), "file/(.*)").getMatch(0);
        }

        this.br.postPageRaw("http://www.mediafire.com/basicapi/premiumapi.php", "premium_key=" + MediafireCom.CONFIGURATION_KEYS.get(account) + "&files=" + this.fileID);
        String url = this.br.getRegex("<url>(http.*?)</url>").getMatch(0);
        boolean passwordprotected = false;
        if ("-204".equals(this.br.getRegex("<flags>(.*?)</").getMatch(0))) {
            passwordprotected = true;
            new PasswordSolver(this, this.br, downloadLink) {

                @Override
                protected void handlePassword(final String password) throws Exception {
                    this.br.postPageRaw("http://www.mediafire.com/basicapi/premiumapi.php", "file_1=" + MediafireCom.this.fileID + "&password_1=" + password + "&premium_key=" + MediafireCom.CONFIGURATION_KEYS.get(account) + "&files=" + MediafireCom.this.fileID);

                }

                @Override
                protected boolean isCorrect() {
                    return this.br.getRegex("<url>(http.*?)</url>").getMatch(0) != null;
                }

            }.run();

            url = this.br.getRegex("<url>(http.*?)</url>").getMatch(0);

        }

        if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
        if (!this.dl.getConnection().isContentDisposition()) {
            Plugin.logger.info("Error (4)");
            this.br.followConnection();
            if (this.br.getRequest().getHttpConnection().getResponseCode() == 403) {
                Plugin.logger.info("Error (3)");
            } else if (this.br.getRequest().getHttpConnection().getResponseCode() == 200 && passwordprotected) {
                // workaround for api error:
                // try website password solving
                this.handlePremiumPassword(downloadLink, account);
                return;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    private void handlePremiumPassword(final DownloadLink downloadLink, final Account account) throws Exception {
        // API currently does not work
        // http://support.mediafire.com/index.php?_m=knowledgebase&_a=viewarticle&kbarticleid=68
        this.br.getPage(downloadLink.getDownloadURL());
        this.handlePW(downloadLink);
        final String url = this.getDownloadUrl();
        if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);

        if (!this.dl.getConnection().isContentDisposition()) {
            Plugin.logger.info("Error (3)");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    private void handlePW(final DownloadLink downloadLink) throws Exception {
        if (this.br.containsHTML("dh\\(''\\)")) {
            new PasswordSolver(this, this.br, downloadLink) {

                @Override
                protected void handlePassword(final String password) throws Exception {
                    final Form form = this.br.getFormbyProperty("name", "form_password");
                    form.put("downloadp", password);
                    this.br.submitForm(form);
                }

                @Override
                protected boolean isCorrect() {
                    return this.br.getFormbyProperty("name", "form_password") != null && !this.br.containsHTML("dh\\(''\\)");
                }

            }.run();
        }

    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    public void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(false);
        this.br.getPage("http://www.mediafire.com/");
        Form form = this.br.getFormbyProperty("name", "form_login1");
        if (form == null) {
            form = this.br.getFormBySubmitvalue("login_email");
        }
        if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        form.put("login_email", Encoding.urlEncode(account.getUser()));
        form.put("login_pass", Encoding.urlEncode(account.getPass()));
        this.br.submitForm(form);
        this.br.getPage("http://www.mediafire.com/myfiles.php");
        final String acc = this.br.getRegex("Account:.*?style=\"margin.*?\">(.*?)</").getMatch(0);
        final String cookie = this.br.getCookie("http://www.mediafire.com", "user");
        if (cookie.equals("x") || !acc.equals("MediaPro")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (MediafireCom.CONFIGURATION_KEYS.get(account) == null) {
            this.br.getPage("http://www.mediafire.com/myaccount/download_options.php");
            final String configurationKey = this.br.getRegex("Configuration Key:.*? value=\"(.*?)\"").getMatch(0);
            MediafireCom.CONFIGURATION_KEYS.put(account, configurationKey);
        }

    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(false);
        downloadLink.setProperty("type", "");
        final String url = downloadLink.getDownloadURL();
        AvailableStatus status = AvailableStatus.TRUE;
        for (int i = 0; i < MediafireCom.NUMBER_OF_RETRIES; i++) {
            try {
                this.br.getPage(url);
                final String redirectURL = this.br.getRedirectLocation();
                if (redirectURL != null && redirectURL.indexOf(MediafireCom.ERROR_PAGE) > 0) {
                    status = AvailableStatus.FALSE;
                    final String errorCode = redirectURL.substring(redirectURL.indexOf("=") + 1, redirectURL.length());
                    if (errorCode.equals("320")) {
                        Plugin.logger.warning("The requested file ['" + url + "'] is invalid");
                    }
                    break;
                }

                if (redirectURL != null && this.br.getCookie("http://www.mediafire.com", "ukey") != null) {
                    if (url.contains("download.php") || url.contains("fire.com/file/")) {
                        /* new redirect format */
                        if (!new Regex(redirectURL, "http://download\\d+\\.mediafire").matches()) {
                            this.br.getPage(redirectURL);
                            break;
                        }
                    }
                    downloadLink.setProperty("type", "direct");
                    if (!downloadLink.getStringProperty("origin", "").equalsIgnoreCase("decrypter")) {
                        downloadLink.setName(Plugin.extractFileNameFromURL(redirectURL));
                    }
                    return AvailableStatus.TRUE;
                }

                break;
            } catch (final IOException e) {
                if (e.getMessage().contains("code: 500")) {
                    Plugin.logger.info("ErrorCode 500! Wait a moment!");
                    Thread.sleep(200);
                    continue;
                } else {
                    status = AvailableStatus.FALSE;
                }
            }
        }
        if (status == AvailableStatus.FALSE) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (this.br.getRegex(MediafireCom.offlinelink).matches()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String filename = this.br.getRegex("<title>(.*?)<\\/title>").getMatch(0);
        String filesize = this.br.getRegex("<input type=\"hidden\" id=\"sharedtabsfileinfo1-fs\" value=\"(.*?)\">").getMatch(0);
        if (filesize == null) {
            filesize = this.br.getRegex("<input type=\"hidden\" id=\"sharedtabsfileinfo-fs\" value=\"(.*?)\">").getMatch(0);
        }
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setFinalFileName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        status = AvailableStatus.TRUE;
        return status;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty("type", "");
    }

    @Override
    public void resetPluginGlobals() {
    }
}
