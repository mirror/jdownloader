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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.download.HashInfo;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.FourChanConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "boards.4chan.org" }, urls = { "https?://[\\w\\.]*?boards\\.(?:4chan|4channel)\\.org/[0-9a-z]{1,}/(thread/\\d+)?" })
public class Brds4Chnrg extends PluginForDecrypt {
    public Brds4Chnrg(PluginWrapper wrapper) {
        super(wrapper);
        try {
            /* 2020-11-19: https://github.com/4chan/4chan-API#api-rules */
            Browser.setRequestIntervalLimitGlobal(API_BASE, true, 1250);
        } catch (final Throwable e) {
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-11-19: Preventive measure */
        return 1;
    }

    private static final String TYPE_CATEGORY = "https?://[\\w\\.]*?boards\\.[^/]+/([0-9a-z]{1,})/$";
    private static final String TYPE_THREAD   = "https?://[\\w\\.]*?boards\\.[^/]+/([0-9a-z]{1,})/thread/(\\d+)$";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks;
        final boolean useAPI = true;
        if (param.getCryptedUrl().matches(TYPE_THREAD)) {
            if (useAPI) {
                decryptedLinks = crawlSingleThreadAPI(param);
            } else {
                decryptedLinks = crawlSingleThreadWebsite(param);
            }
        } else {
            if (useAPI) {
                decryptedLinks = crawlBoardThreadsAPI(param);
            } else {
                decryptedLinks = crawlBoardThreadsWebsite(param);
            }
        }
        return decryptedLinks;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlBoardThreadsWebsite(final CryptedLink param) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        String[] threads = br.getRegex("\\[<a href=\"thread/(\\d+)").getColumn(0);
        for (String thread : threads) {
            decryptedLinks.add(createDownloadlink(param.getCryptedUrl() + "thread/" + thread));
        }
        return decryptedLinks;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlSingleThreadWebsite(final CryptedLink param) throws IOException, PluginException {
        final String boardShortTitle = new Regex(param.getCryptedUrl(), TYPE_THREAD).getMatch(0);
        final String threadID = new Regex(param.getCryptedUrl(), TYPE_THREAD).getMatch(1);
        if (boardShortTitle == null || threadID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        final boolean preferServerFilenames = PluginJsonConfig.get(this.getConfigInterface()).isPreferServerFilenamesOverPluginDefaultFilenames();
        final String IMAGERDOMAINS = "(i\\.4cdn\\.org|is\\d*?\\.4chan\\.org|images\\.4chan\\.org)";
        String[] images = br.getRegex("(?i)File: <a (title=\"[^<>\"/]+\" )?href=\"(//" + IMAGERDOMAINS + "/[0-9a-z]{1,}/(src/)?\\d+\\.(gif|jpg|png|webm))\"").getColumn(1);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 - Not Found")) {
            fp.setName("4chan - 404 - Not Found");
            br.getPage("//sys.4chan.org/error/404/rid.php");
            String image404 = br.getRegex("(https?://.+)").getMatch(0);
            DownloadLink dl = createDownloadlink(image404);
            dl.setAvailableStatus(AvailableStatus.TRUE);
            fp.add(dl);
            decryptedLinks.add(dl);
        } else if (images.length == 0) {
            logger.info("Empty 4chan link: " + param.getCryptedUrl());
            return decryptedLinks;
        } else {
            final String boardLongTitle = br.getRegex("<div class=\"boardTitle\"[^>]*>/(?:.{1,4}|trash)/\\s*-\\s*(.*?)\\s*</div>").getMatch(0);
            if (boardLongTitle != null) {
                fp.setName("4chan.org" + " - " + boardLongTitle + " - " + threadID);
            } else {
                fp.setName("4chan.org" + " - " + boardShortTitle + " - " + threadID);
            }
            /* First post = "postContainer opContainer", all others = "postContainer replyContainer" */
            final String[] posts = br.getRegex("<div class=\"postContainer [^\"]+\".*?</blockquote></div></div>").getColumn(-1);
            for (final String post : posts) {
                String url = new Regex(post, "<a[^>]*href=\"((//|http)[^\"]+)\"").getMatch(0);
                if (url == null) {
                    continue;
                } else {
                    url = br.getURL(url).toString();
                }
                final DownloadLink dl = this.createDownloadlink(url);
                dl.setAvailable(true);
                String filename = new Regex(post, "<a title=\"([^\"]+)\" href=\"").getMatch(0);
                if (filename == null) {
                    filename = new Regex(post, "target=\"_blank\">\\s*([^<>\"]+)\\s*</a>").getMatch(0);
                }
                /* Set no name if user prefers server-filenames --> These will be auto-set on downloadstart */
                if (filename != null && !preferServerFilenames) {
                    dl.setForcedFileName(Encoding.htmlDecode(filename).trim());
                }
                final String filesizeStr = new Regex(post, "\\((\\d+[^<>\"]+), \\d+x\\d+\\)").getMatch(0);
                if (filesizeStr != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                }
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
            }
            if (decryptedLinks.size() == 0) {
                /* Fallback - old method which was used until rev 42702 */
                for (String image : images) {
                    image = br.getURL(image).toString();
                    DownloadLink dl = createDownloadlink(image);
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                    fp.add(dl);
                    decryptedLinks.add(dl);
                }
            }
        }
        return decryptedLinks;
    }

    /************************************************** API methods here ****************************************************/
    /*
     * API does not return long title when crawling a thread --> Cache all once per session / if current short-title is not yet in that list
     * --> Ideally once once per session!
     */
    private static LinkedHashMap<String, String> BOARD_LONG_TITLE_CACHE = new LinkedHashMap<String, String>() {
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 200;
        };
    };
    /** Docs: https://github.com/4chan/4chan-API */
    private static final String                  API_BASE               = "https://a.4cdn.org/";

    private Browser prepBrAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    /**
     * See: https://github.com/4chan/4chan-API/blob/master/pages/Threadlist.md
     *
     * @throws PluginException
     */
    private ArrayList<DownloadLink> crawlBoardThreadsAPI(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String boardShortTitle = new Regex(param.getCryptedUrl(), TYPE_CATEGORY).getMatch(0);
        if (boardShortTitle == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(API_BASE + boardShortTitle + "/threads.json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        Map<String, Object> entries = null;
        final List<Object> pages = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
        final int maxPage = PluginJsonConfig.get(this.getConfigInterface()).getCategoryCrawlerPageLimit();
        for (final Object pageO : pages) {
            entries = (Map<String, Object>) pageO;
            final int currentPage = ((Number) entries.get("page")).intValue();
            final List<Object> threads = (List<Object>) entries.get("threads");
            for (final Object threadO : threads) {
                entries = (Map<String, Object>) threadO;
                final long threadID = ((Number) entries.get("no")).longValue();
                decryptedLinks.add(this.createDownloadlink("https://boards.4channel.org/" + boardShortTitle + "/thread/" + threadID));
            }
            if (currentPage >= maxPage) {
                logger.info("Stopping because reached max. user defined page: " + maxPage);
                break;
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Category has no threads at all??");
        }
        return decryptedLinks;
    }

    /** See: https://github.com/4chan/4chan-API/blob/master/pages/Threads.md */
    private ArrayList<DownloadLink> crawlSingleThreadAPI(final CryptedLink param) throws IOException, PluginException {
        String boardLongTitle = null;
        final String boardShortTitle = new Regex(param.getCryptedUrl(), TYPE_THREAD).getMatch(0);
        final String threadID = new Regex(param.getCryptedUrl(), TYPE_THREAD).getMatch(1);
        if (boardShortTitle == null || threadID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBrAPI(this.br);
        Map<String, Object> entries;
        /* We need this because the other method doesn't return the long title of our board. */
        synchronized (BOARD_LONG_TITLE_CACHE) {
            /** See: https://github.com/4chan/4chan-API/blob/master/pages/Boards.md */
            if (!BOARD_LONG_TITLE_CACHE.containsKey(boardShortTitle)) {
                logger.info("Updating board cache because it doesn't contain the long title for: " + boardShortTitle);
                br.getPage(API_BASE + "/boards.json");
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final List<Object> boards = (List<Object>) entries.get("boards");
                for (final Object boardO : boards) {
                    entries = (Map<String, Object>) boardO;
                    final String boardShortTitleTmp = (String) entries.get("board");
                    final String boardLongTitleTmp = (String) entries.get("title");
                    if (StringUtils.isEmpty(boardShortTitleTmp) || StringUtils.isEmpty(boardLongTitleTmp)) {
                        /* Skip invalid items */
                        continue;
                    }
                    BOARD_LONG_TITLE_CACHE.put(boardShortTitleTmp, boardLongTitleTmp);
                }
                if (!BOARD_LONG_TITLE_CACHE.containsKey(boardShortTitle)) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        boardLongTitle = BOARD_LONG_TITLE_CACHE.get(boardShortTitle);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(API_BASE + boardShortTitle + "/thread/" + threadID + ".json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final boolean preferServerFilenames = PluginJsonConfig.get(this.getConfigInterface()).isPreferServerFilenamesOverPluginDefaultFilenames();
        entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Object> posts = (List<Object>) entries.get("posts");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("4chan.org" + " - " + boardLongTitle + " - " + threadID);
        for (final Object postO : posts) {
            entries = (Map<String, Object>) postO;
            final String md5_base64O = (String) entries.get("md5");
            if (md5_base64O == null) {
                /* Skip posts with no file attached */
                continue;
            }
            final long tim = ((Number) entries.get("tim")).longValue();
            String filename = (String) entries.get("filename");
            if (StringUtils.isEmpty(filename)) {
                filename = String.valueOf(tim);
            }
            String ext = (String) entries.get("ext");
            if (StringUtils.isEmpty(ext)) {
                ext = "";
            }
            final long fsize = ((Number) entries.get("fsize")).longValue();
            /* https://github.com/4chan/4chan-API/blob/master/pages/User_images_and_static_content.md */
            final DownloadLink dl = this.createDownloadlink("https://i.4cdn.org/" + boardShortTitle + "/" + tim + ext);
            final String setFileName;
            if (preferServerFilenames) {
                setFileName = tim + ext;
            } else {
                setFileName = filename + ext;
            }
            dl.setHashInfo(HashInfo.parse(HexFormatter.byteArrayToHex(Base64.decode(md5_base64O))));
            dl.setFinalFileName(setFileName);
            dl.setProperty("fixName", setFileName);
            dl.setVerifiedFileSize(fsize);
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() == 0) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), threadID + "_Thread doesn't contain any media", threadID + "_Thread doesn't contain any media"));
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public Class<? extends FourChanConfig> getConfigInterface() {
        return FourChanConfig.class;
    }
}