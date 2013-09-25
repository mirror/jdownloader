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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fernsehkritik.tv" }, urls = { "http://(couch\\.)?fernsehkritik\\.tv/(jdownloaderfolge\\d+(\\-\\d)?\\.flv|inline\\-video/postecke\\.php\\?(iframe=true\\&width=\\d+\\&height=\\d+\\&ep=|ep=)\\d+|dl/fernsehkritik\\d+\\.[a-z0-9]{1,4}|folge-\\d+.*|userbereich/archive#stream:\\d+)" }, flags = { 2 })
public class FernsehkritikTv extends PluginForHost {

    // Refactored on the 02.07.2011, Rev. 14521,
    // http://svn.jdownloader.org/projects/jd/repository/revisions/14521
    public FernsehkritikTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://couch.fernsehkritik.tv/register.php");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://fernsehkritik.tv/datenschutzbestimmungen/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String POSTECKE    = "http://fernsehkritik\\.tv/inline\\-video/postecke\\.php\\?(iframe=true\\&width=\\d+\\&height=\\d+\\&ep=|ep=)\\d+";
    private static final String COUCH       = "http://couch\\.fernsehkritik\\.tv.*";
    private static final String COUCHSTREAM = "http://couch\\.fernsehkritik\\.tv/userbereich/archive#stream:.*";
    private static final String COUCHHOST   = "http://couch.fernsehkritik.tv";
    private static Object       LOCK        = new Object();
    private static final String LOGIN_ERROR = "Login fehlerhaft";
    private String              DLLINK      = null;
    private static final String DL_AS_MOV   = "DL_AS_MOV";
    private static final String DL_AS_MP4   = "DL_AS_MP4";
    private static final String DL_AS_FLV   = "DL_AS_FLV";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, AccountController.getInstance().getValidAccount(this));
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink, Account account) throws Exception {
        DLLINK = null;
        if (downloadLink.getDownloadURL().matches(POSTECKE)) {
            final String episodenumber = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            br.getPage("http://fernsehkritik.tv/folge-" + episodenumber + "/Start/");
            final String date = br.getRegex("var flattr_tle = \\'Fernsehkritik\\-TV Folge \\d+ vom(.*?)\\'").getMatch(0);
            if (date == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(downloadLink.getDownloadURL());
            DLLINK = br.getRegex("playlist = \\[ \\{ url: \\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (DLLINK == null) DLLINK = br.getRegex("\\'(http://dl\\d+\\.fernsehkritik\\.tv/postecke/postecke\\d+\\.flv)\\'").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setFinalFileName("Fernsehkritik-TV Postecke " + episodenumber + " vom " + date + ".flv");
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else if (downloadLink.getDownloadURL().matches(COUCHSTREAM)) {
            final String episodenumber = new Regex(downloadLink.getDownloadURL(), "archive#stream:(.*?)$").getMatch(0);
            br.getPage("http://fernsehkritik.tv/folge-" + episodenumber + "/Start/");
            final String date = br.getRegex("var flattr_tle = \\'Fernsehkritik\\-TV Folge \\d+ vom(.*?)\\'").getMatch(0);
            if (account != null) {
                try {
                    login(account, false);
                } catch (final PluginException e) {
                    return AvailableStatus.UNCHECKABLE;
                }
            } else {
                return AvailableStatus.UNCHECKABLE;
            }
            br.getPage(downloadLink.getDownloadURL());
            final Regex reg = br.getRegex("id:\"(\\d+)\", hash:\"([a-z0-9]+)\", stamp:\"([a-z0-9]+)\"");
            final String id = reg.getMatch(0);
            final String hash = reg.getMatch(1);
            final String stamp = reg.getMatch(2);
            br.getPage("http://couch.fernsehkritik.tv/dl/getData2.php?mode=stream&ep=" + episodenumber + "&id=" + id + "&hash=" + hash + "&stamp=" + stamp + "&j=0");
            DLLINK = "http://couch.fernsehkritik.tv" + br.getRegex("\'file\': \"(/dl/\\d+-[a-z0-9]+-[a-z0-9]+-\\d+\\.flv)\"").getMatch(0);

            if (date == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            URLConnectionAdapter con = null;
            try {
                downloadLink.setFinalFileName("Fernsehkritik-TV Folge " + episodenumber + " vom " + date + ".flv");
                br.setFollowRedirects(true);
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    br.setFollowRedirects(false);
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else if (downloadLink.getDownloadURL().matches(COUCH)) {
            final String episodenumber = new Regex(downloadLink.getDownloadURL(), "fernsehkritik(\\d+)\\..*?$").getMatch(0);
            br.getPage("http://fernsehkritik.tv/folge-" + episodenumber + "/Start/");
            final String date = br.getRegex("var flattr_tle = \\'Fernsehkritik\\-TV Folge \\d+ vom(.*?)\\'").getMatch(0);
            if (date == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            URLConnectionAdapter con = null;
            try {
                String extension = new Regex(downloadLink.getDownloadURL(), "fernsehkritik(\\d+)\\.(.*?)$").getMatch(1);
                if (extension == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                downloadLink.setFinalFileName("Fernsehkritik-TV Folge " + episodenumber + " vom " + date + "." + extension);
                if (account != null) {
                    try {
                        login(account, false);
                    } catch (final PluginException e) {
                        return AvailableStatus.UNCHECKABLE;
                    }
                } else {
                    return AvailableStatus.UNCHECKABLE;
                }
                br.setFollowRedirects(true);
                con = br.openGetConnection(downloadLink.getDownloadURL());
                if (!con.getContentType().contains("html")) {
                    DLLINK = downloadLink.getDownloadURL();
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    br.setFollowRedirects(false);
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (!(account.getUser().matches(".+@.+"))) {
            ai.setStatus("Please enter your E-Mail adress as username!");
            account.setValid(false);
            return ai;
        }
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        String expire = br.getRegex("gültig bis zum:.*?<strong>(.*?)</strong>").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm", Locale.UK));
        }
        return ai;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        AvailableStatus availStatus = requestFileInformation(downloadLink);
        if (AvailableStatus.UNCHECKABLE.equals(availStatus)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only");
            }
        }
        br.setFollowRedirects(false);
        if (!downloadLink.getDownloadURL().matches(POSTECKE)) {
            br.getPage("http://fernsehkritik.tv/js/directme.php?file=" + new Regex(downloadLink.getDownloadURL(), "fernsehkritik\\.tv/jdownloaderfolge(.+)").getMatch(0));
            DLLINK = br.getRedirectLocation();
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // More chunks work but download will stop at random point then
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        AvailableStatus ret = requestFileInformation(link, account);
        if (AvailableStatus.UNCHECKABLE.equals(ret)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only");
            }
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
        // if (link.getDownloadURL().matches("http://fernsehkritik\\.tv/folge-.*")) {
        // /* TODO */
        // String folge = new Regex(link.getDownloadURL(), "http://fernsehkritik\\.tv/folge-(.*?)").getMatch(0);
        // link.setUrlDownload("http://couch.fernsehkritik.tv/fernsehkritik" + folge + ".mp4");
        // br.setFollowRedirects(true);
        // dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 1);
        // }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.getHeaders().put("Accept-Language", "de-de,de;q=0.8");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(COUCHHOST, key, value);
                        }
                        return;
                    }
                }
                br.getHeaders().put("Accept-Encoding", "gzip");
                br.setFollowRedirects(true);
                br.getPage(COUCHHOST);
                br.postPage(COUCHHOST + "/login.php", "location=&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&setCookie=set");
                if (br.containsHTML(LOGIN_ERROR)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                if (br.getCookie(COUCHHOST, "couchlogin") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(COUCHHOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DL_AS_MOV, JDL.L("plugins.hoster.fernsehkritik.mov", "Load Free Streams as Premium .mov")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DL_AS_MP4, JDL.L("plugins.hoster.fernsehkritik.mp4", "Load Free Streams as Premium .mp4")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DL_AS_FLV, JDL.L("plugins.hoster.fernsehkritik.flv", "Load Free Streams as Premium .flv")).setDefaultValue(true));

    }
}