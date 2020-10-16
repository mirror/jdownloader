package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fapcat.com" }, urls = { "https?://(?:www\\.)?fapcat\\.com/albums/(?!.*?sites).+" })
public class FapCatComGallery extends SimpleHtmlBasedGalleryPlugin {
    public FapCatComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected void populateDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, String url) throws PluginException {
        final String[] links = determineLinks(url);
        final int padLength = (int) Math.log10(links.length) + 1;
        int index = 1;
        for (final String link : links) {
            if (!isAbort()) {
                // request to "link" is responded with 302 and the actual URL of the picture (in location header)
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                try {
                    brc.getPage(link);
                } catch (IOException e) {
                    throw new PluginException(LinkStatus.ERROR_RETRY, null, e);
                }
                String location = brc.getRedirectLocation();
                if (StringUtils.isEmpty(location)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    decryptedLinks.add(buildDownloadLink(padLength, index, location));
                }
                index++;
            }
        }
    }

    protected String[] determineLinks(String url) throws PluginException {
        final String[] links = br.getRegex("href\\s*=\\s*(?:\"|')([^\"']+\\.jpg/)(?:\"|')").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("found 0 images for " + url);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return links;
    }
}
