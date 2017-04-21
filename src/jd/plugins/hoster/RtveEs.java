//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDHexUtils;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtve.es" }, urls = { "http://(www\\.)?rtve\\.es/(?:alacarta/(audios|videos)/[\\w\\-]+/[\\w\\-]+/\\d+/?(\\?modl=COMTS)?|infantil/serie/[^/]+/video/[^/]+/\\d+/)|https?://rio2016\\.rtve\\.es/video/article/[a-z0-9\\-]+.html" })
public class RtveEs extends PluginForHost {

    private static final String TYPE_NORMAL  = "http://(?:www\\.)?rtve\\.es/alacarta/(?:audios|videos)/[\\w\\-]+/[\\w\\-]+/\\d+/?(?:\\?modl=COMTS)?";
    private static final String TYPE_SERIES  = "http://(?:www\\.)?rtve\\.es/infantil/serie/[^/]+/video/[^/]+/\\d+/";
    private static final String TYPE_RIO2016 = "https?://rio2016\\.rtve\\.es/video/article/[a-z0-9\\-]+.html";

    private String              dllink       = null;
    private String              BLOWFISHKEY  = "eWVMJmRhRDM=";
    private String              dl_now_now   = null;
    private boolean             geo_blocked  = false;

    public RtveEs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.rtve.es/comunes/aviso_legal.html";
    }

    private byte[] getBlowfish(byte[] value, boolean decrypt) {
        try {
            Cipher c = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(org.appwork.utils.encoding.Base64.decode(BLOWFISHKEY), "Blowfish");
            if (decrypt) {
                c.init(Cipher.DECRYPT_MODE, keySpec);
            } else {
                c.init(Cipher.ENCRYPT_MODE, keySpec);
            }
            byte[] result = c.doFinal(value);
            return result;
        } catch (Throwable e) {
            return null;
        }
    }

    private String getLink(String xml) {
        if (xml == null) {
            return null;
        }
        ArrayList<String> dllinks = new ArrayList<String>(Arrays.asList(new Regex(xml, "provider=\'[\\w\\-]+\'>([^<]+)").getColumn(0)));
        // Collections.shuffle(dllinks);
        for (String dllink : dllinks) {
            if (dllink.startsWith("rtmp") || dllink.endsWith("type=.smil")) {
                continue;
            }
            return dllink;
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestVideo(downloadLink);
        if (geo_blocked) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
        } else if (dl_now_now != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: " + dl_now_now);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("/manifest")) {
            /* HLS download */
            this.br.getPage(dllink);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else {
            /* HTTP download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        geo_blocked = false;
        requestVideo(downloadLink);
        setBrowserExclusive();
        if (dl_now_now != null) {
            return AvailableStatus.TRUE;
        }
        if (!dllink.contains("/manifest")) {
            try {
                if (!br.openGetConnection(dllink).getContentType().contains("html")) {
                    downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                    br.getHttpConnection().disconnect();
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                if (br.getHttpConnection() != null) {
                    br.getHttpConnection().disconnect();
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    private AvailableStatus requestVideo(final DownloadLink downloadLink) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("La página solicitada no está disponible por haber cambiado la dirección \\(URL\\) o no existir\\.|id=\"errorndispo\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("No hay vídeos o audios para la búsqueda efectuada")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("/alacarta20/i/imgError/video\\.png")) {
            this.geo_blocked = true;
            return AvailableStatus.TRUE;
        }
        String filename = null;
        if (downloadLink.getDownloadURL().matches(TYPE_RIO2016)) {
            String videoid = PluginJSonUtils.getJsonValue(this.br, "VideoID");
            if (videoid == null) {
                videoid = PluginJSonUtils.getJsonValue(this.br, "s.forgeid");
            }
            if (videoid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage("/VideoData/" + videoid + ".xml");
            filename = this.br.getRegex(" <title><\\!\\[CDATA\\[([^<>\"]+)\\]\\]></title>").getMatch(0);
            if (filename == null) {
                /* Fallback */
                filename = videoid;
            }
            dllink = this.br.getRegex("<videoSource format=\"HLS\" offset=\"\\d+:\\d+:\\d+\">[+\t\n\r ]*?<uri>(http[^<>\"]*?)</uri>").getMatch(0);
        } else {
            if (downloadLink.getDownloadURL().matches(TYPE_SERIES)) {
                final Regex finfo = new Regex(downloadLink.getDownloadURL(), "rtve.es/infantil/serie/([^/]+)/video/([^/]+)/");
                filename = finfo.getMatch(0) + " - " + finfo.getMatch(1);
            } else {
                filename = br.getRegex("<h1><span title=\"([^\"]+)").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"last\">([^<]+)").getMatch(0);
                }
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename.trim());
            dl_now_now = br.getRegex(">(Lunes a jueves a las \\d{2}\\.\\d{2} y \\d{2}\\.\\d{2} horas)<").getMatch(0);
            if (dl_now_now != null) {
                downloadLink.getLinkStatus().setStatusText("Server error: " + dl_now_now);
                downloadLink.setName(filename + ".mp4");
                return AvailableStatus.TRUE;
            }

            String[] flashVars = br.getRegex("assetID=(\\d+)_([a-z]{2,3})_(audios|videos)\\&location=alacarta").getRow(0);
            if (flashVars == null || flashVars.length != 3) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* encrypt request query */
            String getEncData = org.appwork.utils.encoding.Base64.encodeToString(getBlowfish(JDHexUtils.getByteArray(JDHexUtils.getHexString(flashVars[0] + "_default_" + ("audios".equals(flashVars[2]) ? "audio" : "video") + "_" + flashVars[1])), false), false);
            getEncData = getEncData.replaceAll("/", "_");
            Browser enc = br.cloneBrowser();
            enc.getPage("http://ztnr.rtve.es/ztnr/res/" + getEncData);
            /* Check for empty page */
            if (enc.toString().length() <= 22) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* decrypt response body */
            dllink = getLink(JDHexUtils.toString(JDHexUtils.getHexString(getBlowfish(org.appwork.utils.encoding.Base64.decode(enc.toString()), true))));
        }

        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        downloadLink.setName(filename + ext);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}