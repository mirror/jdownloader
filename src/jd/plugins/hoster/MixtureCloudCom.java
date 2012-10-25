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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixturecloud.com" }, urls = { "https?://(www\\.)?mixture(cloud|audio|doc|file|image|video)\\.com/(media/(download/)?|download=)[A-Za-z0-9]+" }, flags = { 2 })
public class MixtureCloudCom extends PluginForHost {

    // free: 1maxdl * 1 chunk
    // protocol: They have HTTPS certificate but httpd not setup correctly
    // captchatype: recaptcha
    // other: Multiple domains all redirect back to 'sub.mixturecloud.com/' uids
    // are transferable between each (sub)?domain & section. All links have
    // recaptcha with this one size fits all download method.
    private static final String PREMIUMONLY         = "File access is limited to users with unlimited";
    private static final String PREMIUMONLYUSERTEXT = JDL.L("plugins.hoster.mixturecloudcom", "Only downloadable for premium users");

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

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "mx_l", "de");
        br.getPage(link.getDownloadURL());
        // Link offline
        if (br.containsHTML("(There is no file here<|404: page not found \\!<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Link abused
        if (br.containsHTML("Als Reaktion auf eine Beschwerde")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<\\!\\-\\- File header informations  \\-\\->[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?) \\- MixtureCloud\\.com \\-").getMatch(0);
        }
        String filesize = br.getRegex("<h5>Größe : ([^<>\"]*?)</h5>").getMatch(0);
        if (filesize == null) {
            logger.warning("MixtureCloud: Couldn't find filesize. Please report this to the JDownloader Development Team.");
            logger.warning("MixtureCloud: Continuing...");
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML(PREMIUMONLY)) link.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Sie haben in den letzten 30 Minuten eine Datei")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
        if (br.containsHTML(PREMIUMONLY)) throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
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
                br.setCookie(MAINPAGE, "mx_l", "de");
                br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
                br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
                br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                br.getPage("https://www.mixturecloud.com/login");
                final String secCode = br.getRegex("type=\"hidden\" name=\"securecode\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (secCode == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.postPage("https://www.mixturecloud.com/login", "back=&securecode=" + Encoding.urlEncode(secCode) + "&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "mx") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (!br.containsHTML(">Ihr Konto ist bereits unbegrenzt") && !br.containsHTML(">Votre compte est déjà Illimité")) {
                    logger.info("Unsupported accounttype!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Save cookies
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
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
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