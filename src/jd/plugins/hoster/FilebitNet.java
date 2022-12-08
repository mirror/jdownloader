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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FilebitNet extends PluginForHost {
    public FilebitNet(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://filebit.net/plans");
    }

    @Override
    public String getAGBLink() {
        return "https://filebit.net/tos";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filebit.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/f/([A-Za-z0-9]+)(\\?i=[^#]+)?#([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean      FREE_RESUME       = true;
    private final int          FREE_MAXCHUNKS    = 0;
    private final int          FREE_MAXDOWNLOADS = -1;
    public static final String API_BASE          = "https://filebit.net";

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

    private String getKey(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }

    public Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader " + getVersion());
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        prepBR(br);
        br.postPageRaw("https://" + this.getHost() + "/storage/bucket/info.json", "{\"file\":\"" + this.getFID(link) + "\"}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final String error = (String) entries.get("error");
        if (error != null) {
            /* E.g. {"error":"invalid file"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> hash = (Map<String, Object>) entries.get("hash");
        if (hash != null) {
            final String hashType = hash.get("type").toString();
            if (hashType.equalsIgnoreCase("sha256")) {
                link.setSha256Hash(hash.get("value").toString());
            }
        }
        final String filename = entries.get("filename").toString();
        final Number filesize = (Number) entries.get("filesize");
        if (filename != null) {
            // TODO: Decrypt filename
            link.setFinalFileName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(filesize.longValue());
        }
        return AvailableStatus.TRUE;
    }

    /** See https://filebit.net/docs/#/?id=multi-file-informations */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBR(this.br);
            br.setCookiesExclusive(true);
            final List<String> fileIDs = new ArrayList<String>();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* 2022-12-08: Tested with 100 items max. */
                    if (index == urls.length || links.size() == 100) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                fileIDs.clear();
                for (final DownloadLink link : links) {
                    fileIDs.add(this.getFID(link));
                }
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("files", fileIDs);
                br.postPageRaw(API_BASE + "/storage/multiinfo.json", JSonStorage.serializeToJson(postData));
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                for (final DownloadLink link : links) {
                    final String fid = this.getFID(link);
                    if (!link.isNameSet()) {
                        link.setName(fid);
                    }
                    final Map<String, Object> info = (Map<String, Object>) entries.get(fid);
                    if (info == null) {
                        /* This should never happen! */
                        link.setAvailable(false);
                        continue;
                    }
                    // TODO: Decrypt filename
                    final String state = info.get("state").toString();
                    final String filename = (String) info.get("name");
                    final Number filesize = (Number) info.get("size");
                    if (!StringUtils.isEmpty(filename)) {
                        link.setFinalFileName(filename);
                    }
                    if (filesize != null) {
                        link.setVerifiedFileSize(filesize.longValue());
                    }
                    if (state.equalsIgnoreCase("ONLINE")) {
                        link.setAvailable(true);
                    } else {
                        /* E.g. {"state":"ERROR"} */
                        link.setAvailable(false);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (true) {
            /* 2022-10-05: This plugin is unfinished */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty)) {
            requestFileInformation(link);
            String dllink = br.getRegex("").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, FREE_RESUME, FREE_MAXCHUNKS);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(directlinkproperty);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
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