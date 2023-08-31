package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "comunidadmontepinar.es" }, urls = { "https?://(?:www\\.)?comunidadmontepinar\\.es/episodios/\\d+x\\d+" })
public class ComunidadmontepinarEs extends antiDDoSForDecrypt {
    public ComunidadmontepinarEs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "addtl_consent", "1~");
        getPage(param.getCryptedUrl());
        final String m3u8IframeURLNew = br.getRegex("(https?://comunidadmontepinar\\.es/m3u8/t\\d+/[^/]+/nuevo\\.html)").getMatch(0);
        final String m3u8IframeURLOld = br.getRegex("(https?://comunidadmontepinar\\.es/m3u8/t\\d+/[^/]+/index\\.html)").getMatch(0);
        if (m3u8IframeURLNew == null && m3u8IframeURLOld == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String season = new Regex(param.getCryptedUrl(), "/(\\d+)").getMatch(0);
        final String episode = new Regex(param.getCryptedUrl(), "/\\d+x(\\d+)").getMatch(0);
        final String title = "Comunidad Montepinar - La Que Se Avecina" + "_S" + season + "E" + episode;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final boolean findSubtitles;
        if (m3u8IframeURLNew != null) {
            getPage(m3u8IframeURLNew);
            final String[][] qualities = br.getRegex("<source src=\"(https?://[^\"]+)\" type='video/mp4' label='(\\d+p)'[^>]*/>").getMatches();
            for (final String[] qualInfo : qualities) {
                final String url = qualInfo[0];
                final String qualityLabel = qualInfo[1];
                final DownloadLink video = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                video.setFinalFileName(title + "_" + qualityLabel + ".mp4");
                video.setAvailable(true);
                ret.add(video);
            }
            findSubtitles = true;
        } else {
            Browser brc = br.cloneBrowser();
            getPage(brc, "https://api.comunidadmontepinar.es/token");
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final String token = entries.get("token").toString();
            final Browser brc2 = br.cloneBrowser();
            getPage(brc2, m3u8IframeURLOld.replaceFirst("index\\.html$", "static/load.js"));
            String hlsMaster = brc2.getRegex("\\?u=\"\\s*\\+\\s*\"(https?://[^\"]+\\.m3u8)").getMatch(0);
            if (hlsMaster == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            hlsMaster += "?" + token;
            // final String src = br.getRegex("source\\s*src\\s*=\\s*\"([^\"]*\\.m3u8)").getMatch(0);
            // if (src == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            brc = br.cloneBrowser();
            getPage(brc, hlsMaster);
            final ArrayList<DownloadLink> hlsResult = GenericM3u8Decrypter.parseM3U8(this, hlsMaster, brc, param.getCryptedUrl(), null, title);
            if (hlsResult == null || hlsResult.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ret.addAll(hlsResult);
            /* Access that iframe to find subtitles. */
            findSubtitles = false;
            if (findSubtitles) {
                getPage(m3u8IframeURLOld + "?u=" + hlsMaster);
            }
        }
        if (findSubtitles) {
            final String subtitles[][] = br.getRegex("kind\\s*=\\s*\"captions\"\\s*src\\s*=\\s*\"([^\"]*\\.vtt)\"\\s*srclang\\s*=\\s*\"(.*?)\"").getMatches();
            if (subtitles != null && subtitles.length > 0) {
                for (String[] subtitle : subtitles) {
                    final DownloadLink sub = createDownloadlink(DirectHTTP.createURLForThisPlugin(br.getURL(subtitle[0]).toString()));
                    sub.setFinalFileName(title + "_" + subtitle[1] + ".vtt");
                    ret.add(sub);
                }
            }
        }
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(ret);
        }
        return ret;
    }
}
