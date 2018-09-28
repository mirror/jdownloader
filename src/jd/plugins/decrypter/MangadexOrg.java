package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangadex.org" }, urls = { "https?://(www\\.)?mangadex\\.org/chapter/\\d+" })
public class MangadexOrg extends antiDDoSForDecrypt {
    public MangadexOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String chapterID = new Regex(parameter, "/chapter/(\\d+)").getMatch(0);
        getPage("https://mangadex.org/api/?id=" + chapterID + "&type=chapter");
        final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String hash = (String) map.get("hash");
        final String server = (String) map.get("server");
        if (StringUtils.isEmpty(server)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (StringUtils.isEmpty(hash)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String mangaID = map.get("manga_id") != null ? String.valueOf(map.get("manga_id")) : null;
        final StringBuilder sb = new StringBuilder();
        final String mangaTitle;
        if (StringUtils.isNotEmpty(mangaID)) {
            getPage("https://mangadex.org/api/?id=" + mangaID + "&type=manga");
            final Map<String, Object> manga = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            mangaTitle = (String) ((Map<String, Object>) manga.get("manga")).get("title");
            if (StringUtils.isNotEmpty(mangaTitle)) {
                sb.append(mangaTitle);
            }
        } else {
            mangaTitle = null;
        }
        final String volume = (String) map.get("volume");
        final String chapter = (String) map.get("chapter");
        String title = (String) map.get("title");
        final FilePackage fp = FilePackage.getInstance();
        if (StringUtils.isNotEmpty(volume)) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append("Vol_");
            sb.append(volume);
        }
        if (StringUtils.isNotEmpty(chapter)) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append("Ch_");
            sb.append(chapter);
        }
        if (StringUtils.isNotEmpty(title)) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(title);
        } else if (StringUtils.isNotEmpty(mangaTitle)) {
            title = mangaTitle;
        } else {
            title = "";
        }
        fp.setName(sb.toString());
        final List<Object> page_array = (List<Object>) map.get("page_array");
        if (page_array == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int pageIndex = 0;
        for (Object page : page_array) {
            pageIndex++;
            if (page == null) {
                continue;
            } else if (page instanceof String) {
                final String url = br.getURL(server + hash + "/" + page).toString();
                final DownloadLink link = createDownloadlink("directhttp://" + url);
                link.setProperty("refURL", parameter.getCryptedUrl());
                link.setAvailable(true);
                if (title != null) {
                    link.setFinalFileName(title + "-Page_" + pageIndex + Plugin.getFileNameExtensionFromURL(url));
                }
                link.setContentUrl(parameter.getCryptedUrl() + "/" + pageIndex);
                link.setLinkID(getHost() + "://" + chapterID + "/" + page);
                fp.add(link);
                distribute(link);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }
}
