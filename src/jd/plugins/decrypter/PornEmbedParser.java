package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.DecrypterArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

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
     * PornEmbedParser 0.3.2
     *
     *
     * porn_plugin
     *
     *
     * This method is designed to find embedded porn urls in html code.
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
        // use plugin regex where possible... this means less maintaince required.
        /* Cleanup/Improve title */
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            title = encodeUnicode(title);
        }
        logger.info("PornEmbedParser is being executed...");
        String externID = null;
        // youporn.com handling 1
        externID = br.getRegex("youporn\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add("//www.youporn.com/watch/" + externID + "/" + System.currentTimeMillis());
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // youporn.com handling 3 2018-01-07
        externID = br.getRegex("ypncdn\\.com/[\\d]+/[\\d]+/(\\d+)/").getMatch(0);
        if (externID != null) {
            decryptedLinks.add("//www.youporn.com/watch/" + externID + "/" + System.currentTimeMillis());
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("pornyeah\\.com/playerConfig\\.php\\?[a-z0-9]+\\.[a-z0-9\\.]+\\|(\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("//www.pornyeah.com/videos/" + Integer.toString(new Random().nextInt(1000000)) + "-" + externID + ".html");
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("<source src=\"[^\"]+extremetube.spankcdn.net/media/\\d+/\\d*/(\\d+)[^\"]+\"").getMatch(0);
        if (externID != null) {
            externID = "https://www.extremetube.com/video/-" + externID;
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("\"((?:https?:)?//(?:www\\.)?boysfood\\.com/embed/\\d+/?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("\"((?:https?:)?//video\\.fc2\\.com/a/flv2\\.swf\\?i=\\w+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // isharemybitch.com #1
        externID = br.getRegex("(\"|')((?:https?:)?//(?:www\\.)?isharemybitch\\.com/flvPlayer\\.swf\\?settings=[^<>\"]*?)\"").getMatch(0);
        // isharemybitch.com #2
        if (externID == null) {
            externID = br.getRegex("\"((?:https?:)?//(?:www\\.)?share-image\\.com/gallery/[^<>\"]*?)\"").getMatch(0);
        }
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("src=\"(?:https?:)?//videos\\.allelitepass\\.com/txc/([^<>\"/]*?)\\.swf\"").getMatch(0);
        if (externID != null) {
            /* Add as offline -this site is down! */
            decryptedLinks.add(createOfflinelink("//videos.allelitepass.com/txc/player.php?video=" + Encoding.htmlDecode(externID)));
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("\"((?:https?:)?//(?:www\\.)?isharemybitch\\.com/flvPlayer\\.swf\\?settings=[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("\"((?:https?:)?//(?:www\\.)?youtube\\.com/embed/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        /* RegExes for permanently offline websites go here */
        /* 2017-01-27 porn.com */
        externID = br.getRegex("('|\")((?:https?:)?//(?:www\\.)?porn\\.com/videos/embed/\\d+?)\\1").getMatch(1);
        if (externID == null) {
            externID = br.getRegex("('|\")((?:https?:)?//(?:www\\.)?porn\\.com/embed/\\d+)\\1").getMatch(1);
        }
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2018-01-07 hotmovs.com
        externID = br.getRegex("src=\"https?://www.hotmovs.com/embed/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add("http://hotmovs.com/videos/" + externID + "/anything/");
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2018-01-08 mcfucker.com
        externID = br.getRegex("<iframe [^<>]+ src=\"(https?://www.mcfucker.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2018-06-16 cliphunter.com
        externID = br.getRegex("<iframe [^<>]+ src=(?:'|\")(https?://www.cliphunter.com/embed/\\d+)(?:'|\")").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(externID.replace("/embed/", "/w/") + "/anything");
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2019-01-15 share-videos.se
        externID = br.getRegex("(embed\\.share\\-videos\\.se/auto/embed/\\d+\\?uid=\\d+)").getMatch(0);
        if (externID != null) {
            externID = "https://" + externID;
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2019-01-15 asianclub.tv
        externID = br.getRegex("(https?://asianclub\\.tv/v/[A-Za-z0-9\\-]+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2019-01-16 javynow.com
        externID = br.getRegex("(javynow\\.com/player/\\d+/[^\"]+)").getMatch(0);
        if (externID != null) {
            externID = "https://" + externID;
            decryptedLinks.add(externID);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2019-01-24 flyflv.com
        externID = br.getRegex("(//(?:www\\.)?flyflv\\.com/movies/player/\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = this.createDownloadlink(externID);
            /* Filename is good to have but not necessarily required, */
            if (title != null) {
                title += ".mp4";
                dl.setFinalFileName(title);
            }
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        // 2019-01-24 hqwo.cc (no main webpage, only works when you have URLs which lead to content!)
        externID = br.getRegex("(//hqwo\\.cc/player/[^<>\"]+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = this.createDownloadlink(externID);
            /* Filename is good to have but not necessarily required, */
            if (title != null) {
                title += ".mp4";
                dl.setFinalFileName(title);
            }
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        /* 2020-05-19: cwtembeds.com (main page will display error 404, they ONLY provide embedded URLs! E.g. /embed/1362088 ) */
        externID = br.getRegex("(https?://(?:www\\.)?cwtvembeds\\.com/embed/\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = this.createDownloadlink(externID);
            /* Filename is good to have but not necessarily required, */
            if (title != null) {
                title += ".mp4";
                dl.setFinalFileName(title);
            }
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
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
        externID = br.getRegex("(https?://redgifs\\.com/ifr/[A-Za-z0-9]+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = this.createDownloadlink(externID);
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
        /* TODO: Remove as much Browser-accesses as possible, handle all embedded urls in the corresponding host plugins! */
        externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("//www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
            dl.setFinalFileName(title);
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        externID = br.getRegex("src=\"(?:https?:)?//videos\\.allelitepass\\.com/txc/([^<>\"/]*?)\\.swf\"").getMatch(0);
        if (externID != null) {
            final Browser al = br.cloneBrowser();
            getPage(al, "http://videos.allelitepass.com/txc/player.php?video=" + Encoding.htmlDecode(externID));
            externID = al.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setForcedFileName(title + ".flv");
                decryptedLinks.add(dl);
                if (!processAll) {
                    return decryptedLinks;
                }
            }
        }
        // youporn.com handling 2
        externID = br.getRegex("flashvars=\"file=(http%3A%2F%2Fdownload\\.youporn\\.com[^<>\"]*?)&").getMatch(0);
        if (externID != null) {
            final Browser yp = br.cloneBrowser();
            yp.setCookie("http://youporn.com/", "age_verified", "1");
            yp.setCookie("http://youporn.com/", "is_pc", "1");
            yp.setCookie("http://youporn.com/", "language", "en");
            getPage(yp, Encoding.htmlDecode(externID));
            if (yp.getRequest().getHttpConnection().getResponseCode() == 404) {
                if (!processAll) {
                    return decryptedLinks;
                }
            } else if (yp.containsHTML("download\\.youporn\\.com/agecheck")) {
                if (!processAll) {
                    return decryptedLinks;
                }
            } else {
                externID = yp.getRegex("\"((?:https?:)?//(www\\.)?download\\.youporn.com/download/\\d+/\\?xml=1)\"").getMatch(0);
                if (externID != null) {
                    getPage(yp, externID);
                    final String finallink = yp.getRegex("<location>((?:https?:)?//.*?)</location>").getMatch(0);
                    if (finallink != null) {
                        final DownloadLink dl = createDownloadlink("directhttp://" + Request.getLocation(Encoding.htmlDecode(finallink), br.getRequest()));
                        String type = yp.getRegex("<meta rel=\"type\">(.*?)</meta>").getMatch(0);
                        if (type == null) {
                            type = "flv";
                        }
                        dl.setForcedFileName(title + "." + type);
                        decryptedLinks.add(dl);
                        if (!processAll) {
                            return decryptedLinks;
                        }
                    }
                }
            }
        }
        // 2018-01-07 Unknown? - directHTTP
        externID = br.getRegex("(https://cv.rdtcdn.com/[^\"]+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            if (title != null) {
                dl.setForcedFileName(title + ".mp4");
            }
            decryptedLinks.add(dl);
            if (!processAll) {
                return decryptedLinks;
            }
        }
        /*
         * 2020-07-21: Skip DownloadLinks of current host found in current html. E.g. pornrabbit.com --> Will find same pornrabbit URL as
         * "embed URL" --> This will lose file title
         */
        return decryptedLinks;
    }

    public List<DownloadLink> convert(Browser br, String title, String url, List<? extends LazyPlugin> lazyPlugins) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (lazyPlugins.size() > 0) {
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
        }
        return ret;
    }

    @Override
    public DownloadLink createDownloadlink(String url) {
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        return super.createDownloadlink(url);
    }
}
