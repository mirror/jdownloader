package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 33293 $", interfaceVersion = 3, names = { "br.de" }, urls = { "https?://(www\\.)?br\\.de/radio/bayern2/[^<>\"]+100\\.html" }, flags = { 32 })
public class Br2RadioDe extends PluginForDecrypt {

    public Br2RadioDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.getPage(parameter.getCryptedUrl());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String dataURL = br.getRegex("dataURL:'(/radio/bayern2/.*xml)'").getMatch(0);
        if (dataURL != null) {
            br.getPage(dataURL);
            final String assets[][] = br.getRegex("<asset type=\"(STANDARD|MOBILEAAC|STANDARDAAC)\">.*?<downloadurl>(https?://.*?)</downloadurl>.*?<size>(\\d+)</size>").getMatches();
            final String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
            for (String asset[] : assets) {
                final DownloadLink link = createDownloadlink(asset[1]);
                link.setAvailable(true);
                link.setVerifiedFileSize(Long.parseLong(asset[2]));
                if (title != null) {
                    link.setFinalFileName(encodeUnicode(title) + "_" + asset[0] + Plugin.getFileNameExtensionFromURL(asset[1]));
                }
                ret.add(link);
            }
            if (title != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(encodeUnicode(title));
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    private static String encodeUnicode(final String input) {
        if (input == null) {
            return null;
        } else {
            String output = input;
            output = output.replace(":", ";");
            output = output.replace("|", "¦");
            output = output.replace("<", "[");
            output = output.replace(">", "]");
            output = output.replace("/", "⁄");
            output = output.replace("\\", "∖");
            output = output.replace("*", "#");
            output = output.replace("?", "¿");
            output = output.replace("!", "¡");
            output = output.replace("\"", "'");
            return output;
        }
    }

}
