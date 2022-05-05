package jd.plugins.decrypter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class AbstractPastebinCrawler extends PluginForDecrypt {
    public AbstractPastebinCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    /** Returns unique contentID which is expected to be in the given url. */
    abstract String getFID(final String url);

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* TODO: Implement logic of pastebin settings once available: https://svn.jdownloader.org/issues/90043 */
        this.preProcess(param);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String plaintxt = getPastebinText(this.br);
        if (plaintxt == null) {
            logger.warning("Could not find pastebin textfield");
            return decryptedLinks;
        }
        final PluginForHost sisterPlugin = JDUtilities.getPluginForHost(this.getHost());
        if (sisterPlugin != null) {
            final DownloadLink textfile = getDownloadlinkForHosterplugin(param, plaintxt);
            decryptedLinks.add(textfile);
        }
        /* TODO: Differentiate between URLs that we support (= have plugins for) and those we don't support. */
        final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        logger.info("Found " + links.length + " URLs in plaintext");
        for (final String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }
        return decryptedLinks;
    }

    protected DownloadLink getDownloadlinkForHosterplugin(final CryptedLink link, final String pastebinText) {
        final DownloadLink textfile = this.createDownloadlink(link.getCryptedUrl());
        if (StringUtils.isEmpty(pastebinText)) {
            return null;
        }
        try {
            textfile.setDownloadSize(pastebinText.getBytes("UTF-8").length);
        } catch (final UnsupportedEncodingException ignore) {
            ignore.printStackTrace();
        }
        /* TODO: Set filename according to user preference */
        textfile.setFinalFileName(this.getFID(link.getCryptedUrl()) + ".txt");
        textfile.setAvailable(true);
        return textfile;
    }

    /** Accesses URL, checks if content looks like it's available and handles password/captcha until plaintext is available in HTML. */
    protected abstract void preProcess(final CryptedLink param) throws IOException, PluginException;

    protected abstract String getPastebinText(final Browser br) throws PluginException, IOException;
}
