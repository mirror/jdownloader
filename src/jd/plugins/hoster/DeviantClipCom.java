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

import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantclip.com" }, urls = { "http://(www\\.)?(gsghe366REHrtzegiolp/Media\\-([0-9]+-[0-9]+_|[0-9]+_).*?\\.html(\\-\\-\\-picture|\\-\\-\\-video)|deviantclip\\.com/watch/.+)" }, flags = { 0 })
public class DeviantClipCom extends PluginForHost {

    public DeviantClipCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This hoster also got a decrypter called "DeviantClipComGallery" so if the
    // host goes down please also delete the decrypter!
    public String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.deviantclip.com/DMCA.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("gsghe366REHrtzegiolp", "deviantclip.com"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        String thelink = downloadLink.getDownloadURL();
        if (thelink.contains("---video") || thelink.contains("/watch/")) {
            br.getPage(thelink.replace("---video", ""));
            String filename = br.getRegex("<li class=\"text\"><h1>(.*?)</h1></li>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("title:\\'(.*?)\\'").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"main\\-sectioncontent\"><p class=\"footer\">.*?<b>(.*?)</b>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("name=\"DC\\.title\" content=\"(.*?)\">").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
                        }
                    }
                }
            }
            dllink = br.getRegex("\"file\":\"(.*?)\"").getMatch(0);
            if (dllink == null) dllink = new Regex(Encoding.htmlDecode(br.toString()), "\"(http://medias\\.deviantclip\\.com/media/[0-9]+/.*?\\.flv\\?.*?)\"").getMatch(0);
            if (filename == null || filename.matches("Free Porn Tube Videos, Extreme Hardcore Porn Galleries")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = filename.trim();
            downloadLink.setFinalFileName(filename + ".flv");
        } else {
            br.getPage(thelink.replace("---picture", ""));
            dllink = br.getRegex("\"(http://medias\\.deviantclip\\.com/media/[0-9]+/.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        URLConnectionAdapter con = br.openGetConnection(dllink);
        if (thelink.contains("---picture") && downloadLink.getName() != null) {
            String ending = LoadImage.getFileType(dllink, con.getContentType());
            if (ending != null) downloadLink.setFinalFileName(downloadLink.getName() + ending);
        }
        if (!con.getContentType().contains("html")) {
            long size = con.getLongContentLength();
            if (size != 0)
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
