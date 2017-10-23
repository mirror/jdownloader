//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/(?!art/|status/)[^<>\"]+" })
public class DeviantArtCom extends PluginForDecrypt {
    /**
     * @author raztoki
     */
    public DeviantArtCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This plugin grabs range of content depending on parameter.
    // profile.devart.com/gallery/uid*
    // profile.devart.com/favorites/uid*
    // profile.devart.com/gallery/*
    // profile.devart.com/favorites/*
    // * = ?offset=\\d+
    //
    // All of the above formats should support spanning pages, but when
    // parameter contains '?offset=x' it will not span.
    //
    // profilename.deviantart.com/art/uid/ == grabs the 'download image' (best
    // quality available).
    //
    // I've created the plugin this way to allow users to grab as little or as
    // much, content as they wish. Hopefully this wont create any
    // issues.
    private static Object       LOCK                          = new Object();
    private static final String FASTLINKCHECK_2               = "FASTLINKCHECK_2";
    private static final String TYPE_COLLECTIONS              = "https?://[\\w\\.\\-]*?deviantart\\.com/.*?/collections(/.+)?";
    private static final String TYPE_CATPATH_ALL              = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath(=.+)?";
    private static final String TYPE_CATPATH_1                = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath(=(/|%2F([a-z0-9]+)?|[a-z0-9]+)(\\&offset=\\d+)?)?";
    private static final String TYPE_CATPATH_2                = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath=[a-z0-9]{1,}(\\&offset=\\d+)?";
    private static final String TYPE_JOURNAL                  = "https?://[\\w\\.\\-]*?deviantart\\.com/journal.+";
    private static final String LINKTYPE_JOURNAL              = "https?://[\\w\\.\\-]*?deviantart\\.com/journal/[\\w\\-]+/?";
    private static final String TYPE_BLOG                     = "https?://[\\w\\.\\-]*?deviantart\\.com/blog/(\\?offset=\\d+)?";
    // private static final String TYPE_INVALID = "https?://[\\w\\.\\-]*?deviantart\\.com/stats/*?";
    private String              parameter                     = null;
    private boolean             fastLinkCheck                 = false;
    private boolean             forceHtmlDownload             = false;
    private boolean             crawlGivenOffsetsIndividually = false;
    private long                decryptedUrlsNum              = 0;

    protected void distribute(final DownloadLink dl) {
        super.distribute(dl);
        decryptedUrlsNum++;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        JDUtilities.getPluginForHost(this.getHost());
        jd.plugins.hoster.DeviantArtCom.prepBR(this.br);
        fastLinkCheck = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.DeviantArtCom.FASTLINKCHECK_2, false);
        forceHtmlDownload = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.DeviantArtCom.FORCEHTMLDOWNLOAD, false);
        crawlGivenOffsetsIndividually = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.DeviantArtCom.CRAWL_GIVEN_OFFSETS_INDIVIDUALLY, jd.plugins.hoster.DeviantArtCom.default_CRAWL_GIVEN_OFFSETS_INDIVIDUALLY);
        parameter = param.toString();
        /* Remove trash */
        final String replace = new Regex(parameter, "(#.+)").getMatch(0);
        if (replace != null) {
            parameter = parameter.replace(replace, "");
        }
        /* Fix journallinks: http://xx.deviantart.com/journal/poll/xx/ */
        parameter = parameter.replaceAll("/(poll|stats)/", "/");
        if (parameter.matches(LINKTYPE_JOURNAL)) {
            final DownloadLink journal = createDownloadlink(parameter.replace("deviantart.com/", "deviantartdecrypted.com/"));
            journal.setName(new Regex(parameter, "deviantart\\.com/journal/([\\w\\-]+)").getMatch(0));
            if (fastLinkCheck) {
                journal.setAvailable(true);
            }
            distribute(journal);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        /* Login if possible. Sometimes not all items of a gallery are visible without being logged in. */
        final Account acc = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        if (acc != null) {
            try {
                // broken at the moment
                // jd.plugins.hoster.DeviantArtCom.login(this.br, acc, false);
            } catch (final Throwable e) {
            }
        }
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("The page you were looking for doesn\\'t exist\\.") || br.getURL().matches("https?://([A-Za-z0-9]+\\.)?deviantart\\.com/browse/.+")) {
            distribute(this.createOfflinelink(this.parameter));
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_JOURNAL)) {
            decryptJournals();
        } else if (new Regex(parameter, Pattern.compile(TYPE_COLLECTIONS, Pattern.CASE_INSENSITIVE)).matches()) {
            decryptCollections();
        } else if (parameter.matches(TYPE_BLOG)) {
            decryptBlog();
        } else if (parameter.contains("/gallery/") || parameter.contains("/favourites/")) {
            decryptStandard();
        } else {
            logger.info("Link unsupported: " + parameter);
            return decryptedLinks;
        }
        if (decryptedUrlsNum == 0) {
            logger.info("Link probably offline: " + parameter);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void decryptJournals() throws DecrypterException, IOException {
        if (br.containsHTML("class=\"empty\\-state journal\"")) {
            distribute(this.createOfflinelink(parameter));
            return;
        }
        String username = getSiteUsername();
        if (username == null) {
            username = getURLUsername();
        }
        String paramdecrypt;
        if (parameter.contains("catpath=/")) {
            paramdecrypt = parameter.replace("catpath=/", "catpath=%2F") + "&offset=";
        } else {
            paramdecrypt = parameter + "?offset=";
        }
        if (username == null) {
            logger.warning("Plugin broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + " - Journal");
        fp.setProperty("ALLOW_MERGE", true);
        String next = null;
        int previousOffset = 0;
        int currentOffset = 0;
        final boolean stop_after_first_run = getOffsetFromURL() != null;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return;
            }
            logger.info("Decrypting offset " + next);
            if (next != null) {
                currentOffset = Integer.parseInt(next);
                /* Fail safe */
                if (currentOffset <= previousOffset) {
                    logger.info("Seems like we're done!");
                    break;
                }
                br.getPage(paramdecrypt + next);
            }
            final String jinfo[] = br.getRegex("data\\-deviationid=\"\\d+\" href=\"(https?://[\\w\\.\\-]*?\\.deviantart\\.com/journal/[\\w\\-]+)\"").getColumn(0);
            if (jinfo == null || jinfo.length == 0) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            for (final String link : jinfo) {
                final String urltitle = new Regex(link, "deviantart\\.com/journal/([\\w\\-]+)").getMatch(0);
                final DownloadLink dl = createDownloadlink(link.replace("deviantart.com/", "deviantartdecrypted.com/"));
                if (fastLinkCheck) {
                    dl.setAvailable(true);
                }
                /* No reason to hide their single links */
                dl.setContentUrl(link);
                dl.setName(urltitle + ".html");
                dl.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.HTML);
                dl._setFilePackage(fp);
                distribute(dl);
            }
            next = br.getRegex("class=\"next\"><a class=\"away\" data\\-offset=\"(\\d+)\"").getMatch(0);
            previousOffset = currentOffset;
            if (stop_after_first_run) {
                logger.info("Decrypted given offset, stopping...");
                break;
            }
        } while (next != null);
    }

    private void decryptCollections() throws DecrypterException {
        final String[] links = br.getRegex("<a href=\"(https?://[^<>\"/]+\\.deviantart\\.com/(art|journal)/[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Plugin broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (final String aLink : links) {
            final DownloadLink dl = createDownloadlink(aLink);
            if (fastLinkCheck) {
                dl.setAvailable(true);
            }
            if (forceHtmlDownload) {
                dl.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.HTML);
            } else {
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            }
            distribute(dl);
        }
    }

    private void decryptBlog() throws DecrypterException, IOException {
        if (br.containsHTML(">Sorry\\! This blog entry cannot be displayed")) {
            distribute(this.createOfflinelink(this.parameter));
            return;
        }
        String fpName = br.getRegex("name=\"og:title\" content=\"([^<>\"]*?) on DeviantArt\"").getMatch(0);
        final FilePackage fp;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fpName + " - Journal");
            fp.setProperty("ALLOW_MERGE", true);
        } else {
            fp = null;
        }
        final boolean stop_after_first_run = getOffsetFromURL() != null;
        int currentOffset = 0;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return;
            }
            logger.info("Decrypting offset " + currentOffset);
            if (currentOffset > 0) {
                accessOffset(currentOffset);
            }
            final String[] links = br.getRegex("<a href=\"(https?://[^<>\"/]+\\.deviantart\\.com/journal/[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Plugin broken for link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            for (final String aLink : links) {
                final DownloadLink dl = createDownloadlink(aLink);
                if (fastLinkCheck) {
                    dl.setAvailable(true);
                }
                dl.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.HTML);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                distribute(dl);
            }
            currentOffset += 5;
        } while (br.containsHTML("class=\"next\"><a class=\"away\" data\\-offset=\"\\d+\"") && !stop_after_first_run);
    }

    private void decryptStandard() throws DecrypterException, IOException {
        if (br.containsHTML("class=\"empty\\-state gallery\"|class=\"empty\\-state faves\"")) {
            distribute(this.createOfflinelink(parameter));
            return;
        }
        /* Correct input links */
        if (parameter.matches("https?://[^<>\"/]+\\.deviantart\\.com/gallery/\\?\\d+")) {
            final Regex paramregex = new Regex(parameter, "(https?://[^<>\"/]+\\.deviantart\\.com/gallery/\\?)(\\d+)");
            parameter = paramregex.getMatch(0) + "set=" + paramregex.getMatch(1);
        }
        /* only non /art/ requires packagename */
        // find and set username
        String username = getSiteUsername();
        if (username == null && !parameter.contains("://www.")) {
            username = new Regex(parameter, "https?://([^<>\"]*?)\\.deviantart\\.com/").getMatch(0);
        }
        if (username == null) {
            logger.warning("Plugin broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        // find and set page type
        String pagetype = "";
        if (parameter.matches(TYPE_CATPATH_2)) {
            pagetype = new Regex(parameter, "deviantart\\.com/gallery/\\?catpath=([a-z0-9]+)").getMatch(0);
            /* First letter = capital letter */
            pagetype = pagetype.substring(0, 1).toUpperCase() + pagetype.substring(1, pagetype.length());
        } else if (parameter.contains("/favourites/")) {
            pagetype = "Favourites";
        } else if (parameter.contains("/gallery/")) {
            pagetype = "Gallery";
        } else {
            pagetype = "Unknown";
        }
        // find and set pagename
        String pagename = br.getRegex("class=\"folder\\-title\">([^<>\"]*?)</span>").getMatch(0);
        if (pagename != null) {
            pagename = Encoding.htmlDecode(pagename.trim());
        }
        // set packagename
        String fpName = "";
        if (pagename != null) {
            fpName = username + " - " + pagetype + " - " + pagename;
        } else {
            fpName = username + " - " + pagetype;
        }
        int currentOffset = 0;
        int maxOffset = 0;
        int timesNoItems = 0;
        final int times_No_Items_Max = 9;
        final int offsetIncrease = 24;
        int mp = 0;
        int request_counter = 1;
        boolean startOffsetGiven = false;
        if (parameter.contains("offset=")) {
            final int offsetLink = Integer.parseInt(new Regex(parameter, "offset=(\\d+)").getMatch(0));
            currentOffset = offsetLink;
            startOffsetGiven = true;
        }
        /* Debug */
        // currentOffset = 130403;
        // mp = 5435;
        // counter = 2;
        FilePackage fp = null;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.setProperty("ALLOW_MERGE", true);
        }
        final String csrf = PluginJSonUtils.getJsonValue(this.br, "csrf");
        final String requestid = PluginJSonUtils.getJsonValue(this.br, "requestid");
        final String galleryid = PluginJSonUtils.getJsonValue(this.br, "galleryId");
        String catpath_var = new Regex(parameter, "catpath=([^\\&]+)").getMatch(0);
        if (catpath_var != null) {
            catpath_var = Encoding.urlEncode(catpath_var);
        }
        boolean use_ajax_requests = false;
        boolean has_more = true;
        String has_more_str = null;
        if (csrf == null || csrf.equals("") || requestid == null || requestid.equals("") || galleryid == null || galleryid.equals("")) {
            throw new DecrypterException("Plugin broken");
        }
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return;
            }
            logger.info("Decrypting offset " + currentOffset + " of ?");
            if (request_counter > 1) {
                if (use_ajax_requests) {
                    /*
                     * 2017-02-20: Now they have an API. At the moment we do not yet parse the json as we really only need the URLs which
                     * are inside the html which is inside the json :)
                     */
                    String postdata = "username=" + username + "&offset=" + currentOffset + "&limit=" + offsetIncrease + "&_csrf=" + csrf + "&dapiIid=" + requestid;
                    if (catpath_var != null) {
                        postdata += "&catpath=" + catpath_var;
                    }
                    this.br.getHeaders().put("Referer", this.parameter);
                    this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                    this.br.postPage(String.format("https://www.deviantart.com/dapi/v1/gallery/%s?iid=%s&mp=%s", galleryid, requestid, mp), postdata);
                    has_more_str = PluginJSonUtils.getJsonValue(this.br, "has_more");
                    if (has_more_str != null && has_more_str.matches("true|false")) {
                        has_more = Boolean.parseBoolean(has_more_str);
                    } else {
                        has_more = false;
                    }
                    mp++;
                } else {
                    /* Old method */
                    accessOffset(currentOffset);
                }
            }
            try {
                final boolean galleryEndReached = this.br.containsHTML("class=\"empty\\-state gallery\"");
                if (galleryEndReached) {
                    /* 2017-02-04 */
                    logger.info("Seems like we have reached the end of the gallery");
                    break;
                }
                String grab;
                if (request_counter == 1 || !use_ajax_requests) {
                    grab = br.getRegex("class=\"smbutton smbutton\\-green browse\\-search\\-button\"(.*?)class=\"rss\\-link\"").getMatch(0);
                    if (grab == null) {
                        grab = br.getRegex("class=\"folderview\\-art\"(.*?)class=\"rss\\-link\"").getMatch(0);
                    }
                    if (grab == null) {
                        /* 2017-02-01: Favourites */
                        grab = br.getRegex("value=\"Search Favourites\"(.*?)class=\"footer_copyright\"").getMatch(0);
                    }
                    if (grab == null) {
                        /* 2017-02-02: Favourites */
                        grab = br.getRegex("class=\"folderview\\-top\"(.*?)class=\"footer_copyright\"").getMatch(0);
                    }
                    if (grab == null) {
                        /* 2017-02-02: Gallery */
                        grab = br.getRegex("value=\"Search Gallery\"(.*?)data\\-gmiclass=\"CCommentThread\"").getMatch(0);
                    }
                } else {
                    /* Unescape json */
                    grab = this.br.toString().replace("\\", "");
                }
                final String[] links = new Regex(grab, "\"(https?://[\\w\\.\\-]*?deviantart\\.com/(art|journal)/[\\w\\-]+)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    /* "deviation in storage" links are no links - that are empty items so there is no reason to stop. */
                    final String[] empty_links = br.getRegex("class=\"(instorage)\"").getColumn(0);
                    if (empty_links != null && empty_links.length > 0) {
                        logger.info("This offset only contains dummy links --> Continuing");
                        continue;
                    }
                    if (timesNoItems <= times_No_Items_Max) {
                        /* No items on this page but maybe later. */
                        logger.info("Current offset contains no items: " + currentOffset);
                        timesNoItems++;
                        continue;
                    } else {
                        /* We went too far - we should already have all links (or this is a fatal error situation) --> Stop [fail safe] */
                        logger.info("No items found on " + times_No_Items_Max + " pages in a row --> Stopping");
                        break;
                    }
                } else {
                    /* Reset no-items counter as we found items. */
                    timesNoItems = 0;
                }
                for (final String artlink : links) {
                    final DownloadLink fina = createDownloadlink(artlink);
                    if (fastLinkCheck) {
                        fina.setAvailable(true);
                    }
                    /* No reason to hide their single links */
                    fina.setContentUrl(artlink);
                    if (fp != null) {
                        fina._setFilePackage(fp);
                    }
                    if (forceHtmlDownload) {
                        fina.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.HTML);
                    } else {
                        fina.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                    }
                    distribute(fina);
                }
            } finally {
                currentOffset += offsetIncrease;
                request_counter++;
            }
            if (startOffsetGiven && crawlGivenOffsetsIndividually) {
                logger.info("Star-toffset given and crawled and user set plugin settings to stop after crawling individual offsets");
                break;
            }
            /* Really make sure that we're not ending up in an infinite loop! */
        } while (has_more || maxOffset > 0 && currentOffset >= maxOffset);
    }

    private void accessOffset(final int offset) throws IOException {
        if (parameter.contains("?")) {
            br.getPage(parameter + "&offset=" + offset);
        } else {
            br.getPage(parameter + "?offset=" + offset);
        }
    }

    private String getOffsetFromURL() {
        return new Regex(parameter, "offset=(\\d+)").getMatch(0);
    }

    private String getSiteUsername() {
        return br.getRegex("name=\"username\" value=\"([^<>\"]*?)\"").getMatch(0);
    }

    private String getURLUsername() {
        return new Regex(parameter, "https?://(?:www\\.)?([A-Za-z0-9\\-]+)\\.deviantart.com/.+").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}