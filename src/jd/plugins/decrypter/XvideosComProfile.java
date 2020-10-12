//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.config.XvideosComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.XvideosCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XvideosComProfile extends PluginForDecrypt {
    public XvideosComProfile(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xvideos.com", "xvideos.es", "xvideos2.com", "xvideos2.es", "xvideos3.com", "xvideos3.es", "xvideos.red" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/");
            sb.append("(");
            sb.append("(?:profiles|(?:pornstar-|amateur-|model-)?(?:channels|models))/[A-Za-z0-9\\-_]+(?:/photos/\\d+/[A-Za-z0-9\\-_]+)?");
            sb.append("|favorite/\\d+/[a-z0-9\\_]+");
            sb.append(")");
            ret.add(sb.toString());
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_FAVOURITES = "https?://[^/]+/favorite/(\\d+)/([a-z0-9\\-_]+).*";
    private static final String TYPE_USER       = "https?://[^/]+/(?:profiles|(?:pornstar-|amateur-|model-)?(?:channels|models))/[A-Za-z0-9\\-_]+$";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        /*
         * 2020-10-12: In general, we use the user-added domain but some are dead but the content might still be alive --> Use main plugin
         * domain for such cases
         */
        for (final String deadDomain : XvideosCom.deadDomains) {
            if (parameter.contains(deadDomain)) {
                parameter = parameter.replace(deadDomain + "/", this.getHost() + "/");
                break;
            }
        }
        final boolean premiumAccountRequired = Browser.getHost(parameter).equals("xvideos.red");
        br.addAllowedResponseCodes(new int[] { 400 });
        br.setFollowRedirects(true);
        Account account = AccountController.getInstance().getValidAccount(getHost());
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        if (account != null) {
            try {
                ((jd.plugins.hoster.XvideosCom) plg).login(this, account, false);
            } catch (PluginException e) {
                logger.info("Login failure");
                handleAccountException(account, e);
                account = null;
                if (premiumAccountRequired) {
                    logger.warning("Account required but login failed");
                    throw new AccountRequiredException();
                }
            }
        } else if (premiumAccountRequired) {
            /* Account required! */
            throw new AccountRequiredException();
        }
        br.getPage(parameter);
        /* E.g. xvideos.com can redirect to xvideos.red when account is active. */
        final boolean premiumAccountActive = this.br.getHost().equals("xvideos.red");
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_FAVOURITES)) {
            this.crawlFavourites(parameter, decryptedLinks);
        } else if (parameter.matches(".+/photos/.+")) {
            crawlPhotos(parameter, decryptedLinks);
        } else if (parameter.matches(TYPE_USER) && premiumAccountActive) {
            crawlVideosPremium(parameter, decryptedLinks);
        } else {
            crawlVideos(parameter, decryptedLinks);
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Failed to find and content for: " + parameter);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void crawlFavourites(final String parameter, final ArrayList<DownloadLink> decryptedLinks) throws IOException {
        final String fpname = new Regex(parameter, TYPE_FAVOURITES).getMatch(1);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpname);
        String nextpage = null;
        do {
            final String[] urls = br.getRegex("\"(/video\\d+/[^<>\"]+)").getColumn(0);
            for (String url : urls) {
                url = br.getURL(url).toString();
                final String url_title = new Regex(url, "/video\\d+/([^/\\?]+)").getMatch(0);
                final DownloadLink dl = this.createDownloadlink(url);
                /* Save http requests */
                dl.setAvailable(true);
                dl.setName(url_title + ".mp4");
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            nextpage = br.getRegex("href=\"(/favorite/\\d+/[^/]+/\\d+)\"[^>]*class=\"no-page next-page\"").getMatch(0);
            if (nextpage != null) {
                logger.info("Working on page: " + nextpage);
                br.getPage(nextpage);
            } else {
                break;
            }
        } while (!this.isAbort());
    }

    private void crawlVideos(final String parameter, final ArrayList<DownloadLink> decryptedLinks) throws IOException, PluginException {
        final ArrayList<String> dupeList = new ArrayList<String>();
        final Regex urlinfo = new Regex(parameter, "https?://[^/]+/([^/]+)/(.+)");
        final String type = urlinfo.getMatch(0);
        final String username = urlinfo.getMatch(1);
        if (!br.getURL().contains(username)) {
            /* E.g. redirect to mainpage */
            logger.info("Profile does not exist anymore");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(username.trim()));
        fp.addLinks(decryptedLinks);
        short pageNum = 0;
        int decryptedLinksNum;
        final boolean fast_linkcheck = PluginJsonConfig.get(XvideosComConfig.class).isEnableFastLinkcheckForProfileCrawler();
        do {
            logger.info(String.format("Crawling page %d", pageNum));
            decryptedLinksNum = 0;
            br.getPage("/" + type + "/" + username + "/videos/best/" + pageNum);
            // users don't always have profile... as guardo finds links from google... false positive.
            if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 400) {
                return;
            } else if (br.toString().matches("<h4 class=\"text-center\">[^<]+  hat keine hochgeladene Videos</h4>\\s*")) {
                logger.info("This user does not have any videos");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return;
            }
            final String[] links = br.getRegex("(/prof-video-click/[^/]+/[^/]+/\\d+((?:/THUMBNUM)?/[^/\"\\']+)?)").getColumn(0);
            if (!br.containsHTML("profile-listing-uploads") && !br.containsHTML("profile-videos-sort") && (links == null || links.length == 0)) {
                logger.info("All videos found or this user does not have any videos");
                break;
            } else if (links == null || links.length == 0) {
                logger.info("Stopping because: Failed to find anything on current page");
                break;
            }
            decryptedLinksNum = links.length;
            for (String singleLink : links) {
                final String videoID = new Regex(singleLink, "prof-video-click/[^/]+/[^/]+/(\\d+)").getMatch(0);
                /* Only add new URLs */
                if (!dupeList.contains(videoID)) {
                    singleLink = "https://www." + this.br.getHost() + singleLink;
                    final String url_name = new Regex(singleLink, "/\\d+/(?:THUMBNUM/)?(.+)").getMatch(0);
                    final String name_temp;
                    final DownloadLink dl = createDownloadlink(singleLink);
                    if (fast_linkcheck) {
                        /* Usually we will crawl a lot of URLs at this stage --> Set onlinestatus right away! */
                        dl.setAvailable(true);
                    }
                    fp.add(dl);
                    if (url_name != null) {
                        String clean = url_name.replaceAll("(watch_)?(free_)?(live_)?camgirls_at_(www(_|\\.))?teenhdcams(_|\\.)com$", "");
                        clean = clean.replaceAll("(watch_)?free_at_(www(_|\\.))?teenhdcams(_|\\.)com$", "");
                        clean = clean.replaceAll("(watch_)?full_video_at_(www(_|\\.))?teenhdcams(_|\\.)com$", "");
                        clean = clean.replaceAll("\\.*_*$", "");
                        name_temp = videoID + "_" + clean;
                    } else {
                        name_temp = videoID;
                    }
                    dl.setName(name_temp + ".mp4");
                    /* Packagizer properties */
                    dl.setProperty("username", username);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    decryptedLinksNum++;
                    dupeList.add(videoID);
                }
            }
            pageNum++;
        } while (!this.isAbort() && decryptedLinksNum >= 36);
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    /**
     * 2020-10-06: Special method for xvideos.red though this will also work for xvideos.com. Old method is still in use as it's still
     * working!
     */
    private void crawlVideosPremium(final String parameter, final ArrayList<DownloadLink> decryptedLinks) throws IOException, PluginException {
        final ArrayList<String> dupeList = new ArrayList<String>();
        final Regex urlinfo = new Regex(parameter, "https?://[^/]+/([^/]+)/(.+)");
        final String type = urlinfo.getMatch(0);
        final String username = urlinfo.getMatch(1);
        if (!br.getURL().contains(username)) {
            /* E.g. redirect to mainpage */
            logger.info("Profile does not exist anymore");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(username.trim()));
        fp.addLinks(decryptedLinks);
        short pageNum = 0;
        int decryptedLinksNum;
        final boolean fast_linkcheck = PluginJsonConfig.get(XvideosComConfig.class).isEnableFastLinkcheckForProfileCrawler();
        do {
            logger.info(String.format("Crawling page %d", pageNum));
            decryptedLinksNum = 0;
            br.getPage("/" + type + "/" + username + "/videos/premium/" + pageNum);
            // users don't always have profile... as Guardao finds links from google... false positive.
            if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 400) {
                return;
            }
            final String[] links = br.getRegex("(/video\\d+/[^\"]+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.info("Stopping because: Failed to find anything on current page");
                break;
            }
            decryptedLinksNum = links.length;
            for (String singleLink : links) {
                final Regex urlRegex = new Regex(singleLink, "/video(\\d+)/([^\"]+)");
                final String videoID = urlRegex.getMatch(0);
                /* Only add new URLs */
                if (!dupeList.contains(videoID)) {
                    singleLink = "https://www." + this.br.getHost() + singleLink;
                    final String url_name = urlRegex.getMatch(1);
                    final String name_temp;
                    final DownloadLink dl = createDownloadlink(singleLink);
                    if (fast_linkcheck) {
                        /* Usually we will crawl a lot of URLs at this stage --> Set onlinestatus right away! */
                        dl.setAvailable(true);
                    }
                    fp.add(dl);
                    if (url_name != null) {
                        name_temp = videoID + "_" + url_name.replace("-", "");
                    } else {
                        name_temp = videoID;
                    }
                    dl.setName(name_temp + ".mp4");
                    /* Packagizer properties */
                    dl.setProperty("username", username);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    decryptedLinksNum++;
                    dupeList.add(videoID);
                }
            }
            pageNum++;
        } while (!this.isAbort() && decryptedLinksNum >= 36);
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void crawlPhotos(final String parameter, final ArrayList<DownloadLink> decryptedLinks) throws IOException, PluginException {
        final Regex urlinfo = new Regex(parameter, "https?://[^/]+/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)");
        final String type = urlinfo.getMatch(0);
        final String username = urlinfo.getMatch(1);
        final String galleryID = urlinfo.getMatch(3);
        final String galleryName = urlinfo.getMatch(4);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + " - " + galleryName);
        fp.addLinks(decryptedLinks);
        /* These are direct-URLs */
        final String[] links = br.getRegex("class=\"embed\\-responsive\\-item\" href=\"(https?[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Failed to find any photos");
            return;
        }
        int counter = 1;
        for (String singleLink : links) {
            if (this.isAbort()) {
                return;
            }
            String url_filename = getFileNameFromURL(new URL(singleLink));
            if (url_filename == null) {
                url_filename = "_" + counter;
            }
            url_filename = username + "_" + galleryID + "_" + galleryName + url_filename;
            final DownloadLink dl = createDownloadlink(singleLink);
            /* Usually we will crawl a lot of URLs at this stage --> Set onlinestatus right away! */
            dl.setAvailable(true);
            fp.add(dl);
            dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            dl.setFinalFileName(url_filename);
            /* Packagizer properties */
            dl.setProperty("username", username);
            decryptedLinks.add(dl);
            distribute(dl);
            counter++;
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }
}
