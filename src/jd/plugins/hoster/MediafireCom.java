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

    static private final String offlinelink = "tos_aup_violation";

    /** The name of the error page used by MediaFire */
    private static final String ERROR_PAGE = "error.php";

    /**
     * The number of retries to be performed in order to determine if a file is
     * available
     */
    private static final int NUMBER_OF_RETRIES = 3;
    /**
     * Map to cache the configuration keys
     */
    private static final HashMap<Account, String> CONFIGURATION_KEYS = new HashMap<Account, String>();

    private String fileID;

    public MediafireCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.mediafire.com/register.php");

    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 250);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("http://media", "http://www.media"));
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://www.mediafire.com/");
        Form form = br.getFormbyProperty("name", "form_login1");
        if (form == null) form = br.getFormBySubmitvalue("login_email");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("login_email", Encoding.urlEncode(account.getUser()));
        form.put("login_pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.getPage("http://www.mediafire.com/myfiles.php");
        String acc = br.getRegex("Account:.*?style=\"margin.*?\">(.*?)</").getMatch(0);
        String cookie = br.getCookie("http://www.mediafire.com", "user");
        if (cookie.equals("x") || !acc.equals("MediaPro")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (CONFIGURATION_KEYS.get(account) == null) {
            br.getPage("http://www.mediafire.com/myaccount/download_options.php");
            String configurationKey = br.getRegex("Configuration Key:.*? value=\"(.*?)\"").getMatch(0);
            CONFIGURATION_KEYS.put(account, configurationKey);
        }

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
        account.setValid(true);
        br.getPage("http://www.mediafire.com/myaccount.php");
        String hostedFiles = br.getRegex("> From.*?(\\d+).*?total files <").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        String usedspace = br.getRegex("Total Storage Used </div> <div style=\".*?div style=\"font-size.*?\">(.*?)</div").getMatch(0);
        if (usedspace != null) ai.setUsedSpace(usedspace.trim());
        String trafficleft = br.getRegex("Available Bandwidth </div> <div style=.*?<div style=\"font-size.*?\">(.*?)</div").getMatch(0);
        if (trafficleft != null) ai.setTrafficLeft(Regex.getSize(trafficleft.trim()));
        ai.setStatus("Premium User");
        return ai;
    }

    private void handlePW(DownloadLink downloadLink) throws Exception {
        if (br.containsHTML("dh\\(''\\)")) {
            new PasswordSolver(this, br, downloadLink) {

                @Override
                protected void handlePassword(String password) throws Exception {
                    Form form = br.getFormbyProperty("name", "form_password");
                    form.put("downloadp", password);
                    br.submitForm(form);
                }

                @Override
                protected boolean isCorrect() {
                    return br.getFormbyProperty("name", "form_password") != null && !br.containsHTML("dh\\(''\\)");
                }

            }.run();
        }

    }

    @Override
    public void handlePremium(DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);

        fileID = new Regex(downloadLink.getDownloadURL(), "\\?(.*)").getMatch(0);
        if (fileID == null) {
            fileID = new Regex(downloadLink.getDownloadURL(), "file/(.*)").getMatch(0);
        }

        br.postPageRaw("http://www.mediafire.com/basicapi/premiumapi.php", "premium_key=" + CONFIGURATION_KEYS.get(account) + "&files=" + fileID);
        String url = br.getRegex("<url>(http.*?)</url>").getMatch(0);
        boolean passwordprotected = false;
        if ("-204".equals(br.getRegex("<flags>(.*?)</").getMatch(0))) {
            passwordprotected = true;
            new PasswordSolver(this, br, downloadLink) {

                @Override
                protected void handlePassword(String password) throws Exception {
                    br.postPageRaw("http://www.mediafire.com/basicapi/premiumapi.php", "file_1=" + fileID + "&password_1=" + password + "&premium_key=" + CONFIGURATION_KEYS.get(account) + "&files=" + fileID);

                }

                @Override
                protected boolean isCorrect() {
                    return br.getRegex("<url>(http.*?)</url>").getMatch(0) != null;
                }

            }.run();

            url = br.getRegex("<url>(http.*?)</url>").getMatch(0);

        }

        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            logger.info("Error (4)");
            br.followConnection();
            if (br.getRequest().getHttpConnection().getResponseCode() == 403) {
                logger.info("Error (3)");
            } else if (br.getRequest().getHttpConnection().getResponseCode() == 200 && passwordprotected) {
                // workaround for api error:
                // try website password solving
                handlePremiumPassword(downloadLink, account);
                return;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handlePremiumPassword(DownloadLink downloadLink, Account account) throws Exception {
        // API currently does not work
        // http://support.mediafire.com/index.php?_m=knowledgebase&_a=viewarticle&kbarticleid=68
        br.getPage(downloadLink.getDownloadURL());
        handlePW(downloadLink);
        String url = getDownloadUrl();
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);

        if (!dl.getConnection().isContentDisposition()) {
            logger.info("Error (3)");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        downloadLink.setProperty("type", "");
        String url = downloadLink.getDownloadURL();
        AvailableStatus status = AvailableStatus.TRUE;
        for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
            try {
                br.getPage(url);
                String redirectURL = br.getRedirectLocation();
                if (redirectURL != null && redirectURL.indexOf(ERROR_PAGE) > 0) {
                    status = AvailableStatus.FALSE;
                    String errorCode = redirectURL.substring(redirectURL.indexOf("=") + 1, redirectURL.length());
                    if (errorCode.equals("320")) {
                        logger.warning("The requested file ['" + url + "'] is invalid");
                    }
                    break;
                }

                if (redirectURL != null && br.getCookie("http://www.mediafire.com", "ukey") != null) {
                    if (url.contains("download.php") || url.contains("fire.com/file/")) {
                        /* new redirect format */
                        if (!new Regex(redirectURL, "http://download\\d+\\.mediafire").matches()) {
                            br.getPage(redirectURL);
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
            } catch (IOException e) {
                if (e.getMessage().contains("code: 500")) {
                    logger.info("ErrorCode 500! Wait a moment!");
                    Thread.sleep(200);
                    continue;
                } else {
                    status = AvailableStatus.FALSE;
                }
            }
        }
        if (status == AvailableStatus.FALSE) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getRegex(offlinelink).matches()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)<\\/title>").getMatch(0);
        String filesize = br.getRegex("<input type=\"hidden\" id=\"sharedtabsfileinfo1-fs\" value=\"(.*?)\">").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<input type=\"hidden\" id=\"sharedtabsfileinfo-fs\" value=\"(.*?)\">").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        status = AvailableStatus.TRUE;
        return status;
    }

    private String getDownloadUrl() throws Exception {
        // if (Integer.parseInt(JDUtilities.getRevision().replace(".", "")) <
        // 10000) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT,
        // "Use Nightly"); }
        ExtBrowser eb = new ExtBrowser();
        try {

            //
            // Set the browserenviroment. We blacklist a few urls here, because
            // we do not need css, and several other sites
            // we enable css evaluation, because we need this to find invisible
            // links
            // internal css is enough.
            eb.setBrowserEnviroment(new BasicBrowserEnviroment(new String[] { ".*pmsrvr.com.*", ".*yahoo.com.*", ".*templates/linkto.*", ".*cdn.mediafire.com/css/.*", ".*/blank.html" }, null) {

                public boolean isInternalCSSEnabled() {
                    // TODO Auto-generated method stub
                    return true;
                }
            });
            // Start Evaluation of br
            eb.eval(br);
            // wait for workframe2, but max 30 seconds
            eb.waitForFrame("workframe2", 30000);
            // get all links now
            HTMLCollection links = eb.getDocument().getLinks();

            for (int i = 0; i < links.getLength(); i++) {
                HTMLLinkElementImpl l = (HTMLLinkElementImpl) links.item(i);
                // check if the link is visible in browser
                l = l;
                System.out.println(l.getOuterHTML());
                if (l.getInnerHTML().toLowerCase().contains("start download")) {
                    System.out.println("Download start");
                    if (isVisible(l)) {
                        System.out.println("visible");
                        if (l.getAbsoluteHref().contains("mediafire.com")) {
                            System.out.println("contains mf");
                            return l.getAbsoluteHref();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.info(eb.getHtmlText());
            e.printStackTrace();
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private static ArrayList<HTMLElementImpl> getPath(HTMLElementImpl impl) {
        ArrayList<HTMLElementImpl> styles = new ArrayList<HTMLElementImpl>();

        HTMLElementImpl p = impl;
        while (p != null) {
            styles.add(0, p);
            p = p.getParent("*");
        }
        return styles;
    }

    public static boolean isVisible(HTMLElementImpl impl) {

        ArrayList<HTMLElementImpl> styles = getPath(impl);
        int x = 0;
        int y = 0;
        for (HTMLElementImpl p : styles) {
            AbstractCSS2Properties style = p.getComputedStyle(null);
            System.out.println(style + " : " + style.toStringForm());

            if ("none".equalsIgnoreCase(style.getDisplay())) {
                //
                System.out.println("NO DISPLAY");
                return false;
            }
            if ("absolute".equalsIgnoreCase(style.getPosition())) {
                x = y = 0;
            }
            if (style.getTop() != null) {
                y += covertToPixel(style.getTop());
            }
            if (style.getLeft() != null) {
                x += covertToPixel(style.getLeft());

            }

        }
        if (y < 0) {
            System.out.println("y<0" + " " + x + " - " + y);
            return false;
        }
        return true;
    }

    private static int covertToPixel(String top) {
        if (top == null) return 0;
        if (top.toLowerCase().trim().endsWith("px")) { return Integer.parseInt(top.substring(0, top.length() - 2)); }
        String value = new Regex(top, "([\\-\\+]?\\s*\\d+)").getMatch(0);
        if (value == null) return 0;
        return Integer.parseInt(value);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String url = null;
        br.setDebug(true);
        for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
            if (url != null) break;
            requestFileInformation(downloadLink);
            try {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                Form form = br.getFormbyProperty("name", "form_captcha");
                String id = br.getRegex("e\\?k=(.+?)\"").getMatch(0);
                if (id != null) {
                    rc.setId(id);
                    InputField challenge = new InputField("recaptcha_challenge_field", null);
                    InputField code = new InputField("recaptcha_response_field", null);
                    form.addInputField(challenge);
                    form.addInputField(code);
                    rc.setForm(form);
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());

                    try {
                        String c = getCaptchaCode(cf, downloadLink);
                        rc.setCode(c);
                    } catch (PluginException e) {
                        /**
                         * captcha input timeout run out.. try to reconnect
                         */
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                    }
                }
            } catch (Exception e) {
                JDLogger.exception(e);
            }

            if (downloadLink.getStringProperty("type", "").equalsIgnoreCase("direct")) {
                logger.info("DirectDownload");
                url = br.getRedirectLocation();
            } else {
                handlePW(downloadLink);
                url = getDownloadUrl();
            }
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            logger.info("Error (3)");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("type", "");
    }

    public static abstract class PasswordSolver {

        protected Browser br;
        protected PluginForHost plg;
        protected DownloadLink dlink;
        private int maxTries;
        private int currentTry;

        public PasswordSolver(PluginForHost plg, Browser br, DownloadLink downloadLink) {
            this.plg = plg;
            this.br = br;
            this.dlink = downloadLink;
            this.maxTries = 3;
            this.currentTry = 0;
        }

        public void run() throws Exception {
            while (currentTry++ < maxTries) {
                String password = null;
                if (dlink.getStringProperty("pass", null) != null) {
                    password = dlink.getStringProperty("pass", null);
                } else if (plg.getPluginConfig().getStringProperty("pass", null) != null) {
                    password = plg.getPluginConfig().getStringProperty("pass", null);

                } else {
                    password = Plugin.getUserInput(Loc.LF("PasswordSolver.askdialog", "Downloadpassword for %s/%s", plg.getHost(), dlink.getName()), dlink);

                }
                if (password == null) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong")); }
                handlePassword(password);
                if (!isCorrect()) {
                    dlink.setProperty("pass", null);
                    plg.getPluginConfig().setProperty("pass", null);
                    plg.getPluginConfig().save();
                    continue;
                } else {
                    dlink.setProperty("pass", password);
                    plg.getPluginConfig().setProperty("pass", password);
                    plg.getPluginConfig().save();
                    return;
                }

            }
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }

        abstract protected boolean isCorrect();

        abstract protected void handlePassword(String password) throws Exception;
    }
}
