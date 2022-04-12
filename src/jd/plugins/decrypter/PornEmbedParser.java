package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.DecrypterArrayList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class PornEmbedParser extends antiDDoSForDecrypt {
    public PornEmbedParser(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * find the first within findEmbedUrl method from default this.br browser.
     *
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl() throws Exception {
        return findEmbedUrls(br, null, false);
    }

    /**
     * finds and returns only the first
     *
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl(final String title) throws Exception {
        return findEmbedUrls(br, title, false);
    }

    /**
     * finds and returns only the first from imported browser
     *
     * @param ibr
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl(final Browser ibr, final String title) throws Exception {
        return findEmbedUrls(ibr, title, false);
    }

    /**
     * finds all embed urls from this.br
     *
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrls() throws Exception {
        return findEmbedUrls(br, null, true);
    }

    /**
     * finds all embed urls from this.br, with provided title
     *
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrls(final String title) throws Exception {
        return findEmbedUrls(br, title, true);
    }

    /**
     * finds all embed urls from imported browser, with provided title
     *
     * @param ibr
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrls(final Browser ibr, final String title) throws Exception {
        return findEmbedUrls(ibr, title, true);
    }

    /**
     * true = skip results with same domain of source URL. </br>
     * Warning: Disabling this may have unwanted side-effects as a lot of video sites will show recommendations/similar videos on single
     * video pages which would then also be grabbed!
     */
    protected boolean skipSamePluginResults() {
        return true;
    }

    /**
     * porn_plugin
     *
     *
     * This method is designed to find porn website urls in html code.
     *
     * @param pluginBrowser
     *            : Browser containing the sourceurl with the embed urls/codes *
     *
     *
     * @param title
     *            : Title to be used in case a directhttp url is found. If the title is not given, directhttp urls will never be decrypted.
     * @throws Exception
     *
     *
     */
    public final ArrayList<DownloadLink> findEmbedUrls(final Browser br, String title, final boolean processAll) throws Exception {
        final DecrypterArrayList<DownloadLink> decryptedLinks = new DecrypterArrayList<DownloadLink>() {
            /**
             *
             */
            private static final long serialVersionUID = 4665325651021721965L;

            @Override
            public boolean add(final String link) {
                return add(link, br);
            }

            @Override
            public boolean add(String link, final Browser br) {
                if (link.startsWith("//")) {
                    link = "https:" + link;
                }
                final String url = Request.getLocation(link, br.getRequest());
                return add(createDownloadlink(url));
            }
        };
        /* Cleanup/Improve title */
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        logger.info("PornEmbedParser is being executed...");
        String externID = null;
        externID = br.getRegex("(https?://(?:www\\.)?camhub\\.(?:world|cc)/embed/\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = this.createDownloadlink(externID);
            /* Filename is good to have but not necessarily required, */
            if (title != null) {
                title += ".mp4";
                /* 2020-09-29: Special: Enforce this filename because host-plugin will not be able to find a meaningful filename! */
                dl.setForcedFileName(title);
            }
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        /************************************************************************************************************/
        // Now check for all existant URLs if they're supported by any plugin tagged as porn plugin
        /************************************************************************************************************/
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        final int before = decryptedLinks.size();
        for (final String url : urls) {
            final List<LazyCrawlerPlugin> nextLazyCrawlerPlugins = findNextLazyCrawlerPlugins(url, LazyPlugin.FEATURE.XXX);
            if (nextLazyCrawlerPlugins.size() > 0) {
                decryptedLinks.addAll(convert(br, title, url, nextLazyCrawlerPlugins));
            }
            final List<LazyHostPlugin> nextLazyHostPlugins = findNextLazyHostPlugins(url, LazyPlugin.FEATURE.XXX);
            if (nextLazyHostPlugins.size() > 0) {
                decryptedLinks.addAll(convert(br, title, url, nextLazyHostPlugins));
            }
        }
        final int results = decryptedLinks.size() - before;
        if (results > 0 && !processAll) {
            return decryptedLinks;
        }
        /************************************************************************************************************/
        // filename needed for all IDs below
        /************************************************************************************************************/
        if (title == null) {
            if (!processAll) {
                return decryptedLinks;
            }
        }
        return decryptedLinks;
    }

    protected List<DownloadLink> convert(final Browser br, final String title, final String url, List<? extends LazyPlugin> lazyPlugins) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (lazyPlugins.size() == 0) {
            return ret;
        }
        if (this.canHandle(url) && skipSamePluginResults()) {
            return ret;
        }
        final DownloadLink dl = createDownloadlink(Request.getLocation(url, br.getRequest()));
        if (lazyPlugins.size() == 1) {
            // TODO: better way for this
            if ("mydaddy.cc".equals(lazyPlugins.get(0).getDisplayName())) {
                if (title != null) {
                    dl.setProperty("decryptertitle", title);
                }
            }
        }
        ret.add(dl);
        return ret;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public DownloadLink createDownloadlink(String url) {
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        return super.createDownloadlink(url);
    }
}
