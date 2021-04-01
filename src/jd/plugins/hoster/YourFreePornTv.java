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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class YourFreePornTv extends PluginForHost {
    private String dllink = null;

    public YourFreePornTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.yourfreeporn.us/static/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "yourfreeporn.tv", "yourfreeporn.us" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/(\\d+)(/([a-z0-9\\-_]+))?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2021-04-01: change main domain has changed from yourfreeporn.us -> yourfreeporn.tv */
        return this.rewriteHost(getPluginDomains(), host);
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private static final String HTML_LIMITREACHED = ">\\s*You have reached your free daily limit";
    private static final String HTML_PREMIUMONLY  = "class=\"goPremiumPitch[^\"]*\"";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // br.postPage(link.getDownloadURL(), "language=en_US");
        /* 2021-04-01 */
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*This video cannot be found") || !this.br.getURL().contains(this.getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        /**
         * Limit reached? We don't care, we can get the filename from the url and still start the download
         */
        String filenameURL = new Regex(link.getPluginPatternMatcher(), "/video/\\d+/([a-z0-9\\-_]+)").getMatch(0);
        if (filenameURL != null) {
            filenameURL = filenameURL.replace("-", " ").trim();
        }
        filename = filenameURL;
        if (br.containsHTML(HTML_PREMIUMONLY) || br.containsHTML(HTML_LIMITREACHED)) {
            if (br.containsHTML(HTML_PREMIUMONLY)) {
                link.getLinkStatus().setStatusText("This file can only be downloaded by premium users");
            }
        } else {
            if (filename == null) {
                final String ogTitle = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
                final String header = br.getRegex("class\\s*=\\s*\"page_title\"\\s*>\\s*(.*?)\\s*</h").getMatch(0);
                if (StringUtils.isAllNotEmpty(header, ogTitle)) {
                    if (ogTitle.length() < header.length()) {
                        filename = ogTitle;
                    } else {
                        filename = header;
                    }
                } else if (StringUtils.isNotEmpty(header)) {
                    filename = header;
                } else {
                    filename = ogTitle;
                }
            }
        }
        if (filename == null) {
            filename = this.getFID(link);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML(HTML_LIMITREACHED)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        } else if (br.containsHTML(HTML_PREMIUMONLY)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        final String key = br.getRegex("\\?key=([a-f0-9]+)").getMatch(0);
        if (key == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("/media/nuevo/play.php?key=" + key);
        dllink = br.getRegex("<file>(http[^<>\"]+)</file>").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -3);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML(">403 \\- Forbidden<")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            }
            /* 2021-04-01: Random errors may appear such as 471 Unauthorized */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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
}