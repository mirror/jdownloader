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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uzmantv.com" }, urls = { "http://(www\\.)?uzmantv\\.com/[a-z0-9-]+" }, flags = { 0 })
public class UzManTvCom extends PluginForHost {

    public UzManTvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.uzmantv.com/kullanimkosullari";
    }

    private static final String DLLINKPART = "?source=site";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(\">Sayfa bulunamadı|Buraya gelmek için tıkladığınız linkte bir sorun var gibi görünüyor\\. Çünkü maalesef UZMANTV'de böyle bir sayfa yok\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"clear\"></div></div><h5>(.*?)</h5>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"soru_sel_txt\">(.*?)</div><div").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) - UZMANTV</title>").getMatch(0);
            }
        }
        DLLINK = getDllink();
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + DLLINK.substring(DLLINK.length() - 4, DLLINK.length()));
        DLLINK += DLLINKPART;
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink() throws Exception {
        String crypticID = br.getRegex("/getswf/(.*?)\"").getMatch(0);
        if (crypticID == null) {
            crypticID = br.getRegex("id=\"konulink(.*?)\"").getMatch(0);
            if (crypticID == null) {
                crypticID = br.getRegex(">video=\\'(.*?)\\'").getMatch(0);
                if (crypticID == null) {
                    crypticID = br.getRegex("v:\\((.*?)\\)").getMatch(0);
                    if (crypticID == null) {
                        crypticID = br.getRegex("\\'v=(.*?)\\&").getMatch(0);
                    }
                }
            }
        }
        String videoID = br.getRegex("\\'\\&no=(\\d+)\\&").getMatch(0);
        if (videoID == null) {
            videoID = br.getRegex("property=\"og:title\" /><meta content=\"http://st\\d+\\.uzmantv\\.com/videos/(\\d+)/").getMatch(0);
            if (videoID == null) {
                videoID = br.getRegex("rel=\"canonical\" /><link href=\"http://st\\d+\\.uzmantv\\.com/videos/(\\d+)/").getMatch(0);
                if (videoID == null) {
                    videoID = br.getRegex("rel=\"image_src\" /><link href=\"http://st\\d+\\.uzmantv\\.com/videos/(\\d+)/").getMatch(0);
                    if (videoID == null) {
                        videoID = br.getRegex("rel=\"thumbnail\" /><link href=\"http://st\\d+\\.uzmantv\\.com/videos/(\\d+)/").getMatch(0);
                    }
                }
            }
        }
        String jsOne = br.getRegex("</div><script type=\"text/javascript\">(.*?)</script>").getMatch(0);
        String securedStuff = br.getRegex("var tok = (.*?);").getMatch(0);
        String ext = br.getRegex("ext=([a-z0-9]{2,5})\\&").getMatch(0);
        if (ext == null) ext = "flv";
        if (securedStuff == null || jsOne == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        securedStuff = execJS(jsOne + securedStuff);
        return "http://st2.uzmantv.com/c/" + crypticID + "_" + videoID + "_" + securedStuff + "." + ext;
    }

    private String execJS(final String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            result = engine.eval(fun);
        } catch (final Exception e) {
            JDLogger.exception(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return result.toString();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
