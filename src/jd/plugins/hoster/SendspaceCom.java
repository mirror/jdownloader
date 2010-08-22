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

import java.io.IOException;
import java.util.regex.Pattern;

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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sendspace.com" }, urls = { "http://[\\w\\.]*?sendspace\\.com/file/[0-9a-zA-Z]+" }, flags = { 2 })
public class SendspaceCom extends PluginForHost {

    public SendspaceCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.sendspace.com/joinpro_pay.html");

        setStartIntervall(5000l);
    }

    @Override
    public void init() {
        br.setRequestIntervalLimit(getHost(), 750);
    }

    // TODO: Add handling for password protected files for handle premium,
    // actually it only works for handle free
    @Override
    public String getAGBLink() {
        return "http://www.sendspace.com/terms.html";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://www.sendspace.com/login.html");
        br.postPage("http://www.sendspace.com/login.html", "action=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=1&submit=login&openid_url=&action_type=login");
        if (br.getCookie("http://www.sendspace.com", "ssal") == null || br.getCookie("http://www.sendspace.com", "ssal").equalsIgnoreCase("deleted")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!isPremium()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.sendspace.com/mysendspace/myindex.html?l=1");
        if (br.containsHTML("Your membership is valid for")) return true;
        return false;
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
        String left = br.getRegex("You have downloaded (.*?) today").getMatch(0);
        if (left != null) {
            ai.setTrafficLeft(8l * 1024l * 1024l * 1024l - Regex.getSize(left));
        }
        String days = br.getRegex("Your membership is valid for[ ]+(\\d+)[ ]+days").getMatch(0);
        if (days != null && !days.equals("0")) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(days) * 24 * 50 * 50 * 1000));
        } else {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String linkurl = br.getRegex("<a id=\"downlink\" class=\"mango\" href=\"(.*?)\"").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, linkurl, true, 1);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.containsHTML("User Verification") && br.containsHTML("Please type all the characters") || br.containsHTML("No htmlCode read")) { return AvailableStatus.UNCHECKABLE; }
        if (!br.containsHTML("the file you requested is not available")) {
            String[] infos = br.getRegex("<b>Name:</b>(.*?)<br><b>Size:</b>(.*?)<br>").getRow(0);
            if (infos != null) {
                downloadLink.setName(Encoding.htmlDecode(infos[0]).trim());
                downloadLink.setDownloadSize(Regex.getSize(infos[1].trim().replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        // Do we have to submit a form to enter the "free download area" for the
        // file ?
        if (br.getForm(1) != null) br.submitForm(br.getForm(1));
        if (br.containsHTML("You have reached your daily download limit")) {
            int minutes = 0, hours = 0;
            String tmphrs = br.getRegex("again in.*?(\\d+)h:.*?m or").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("again in.*?h:(\\d+)m or").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            int waittime = ((3600 * hours) + (60 * minutes) + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        // Password protected links handling
        String passCode = null;
        if (br.containsHTML("name=\"filepassword\"")) {
            logger.info("This link seems to be püassword protected...");
            for (int i = 0; i < 2; i++) {
                Form pwform = br.getFormbyKey("filepassword");
                if (pwform == null) pwform = br.getForm(0);
                if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                pwform.put("filepassword", passCode);
                br.submitForm(pwform);
                if (br.containsHTML("(name=\"filepassword\"|Incorrect Password)")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("(name=\"filepassword\"|Incorrect Password)")) throw new PluginException(LinkStatus.ERROR_FATAL, "Wrong Password");
        }
        /* bypass captcha with retry ;) */
        if (br.containsHTML("User Verification") && br.containsHTML("Please type all the characters") || br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 1 * 60 * 1000l);
        handleErrors(false);
        /* Link holen */
        String script = br.getRegex(Pattern.compile("<script type=\"text/javascript\">(function .*?)</script>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (script == null) {
            logger.info("script equals null");
            br.postPage(downloadLink.getDownloadURL(), "download=%C2%A0REGULAR+DOWNLOAD%C2%A0");
            script = br.getRegex(Pattern.compile("<script type=\"text/javascript\">(function .*?)</script>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (script == null) {
                logger.warning("script still equals null, stopping...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String dec = br.getRegex(Pattern.compile("base64ToText\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        script += new Browser().getPage("http://www.sendspace.com/jsc/download.js");
        String fun = "function f(){ " + script + " return  utf8_decode(enc(base64ToText('" + dec + "')));} f()";
        Context cx = ContextFactory.getGlobal().enter();
        Scriptable scope = cx.initStandardObjects();
        // Now evaluate the string we've colected.
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        // Convert the result to a string and print it.
        String linkurl = Context.toString(result);
        if (linkurl == null) {
            logger.warning("linkurl equals null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        linkurl = new Regex(linkurl, "href=\"(.*?)\"").getMatch(0);
        if (linkurl == null) {
            logger.warning("linkurl2 equals null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getURL().toExternalForm().contains("?e=") || con.getContentType().contains("html")) {
            br.followConnection();
            handleErrors(true);
        }
        if (con.getResponseCode() == 416) {
            // HTTP/1.1 416 Requested Range Not Satisfiable
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private void handleErrors(boolean plugindefect) throws PluginException {
        String error = br.getRegex("<div class=\"errorbox-bad\".*?>(.*?)</div>").getMatch(0);
        if (error == null) error = br.getRegex("<div class=\"errorbox-bad\".*?>.*?>(.*?)</>").getMatch(0);
        if (error == null && !plugindefect) return;
        if (error == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sendspacecom.errors.servererror", "Unknown server error"), 5 * 60 * 1000l);
        if (error.contains("You may now download the file")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 30 * 1000l); }
        if (error.contains("full capacity")) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.sendspacecom.errors.serverfull", "Free service capacity full"), 5 * 60 * 1000l); }
        if (error.contains("this connection has reached the")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000);
        if (error.contains("reached daily download")) {
            int wait = 60;
            String untilh = br.getRegex("again in (\\d+)h:(\\d+)m").getMatch(0);
            String untilm = br.getRegex("again in (\\d+)h:(\\d+)m").getMatch(1);
            if (untilh != null) wait = Integer.parseInt(untilh) * 60;
            if (untilm != null && untilh != null) wait = wait + Integer.parseInt(untilm);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your daily download limit", wait * 60 * 1000l);
        }
        if (plugindefect) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
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
