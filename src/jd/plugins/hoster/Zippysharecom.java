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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zippyshare.com" }, urls = { "http://www\\d{0,}\\.zippyshare\\.com/(v/\\d+/file\\.html|.*?key=\\d+)" }, flags = { 0 })
public class Zippysharecom extends PluginForHost {

    private String DLLINK = null;

    public Zippysharecom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String execJS(final String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            result = ((Double) engine.eval(fun)).intValue();
        } catch (final Exception e) {
            JDLogger.exception(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        return result.toString();
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        setBrowserExclusive();
        prepareBrowser(downloadLink);
        String mainpage = downloadLink.getDownloadURL().substring(0, downloadLink.getDownloadURL().indexOf(".com/") + 5);
        // DLLINK via packed JS or Flash App
        if (br.containsHTML("DownloadButton_v1\\.14s\\.swf")) {
            final String flashContent = br.getRegex("swfobject.embedSWF\\((.*?)\\)").getMatch(0);
            if (flashContent != null) {
                DLLINK = new Regex(flashContent, "url: '(.*?)'").getMatch(0);
                final String seed = new Regex(flashContent, "seed: (\\d+)").getMatch(0);
                if (DLLINK == null || seed == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                long time = Integer.parseInt(seed);
                time = 24 * time % 6743256;
                DLLINK = DLLINK + "&time=" + time;
                // corrupted files?
                if (DLLINK.startsWith("nulldownload")) {
                    DLLINK = DLLINK.replaceAll("null", mainpage);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
            }
        } else {
            DLLINK = br.getRegex("var fulllink = '(.*?)';").getMatch(0);
            if (DLLINK != null) {
                final String var = new Regex(DLLINK, "'\\+(.*?)\\+'").getMatch(0);
                String data = br.getRegex("var " + var + " = (.*?)\n").getMatch(0);
                data = execJS(data);
                if (DLLINK.contains(var)) {
                    DLLINK = DLLINK.replace("'+" + var + "+'", data);
                }
            } else {
                DLLINK = br.getRegex("document\\.getElementById\\('dlbutton'\\).href = \"/(.*?)\";").getMatch(0);
                if (DLLINK != null) {
                    String var = new Regex(DLLINK, "\"\\+(.*?)\\+\"").getMatch(0);
                    if (var.matches("\\w+")) {
                        String tmpvar = br.getRegex("var " + var + " = (.*?);").getMatch(0);
                        DLLINK = DLLINK.replaceAll("\\+" + var + "\\+", "\\+" + tmpvar + "\\+");
                        var = tmpvar;
                    }
                    String data = execJS(var);
                    if (DLLINK.contains(var)) {
                        DLLINK = DLLINK.replace("\"+" + var + "+\"", data);
                    }
                    DLLINK = mainpage + DLLINK;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 1);
        dl.setFilenameFix(true);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepareBrowser(final DownloadLink downloadLink) throws IOException {
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.zippyshare.com", "ziplocale", "en");
        br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        prepareBrowser(downloadLink);
        if (br.containsHTML("(File has expired and does not exist anymore on this server|<title>Zippyshare.com - File does not exist</title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex(Pattern.compile("Name:(\\s+)?</font>(\\s+)?<font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(2);
        if (filename == null) {
            final String var = br.getRegex("var fulllink.*?'\\+(.*?)\\+'").getMatch(0);
            filename = Encoding.htmlDecode(br.getRegex("'\\+" + var + "\\+'/(.*?)';").getMatch(0));
        }
        if (filename.contains("/fileName?key=")) {
            final String url = br.getRegex("var fulllink = '(.*?)';").getMatch(-1);
            filename = new Regex(url, "'/(.*?)';").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("\\+\"/(.*?)\";").getMatch(0);
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        if (!br.containsHTML(">Share movie:")) {
            final String filesize = br.getRegex(Pattern.compile("Size:(\\s+)?</font>(\\s+)?<font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(2);
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }
}
