//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.KoofrNetFolder;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { KoofrNetFolder.class })
public class KoofrNet extends PluginForHost {
    public KoofrNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static final String PROPERTY_FOLDER_ID             = "folder_id";
    public static final String PROPERTY_FILENAME_FROM_CRAWLER = "filename_from_crawler";

    public static List<String[]> getPluginDomains() {
        return KoofrNetFolder.getPluginDomains();
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
            /* No regex needed: All items get added via crawler plugin. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://koofr.eu/tos/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFolderID(link);
        final String path = getInternalPath(link);
        if (fid != null && path != null) {
            return "koofr://" + fid + "_" + path;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFolderID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_FOLDER_ID);
    }

    private String getInternalPath(final DownloadLink link) {
        try {
            return UrlQuery.parse(link.getPluginPatternMatcher()).get("path");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        final String dllink = getDirecturl(link);
        prepareDownloadHeaders(br, link);
        basicLinkCheck(br.cloneBrowser(), br.createGetRequest(dllink), link, null, null);
        final String jsonFromHeader = br.getRequest().getResponseHeader("x-file-info");
        if (jsonFromHeader != null && jsonFromHeader.startsWith("{")) {
            /* Same json rthey return via webapi. */
            final Map<String, Object> resource = restoreFromString(jsonFromHeader, TypeRef.MAP);
            parseFileInfo(link, resource);
        }
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfo(final DownloadLink file, final Map<String, Object> resource) {
        file.setFinalFileName(resource.get("name").toString());
        file.setVerifiedFileSize(((Number) resource.get("size")).longValue());
        file.setMD5Hash(resource.get("hash").toString());
    }

    private String getDirecturl(final DownloadLink link) {
        final String filename = link.getStringProperty(PROPERTY_FILENAME_FROM_CRAWLER);
        final String password = link.getDownloadPassword();
        String url = "https://app.koofr.net/content/links/" + this.getFolderID(link) + "/files/get/" + Encoding.urlEncode(filename) + "?path=" + this.getInternalPath(link);
        if (password != null) {
            url += "&password=" + Encoding.urlEncode(password);
        }
        return url;
    }

    private void prepareDownloadHeaders(final Browser br, final DownloadLink link) {
        br.getHeaders().put("Referer", "https://app.koofr.net/links/" + this.getFolderID(link));
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String dllink = getDirecturl(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepareDownloadHeaders(br, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    protected void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (br.getRequest().getHtmlCode().length() <= 100 && con.getContentType().contains("text")) {
                /*
                 * Response is a human readable errormessage e.g. "File download restricted due to possible dangerous content" for .exe
                 * files.
                 */
                throw new PluginException(LinkStatus.ERROR_FATAL, br.getRequest().getHtmlCode().trim());
            } else {
                super.handleConnectionErrors(br, con);
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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