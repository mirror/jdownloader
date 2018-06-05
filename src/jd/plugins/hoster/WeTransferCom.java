//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com" }, urls = { "http://wetransferdecrypted/[a-f0-9]{46}/[a-f0-9]{4,12}/[a-f0-9]{46}" })
public class WeTransferCom extends PluginForHost {
    public WeTransferCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://wetransfer.info/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public static Browser prepBR(final Browser br) {
        br.addAllowedResponseCodes(new int[] { 410, 503 });
        br.setCookie("wetransfer.com", "wt_tandc", "20170208");
        return br;
    }

    private String dlLink = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = prepBR(new Browser());
        setBrowserExclusive();
        final String[] dlinfo = link.getDownloadURL().replace("http://wetransferdecrypted/", "").split("/");
        final String id_main = dlinfo[0];
        final String security_hash = dlinfo[1];
        final String id_single = dlinfo[2];
        if (security_hash == null || id_main == null || id_single == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String referer = link.getStringProperty("referer");
        if (referer == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(referer);
        final String[] recipient_id = referer.replaceFirst("https?://[^/]+/+", "").split("/");
        if (recipient_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("security_hash", security_hash);
        map.put("file_ids", Arrays.asList(new String[] { id_single }));
        if (recipient_id.length == 4) {
            map.put("recipient_id", recipient_id[2]);
        }
        final PostRequest post = new PostRequest(br.getURL(("/api/v4/transfers/" + id_main + "/download")));
        post.getHeaders().put("Accept", "application/json");
        post.getHeaders().put("Content-Type", "application/json");
        post.setPostDataString(JSonStorage.toString(map));
        br.getPage(post);
        if ("invalid_transfer".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dlLink = PluginJSonUtils.getJsonValue(br, "direct_link");
        if (dlLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dlLink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>Error while downloading your file")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}