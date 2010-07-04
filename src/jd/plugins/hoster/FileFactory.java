//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefactory.com" }, urls = { "http://[\\w\\.]*?filefactory\\.com(/|//)file/[\\w]+/?" }, flags = { 2 })
public class FileFactory extends PluginForHost {

    private static final String FILESIZE = "<span>(.*? (B|KB|MB|GB)) file";

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String SLOTEXPIRED = "<p>Your download slot has expired\\.";

    private static final String LOGIN_ERROR = "The email or password you have entered is incorrect";

    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";

    public FileFactory(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filefactory.com/info/premium.php");
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 200;
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        requestFileInformation(parameter);
        try {
            handleFree0(parameter);
        } catch (PluginException e4) {
            throw e4;
        } catch (InterruptedException e2) {
            return;
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            if (e.getMessage() != null && e.getMessage().contains("502")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (e.getMessage() != null && e.getMessage().contains("503")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw e;
            }
        }
    }

    public void checkErrors() throws PluginException {
        if (br.containsHTML(SLOTEXPIRED)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        if (br.containsHTML("there are currently no free download slots")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        if (br.containsHTML(NOT_AVAILABLE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(SERVER_DOWN) || br.containsHTML(NO_SLOT)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        if (br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0) != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0)) * 60 * 1001l);
    }

    public void handleFree0(DownloadLink parameter) throws Exception {
        checkErrors();
        String urlWithFilename = null;
        if (br.containsHTML("recaptcha_ajax.js")) {
            urlWithFilename = handleRecaptcha(br, parameter);
        } else {
            urlWithFilename = getUrl();
        }
        if (urlWithFilename == null) {
            logger.warning("getUrl is broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        br.getPage(urlWithFilename);
        String wait = br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
        long waittime;
        if (wait != null) {
            waittime = Long.parseLong(wait) * 1000l;
            if (waittime > 60000) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        }
        checkErrors();
        String downloadUrl = getUrl();
        if (downloadUrl == null) {
            logger.warning("getUrl is broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        wait = br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
        waittime = 60 * 1000l;
        if (wait != null) waittime = Long.parseLong(wait) * 1000l;
        if (waittime > 60000l) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        waittime += 1000;
        sleep(waittime, parameter);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, downloadUrl);
        // Prüft ob content disposition header da sind
        if (dl.getConnection().isContentDisposition()) {
            dl.startDownload();
        } else {
            br.followConnection();
            if (br.containsHTML("have exceeded the download limit")) {
                waittime = 10 * 60 * 1000l;
                try {
                    waittime = Long.parseLong(br.getRegex("Please wait (\\d+) minutes to download more files").getMatch(0)) * 1000l;
                } catch (Exception e) {
                }
                if (waittime > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
            }
            if (br.containsHTML("You are currently downloading too many files at once")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            checkErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public String handleRecaptcha(Browser br, DownloadLink link) throws Exception {
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        String id = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
        rc.setId(id);
        Form form = new Form();
        form.setAction("/file/checkCaptcha.php");
        String check = br.getRegex("check:'(.*?)'").getMatch(0);
        form.put("check", check);
        rc.setForm(form);
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, link);
        rc.setCode(c);
        if (!br.containsHTML("status:\"ok")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String url = br.getRegex("path:\"(.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (url.startsWith("http")) return url;
        return "http://www.filefactory.com" + url;
    }

    public String getUrl() throws IOException, PluginException {
        final Context cx = ContextFactory.getGlobal().enter();
        final Scriptable scope = cx.initStandardObjects();
        String[] eval = br.getRegex("var (.*?) = (.*?), (.*?) = (.*?)+\"(.*?)\", (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?);").getRow(0);
        if (eval != null) {
            // first load js
            Object result = cx.evaluateString(scope, "function g(){return " + eval[1] + "} g();", "<cmd>", 1, null);
            String link = "/file" + result + eval[4];
            br.getPage("http://www.filefactory.com" + link);

        } else {
            /* try get link from page */
            String url = br.getRegex("downloadLink.*?style.*?href=\"(http.*?)\"").getMatch(0);
            if (url != null) return url;
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String[] row = br.getRegex("var (.*?) = '';(.*;) (.*?)=(.*?)\\(\\);").getRow(0);
        Object result = cx.evaluateString(scope, row[1] + row[3] + " ();", "<cmd>", 1, null);
        if (result.toString().startsWith("http")) return result + "";
        return "http://www.filefactory.com" + result;

    }

    @Override
    public int getMaxRetries() {
        return 20;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://filefactory.com");
        Form login = br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        if (br.containsHTML(LOGIN_ERROR)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.filefactory.com/member/");
        String expire = br.getMatch("Your account is valid until the <strong>(.*?)</strong>");
        if (expire == null) {
            account.setValid(false);
            return ai;
        }
        expire = expire.replaceFirst("([^\\d].*?) ", " ");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM, yyyy", Locale.UK));
        String loaded = br.getRegex("You have downloaded(.*?)out").getMatch(0);
        String max = br.getRegex("limit of(.*?\\. )").getMatch(0);
        if (max != null && loaded != null) {
            ai.setTrafficMax(Regex.getSize(max));
            ai.setTrafficLeft(ai.getTrafficMax() - Regex.getSize(loaded));
        } else {
            max = br.getRegex("You can now download up to(.*?)in").getMatch(0);
            ai.setTrafficMax(Regex.getSize(max));
        }
        br.getPage("http://www.filefactory.com/reward/summary.php");
        String points = br.getMatch("Available reward points.*?class=\"amount\">(.*?) points");
        if (points != null) {
            /* not always enough info available to calculate points */
            ai.setPremiumPoints(Long.parseLong(points.replaceAll("\\,", "").trim()));
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML(NOT_AVAILABLE)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(SERVER_DOWN)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
            } else {
                String red = br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                logger.finer("Indirect download");
                br.setFollowRedirects(true);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, red, true, 0);
            }
        } else {
            logger.finer("DIRECT download");
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll(".com//", ".com/"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            StringBuilder sb = new StringBuilder();
            br.setCookiesExclusive(true);
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                br.getPage("http://filefactory.com/tool/links.php");
                links.clear();
                while (true) {
                    if (index == urls.length || links.size() > 25) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("func=links&links=");
                for (DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("%0D%0A");
                }
                br.postPage("http://filefactory.com/tool/links.php", sb.toString());
                for (DownloadLink dl : links) {
                    String size = br.getRegex("div class=\"metadata\".*?" + dl.getDownloadURL() + ".*?</div>.*?</td>.*?<td>(.*?)</td>").getMatch(0);
                    String name = br.getRegex("<a href=.*?" + dl.getDownloadURL() + ".*?\">(.*?)<").getMatch(0);
                    if (name != null && size != null) {
                        dl.setName(name.trim());
                        dl.setDownloadSize(Regex.getSize(size.trim()));
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generateFF());
        br.setFollowRedirects(true);
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            try {
                br.getPage(downloadLink.getDownloadURL());
                break;
            } catch (Exception e) {
                if (i == 3) throw e;
            }
        }
        if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML("there are currently no free download slots")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            if (br.containsHTML("there are currently no free download slots")) {
                downloadLink.getLinkStatus().setErrorMessage(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
            } else {
                if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                String fileName = br.getRegex("<title>(.*?) - download now for free").getMatch(0);
                String fileSize = br.getRegex(FILESIZE).getMatch(0);
                if (fileName == null || fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setName(fileName.trim());
                downloadLink.setDownloadSize(Regex.getSize(fileSize));
            }

        }
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    @Override
    public void init() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
