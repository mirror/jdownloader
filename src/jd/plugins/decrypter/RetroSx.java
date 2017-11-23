package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "retro.sx" }, urls = { "https?://(?:www\\.)?retro\\.sx/music/\\d+" })
public class RetroSx extends PluginForDecrypt {
    public RetroSx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        final String id = new Regex(parameter.toString(), ".+/(\\d+)").getMatch(0);
        final String title = Encoding.htmlDecode(br.getRegex("meta\\s*property='og:title'\\s*content='(.*?)'").getMatch(0));
        final String all_data[][] = br.getRegex("<tr[^~]*?data-rttid='(\\d+)'[^~]*?class='ttDuration'\\s*title='([0-9\\.]+MB)'[^~]*?class='ttName'\\s*title='(.*?)'").getMatches();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        int index = 1;
        for (final String data[] : all_data) {
            final DownloadLink downloadLink = createDownloadlink("https://retro.sx/rest/" + id + "/" + data[0]);
            downloadLink.setDownloadSize(SizeFormatter.getSize(data[1]));
            final String fileName = String.format(Locale.US, "%0" + padLength(all_data.length) + "d", index++) + "-" + data[2] + ".mp3";
            downloadLink.setFinalFileName(fileName);
            downloadLink.setAvailable(true);
            fp.add(downloadLink);
            ret.add(downloadLink);
        }
        return ret;
    }

    private final int padLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else {
            throw new WTFException();
        }
    }
}
