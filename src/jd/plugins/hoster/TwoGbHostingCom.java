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

import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "2gb-hosting.com" }, urls = { "http://(www\\.)?2gb\\-hosting\\.com/(videos|v)/[a-z0-9]+/.*?\\.html" }, flags = { 0 })
public class TwoGbHostingCom extends PluginForHost {

    public TwoGbHostingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.2gb-hosting.com/terms-of-use";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/v/", "/videos/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("2gb-hosting.com/404.html") || br.containsHTML("(>404 Error: Video not found|>404: Video not found\\!|Please check if the link is valid\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>([^<>\"]*?) \\- 2GB Hosting</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String k = br.getRegex("name=\"k\" value=\"(\\d+)\"").getMatch(0);
        if (k == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(downloadLink.getDownloadURL(), "submit=Continue&k=" + k);
        final String js = br.getRegex("<script type=\\'text/javascript\\'>[\t\n\r ]+(eval\\(function.*?)</script>").getMatch(0);
        if (js == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String dllink = decodeDownloadLink(js);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String decodeDownloadLink(final String s) {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            result = engine.eval(new Regex(s, "eval(.*?)$").getMatch(0));
        } catch (final Throwable e) {
            result = null;
        }
        if (result == null || result.toString().length() == 0) { return null; }
        return new Regex(result.toString(), "\'file\'\\s?:\\s?\'(http://.*?)\\'").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}