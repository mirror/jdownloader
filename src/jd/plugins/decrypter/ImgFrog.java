package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40452 $", interfaceVersion = 3, names = { "imgfrog.pw" }, urls = { "https?://(files\\.)?imgfrog\\.(?:cf|pw)/(a/([a-zA-Z0-9\\-_]+\\.)?[a-zA-Z0-9]{4,}|a/[a-zA-Z0-9\\-_]+)" })
public class ImgFrog extends antiDDoSForDecrypt {
    public ImgFrog(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (!canHandle(br.getURL())) {
            return ret;
        } else {
            String title = br.getRegex("<strong\\s*data-text\\s*=\\s*\"album-name\"\\s*>\\s*(.*?)\\s*</").getMatch(0);
            if (title == null) {
                title = br.getRegex("<title>\\s*(.*?)\\s*</").getMatch(0);
            }
            int page = 1;
            final HashSet<String> dups = new HashSet<String>();
            while (!isAbort()) {
                final int retSize = ret.size();
                final String items[][] = br.getRegex("<a[^<>]*href\\s*=\\s*\"\\s*(https?://(?:cdn\\.)?imgfrog.(?:cf|pw)/(?:i|f)/[^\"]*)\"[^<>]*>\\s*([^<>]*?)\\s*<").getMatches();
                if (items == null || items.length == 0) {
                    if (page > 1) {
                        break;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                final List<String[]> retryItems = new ArrayList<String[]>();
                for (final String item[] : items) {
                    final DownloadLink link = createDownloadlink(URLEncode.encodeURIComponent(item[0]));
                    if (jd.plugins.hoster.ImgFrog.isFileLink(link)) {
                        // auto name from URL
                    } else if (jd.plugins.hoster.ImgFrog.isImageLink(link)) {
                        if (StringUtils.isEmpty(item[1])) {
                            retryItems.add(item);
                            continue;
                        } else {
                            link.setName(item[1] + ".jpg");
                        }
                    }
                    link.setAvailable(true);
                    if (dups.add(item[0])) {
                        ret.add(link);
                    }
                }
                for (final String item[] : retryItems) {
                    if (dups.add(item[0])) {
                        final DownloadLink link = createDownloadlink(URLEncode.encodeURIComponent(item[0]));
                        link.setAvailable(true);
                        ret.add(link);
                    }
                }
                if (ret.size() != retSize) {
                    getPage("?sort=date_desc&page=" + ++page);
                } else {
                    break;
                }
            }
            if (ret.size() > 1 && title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
            return ret;
        }
    }
}
