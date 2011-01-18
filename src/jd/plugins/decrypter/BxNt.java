//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "box.net" }, urls = { "http://www\\.box\\.net/shared/(\\w+\\b(?<!\\bstatic))(/rss\\.xml|#\\w*)?" }, flags = { 0 })
public class BxNt extends PluginForDecrypt {
    private static final String BASE_URL_PATTERN = "(http://www\\.box\\.net/shared/\\w+)(#\\w*)?";

    private static final Pattern FEED_FILEINFO_PATTERN = Pattern.compile("<item>(.*?)<\\/item>", Pattern.DOTALL);
    private static final Pattern FEED_FILETITLE_PATTERN = Pattern.compile("<title>(.*?)<\\/title>", Pattern.DOTALL);
    private static final Pattern FEED_DL_LINK_PATTERN = Pattern.compile("<media:content url=\\\"(.*?)\\\"\\s*/>", Pattern.DOTALL);

    private static final Pattern SINGLE_DOWNLOAD_LINK_PATTERN = Pattern.compile("(http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&amp;file_id=.+?\\&amp;shared_name=\\w+)");

    public BxNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        logger.finer("Decrypting: " + parameter.getCryptedUrl());
        // check if page is a rss feed
        if (parameter.toString().endsWith("rss.xml")) { return feedExists(parameter.toString()) ? decryptFeed(parameter.toString()) : null; }
        // String alllinks[] =
        // br.getRegex(",\"shared_link\":\"(.*?)\"").getColumn(0);
        // if it's not a rss feed, check if an rss feed exists
        String baseUrl = new Regex(parameter, BASE_URL_PATTERN).getMatch(0);
        String feedUrl = baseUrl + "/rss.xml";
        if (feedExists(feedUrl)) { return decryptFeed(feedUrl); }

        // there is no feed -> it's a single file download
        return decryptSingleDLPage(parameter.getCryptedUrl());
    }

    /**
     * Extracts the download link from a single file download page.
     * 
     * @param cryptedUrl
     *            the url of the download page
     * @return a list that contains the extracted download link, null if the
     *         download link couldn't be extracted.
     * @throws IOException
     */
    private ArrayList<DownloadLink> decryptSingleDLPage(String cryptedUrl) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(cryptedUrl);
        String decryptedLink = br.getRegex(SINGLE_DOWNLOAD_LINK_PATTERN).getMatch(0);
        decryptedLink = decryptedLink.replace("amp;", "");
        if (decryptedLink == null) return null;

        decryptedLink = Encoding.htmlDecode(decryptedLink);
        decryptedLinks.add(createDownloadlink(decryptedLink));

        return decryptedLinks;
    }

    /**
     * Extracts download links from a box.net rss feed.
     * 
     * @param feedUrl
     *            the url of the rss feed
     * @return a list of decrypted links, null if the links could not be
     *         extracted.
     * @throws IOException
     */
    private ArrayList<DownloadLink> decryptFeed(String feedUrl) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(feedUrl);
        String title = br.getRegex(FEED_FILETITLE_PATTERN).getMatch(0);
        String[] folder = br.getRegex(FEED_FILEINFO_PATTERN).getColumn(0);
        if (folder == null) return null;
        for (String fileInfo : folder) {
            String dlUrl = new Regex(fileInfo, FEED_DL_LINK_PATTERN).getMatch(0);
            if (dlUrl == null) {
                logger.info("Couldn't find download link. Skipping file.");
                continue;
            }
            // These ones are direct links so let's handle them as directlinks^^
            dlUrl = "directhttp://" + dlUrl.replace("amp;", "");
            logger.finer("Found link in rss feed: " + dlUrl);
            DownloadLink dl = createDownloadlink(dlUrl);
            decryptedLinks.add(dl);
            if (title != null) {
                FilePackage filePackage = FilePackage.getInstance();
                filePackage.setName(title);
                filePackage.add(dl);
            }
        }
        logger.info("Found " + decryptedLinks.size() + " links in feed: " + feedUrl);
        return decryptedLinks;
    }

    /**
     * Checks if a rss feed exists.
     * 
     * @param feedUrl
     *            the url of the rss feed
     * @return true if the feed exists, false otherwise
     * @throws IOException
     */
    private boolean feedExists(String feedUrl) throws IOException {
        br.getPage(feedUrl);
        return !br.containsHTML("RSS channel not found");
    }

}
