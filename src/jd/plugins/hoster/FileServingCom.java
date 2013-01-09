//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserving.com" }, urls = { "http://(www\\.)?fileserving\\.com/files/[a-zA-Z0-9_\\-]+" }, flags = { 2 })
public class FileServingCom extends PluginForHost {

    public String                FILEIDREGEX  = "fileserving\\.com/files/([a-zA-Z0-9_\\-]+)(http:.*)?";
    private final String         FILEOFFLINE  = "(<h1>File not available</h1>|<b>The file could not be found\\. Please check the download link)";
    private boolean              isRegistered = false;

    private static AtomicInteger maxDls       = new AtomicInteger(-1);
    private static Object        LOCK         = new Object();

    // DEV NOTES
    // other: unsure about the expire time, if it only shows days or what...
    // only had one account to play with.
    // belongs to mydrive.com

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // All links should look the same to get no problems with regexing them
        // later
        link.setUrlDownload("http://www.fileserving.com/files/" + getID(link));
    }

    public FileServingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fileserving.com/Public/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileserving.com/Public/term";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private String getID(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("(?i)>Sorry, this file has been removed\\. It may have been deleted by the uploader or due to the received complaint\\.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            final String fid = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);
            String sid = br.getRegex("sid:\\'(\\d+)\\'").getMatch(0);
            if (sid == null) sid = "";
            String server = br.getRegex("server:\\'([^<>\"\\']+)\\'").getMatch(0);
            if (server == null) server = "";
            final String rcID = br.getRegex("k=([^\\'<>\"]+)\"").getMatch(0);
            if (fid == null || rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            Browser xmlBrowser = br.cloneBrowser();
            xmlBrowser.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(new Form());
            rc.setId(rcID);
            for (int i = 0; i <= 5; i++) {
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                xmlBrowser.getPage("http://www.fileserving.com/Index/verifyRecaptcha?fid=" + fid + "&sid=" + sid + "&server=" + server + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                if (xmlBrowser.containsHTML("\"error\"")) continue;
                break;
            }
            if (xmlBrowser.containsHTML("\"error\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            final String correctedXml = xmlBrowser.toString().replace("\\", "");
            dllink = new Regex(correctedXml, "Your download is ready\\! <a href=\"(http:[^<>\"]+)\"").getMatch(0);
            if (dllink == null) dllink = new Regex(correctedXml, "\"(http://s\\d+\\.fileserving\\.com/file/" + fid + "/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (this.checkLinks(new DownloadLink[] { link }) == false) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            br.setCustomCharset("utf-8");
            this.br.getPage("http://www.fileserving.com/Public/linkchecker");
            final String hash = br.getRegex("name=\"__hash__\" value=\"([a-z0-9]+)\"").getMatch(0);
            if (hash == null) {
                logger.warning("Fileserving.com availablecheck is broken!");
                return false;
            }
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("__hash__=" + hash + "&links=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links, probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                this.br.postPage("http://www.fileserving.com/Public/linkchecker", sb.toString());
                for (final DownloadLink dl : links) {
                    final String fileid = new Regex(dl.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Fileserving.com availablecheck is broken!");
                        return false;
                    }
                    Regex fileInfo = br.getRegex("(fileserving\\.com/files/" + fileid + "[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]+)</td>[\t\n\r ]+<td>([^<>\"]+)</td>[\t\n\r ]+<td  class=\"staus\">[\t\r\n ]+<span class=\"([^<>\"]+)\"></span>)");
                    String filename = fileInfo.getMatch(1);
                    String filesize = fileInfo.getMatch(2);
                    String status = fileInfo.getMatch(3);

                    if (status.contains("removed")) {
                        dl.setAvailable(false);
                        continue;
                    }
                    // not sure if the below if is still needed.
                    if (br.containsHTML("class=\"icon_file_check_notvalid\"></span>[\t\n\r ]+http://(www\\.)?fileserving\\.com/files/" + fileid)) {
                        dl.setAvailable(false);
                        continue;
                    } else if (filename == null || status == null) {
                        // best to not set false for filesize.
                        logger.warning("Fileserving.com availablecheck is possibly broken! Please report this to JD Development team");
                        dl.setAvailable(false);
                    } else if (status.contains("valid")) {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    if (filesize != null) {
                        if (filesize.contains(",") && filesize.contains(".")) {
                            /* workaround for 1.000,00 MB bug */
                            filesize = filesize.replaceFirst("\\.", "");
                        }
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
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

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (!isRegistered) {
            br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            final String dllink = br.getRedirectLocation();
            // not sure about below, but can't hurt either way
            if (dllink == null) {
                if (br.containsHTML(FILEOFFLINE)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            handleFree(link);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("(?i)Expire in <b>(\\d+)</b>[\r\n\t ]+days\\.\\)").getMatch(0);
        if (expire == null) {
            // ai.setExpired(true);
            isRegistered = true;
            ai.setStatus("Free User");
            try {
                maxDls.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable noin09581Stable) {
            }
        } else {
            try {
                maxDls.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable noin09581Stable) {
            }
            ai.setValidUntil(System.currentTimeMillis() + (Integer.parseInt(expire) * 24 * 60 * 60 * 1000l));
            ai.setStatus("Premium User");
        }
        return ai;
    }

    private void login(final Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
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
                            this.br.setCookie("http://www.fileserving.com", key, value);
                        }
                        return;
                    }
                }

                /* reset maxPrem workaround on every fetchaccount info */
                maxDls.set(1);
                br.setFollowRedirects(true);
                setBrowserExclusive();
                br.setCustomCharset("utf-8");
                br.getPage("http://www.fileserving.com/Public/login");
                Form login = br.getForm(0);
                if (login == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                if (br.containsHTML("Wrong password")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (!br.getURL().contains("/My/index")) {
                    br.getPage("http://www.fileserving.com/My/index");
                    if (br.containsHTML("Please login first")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (!br.containsHTML("Expire in <b>\\d+") || !br.containsHTML("(?i)Account Type: <span>Premium</span>")) {
                    if (account.getBooleanProperty("premium", false) == true) {
                        logger.info("DEBUG: " + br.toString());

                    }
                    // ai.setExpired(true);
                    isRegistered = true;
                    try {
                        maxDls.set(1);
                        account.setMaxSimultanDownloads(1);
                        account.setConcurrentUsePossible(false);
                    } catch (final Throwable noin09581Stable) {
                    }
                } else {
                    account.setProperty("premium", true);
                    try {
                        maxDls.set(-1);
                        account.setMaxSimultanDownloads(-1);
                        account.setConcurrentUsePossible(true);
                    } catch (final Throwable noin09581Stable) {
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://www.fileserving.com");
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
    public void resetDownloadlink(DownloadLink link) {
    }

}