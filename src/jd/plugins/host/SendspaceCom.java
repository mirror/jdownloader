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
package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class SendspaceCom extends PluginForHost {

    public SendspaceCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.sendspace.com/joinpro_pay.html");
        setStartIntervall(5000l);
    }

    //@Override
    public String getAGBLink() {
        return "http://www.sendspace.com/terms.html";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://www.sendspace.com/login.html");
        br.postPage("http://www.sendspace.com/login.html", "action=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=1&submit=login&openid_url=&action_type=login");
        if (br.getCookie("http://www.sendspace.com", "ssal") == null || br.getCookie("http://www.sendspace.com", "ssal").equalsIgnoreCase("deleted")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        if (!isPremium()) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.sendspace.com/mysendspace/myindex.html?l=1");
        if (br.containsHTML("Your membership is valid for")) return true;
        return false;
    }

    //@Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        String left = br.getRegex("You have downloaded (.*?)GB today").getMatch(0);
        if (left != null) {
            int tleft = 800 - Integer.parseInt(left.replaceAll("\\.", ""));
            ai.setTrafficLeft((long) (1024l * 1024l * 1024 * (tleft / 100.0)));
        }
        String days = br.getRegex("Your membership is valid for (\\d+) days").getMatch(0);
        if (days != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(days) * 24 * 50 * 50 * 1000));
        } else if (days == null || days.equals("0")) {
            ai.setExpired(true);
            ai.setValid(false);
            return ai;
        }
        String points = br.getRegex(Pattern.compile("You have([\\d+, ]+)<a href=\"maxpoints", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Long.parseLong(points.trim().replaceAll(",|\\.", "")));
        ai.setValid(true);
        return ai;
    }

    //@Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String linkurl = br.getRegex("<a id=\"downlink\" class=\"mango\" href=\"(.*?)\"").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = br.openDownload(link, linkurl, true, 0);
        dl.startDownload();
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        br.getPage(url);
        if (!br.containsHTML("the file you requested is not available")) {
            downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("<b>Name:</b> (.*?)[ \t]+<br><b>", Pattern.CASE_INSENSITIVE)).getMatch(0));
            downloadSize = (br.getRegex(Pattern.compile("<b>Size:</b> (.*?)[ \t]+<br>")).getMatch(0));
            if (!(downloadName == null || downloadSize == null)) {
                downloadLink.setName(downloadName.trim());
                downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        /* Link holen */
        String script = br.getRegex(Pattern.compile("<script type=\"text/javascript\">(.*?)</script>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String dec = br.getRegex(Pattern.compile("base64ToText\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        script += new Browser().getPage("http://www.sendspace.com/jsc/download.js");
        String fun = "function f(){ " + script + " return  utf8_decode(enc(base64ToText('" + dec + "')));} f()";
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        // Now evaluate the string we've colected.
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        // Convert the result to a string and print it.
        String linkurl = Context.toString(result);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        linkurl = new Regex(linkurl, "href=\"(.*?)\"").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getURL().toExternalForm().contains("?e=") || con.getContentType().contains("html")) {
            con.disconnect();
            br.getPage(con.getURL().toExternalForm());
            String error = br.getRegex("<div class=\"errorbox-bad\".*?>(.*?)</div>").getMatch(0);
            if (error == null) error = br.getRegex("<div class=\"errorbox-bad\".*?>.*?>(.*?)</>").getMatch(0);
            if (error == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.hoster.sendspacecom.errors.servererror", "Unknown server error"), 5 * 60 * 1000l);
            if (error.contains("You may now download the file")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 30 * 1000l); }
            if (error.contains("full capacity")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.hoster.sendspacecom.errors.serverfull", "Free service capacity full"), 5 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
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

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}