package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porncomixinfo.net" }, urls = { "https?(?:www\\.)?://porncomixinfo\\.(?:net|com)/chapter/([a-z0-9\\-]+/[a-z0-9\\-]+)/?" })
public class PorncomixinfoNet extends PluginForDecrypt {
    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String addedurl = param.getCryptedUrl();
        br.getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urltitle = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        String postTitle = br.getRegex("<title>([^<>\"]+) - Porn Comics</title>").getMatch(0);
        if (StringUtils.isEmpty(postTitle)) {
            /* Fallback */
            postTitle = urltitle;
        }
        /* Similar to hentairead.com */
        String imagesText = br.getRegex("chapter_preloaded_images = \\[(.*?)\\]").getMatch(0);
        imagesText = PluginJSonUtils.unescape(imagesText);
        imagesText = imagesText.replace("\"", "");
        String[] images = imagesText.split(",");
        for (final String imageurl : images) {
            /* 2020-11-13: Not needed anymore */
            // imageurl = Encoding.htmlDecode(imageurl).replaceFirst("(-\\d+x\\d+)\\.(jpe?g|gif|png)$", ".$2");
            final DownloadLink link = createDownloadlink(imageurl);
            link.setAvailable(true);
            link.setContainerUrl(param.getCryptedUrl());
            ret.add(link);
        }
        if (postTitle != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(postTitle));
            fp.addLinks(ret);
        }
        return ret;
    }
}
