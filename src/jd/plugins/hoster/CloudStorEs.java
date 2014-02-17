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
import java.util.Locale;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloudstor.es" }, urls = { "http://(www\\.)?cloudstor\\.es/(f|file)/[A-Za-z0-9_]+/(\\d+/)?" }, flags = { 2 })
public class CloudStorEs extends PluginForHost {

    private static final String veryBusy = "We are currently very busy\\. Please check back soon!";

    public CloudStorEs(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://cloudstor.es/packages/");
    }

    @Override
    public String getAGBLink() {
        return "http://cloudstor.es/policies/tos/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Error 404: Page Not Found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(veryBusy)) {
            // could be a temp issue??
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("Free Download Limit Reached")) {
            link.getLinkStatus().setStatusText("Free download limit reached -> Cannot get filename & filesize");
            return AvailableStatus.TRUE;
        }
        final Regex fInfo = br.getRegex("<h1>([^<>\"]*?)</h1>[^<>\"]*? \\| (\\d+(\\.\\d+)? [A-Za-z]{1,5})[ ]+</div>");
        final String filename = fInfo.getMatch(0);
        final String filesize = fInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(veryBusy)) {
            // could be a temp issue?? but we can't download...
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000);
        }
        if (br.containsHTML("Free Download Limit Reached")) {
            final String minutes = br.getRegex("for lightning fast, unlimited downloading or wait another <strong>(\\d+) minutes\\.</strong>").getMatch(0);
            if (minutes != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(minutes) * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // jd0.9 doesn't set this following header with postPage or submitform. JD2 does!
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        if (br.containsHTML("\\'/submit/_dl_isozone\\.php\\'")) {
            final Regex dlInfo = br.getRegex("id: \\'(\\d+)\\', part: \\'(\\d+)\\', token: \\'([a-z0-9]+)\\'");
            if (dlInfo.getMatches().length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.postPage("http://cloudstor.es/submit/_dl_isozone.php", "id=" + dlInfo.getMatch(0) + "&part=" + dlInfo.getMatch(1) + "&token=" + dlInfo.getMatch(2));
        } else {
            final String postLink = br.getRegex("url: \\'(/submit/[A-Za-z0-9\\-_]+\\.php)\\'").getMatch(0);
            if (postLink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            final Regex dlInfo = br.getRegex("hash: \\'([A-Za-z0-9_]+)\\', token: \\'([a-z0-9]+)\\'");
            if (dlInfo.getMatches().length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.postPage("http://cloudstor.es" + postLink, "hash=" + dlInfo.getMatch(0) + "&token=" + dlInfo.getMatch(1));
        }
        br.getHeaders().put("Content-Type", null);
        String dllink = br.toString();
        if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) dllink = br.getRegex("(http://[^\r\n]+)").getMatch(0);
        if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("%0D%0A", "").trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().equals("http://cloudstor.es/503.php")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultaneous downloads, wait till you can start another download...", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://cloudstor.es";
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
                br.setFollowRedirects(false);
                br.postPage("http://cloudstor.es/authlogin/", "remember=on&button=Submit&frmLogin=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "cloudstores_pass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
            throw e;
        }
        br.getPage("http://cloudstor.es/cp/my-account/");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("Next Payment Due[\t\n\r ]+<div class=\"cp_row_right\">([^<>\"]*?)</div>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "MMMM dd, yyyy", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML(veryBusy)) {
            // could be a temp issue?? but we can't download...
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000);
        }
        login(account, false);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}