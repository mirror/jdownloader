package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.storage.flexijson.FlexiJSONParser;
import org.appwork.storage.flexijson.ParsingError;
import org.appwork.storage.flexijson.mapper.FlexiJSonMapper;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "genshin.hoyoverse.com" }, urls = { "https?://genshin.hoyoverse.com/(?:[a-z]{2}/)?manga/detail/\\d+" })
public class GenshinManga extends PluginForDecrypt {
    public GenshinManga(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String detailID = new Regex(parameter.getCryptedUrl(), "/detail/(\\d+)").getMatch(0);
        String stringArray = br.getRegex("\\}\\s*\\}\\s*\\((.*?)\\)\\)\\s*;").getMatch(0);
        stringArray = stringArray.replaceAll("(Array\\(\\d+\\))", "\"IGNORE\"");
        final List<Object> objectArray = restoreFromString("[" + stringArray + "]", TypeRef.LIST);
        final String pages = br.getRegex("ext\\s*:\\s*\\[(\\s*\\{arrtName.*?\\]\\s*\\})").getMatch(0);
        final String indexString = br.getRegex("function\\((a,.*?)\\)").getMatch(0);
        final List<String> indexArray = Arrays.asList(indexString.split(","));
        final FlexiJSONParser parser = new FlexiJSONParser(pages).setIgnoreIssues(new HashSet<ParsingError>(Arrays.asList(new ParsingError[] { ParsingError.ERROR_STRING_VALUE_WITHOUT_QUOTES, ParsingError.ERROR_STRING_TOKEN_WITHOUT_QUOTES, ParsingError.ERROR_KEY_WITHOUT_QUOTES })));
        parser.setDebug(new StringBuilder());
        final Map<String, Object> map = new FlexiJSonMapper().jsonToObject(parser.parse(), TypeRef.MAP);
        final String mangaTitle = br.getRegex("<title[^>]*>\\s*(Genshin Impact.*?)\\s*</title>").getMatch(0);
        if (mangaTitle == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String chapterTitle = br.getRegex("id\\s*:\"" + detailID + "\"\\s*,\\s*title\\s*:\\s*\"(.*?)\"").getMatch(0);
        if (chapterTitle == null) {
            final String id = br.getRegex("\\{\\s*id\\s*:\\s*([^\" ,]+)").getMatch(0);
            final String title = br.getRegex("\\{\\s*id\\s*:\\s*[^\" ,]+\\s*,\\s*title\\s*:\\s*([^\" ,\\}]+)").getMatch(0);
            final String id_mapped = StringUtils.valueOfOrNull(objectArray.get(indexArray.indexOf((id))));
            if (StringUtils.equals(detailID, id_mapped)) {
                chapterTitle = StringUtils.valueOfOrNull(objectArray.get(indexArray.indexOf((title))));
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int pageIndex = 1;
        final List<Map<String, Object>> pageEntries = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(map, "value");
        final int numLength = String.valueOf(pageEntries.size()).length();
        for (Map<String, Object> page : pageEntries) {
            final Object url = page.get("url");
            final String imageURL;
            if (url instanceof String) {
                String s = (String) url;
                if (StringUtils.startsWithCaseInsensitive(s, "http")) {
                    imageURL = s;
                } else {
                    final int index = indexArray.indexOf(s);
                    if (index == -1) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Index missing:" + s);
                    }
                    imageURL = StringUtils.valueOfOrNull(objectArray.get(index));
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unknown urlType:" + url);
            }
            if (imageURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (!StringUtils.startsWithCaseInsensitive(imageURL, "http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "invalid imageURL:" + imageURL);
            }
            final DownloadLink downloadLink = createDownloadlink("directhttp://" + imageURL, false);
            final String name = String.format(Locale.US, "%0" + numLength + "d", pageIndex++) + getFileNameExtensionFromURL(imageURL, ".jpg");
            downloadLink.setAvailable(true);
            downloadLink.setFinalFileName(name);
            downloadLink.setProperty(DirectHTTP.FIXNAME, name);
            ret.add(downloadLink);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (chapterTitle != null) {
            fp.setName(mangaTitle + "-" + chapterTitle);
        } else {
            fp.setName(mangaTitle);
        }
        fp.addLinks(ret);
        return ret;
    }
}
