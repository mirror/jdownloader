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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.FileconvoyComFolder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { FileconvoyComFolder.class })
public class FileconvoyCom extends PluginForHost {
    public FileconvoyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileconvoy.com/index.php?Section=11";
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

    private String getFID(final DownloadLink link) {
        try {
            return UrlQuery.parse(link.getPluginPatternMatcher()).get("id");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String[]> getPluginDomains() {
        return FileconvoyComFolder.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            /* No regex: URLs get added via crawler */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    private int getMaxChunks(final Account account) {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            final String directurl = link.getPluginPatternMatcher();
            if (isDownload) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, isResumeable(link, null), getMaxChunks(null));
                con = dl.getConnection();
            } else {
                con = br.openGetConnection(directurl);
            }
            try {
                handleConnectionErrors(br, con);
            } catch (final PluginException e) {
                /* E.g. cdn.bunkr.ru -> bunkr.su/v/... -> Try to find fresh directurl */
                logger.info("ContentURL did not lead to downloadable content -> Looking for fresh directurl");
                final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
                final String fid = this.getFID(link);
                if (fid == null) {
                    throw e;
                }
                String freshDirecturl = null;
                for (final String url : urls) {
                    if (url.contains(fid)) {
                        freshDirecturl = url;
                    }
                }
                if (freshDirecturl == null) {
                    logger.info("Failed to find fresh directurl");
                    throw e;
                } else if (StringUtils.equals(directurl, freshDirecturl)) {
                    logger.info("Fresh directurl is the same as old one -> Retrying doesn't make any sense");
                    throw e;
                } else {
                    logger.info("Trying again with fresh directurl: " + freshDirecturl);
                    if (isDownload) {
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, freshDirecturl, true, this.getMaxChunks(null));
                        con = dl.getConnection();
                    } else {
                        con = br.openGetConnection(freshDirecturl);
                    }
                    handleConnectionErrors(br, con);
                    logger.info("Fresh directurl is working: " + freshDirecturl);
                }
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            final String filenameFromHeader = Plugin.getFileNameFromHeader(con);
            if (!StringUtils.isEmpty(filenameFromHeader)) {
                link.setFinalFileName(filenameFromHeader);
            }
        } finally {
            if (!isDownload) {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (this.dl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (FileconvoyComFolder.isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}