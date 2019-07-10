package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 38918 $", interfaceVersion = 2, names = { "comunidadmontepinar.es" }, urls = { "https?://(www\\.)?comunidadmontepinar\\.es/episodios/\\d+x\\d+" })
public class ComunidadmontepinarEs extends antiDDoSForDecrypt {
    public ComunidadmontepinarEs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl());
        final String m3u8 = br.getRegex("(https?://comunidadmontepinar\\.es/m3u8/t\\d+/\\d+/index\\.html)").getMatch(0);
        if (m3u8 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(m3u8);
        final String src = br.getRegex("source\\s*src\\s*=\\s*\"([^\"]*\\.m3u8)").getMatch(0);
        if (src == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            final Browser brc = br.cloneBrowser();
            getPage(brc, src);
            final String season = new Regex(parameter.getCryptedUrl(), "/(\\d+)").getMatch(0);
            final String episode = new Regex(parameter.getCryptedUrl(), "/\\d+x(\\d+)").getMatch(0);
            final String title = "Comunidad Montepinar - La Que Se Avecina" + "_S" + season + "E" + episode;
            final ArrayList<DownloadLink> ret = GenericM3u8Decrypter.parseM3U8(this, src, brc, parameter.getCryptedUrl(), null, null, title);
            decryptedLinks.addAll(ret);
            final String subtitles[][] = br.getRegex("kind\\s*=\\s*\"captions\"\\s*src\\s*=\\s*\"([^\"]*\\.vtt)\"\\s*srclang\\s*=\\s*\"(.*?)\"").getMatches();
            if (subtitles != null && subtitles.length > 0) {
                for (String[] subtitle : subtitles) {
                    final DownloadLink sub = createDownloadlink("directhttp://" + br.getURL(subtitle[0]).toString());
                    sub.setFinalFileName(title + "_" + subtitle[1] + ".vtt");
                    decryptedLinks.add(sub);
                }
            }
            if (title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
    }
}
