//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "remixshare.com" }, urls = { "http://[\\w\\.]*?remixshare\\.com/(.*?\\?file=|download/)[a-z0-9]+" }, flags = { 0 })
public class RemixShareCom extends PluginForHost {

    public static final String BLOCKED = "(class=\"blocked\"|>Von Deiner IP Adresse kamen zu viele Anfragen innerhalb kurzer Zeit\\.)";

    public RemixShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(3000l);
    }

    private String execJS() throws Exception {
        String fun = br.getRegex("/>Your Download has been started\\.[\t\n\r ]+</div>[\t\n\r ]+<script type=\"text/javascript\" language=\"Javascript\">(.*?)</script>").getMatch(0);
        if (fun == null) {
            String[] allJs = br.getRegex("<script type=\"text/javascript\">(.*?)</script>").getColumn(0);
            for (String s : allJs) {
                if (!s.contains("downloadfinal")) continue;
                fun = s;
                break;
            }
        }
        if (fun == null) return null;
        fun = "var game = \"\";" + fun;
        String ah = new Regex(fun, "(document\\.getElementById\\((.*?)\\)\\.(innerHTML|href))").getMatch(0);
        if (ah != null) fun = fun.replace(ah, "game");
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            result = engine.eval(fun);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
        if (result == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return result.toString();
    }

    public String getAGBLink() {
        return "http://remixshare.com/information/";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML(BLOCKED)) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 10 * 1000l);
        if (br.containsHTML("Download password")) {
            Form pw = br.getFormbyProperty("name", "pass");
            String pass = downloadLink.getStringProperty("pass", null);
            if (pass == null) pass = Plugin.getUserInput("Password?", downloadLink);
            pw.put("passwd", pass);
            br.submitForm(pw);
            br.getPage(br.getRedirectLocation());
            if (br.containsHTML("Incorrect password entered")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                downloadLink.setProperty("pass", pass);
            }
        }
        String lnk = execJS();
        if (lnk == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!lnk.startsWith("http://")) lnk = new Regex(lnk, "<a href=\"(http://.*?)\"").getMatch(0);
        br.getPage(lnk);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getLongContentLength() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 2 * 60 * 60 * 1000l);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://remixshare.com", "lang_en", "english");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(BLOCKED)) return AvailableStatus.UNCHECKABLE;
        br.setFollowRedirects(false);
        if (br.containsHTML("Error Code: 500\\.") || br.containsHTML("Please check the downloadlink")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<span title=\\'([0-9]{10}_)?(.*?)\\'>", Pattern.CASE_INSENSITIVE)).getMatch(1));
        if (filename == null) filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<title>(.*?)Download at remiXshare Filehosting", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("(>|\\.\\.\\.)\\&nbsp;\\((.*?)\\)<").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        }
        String md5Hash = br.getRegex("/>MD5:(.*?)<").getMatch(0);
        if (md5Hash != null && md5Hash.trim().length() == 32) {
            downloadLink.setMD5Hash(md5Hash.trim());
        } else {
            /* fix broken md5 sums */
            downloadLink.setMD5Hash(null);
        }
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }

}