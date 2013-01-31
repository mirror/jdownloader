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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datei.to", "sharebase.to" }, urls = { "http://(www\\.)?datei\\.to/(datei/[A-Za-z0-9]+\\.html|\\?[A-Za-z0-9]+)", "blablablaInvalid_regexbvj54zjhrß96ujß" }, flags = { 2, 0 })
public class DateiTo extends PluginForHost {

    private static final String  APIPAGE = "http://datei.to/api/jdownloader/";
    private static AtomicBoolean useAPI  = new AtomicBoolean(true);

    public DateiTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datei.to/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://datei.to/agb";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws MalformedURLException {
        String id = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)\\.html$").getMatch(0);
        if (id != null) link.setUrlDownload("http://datei.to/?" + id);
    }

    private void prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            prepBrowser(br);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("op=check&file=");
                for (final DownloadLink dl : links) {
                    correctDownloadLink(dl);
                    sb.append(getFID(dl));
                    sb.append(";");
                }
                br.postPage(APIPAGE, sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = getFID(dllink);
                    if (br.containsHTML(fid + ";offline")) {
                        dllink.setAvailable(false);
                    } else {
                        final String[][] linkInfo = br.getRegex(fid + ";online;([^<>\"/;]*?);(\\d+)").getMatches();
                        if (linkInfo.length != 1) {
                            logger.warning("Linkchecker for datei.to is broken!");
                            return false;
                        }
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(Encoding.htmlDecode(linkInfo[0][0]));
                        dllink.setDownloadSize(Long.parseLong(linkInfo[0][1]));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /** Old linkcheck code can be found in rev 16195 */
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.postPage(APIPAGE, "op=free&step=1&file=" + getFID(downloadLink));
        generalAPIErrorhandling();
        generalFreeAPIErrorhandling();
        final Regex waitAndID = br.getRegex("(\\d+);([A-Za-z0-9]+)");
        if (waitAndID.getMatches().length != 1) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        this.sleep(Long.parseLong(waitAndID.getMatch(0)) * 1001l, downloadLink);
        final String id = waitAndID.getMatch(1);

        for (int i = 0; i <= 5; i++) {
            br.postPage(APIPAGE, "op=free&step=2&id=" + id);
            final String reCaptchaId = br.toString().trim();
            if (reCaptchaId == null) {
                logger.warning("reCaptchaId is null...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(reCaptchaId);
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            br.postPage(APIPAGE, "op=free&step=3&id=" + id + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&recaptcha_challenge_field=" + rc.getChallenge());
            if (!br.containsHTML("(wrong|no input)") && br.containsHTML("ok")) {
                break;
            }
        }
        if (br.containsHTML("(wrong|no input)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        br.postPage(APIPAGE, "op=free&step=4&id=" + id);
        generalFreeAPIErrorhandling();
        if (br.containsHTML("ticket expired")) throw new PluginException(LinkStatus.ERROR_RETRY);
        String dlUrl = br.toString();
        if (dlUrl == null || !dlUrl.startsWith("http") || dlUrl.length() > 500 || dlUrl.contains("no file")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        dlUrl = dlUrl.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, false, 1);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink doesn't seem to be a file...");
            br.followConnection();
            // Shouldn't happen often
            if (br.containsHTML("(window\\.location\\.href=\\'http://datei\\.to/login|form id=\"UploadForm\")")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void generalAPIErrorhandling() throws PluginException {
        if (br.containsHTML("temp down")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, file is temporarily not downloadable!", 30 * 60 * 1000l);
        if (br.containsHTML("no file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private void generalFreeAPIErrorhandling() throws NumberFormatException, PluginException {
        if (br.containsHTML("limit reached")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(br.getRegex("limit reached;(\\d+)").getMatch(0)));
        if (br.containsHTML("download active")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Another free download is already active, please wait before starting new ones.", 5 * 60 * 1000l);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        // to pick up when free account has been picked up from api and throw
        // exception, remove when free account supported.
        account.setProperty("isPremium", true);
        if (useAPI.get() == true) {
            try {
                apiLogin(account);
            } catch (PluginException e) {
                account.setValid(false);
                throw (PluginException) e;
            }
            if (useAPI.get() == true) {
                final Regex accInfo = br.getRegex("premium;(\\d+);(\\d+)");
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(accInfo.getMatch(0)) * 1000l);
                ai.setTrafficLeft(Long.parseLong(accInfo.getMatch(1)));
                ai.setStatus("Premium User");
            }
        } else {
            try {
                webLogin(account, true);
            } catch (PluginException e) {
                account.setValid(false);
                return ai;
            }
            br.getPage("/konto");
            String accountType = br.getRegex(">Konto\\-Typ:</div><div[^>]+><span[^>]+>(.*?)\\-Account</span>").getMatch(0);
            if (accountType != null && accountType.equals("Premium")) {
                // premium account
                String space = br.getRegex(">loadSpaceUsed\\(\\d+, (\\d+)").getMatch(0);
                if (space != null) {
                    ai.setUsedSpace(space + " GB");
                } else {
                    logger.warning("Couldn't find space used!");
                }
                String traffic = br.getRegex("loadTrafficUsed\\((\\d+(\\.\\d+))").getMatch(0);
                if (traffic != null) {
                    ai.setTrafficLeft(SizeFormatter.getSize(traffic + " GB"));
                } else {
                    logger.warning("Couldn't find traffic used!");
                }
                String expire = br.getRegex(">Premium aktiv bis:</div><div[^>]+>(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}) Uhr<").getMatch(0);
                if (expire != null) {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm:ss", Locale.ENGLISH));
                } else {
                    logger.warning("Couldn't find expire date!");
                }
            } else if (accountType != null && accountType.equals("Free")) {
                // free account not supported yet...
                account.setProperty("isPremium", false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                // account type == not found or not supported?
                logger.warning("Can't determine account type.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }

        }
        return ai;
    }

    public void apiLogin(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.postPage(APIPAGE, "op=login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));

        if (!br.containsHTML("premium;")) {
            logger.info("Free account found->Not supported->Disable!");
            account.setProperty("isPremium", false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.containsHTML("wrong login")) {
            logger.info("Wrong login or password entered!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public void webLogin(final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        try {
            /** Load cookies */
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
                        this.br.setCookie(this.getHost(), key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(false);
            br.postPage("http://datei.to/response/login", "Login_User=" + Encoding.urlEncode(account.getUser()) + "&Login_Pass=" + Encoding.urlEncode(account.getPass()));
            if (br.getCookie(this.getHost(), "User") == null || br.getCookie(this.getHost(), "Pass") == null) {
                logger.warning("Not a valid user:password");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            /** Save cookies */
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(this.getHost());
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

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        if (useAPI.get() == true) {
            // api dl
            requestFileInformation(downloadLink);
            br.postPage(APIPAGE, "op=premium&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&file=" + getFID(downloadLink));
            generalAPIErrorhandling();
            if (br.containsHTML("no premium")) {
                logger.info("Cannot start download, this is no premium account anymore...");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("wrong login")) {
                logger.info("Cannot start download, username or password wrong!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            String dlUrl = br.toString();
            if (dlUrl == null || !dlUrl.startsWith("http") || dlUrl.length() > 500 || dlUrl.contains("no file")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            dlUrl = dlUrl.trim();
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 0);
            br.setFollowRedirects(true);
            if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                logger.severe("PremiumError: " + br.toString());
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            dl.startDownload();
        } else {
            // web dl
            requestFileInformation(downloadLink);
            webLogin(account, false);
            br.setFollowRedirects(false);
            // direct downloads
            br.getPage(downloadLink.getDownloadURL());
            String dllink = br.getRedirectLocation();
            if (dllink == null || !dllink.matches("(https?://\\w+\\.datei\\.to/file/[a-z0-9]{32}/[A-Za-z0-9]{8}/[A-Za-z0-9]{10}/[^\"\\']+)")) {
                // direct download failed to match or disabled feature in users
                // profile
                String id = br.getRegex("<button id=\"([^\"]+)\">Download starten<").getMatch(0);
                if (id == null) {
                    logger.warning("'id' could not be found");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.postPage("/response/download", "Step=1&ID=" + id);
                dllink = br.getRegex("(https?://\\w+\\.datei\\.to/dl/[A-Za-z0-9]+)").getMatch(0);
                br.setFollowRedirects(true);
            }
            if (dllink == null) {
                logger.warning("Could not find dllink");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
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