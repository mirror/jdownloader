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

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "http://(www\\.)?filer\\.net/(get|dl)/[a-z0-9]+" }, flags = { 2 })
public class FilerNet extends PluginForHost {

    private static Object       LOCK                            = new Object();
    private int                 STATUSCODE                      = 0;
    private static final int    APIDISABLED                     = 400;
    private static final String APIDISABLEDTEXT                 = "API is disabled, please wait or use filer.net from your browser";
    private static final int    DOWNLOADTEMPORARILYDISABLED     = 500;
    private static final String DOWNLOADTEMPORARILYDISABLEDTEXT = "Download temporarily disabled!";
    private static final int    UNKNOWNERROR                    = 599;
    private static final String UNKNOWNERRORTEXT                = "Unknown file error";

    public FilerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filer.net/upgrade");
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("://www.", "://").replace("/dl/", "/get/"));
    }

    private void prepBrowser() {
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    @Override
    public String getAGBLink() {
        return "http://filer.net/agb.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        callAPI("http://api.filer.net/api/status/" + getFID(link) + ".json");
        if (STATUSCODE == APIDISABLED) {
            link.getLinkStatus().setStatusText(APIDISABLEDTEXT);
            return AvailableStatus.UNCHECKABLE;
        } else if (STATUSCODE == 505) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (STATUSCODE == DOWNLOADTEMPORARILYDISABLED) {
            link.getLinkStatus().setStatusText(DOWNLOADTEMPORARILYDISABLEDTEXT);
        } else if (STATUSCODE == UNKNOWNERROR) {
            link.getLinkStatus().setStatusText(UNKNOWNERRORTEXT);
            return AvailableStatus.UNCHECKABLE;
        }
        link.setFinalFileName(getJson("name", br.toString()));
        link.setDownloadSize(Long.parseLong(getJson("size", br.toString())));
        link.setMD5Hash(getJson("hash", br.toString()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        handleDownloadErrors();
        callAPI("http://filer.net/get/" + getFID(downloadLink) + ".json");

        if (STATUSCODE == 501) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available, wait or buy premium!", 10 * 60 * 1000l);
        } else if (STATUSCODE == 502) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max free simultan-downloads-limit reached, please finish running downloads before starting new ones!", 5 * 60 * 1000l); }

        int wait = Integer.parseInt(getJson("wait", br.toString()));
        if (STATUSCODE == 203) {
            sleep(wait * 1001l, downloadLink);
        } else if (STATUSCODE == 503) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l); }
        callAPI("http://filer.net/get/" + getFID(downloadLink) + ".json" + "?token=" + getJson("token", br.toString()));
        String dllink = null;
        if (STATUSCODE == 202) {
            int maxCaptchaTries = 5;
            int tries = 0;
            while (tries < maxCaptchaTries) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId(getJson("recaptcha_challange", br.toString()));
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                tries++;
                br.postPage("http://filer.net/get/" + getFID(downloadLink) + ".json", "recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&hash=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)§").getMatch(0));
                dllink = br.getRedirectLocation();
                if (dllink == null) continue;
                break;
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
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

    public void login(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (LOCK) {
            /** Load cookies */
            br.setCookiesExclusive(true);
            prepBrowser();
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            try {
                callAPI("http://api.filer.net/profile.json");
                if (br.getRedirectLocation() != null) callAPI(br.getRedirectLocation());
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (getJson("state", br.toString()).equals("free")) {
            ai.setStatus("Free Account (dieser Accounttyp wird nicht unterstützt)");
            account.setValid(false);
            return ai;
        }
        ai.setTrafficLeft(Long.parseLong(getJson("traffic", br.toString())));
        ai.setValidUntil(Long.parseLong(getJson("until", br.toString())) * 1000);
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setBrowserExclusive();
        requestFileInformation(downloadLink);
        handleDownloadErrors();
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        callAPI("http://filer.net/api/dl/" + getFID(downloadLink) + ".json");
        if (STATUSCODE == 504) {
            logger.info("No traffic available!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (STATUSCODE == UNKNOWNERROR) { throw new PluginException(LinkStatus.ERROR_FATAL, UNKNOWNERRORTEXT); }
        final String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Important!!
        br.getHeaders().put("Authorization", "");
        // Chunks deactivated to prevent errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        dl.startDownload();
    }

    private void handleDownloadErrors() throws PluginException {
        if (STATUSCODE == APIDISABLED) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, APIDISABLEDTEXT, 2 * 60 * 60 * 1000l);
        } else if (STATUSCODE == DOWNLOADTEMPORARILYDISABLED) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, DOWNLOADTEMPORARILYDISABLEDTEXT, 2 * 60 * 60 * 1000l);
        } else if (STATUSCODE == UNKNOWNERROR) { throw new PluginException(LinkStatus.ERROR_FATAL, UNKNOWNERRORTEXT); }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    private void callAPI(final String url) throws IOException {
        br.getPage(url);
        updateStatuscode();
    }

    private void updateStatuscode() {
        final String code = getJson("code", br.toString());
        if (code != null) STATUSCODE = Integer.parseInt(code);
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
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