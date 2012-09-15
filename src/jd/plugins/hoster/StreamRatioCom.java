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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "streamratio.com" }, urls = { "http://(www\\.)?streamratio\\.com/(files|get)/[A-Za-z0-9]+(/[^<>\"/]+)?\\.html" }, flags = { 2 })
public class StreamRatioCom extends PluginForHost {

    // DEV NOTES
    // mods:
    // non account: 1 * 2
    // free account: same as above
    // premium account: 1 * 5?
    // protocol: no https
    // captchatype:
    // other: no redirects

    private static final String  MAINPAGE = "http://www.streamratio.com";
    private static Object        LOCK     = new Object();
    private static AtomicInteger maxPrem  = new AtomicInteger(1);

    public StreamRatioCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/premium.html");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/help/terms.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/get/", "/files/"));
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(MAINPAGE, "lang", "english");
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser();
        br.getPage(link.getDownloadURL());
        handleErrors();
        Regex fileInfo = br.getRegex(">Download:\\&nbsp;<h1>([^<>\"]*?) \\((\\d+(\\.\\d+)? [A-Za-z]*?)\\)</h1>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set final filename here because hoster taggs files
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("/files/", "/get/"));
        Browser br2 = br.cloneBrowser();
        br2.getPage(br.getRegex("src=\"\\.(/[\\w/]+/core\\.js)").getMatch(0));
        String ttt = br.getRegex("var waitSecs = (\\d+);").getMatch(0);
        String fileid = br.getRegex("var file_id = '([^\\']+)\\';").getMatch(0);
        if (fileid == null) fileid = new Regex(downloadLink.getDownloadURL(), "/(.+)\\.html").getMatch(0);
        int tt = 60;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 240) {
            // 10 Minutes reconnect wait is not enough, let's wait 1 hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        sleep(tt * 1001l, downloadLink);
        String form = br2.getRegex("(<form.*</form>)").getMatch(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form = form.replace("' + base + '", "").replace("' + file_id + '", fileid).replace("' + timestamp + '", "" + System.currentTimeMillis());
        Form dlForm = new Form(form);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlForm, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
            if (unknownError != null) logger.warning("Unknown error occured: " + unknownError);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String[][] space = br.getRegex(">Used Space: ([\\d\\.]+) (KB|MB|GB|TB)<").getMatches();
        if ((space != null && space.length != 0) && (space[0][0] != null && space[0][1] != null)) ai.setUsedSpace(space[0][0] + " " + space[0][1]);
        account.setValid(true);
        ai.setUnlimitedTraffic();
        if (account.getBooleanProperty("nopremium")) {
            ai.setStatus("Registered (free) User");
            try {
                maxPrem.set(2);
                account.setMaxSimultanDownloads(2);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
        } else {
            String expire = br.getRegex(">Premium Expires:(\\d{2}\\-\\d{2}\\-\\d{4} @ \\d{2}:\\d{2}:\\d{2})<").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                expire = expire.replaceAll("\\@ ", "");
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy hh:mm:ss", null));
                try {
                    maxPrem.set(5);
                    account.setMaxSimultanDownloads(5);
                    account.setConcurrentUsePossible(true);
                } catch (final Throwable e) {
                }
            }
            ai.setStatus("Premium User");
        }
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser();
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
                br.postPage(MAINPAGE + "/account/login", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&task=dologin&return=http%3A%2F%2Fstreamratio.com%2Fmembers%2Fmyfiles");
                if (!br.getURL().contains("/members/myfiles")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (!br.getRegex(">Account type: <strong>(Premium Member)<").matches()) {
                    account.setProperty("nopremium", true);
                } else {
                    account.setProperty("nopremium", false);
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
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        if (account.getBooleanProperty("nopremium")) {
            handleFree(link);
        } else {
            requestFileInformation(link);
            login(account, false);
            br.setFollowRedirects(false);
            String dllink = null;
            if (dllink == null) {
                br.getPage(link.getDownloadURL().replace("/files/", "/get/"));
                Browser br2 = br.cloneBrowser();
                br2.getPage(br.getRegex("src=\"\\.(/[\\w/]+/core\\.js)").getMatch(0));
                String ttt = br.getRegex("var waitSecs = (\\d+);").getMatch(0);
                String fileid = br.getRegex("var file_id = '([^\\']+)\\';").getMatch(0);
                if (fileid == null) fileid = new Regex(link.getDownloadURL(), "/(.+)\\.html").getMatch(0);
                int tt = 5;
                if (ttt != null) tt = Integer.parseInt(ttt);
                sleep(tt * 1001l, link);
                String form = br2.getRegex("(<form.*</form>)").getMatch(0);
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                form = form.replace("' + base + '", "").replace("' + file_id + '", fileid).replace("' + timestamp + '", "" + System.currentTimeMillis());
                Form dlForm = new Form(form);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlForm, false, 1);
                if (dl.getConnection().getContentType().contains("html")) {
                    br.followConnection();
                    handleErrors();
                    final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
                    if (unknownError != null) logger.warning("Unknown error occured: " + unknownError);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    private void handleErrors() throws Exception {
        logger.info("Handling errors...");
        if (br.containsHTML("(>The file you have requested (cannot be found|does not exist)|>File not found|>The file was deleted by the uploader)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Your download link is invalid or has expired, please try again\\.<")) throw new PluginException(LinkStatus.ERROR_RETRY, "Hoster issue?", 10 * 60 * 1000l);
        if (br.containsHTML("(>You can only download a max of \\d+ files per hour as a free user\\!<|>Los usuarios de Cuenta Gratis pueden descargar)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}