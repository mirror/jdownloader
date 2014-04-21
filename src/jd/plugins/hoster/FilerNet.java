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
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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
import jd.utils.locale.JDL;

import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "https?://(www\\.)?filer\\.net/(get|dl)/[a-z0-9]+" }, flags = { 2 })
public class FilerNet extends PluginForHost {

    private static Object       LOCK                            = new Object();
    private int                 STATUSCODE                      = 0;
    private String              fuid                            = null;
    private String              recapID                         = null;
    private String              dllink                          = null;
    private static final int    APIDISABLED                     = 400;
    private static final String APIDISABLEDTEXT                 = "API is disabled, please wait or use filer.net from your browser";
    private static final int    DOWNLOADTEMPORARILYDISABLED     = 500;
    private static final String DOWNLOADTEMPORARILYDISABLEDTEXT = "Download temporarily disabled!";
    private static final int    UNKNOWNERROR                    = 599;
    private static final String UNKNOWNERRORTEXT                = "Unknown file error";
    private static final String NORESUME                        = "NORESUME";

    public FilerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filer.net/upgrade");
    }

    protected void showFreeDialog(final String domain) {
        if (System.getProperty("org.jdownloader.revision") != null) { /* JD2 ONLY! */
            super.showFreeDialog(domain);
            return;
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        String tab = "                        ";
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download";
                            message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = domain + " Free Download";
                            message = "You are using the " + domain + " Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkShowFreeDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                if (config.getProperty("premAdShown2") == null) {
                    File checkFile = JDUtilities.getResourceFile("tmp/filer");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("filer.net");
                    }
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("premAdShown", Boolean.TRUE);
                config.setProperty("premAdShown2", "shown");
                config.save();
            }
        }
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://filer.net/get/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
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
        fuid = getFID(link);
        if (fuid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        callAPI("http://api.filer.net/api/status/" + fuid + ".json");
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
        /* hash != md5, its the hash of fileID */
        link.setMD5Hash(null);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        handleDownloadErrors();
        checkShowFreeDialog();
        callAPI("http://filer.net/get/" + fuid + ".json");

        if (STATUSCODE == 501) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available, wait or buy premium!", 10 * 60 * 1000l);
        } else if (STATUSCODE == 502) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max free simultan-downloads-limit reached, please finish running downloads before starting new ones!", 5 * 60 * 1000l); }

        final String token = getJson("token", br.toString());
        if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // 203 503 wait
        int wait = getWait();
        if (STATUSCODE == 203) {
            sleep(wait * 1001l, downloadLink);
        } else if (STATUSCODE == 503) {
            // Waittime too small->Don't reconnect
            if (wait < 61) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads...", wait * 1001l);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        }
        callAPI("http://filer.net/get/" + fuid + ".json?token=" + token);
        String dllink = null;
        if (STATUSCODE == 202) {
            int maxCaptchaTries = 5;
            int tries = 0;
            while (tries < maxCaptchaTries) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                if (recapID == null) recapID = getJson("recaptcha_challange", br.toString());
                if (recapID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                rc.setId(recapID);
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                tries++;
                br.postPage("http://filer.net/get/" + fuid + ".json", "recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&hash=" + fuid);
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    updateStatuscode();
                    if (STATUSCODE == 501) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available, wait or buy premium!", 10 * 60 * 1000l);
                    } else if (STATUSCODE == 502) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max free simultan-downloads-limit reached, please finish running downloads before starting new ones!", 5 * 60 * 1000l);
                    } else {
                        continue;
                    }
                } else {
                    break;
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        boolean resume = true;
        if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false)) resume = false;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // Temporary errorhandling for a bug which isn't handled by the API
            if (br.getURL().equals("http://filer.net/error/500")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler", 60 * 60 * 1000l);
            if (br.getURL().equals("http://filer.net/error/430") || br.containsHTML("Diese Adresse ist nicht bekannt oder")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(FilerNet.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private int getWait() {
        final String tiaw = getJson("wait", br.toString());
        if (tiaw != null)
            return Integer.parseInt(tiaw);
        else
            return 15;
    }

    // TODO: remove this when v2 becomes stable. use br.getFormbyKey(String key, String value)
    /**
     * Returns the first form that has a 'key' that equals 'value'.
     * 
     * @param key
     *            name
     * @param value
     *            expected value
     * @param ibr
     *            import browser
     * */
    private Form getFormByInput(final Browser ibr, final String key, final String value) {
        Form[] workaround = ibr.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                if (f.containsHTML(key + "=(\"|')" + value + "\\1")) return f;
            }
        }
        return null;
    }

    private Browser prepBrowser(final Browser prepBr) {
        prepBr.setFollowRedirects(false);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.73.11 (KHTML, like Gecko) Version/7.0.1 Safari/537.73.11");
        return prepBr;
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
                callAPI("http://api.filer.net/api/profile.json");
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
        // try {
        login(account);
        // } catch (PluginException e) {
        // account.setValid(false);
        // return ai;
        // }
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
        callAPI("http://filer.net/api/dl/" + fuid + ".json");
        if (STATUSCODE == 504) {
            logger.info("No traffic available!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (STATUSCODE == UNKNOWNERROR) { throw new PluginException(LinkStatus.ERROR_FATAL, UNKNOWNERRORTEXT); }
        final String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Important!!
        br.getHeaders().put("Authorization", "");
        boolean resume = true;
        if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false)) resume = false;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // Temporary errorhandling for a bug which isn't handled by the API
            if (br.getURL().equals("http://filer.net/error/500")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler", 60 * 60 * 1000l);
            if (br.getURL().equals("http://filer.net/error/430") || br.containsHTML("Diese Adresse ist nicht bekannt oder")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(FilerNet.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
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