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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class IcedriveNet extends PluginForHost {
    public IcedriveNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://www.test.com/help/privacy";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "icedrive.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/\\d+");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_OLD                    = "https?://[^/]+/0/([A-Za-z0-9]+)";
    private static final String TYPE_NEW                    = "https?://[^/]+/file/(\\d+)";
    public static final String  PROPERTY_INTERNAL_FILE_ID   = "internal_file_id";
    public static final String  PROPERTY_INTERNAL_FOLDER_ID = "internal_folder_id";

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final Account account) {
        return 0;
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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_OLD)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_OLD).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NEW).getMatch(0);
        }
    }

    private String getInternalFileID(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_INTERNAL_FILE_ID)) {
            return link.getStringProperty(PROPERTY_INTERNAL_FILE_ID);
        } else if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_NEW)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NEW).getMatch(0);
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback-filename */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(TYPE_OLD)) {
            // Deprecated/old
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String folderID = br.getRegex("downloadFolderZip\\((.*?)\\)").getMatch(0);
            /* Name of current/root folder */
            String filename = br.getRegex(">\\s*Filename\\s*</p>\\s*<p\\s*class\\s*=\\s*\"value\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
            final String type = br.getRegex(">\\s*Type\\s*</p>\\s*<p\\s*class\\s*=\\s*\"value\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
            final String filesize = br.getRegex(">\\s*Size\\s*</p>\\s*<p\\s*class\\s*=\\s*\"value\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
            if ("Folder".equals(type) || folderID != null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!StringUtils.isEmpty(filename)) {
                filename = Encoding.htmlDecode(filename).trim();
                link.setName(filename);
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            final String fileID = br.getRegex("previewItem\\('(.*?)'").getMatch(0);
            if (fileID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(PROPERTY_INTERNAL_FILE_ID, fileID);
            return AvailableStatus.TRUE;
        } else {
            if (link.getFinalFileName() == null) {
                requestFileInformation(link, br);
            }
            /* 2022-02-08: Don't check at all. */
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        this.handleDownload(link);
    }

    private String requestFileInformation(final DownloadLink link, final Browser br) throws IOException, PluginException {
        final String internalFileID = this.getInternalFileID(link);
        if (internalFileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.getPage("https://icedrive.net/API/Internal/V1/?request=download-multi&items=file-" + URLEncode.encodeURIComponent(internalFileID) + "&public=1&sess=1");
        final Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        final Boolean error = (Boolean) entries.get("error");
        if (error == Boolean.TRUE) {
            final String message = (String) entries.get("message");
            if ("No files found".equals(message)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
        }
        final List<Map<String, Object>> mirrors = (List<Map<String, Object>>) entries.get("urls");
        if (mirrors.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find any download mirror");
        }
        final Map<String, Object> mirror = mirrors.get(0);
        final String dllink = (String) mirror.get("url");
        final long fileSize = JavaScriptEngineFactory.toLong(mirror.get("filesize"), -1);
        if (fileSize >= 0) {
            link.setVerifiedFileSize(fileSize);
        }
        final String filename = (String) mirror.get("filename");
        if (link.getFinalFileName() == null) {
            link.setFinalFileName(filename);
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return dllink;
        }
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        final String directlinkproperty = "free_directlink";
        if (!attemptStoredDownloadurlDownload(link, "free_directlink")) {
            final String dllink = requestFileInformation(link, br);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isResumeable(link, null), this.getMaxChunks(null));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directurlproperty) throws Exception {
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, null), this.getMaxChunks(null));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                link.removeProperty(directurlproperty);
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}