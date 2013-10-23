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

import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "divxstage.net" }, urls = { "http://(www\\.)?(divxstage\\.(net|eu)/video/|embed\\.divxstage\\.(net|eu)/embed\\.php\\?v=)[a-z0-9]+" }, flags = { 0 })
public class DivxStageNet extends PluginForHost {

    public String DLLINK = null;

    public DivxStageNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://divxstage.eu/video/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    }

    @Override
    public String getAGBLink() {
        return "http://www.divxstage.net/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("The file is beeing transfered to our other servers")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        dl.startDownload();
    }

    // This plugin is 99,99% copy the same as the MovShareNet plugin, if this
    // gets broken please also check the other one!
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We need you to prove you're human")) {
            Form IAmAHuman = br.getForm(0);
            if (IAmAHuman == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.submitForm(IAmAHuman);
        }
        if (br.containsHTML("The file is beeing transfered to our other servers")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        if (br.containsHTML("This file no longer exists on our servers")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"video_det\">.*?<strong>(.*?)</strong>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (!br.containsHTML("The file is beeing transfered to our other servers")) {
            String fkey = br.getRegex("filekey=\"([^<>\"]*?)\"").getMatch(0);
            if (fkey == null && br.containsHTML("w,i,s,e")) {
                String result = unWise();
                fkey = new Regex(result, "(\"\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}-[a-f0-9]{32})\"").getMatch(0);
            }
            if (fkey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://divxstage.eu/api/player.api.php?file=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + "&user=undefined&key=" + Encoding.urlEncode(fkey) + "&pass=undefined&codes=1");
            DLLINK = br.getRegex("url=(http://[^<>\"]*?)\\&").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename.trim().equals("Untitled")) {
            downloadLink.setFinalFileName("Video " + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + ".avi");
        } else {
            downloadLink.setFinalFileName(filename + ".flv");
        }
        return AvailableStatus.TRUE;
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) return null;
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("ECMAScript");
        try {
            engine.eval("var res = " + fn);
            result = (String) engine.get("res");
            result = new Regex(result, "eval\\((.*?)\\);$").getMatch(0);
            engine.eval("res = " + result);
            result = (String) engine.get("res");
            String res[] = result.split(";\\s;");
            engine.eval("res = " + new Regex(res[res.length - 1], "eval\\((.*?)\\);$").getMatch(0));
            result = (String) engine.get("res");
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
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