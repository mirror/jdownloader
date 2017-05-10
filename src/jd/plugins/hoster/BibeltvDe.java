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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bibeltv.de" }, urls = { "http://(?:www\\.)?bibeltv\\.de/mediathek/video/[a-z0-9\\-]+/\\?no_cache=1\\&cHash=[a-f0-9]{32}" })
public class BibeltvDe extends PluginForHost {
    public BibeltvDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: kaltura player, medianac, api.medianac.com */
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              tempunavailable   = false;

    @Override
    public String getAGBLink() {
        return "http://www.bibeltv.de/impressum/";
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String url_filename = new Regex(link.getDownloadURL(), "/mediathek/video/([^/]*?)(?:\\-\\d+)?/").getMatch(0).replace("-", " ");
        link.setName(url_filename);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("An error occurred while trying to")) {
            /* Wrong url */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML(">Das Video ist derzeit nicht öffentlich")) {
            /* Video not available at this moment. */
            tempunavailable = true;
            return AvailableStatus.TRUE;
        } else if (this.br.containsHTML(">Video nicht verf")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("itemprop=\"name\" content=\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        String player_embed_url = this.br.getRegex("(//api\\.medianac\\.com/p/\\d+/sp/\\d+/embedIframeJs/[^<>\"]+)\"").getMatch(0);
        if (player_embed_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        player_embed_url = Encoding.htmlDecode(player_embed_url);
        player_embed_url = "http:" + player_embed_url;
        String partner_id = new Regex(player_embed_url, "/partner_id/(\\d+)").getMatch(0);
        if (partner_id == null) {
            partner_id = "107";
        }
        String uiconf_id = this.br.getRegex("uiconf_id/(\\d+)").getMatch(0);
        if (uiconf_id == null) {
            uiconf_id = "11601650";
        }
        String sp = new Regex(player_embed_url, "/sp/(\\d+)/").getMatch(0);
        if (sp == null) {
            sp = "10700";
        }
        String entry_id = new Regex(player_embed_url, "/entry_id/([^/]+)").getMatch(0);
        if (entry_id == null) {
            entry_id = new Regex(player_embed_url, "entry_id=([^\\&=]+)").getMatch(0);
        }
        if (entry_id == null) {
            /* New 2016-12-07 */
            entry_id = this.br.getRegex("/entry_id/([^/]+)").getMatch(0);
        }
        if (entry_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage("http://api.medianac.com/html5/html5lib/v2.39/mwEmbedFrame.php?&wid=_" + partner_id + "&uiconf_id=" + uiconf_id + "&cache_st=0&entry_id=" + entry_id + "&flashvars[streamerType]=auto&playerId=kaltura_player_664&ServiceUrl=http%3A%2F%2Fapi.medianac.com&CdnUrl=http%3A%2F%2Fapi.medianac.com&ServiceBase=%2Fapi_v3%2Findex.php%3Fservice%3D&UseManifestUrls=false&forceMobileHTML5=true&urid=2.39&callback=mwi_kalturaplayer6640");
        String js = this.br.getRegex("kalturaIframePackageData = (\\{.*?\\}\\}\\});").getMatch(0);
        if (js == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        js = js.replace("\\\"", "\"");
        js = new Regex(js, "\"flavorAssets\":(\\[.*?\\])").getMatch(0);
        if (js == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(js);
        long filesize = 0;
        long max_bitrate = 0;
        long max_bitrate_temp = 0;
        LinkedHashMap<String, Object> entries = null;
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String flavourid = (String) entries.get("id");
            if (flavourid == null) {
                continue;
            }
            max_bitrate_temp = JavaScriptEngineFactory.toLong(entries.get("bitrate"), 0);
            if (max_bitrate_temp > max_bitrate) {
                dllink = "http://api.medianac.com/p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id + "/flavorId/" + flavourid + "/format/url/protocol/http/a.mp4";
                max_bitrate = max_bitrate_temp;
                filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (filesize > 0) {
            filesize = filesize * 1024;
        } else {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br2.openHeadConnection(dllink);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                link.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (tempunavailable) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video not available at the moment", 60 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KalturaVideoPlatform;
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
