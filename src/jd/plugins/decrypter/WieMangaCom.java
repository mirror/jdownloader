package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WieMangaCom extends PluginForDecrypt {
    public WieMangaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wiemanga.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/chapter/[^/]+/\\d+/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String manga = br.getRegex("(?i)href\\s*=\\s*\"https?://(?:www.)?wiemanga\\.com/manga/.*?\\.html\".*?>\\s*(.*?)\\s*<").getMatch(0);
        final String chapter = br.getRegex("(?i)href\\s*=\\s*\"https?://(?:www.)?wiemanga\\.com/chapter/.*?>[^<]*(?:Kapitel|Chapter) (\\d+)").getMatch(0);
        final String title = manga + "-Kapitel_" + chapter;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final String chapterID = new Regex(param.getCryptedUrl(), "/(\\d+)/?$").getMatch(0);
        final String[] pageurls = br.getRegex("<option value=\"[^\"]*(/chapter/[^/]+/" + chapterID + "-\\d+\\.html)\"").getColumn(0);
        int page = 1;
        final int padLength = StringUtils.getPadLength(pageurls.length);
        final ArrayList<String> pageurlsDeduped = new ArrayList<String>();
        for (final String pageurl : pageurls) {
            if (!pageurlsDeduped.contains(pageurl)) {
                pageurlsDeduped.add(pageurl);
            }
        }
        for (final String pageurl : pageurlsDeduped) {
            logger.info("Crawling page " + page + "/" + pageurls.length);
            /* Start = Page 1 so we do not have to access it again */
            if (page > 1) {
                br.getPage(pageurl);
            }
            final String image = br.getRegex("(?i)img\\s*id='comicpic'[^>]*src=.(https?://[^'\"]+)").getMatch(0);
            if (image == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink link = createDownloadlink("directhttp://" + image);
            link.setAvailable(true);
            if (title != null) {
                link.setFinalFileName(title + "-Seite_" + StringUtils.formatByPadLength(padLength, page) + Plugin.getFileNameExtensionFromURL(image));
            }
            fp.add(link);
            distribute(link);
            page++;
        }
        return ret;
    }
}
