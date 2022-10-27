package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bestporncomix.com" }, urls = { "https?://(?:www\\.)?(?:porncomix\\.info|bestporncomix\\.com)/gallery/([a-z0-9\\-]+)/?" })
public class PornComixInfo extends PluginForDecrypt {
    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String addedurl = param.getCryptedUrl();
        /* 2020-11-13: Main domain has changed from porncomix.info --> bestporncomix.com */
        addedurl = addedurl.replace(Browser.getHost(addedurl) + "/", this.getHost() + "/");
        br.getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urltitle = new Regex(addedurl, this.getSupportedLinks()).getMatch(0);
        String postTitle = br.getRegex("(?i)<title>([^<>\"]+) - \\| 18\\+ Porn Comics</title>").getMatch(0);
        if (StringUtils.isEmpty(postTitle)) {
            /* Fallback */
            postTitle = urltitle.replace("-", " ");
        }
        String[] images = br.getRegex("<figure[^>]*class='dgwt-jg-item'[^>]*><a href=\\'([^<>\"\\']+)'").getColumn(0);
        if (images != null) {
            for (final String imageurl : images) {
                final DownloadLink link = createDownloadlink(imageurl);
                link.setAvailable(true);
                link.setContainerUrl(addedurl);
                ret.add(link);
            }
        }
        if (postTitle != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(postTitle));
            fp.addLinks(ret);
        }
        return ret;
    }
}
