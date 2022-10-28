package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "retro.sx" }, urls = { "https?://(?:www\\.)?retro\\.sx/music/\\d+" })
public class RetroSxCrawler extends PluginForDecrypt {
    public RetroSxCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String albumID = new Regex(param.getCryptedUrl(), ".+/(\\d+)").getMatch(0);
        final String albumTitle = HTMLSearch.searchMetaTag(br, "og:title");
        final String all_data[][] = br.getRegex("(?i)<tr[^~]*?data-rttid='(\\d+)'[^~]*?class='ttDuration'\\s*title='([0-9\\.]+MB)'[^~]*?class='ttName'\\s*title='(.*?)'").getMatches();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(albumTitle).trim());
        int index = 1;
        final int padLength = StringUtils.getPadLength(all_data.length);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final String data[] : all_data) {
            final String trackID = data[0];
            final String trackTitle = Encoding.htmlDecode(data[2]).trim();
            final DownloadLink music = createDownloadlink("https://" + this.getHost() + "/rest/" + albumID + "/" + trackID);
            music.setContentUrl("https://" + this.getHost() + "/music/" + albumID + "#" + trackID);
            music.setDownloadSize(SizeFormatter.getSize(data[1]));
            music.setFinalFileName(StringUtils.formatByPadLength(padLength, index) + "-" + trackTitle + ".mp3");
            music.setAvailable(true);
            fp.add(music);
            ret.add(music);
            index++;
        }
        return ret;
    }
}
