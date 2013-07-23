//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixturecloud.com" }, urls = { "https?://((www|audio|doc|file|image|video)\\.)?mixture(cloud|audio|doc|file|image|video)\\.com/(media/(download/)?|download=)[A-Za-z0-9]+" }, flags = { 2 })
public class MixtureCloudCom extends PluginForHost {

    // free: 1maxdl * 1 chunk
    // protocol: They have HTTPS certificate but httpd not setup correctly
    // captchatype: recaptcha
    // other: Multiple domains all redirect back to 'sub.mixturecloud.com/' uids
    // are transferable between each (sub)?domain & section. All links have
    // recaptcha with this one size fits all download method.

    private static final String PREMIUMONLY         = "File access is limited to users with unlimited";
    private static final String PREMIUMONLYUSERTEXT = JDL.L("plugins.hoster.mixturecloudcom", "Only downloadable via free or premium account [Not downloadable now]");

    public MixtureCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.mixturecloud.com/price");
    }

    public void correctDownloadLink(DownloadLink link) {
        final String uid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        link.setUrlDownload("http://www.mixturecloud.com/media/download/" + uid);
    }

    @Override
    public String getAGBLink() {
        return "http://file.mixturecloud.com/terms";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static StringContainer agent = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    private Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setCookie(this.getHost(), "mx_l", "en");
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        checkErrors();
        String filename = br.getRegex("<\\!\\-\\- File header informations  \\-\\->[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?) \\- MixtureCloud\\.com \\-").getMatch(0);
        }
        String filesize = br.getRegex("<h5>Size : ([^<>\"]*?)</h5>").getMatch(0);
        if (filesize == null) {
            logger.warning("MixtureCloud: Couldn't find filesize. Please report this to the JDownloader Development Team.");
            logger.warning("MixtureCloud: Continuing...");
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML(PREMIUMONLY)) link.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        checkErrors();
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("File access is limited to users with unlimited")) throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        String dllink = br.getRegex("style=\"padding\\-left:30px\"></div>[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.mixturecloud\\.com/down\\.php\\?d=[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Waittime can be skipped */
        int wait = 52;
        final String waittime = br.getRegex("var time=(\\d+)").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://mixturecloud.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("https://www.mixturecloud.com/");
                final String secCode = br.getRegex("type=\"hidden\" name=\"securecode\" value=\"([^<>\"]{8})\"").getMatch(0);
                if (secCode == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                String postData = "back=&securecode=" + Encoding.urlEncode(secCode) + "&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&login=1";
                // Check if we have to enter a login captcha
                final String rcID = br.getRegex("google\\.com/recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                if (rcID != null) {
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(rcID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "mixturecloud.com", "http://mixturecloud.com", true);
                    final String c = getCaptchaCode(cf, dummyLink);
                    postData += "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                }
                br.postPage("/login", postData);
                if (br.getCookie(MAINPAGE, "mx") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);

                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("lastlogin", System.currentTimeMillis());
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                account.setProperty("lastlogin", Property.NULL);
                throw e;
            }
        }
    }

    private void checkErrors() throws Exception {
        if (br.containsHTML("(There is no file here<|404: page not found \\!<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("In response to a complaint received under the US Digital Millennium Copyright Act, you can't access to this file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Sie haben in den letzten 30 Minuten eine Datei")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        // dead
        account.setProperty("freeacc", Property.NULL); // remove after next round of plugin updates 20130724
        // endofdead
        AccountInfo ai = new AccountInfo();
        try {
            // captcha on each login == lame, we will only login very 12 hours after lastlogin to refresh cookie session
            if (account.getStringProperty("lastlogin") != null && (System.currentTimeMillis() - 43200000 <= Long.parseLong(account.getStringProperty("lastlogin"))))
                login(account, false);
            else
                login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("https://www.mixturecloud.com/account");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        if (br.containsHTML("<\\!\\-\\- PREMIUM \\-\\->")) {
            ai.setStatus("Premium User");
            account.setProperty("free", false);
        } else if (br.containsHTML("<h2>Current account</h2>.*<h1>Basic</h1>")) {
            ai.setStatus("Free registered User");
            account.setProperty("free", true);
        } else {
            ai.setStatus("Unknown (invalid) accounttype");
            account.setValid(false);
            return ai;
        }

        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setCookie(MAINPAGE, "mx_l", "en");
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("free")) {
            doFree(link);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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