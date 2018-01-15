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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
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
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornktube.com" }, urls = { "https?://(?:www\\.)?pornktube\\.com/videos/\\d+/[a-z0-9\\-]+/" })
public class PornktubeCom extends PluginForHost {
    public PornktubeCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), "Preferred_format", FORMATS, "Preferred Format").setDefaultValue(0));
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean  free_resume       = true;
    private static final int      free_maxchunks    = 0;
    private static final int      free_maxdownloads = -1;
    private String                dllink            = null;
    private boolean               server_issues     = false;
    private static final String[] FORMATS           = new String[] { "Best available", "1080p", "720p", "480p", "360p", "240p" };

    @Override
    public String getAGBLink() {
        return "http://www.pornktube.com/support.php";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)/$").getMatch(0).replace("-", " ");
        String filename = br.getRegex("class=\"porntitle left\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        final String json = this.br.getRegex("sources[\t\n\r]*?:[\t\n\r]*?(\\[.*?\\])").getMatch(0);
        if (json != null) {
            int highest = 0;
            int highest_temp = 0;
            String qualtemp = null;
            LinkedHashMap<String, Object> entries = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            for (final Object videoo : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) videoo;
                qualtemp = (String) entries.get("label");
                if (qualtemp != null) {
                    qualtemp = new Regex(qualtemp, "(\\d+)p").getMatch(0);
                }
                if (qualtemp == null) {
                    continue;
                }
                highest_temp = Integer.parseInt(qualtemp);
                if (highest_temp > highest) {
                    highest = highest_temp;
                    dllink = (String) entries.get("file");
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("\\'(?:file|video)\\'[\t\n\r ]*?:[\t\n\r ]*?\\'(http[^<>\"]*?)\\'").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("(http://[A-Za-z0-9\\.\\-]+/get_file/[^<>\"\\&]*?)(?:\\&|'|\")").getMatch(0);
        }
        if (dllink == null) {
            // String id = br.getRegex("data-id=\"(\\d+)\"").getMatch(0);
            // String data_s = br.getRegex("data-s=\"(\\d+)\"").getMatch(0);
            final SubConfiguration cfg = getPluginConfig();
            final int Preferred_format = cfg.getIntegerProperty("Preferred_format", 0);
            logger.info("Debug info: Preferred_format: " + Preferred_format);
            /* Preferred_format 0 = best, 1 = 1080p, 2 = 720p, 3 = 480p, 4 = 360p, 5 = 240p */
            final String items[][] = br.getRegex("data-c=\"([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);([^;]+)").getMatches();
            if (items != null && items.length > 0) {
                for (final String item[] : items) {
                    logger.info("Debug info: Preferred_format: " + Preferred_format + ", checking format: " + item[1]);
                    dllink = "http://s" + item[7] + ".cdna.tv/svideo/?t=" + item[5] + "&k=" + item[6] + "&n=/13000/" + item[4] + "/" + item[4] + "_" + item[1] + ".mp4";
                    logger.info("Debug info: checking dllink: " + dllink);
                    checkDllink(link);
                    if (dllink == null) {
                        continue;
                    } else if (dllink != null && Preferred_format == 0 || Preferred_format == 1 && item[1].equals("1080p") || Preferred_format == 2 && item[1].equals("720p") || Preferred_format == 3 && item[1].equals("480p") || Preferred_format == 4 && item[1].equals("360p") || Preferred_format == 5 && item[1].equals("240p")) {
                        logger.info("Debug info: Preferred_format " + Preferred_format + ", found: " + item[1]);
                        break;
                    }
                }
            }
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", dllink: " + dllink);
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
        return AvailableStatus.TRUE;
    }

    private String checkDllink(final DownloadLink link) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setProperty("directlink", dllink);
            } else {
                dllink = null;
            }
        } catch (final Exception e) { // Connection problem
            dllink = null;
        } finally {
            try {
                con.disconnect();
            } catch (final Exception e) {
            }
        }
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        return SiteTemplate.KernelVideoSharing;
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
