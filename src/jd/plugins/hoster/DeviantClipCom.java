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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantclip.com", "dachix.com", "dagay.com" }, urls = { "http://(www\\.)?deviantclipdecrypted\\.com/watch/[a-z0-9\\-]+(\\?fileid=[A-Za-z0-9]+)?", "http://(www\\.)?dachixdecrypted\\.com/watch/[A-Za-z0-9\\-]+(\\?fileid=[A-Za-z0-9]+)?", "http://(www\\.)?dagaydecrypted\\.com/watch/[A-Za-z0-9\\-]+(\\?fileid=[A-Za-z0-9]+)?" })
public class DeviantClipCom extends PluginForHost {
    public DeviantClipCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("decrypted.com/", ".com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.deviantclip.com/DMCA.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    // This hoster also got a decrypter called "DeviantClipComGallery" so if the
    // host goes down please also delete the decrypter!
    /* Tags: crakpass network, dagfs.com, bestgonzo.com, kinkyfrenchies.com */
    public String   dllink        = null;
    private boolean server_issues = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final String url_filename = new Regex(downloadLink.getDownloadURL(), "/watch/(.+)").getMatch(0);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String thelink = downloadLink.getDownloadURL();
        br.getPage(thelink);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("jpg\">\\s*</video>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (downloadLink.getDownloadURL().contains("?fileid=")) {
            dllink = br.getRegex("<img  \\d+ src=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else {
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
            if (dllink == null) {
                dllink = br.getRegex("<source src=(?:'|\")(https?://[^<>'\"]*?)('|\")").getMatch(0);
            }
            if (filename != null && filename.matches("Free Porn Tube Videos, Extreme Hardcore Porn Galleries")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (filename == null) {
                filename = url_filename;
            }
            filename = Encoding.htmlDecode(filename.trim());
            if (filename.contains("\\x")) {
                filename = Encoding.urlDecode(filename.replaceAll("\\\\x", "%"), false);
            }
            downloadLink.setName(filename + ".mp4");
        }
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(dllink);
                if (downloadLink.getDownloadURL().contains("?fileid=")) {
                    final String ending = LoadImage.getFileType(dllink, con.getContentType());
                    if (ending != null && !downloadLink.getName().endsWith(ending)) {
                        downloadLink.setFinalFileName(downloadLink.getName() + ending);
                    }
                }
                if (!con.getContentType().contains("html")) {
                    long size = con.getLongContentLength();
                    if (size != 0) {
                        downloadLink.setDownloadSize(con.getLongContentLength());
                    } else {
                        server_issues = true;
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            if (br.containsHTML("<embed src=")) {
                /* E.g. video not even playable via browser. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}