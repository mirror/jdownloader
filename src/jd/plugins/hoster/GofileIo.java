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
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/\\?c=[A-Za-z0-9]+#file=\\d+" })
public class GofileIo extends PluginForHost {
    public GofileIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://gofile.io/";
    }

    private String getC(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "c=([A-Za-z0-9]+)").getMatch(0);
    }

    private String getFileID(final DownloadLink link) throws PluginException {
        return new Regex(link.getPluginPatternMatcher(), "file=(\\d+)").getMatch(0);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;
    private String               downloadURL       = null;

    /** TODO: Implement official API once available: https://gofile.io/?t=api . The "API" used here is only their website. */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String c = getC(link);
        br.getPage("https://gofile.io/?c=" + c);
        final GetRequest server = br.createGetRequest("https://apiv2.gofile.io/getServer?c=" + c);
        server.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
        Browser brc = br.cloneBrowser();
        brc.getPage(server);
        Map<String, Object> response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        String serverHost = null;
        if ("ok".equals(response.get("status"))) {
            final Map<String, Object> data = (Map<String, Object>) response.get("data");
            serverHost = (String) data.get("server");
        } else if ("error".equals(response.get("status"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (serverHost == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final GetRequest post = br.createGetRequest("https://" + serverHost + ".gofile.io/getUpload?c=" + c);
        post.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
        brc = br.cloneBrowser();
        brc.getPage(post);
        response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        if ("ok".equals(response.get("status"))) {
            /*
             * fileID is needed to find the correct files if multiple ones are in a 'folder'. If this is not available we most likely only
             * have a single file.
             */
            final String fileID = getFileID(link);
            final Map<String, Object> data = (Map<String, Object>) response.get("data");
            final Map<String, Map<String, Object>> files = (Map<String, Map<String, Object>>) data.get("files");
            for (Entry<String, Map<String, Object>> file : files.entrySet()) {
                final String id = file.getKey();
                if (fileID == null || id.toString().equals(fileID)) {
                    final Map<String, Object> entry = file.getValue();
                    downloadURL = (String) entry.get("link");
                    final Number size = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
                    if (size.longValue() >= 0) {
                        link.setDownloadSize(size.longValue());
                    }
                    final String name = (String) entry.get("name");
                    final String md5 = (String) entry.get("md5");
                    if (!StringUtils.isEmpty(name)) {
                        link.setFinalFileName(name);
                    }
                    if (!StringUtils.isEmpty(md5)) {
                        link.setMD5Hash(md5);
                    }
                    return AvailableStatus.TRUE;
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        if (downloadURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadURL, true, 0);
        if (dl.getConnection().isOK() && (dl.getConnection().isContentDisposition() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "application"))) {
            dl.startDownload();
        } else {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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