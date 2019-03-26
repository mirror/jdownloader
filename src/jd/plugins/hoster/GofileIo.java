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
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/\\?c=[A-Za-z0-9]+#index=\\d+&id=\\d+" })
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
        final String ret = new Regex(link.getPluginPatternMatcher(), "id=(\\d+)").getMatch(0);
        if (ret == null) {
            if (link.getPluginPatternMatcher().contains("#index=")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            return ret;
        }
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;
    private int                  fileIndex         = -1;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String c = getC(link);
        br.getPage("https://gofile.io/?c=" + c);
        final PostRequest post = br.createPostRequest("https://api.gofile.io/getUpload.php?c=" + c, "");
        post.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
        br.getPage(post);
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if ("ok".equals(response.get("status"))) {
            final String fileID = getFileID(link);
            final List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            int index = 0;
            for (Map<String, Object> entry : data) {
                final Number id = JavaScriptEngineFactory.toLong(entry.get("id"), -1);
                if (id.toString().equals(fileID)) {
                    fileIndex = index;
                    final Number size = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
                    if (size.longValue() >= 0) {
                        // not verified!
                        link.setDownloadSize(size.longValue());
                    }
                    final String name = (String) entry.get("name");
                    if (name != null) {
                        link.setFinalFileName(name);
                    }
                    return AvailableStatus.TRUE;
                }
                index++;
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
        final String c = getC(downloadLink);
        final PostRequest post = br.createPostRequest("https://api.gofile.io/createLink.php?c=" + c, "");
        post.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
        br.getPage(post);
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String downloadURL;
        if ("ok".equals(response.get("status"))) {
            final List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            final Map<String, Object> entry = data.get(fileIndex);
            downloadURL = (String) entry.get("link");
            if (StringUtils.isEmpty(downloadURL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
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