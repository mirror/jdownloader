package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.DecrypterArrayList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class PornEmbedParser extends PluginForDecrypt {
    public PornEmbedParser(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    protected Browser prepareBrowser(final Browser br) {
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.addAll(this.preProcessCryptedLink(param));
        if (!ret.isEmpty()) {
            return ret;
        } else {
            ret.addAll(findEmbedUrls());
            return ret;
        }
    }

    /** Place dead domains here so crawler will change URLs containing dead domains in an attempt to make them work. */
    protected ArrayList<String> getDeadDomains() {
        return null;
    }

    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return null;
    }

    /** Returns true if content is offline according to html code or http response. */
    abstract boolean isOffline(final Browser br);

    /**
     * Override this if it is possible to recognize selfhosted content before looking for external URLs. </br>
     * Example plugin: boobinspector.com
     */
    protected boolean isSelfhosted(final Browser br) {
        return false;
    }

    /** Use this if you want to change the URL added by the user before processing it. */
    protected void correctCryptedLink(final CryptedLink param) {
        final String addedLinkDomain = Browser.getHost(param.getCryptedUrl(), true);
        final ArrayList<String> deadDomains = this.getDeadDomains();
        if (deadDomains != null && deadDomains.contains(addedLinkDomain)) {
            param.setCryptedUrl(param.getCryptedUrl().replaceFirst(Pattern.quote(addedLinkDomain), this.getHost()));
        }
    }

    protected ArrayList<DownloadLink> preProcessCryptedLink(final CryptedLink param) throws Exception {
        prepareBrowser(br);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (returnRedirectToUnsupportedLinkAsResult()) {
            br.getPage(param.getCryptedUrl());
            int redirectCounter = 0;
            while (true) {
                if (br.getRedirectLocation() == null) {
                    break;
                }
                if (this.isOffline(br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                redirectCounter++;
                if (redirectCounter >= 20) {
                    throw new IllegalStateException("Too many redirects!");
                } else if (!this.canHandle(br.getRedirectLocation())) {
                    /* Redirect to external website */
                    ret.add(createDownloadlink(br.getRedirectLocation()));
                    return ret;
                } else {
                    br.followRedirect();
                }
            }
        } else {
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
        }
        if (this.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isSelfhosted(br)) {
            ret.add(getDownloadLinkSelfhosted(param, br));
            return ret;
        } else {
            ret.addAll(this.findEmbedUrl(br, getFileTitle(param, br)));
            if (ret.isEmpty() && assumeSelfhostedContentOnNoResults()) {
                ret.add(getDownloadLinkSelfhosted(param, br));
            }
            if (ret.isEmpty() && assumeOfflineOnNoResults()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return ret;
        }
    }

    protected DownloadLink getDownloadLinkSelfhosted(final CryptedLink param, final Browser br) {
        final DownloadLink selfhosted = this.createDownloadlink(br.getURL());
        final String fileTitle = getFileTitle(param, br);
        if (fileTitle != null) {
            if (selfhostedContentSetFinalFilename()) {
                selfhosted.setFinalFileName(fileTitle + ".mp4");
            } else {
                selfhosted.setName(fileTitle + ".mp4");
            }
        }
        if (selfhostedContentSkipAvailablecheck()) {
            selfhosted.setAvailable(true);
        }
        return selfhosted;
    }

    /**
     * Get source string to parse URLs from. Useful for websites which contain e.g. one video but also URLs to porn channels which would
     * otherwise be crawled by our auto handling e.g. woodrocket.com. </br>
     * If this returns null, crawler will fail!! </br>
     * By default this will scan the complete html code of given browser instance.
     */
    protected String getParseSource(final Browser br) {
        return br.getRequest().getHtmlCode();
    }

    /**
     * Use this to allow/skip found URLs by pattern. </br>
     * Does by default not allow items that would go back into current crawler plugin.
     */
    protected boolean allowResult(final String url) {
        if (this.canHandle(url)) {
            /* Do not allow results that this plugin would handle by default. */
            return false;
        } else {
            return true;
        }
    }

    /** Example where it makes sense to enable this: amateurmasturbations.com. */
    protected boolean returnRedirectToUnsupportedLinkAsResult() {
        return false;
    }

    /** Assumes that content is selfhosted if no external results are found. */
    protected boolean assumeSelfhostedContentOnNoResults() {
        return false;
    }

    /** Override this and return true if PluginException with LinkStatus FILE_NOT_FOUND should be thrown if no results were found. */
    protected boolean assumeOfflineOnNoResults() {
        return false;
    }

    /**
     * If set to true, AvailableStatus of DownloadLink for selfhosted content will be set to TRUE in crawler so it does not need to get
     * linkchecked by hosterplugin --> Appears in linkgrabber faster.
     */
    protected boolean selfhostedContentSkipAvailablecheck() {
        return true;
    }

    /** Enable this if DownloadLink.setFinalFilename should be used over DownloadLink.setName. */
    protected boolean selfhostedContentSetFinalFilename() {
        return false;
    }

    /**
     * find the first within findEmbedUrl method from default this.br browser.
     *
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl() throws Exception {
        return findEmbedUrls(br, false);
    }

    /**
     * finds and returns only the first
     *
     * @param title
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrl(final String title) throws Exception {
        return findEmbedUrls(br, false);
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
        return findEmbedUrls(ibr, false);
    }

    /**
     * finds all embed urls from this.br
     *
     * @return
     * @throws Exception
     */
    public final ArrayList<DownloadLink> findEmbedUrls() throws Exception {
        return findEmbedUrls(br, true);
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
        return findEmbedUrls(ibr, true);
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
    public final ArrayList<DownloadLink> findEmbedUrls(final Browser br, final boolean processAll) throws Exception {
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
        logger.info("PornEmbedParser is being executed...");
        /************************************************************************************************************/
        // Now check for all existant URLs if they're supported by any plugin tagged as porn plugin
        /************************************************************************************************************/
        final String[] urls = getEmbedURLs(br);
        if (urls != null) {
            for (final String url : urls) {
                if (allowResult(url)) {
                    final List<LazyCrawlerPlugin> nextLazyCrawlerPlugins = findNextLazyCrawlerPlugins(url, LazyPlugin.FEATURE.XXX);
                    if (nextLazyCrawlerPlugins.size() > 0) {
                        decryptedLinks.addAll(convert(br, url, nextLazyCrawlerPlugins));
                    }
                    final List<LazyHostPlugin> nextLazyHostPlugins = findNextLazyHostPlugins(url, LazyPlugin.FEATURE.XXX);
                    if (nextLazyHostPlugins.size() > 0) {
                        decryptedLinks.addAll(convert(br, url, nextLazyHostPlugins));
                    }
                }
            }
        }
        return decryptedLinks;
    }

    protected String[] getEmbedURLs(final Browser br) throws Exception {
        final String parseSource = getParseSource(br);
        final String[] urls = HTMLParser.getHttpLinks(parseSource, br.getURL());
        return urls;
    }

    protected List<DownloadLink> convert(final Browser br, final String url, List<? extends LazyPlugin> lazyPlugins) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (lazyPlugins.size() == 0) {
            return ret;
        }
        ret.add(createDownloadlink(Request.getLocation(url, br.getRequest())));
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
