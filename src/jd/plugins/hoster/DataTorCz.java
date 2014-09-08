//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 21813 $", interfaceVersion = 2, names = { "datator.cz" }, urls = { "http://(?:www\\.)?datator\\.cz/soubor-ke-stazeni-.*?(\\d+)\\.html" }, flags = { 2 })
public class DataTorCz extends PluginForHost {

    // devnotes
    // number before .html is the file uid
    // to make valid link soubor-ke-stazeni- + number + .html

    private static Object ACCLOCK  = new Object();
    private final String  language = System.getProperty("user.language");

    public DataTorCz(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.datator.cz/navysit-kredit.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.datator.cz/clanek-vseobecne-podminky-3.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        final String msg = "účet Povinné - Account Required";
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, msg);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // offline
        if (br.getURL().endsWith("datator.cz/404.html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final String[] fileInfo = br.getRegex("content=\".*?: (.*?) \\| \\d{1,2}\\.\\d{1,2}\\.\\d{4} \\|  ([\\d A-Z]+)\" />").getRow(0);
        if (fileInfo != null) {
            if (fileInfo[1] != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(fileInfo[1].replaceAll("\\s", "")));
            }
            if (fileInfo[0] != null) {
                downloadLink.setFinalFileName(fileInfo[0]);
            } else {
                return AvailableStatus.FALSE;
            }
        }
        // In case the link redirects to the finallink
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, ai, true, true);
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, AccountInfo importedAI, final boolean loginFull, final boolean loginInfo) throws Exception {
        synchronized (ACCLOCK) {
            // we need accountInfo reference for below!
            AccountInfo ai = importedAI;
            if (ai == null) {
                ai = new AccountInfo();
            }
            // used in finally to restore browser redirect status.
            final boolean frd = br.isFollowingRedirects();
            try {
                /** Load cookies */
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = (Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser()))) && Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass()))) && ret != null && ret instanceof HashMap<?, ?> && account.isValid() && !loginFull ? true : false);
                if (acmatch) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    // hashmap could theoretically be empty.
                    if (!cookies.isEmpty()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(getHost(), key, value);
                        }
                    } else {
                        acmatch = false;
                    }
                }
                if (!acmatch || loginFull) {
                    br.setFollowRedirects(true);
                    br.postPage("http://www.datator.cz/prihlaseni.html", "username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&permLogin=on&Ok=Log+in&login=1");
                    if (br.getCookie(getHost(), "permLoginKey") == null || br.containsHTML("Špatný email nebo heslo")) {
                        if ("de".equalsIgnoreCase(language)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    /** Save cookies */
                    final HashMap<String, String> cookies = new HashMap<String, String>();
                    final Cookies add = br.getCookies(getHost());
                    for (final Cookie c : add.getCookies()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                    account.setProperty("name", Encoding.urlEncode(account.getUser()));
                    account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                    account.setProperty("cookies", cookies);
                    account.setProperty("lastlogin", System.currentTimeMillis());
                }
                // fetch account info, we need it here, so that we can always determine free|premium status.
                if (!acmatch || loginInfo) {
                    // required for when we don't login fully.
                    if (br.getURL() == null) {
                        br.setFollowRedirects(true);
                        br.getPage("/");
                    }
                    // seems to be a credit based system no date of expire
                    String credit = br.getRegex("class=\"credit\" title=\"Kredit\\s*([\\d A-Z]+)\"").getMatch(0);
                    if (credit != null) {
                        ai.setTrafficLeft(SizeFormatter.getSize(credit.replaceAll("\\s+", "")));
                    }
                    if (ai.getTrafficLeft() >= 0) {
                        account.setValid(true);
                        ai.setStatus("Premium Account");
                    } else {
                        ai.setStatus("Free Account");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free accounts are not supported!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }

                    // this basically updates account info before returning.
                    if (importedAI == null) {
                        account.setAccountInfo(ai);
                    } else {
                        importedAI = ai;
                    }
                }
            } catch (final PluginException e) {
                resetAccount(account);
                throw e;
            } finally {
                br.setFollowRedirects(frd);
            }
        }
    }

    private Browser prepBrowser(Browser prepBr) {
        return prepBr;
    }

    private String getFUID(final String link) {
        return new Regex(link, getSupportedLinks()).getMatch(0);
    }

    private String getFUID(final DownloadLink downloadLink) {
        return getFUID(downloadLink.getDownloadURL());
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String fuid = getFUID(downloadLink);
        if (fuid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        requestFileInformation(downloadLink);
        login(account, null, false, false);
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRegex("<a href=\"(https?://" + fuid + "[^\"]*\\.datator\\.cz/[^\"]+-" + fuid + "\\.html)\"[^>]*class=\"button_download\"[^>]*>").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /**
     * resets provided accounts properties fields. This will allow next login to be a full login!
     *
     * @author raztoki
     * */
    private void resetAccount(final Account account) {
        synchronized (ACCLOCK) {
            account.setProperty("cookies", Property.NULL);
            account.setProperty("lastlogin", Property.NULL);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}