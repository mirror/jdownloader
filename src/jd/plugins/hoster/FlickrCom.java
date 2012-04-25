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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "http://(www\\.)?flickrdecrypted\\.com/photos/[^<>\"/]+/\\d+" }, flags = { 0 })
public class FlickrCom extends PluginForHost {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://flickr.com";
    }

    private static final Object LOCK   = new Object();
    private static boolean      loaded = false;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("flickrdecrypted.com/", "flickr.com/"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        PluginForDecrypt flickrDecrypter = JDUtilities.getPluginForDecrypt("flickr.com");
        if (loaded == false) {
            synchronized (LOCK) {
                if (loaded == false) {
                    /*
                     * we only have to load this once, to make sure its loaded
                     */
                    flickrDecrypter = JDUtilities.getPluginForDecrypt("flickr.com");
                }
                loaded = true;
            }
        }
        final Object ret = flickrDecrypter.getPluginConfig().getProperty("cookies", null);
        if (downloadLink.getBooleanProperty("cookiesneeded") && ret == null) {
            // This should never happen
            logger.info("A property couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (downloadLink.getBooleanProperty("cookiesneeded")) {
            final HashMap<String, String> cookies = (HashMap<String, String>) ret;
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                this.br.setCookie("http://flickr.com", entry.getKey(), entry.getValue());
            }
        }
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String filename = getFilename();
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML("(photo\\-div video\\-div|class=\"video\\-wrapper\")")) {
            final String lq = createGuid();
            final String secret = br.getRegex("photo_secret=(.*?)\\&").getMatch(0);
            final String nodeID = br.getRegex("data\\-comment\\-id=\"(\\d+\\-\\d+)\\-").getMatch(0);
            if (secret == null || nodeID == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://www.flickr.com/video_playlist.gne?node_id=" + nodeID + "&tech=flash&mode=playlist&lq=" + lq + "&bitrate=700&secret=" + secret + "&rd=video.yahoo.com&noad=1");
            final Regex parts = br.getRegex("<STREAM APP=\"(http://.*?)\" FULLPATH=\"(/.*?)\"");
            final String part1 = parts.getMatch(0);
            final String part2 = parts.getMatch(1);
            if (part1 == null || part2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            filename += ".flv";
            DLLINK = part1 + part2.replace("&amp;", "&");
        } else {
            br.getPage(downloadLink.getDownloadURL() + "/in/photostream");
            DLLINK = getFinalLink();
            if (DLLINK == null) DLLINK = br.getRegex("\"(http://farm\\d+\\.(static\\.flickr|staticflickr)\\.com/\\d+/.*?)\"").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            filename = Encoding.htmlDecode(filename.trim() + ext);
        }
        downloadLink.setFinalFileName(filename);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String createGuid() {
        String a = "";
        final String b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._";
        int c = 0;
        while (c < 22) {
            final int index = (int) Math.floor(Math.random() * b.length());
            a = a + b.substring(index, index + 1);
            c++;
        }
        return a;
    }

    private String getFinalLink() {
        final String[] sizes = { "o", "l", "c", "z", "m", "n", "s", "t", "q", "sq" };
        String finallink = null;
        for (String size : sizes) {
            finallink = br.getRegex(size + ": \\{[\t\n\r ]+url: \\'(http://[^<>\"]*?)\\',[\t\n\r ]+").getMatch(0);
            if (finallink != null) break;
        }
        return finallink;
    }

    private String getFilename() {
        String filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\">").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"photo\\-title\">(.*?)</h1").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\| Flickr \\- (F|Ph)otosharing\\!</title>").getMatch(0);
            }
        }
        return filename;
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
