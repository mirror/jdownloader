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
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.WesenditComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { WesenditComCrawler.class })
public class WesenditCom extends PluginForHost {
    public WesenditCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        WesenditComCrawler.prepBR(br);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.wesendit.com/terms-of-use";
    }

    private static List<String[]> getPluginDomains() {
        return WesenditComCrawler.getPluginDomains();
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
            /* Items are added via crawler so no regular expression is needed here */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    public static final String PROPERTY_FOLDER_ID    = "folder_id";
    public static final String PROPERTY_FILE_ID      = "file_id";
    public static final String PROPERTY_RECIPIENT_ID = "recipient_id";
    private final String       PROPERTY_DIRECTURL    = "directurl";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String folderID = link.getStringProperty(PROPERTY_FOLDER_ID);
        final long fileID = link.getLongProperty(PROPERTY_FILE_ID, -1);
        if (folderID != null && fileID != -1) {
            return this.getHost() + "://folder/" + folderID + "/file/" + fileID;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        final String folderID = link.getStringProperty(PROPERTY_FOLDER_ID);
        final long fileID = link.getLongProperty(PROPERTY_FILE_ID, -1);
        final String recipientID = link.getStringProperty(PROPERTY_RECIPIENT_ID);
        if (folderID == null || fileID == -1) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        // br.setAllowedResponseCodes(400);
        br.getPage("https://www." + getHost() + "/?timestamp=" + System.currentTimeMillis());
        final String ipWithPort = br.getRequest().getResponseHeader("Cloudfront-Viewer-Address");
        if (ipWithPort == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ip = ipWithPort.replaceFirst(":\\d+$", ""); // remove port
        final Map<String, Object> postdata = new HashMap<String, Object>();
        postdata.put("downloaderIpAddress", ip);
        postdata.put("fileId", fileID);
        postdata.put("isDownloadAll", false);
        postdata.put("publicId", folderID);
        if (recipientID != null) {
            postdata.put("recipient", recipientID);
        }
        br.postPageRaw("https://api-prod.wesendit.com/web2/api/files/transfers/download", JSonStorage.serializeToJson(postdata));
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String dllink = entries.get("downloadUrl").toString();
        link.setProperty(PROPERTY_DIRECTURL, dllink);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}