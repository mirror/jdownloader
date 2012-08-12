//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uloz.to" }, urls = { "http://(www\\.)?(uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)/[a-zA-Z0-9]+/.+" }, flags = { 2 })
public class UlozTo extends PluginForHost {

    private static final String REPEAT_CAPTCHA      = "REPEAT_CAPTCHA";
    private static final String CAPTCHA_TEXT        = "CAPTCHA_TEXT";
    private static final String CAPTCHA_ID          = "CAPTCHA_ID";
    private static final String QUICKDOWNLOAD       = "http://(www\\.)?uloz\\.to/quickDownload/\\d+";
    private static final String PREMIUMONLYUSERTEXT = JDL.L("plugins.hoster.ulozto.premiumonly", "Only downloadable for premium users!");

    public UlozTo(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
        this.enablePremium("http://uloz.to/kredit/");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)", "uloz.to"));
    }

    @Override
    public String getAGBLink() {
        return "http://img.uloz.to/terms.pdf";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().matches(QUICKDOWNLOAD)) {
            downloadLink.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
            return AvailableStatus.TRUE;
        }
        handleDownloadUrl(downloadLink);
        // not sure if this is still needed with 2012/02/01 changes
        String continuePage = br.getRegex("<p><a href=\"(http://.*?)\">Please click here to continue</a>").getMatch(0);
        if (continuePage != null) {
            downloadLink.setUrlDownload(continuePage);
            br.getPage(downloadLink.getDownloadURL());
        }
        // Wrong links show the mainpage so here we check if we got the mainpage
        // or not
        if (br.containsHTML("(multipart/form\\-data|Chybka 404 \\- požadovaná stránka nebyla nalezena<br>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) \\| Ulož\\.to</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<a href=\"#download\" class=\"jsShowDownload\">(.*?)</a>").getMatch(0);
        final String filesize = br.getRegex("<span id=\"fileSize\">(\\d+:\\d+ \\| )?(.*?)</span>").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    private void handleDownloadUrl(DownloadLink downloadLink) throws IOException {
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            logger.info("Getting redirect-page");
            br.getPage(br.getRedirectLocation());
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(QUICKDOWNLOAD)) throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        String dllink = null;
        Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            String captchaUrl = br.getRegex(Pattern.compile("\"(http://img\\.uloz\\.to/captcha/\\d+\\.png)\"")).getMatch(0);
            Form captchaForm = br.getFormbyProperty("id", "frm-downloadDialog-freeDownloadForm");
            final String captchaKey = br.getRegex("id=\"captcha_key\" name=\"captcha_key\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (captchaForm == null || captchaUrl == null || captchaKey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            String key = null, code = null, ts = null, sign = null, cid = null;
            // Tries to read if property selected
            if (getPluginConfig().getBooleanProperty(REPEAT_CAPTCHA)) {
                key = getPluginConfig().getStringProperty(CAPTCHA_ID);
                code = getPluginConfig().getStringProperty(CAPTCHA_TEXT);
                ts = getPluginConfig().getStringProperty("ts");
                sign = getPluginConfig().getStringProperty("cid");
                cid = getPluginConfig().getStringProperty("sign");
            }

            // If property not selected or read failed (no data), asks to solve
            if (key == null || code == null) {
                code = getCaptchaCode(captchaUrl, downloadLink);
                final Matcher m = Pattern.compile("http://img\\.uloz\\.to/captcha/(\\d+)\\.png").matcher(captchaUrl);
                if (m.find()) {
                    key = m.group(1);
                    getPluginConfig().setProperty(CAPTCHA_ID, key);
                    getPluginConfig().setProperty(CAPTCHA_TEXT, code);
                    getPluginConfig().setProperty("ts", new Regex(captchaForm.getHtmlCode(), "name=\"ts\" id=\"frmfreeDownloadForm\\-ts\" value=\"([^<>\"]*?)\"").getMatch(0));
                    getPluginConfig().setProperty("cid", new Regex(captchaForm.getHtmlCode(), "name=\"cid\" id=\"frmfreeDownloadForm\\-cid\" value=\"([^<>\"]*?)\"").getMatch(0));
                    getPluginConfig().setProperty("sign", new Regex(captchaForm.getHtmlCode(), "name=\"sign\" id=\"frmfreeDownloadForm\\-sign\" value=\"([^<>\"]*?)\"").getMatch(0));
                    getPluginConfig().setProperty(REPEAT_CAPTCHA, true);
                }
            }

            // if something failed
            if (key == null || code == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            captchaForm.put("captcha_id", key);
            captchaForm.put("captcha_value", code);
            captchaForm.put("captcha_key", captchaKey);
            captchaForm.remove(null);
            if (ts != null) captchaForm.put("ts", ts);
            if (cid != null) captchaForm.put("cid", cid);
            if (sign != null) captchaForm.put("sign", sign);
            br.submitForm(captchaForm);

            // If captcha fails, throws exception
            // If in automatic mode, clears saved data
            if (br.containsHTML("Text je opsán špatně")) {
                if (getPluginConfig().getBooleanProperty(REPEAT_CAPTCHA)) {
                    getPluginConfig().setProperty(CAPTCHA_ID, Property.NULL);
                    getPluginConfig().setProperty(CAPTCHA_TEXT, Property.NULL);
                    getPluginConfig().setProperty(REPEAT_CAPTCHA, false);
                    getPluginConfig().setProperty("ts", Property.NULL);
                    getPluginConfig().setProperty("cid", Property.NULL);
                    getPluginConfig().setProperty("sign", Property.NULL);
                }
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }

            dllink = br.getRedirectLocation();
            if (dllink == null) break;
            URLConnectionAdapter con = null;
            try {
                br2.setDebug(true);
                con = br2.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    failed = false;
                    break;
                } else {
                    br2.followConnection();
                    if (br2.containsHTML("Stránka nenalezena")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    br.clearCookies("http://www.uloz.to/");
                    handleDownloadUrl(downloadLink);
                    continue;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }

        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.contains("/error404/?fid=file_not_found")) {
            logger.info("The user entered the correct captcha but this file is offline...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) {
                logger.info("503 server error found...");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            }
            logger.warning("The finallink doesn't seem to be a file: " + dllink);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        String dllink = parameter.getDownloadURL() + "?do=directDownload";
        if (parameter.getDownloadURL().matches(QUICKDOWNLOAD)) dllink = parameter.getDownloadURL();
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage("http://uloz.to/?do=web-login");
        final String key = new Regex(br.getURL(), "uloz\\.to/login\\?key=([a-z0-9]+)").getMatch(0);
        if (key == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.postPage("http://uloz.to/login?key=" + key + "&do=loginForm-submit", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&login=P%C5%99ihl%C3%A1sit");
        if (br.getCookie("http://uloz.to/", "permanentLogin") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String trafficleft = br.getRegex("href=\"/kredit/\" title=\"([0-9\\.]+ GB)").getMatch(0);
        if (trafficleft != null) ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), UlozTo.REPEAT_CAPTCHA, JDL.L("plugins.hoster.uloz.to.captchas", "Solve captcha by replaying previous (disable to solve manually)")).setDefaultValue(true));
    }
}