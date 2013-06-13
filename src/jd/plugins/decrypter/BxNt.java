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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "box.net" }, urls = { "https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)/(shared|s)/(?!static)[a-z0-9]+(/\\d+/\\d+)?" }, flags = { 0 })
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

    // /* NOTE: no override to keep compatible to old stable */
    // public int getMaxConcurrentProcessingInstances() {
    // return 1;
    // }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String cryptedlink = parameter.toString().replace("box.net/", "box.com/").replace("box.com/s/", "box.com/shared/");
        logger.finer("Decrypting: " + cryptedlink);
        // check if page is a rss feed
        br.setCookie("http://box.com", "country_code", "US");
        if (cryptedlink.endsWith("rss.xml")) {
            if (feedExists(cryptedlink)) {
                decryptFeed(cryptedlink, cryptedlink);
            } else {
                logger.warning("Invalid URL or URL no longer exists : " + cryptedlink);
            }
        } else {
            // if it's not a rss feed, check if an rss feed exists
            String baseUrl = new Regex(cryptedlink, BASE_URL_PATTERN).getMatch(0);
            String feedUrl = baseUrl + "/rss.xml";
            if (feedExists(feedUrl)) {
                decryptedLinks = decryptFeed(feedUrl, cryptedlink);
            }
            if (decryptedLinks == null || decryptedLinks.size() == 0) {
                logger.info("There is no RSS link so it might be a folder");
                br.setFollowRedirects(true);
                br.getPage(cryptedlink);
                if (br.getURL().equals("https://www.box.com/freeshare")) {
                    final DownloadLink dl = createDownloadlink(cryptedlink.replaceAll("box\\.com/shared", "boxdecrypted.com/shared"));
                    dl.setAvailable(false);
                    dl.setProperty("offline", true);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
                if (br.containsHTML("<title>Box \\| 404 Page Not Found</title>") || br.containsHTML("error_message_not_found")) {
                    final DownloadLink dl = createDownloadlink(cryptedlink.replaceAll("box\\.com/shared", "boxdecrypted.com/shared"));
                    dl.setAvailable(false);
                    dl.setProperty("offline", true);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
                String fpName = null;
                if (br.containsHTML("id=\"name\" title=\"boxdocs\"")) {
                    fpName = new Regex(cryptedlink, "([a-z0-9]+)$").getMatch(0);
                    final String textArea = br.getRegex("<tr id=\"wrp_base\" style=\"height: 100%;\">(.*?)<tr id=\"wrp_footer\">").getMatch(0);
                    if (textArea == null) {
                        logger.warning("Decrypt failed for link: " + cryptedlink);
                        return null;
                    }
                    final String[] pictureLinks = HTMLParser.getHttpLinks(textArea, "");
                    if (pictureLinks != null && pictureLinks.length != 0) {
                        for (final String pictureLink : pictureLinks) {
                            if (!pictureLink.contains("box.com/")) {
                                final DownloadLink dl = createDownloadlink("directhttp://" + pictureLink);
                                decryptedLinks.add(dl);
                            }
                        }
                    }
                } else {
                    br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                    final String pathValue = br.getRegex("\\{\"p_(\\d+)\"").getMatch(0);
                    fpName = br.getRegex("\"name\":\"([^<>\"]*?)\"").getMatch(0);
                    if (pathValue == null || fpName == null) {
                        logger.warning("Decrypt failed for link: " + cryptedlink);
                        return null;
                    }
                    final String basicLink = br.getURL().replace("/shared/", "/s/");
                    final String pageCount = br.getRegex("\"page_count\":(\\d+)").getMatch(0);
                    final String linkID = new Regex(cryptedlink, "box\\.com/shared/([a-z0-9]+)").getMatch(0);
                    int pages = 1;
                    if (pageCount != null) pages = Integer.parseInt(pageCount);
                    for (int i = 1; i <= pages; i++) {
                        logger.info("Decrypting page " + i + " of " + pages);
                        br.getPage(basicLink + "/" + i + "/" + pathValue);
                        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                        final String[] links = br.getRegex("\"nnttttdata\\-href=\"(/s/[a-z0-9]+/\\d+/\\d+/\\d+/\\d+)\"").getColumn(0);
                        final String[] filenames = br.getRegex("data\\-downloadurl=\"[a-z0-9/]+:([^<>\"]*?):https://www\\.box").getColumn(0);
                        final String[] filesizes = br.getRegex("class=\"item_size\">([^<>\"]*?)</li>").getColumn(0);
                        final String[] folderLinks = br.getRegex("\"unidb\":\"folder_(\\d+)\"").getColumn(0);
                        if ((links == null || links.length == 0 || filenames == null || filenames.length == 0 || filesizes == null || filesizes.length == 0) && (folderLinks == null || folderLinks.length == 0)) {
                            logger.warning("Decrypt failed for link: " + cryptedlink);
                            return null;
                        }
                        if (folderLinks != null && folderLinks.length != 0) {
                            for (final String folderLink : folderLinks) {
                                final DownloadLink dl = createDownloadlink("https://www.box.com/shared/" + linkID + "/1/" + folderLink);
                                decryptedLinks.add(dl);
                            }
                        }
                        if (links != null && links.length != 0 && filenames != null && filenames.length != 0 && filesizes != null && filesizes.length != 0) {
                            final int numberOfEntries = links.length;
                            if (filenames.length != numberOfEntries || filesizes.length != numberOfEntries) {
                                logger.warning("Decrypt failed for link: " + cryptedlink);
                                return null;
                            }
                            for (int count = 0; count < numberOfEntries; count++) {
                                final String url = links[count];
                                final String filename = filenames[count];
                                final String filesize = filesizes[count];
                                final DownloadLink dl = createDownloadlink("https://www.boxdecrypted.com" + url);
                                dl.setProperty("plainfilename", filename);
                                dl.setProperty("plainfilesize", filesize);
                                dl.setName(Encoding.htmlDecode(filename.trim()));
                                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                                dl.setAvailable(true);
                                decryptedLinks.add(dl);
                            }
                        }
                    }

                }
                if (fpName != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(fpName.trim()));
                    fp.addLinks(decryptedLinks);
                }
                if (decryptedLinks.size() == 0) {
                    logger.info("Haven't found any links to decrypt, now trying decryptSingleDLPage");
                    decryptedLinks.add(createDownloadlink(cryptedlink.replaceAll("box\\.com/shared", "boxdecrypted.com/shared")));
                }
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

    /**
     * Extracts download links from a box.net rss feed.
     * 
     * @param feedUrl
     *            the url of the rss feed
     * @return a list of decrypted links, null if the links could not be extracted.
     * @throws IOException
     */
    private ArrayList<DownloadLink> decryptFeed(String feedUrl, final String cryptedlink) throws IOException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("<title>Box \\| 404 Page Not Found</title>")) {
            final DownloadLink dl = createDownloadlink(cryptedlink.replaceAll("box\\.com/shared", "boxdecrypted.com/shared"));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
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
        return !br.containsHTML(ERROR);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}