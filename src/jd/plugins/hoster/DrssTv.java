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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drss.tv" }, urls = { "http://(www\\.)?drssdecrypted\\.tv/sendung/\\d{2}\\-\\d{2}\\-\\d{4}/" }, flags = { 2 })
public class DrssTv extends PluginForHost {

    public DrssTv(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.drss.tv/index/agb/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("drssdecrypted.tv/", "drss.tv/"));
    }

    /* Settings stuff */
    private static final String  ALLOW_TRAILER     = "ALLOW_TRAILER";
    private static final String  ALLOW_TEASER_PIC  = "ALLOW_TEASER_PIC";
    private static final String  ALLOW_GALLERY     = "ALLOW_GALLERY";
    private static final String  ALLOW_OTHERS      = "ALLOW_OTHERS";

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    private String               DLLINK            = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String date = br.getRegex("class=\"subline\">Sendung vom (\\d{2}\\.\\d{2}\\.\\d{4})</span>").getMatch(0);
        if (date == null) {
            /* Only get date from url when site fails as url date is sometimes wrong/not accurate! */
            date = new Regex(link.getDownloadURL(), "sendung/(\\d{2}\\-\\d{2}\\-\\d{4})/").getMatch(0).replace("-", ".");
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        DLLINK = br.getRegex("<source type=\"video/mp4\"  src=\"(https?://[^<>\"]*?)\"").getMatch(0);
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        filename = date + "_" + filename;

        if (DLLINK.contains("medianac.nacamar.de/")) {
            br.getHeaders().put("Accept-Encoding", null);
        }
        URLConnectionAdapter con = null;
        try {
            try {
                try {
                    /* @since JD2 */
                    con = br.openHeadConnection(DLLINK);
                } catch (final Throwable t) {
                    /* Not supported in old 0.9.581 Stable */
                    con = br.openGetConnection(DLLINK);
                }
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, free_resume, free_maxchunks, "free_directlink");
    }

    /*
     * Streaming example premiumlink from this site: http://www.drss.tv/club/index/
     * http://streaming.drss.tv/videos/8c25ecd576e___nothere___45640420cbeeaae9509bd/8c25ecd576e45640420cbeeaae9509bd
     * .mp4?hash=ea1d74e72b5c42d02ef7da11dd95cca9
     */
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, DLLINK);
        dl.startDownload();
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public String getDescription() {
        return "JDownloader's drss Plugin helps downloading videoclips and photo galleries from drss.tv.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Einstellungen zum Video Download:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_TRAILER, JDL.L("plugins.hoster.drsstv.grabtrailer", "Trailer auch laden, wenn eine komplette Folge verfügbar ist?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_TEASER_PIC, JDL.L("plugins.hoster.drsstv.grabteaserpicture", "Titelbild laden?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_GALLERY, JDL.L("plugins.hoster.drsstv.grabgallery", "Photogallerie laden?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_OTHERS, JDL.L("plugins.hoster.drsstv.hrabothers", "Andere Inhalte (z.B. 'Vor der Sendung'-Videos) laden'?")).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}