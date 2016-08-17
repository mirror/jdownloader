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
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/(?!art/|status/)[^<>\"]+" }) 
public class DevArtCm extends PluginForDecrypt {

    /**
     * @author raztoki
     */
    public DevArtCm(PluginWrapper wrapper) {
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

    private static Object         LOCK             = new Object();

    private static final String   FASTLINKCHECK_2  = "FASTLINKCHECK_2";
    private static final String   TYPE_COLLECTIONS = "https?://[\\w\\.\\-]*?deviantart\\.com/.*?/collections(/.+)?";
    private static final String   TYPE_CATPATH_ALL = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath(=.+)?";
    private static final String   TYPE_CATPATH_1   = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath(=(/|%2F([a-z0-9]+)?|[a-z0-9]+)(\\&offset=\\d+)?)?";
    private static final String   TYPE_CATPATH_2   = "https?://[\\w\\.\\-]*?deviantart\\.com/(gallery|favourites)/\\?catpath=[a-z0-9]{1,}(\\&offset=\\d+)?";
    private static final String   TYPE_JOURNAL     = "https?://[\\w\\.\\-]*?deviantart\\.com/journal.+";
    private static final String   LINKTYPE_JOURNAL = "https?://[\\w\\.\\-]*?deviantart\\.com/journal/[\\w\\-]+/?";
    private static final String   TYPE_BLOG        = "https?://[\\w\\.\\-]*?deviantart\\.com/blog/(\\?offset=\\d+)?";

    // private static final String TYPE_INVALID = "https?://[\\w\\.\\-]*?deviantart\\.com/stats/*?";

    final ArrayList<DownloadLink> decryptedLinks   = new ArrayList<DownloadLink>();

    private String                parameter        = null;
    private boolean               fastLinkCheck    = false;

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        JDUtilities.getPluginForHost("deviantart.com");
        jd.plugins.hoster.DeviantArtCom.prepBR(this.br);
        fastLinkCheck = SubConfiguration.getConfig("deviantart.com").getBooleanProperty(FASTLINKCHECK_2, false);
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
            decryptedLinks.add(journal);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException be) {
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("The page you were looking for doesn\\'t exist\\.") || br.getURL().matches("https?://([A-Za-z0-9]+\\.)?deviantart\\.com/browse/.+")) {
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
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
        if (decryptedLinks.size() == 0) {
            logger.info("Link probably offline: " + parameter);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void decryptJournals() throws DecrypterException, IOException {
        if (br.containsHTML("class=\"empty\\-state journal\"")) {
            this.decryptedLinks.add(this.createOfflinelink(parameter));
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
            final String jinfo[] = br.getRegex("data\\-deviationid=\"\\d+\" href=\"(http://[\\w\\.\\-]*?\\.deviantart\\.com/journal/[\\w\\-]+)\"").getColumn(0);
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
                dl._setFilePackage(fp);
                distribute(dl);
                decryptedLinks.add(dl);
            }
            next = br.getRegex("class=\"next\"><a class=\"away\" data\\-offset=\"(\\d+)\"").getMatch(0);
            previousOffset = currentOffset;
            if (stop_after_first_run) {
                logger.info("Decrypted given offset, stopping...");
                break;
            }
        } while (next != null);
        fp.addLinks(decryptedLinks);
    }

    private void decryptCollections() throws DecrypterException {
        final String[] links = br.getRegex("<a href=\"(http://[^<>\"/]+\\.deviantart\\.com/(art|journal)/[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Plugin broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (final String aLink : links) {
            final DownloadLink dl = createDownloadlink(aLink);
            if (fastLinkCheck) {
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
        }
    }

    private void decryptBlog() throws DecrypterException, IOException {
        if (br.containsHTML(">Sorry\\! This blog entry cannot be displayed")) {
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        }
        String fpName = br.getRegex("name=\"og:title\" content=\"([^<>\"]*?) on DeviantArt\"").getMatch(0);
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
            final String[] links = br.getRegex("<a href=\"(http://[^<>\"/]+\\.deviantart\\.com/journal/[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Plugin broken for link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            for (final String aLink : links) {
                final DownloadLink dl = createDownloadlink(aLink);
                if (fastLinkCheck) {
                    dl.setAvailable(true);
                }
                decryptedLinks.add(dl);
            }
            currentOffset += 5;
        } while (br.containsHTML("class=\"next\"><a class=\"away\" data\\-offset=\"\\d+\"") && !stop_after_first_run);
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName).trim();
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName + " - Journal");
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(decryptedLinks);
        }
    }

    private void decryptStandard() throws DecrypterException, IOException {
        if (br.containsHTML("class=\"empty\\-state gallery\"|class=\"empty\\-state faves\"")) {
            this.decryptedLinks.add(this.createOfflinelink(parameter));
            return;
        }
        /* Correct input links */
        if (parameter.matches("http://[^<>\"/]+\\.deviantart\\.com/gallery/\\?\\d+")) {
            final Regex paramregex = new Regex(parameter, "(http://[^<>\"/]+\\.deviantart\\.com/gallery/\\?)(\\d+)");
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
        final int offsetIncrease = 24;
        int counter = 1;
        if (parameter.contains("offset=")) {
            final int offsetLink = Integer.parseInt(new Regex(parameter, "(\\d+)$").getMatch(0));
            currentOffset = offsetLink;
            maxOffset = offsetLink;
        } else if (!parameter.matches(TYPE_CATPATH_1)) {
            final String[] offsets = br.getRegex("data\\-offset=\"(\\d+)\" name=\"gmi\\-GPageButton\"").getColumn(0);
            if (offsets != null && offsets.length != 0) {
                for (final String offset : offsets) {
                    final int offs = Integer.parseInt(offset);
                    if (offs > maxOffset) {
                        maxOffset = offs;
                    }
                }
            }
        }
        FilePackage fp = null;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.setProperty("ALLOW_MERGE", true);
        }
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return;
            }
            logger.info("Decrypting offset " + currentOffset + " of " + maxOffset);
            if (parameter.matches(TYPE_CATPATH_1) && !parameter.contains("offset=")) {
                if (counter > 1) {
                    br.getPage(parameter + "&offset=" + currentOffset);
                }
                // catpath links have an unknown end-offset
                final String nextOffset = br.getRegex("\\?catpath=[A-Za-z0-9%]+\\&amp;offset=(\\d+)\"><span>Next</span></a>").getMatch(0);
                if (nextOffset != null) {
                    maxOffset = Integer.parseInt(nextOffset);
                }
            } else if (counter > 1) {
                accessOffset(currentOffset);
            }
            if (this.br.containsHTML("475970334")) {
                logger.info("WTF");
            }
            try {
                final String grab = br.getRegex("<smoothie q=(.*?)(class=\"folderview-bottom\"></div>|div id=\"gallery_pager\")").getMatch(0);
                String[] links = new Regex(grab, "\"(https?://[\\w\\.\\-]*?deviantart\\.com/(art|journal)/[\\w\\-]+)\"").getColumn(0);
                if ((links == null || links.length == 0) && counter == 1) {
                    logger.warning("Possible Plugin error, with finding /(art|journal)/ links: " + parameter);
                    throw new DecrypterException("Decrypter broken for link: " + parameter);
                } else if (links == null || links.length == 0) {
                    /* "deviation in storage" links are no links - that are empty items so there is no reason to stop. */
                    final String[] empty_links = br.getRegex("class=\"(instorage)\"").getColumn(0);
                    if (empty_links != null && empty_links.length > 0) {
                        logger.info("This offset only contains dummy links --> Continuing");
                        continue;
                    }
                    /* We went too far - we should already have links */
                    logger.info("Current offset contains no links --> Stopping");
                    break;
                }
                if (links != null && links.length != 0) {
                    for (final String artlink : links) {
                        final DownloadLink fina = createDownloadlink(artlink);
                        if (fastLinkCheck) {
                            fina.setAvailable(true);
                        }
                        /* No reason to hide their single links */
                        fina.setContentUrl(artlink);
                        if (fp != null) {
                            fp.add(fina);
                        }
                        fina.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
                        distribute(fina);
                        decryptedLinks.add(fina);
                    }
                }
            } finally {
                currentOffset += offsetIncrease;
                counter++;
            }
            /* Really make sure that we're not ending up in an infinite loop! */
        } while (currentOffset <= maxOffset && br.containsHTML("class=\"next\"><a class=\"away\" data\\-offset=\"\\d+\""));
        if (fpName != null) {
            fp.addLinks(decryptedLinks);
        }
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
        return new Regex(parameter, "http://(?:www\\.)?([A-Za-z0-9\\-]+)\\.deviantart.com/.+").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}