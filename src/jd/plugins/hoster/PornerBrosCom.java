//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornerbros.com" }, urls = { "https?://(?:www\\.)?pornerbros\\.com/videos/[a-z0-9\\-_]+" })
public class PornerBrosCom extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    /* tags: fux.com, porntube.com, 4tube.com, pornerbros.com */
    private String dllink = null;

    public PornerBrosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decryptUrl(String encrypted) {
        char[] c = new char[encrypted.length() / 2];
        for (int i = 0, j = 0; i < encrypted.length(); i += 2, j++) {
            c[j] = (char) ((encrypted.codePointAt(i) - 65) * 16 + (encrypted.codePointAt(i + 1) - 65));
        }
        return String.valueOf(c);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornerbros.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("/videos?error=")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().equals("http://www.pornerbros.com/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\\| PornerBros\"").getMatch(0);
        if (filename == null) {
            filename = new Regex(downloadLink.getDownloadURL(), "videos/(?:\\d+/)?([a-z0-9\\-_]+)/?$").getMatch(0);
            /* Make it look a bit better by using spaces instead of '-' which is always used inside their URLs. */
            filename = filename.replace("-", " ");
        }
        filename = filename.trim().replaceAll("\\.$", "");
        // both downloadmethods are still in use
        String paramJs = br.getRegex("<script type=\"text/javascript\" src=\"(/content/\\d+\\.js([^\"]+)?)\"></script>").getMatch(0);
        if (paramJs != null) {
            br.getPage("http://www.pornerbros.com" + paramJs);
            dllink = br.getRegex("url:escape\\(\\'(.*?)\\'\\)").getMatch(0);
            if (dllink == null) {
                // confirmed 16. March 2014
                dllink = br.getRegex("hwurl=\\'([^']+)").getMatch(0);
            }
            if (dllink == null) {
                // confirmed 16. March 2014
                dllink = br.getRegex("file:\\'([^']+)").getMatch(0);
            }
            if (dllink == null) {
                logger.warning("Null download link, reverting to secondary method. Continuing....");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String fileExtension = new Regex(dllink, "https?://[\\w\\/\\-\\.]+(\\.[a-zA-Z0-9]{0,4})\\?.*").getMatch(0);
            if (fileExtension == ".") {
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename + ".flv"));
            } else if (fileExtension != "." && fileExtension != null) {
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename + fileExtension));
            }
        }
        if (dllink == null) {
            final String[] qualities = { "1080", "720", "480", "360", "240" };
            String availablequalities = br.getRegex("\\}\\)\\(\\d+, \\d+, \\[([0-9,]+)\\]\\);").getMatch(0);
            String mediaID = br.getRegex("id=\"download\\d+p\" data\\-id=\"(\\d+)\"").getMatch(0);
            if (mediaID == null) {
                mediaID = br.getRegex("data-id=\"(\\d+)\"").getMatch(0);
            }
            if (mediaID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (availablequalities != null) {
                availablequalities = availablequalities.replace(",", "+");
            } else {
                availablequalities = "";
                /* fallback - first try to find possible qualities */
                for (final String quality : qualities) {
                    if (this.br.containsHTML(">" + quality + "p") && !this.br.containsHTML(quality + "p • N//A")) {
                        if (!availablequalities.equals("")) {
                            availablequalities += "+";
                        }
                        availablequalities += quality;
                    }
                }
                /*
                 * We failed completely - fallback to the basic qualities only. 480p does NOT belong to the basic qualities. NEVER use
                 * values if which you do not know whther the video is available in them or not! If you do that, corresponding final URLs
                 * will end up in 404.
                 */
                if (availablequalities.equals("")) {
                    availablequalities = "360+240";
                }
            }
            this.br.getHeaders().put("Origin", "http://www.pornerbros.com");
            final boolean newWay = true;
            if (newWay) {
                /* 2017-05-31 */
                br.postPage("https://tkn.kodicdn.com/" + mediaID + "/desktop/" + availablequalities, "");
            } else {
                br.postPage("https://tkn.pornerbros.com/" + mediaID + "/desktop/" + availablequalities, "");
            }
            for (final String quality : qualities) {
                dllink = br.getRegex("\"" + quality + "\".*?\"token\":\"(http:[^<>\"]*?)\"").getMatch(0);
                if (dllink != null) {
                    break;
                }
            }
            if (dllink == null) {
                dllink = PluginJSonUtils.getJsonValue(this.br, "token");
            }
            // String paramXml = br.getRegex("name=\"FlashVars\" value=\"xmlfile=(.*?)?(http://.*?)\"").getMatch(1);
            // if (paramXml == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // br.getPage(paramXml);
            // String urlCipher = br.getRegex("file=\"(.*?)\"").getMatch(0);
            // if (urlCipher == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            //
            // DLLINK = decryptUrl(urlCipher);
            if (dllink == null || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String ext = new Regex(dllink, "\\&format=([A-Za-z0-9]{1,5})").getMatch(0);
            if (ext == null) {
                ext = getFileNameExtensionFromString(dllink, ".mp4");
            }
            if (!ext.startsWith(".")) {
                ext = "." + ext;
            }
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        }
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            br2.getHeaders().put("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5");
            con = br2.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript6;
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