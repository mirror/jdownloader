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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datator.cz" }, urls = { "http://(?:www\\.)?datator\\.cz/soubor-ke-stazeni-.*?(\\d+)\\.html" })
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

    /**
     * Rules to prevent new downloads from commencing
     *
     */
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            return true;
        }
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

    private static AtomicBoolean useAPI = new AtomicBoolean(false);

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
            if (accounts != null && accounts.size() != 0) {
                // lets sort, premium > non premium
                Collections.sort(accounts, new Comparator<Account>() {
                    @Override
                    public int compare(Account o1, Account o2) {
                        final int io1 = o1.getBooleanProperty("free", false) ? 0 : 1;
                        final int io2 = o2.getBooleanProperty("free", false) ? 0 : 1;
                        return io1 <= io2 ? io1 : io2;
                    }
                });
                Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        account = n;
                        break;
                    }
                }
            }
        }
        if (useAPI.get() && account != null) {
            // api can only be used with an account....
            br.getPage("http://japi.datator.cz/api.php?action=getFileInfo&hash=" + getHash(account) + "&url=" + Encoding.urlEncode(downloadLink.getDownloadURL()));
            final String filename = PluginJSonUtils.getJsonValue(br, "filename");
            final String filesize = PluginJSonUtils.getJsonValue(br, "size");
            final String error = PluginJSonUtils.getJsonValue(br, "error");
            if (inValidate(filename)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if ("file no exist".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if ("auth error".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
                // dump..
                account.setProperty("hash", Property.NULL);
                // try web
                return requestFileInformationWeb(downloadLink);
            }
            downloadLink.setName(filename);
            if (!inValidate(filesize)) {
                try {
                    downloadLink.setVerifiedFileSize(Long.parseLong(filesize));
                } catch (final Throwable t) {
                    downloadLink.setDownloadSize(Long.parseLong(filesize));
                }
            }
            return AvailableStatus.TRUE;
        } else {
            return requestFileInformationWeb(downloadLink);
        }
    }

    @SuppressWarnings("deprecation")
    private AvailableStatus requestFileInformationWeb(DownloadLink downloadLink) throws Exception {
        prepBRWebsite(this.br);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().endsWith("datator.cz/404.html") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 410) {
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

    private Browser prepBRWebsite(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        return br;
    }

    private final String getHash(final Account account) throws Exception {
        String hash = account.getStringProperty("hash", null);
        if (hash == null) {
            // login again?
            account.setAccountInfo(fetchAccountInfo(account));
            hash = account.getStringProperty("hash", null);
            if (hash == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return hash;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (useAPI.get()) {
            // we will use api
            br.getPage("http://japi.datator.cz/api.php?action=login&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final String hash = PluginJSonUtils.getJsonValue(br, "hash");
            final String credits = PluginJSonUtils.getJsonValue(br, "kredit");
            if ("auth error".equals(PluginJSonUtils.getJsonValue(br, "error")) || hash == null) {
                if ("de".equalsIgnoreCase(language)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.setProperty("hash", hash);
            ai.setTrafficLeft(SizeFormatter.getSize(credits + " MiB"));
            if (ai.getTrafficLeft() >= 0) {
                account.setValid(true);
                ai.setStatus("Premium Account");
                account.setProperty("free", false);
            } else {
                ai.setStatus("Free Account");
                account.setProperty("free", true);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free accounts are not supported!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else {
            // web (disabled)
            login(account, ai, true, true);
        }
        return ai;
    }

    /**
     * disabled..
     *
     * @author raztoki
     * @param account
     * @param importedAI
     * @param loginFull
     * @param loginInfo
     * @throws Exception
     */
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
        String dllink = null;
        int chunks = 1;
        boolean resumes = false;
        if (useAPI.get()) {
            requestFileInformation(downloadLink, account);
            br.getPage("http://japi.datator.cz/api.php?action=getDownloadLink&hash=" + getHash(account) + "&url=" + Encoding.urlEncode(downloadLink.getDownloadURL()));
            if ("auth error".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
                // dump..
                account.setProperty("hash", Property.NULL);
                // retry
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            dllink = PluginJSonUtils.getJsonValue(br, "downloadLink");
            final String c = PluginJSonUtils.getJsonValue(br, "chunks");
            if (c != null && c.matches("-?\\d+")) {
                chunks = Integer.parseInt(c);
                chunks = chunks < 1 ? -chunks : chunks;
            }
            final String r = PluginJSonUtils.getJsonValue(br, "resumes");
            if (r != null && r.matches("true|false")) {
                resumes = Boolean.parseBoolean(r);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            String fuid = getFUID(downloadLink);
            if (fuid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            requestFileInformation(downloadLink);
            login(account, null, false, false);
            br.getPage(downloadLink.getDownloadURL());
            dllink = br.getRegex("<a href=\"(https?://" + fuid + "[^\"]*\\.datator\\.cz/[^\"]+-" + fuid + "\\.html)\"[^>]*class=\"button_download\"[^>]*>").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
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
     */
    private void resetAccount(final Account account) {
        synchronized (ACCLOCK) {
            account.setProperty("cookies", Property.NULL);
            account.setProperty("lastlogin", Property.NULL);
        }
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
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