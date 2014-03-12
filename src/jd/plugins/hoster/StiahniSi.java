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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stiahni.si" }, urls = { "http://(www\\.)?(stiahni|stahni)\\.si/((de|en|hu|pl|sk)/)?file/[A-Za-z0-9]+(/.{1})?" }, flags = { 2 })
public class StiahniSi extends PluginForHost {

    public StiahniSi(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://stiahni.si/en/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.stiahni.si/contact.php";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("stahni.si/", "stiahni.si/"));
        if (link.getDownloadURL().matches(TYPE_NEW)) {
            link.setUrlDownload("http://www.stiahni.si/en/file/" + new Regex(link.getDownloadURL(), "file/(.+)").getMatch(0));
        }
    }

    private static final boolean SKIPWAIT = false;
    private static final String  TYPE_NEW = "http://(www\\.)?stiahni\\.si/((de|en|hu|pl|sk)/)?file/[A-Za-z0-9]+/.{1}";
    private static final String  TYPE_OLD = "http://(www\\.)?stiahni\\.si/file/\\d+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        correctDownloadLink(link);
        // Offline links should also have nice filenames
        link.setName(new Regex(link.getDownloadURL(), "/file/(.+)").getMatch(0));
        this.setBrowserExclusive();
        prepBr();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Stiahni\\.si \\-([^<>\"]*?)\\- Damn good file\\-hosting</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("class=\"file_download_name\">([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("file\\-title\"><h1>([^<>\"]*?)</h1>").getMatch(0);
        final String filesize = br.getRegex("Size: ([^<>\"]*?)<br/>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim());
        link.setFinalFileName(filename);
        if (br.containsHTML("has been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">You cannot download more than one file at once<")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Finish running downloads to start more", 2 * 60 * 1000l);
        if (!SKIPWAIT) {
            final String waittime = br.getRegex("var parselimit = (\\d+);").getMatch(0);
            int wait = 60;
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
        }
        br.setFollowRedirects(false);
        final String dllink = "http://www.stiahni.si/en/freedownload?hash=" + getFID();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 500) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Finish running downloads to start more", 2 * 60 * 1000l);
            br.followConnection();
            // too many concurrent connections?? or something else not sure.
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Finish running downloads to start more", 2 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://stiahni.si";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBr();
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
                br.getPage("http://stiahni.si/en/index");
                br.postPage("http://stiahni.si/en/login", "rememberMe=1&yt0=Sign+in&LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5BoldPassword%5D=" + Encoding.urlEncode(account.getPass()));
                br.getPage("http://stiahni.si/en/index");
                if (!br.containsHTML(">Log out</span>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        final String expire = br.getRegex("class=\"btn btn\\-info btn\\-sm\">(\\d+) Days</a>").getMatch(0);
        final String traffic = br.getRegex("class=\"btn btn\\-success btn\\-sm\" >([^<>\"]*?)</a>").getMatch(0);
        if (expire == null && traffic == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (expire != null) {
            ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expire) * 24 * 60 * 60 * 1001l);
            ai.setStatus("Time premium account");
        } else {
            ai.setTrafficLeft(SizeFormatter.getSize(traffic));
            ai.setStatus("Traffic premium account");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* Force login because they have no (working) cookies */
        login(account, true);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final String hash = br.getRegex("hash=([A-Za-z0-9\\-_]+)\\'").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String dllink = "http://www.stiahni.si/en/download?hash=" + hash;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFID() {
        return new Regex(br.getURL(), "stiahni\\.si/([a-z]{2}/)?file/([A-Za-z0-9]+)").getMatch(1);
    }

    private void prepBr() {
        br.setFollowRedirects(false);
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
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
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}