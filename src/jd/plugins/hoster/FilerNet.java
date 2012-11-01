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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "http://[\\w\\.]*?filer.net/(file[\\d]+|get|dl)/.*" }, flags = { 2 })
public class FilerNet extends PluginForHost {

    private final Pattern     PATTERN_MATCHER_ERROR = Pattern.compile("errors", Pattern.CASE_INSENSITIVE);
    private static AtomicLong LAST_FREE_DOWNLOAD    = new AtomicLong(0l);

    public FilerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://filer.net/premium");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String trafficleft = br.getRegex(Pattern.compile("<th>Traffic</th>.*?<td>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String validuntil = br.getRegex(Pattern.compile("<th>Mitglied bis</th>.*?<td>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        ai.setTrafficLeft(trafficleft);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");

        String[] splitted = validuntil.split("-");
        Date date = dateFormat.parse(splitted[2] + "." + splitted[1] + "." + splitted[0]);
        ai.setValidUntil(date.getTime());

        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.filer.net/agb.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    private static final String SLOTSFILLED = ">Slots filled<|>Download Slots max<|You have used all your available download-slots";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        final String reconWait = br.getRegex("<p>Please wait <span id=\"time\">(\\d+)</span> seconds").getMatch(0);
        if (reconWait != null) {
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText("Cannot show filename while the downloadlimit is reached");
            return AvailableStatus.TRUE;
        } else if (br.containsHTML(SLOTSFILLED)) {
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText("Cannot show filename while all slots are filled");
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(false);
        if (br.containsHTML(">Not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex(">Free Download ([^<>\"]*?)<").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(SLOTSFILLED)) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "All slots are filled", 10 * 60 * 1000l);
        final String reconWait = br.getRegex("<p>Please wait <span id=\"time\">(\\d+)</span> seconds").getMatch(0);
        int wait = 0;
        if (reconWait != null) wait = Integer.parseInt(reconWait);
        if (wait < 180) {
            sleep(wait * 1001l, downloadLink);
            br.getPage(downloadLink.getDownloadURL());
        } else {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        }
        final String token = br.getRegex("name=\"token\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(br.getURL(), "token=" + Encoding.urlEncode(token));
        br.setFollowRedirects(false);
        int maxCaptchaTries = 5;
        br.setCookiesExclusive(true);
        int tries = 0;
        while (tries < maxCaptchaTries) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            if (id == null) id = this.br.getRegex("Recaptcha\\.create\\(\"([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            tries++;
            br.postPage(br.getURL(), "recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&hash=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)§").getMatch(0));
            if (!br.containsHTML("google\\.com/recaptcha/")) {
                break;
            }
        }
        if (br.containsHTML("google\\.com/recaptcha/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        final String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        // TODO: Fix ALL premium functions
        setBrowserExclusive();
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        Thread.sleep(500);
        br.setFollowRedirects(false);
        String id = br.getRegex("<a href=\"\\/dl\\/(.*?)\">.*?<\\/a>").getMatch(0);
        if (id == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.getPage("http://www.filer.net/dl/" + id);
        String url = br.getRegex("url=(http.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
        if (dl.getConnection().getContentType().contains("text")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE); }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void login(Account account) throws IOException, PluginException, InterruptedException {
        br.getPage("http://www.filer.net/");
        Thread.sleep(500);
        br.getPage("http://www.filer.net/login");
        Thread.sleep(500);
        br.postPage("http://www.filer.net/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&commit=Einloggen");
        Thread.sleep(500);
        String cookie = br.getCookie("http://filer.net", "filer_net");
        if (cookie == null) {
            if (br.containsHTML("Mit diesem Usernamen ist bereits ein Benutzer eingelogged")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Fraud Detection. This Username is currently used by someone else.", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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