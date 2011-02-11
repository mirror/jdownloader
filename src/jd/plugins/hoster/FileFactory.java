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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefactory.com" }, urls = { "http://[\\w\\.]*?filefactory\\.com(/|//)file/[\\w]+/?" }, flags = { 2 })
public class FileFactory extends PluginForHost {

    private static final String FILESIZE      = "<span>(.*? (B|KB|MB|GB)) file";

    private static final String NO_SLOT       = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String SLOTEXPIRED   = "<p>Your download slot has expired\\.";
    private static final String LOGIN_ERROR   = "The email or password you have entered is incorrect";
    private static final String SERVER_DOWN   = "server hosting the file you are requesting is currently down";
    private static final String CAPTCHALIMIT  = "<p>We have detected several recent attempts to bypass our free download restrictions originating from your IP Address";

    public FileFactory(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filefactory.com/info/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/legal/terms.php";
    }

    public void checkErrors() throws PluginException {
        if (br.containsHTML(CAPTCHALIMIT)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (this.br.containsHTML(FileFactory.SLOTEXPIRED)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error"); }
        if (this.br.containsHTML("there are currently no free download slots") || this.br.containsHTML("download slots on this server are")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots", 10 * 60 * 1000l); }
        if (this.br.containsHTML(FileFactory.NOT_AVAILABLE)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (this.br.containsHTML(FileFactory.SERVER_DOWN) || this.br.containsHTML(FileFactory.NO_SLOT)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }
        if (this.br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0) != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(this.br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0)) * 60 * 1001l); }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.forceDebug(true);
            final StringBuilder sb = new StringBuilder();
            br.setCookiesExclusive(true);
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                br.getPage("http://filefactory.com/tool/links.php");
                links.clear();
                while (true) {
                    if (index == urls.length || links.size() > 25) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("func=links&links=");
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("%0D%0A");
                }
                br.postPage("http://filefactory.com/tool/links.php", sb.toString());
                for (final DownloadLink dl : links) {
                    final String size = br.getRegex("div class=\"metadata\".*?" + dl.getDownloadURL() + ".*?</div>.*?</td>.*?<td>(.*?)</td>").getMatch(0);
                    final String name = br.getRegex("<a href=.*?" + dl.getDownloadURL() + ".*?\">(.*?)<").getMatch(0);
                    if (name != null && size != null) {
                        dl.setName(name.trim());
                        dl.setDownloadSize(SizeFormatter.getSize(size.trim()));
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
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
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll(".com//", ".com/"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        this.br.getPage("http://www.filefactory.com/member/");
        String expire = this.br.getMatch("Your account is valid until the <strong>(.*?)</strong>");
        if (expire == null) {
            account.setValid(false);
            return ai;
        }
        expire = expire.replaceFirst("([^\\d].*?) ", " ");
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM, yyyy", Locale.UK));
        final String loaded = this.br.getRegex("You have downloaded(.*?)out").getMatch(0);
        String max = this.br.getRegex("limit of(.*?\\. )").getMatch(0);
        if (max != null && loaded != null) {
            ai.setTrafficMax(SizeFormatter.getSize(max));
            ai.setTrafficLeft(ai.getTrafficMax() - SizeFormatter.getSize(loaded));
        } else {
            max = this.br.getRegex("You can now download up to(.*?)in").getMatch(0);
            ai.setTrafficMax(SizeFormatter.getSize(max));
        }
        this.br.getPage("http://www.filefactory.com/reward/summary.php");
        final String points = this.br.getMatch("Available reward points.*?class=\"amount\">(.*?) points");
        if (points != null) {
            /* not always enough info available to calculate points */
            ai.setPremiumPoints(Long.parseLong(points.replaceAll("\\,", "").trim()));
        }
        return ai;
    }

    @Override
    public int getMaxRetries() {
        return 20;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 200;
    }

    public String getUrl() throws IOException, PluginException {
        String url = this.br.getRegex("<div.*?id=\"downloadLink\".*?>.*?<a .*?href=\"(.*?)\".*?\"downloadLinkTarget").getMatch(0);
        if (url == null) {
            Context cx = null;
            try {
                cx = ContextFactory.getGlobal().enterContext();
                final Scriptable scope = cx.initStandardObjects();
                final String[] eval = this.br.getRegex("var (.*?) = (.*?), (.*?) = (.*?)+\"(.*?)\", (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?);").getRow(0);
                if (eval != null) {
                    // first load js
                    Object result = cx.evaluateString(scope, "function g(){return " + eval[1] + "} g();", "<cmd>", 1, null);
                    final String link = "/file" + result + eval[4];
                    this.br.getPage("http://www.filefactory.com" + link);
                    final String[] row = this.br.getRegex("var (.*?) = '';(.*;) (.*?)=(.*?)\\(\\);").getRow(0);
                    result = cx.evaluateString(scope, row[1] + row[3] + " ();", "<cmd>", 1, null);
                    if (result.toString().startsWith("http")) {
                        url = result + "";
                    } else {
                        url = "http://www.filefactory.com" + result;
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } finally {
                if (cx != null) Context.exit();
            }
        }
        return url;

    }

    @Override
    public void handleFree(final DownloadLink parameter) throws Exception {
        this.requestFileInformation(parameter);
        try {
            this.handleFree0(parameter);
        } catch (final PluginException e4) {
            throw e4;
        } catch (final InterruptedException e2) {
            return;
        } catch (final IOException e) {
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

    public void handleFree0(final DownloadLink parameter) throws Exception {
        this.checkErrors();
        String urlWithFilename = null;
        if (this.br.containsHTML("recaptcha_ajax.js")) {
            urlWithFilename = this.handleRecaptcha(this.br, parameter);
        } else {
            urlWithFilename = this.getUrl();
        }
        if (urlWithFilename == null) {
            logger.warning("getUrl is broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.setFollowRedirects(true);
        this.br.getPage(urlWithFilename);
        String wait = this.br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
        long waittime;
        if (wait != null) {
            waittime = Long.parseLong(wait) * 1000l;
            if (waittime > 60000) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime); }
        }
        this.checkErrors();
        final String downloadUrl = this.getUrl();
        if (downloadUrl == null) {
            logger.warning("getUrl is broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        wait = this.br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
        waittime = 60 * 1000l;
        if (wait != null) {
            waittime = Long.parseLong(wait) * 1000l;
        }
        if (waittime > 60000l) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime); }
        waittime += 1000;
        this.sleep(waittime, parameter);
        this.br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, parameter, downloadUrl);
        // Prüft ob content disposition header da sind
        if (this.dl.getConnection().isContentDisposition()) {
            this.dl.startDownload();
        } else {
            this.br.followConnection();
            if (this.br.containsHTML("have exceeded the download limit")) {
                waittime = 10 * 60 * 1000l;
                try {
                    waittime = Long.parseLong(this.br.getRegex("Please wait (\\d+) minutes to download more files").getMatch(0)) * 1000l;
                } catch (final Exception e) {
                }
                if (waittime > 0) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime); }
            }
            if (this.br.containsHTML("You are currently downloading too many files at once")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l); }
            this.checkErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.requestFileInformation(downloadLink);
        this.login(account);
        this.br.setFollowRedirects(false);
        this.br.getPage(downloadLink.getDownloadURL());
        this.br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, this.br.getRedirectLocation(), true, 0);
        if (!this.dl.getConnection().isContentDisposition()) {
            this.br.followConnection();
            if (this.br.containsHTML(FileFactory.NOT_AVAILABLE)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.containsHTML(FileFactory.SERVER_DOWN)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
            } else {
                final String red = this.br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                logger.finer("Indirect download");
                this.br.setFollowRedirects(true);
                this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, red, true, 0);
            }
        } else {
            logger.finer("DIRECT download");
        }
        this.dl.startDownload();
    }

    public String handleRecaptcha(final Browser br, final DownloadLink link) throws Exception {
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        final String id = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
        rc.setId(id);
        final Form form = new Form();
        form.setAction("/file/checkCaptcha.php");
        final String check = br.getRegex("check:'(.*?)'").getMatch(0);
        form.put("check", check);
        rc.setForm(form);
        rc.load();
        final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
        final String c = this.getCaptchaCode(cf, link);
        rc.setCode(c);
        if (br.containsHTML(CAPTCHALIMIT)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (!br.containsHTML("status:\"ok")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        final String url = br.getRegex("path:\"(.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (url.startsWith("http")) { return url; }
        return "http://www.filefactory.com" + url;
    }

    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(true);
        this.br.getPage("http://filefactory.com");
        final Form login = this.br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        this.br.submitForm(login);
        if (this.br.containsHTML(FileFactory.LOGIN_ERROR)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        this.br.getHeaders().put("User-Agent", RandomUserAgent.generateFF());
        this.br.setFollowRedirects(true);
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(200);
            } catch (final Exception e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            try {
                this.br.getPage(downloadLink.getDownloadURL());
                break;
            } catch (final Exception e) {
                if (i == 3) { throw e; }
            }
        }
        if (this.br.containsHTML(FileFactory.NOT_AVAILABLE) && !this.br.containsHTML("there are currently no free download slots")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML(FileFactory.SERVER_DOWN)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            if (this.br.containsHTML("there are currently no free download slots") || this.br.containsHTML("download slots on this server are")) {
                downloadLink.getLinkStatus().setErrorMessage(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", "No slots free available"));
            } else {
                if (this.br.containsHTML("File Not Found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                final String fileName = this.br.getRegex("<title>(.*?) - download now for free").getMatch(0);
                final String fileSize = this.br.getRegex(FileFactory.FILESIZE).getMatch(0);
                if (fileName == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                downloadLink.setName(fileName.trim());
                if (fileSize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
            }

        }
        this.br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}
