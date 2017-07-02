//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anilinkz.com" }, urls = { "http://(?:www\\.)?anilinkz\\.(?:com|tv|io|to)/[^<>\"/]+(/[^<>\"/]+)?" })
/**
 * @author raztoki
 */
@SuppressWarnings("deprecation")
public class AniLinkzCom extends antiDDoSForDecrypt {
    private final String            supported_hoster = "(4shared\\.com|4vid\\.me|aniupload\\.com|animegg\\.tv|animeultima\\.tv|arkvid\\.tv|byzoo\\.org|chia\\-anime\\.com|cheesestream\\.com|cizgifilmlerizle\\.com|dailymotion\\.com|easyvideo\\.me|facebook\\.com/video|gogoanime\\.com|gorillavid\\.in|mp4star\\.com|mp4upload\\.com|movreel\\.com|myspace\\.com|nowvideo\\.eu|novamov\\.com|play44\\.net|playbb\\.me|playpanda\\.net|rutube\\.ru|stagevu\\.com|upload2\\.com|uploadc\\.com|uploadcrazy\\.net|veevr\\.com|veoh\\.com|vidbox\\.yt|vidcrazy\\.net|video44\\.net|videobb\\.com|videobam\\.com|videobull\\.com|videodrive\\.tv|videofun\\.me|videonest\\.net|videous\\.tv|videoweed\\.com|videowing\\.me|vidzur\\.com|videozoo\\.me|vimeo\\.com|vk\\.com|yourupload\\.com|youtube\\.com|zshare\\.net|220\\.ro|videos\\.sapo\\.pt)";
    private final String            invalid_links    = "http://(?:www\\.)?anilinkz\\.(?:com|tv|io|to)/(search|affiliates|get|img|dsa|forums|files|category|\\?page=|faqs|.*?-list|.*?-info|\\?random).*?";
    private String                  parameter        = null;
    private String                  fpName           = null;
    private String                  escapeAll        = null;
    private int                     spart            = -1;
    private int                     spart_count      = 0;
    private Browser                 br2              = new Browser();
    private ArrayList<DownloadLink> decryptedLinks   = null;
    private static Object           LOCK             = new Object();

    public AniLinkzCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.param = param;
        // testing purpose lets null/zero/false storables
        decryptedLinks = new ArrayList<DownloadLink>();
        escapeAll = null;
        spart = -1;
        spart_count = 0;
        parameter = param.toString().replaceFirst("anilinkz\\..*?/", "anilinkz.to/");
        if (parameter.matches(invalid_links)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        // only allow one thread! To minimise/reduce loads.
        synchronized (LOCK) {
            getPage(parameter);
            boolean offline = false;
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/home/anilinkz/public_html/")) {
                logger.info("Incorrect Link! Redirecting to search page...");
                offline = true;
            } else if (br.getRedirectLocation() != null) {
                br.setFollowRedirects(true);
                getPage(br.getRedirectLocation());
            }
            if (br.containsHTML(">Page Not Found<")) {
                logger.info("Link offline: " + parameter);
                offline = true;
            }
            if (br.containsHTML(">No Results Found|>Search Results for")) {
                logger.info("Link offline: " + parameter);
                offline = true;
            }
            if (offline) {
                try {
                    decryptedLinks.add(createOfflinelink(parameter));
                } catch (final Throwable t) {
                }
                return decryptedLinks;
            }
            if (parameter.matches(".+\\.(?:com|tv|io|to)/series/.+")) {
                int p = 1;
                String page = new Regex(parameter, "\\?page=(\\d+)").getMatch(0);
                if (page != null) {
                    p = Integer.parseInt(page);
                }
                for (int i = 0; i != p; i++) {
                    String host = new Regex(br.getURL(), "(https?://[^/]+)").getMatch(0);
                    // new
                    // <script type="text/javascript">
                    // $(function() {
                    // $('#pagenavi').pagination({
                    // pages: 2,
                    // displayedPages: 7,
                    // currentPage: 1, selectOnClick: false,
                    // hrefTextPrefix: '/series/gigant-shooter-tsukasa?page=',
                    // cssStyle: 'light-theme'
                    // });
                    // });
                    // </script>
                    String pages = br.getRegex("pages: (\\d+),").getMatch(0);
                    String txtPrefix = br.getRegex("hrefTextPrefix: '(/series/[^\\?]+\\?page=)").getMatch(0);
                    String nextPage = (pages != null && Integer.parseInt(pages) >= (p + 1) ? txtPrefix + (p + 1) : null);
                    String[] links = br.getRegex("href=\"(/[^\"]+)\">[^<]+</a>\\s*</span>\\s*Series:").getColumn(0);
                    if (links == null || links.length == 0) {
                        logger.warning("Could not find series 'links' : " + parameter);
                        return null;
                    }
                    for (String link : links) {
                        decryptedLinks.add(createDownloadlink(host + link));
                    }
                    // if page is provided within parameter only add that page
                    if (nextPage != null && !parameter.contains("?page=")) {
                        p++;
                        getPage(nextPage);
                    } else {
                        break;
                    }
                }
            } else {
                // set filepackage
                fpName = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
                if (fpName == null) {
                    logger.warning("filepackage == null: " + parameter);
                    logger.warning("Please report issue to JDownloader Development team!");
                    return null;
                }
                if (parameter.matches(".+\\?src=\\d+")) {
                    // if the user imports src link, just return that link
                    br2 = br.cloneBrowser();
                    parsePage();
                } else {
                    // if the user imports src link, just return that link
                    br2 = br.cloneBrowser();
                    parsePage();
                    // grab src links and process
                    String[] links = br.getRegex("<a rel=\"nofollow\" title=\"[^\"]+(?!dead) Source\" href=\"(/[^\"]+\\?src=\\d+)\">").getColumn(0);
                    if (links != null && links.length != 0) {
                        for (String link : links) {
                            // we should reset any values that carry over!
                            spart = -1;
                            spart_count = 0;
                            br2 = br.cloneBrowser();
                            getPage(br2, link);
                            parsePage();
                        }
                    }
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.setProperty("ALLOW_MERGE", true);
                fp.addLinks(decryptedLinks);
            }
            if (decryptedLinks.isEmpty()) {
                // not necessarily an error...
                // logger.warning("Decrypter out of date for link: " + parameter);
                // return null;
            }
        }
        return decryptedLinks;
    }

    private boolean parsePage() throws Exception {
        if (escapeAll != null && spart > 1) {
            // to prevent over write of escapeAll storable, for split parts within escapeAll
        } else {
            escapeAll = br2.getRegex("escapeall\\('(.*)'\\)\\)\\);").getMatch(0);
            if (!inValidate(escapeAll)) {
                escapeAll = escapeAll.replaceAll("[A-Z~!@#\\$\\*\\{\\}\\[\\]\\-\\+\\.]?", "");
                escapeAll = Encoding.htmlDecode(escapeAll);
                escapeAll = Encoding.urlDecode(escapeAll, false);
                // cleanup crap.. wondering why we do this now?? there are hoster plugins exist of these two
                // if (new Regex(escapeAll, "(?:https?:)?//[^\"]+(cizgifilmlerizle\\.com)/[^\"]+<div[^>]+").matches()) {
                // escapeAll = escapeAll.replaceAll("<div[^>]+>", "");
                // }
            }
            if (inValidate(escapeAll)) {
                // seems they might not be using escape function.. any longer...
                escapeAll = br2.toString();
            }
            if (inValidate(escapeAll) || new Regex(escapeAll, "(/img/\\w+dead\\.jpg|http://www\\./media)").matches()) {
                // escapeAll == null / not online yet... || offline results within escapeAll
                if (br.containsHTML("This page will be updated as soon as|becomes available\\. Stay tuned for ")) {
                    logger.info("Not been release yet... : " + br2.getURL());
                } else if (inValidate(escapeAll)) {
                    logger.info("DeadLink!... : " + br2.getURL());
                } else {
                    logger.warning("Decrypter out of date for link: " + br2.getURL());
                }
                try {
                    decryptedLinks.add(createOfflinelink(br2.getURL()));
                } catch (final Throwable t) {
                }
                return false;
            }
        }
        // embed links that are not found by generic's
        String link = new Regex(escapeAll, "((?:https?:)?//(\\w+\\.)?vureel\\.com/playwire\\.php\\?vid=\\d+)").getMatch(0);
        // with stagevu they are directly imported finallink and not embed player. We want the image for the uid, return to hoster.
        if (inValidate(link) && escapeAll.contains("stagevu.com/")) {
            String stagevu = new Regex(escapeAll, "previewImage=\"https?://stagevu\\.com/img/thumbnail/([a-z]{12})").getMatch(0);
            if (!inValidate(stagevu)) {
                link = "http://stagevu.com/video/" + stagevu;
            } else {
                // error
            }
        } else if (inValidate(link) && escapeAll.contains("smotri.com/")) {
            String smotri = new Regex(escapeAll, "file=(v\\d+)").getMatch(0);
            if (!inValidate(smotri)) {
                link = "http://smotri.com/video/view/?id=" + smotri;
            } else {
                // error
            }
        }
        // generic fail overs
        if (inValidate(link)) {
            link = new Regex(escapeAll, "<iframe src=\"((?:https?:)?//([^<>\"]+)?" + supported_hoster + "/[^<>\"]+)\"").getMatch(0);
        }
        if (inValidate(link)) {
            link = new Regex(escapeAll, "(href|url|file)=\"?((?:https?:)?//([^<>\"]+)?" + supported_hoster + "/[^<>\"]+)\"").getMatch(1);
        }
        if (inValidate(link)) {
            link = new Regex(escapeAll, "src=\"((?:https?:)?//([^<>\"]+)?" + supported_hoster + "/[^<>\"]+)\"").getMatch(0);
        }
        if (!inValidate(link)) {
            if (!link.startsWith("http") && link.startsWith("//")) {
                link = "http:" + link;
            }
            decryptedLinks.add(createDownloadlink(link));
        } else if (inValidate(link) && new Regex(escapeAll, "(anilinkz\\.(?:com|tv|io|to)/get/|chia-anime\\.com/|myvideo\\.de/)").matches()) {
            String[] aLinks = new Regex(escapeAll, "((?:https?:)?[^\"]+/get/[^\"]+)").getColumn(0);
            // chia-anime can't be redirected back into dedicated plugin
            if ((aLinks == null || aLinks.length == 0) && escapeAll.contains("chia-anime.com")) {
                aLinks = new Regex(escapeAll, "url\":\"((?:https?:)?[^\"]+chia-anime\\.com[^\"]+)").getColumn(0);
            }
            // myvideo links are also direct links to flvs, using anilinks own swf player.
            if ((aLinks == null || aLinks.length == 0) && escapeAll.contains("myvideo.de")) {
                aLinks = new Regex(escapeAll, "=((?:https?:)?[^\"]+myvideo\\.de/[^\"]+)").getColumn(0);
            }
            if (aLinks != null && aLinks.length != 0) {
                for (String aLink : aLinks) {
                    DownloadLink downloadLink = createDownloadlink("directhttp://" + aLink);
                    downloadLink.setFinalFileName(fpName + aLink.substring(aLink.lastIndexOf(".")));
                    Browser br2 = br.cloneBrowser();
                    // In case the link redirects to the finallink
                    br2.setFollowRedirects(true);
                    URLConnectionAdapter con = null;
                    try {
                        con = br2.openGetConnection(aLink);
                        // only way to check for made up links... or offline is here
                        if (!con.getContentType().contains("html")) {
                            downloadLink.setName(fpName + getFileNameFromHeader(con).substring(getFileNameFromHeader(con).lastIndexOf(".")));
                            downloadLink.setDownloadSize(con.getLongContentLength());
                            downloadLink.setAvailable(true);
                        } else {
                            downloadLink.setAvailable(false);
                        }
                        decryptedLinks.add(downloadLink);
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }
                }
            }
        }
        // logic to deal with split parts within escapeAll. Uses all existing code within parsePage (see #9373)
        final String[] sprt = new Regex(escapeAll, "(<div class=\"spart\".*?</div>|Part \\d+\r\n)").getColumn(0);
        if (sprt != null && sprt.length > 1) {
            if (spart == -1) {
                spart = sprt.length;
            }
            // lets remove previous results from escape all
            if (spart > 1 && link != null && spart != (spart_count + 1)) {
                escapeAll = escapeAll.replace(link, "");
                spart_count++;
                return parsePage();
            }
        }
        if (link == null) {
            logger.warning("link == null");
            logger.warning(escapeAll);
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}