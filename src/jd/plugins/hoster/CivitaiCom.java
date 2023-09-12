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
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CivitaiCom extends PluginForHost {
    public CivitaiCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://civitai.com/content/tos";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "civitai.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/images/\\d+.*");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean FREE_RESUME        = true;
    private final int     FREE_MAXCHUNKS     = 0;
    private final int     FREE_MAXDOWNLOADS  = -1;
    private final Pattern PATTERN_IMAGE      = Pattern.compile("(?i)https?://[^/]+/images/(\\d+).*");
    private final String  PROPERTY_DIRECTURL = "directurl";

    // private final boolean ACCOUNT_FREE_RESUME = true;
    // private final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private final int ACCOUNT_FREE_MAXDOWNLOADS = -1;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "civitai://image/" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), PATTERN_IMAGE).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("type=\"application/json\"[^>]*>(\\{\"props.*?)</script>").getMatch(0);
        final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
        final Map<String, Object> imagemap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/trpcState/json/queries/{0}/state/data");
        final Map<String, Object> metadata = (Map<String, Object>) imagemap.get("metadata");
        String filename = imagemap.get("name").toString();
        final Number filesize = (Number) metadata.get("size");
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(filesize.longValue());
        }
        /* 2023-09-11: Base URL hardcoded from: https://civitai.com/_next/static/chunks/pages/_app-191d571abe9dc30e.js */
        final String baseURL = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/";
        final String directurl = baseURL + imagemap.get("url") + "/width=" + metadata.get("width") + "/" + Encoding.urlEncode(filename);
        link.setProperty(PROPERTY_DIRECTURL, directurl);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media?");
            }
        }
        final String filename = link.getName();
        final String ext = Plugin.getExtensionFromMimeTypeStatic(dl.getConnection().getContentType());
        if (ext != null && filename != null) {
            link.setFinalFileName(this.correctOrApplyFileNameExtension(filename, "." + ext));
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}