package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class AbstractPastebinCrawler extends PluginForDecrypt {
    public AbstractPastebinCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* TODO: Implement logic of pastebin settings once available: https://svn.jdownloader.org/issues/90043 */
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final DownloadLink textfile = getDownloadlinkForHosterplugin(param);
        checkAvailableStatus(textfile, br);
        decryptedLinks.add(textfile);
        final String plaintxt = getPastebinText(this.br);
        if (plaintxt == null) {
            logger.warning("Pastebin crawler: Could not find pastebin textfield");
            return decryptedLinks;
        }
        /* TODO: Differentiate between URLs that we support and those we don't support. */
        final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links.length > 0) {
            for (final String link : links) {
                decryptedLinks.add(createDownloadlink(link));
            }
        } else {
            logger.info("Found no URLs in pastebin plaintext");
        }
        return decryptedLinks;
    }

    protected DownloadLink getDownloadlinkForHosterplugin(final CryptedLink link) {
        return this.createDownloadlink(link.getCryptedUrl());
    }

    protected abstract void checkAvailableStatus(final DownloadLink link, final Browser br) throws IOException, PluginException;

    protected abstract String getPastebinText(final Browser br) throws PluginException, IOException;
}
