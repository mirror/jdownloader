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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mais.uol.com.br" }, urls = { "http://(www\\.)?mais\\.uol\\.com\\.br/view/[a-z0-9]+/[A-Za-z0-9\\-]+" })
public class MaisUolComBr extends PluginForHost {
    public MaisUolComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return "http://mais.uol.com.br/";
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400, 500 });
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404 | br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("> M\\&iacute;dia n\\&atilde;o encontrada|class=\"msg alert\"") || !br.getURL().contains("/view/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String mediaID = br.getRegex("name=\"mediaId\" value=\"(\\d+)\"").getMatch(0);
        if (mediaID == null) {
            mediaID = br.getRegex("mediaId=(\\d+)\"").getMatch(0);
        }
        if (mediaID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("http://api.mais.uol.com.br/apiuol/v3/player/" + mediaID);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("item");
        String filename = (String) entries.get("title");
        final Object formatso = entries.get("formats");
        final Object audioUrlo = entries.get("audioUrl");
        if (audioUrlo != null && audioUrlo instanceof String) {
            /* 2017-02-14 */
            dllink = (String) audioUrlo;
            if (dllink == null || dllink.equals("") || !dllink.startsWith("//")) {
                dllink = null;
            } else {
                dllink = "http:" + dllink;
            }
        }
        if (formatso != null && dllink == null) {
            if (formatso instanceof ArrayList) {
                final ArrayList<Object> ressourcelist = (ArrayList) formatso;
                /* Old */
                for (final Object o : ressourcelist) {
                    final LinkedHashMap<String, Object> format = (LinkedHashMap<String, Object>) o;
                    final int id = ((Number) format.get("id")).intValue();
                    if (id == 9 || id == 2) {
                        dllink = (String) format.get("url");
                        break;
                    }
                }
            } else {
                entries = (LinkedHashMap<String, Object>) formatso;
                final Iterator<Entry<String, Object>> formatiterate = entries.entrySet().iterator();
                while (formatiterate.hasNext()) {
                    final Entry entry = formatiterate.next();
                    entries = (LinkedHashMap<String, Object>) entry.getValue();
                    this.dllink = (String) entries.get("url");
                    if (!StringUtils.isEmpty(dllink) && dllink.startsWith("http")) {
                        break;
                    }
                }
            }
        }
        if (dllink == null) {
            /* Old but still working 2017-02-14 */
            dllink = "http://storage.mais.uol.com.br/" + mediaID + ".mp3?ver=0";
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        final String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            ext = ".mp4";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (con.isOK() && !con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                server_issues = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
