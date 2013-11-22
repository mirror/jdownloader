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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "easy-share.com", "crocko.com" }, urls = { "sgru3465979hg354uigUNUSED_REGEX879t24uj", "http://(www\\.)?(easy\\-share|crocko)\\.com/(?!us|en|pt|accounts|billing|f/|mc)([A-Z0-9]+/?|\\d+)" }, flags = { 0, 2 })
public class CrockoCom extends PluginForHost {

    private static AtomicBoolean longwait     = new AtomicBoolean(false);
    private static final String  MAINPAGE     = "http://www.crocko.com/";
    private static final String  FILENOTFOUND = "Requested file is deleted";
    private static final String  ONLY4PREMIUM = ">You need Premium membership to download this file";

    public CrockoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.crocko.com/billing");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("easy-share.com/", "crocko.com/"));
    }

    // TODO: Implement API if possible: http://www.crocko.com/de/developers.html

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.crocko.com/accounts");
        if (br.containsHTML(">expired")) {
            ai.setExpired(true);
            return ai;
        }
        String isPremium = br.getRegex("Premium membership: <.*?>(Active)<").getMatch(0);
        String ends = br.getRegex("Ends:</span>.*?<span>(.*?)<").getMatch(0);
        /* there are 2 different versions of account info pages */
        if (ends == null) ends = br.getRegex("End time:(.*?)<").getMatch(0);
        if (ends == null) ends = br.getRegex("Starts:.*?Ends: (.*?)<").getMatch(0);
        if (ends == null) ends = br.getRegex("Duration:(.*?)<").getMatch(0);
        if (isPremium == null) isPremium = br.getRegex("Premium account: <.*?>(active)<").getMatch(0);
        if (isPremium == null) isPremium = br.getRegex("Premium: <.*?>(active)<").getMatch(0);
        if (ends != null) ends = ends.trim();
        if (ends == null || isPremium == null) {
            account.setValid(false);
            return ai;
        }
        if ("unlimited".equalsIgnoreCase(ends)) {
            ai.setValidUntil(-1);
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(ends.replaceAll(", in", "").trim(), "dd MMM yyyy HH:mm:ss", null));
        }
        String trafficLeft = br.getRegex("Traffic left:(.*?)<").getMatch(0);
        if (trafficLeft != null) {
            /* it seems they have unlimited traffic */
            // ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
            ai.setUnlimitedTraffic();
        } else {
            ai.setUnlimitedTraffic();
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.crocko.com/de/privacy.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (br.containsHTML(FILENOTFOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("There is another download in progress from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        if (br.containsHTML(ONLY4PREMIUM)) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.host.errormsg.only4premium", "Only downloadable for premium users!"));
        String wait = br.getRegex("w=\\'(\\d+)\\'").getMatch(0);
        int waittime = 0;
        if (wait != null) waittime = Integer.parseInt(wait.trim());
        if (waittime > 180 && longwait.get()) {
            /* first time >90 secs, it can be we are country with long waittime */
            longwait.set(true);
            sleep(waittime * 1000l, downloadLink);
        } else {
            if (longwait == null) longwait.set(false);
            if (waittime > 90 && longwait.get() == false) {
                /*
                 * only request reconnect if we dont have to wait long on every download
                 */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 1000l);
            } else {
                if (br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0) == null) {
                    sleep(waittime * 1000l, downloadLink);
                }
            }
        }

        String id = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
        if (br.containsHTML("Please wait or buy a Premium membership")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);

        if (id == null) br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("There is another download in progress from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        // br = br;
        int tries = 0;
        while (true) {
            tries++;
            id = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
            if (id == null) {
                logger.warning("crocko.com: plugin broken, reCaptchaID is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser rcBr = br.cloneBrowser();
            /* follow redirect needed as google redirects to another domain */
            rcBr.setFollowRedirects(true);
            rcBr.getPage("http://api.recaptcha.net/challenge?k=" + id);
            String challenge = rcBr.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
            String server = rcBr.getRegex("server.*?:.*?'(.*?)',").getMatch(0);
            if (challenge == null || server == null) {
                logger.severe("crocko.com: Recaptcha Module fails: " + br.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String captchaAddress = server + "image?c=" + challenge;
            File cf = getLocalCaptchaFile();
            Browser.download(cf, rcBr.openGetConnection(captchaAddress));
            Form form = null;
            Form[] allForms = br.getForms();
            if (allForms == null || allForms.length == 0) {
                logger.warning("crocko.com: plugin broken, no download forms found");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (Form singleForm : allForms) {
                if (singleForm.containsHTML("\"id\"") && !singleForm.containsHTML("lang_select")) {
                    form = singleForm;
                    break;
                }
            }
            if (form == null) {
                logger.warning("crocko.com: plugin broken, no download forms found #2");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /*
             * another as default cause current stable has easy-captcha method that does not work
             */
            String code = getCaptchaCode("recaptcha", cf, downloadLink);
            form.put("recaptcha_challenge_field", challenge);
            form.put("recaptcha_response_field", Encoding.urlEncode(code));
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 1);
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                if (br.containsHTML("There are no more download slots available right now.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "There are no more download slots available right now.", 10 * 60 * 1000l);
                if (br.containsHTML("There is another download in progress from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
                if (br.containsHTML("Entered code is invalid")) {
                    if (tries <= 5) {
                        continue;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                logger.warning("crocko.com: plugin broken, direct link -> HTML code");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            break;
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(FILENOTFOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String url = br.getRedirectLocation();
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* limited easyshare to max 5 chunks cause too much can create issues */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, -5);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "language", "en");
        br.getPage(MAINPAGE);
        br.setDebug(true);
        br.postPage("http://www.crocko.com/accounts/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=1");
        String acc = br.getCookie(MAINPAGE, "ACCOUNT");
        String prem = br.getCookie(MAINPAGE, "PREMIUM");
        if (acc == null && prem == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (acc != null && prem == null) {
            /*
             * buggy easyshare server, login does not work always, it needs PREMIUM cookie
             */
            br.setCookie(MAINPAGE, "PREMIUM", acc);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        correctDownloadLink(downloadLink);
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)/?$").getMatch(0));
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        URLConnectionAdapter con = null;
        try {
            br.setCookie(MAINPAGE, "language", "en");
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        // Offline links
        if (br.containsHTML("<title>Crocko\\.com 404</title>|>Requested file is deleted|>Searching for file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Invalid links
        if (br.containsHTML(">you have no permission")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Download: +<strong>(.*?)</strong>").getMatch(0);
        if (filename == null) filename = br.getRegex(">Download:</span> <br />[\t\n\r ]+<strong>(.*?)</strong>").getMatch(0);
        final String filesize = br.getRegex("<span class=\"tip1\"><span class=\"inner\">(.*?)</span></span>").getMatch(0);
        if (filename == null || filesize == null) {
            if (br.containsHTML("<h1>Software error:</h1>")) {
                return AvailableStatus.UNCHECKABLE;
            } else if (br.containsHTML("(<div class=\"search_result\">|<form method=\"POST\" action=\"/search_form\">)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                logger.warning("crocko.com: plugin broken, filename or filesize missing");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        filename = filename.replaceAll("<br>", "");
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim() + "b"));
        return AvailableStatus.TRUE;
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}