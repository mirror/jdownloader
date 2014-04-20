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

import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uzmantv.com" }, urls = { "http://(www\\.)?uzmantv\\.com/[a-z0-9-]+" }, flags = { 0 })
public class UzManTvCom extends PluginForHost {

    private String              DLLINK       = null;

    private static final String DLLINKPART   = "?source=site";
    private static final String INVALIDLINKS = "http://(www\\.)?uzmantv\\.com/(kategoriler|kullanimkosullari|favoriler|facebookSubLogin|yardim|konular|konu|uzman|uzmanlar|iphone|iletisim|uyeliksozlesmesi|images|etiketler|etiket|sitemapsv2|programlar)";

    public UzManTvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

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
        return "http://www.uzmantv.com/kullanimkosullari";
    }

    private String getDllink() throws Exception {
        String dllink = br.getRegex("var \\$flv = encodeURIComponent\\(\"(http://[^<>\"]*?)\"\\)").getMatch(0);
        if (dllink == null) {
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
            if (videoID == null) return null;
            String ext = br.getRegex("ext=([a-z0-9]{2,5})\\&").getMatch(0);
            if (ext == null) ext = "flv";
            dllink = "http://st2.uzmantv.com/c/" + crypticID + "_" + videoID + "_" + execJS(getCorrectJsAdditional()) + "." + ext;
        }
        return dllink;
    }

    private String getCorrectJsAdditional() {
        String securedStuff = br.getRegex("var tok = (.*?);").getMatch(0);
        String allJs[] = br.getRegex("<script type=\"text/javascript\">(.*?)</script>").getColumn(0);
        if (securedStuff == null && (allJs == null || allJs.length == 0)) return null;

        int j = 0, bestMatch = 0, index = 0;
        for (int i = 0; i < allJs.length; i++) {
            for (String var : new Regex(allJs[i], "var ([^\\s=]+)\\s?=").getColumn(0)) {
                if (var.length() > 3) {
                    if (securedStuff.contains(var)) {
                        j++;
                    }
                }
            }
            if (j > bestMatch) {
                index = i;
                bestMatch = j;
            }
            j = 0;
        }
        return allJs[index] + securedStuff;
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
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (downloadLink.getDownloadURL().matches(INVALIDLINKS)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        String ext = DLLINK.substring(DLLINK.length() - 4, DLLINK.length());
        if (ext == null || ext.length() > 5) ext = ".mp4";
        if (DLLINK.contains(".mp4")) ext = ".mp4";
        downloadLink.setFinalFileName(filename.replaceAll("\\?$", "") + ext);
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}