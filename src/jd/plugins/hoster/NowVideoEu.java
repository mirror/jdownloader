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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nowvideo.to" }, urls = { "http://(?:www\\.)?(?:nowvideo\\.(?:sx|eu|co|ch|ag|at|ec|li|to)/(?:video/|player\\.php\\?v=|share\\.php\\?id=|embed/\\?v=)|embed\\.nowvideo\\.(sx|eu|co|ch|ag|at)/(embed\\.php|embed/)\\?v=)[a-z0-9]+" })
public class NowVideoEu extends PluginForHost {

    /* Similar plugins: NovaUpMovcom, VideoWeedCom, NowVideoEu, MovShareNet */
    // cloudtime.to and nowvideo are the same provider, finallink servers are identical hostnames!
    // note: avi containers are not converted to flv!
    private static Object                  LOCK               = new Object();
    private static final String            currentMainDomain  = "nowvideo.to";
    private static AtomicReference<String> MAINPAGE           = new AtomicReference<String>("http://www." + currentMainDomain);
    private final String                   ISBEINGCONVERTED   = ">The file is being converted.";
    private final String                   domains            = "nowvideo\\.(sx|eu|co|ch|ag|at|ec|li|to)";
    private String                         filename           = null;
    private static AtomicBoolean           AVAILABLE_PRECHECK = new AtomicBoolean(false);

    // note: .ch parked
    // note: .at parked
    // note: .eu no dns record
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "nowvideo.to", "nowvideo.sx", "nowvideo.co", "nowvideo.ag", "nowvideo.ec", "nowvideo.li" };
    }

    private String validateHost() {
        for (final String domain : siteSupportedNames()) {
            try {
                final Browser br = new Browser();
                workAroundTimeOut(br);
                br.setCookiesExclusive(true);
                br.getPage("http://www." + domain);
                final String redirect = br.getRedirectLocation();
                if (redirect == null && Browser.getHost(br.getURL()).matches(domains)) {
                    // primary domain wont redirect
                    return domain;
                } else if (redirect != null) {
                    final String cctld = new Regex(redirect, domains).getMatch(0);
                    if (cctld != null) {
                        return domain;
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                logger.warning(domain + " seems to be offline...");
            }
        }
        return null;
    }

    private void correctCurrentDomain() throws PluginException {
        if (AVAILABLE_PRECHECK.get() == false) {
            synchronized (LOCK) {
                /*
                 * For example .eu domain are blocked from some Italian ISP, and .co from others, so need to test all domains before
                 * proceeding.
                 */
                final String domain = validateHost();
                if (domain == null) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Could not determine proper ccTLD!");
                }
                MAINPAGE.set("http://www." + domain);
                this.enablePremium(MAINPAGE.toString() + "/premium.php");
                AVAILABLE_PRECHECK.set(true);
            }
        }
    }

    private final static String  PROPERTY_FILENAME_WITH_FUID         = "PROPERTY_FILENAME_WITH_FUID";
    private final static boolean PROPERTY_DEFAULT_FILENAME_WITH_FUID = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_FILENAME_WITH_FUID, "Use FUID (File Unquie ID) within File Name").setDefaultValue(PROPERTY_DEFAULT_FILENAME_WITH_FUID));
    }

    public NowVideoEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE.get() + "/premium.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE.get() + "/terms.php";
    }

    @Override
    public String rewriteHost(String host) {
        if (getHost().matches(domains)) {
            if (host == null || host.matches(domains)) {
                return currentMainDomain;
            }
        }
        return super.rewriteHost(host);
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String fid = getLinkid(link);
        final String newlink = MAINPAGE.get() + "/player.php?v=" + fid;
        link.setLinkID(fid);
        link.setContentUrl(newlink);
    }

    @SuppressWarnings("deprecation")
    private String getLinkid(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    private static void workAroundTimeOut(final Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(45000);
                br.setReadTimeout(45000);
            }
        } catch (final Throwable e) {
        }
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:32.0) Gecko/20100101 Firefox/32.0");
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = new Browser();
        filename = null;
        prepBrowser(br);
        correctCurrentDomain();
        correctDownloadLink(link);
        final String linkid = getLinkid(link);
        /* Offline urls should also get nice filenames! */
        if (!link.isNameSet()) {
            link.setName(linkid);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getContentUrl());
        checkForThis();
        if (br.containsHTML(">This file no longer exists on our servers|>Possible reasons:")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(ISBEINGCONVERTED)) {
            link.getLinkStatus().setStatusText("This file is being converted!");
            link.setName(linkid + ".flv");
            return AvailableStatus.TRUE;
        }
        filename = br.getRegex("<title>Watch (.*?) online \\|").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"Watch (.*?) online \\|").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h4>(.*?)</h4>").getMatch(0);
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        filename = filename.trim() + (getPluginConfig().getBooleanProperty(PROPERTY_FILENAME_WITH_FUID, PROPERTY_DEFAULT_FILENAME_WITH_FUID) ? "(" + linkid + ")" : "");
        /* 2016-09-14: Changed default extension from .flv to .mp4 */
        link.setName(Encoding.htmlDecode(filename) + ".mp4");
        return AvailableStatus.TRUE;
    }

    private final void checkForThis() throws Exception {
        // some bullshit here 20151121
        final Form f = br.getFormbyKey("stepkey");
        if (f != null) {
            br.submitForm(f);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        if (br.containsHTML(ISBEINGCONVERTED)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is being converted!", 2 * 60 * 60 * 1000l);
        }
        {
            for (final Form form : br.getForms()) {
                if (form.containsHTML(">Continue to the video</button>")) {
                    br.submitForm(form);
                    break;
                }
            }
        }
        final String newWay = this.br.getRegex("\"(/download\\.php\\?file=[^<>\"]+)\"").getMatch(0);
        if (newWay != null) {
            /* 2017-03-03 */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, newWay, true, 0);
            dl.startDownload();
        } else {
            // old
            final String cid1 = br.getRegex("flashvars\\.cid=\"(\\d+)\";").getMatch(0);
            final String cid2 = br.getRegex("flashvars\\.cid2=\"(\\d+)\";").getMatch(0);
            String fKey = br.getRegex("flashvars\\.filekey=\"([^<>\"]*)\"").getMatch(0);
            if (fKey == null) {
                fKey = br.getRegex("var fkzd=\"([^<>\"]*)\"").getMatch(0);
            }
            if (fKey == null && br.containsHTML("w,i,s,e")) {
                String result = unWise();
                fKey = new Regex(result, "(\"\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}-[a-f0-9]{32})\"").getMatch(0);
            }
            if (fKey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String player = "/api/player.api.php?pass=undefined&user=undefined&codes=undefined&file=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + "&key=" + Encoding.urlEncode(fKey) + "&cid=" + cid1 + "&cid2=" + (cid2 == null ? "undefined" : cid2) + "&cid3=" + br.getHost() + "&numOfErrors=";
            final String host = new Regex(br.getURL(), "https?://[^/]+").getMatch(-1);
            int errCount = 0;
            br.getPage(player + errCount);
            if (br.containsHTML("The video is being transfered")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: The video is being transfered", 30 * 60 * 1000l);
            }
            if (br.containsHTML("error=1&error_msg=The video is converting")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster Issue: Video still Converting", 30 * 60 * 1000);
            }
            if (br.containsHTML("error_msg=The video has failed to convert")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'The video has failed to convert'", 60 * 60 * 1000l);
            }
            String dllink = br.getRegex("url=(https?://[^<>\"]*?\\.(?:flv|avi))\\&title").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // set correct file extension, flv != avi!
            if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName(filename + getFileNameExtensionFromString(dllink, ".mp4"));
            }
            // end of final filename correction
            while (true) {
                if (errCount >= 1) {
                    br.getHeaders().put("Referer", host + "/player/cloudplayer.swf");
                    br.getPage(host + player + errCount + "&errorCode=404&errorUrl=" + Encoding.urlEncode(dllink));
                    dllink = br.getRegex("url=(https?://[^<>\"]+\\.(?:flv|avi))\\&title").getMatch(0);
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                br.getHeaders().put("Referer", host + "/player/cloudplayer.swf");
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
                if (dl.getConnection().getContentType().contains("html")) {
                    if (dl.getConnection().getResponseCode() == 500 || dl.getConnection().getResponseCode() == 404 || dl.getConnection().getResponseCode() == 403) {
                        if (errCount > 4) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Can not connect to streaming link!", 10 * 60 * 1000l);
                        }
                        errCount++;
                        continue;
                    }
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
                break;
            }
        }
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) {
            return null;
        }
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval("var res = " + fn);
            result = (String) engine.get("res");
            result = new Regex(result, "eval\\((.*?)\\);$").getMatch(0);
            engine.eval("res = " + result);
            result = (String) engine.get("res");
            String res[] = result.split(";\\s;");
            engine.eval("res = " + new Regex(res[res.length - 1], "eval\\((.*?)\\);$").getMatch(0));
            result = (String) engine.get("res");
        } catch (final Exception e) {
            logger.log(e);
            return null;
        }
        return result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    /**
     * Dev note: Never buy premium from them, as freeuser you have no limits, as premium neither and you can't even download the original
     * videos as premiumuser->Senseless!!
     */
    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBrowser(br);
                correctCurrentDomain();
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
                            this.br.setCookie(MAINPAGE.get(), key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage(MAINPAGE.get() + "/login.php?return=", "register=Login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getURL().contains("login.php?e=1") || !br.getURL().contains("panel.php?login=1")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // free vs premium ?? unknown!
                br.getPage("/premium.php");
                if (br.containsHTML(expire)) {
                    account.setProperty("free", false);
                } else {
                    account.setProperty("free", true);
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE.get());
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

    private final String expire = "Your premium membership expires on: (\\d{4}-[A-Za-z]+-\\d+)";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (account.getBooleanProperty("free", false)) {
            ai.setStatus("Free Account");
        } else {
            final String expire_time = br.getRegex(expire).getMatch(0);
            // 2014-Mar-22.
            if (expire_time != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire_time, "yyyy-MMM-dd", Locale.ENGLISH));
            }
            ai.setStatus("Premium Account");
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        br = new Browser();
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("free", false)) {
            doFree(link, account);
            return;
        }
        checkForThis();
        String dllink = br.getRegex("\"(https?://[a-z0-9]+\\." + domains + "/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[a-z0-9\\.]+/dl/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(/download\\.php\\?file=[a-f0-9]{32,}\\.(?:flv|avi))\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        // set correct file extension, flv != avi!
        if (link.getFinalFileName() == null) {
            link.setFinalFileName(filename + getFileNameExtensionFromString(dllink, ".mp4"));
        }
        // end of final filename correction
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
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

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_VideoHosting;
    }
}