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

@HostPlugin(revision = "$Revision: 28691 $", interfaceVersion = 3, names = { "pornmaki.com", "amalandtube.com", "recordedcams.com", "trannyx.com", "429tube.com", "nakedtube.com", "shockingmovies.com", "fapbox.com", "preggoporn.tv", "rookiepornvideos.com", "chickswithdicks.video", "amateurgaymovies.com", "watchtwinks.com", "sextube.desi", "chopsticktube.com", "tubechica.com", "blacktubeporn.net", "bisexualmantube.com", "gaybearflix.com", "realthaisluts.com", "realteenmovies.com", "voyeurtubevideos.com", "ridemycocktube.com", "habibiporn.com", "xlactating.com", "tubeenema.com", "straponfuckvideos.com", "3dtube.xxx", "bigblackcocktube.com", "fishnetfucking.com", "milf.dk", "sluttywifelovers.com", "kingsizetits.com", "cumloadedgirls.com", "skeetporntube.com", "acuptube.com", "chubbycut.com", "nopixeljaps.com", "pinaysmut.com", "sexyfeet.tv", "dirtypantyporn.com", "xxxuniformporn.com",
        "smartgirlstube.com", "facialcumtube.com", "throatfucktube.com", "freeyogaporn.com", "domsubtube.com", "bdsmpornflix.com", "machinefucked.me", "toyspornmovies.com", "tastypussytube.com", "mylesbianporn.com", "inkedgirlsporn.com", "freedptube.com", "tinydicktube.com", "buttsextube.com", "gropingtube.com", "naughtyhighschoolporn.com", "realityxxxtube.com", "onlyhairyporn.com" }, urls = { "http://(?:www\\.)?pornmaki\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?amalandtube\\.com/free/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?recordedcams\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?trannyx\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?429tube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?nakedtube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html",
        "http://(?:www\\.)?shockingmovies\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?fapbox\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?preggoporn\\.tv/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?rookiepornvideos\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?chickswithdicks\\.video/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?amateurgaymovies\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?watchtwinks\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?sextube\\.desi/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?chopsticktube\\.com/video/\\d+/[A-Za-z0-9\\-]+\\.html", "http://(?:www\\.)?tubechica\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?blacktubeporn\\.net/video/[A-Za-z0-9\\-]+\\-\\d+\\.html",
        "http://(?:www\\.)?bisexualmantube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?gaybearflix\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?realthaisluts\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?realteenmovies\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?voyeurtubevideos\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?ridemycocktube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?habibiporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?xlactating\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?tubeenema\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?straponfuckvideos\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?3dtube\\.xxx/video/[A-Za-z0-9\\-]+\\-\\d+\\.html",
        "http://(?:www\\.)?bigblackcocktube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?fishnetfucking\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?milf\\.dk/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?sluttywifelovers\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?kingsizetits\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?cumloadedgirls\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?skeetporntube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?acuptube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?chubbycut\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?nopixeljaps\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?pinaysmut\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html",
        "http://(?:www\\.)?sexyfeet\\.tv/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?dirtypantyporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?xxxuniformporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?smartgirlstube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?facialcumtube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?throatfucktube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?freeyogaporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?domsubtube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?bdsmpornflix\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?machinefucked\\.me/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?toyspornmovies\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html",
        "http://(?:www\\.)?tastypussytube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?mylesbianporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?inkedgirlsporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?freedptube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?tinydicktube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?buttsextube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?gropingtube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?naughtyhighschoolporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?realityxxxtube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?onlyhairyporn\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html" })
public class UnknownPornScript2 extends PluginForHost {

    public UnknownPornScript2(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.1 */
    // other:

    /* Extension which will be used if no correct extension is found */

    private static final String  type_1            = "^http://(?:www\\.)?[^/]+/(?:video|free)/[A-Za-z0-9\\-]+\\-\\d+\\.html$";
    /* E.g. chopsticktube.com */
    private static final String  type_2            = "^http://(?:www\\.)?[^/]+/video/\\d+/[A-Za-z0-9\\-]+\\-\\d+\\.html$";

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://pornmaki.com/static/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String url_filename = null;
        if (downloadLink.getDownloadURL().matches(type_1)) {
            url_filename = new Regex(downloadLink.getDownloadURL(), "/(?:video|free)/([a-z0-9\\-]+)\\-\\d+\\.html").getMatch(0).replace("-", " ");
        } else {
            url_filename = new Regex(downloadLink.getDownloadURL(), "/video/\\d+/([a-z0-9\\-]+)\\.html").getMatch(0).replace("-", " ");
        }
        String filename = this.br.getRegex("<h2 class=\"page\\-title\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        final String html_clip = this.br.getRegex("urls\\.push\\(\\{(.*?)\\}\\);").getMatch(0);
        if (html_clip != null) {
            /* TODO: Check if some sites can have multiple formats/qualities --> Always find the highest bitrate! */
            dllink = new Regex(html_clip, "file:\"(http[^<>\"]*?)\"").getMatch(0);
        } else {
            dllink = br.getRegex("file:\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || dllink == null) {
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
        downloadLink.setFinalFileName(filename);
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
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
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
        return SiteTemplate.UnknownPornScript2;
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
