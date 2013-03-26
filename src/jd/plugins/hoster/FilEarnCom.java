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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filearn.com" }, urls = { "http://(www\\.)?filearn\\.com/files/get/[A-Za-z0-9_\\-]+" }, flags = { 2 })
public class FilEarnCom extends PluginForHost {

    private static final String TOOMANYSIMLUTANDOWNLOADS = ">Only premium users can download more than one file at a time";

    private static final String MAINPAGE                 = "http://filearn.com/";

    private static Object       LOCK                     = new Object();

    public FilEarnCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filearn.com/user/register");
    }

    private String execJS(String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String returnVar = new Regex(fun, "return ([A-Za-z0-9]+);").getMatch(0);
        if (returnVar == null) return null;
        fun = "var iioo = false;" + fun.replace("return " + returnVar + ";", "var lol = " + returnVar + ";");
        try {
            result = engine.eval(fun);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return result.toString();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        Regex damnExpireStuff = br.getRegex("<p>Premuim account will expire on <b>([A-Za-z]+ \\d+)[a-z]{0,5} (\\d{4}), at (\\d+:\\d+) CET</b>");
        String monthAndDay = damnExpireStuff.getMatch(0);
        String year = damnExpireStuff.getMatch(1);
        String time = damnExpireStuff.getMatch(2);
        if (monthAndDay == null && year == null && time == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(monthAndDay + " " + year + " " + time, "MMMM dd yyyy hh:mm", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    private String getAction() throws Exception {
        String jsCrap = br.getRegex("</span></code>[\t\n\r ]+<div>[\t\n\r ]+<script language=\"javascript\">[\t\n\r ]+function [A-Za-z0-9]+\\(iioo\\) \\{(.*?return .*?;)").getMatch(0);
        String action = br.getRegex("\"(http://(www\\.)?filearn\\.com/files/gen/.*?)\"").getMatch(0);
        if (jsCrap == null || action == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String actionPart = execJS(jsCrap);
        if (actionPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return action + "/" + actionPart;
    }

    @Override
    public String getAGBLink() {
        return "http://www.filearn.com/legal/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Browser br2 = br.cloneBrowser();
        String dllink = downloadLink.getStringProperty("dllink");
        try {
            if (dllink != null) {
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html")) {
                    downloadLink.setProperty("dllink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            }
        } catch (Exception e) {
            dllink = null;
        }
        if (dllink == null) {
            if (br.containsHTML(">Only premium users can download more than 150 MB in a 3 hour interval\\.<")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            if (br.containsHTML(TOOMANYSIMLUTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            String action = getAction();
            if (action == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            Form dlForm = new Form();
            dlForm.setAction(action);
            dlForm.put("pass", "");
            dlForm.put("waited", "1");
            dlForm.setMethod(MethodType.POST);
            rc.setForm(dlForm);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            // Waittime can be skipped atm.
            // long timeBefore = System.currentTimeMillis();
            String c = getCaptchaCode(cf, downloadLink);
            // int wait = 60;
            // String waittime =
            // br.getRegex("id=\"waittime\">(\\d+)</span>").getMatch(0);
            // if (waittime != null) wait = Integer.parseInt(waittime);
            // int passedTime = (int) ((System.currentTimeMillis() - timeBefore)
            // / 1000) - 1;
            // wait -= passedTime;
            // sleep(wait * 1000, downloadLink);
            rc.setCode(c);
            if (br.containsHTML(TOOMANYSIMLUTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|>The Captcha you submited was incorrect)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // More chunks are possible but i think they cause many server errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(TOOMANYSIMLUTANDOWNLOADS)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            if (br.containsHTML("(>Download link does not exist|>An Error Was Encountered<)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server problems", 120 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("dllink", dllink);
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        /** Don't use the saved cookies, maybe they cause errors */
        login(account, true);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Downloading disabled! We have detected multiple IP addresses accessing this account")) {
            logger.info("Account blocked, disabling it!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String action = getAction();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, action, "pass=&waited=1", true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML(">Incorrect or expired download url<")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error (dl link expired, too many simultan downloads)", 30 * 60 * 1000l);
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

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
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
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(false);
            br.postPage("http://www.filearn.com/user/login", "submit1=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            br.getPage("http://www.filearn.com/user/homepage");
            if (!br.containsHTML("<b>Account Type</b>: Premium</p>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(File Link Error<|Your file could not be found\\. Please check the download link\\.<|<title>File: Not Found \\- NoelShare</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span id=\"name\">[\t\n\r ]+<nobr>(.*?)</nobr>").getMatch(0);
        String filesize = br.getRegex("<span id=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        // Check if decrypter also gave us a filename
        String videarnName = link.getStringProperty("videarnname");
        if (videarnName != null) {
            String ext = filename.substring(filename.lastIndexOf("."));
            if (ext == null) ext = "";
            filename = videarnName + ext;
        }
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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