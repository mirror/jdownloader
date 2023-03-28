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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spinrilla.com" }, urls = { "https?://(?:www\\.)?spinrilla\\.com/(?:mixtapes|songs)/(\\d+)-([a-z0-9\\-]+)" })
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

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* 2020-05-07: Use part of URL as filename */
        String filename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1).replace("-", " ");
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        if (link.getPluginPatternMatcher().matches(TYPE_SONGS)) {
            link.setFinalFileName(filename + ".mp3");
        } else {
            link.setFinalFileName(filename + ".zip");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getPluginPatternMatcher().matches(TYPE_MIXTAPES) && !br.containsHTML("id=\"released\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            if (link.getDownloadURL().matches(TYPE_SONGS)) {
                dllink = "https://api.spinrilla.com/tracks/" + this.getFID(link) + "/download";
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
                    final String c = getCaptchaCode("recaptcha", cf, link);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        dl.setFilenameFix(true);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* Prefer filename from header */
        final String serverFilename = Plugin.getFileNameFromDispositionHeader(dl.getConnection());
        if (!StringUtils.isEmpty(serverFilename)) {
            link.setFinalFileName(serverFilename);
        }
        link.setProperty("directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                link.setProperty(property, Property.NULL);
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