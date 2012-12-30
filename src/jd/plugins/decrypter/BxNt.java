//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "box.net" }, urls = { "https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)/(shared|s)/((\\w+\\b(?<!\\bstatic))(/rss\\.xml|#\\w*)?|[a-z0-9]{16})" }, flags = { 0 })
public class BxNt extends PluginForDecrypt {
    private static final String  BASE_URL_PATTERN             = "(https?://(www|[a-z0-9\\-_]+)\\.box\\.com/shared/\\w+)(#\\w*)?";
    private static final Pattern FEED_FILEINFO_PATTERN        = Pattern.compile("<item>(.*?)<\\/item>", Pattern.DOTALL);
    private static final Pattern FEED_FILETITLE_PATTERN       = Pattern.compile("<title>(.*?)<\\/title>", Pattern.DOTALL);
    private static final Pattern FEED_DL_LINK_PATTERN         = Pattern.compile("<media:content url=\\\"(.*?)\\\"\\s*/>", Pattern.DOTALL);
    private static final Pattern SINGLE_DOWNLOAD_LINK_PATTERN = Pattern.compile("(https?://(www|[a-z0-9\\-_]+)\\.box\\.com/index\\.php\\?rm=box_download_shared_file\\&amp;file_id=.+?\\&amp;shared_name=\\w+)");
    private static final String  ERROR                        = "(<h2>The webpage you have requested was not found\\.</h2>|<h1>404 File Not Found</h1>|Oops &mdash; this shared file or folder link has been removed\\.|RSS channel not found)";

    public BxNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String cryptedlink = parameter.toString().replace("box.net/", "box.com/").replace("box.com/s/", "box.com/shared/");
        logger.finer("Decrypting: " + cryptedlink);
        // check if page is a rss feed
        if (cryptedlink.endsWith("rss.xml")) {
            if (feedExists(cryptedlink)) {
                decryptFeed(cryptedlink);
            } else {
                logger.warning("Invalid URL or URL no longer exists : " + cryptedlink);
            }
        } else {
            // String alllinks[] = br.getRegex(",\"shared_link\":\"(.*?)\"").getColumn(0);
            // if it's not a rss feed, check if an rss feed exists
            String baseUrl = new Regex(cryptedlink, BASE_URL_PATTERN).getMatch(0);
            String feedUrl = baseUrl + "/rss.xml";
            if (feedExists(feedUrl)) {
                decryptedLinks = decryptFeed(feedUrl);
            } else {
                logger.info("RSS page contains 'ERRORS'");
            }
            if (decryptedLinks.size() == 0) {
                logger.info("Haven't found any links to decrypt, now trying decryptSingleDLPage");
                decryptSingleDLPage(cryptedlink);
            }
        }
        return decryptedLinks;
    }

    /**
     * Extracts the download link from a single file download page.
     * 
     * @param cryptedUrl
     *            the url of the download page
     * @return a list that contains the extracted download link, null if the download link couldn't be extracted.
     * @throws IOException
     */
    private ArrayList<DownloadLink> decryptSingleDLPage(String cryptedUrl) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(cryptedUrl);
        String decryptedLink = br.getRegex(SINGLE_DOWNLOAD_LINK_PATTERN).getMatch(0);
        if (decryptedLink == null) {
            logger.warning("Couldn't find any single links to decrypt. " + cryptedUrl);
            return null;
        }
        decryptedLink = Encoding.htmlDecode(decryptedLink);
        decryptedLinks.add(createDownloadlink(decryptedLink.replaceAll("box\\.(net|com)/s", "boxdecrypted.com/s")));
        return decryptedLinks;
    }

    /**
     * Extracts download links from a box.net rss feed.
     * 
     * @param feedUrl
     *            the url of the rss feed
     * @return a list of decrypted links, null if the links could not be extracted.
     * @throws IOException
     */
    private ArrayList<DownloadLink> decryptFeed(String feedUrl) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
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

    /** Maybe useful in the future */
    // private ArrayList<DownloadLink> decryptFolderAndSingleLink(String cryptedUrl) throws IOException {
    // ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    // br.getPage(cryptedUrl);
    // /**
    // * If it's a folder, decrypt all links, if it's a file just return the link for the hosterplugin
    // */
    // if
    // (br.containsHTML("(Folder Shared from Box|flash_folder = \\'/static/flash/|page = \\'folder\\'|\"type\":\"folder\"|\"is_network_folder\")"))
    // {
    // final String correctedBR = br.toString().replace("\\", "");
    // String[] fileIDs = new Regex(correctedBR, "\"shared_link\":\"http://www\\.box\\.com/file/(\\d+)/encoded").getColumn(0);
    // if (fileIDs == null || fileIDs.length == 0) {
    // fileIDs = new Regex(correctedBR, "\"link_info_(\\d+)\"").getColumn(0);
    // if (fileIDs == null || fileIDs.length == 0) {
    // fileIDs = new Regex(correctedBR, "\"id\":\"(\\d+)\"").getColumn(0);
    // }
    // }
    // final String folderID = new Regex(cryptedUrl, "box\\.com/s/(.+)").getMatch(0);
    // String folderID2 = new Regex(correctedBR, "start_item = \\'d_(\\d+)\\'").getMatch(0);
    // if (folderID2 == null) {
    // folderID2 = new Regex(correctedBR, "\\{\"p_(\\d+)\"").getMatch(0);
    // if (folderID2 == null) {
    // folderID2 = new Regex(correctedBR, "\"shared_item\":\"(\\d+)\"").getMatch(0);
    // }
    // }
    // if (folderID == null || folderID2 == null || (fileIDs == null ||
    // fileIDs.length == 0)) {
    // logger.warning("Decrypter broken for link: " + cryptedUrl);
    // return null;
    // }
    // for (String fileID : fileIDs) {
    // decryptedLinks.add(createDownloadlink("http://www.box.com/shared/" + folderID + "/1/" + folderID2 + "/" + fileID));
    // }
    // } else {
    // decryptedLinks.add(createDownloadlink(cryptedUrl.replace("box.net/s/", "boxdecrypted.net/s/")));
    // }
    // return decryptedLinks;
    // }

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
        return !br.containsHTML(ERROR);
    }

}
