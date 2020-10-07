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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtve.es" }, urls = { "https?://(?:www\\.)?rtve\\.es/(?:alacarta/(?!audios/)videos/[\\w\\-]+/[\\w\\-]+/\\d+/?(\\?modl=COMTS)?|infantil/serie/[^/]+/video/[^/]+/\\d+/)" })
public class RtveEs extends PluginForHost {
    /*
     * 2020-03-23: Attention: Do not accept 'audios' URLs in host plugin anymore as they may lead to 'overview' pages with a lot of content
     * (direct URLs) e.g.:
     * https://www.rtve.es/alacarta/audios/documentos-rne/documentos-rne-antoine-saint-exupery-conquista-del-cielo-20-03-20/4822098/
     */
    private static final String TYPE_NORMAL         = "http://(?:www\\.)?rtve\\.es/alacarta/(?:audios|videos)/[\\w\\-]+/[\\w\\-]+/\\d+/?(?:\\?modl=COMTS)?";
    private static final String TYPE_SERIES         = "http://(?:www\\.)?rtve\\.es/infantil/serie/[^/]+/video/[^/]+/\\d+/";
    private String              dllink              = null;
    private String              BLOWFISHKEY         = "eWVMJmRhRDM=";
    private String              dl_not_possible_now = null;
    private boolean             geo_blocked         = false;

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
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/(\\d+)/?.*?$").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    final boolean use_api_for_availablecheck = true;

    @SuppressWarnings("deprecation")
    private AvailableStatus requestVideo(final DownloadLink link) throws IOException, PluginException {
        br.setFollowRedirects(true);
        String filename = null;
        if (use_api_for_availablecheck) {
            final String fid = getFID(link);
            if (fid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setLinkID(this.getHost() + "://" + fid);
            br.getPage(String.format("https://www.rtve.es/api/videos/%s.json", fid));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "page/items/{0}");
            final long publicationDateTimestamp = JavaScriptEngineFactory.toLong(entries.get("publicationDateTimestamp"), 0);
            // final String publicationDate = (String) entries.get("publicationDate");
            filename = (String) entries.get("longTitle");
            if (StringUtils.isEmpty(filename)) {
                /* Fallback */
                filename = fid;
            }
            if (publicationDateTimestamp > 0) {
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                final String formattedDate = formatter.format(new Date(publicationDateTimestamp));
                filename = formattedDate + "_" + filename;
            }
            String description = (String) entries.get("description");
            if (!StringUtils.isEmpty(description) && link.getComment() == null) {
                if (Encoding.isHtmlEntityCoded(description)) {
                    description = Encoding.htmlDecode(description);
                }
                link.setComment(description);
            }
        } else {
            br.getPage(link.getPluginPatternMatcher());
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
            if (link.getDownloadURL().matches(TYPE_SERIES)) {
                final Regex finfo = new Regex(link.getDownloadURL(), "rtve.es/infantil/serie/([^/]+)/video/([^/]+)/");
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
            dl_not_possible_now = br.getRegex(">(Lunes a jueves a las \\d{2}\\.\\d{2} y \\d{2}\\.\\d{2} horas)<").getMatch(0);
            if (dl_not_possible_now != null) {
                link.getLinkStatus().setStatusText("Server error: " + dl_not_possible_now);
                link.setName(filename + ".mp4");
                return AvailableStatus.TRUE;
            }
        }
        link.setName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (use_api_for_availablecheck) {
            br.getPage(link.getPluginPatternMatcher());
        }
        String[] flashVars = br.getRegex("assetID\\s*=\\s*(?:\"|')?(\\d+)_([a-z]{2,3})_(audios|videos)(\\&location=alacarta)?").getRow(0);
        if (flashVars == null || flashVars.length != 4) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* encrypt request query */
        final String mediaType = "audios".equals(flashVars[2]) ? "audio" : "video";
        String getEncData = org.appwork.utils.encoding.Base64.encodeToString(getBlowfish(JDHexUtils.getByteArray(JDHexUtils.getHexString(flashVars[0] + "_default_" + mediaType + "_" + flashVars[1])), false), false);
        getEncData = getEncData.replaceAll("/", "_");
        Browser enc = br.cloneBrowser();
        /* 2020-07-28: Higher resolutions are "hidden" in their thumbnail. */
        // br.getPage("http://www.rtve.es/ztnr/movil/thumbnail/banebdyede/videos/" + "<fileID>" + ".png");
        enc.getPage("https://ztnr.rtve.es/ztnr/res/" + getEncData);
        /* Check for empty page */
        if (enc.toString().length() <= 22) {
            logger.info("Empty page --> Content offline?");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* decrypt response body */
        dllink = getLink(JDHexUtils.toString(JDHexUtils.getHexString(getBlowfish(org.appwork.utils.encoding.Base64.decode(enc.toString()), true))));
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (geo_blocked) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
        } else if (dl_not_possible_now != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: " + dl_not_possible_now);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink != null && dllink.matches("https?://rtve-hlsvod\\.secure\\.footprint\\.net/resources/TE_NGVA/mp4/.*\\.mp4/playlist\\.m3u8")) {
            /* 2020-07-31: Some content can be downloaded via http instead of via HLS --> Prefer to do that */
            logger.info("Try to download http stream");
            final String httlpDownload = dllink.replace("/playlist.m3u8", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, httlpDownload, true, 0);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This content is not available in your country", 3 * 60 * 60 * 1000l);
            }
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                dl.startDownload();
                return;
            } else {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This content is not available in your country", 3 * 60 * 60 * 1000l);
                }
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                logger.info("HTTP download failed --> Fallback to HLS download");
            }
        }
        logger.info("HLS download");
        /* HLS download */
        this.br.getPage(dllink);
        if (this.br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This content is not available in your country", 3 * 60 * 60 * 1000l);
        }
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        geo_blocked = false;
        requestVideo(downloadLink);
        setBrowserExclusive();
        /* 2019-02-21: Removed this as we need to check the downloadurl always to detect GEO-blocked conditions!! */
        // if (dl_now_now != null) {
        // return AvailableStatus.TRUE;
        // }
        if (dllink != null && !dllink.contains("/manifest")) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(dllink);
                if (con.getResponseCode() == 403) {
                    geo_blocked = true;
                } else if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                    br.getHttpConnection().disconnect();
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
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