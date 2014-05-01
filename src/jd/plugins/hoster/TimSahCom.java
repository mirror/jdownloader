//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.net.UnknownHostException;
import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "timsah.com" }, urls = { "http://(www\\.)?timsah\\.com/[A-Za-z0-9_\\-]+/[A-Za-z0-9]+" }, flags = { 0 })
public class TimSahCom extends PluginForHost {

    private String DLLINK = null;

    public TimSahCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?timsah\\.com/(kategori|uye|videolar|bizeulasin)/.+";

    private String execJS(final String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
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
    public String getAGBLink() {
        return "http://www.timsah.com/bizeulasin/form";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().matches(INVALIDLINKS)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(downloadLink.getDownloadURL());
        if (!br.containsHTML("id=\"playerDiv\"") || br.containsHTML("(\">URLde verilen video ID hatalı|<title>Yeni gelen videolar \\- Timsah\\.com</title>)") || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("</td><td class=\"mid\"><h1>(.*?)</h1></td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("property=\"og:site_name\" name=\"og:site_name\" /><meta content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>Video: (.*?) \\- Timsah\\.com</title>").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = br.getRegex("file:\\'(http://[^<>\"\\']*?)\\',type:\\'flv\\'").getMatch(0);
        if (DLLINK == null) {
            String killJS2 = br.getRegex("var tok = (.*?);").getMatch(0);
            String killJS = br.getRegex("src=\"/v2/js/video\\.comments\\.js\\?v=\\d+\" type=\"text/javascript\"></script><script type=\"text/javascript\">(.*?)</script>").getMatch(0);
            DLLINK = br.getRegex("file:\\'(http.*?)\"").getMatch(0);
            String killedJS = execJS(killJS + killJS2);
            if (killedJS == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = Encoding.htmlDecode(DLLINK) + killedJS + ".flv";
            if (DLLINK == null || killJS == null || killJS2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openGetConnection(DLLINK);
            } catch (final UnknownHostException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
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