package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.WallPapersCraftComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wallpaperscraft.com" }, urls = { "https?://(?:www\\.)?wallpaperscraft\\.com/(download/[^/]+_\\d+/\\d+x\\d+|wallpaper/[^/]+_\\d+)" })
public class WallPapersCraftCom extends PluginForDecrypt {
    public WallPapersCraftCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return WallPapersCraftComConfig.class;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String contenturl = param.getCryptedUrl();
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String originalResolution = br.getRegex("Original Resolution:\\s*<a href=.*?>(\\d+x\\d+)").getMatch(0);
        if (originalResolution == null) {
            originalResolution = br.getRegex("Original Resolution\\s*:\\s*(\\d+x\\d+)").getMatch(0);
        }
        final String[] resolutions = br.getRegex("href=\"/download/[^/]+/(\\d+x\\d+)").getColumn(0);
        if (resolutions == null || resolutions.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String main_image_url = br.getRegex("class=\"wallpaper__image\" src=\"(https?://[^\"]+)\"").getMatch(0);
        if (main_image_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String resolution_from_main_image_url = new Regex(main_image_url, "(\\d+x\\d+)\\.[a-z]+$").getMatch(0);
        if (resolution_from_main_image_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String title = br.getRegex("<h1 class=\"gui-h2 gui-heading\"[^>]*>([^<]+)</h1>").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = br._getURL().getPath();
        }
        title = Encoding.htmlDecode(title).trim();
        title = title.replaceFirst("(?i)^Download\\s*", "");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final Set<String> dupes = new HashSet<String>();
        DownloadLink result_original = null;
        DownloadLink result_wishedResolution = null;
        final String resolutionFromURL = new Regex(br.getURL(), "(\\d+x\\d+)$").getMatch(0);
        for (final String resolution : resolutions) {
            if (!dupes.add(resolution)) {
                continue;
            }
            final String imageURL = main_image_url.replaceFirst(resolution_from_main_image_url, resolution);
            final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(imageURL));
            final String name = Plugin.getFileNameFromURL(new URL(imageURL));
            if (name != null) {
                link.setFinalFileName(name);
            }
            link.setReferrerUrl(br.getURL());
            link.setAvailable(true);
            link._setFilePackage(fp);
            ret.add(link);
            if (StringUtils.equalsIgnoreCase(resolution, resolutionFromURL)) {
                result_wishedResolution = link;
            }
            if (StringUtils.equalsIgnoreCase(resolution, originalResolution)) {
                result_original = link;
            }
        }
        logger.info("Total number of resolutions found: " + dupes.size());
        /* Check if we got something that the user typically prefers */
        if (result_wishedResolution != null) {
            logger.info("Returning only resolution from added URL: " + resolutionFromURL);
            ret.clear();
            ret.add(result_wishedResolution);
        } else if (result_original != null && PluginJsonConfig.get(WallPapersCraftComConfig.class).isPreferOriginalResolution()) {
            logger.info("Returning only original resolution: " + originalResolution);
            ret.clear();
            ret.add(result_original);
        }
        return ret;
    }
}
