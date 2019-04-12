package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy" }, urls = { "https?://(www\\.)?(yourporn\\.sexy|sxyprn\\.com)/[^/]*?\\.html(\\?page=\\d+)?" })
public class YourPornSexy extends antiDDoSForDecrypt {
    public YourPornSexy(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl());
        final String[][] hits = br.getRegex("<a href=(?:\"|')(/post/[a-fA-F0-9]{13}\\.html)\\?sk=.*?title='(.*?)'").getMatches();
        for (final String[] hit : hits) {
            final DownloadLink link = createDownloadlink(br.getURL(hit[0]).toString());
            link.setName(hit[1].trim() + ".mp4");
            link.setAvailable(true);
            ret.add(link);
        }
        final String pages[] = br.getRegex("<a href=(?:\"|')(/[^/]*?\\.html\\?page=\\d+)").getColumn(0);
        for (final String page : pages) {
            final String url = br.getURL(page).toString();
            if (!StringUtils.equals(parameter.getCryptedUrl(), url)) {
                final DownloadLink link = createDownloadlink(url);
                ret.add(link);
            }
        }
        final String packageName = new Regex(parameter.getCryptedUrl(), "/([^/]*?)\\.html").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(packageName);
        fp.addLinks(ret);
        return ret;
    }
}
