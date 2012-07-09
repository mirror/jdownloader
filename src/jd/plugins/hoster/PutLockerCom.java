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
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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

/** Works exactly like sockshare.com */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "putlocker.com" }, urls = { "http://(www\\.)?putlocker\\.com/(file|embed)/[A-Z0-9]+" }, flags = { 2 })
public class PutLockerCom extends PluginForHost {

    private static final String MAINPAGE = "http://www.putlocker.com";
    private static final Object LOCK     = new Object();
    private static String       agent    = null;

    public PutLockerCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.putlocker.com/gopro.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/embed/", "/file/"));
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String validUntil = br.getRegex("Expiring </td>.*?>(.*?)<").getMatch(0);
        if (validUntil != null) {
            validUntil = validUntil.replaceFirst(" at ", " ");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "MMMM dd, yyyy HH:mm", null));
            ai.setStatus("Premium okay");
            ai.setUnlimitedTraffic();
            account.setValid(true);
        } else {
            account.setValid(false);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.putlocker.com/page.php?terms";
    }

    private String getDllink(DownloadLink downloadLink) throws IOException {
        // base64 valid chars [A-Za-z0-9_\\-\\=\\+\\/]+
        String dllink = br.getRegex("<a href=\"/gopro\\.php\">Tired of ads and waiting\\? Go Pro\\!</a>[\t\n\r ]+</div>[\t\n\rn ]+<a href=\"(/.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/get_file\\.php\\?(download|id)=[A-Z0-9]+\\&key=[A-Za-z0-9_\\-\\=\\+\\/]+)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(/get_file\\.php\\?(download|id)=[A-Z0-9]+\\&key=[A-Za-z0-9_\\-\\=\\+\\/]+&original=1)\"").getMatch(0);
            if (dllink == null) {
                // Handling for streamlinks
                dllink = br.getRegex("playlist: \\'(/get_file\\.php\\?stream=[A-Za-z0-9_\\-\\=\\+\\/]+)\\'").getMatch(0);
                if (dllink != null) {
                    dllink = MAINPAGE + dllink;
                    downloadLink.setProperty("videolink", dllink);
                    br.getPage(dllink);
                    dllink = br.getRegex("media:content url=\"(http://.*?)\"").getMatch(0);
                    if (dllink == null) dllink = br.getRegex("\"(http://media\\-b\\d+\\.putlocker\\.com/download/\\d+/.*?)\"").getMatch(0);
                }
            }
        }
        if (dllink != null) dllink = dllink.replace("&amp;", "&");
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Form freeform = br.getForm(0);
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Can still be skipped */
        // final String waittime = br.getRegex("var countdownNum = (\\d+);").getMatch(0);
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
        if (br.containsHTML(">You have exceeded the daily stream limit for your country\\. You can wait until tomorrow")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (br.containsHTML("(>This content server has been temporarily disabled for upgrades|Try again soon\\. You can still download it below\\.<)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server temporarily disabled!", 2 * 60 * 60 * 1000l);
        String dllink = getDllink(downloadLink);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("http")) dllink = MAINPAGE + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // My experience was that such files just don't work, i wasn't able to download a link with this error in 3 days!
            if (br.getURL().equals("http://www.putlocker.com/")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.MAINPAGEer.putlockercom.servererrorfilebroken", "Server error - file offline?"));
            if (br.containsHTML(">This link has expired\\. Please try again")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        String dlURL = getDllink(link);
        if (dlURL == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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

    private void login(Account account, boolean fetchInfo) throws Exception {
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
                    br.getPage("http://www.putlocker.com/profile.php?pro");
                    proActive = br.getRegex("Pro  Status<[^>]+>[\r\n\t ]+<[^>]+>(Active)").getMatch(0);
                    if (proActive == null) {
                        logger.severe("No longer Pro-Status, try to fetch new cookie!\r\n" + br.toString());
                    } else {
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://www.putlocker.com/authenticate.php?login");
                Form login = br.getForm(0);
                if (login == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("captcha.php\\?")) {
                    String captchaIMG = br.getRegex("<img src=\"(/include/captcha.php\\?[^\"]+)\" />").getMatch(0);
                    if (captchaIMG == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    DownloadLink dummyLink = new DownloadLink(this, "Account", "putlocker.com", "http://putlocker.com", true);
                    String captcha = getCaptchaCode(captchaIMG, dummyLink);
                    if (captcha != null) login.put("captcha_code", Encoding.urlEncode(captcha));
                }
                login.put("user", Encoding.urlEncode(account.getUser()));
                login.put("pass", Encoding.urlEncode(account.getPass()));
                login.put("remember", "1");
                br.submitForm(login);
                // no auth = not logged / invalid account.
                if (br.getCookie(MAINPAGE, "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // finish off more code here
                br.getPage("http://www.putlocker.com/profile.php?pro");
                proActive = br.getRegex("Pro  Status<[^>]+>[\r\n\t ]+<[^>]+>(Active)").getMatch(0);
                if (proActive == null) {
                    logger.severe(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This file doesn\\'t exist, or has been removed \\.<") || br.getURL().contains("?404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("hd_marker\".*?span>(.*?)<strong").getMatch(0);
        if (filename == null) filename = br.getRegex("site\\-content.*?<h1>(.*?)<strong").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) |").getMatch(0);
        String filesize = br.getRegex("site-content.*?<h1>.*?<strong>\\((.*?)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // User sometimes adds random stuff to filenames when downloading so we better set the final name here
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}