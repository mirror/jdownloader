//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "onlinetvrecorder.com" }, urls = { "http://(www\\.)?81\\.95\\.11\\.\\d{1,2}/download/\\d+/\\d+/\\d+/[a-z0-9]{32}/de/[^<>\"/]+" }, flags = { 2 })
public class OnlineTvRecorderCom extends PluginForHost {

    public OnlineTvRecorderCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://www.onlinetvrecorder.com/";
    }

    private String DLLINK            = null;
    private long   TIMEDIFF          = 0;
    private long   CURRENTTIMEMILLIS = 0;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, ParseException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] { 503 });
        } catch (Throwable e) {
        }
        final String filename = new Regex(downloadLink.getDownloadURL(), "/([^<>\"/]+)$").getMatch(0);
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));

        Calendar cal = new GregorianCalendar();
        CURRENTTIMEMILLIS = cal.get(Calendar.HOUR_OF_DAY) * 60 * 60;
        CURRENTTIMEMILLIS += cal.get(Calendar.MINUTE) * 60;
        CURRENTTIMEMILLIS += cal.get(Calendar.SECOND);
        CURRENTTIMEMILLIS = CURRENTTIMEMILLIS * 1000;
        long expiredTime = 24 * 60 * 60 * 1000l; // 00:00Uhr
        TIMEDIFF = expiredTime - CURRENTTIMEMILLIS; // Differenz aktuelle
                                                    // Uhrzeit bis 00:00Uhr
        if (TIMEDIFF > (8 * 60 * 60 * 1000) && (CURRENTTIMEMILLIS >= (24 * 60 * 60 * 1000)) || CURRENTTIMEMILLIS < (8 * 60 * 60 * 1000)) {
            // In case the link redirects to the finallink
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(downloadLink.getDownloadURL());
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    DLLINK = downloadLink.getDownloadURL();
                } else {
                    if (con.getResponseCode() == 400) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    // Stable workaround
                    get(downloadLink.getDownloadURL());
                    if (br.containsHTML(">Aufnahme nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }

        } else {
            downloadLink.getLinkStatus().setStatusText("Not downloadable before 12 o'clock.");
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (TIMEDIFF > (8 * 60 * 60 * 1000) && (CURRENTTIMEMILLIS >= (24 * 60 * 60 * 1000)) || CURRENTTIMEMILLIS < (8 * 60 * 60 * 1000)) {
            if (DLLINK == null) {
                final String apilink = br.getRegex("var apilink = \"(http://[^<>\"]*?)\"").getMatch(0);
                if (apilink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // 2 hours waiting = maximum
                for (int i = 1; i <= 240; i++) {
                    br.getPage(apilink);
                    DLLINK = getData("filedownloadlink");
                    if (DLLINK != null) break;
                    final String currentPosition = getData("queueposition");
                    if (currentPosition != null) downloadLink.getLinkStatus().setStatusText("In queue, current position is: " + currentPosition);
                    logger.info("In queue, current position is: " + currentPosition);
                    int wait = 30;
                    final String waittime = getData("refreshTime");
                    if (waittime != null) wait = Integer.parseInt(waittime);
                    sleep(wait * 1000l, downloadLink);
                }
                if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                DLLINK = DLLINK.replace("\\", "");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, -2);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Not downloadable before 12 o'clock.", TIMEDIFF);
        }
    }

    private String getData(final String parameter) {
        // "queueposition":93
        return br.getRegex("\"" + parameter + "\":( \")?([^<>\"]*?)(\"|,)").getMatch(1);
    }

    private static final String MAINPAGE = "http://onlinetvrecorder.com";
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
                // br.getPage("");
                br.setCookie(MAINPAGE, "OTRCOUNTRY", "DE");
                br.postPage("https://www.onlinetvrecorder.com/v2/?go=login", "rememberlogin=on&btn_login=+Anmelden+&backto=%3F&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("Diese E-Mail\\-Adresse ist uns nicht bekannt")) {
                    logger.info("Invalid mail-adress!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.containsHTML("Das Passwort ist nicht korrekt")) {
                    logger.info("Invalid password!");
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
        br.getPage(MAINPAGE);
        final String points = br.getRegex("<td align=\"right\"><a href=\"\\?aktion=gwp\">(\\d+(\\.\\d+)?)</a>").getMatch(0);
        if (points != null)
            ai.setStatus("Registered User - " + points + " points left");
        else
            ai.setStatus("Registered User");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, -2);
            } else {
                br.followConnection();
                if (br.containsHTML("<div id=\"error_message\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void get(final String parameter) throws UnsupportedEncodingException, IOException {
        try {
            br.getPage(parameter);
        } catch (Throwable t) {
            String str = readInputStreamToString(br.getHttpConnection().getErrorStream());
            br.getRequest().setHtmlCode(str);
        }
    }

    public static String readInputStreamToString(InputStream fis) throws UnsupportedEncodingException, IOException {
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(fis, "UTF8"));
            String line;
            StringBuilder ret = new StringBuilder();
            String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }
                ret.append(line);
            }
            return ret.toString();
        } finally {
            try {
                f.close();
            } catch (Throwable e) {
            }

        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}