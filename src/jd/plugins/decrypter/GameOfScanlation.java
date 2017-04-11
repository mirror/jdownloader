package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gameofscanlation.moe" }, urls = { "https?://(www\\.)?gameofscanlation\\.moe/(projects/[^/]+/|threads/[^/]+)chapter(-?\\d+(-\\d+)?)?\\.\\d+" })
public class GameOfScanlation extends PluginForDecrypt {

    public GameOfScanlation(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final int padLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else if (size < 10000) {
            return 4;
        } else if (size < 100000) {
            return 5;
        } else if (size < 1000000) {
            return 6;
        } else if (size < 10000000) {
            return 7;
        } else {
            return 8;// hello djmakinera
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String title = Encoding.htmlOnlyDecode(br.getRegex("<title>\\s*(.*?)(\\s*\\|\\s*Game of Scanlation\\s*)?</title>").getMatch(0));
        final String pages[] = br.getRegex("src=\"(https?://gameofscanlation.moe/data/attachment-files/.*?)\"").getColumn(0);
        if (pages != null) {
            int index = 0;
            final int padLength = padLength(pages.length);
            for (final String page : pages) {
                index++;
                String url = page;
                if (url.contains("pagespeed")) {
                    url = url.replaceFirst("\\.pagespeed.+$", "");
                    url = url.replaceFirst("/x", "/");
                }
                final String ext = getFileNameExtensionFromURL(url);
                String pageIndex = String.valueOf(index);
                pageIndex = String.format(Locale.US, "%0" + padLength + "d", Integer.parseInt(pageIndex));
                final DownloadLink link = createDownloadlink("directhttp://" + url);
                link.setAvailable(true);
                final String fileName = title + "_" + pageIndex + ext;
                link.setProperty("fixName", fileName);
                link.setForcedFileName(fileName);
                ret.add(link);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
