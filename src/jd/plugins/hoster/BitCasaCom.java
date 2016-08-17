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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitcasa.com" }, urls = { "https://drive\\.bitcasa\\.com/send/[A-Za-z0-9\\-_]+(?:.+\\?file=[A-Za-z0-9\\-_]+)?|https?://l\\.bitcasa\\.com/[A-Za-z0-9\\-]+" }) 
public class BitCasaCom extends PluginForHost {

    public BitCasaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.bitcasa.com/legal";
    }

    private static final String TYPE_NORMAL             = "^https://drive\\.bitcasa\\.com/send/.+$";
    private static final String TYPE_NORMAL_FOLDER_FILE = "^https://drive\\.bitcasa\\.com/send/[A-Za-z0-9\\-_#]+/.+/\\?file=[A-Za-z0-9\\-_]+$";
    private static final String TYPE_SHORT              = "^https?://l\\.bitcasa\\.com/[A-Za-z0-9\\-]+$";

    private String              digest                  = null;
    private String              nonce                   = null;
    private String              payload                 = null;

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String filename = null;
        String filesize = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        /* 302 = happens when accessing old outdated linktypes. */
        if (br.containsHTML("class=\"errorPage\"") || br.getHttpConnection().getResponseCode() == 301 || br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("404\\.html\\'")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!br.getURL().matches(TYPE_NORMAL) && !br.getURL().matches(TYPE_SHORT)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getDownloadURL().matches(TYPE_SHORT)) {
            if (!br.getURL().matches(TYPE_NORMAL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setUrlDownload(br.getURL());
        }
        if (link.getDownloadURL().matches(TYPE_NORMAL_FOLDER_FILE)) {
            /* Single file inside a folder */
            final Regex inforegex = new Regex(link.getDownloadURL(), "^https://drive\\.bitcasa\\.com/send/([A-Za-z0-9\\-_#]+/.+)/\\?file=([A-Za-z0-9\\-_]+)$");
            final String link_id = inforegex.getMatch(0).replace("#", "");
            final String file_id = inforegex.getMatch(1);
            /* limit=300 == standard value from website */
            this.br.getPage("https://drive.bitcasa.com/portal/v2/shares/" + link_id + "/meta?limit=300");
            final String errcode = PluginJSonUtils.getJsonValue(br, "code");
            if ("4002".equals(errcode)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "result/items");
            for (final Object jso : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) jso;
                final String file_id_temp = (String) entries.get("id");
                if (file_id_temp.equals(file_id)) {
                    filename = (String) entries.get("name");
                    filesize = Long.toString(JavaScriptEngineFactory.toLong(entries.get("size"), -1));
                    digest = (String) JavaScriptEngineFactory.walkJson(entries, "application_data/_server/nebula/digest");
                    nonce = (String) JavaScriptEngineFactory.walkJson(entries, "application_data/_server/nebula/nonce");
                    payload = (String) JavaScriptEngineFactory.walkJson(entries, "application_data/_server/nebula/payload");
                    break;
                }
            }

        } else {
            /* Single file */
            this.br.getPage("https://drive.bitcasa.com/portal/v2/shares/" + getFID(link) + "/meta");
            final String errcode = PluginJSonUtils.getJsonValue(br, "code");
            if ("4002".equals(errcode)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJsonValue(br, "share_name");
            filesize = PluginJSonUtils.getJsonValue(br, "share_size");
            digest = PluginJSonUtils.getJsonValue(br, "digest");
            nonce = PluginJSonUtils.getJsonValue(br, "nonce");
            payload = PluginJSonUtils.getJsonValue(br, "payload");
        }
        /* Did not yet get any password protected links for V2. */
        // /* Filename/size is not available for password protected links */
        // if (br.containsHTML("type=\"password\" name=\"password\"")) {
        // link.getLinkStatus().setStatusText("This link is password protected");
        // return AvailableStatus.TRUE;
        // }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        // String passCode = downloadLink.getStringProperty("pass", null);
        requestFileInformation(downloadLink);
        if (digest == null || nonce == null || payload == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Did not yet get any password protected links for V2. */
        // if (br.containsHTML("type=\"password\" name=\"password\"")) {
        // if (passCode == null) {
        // passCode = Plugin.getUserInput("Password?", downloadLink);
        // }
        // if (passCode == null || passCode.equals("")) {
        // passCode = null;
        // logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
        // passCode = null;
        // downloadLink.setProperty("pass", Property.NULL);
        // throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        // }
        // br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
        // if (br.containsHTML("type=\"password\" name=\"password\"")) {
        // passCode = null;
        // logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
        // passCode = null;
        // downloadLink.setProperty("pass", Property.NULL);
        // throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        // }
        // downloadLink.setProperty("pass", passCode);
        // }
        final String dllink = "https://bitcasa.cfsusercontent.io/download/v2/" + digest + "/" + nonce + "/" + payload;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/([^<>\"/]+)$").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}