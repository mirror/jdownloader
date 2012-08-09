//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hippohosted.com" }, urls = { "http://(www\\.)?hippohosted\\.com/\\?d=[A-Z0-9]+" }, flags = { 0 })
public class HippoHostedCom extends PluginForHost {

    public HippoHostedCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/register.php?g=3");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST     = "http://hippohosted.com";
    private static final int    DEFAULTWAITTIME = 0;

    // MhfScriptBasic 1.8
    // FREE limits: 15 * 1
    // PREMIUM limits: Chunks * Maxdl
    // Captchatype: none
    // Other notes:

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getURL().contains("&code=DL_FileNotFound") || br.containsHTML("(Your requested file is not found|No file found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2 class=\"float\\-left\">([^<>\"]*?)</h2>").getMatch(0);
        String filesize = getData("File size");
        if (filename == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(downloadLink);
        if (br.containsHTML("value=\"Free Users\""))
            br.postPage(downloadLink.getDownloadURL(), "Free=Free+Users");
        else if (br.getFormbyProperty("name", "entryform1") != null) br.submitForm(br.getFormbyProperty("name", "entryform1"));
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");

        final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID != null) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            ajaxBR.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1&recaptcha_response_field=" + c + "&recaptcha_challenge_field=" + rc.getChallenge());
            if (ajaxBR.containsHTML("incorrect\\-captcha\\-sol")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            ajaxBR.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1");
        }
        final String reconnectWaittime = ajaxBR.getRegex("You must wait (\\d+) mins\\. for next download.").getMatch(0);
        if (reconnectWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 60 * 1001l);
        final String finalLink = findLink(ajaxBR);
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = DEFAULTWAITTIME;
        final String waittime = br.getRegex("countdown\\((\\d+)\\);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalLink, true, -15);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();

            if (br.containsHTML(">AccessKey is expired, please request")) throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL server error, waittime skipped?");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String findLink(final Browser br) throws Exception {
        return br.getRegex("(http://[a-z0-9\\-\\.]{5,30}/getfile\\.php\\?id=\\d+[^<>\"\\']*?)(\"|\\')").getMatch(0);
    }

    private String getData(final String data) {
        return br.getRegex(">" + data + "</strong></li>[\t\n\r ]+<li class=\"col\\-w50\">([^<>\"]*?)</li>").getMatch(0);
    }

    private static final Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    public void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
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
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
                br.setCookie(COOKIE_HOST, "yab_mylang", "en");
                br.getPage(COOKIE_HOST + "/login.php");
                Form form = br.getFormbyProperty("name", "lOGIN");
                if (form == null) form = br.getForm(0);
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                form.put("user", Encoding.urlEncode(account.getUser()));
                form.put("pass", Encoding.urlEncode(account.getPass()));
                // If the referer is still in the form (and if it is a valid
                // downloadlink) the download starts directly after logging in
                // so we
                // MUST remove it!
                form.remove("refer_url");
                form.put("autologin", "0");
                br.submitForm(form);
                br.getPage(COOKIE_HOST + "/members.php");
                final String premium = br.getRegex("return overlay\\(this, \\'package_details\\',\\'width=\\d+px,height=\\d+px,center=1,resize=1,scrolling=1\\'\\)\">(Premium)</a>").getMatch(0);
                if (br.getCookie(COOKIE_HOST, "mfh_passhash") == null || "0".equals(br.getCookie(COOKIE_HOST, "mfh_uid")) || premium == null || !premium.equals("Premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}