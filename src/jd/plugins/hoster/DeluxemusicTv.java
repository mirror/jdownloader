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
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.translate._JDT;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deluxemusic.tv" }, urls = { "https?://(?:www\\.)?deluxetv\\-vimp\\.mivitec\\.net/.*?/?video/[^/]+/[a-f0-9]{32}.*" })
public class DeluxemusicTv extends PluginForHost {
    public DeluxemusicTv(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags:
    // protocol: https forced
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://deluxetv-vimp.mivitec.net/pages/view/id/1";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(500);
        final Regex urlregex = new Regex(link.getDownloadURL(), "/video/([^/]+).*([a-f0-9]{32})(/(\\d+))?");
        final String fid = urlregex.getMatch(1);
        final String category_id = urlregex.getMatch(3);
        final String url_filename = urlregex.getMatch(0);
        /* Set unique videoid. */
        link.setLinkID(fid);
        br.getPage(link.getDownloadURL());
        final DeluxemusicTvConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.DeluxemusicTv.DeluxemusicTvConfigInterface.class);
        /*
         * Usually this setting is for decrypters but in this case their contentservers are very slow which is why users can disable the
         * filesize check - it speeds up the linkcheck for this plugin!
         */
        final boolean fastlinkcheck = cfg.isFastLinkcheckEnabled();
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500 || !this.br.getURL().matches(".*?[a-f0-9]{32}.*?")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 500) {
            /* Can be caused by trying to access wrong urls but also if there are server issues! */
            server_issues = true;
            return AvailableStatus.UNCHECKABLE;
        }
        final String description = this.br.getRegex("name=\"description\" content=\"([^<>\"]+)\"").getMatch(0);
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        String filename = br.getRegex("<title>([^<>\"]+):: Medien :: DELUXE MUSIC</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("name=\"title\" content=\"([^<>\"]+)MedienDELUXE MUSIC\"").getMatch(0);
        }
        if (filename == null) {
            filename = url_filename;
        }
        if (category_id != null) {
            /*
             * Actually the "/category/" if not needed even if a category_id is given but we'll handle it similar to how the website handles
             * it.
             */
            dllink = "https://deluxetv-vimp.mivitec.net/category/" + category_id + "/getMedium/" + fid + ".mp4";
        } else {
            dllink = "https://deluxetv-vimp.mivitec.net/getMedium/" + fid + ".mp4";
        }
        filename = nicerDicerFilename(filename);
        final String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (dllink != null && !fastlinkcheck) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    public static String nicerDicerFilename(String name) {
        if (name == null) {
            return null;
        }
        final String discodeluxe_setnumber_str = new Regex(name, Pattern.compile("DISCO.*?DELUXE.*?Set.{0,}?(\\d+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (discodeluxe_setnumber_str != null) {
            final int discodeluxe_setnumber = Integer.parseInt(discodeluxe_setnumber_str);
            final String discodeluxe_setnumber_str_formatted = new DecimalFormat("000").format(discodeluxe_setnumber);
            name = "deluxemusictv_disco_deluxe_set_" + discodeluxe_setnumber_str_formatted;
        } else {
            name = "deluxemusictv_" + name;
        }
        name = Encoding.htmlDecode(name);
        name = name.trim();
        return name;
    }

    private String getLowerQualityVideourl(final String fid) {
        String dllink = br.getRegex("property=\"og:video:url\" content=\"(http[^<>\"]+)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(https?://deluxetv\\-vimp\\.mivitec\\.net/getMedium/[^<>\"/]+)").getMatch(0);
        }
        if (dllink == null) {
            dllink = "https://deluxetv-vimp.mivitec.net/getMedium/" + fid + ".m4v";
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
    public String getDescription() {
        return "Lade Videos aus der DeluxeTV Mediathek herunter";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DeluxemusicTvConfigInterface.class;
    }

    public static interface DeluxemusicTvConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getEnableCategoryCrawler_label() {
                return "Enable category crawler? This may add huge amounts of URLs!";
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(false)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(10)
        boolean isEnableCategoryCrawler();

        void setEnableCategoryCrawler(boolean b);
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
