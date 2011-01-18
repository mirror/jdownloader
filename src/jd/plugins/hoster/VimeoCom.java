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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "http://[\\w\\.]*?vimeo\\.com/[0-9]+" }, flags = { 0 })
public class VimeoCom extends PluginForHost {
    static private final String AGB = "http://www.vimeo.com/terms";
    private String              clipData;
    private String              finalURL;

    public VimeoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return AGB;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();

        br.getPage(downloadLink.getDownloadURL() + "?hd=1");
        String clipID = br.getRegex("targ_clip_id:   (\\d+)").getMatch(0);

        this.clipData = br.getPage("/moogaloop/load/clip:" + clipID + "/local?param_force_embed=0&param_clip_id=" + clipID + "&param_show_portrait=0&param_multimoog=&param_server=vimeo.com&param_show_title=0&param_autoplay=0&param_show_byline=0&param_color=00ADEF&param_fullscreen=1&param_md5=0&param_context_id=&context_id=null");
        String title = getClipData("caption");
        String dlURL = "/moogaloop/play/clip:" + getClipData("clip_id") + "/" + getClipData("request_signature") + "/" + getClipData("request_signature_expires") + "/?q=" + (getClipData("isHD").equals("1") ? "hd" : "sd");
        br.setFollowRedirects(false);
        br.getPage(dlURL);
        this.finalURL = br.getRedirectLocation();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(finalURL);
            if (con.getContentType() != null && con.getContentType().contains("mp4")) {
                downloadLink.setFinalFileName(title + ".mp4");
            } else {
                downloadLink.setFinalFileName(title + ".flv");
            }
            downloadLink.setDownloadSize(br.getRequest().getContentLength());
            if (title == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            downloadLink.setName(title);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private String getClipData(String tag) {
        return new Regex(this.clipData, "<" + tag + ">(.*?)</" + tag + ">").getMatch(0);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        requestFileInformation(downloadLink);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0).startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {

        return 20;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
