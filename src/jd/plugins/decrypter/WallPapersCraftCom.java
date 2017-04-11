package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.config.WallPapersCraftComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wallpaperscraft.com" }, urls = { "https?://(?:www\\.)?wallpaperscraft\\.com/download/[^/]+_\\d+/\\d+x\\d+" })
public class WallPapersCraftCom extends PluginForDecrypt {

    public WallPapersCraftCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return WallPapersCraftComConfig.class;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String resolution = new Regex(parameter.getCryptedUrl(), "(\\d+x\\d+)$").getMatch(0);
        final String originalResolution = br.getRegex("Original Resolution:\\s*<a href=.*?>(\\d+x\\d+)").getMatch(0);
        final String url = br.getRegex("\"downloads_big\">\\s*<img\\s*src=\"(.*?_\\d+_\\d+x\\d+\\..*?)\"").getMatch(0);
        if (url != null) {
            final URL imageURL;
            if (originalResolution != null && PluginJsonConfig.get(WallPapersCraftComConfig.class).isPreferOriginalResolution()) {
                imageURL = br.getURL(url.replace(resolution, originalResolution));
            } else {
                imageURL = br.getURL(url);
            }
            final DownloadLink downloadLink = createDownloadlink("directhttp://" + imageURL.toString());
            final String name = Plugin.getFileNameFromURL(imageURL);
            downloadLink.setForcedFileName(name);
            downloadLink.setProperty("Referer", br.getURL());
            downloadLink.setAvailable(true);
            ret.add(downloadLink);
        }
        return ret;
    }

}
