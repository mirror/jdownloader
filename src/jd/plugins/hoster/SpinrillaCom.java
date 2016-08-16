//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spinrilla.com" }, urls = { "https?://(www\\.)?spinrilla\\.com/(?:mixtapes|songs)/[a-z0-9\\-]+" }, flags = { 0 })
public class SpinrillaCom extends PluginForHost {

    public SpinrillaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://spinrilla.com/dmca";
    }

    private static final String TYPE_MIXTAPES = "https?://(www\\.)?spinrilla\\.com/mixtapes/[a-z0-9\\-]+";
    private static final String TYPE_SONGS    = "https?://(www\\.)?spinrilla\\.com/songs/[a-z0-9\\-]+";

    private boolean             skipCaptcha   = true;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?) \\| Spinrilla</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("content=\\'([^<>\"]*?)\\' name=\\'twitter:title\\'").getMatch(0);
        }
        if (filename == null) {
            filename = link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/"));
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        if (link.getDownloadURL().matches(TYPE_SONGS)) {
            link.setFinalFileName(filename + ".mp3");
        } else {
            if (!br.containsHTML("id=\"released\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setFinalFileName(filename + ".zip");
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (downloadLink.getDownloadURL().matches(TYPE_SONGS)) {
                this.br.setFollowRedirects(false);
                final String linkid = new Regex(downloadLink.getDownloadURL(), "spinrilla\\.com/songs/(\\d+)").getMatch(0);
                if (linkid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                this.br.getPage("https://spinrilla.com/tracks/" + linkid + "/download");
                dllink = br.getRedirectLocation();
            } else {
                final String releaseDateTimestampStr = br.getRegex("var dropDate = new Date\\((\\d+)\\);").getMatch(0);
                if (releaseDateTimestampStr != null || br.containsHTML("Download Soon</button>")) {
                    /* Download/Content not yet available */
                    long timeUntilRelease = 0;
                    long releaseDateTimestamp = 0;
                    if (releaseDateTimestampStr != null) {
                        releaseDateTimestamp = Long.parseLong(releaseDateTimestampStr);
                        timeUntilRelease = releaseDateTimestamp - System.currentTimeMillis();
                    }
                    if (timeUntilRelease <= 0) {
                        /* Wrong date or no date given? Okay but we have to wait anyways! */
                        timeUntilRelease = 1 * 60 * 60 * 1000l;
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This mixtape is not yet released", timeUntilRelease);

                }
                br.setFollowRedirects(false);
                final String baseurl = br.getURL();
                br.getPage(baseurl + "/download_prompt");
                if (!skipCaptcha) {
                    final Form dlForm = br.getForm(0);
                    if (dlForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.findID();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    dlForm.put("recaptcha_challenge_field", rc.getChallenge());
                    dlForm.put("recaptcha_response_field", Encoding.urlEncode(c));
                    br.submitForm(dlForm);
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                br.getPage(baseurl + "/download_final");
                br.getPage(baseurl + "/download");
                dllink = br.getRedirectLocation();
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
                if (con.getContentType().contains("html") || con.getContentType().equals("text/xml") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}