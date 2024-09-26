//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.ImgSrcRu;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class ImgSrcRuCrawler extends PluginForDecrypt {
    // dev notes
    // &pwd= is a md5 hash id once you've provided password for that album.
    public ImgSrcRuCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imgsrc.ru", "imgsrc.su", "imgsrc.ro" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(main/.+|[^/]+/\\d+\\.html)");
        }
        return ret.toArray(new String[0]);
    }

    private String        password     = null;
    private String        username     = null;
    private String        pwd          = null;
    private PluginForHost plugin       = null;
    private List<String>  passwords    = null;
    private long          startTime;
    private final String  PATTERN_USER = "(?i)https?://[^/]+/main/user\\.php\\?user=([^\\&]+)";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    private Browser prepBrowser(final Browser prepBr, final Boolean neu) throws PluginException {
        if (plugin == null) {
            plugin = this.getNewPluginForHostInstance(this.getHost());
            if (plugin == null) {
                throw new IllegalStateException("imgsrc.ru hoster plugin not found!");
            }
        }
        return ((jd.plugins.hoster.ImgSrcRu) plugin).prepBrowser(prepBr);
    }

    private void setInitConstants(final CryptedLink param) {
        this.startTime = System.currentTimeMillis();
        final List<String> passwords = getPreSetPasswords();
        if (param.getDecrypterPassword() != null && !passwords.contains(param.getDecrypterPassword())) {
            passwords.add(param.getDecrypterPassword());
        }
        final String lastPass = this.getPluginConfig().getStringProperty("lastusedpassword");
        this.getPluginConfig().removeProperty("lastusedpassword");
        if (lastPass != null && !passwords.contains(lastPass)) {
            passwords.add(lastPass);
        }
        this.passwords = passwords;
    }

    private boolean tryDefaultPassword = true;

    private void tryDefaultPassword(CryptedLink param, Browser br) {
        if (tryDefaultPassword) {
            tryDefaultPassword = false;
            final String galleryName = getGalleryName(br);
            if (StringUtils.containsIgnoreCase(galleryName, "EZZE")) {
                this.passwords.add(0, "1234554321");
            } else if (StringUtils.containsIgnoreCase(galleryName, "EZE")) {
                this.passwords.add(0, "123454321");
            } else if (StringUtils.containsIgnoreCase(galleryName, "ZEZ")) {
                this.passwords.add(0, "543212345");
            } else if (StringUtils.containsIgnoreCase(galleryName, "ZE")) {
                this.passwords.add(0, "54321");
            } else if (StringUtils.containsIgnoreCase(galleryName, "0EZ6")) {
                this.passwords.add(0, "0123456");
            } else if (StringUtils.contains(galleryName, "EZ6")) {
                this.passwords.add(0, "123456");
            } else if (StringUtils.containsIgnoreCase(galleryName, "0EZ")) {
                this.passwords.add(0, "012345");
            } else if (StringUtils.containsIgnoreCase(galleryName, "EZ") || StringUtils.containsIgnoreCase(galleryName, "12345")) {
                this.passwords.add(0, "12345");
            }
        }
    }

    private String getGalleryName(final Browser br) {
        String ret = br.getRegex("from '<strong>([^\r\n]+)</strong>").getMatch(0);
        if (ret == null) {
            ret = br.getRegex("<title>(.*?)(\\s*@\\s*iMGSRC\\.RU)?</title>").getMatch(0);
            final String remove = new Regex(ret, "\\s*/\\s*([^/]*?)$").getMatch(0);
            if (remove != null && br.containsHTML("alt\\s*=\\s*'" + Pattern.quote(remove) + "'\\s*>")) {
                // remove file name that is part of album name
                ret = new Regex(ret, "(.*?)\\s*/\\s*[^/](.*?)$").getMatch(0);
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        setInitConstants(param);
        prepBrowser(br, false);
        if (param.getCryptedUrl().matches(PATTERN_USER)) {
            return crawlProfile(param);
        } else {
            return crawlGallery(param);
        }
    }

    private ArrayList<DownloadLink> crawlProfile(final CryptedLink param) throws Exception {
        final String username = new Regex(param.getCryptedUrl(), PATTERN_USER).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] urls = br.getRegex("href=.(/" + username + "/[^<>\"\\']+\\.html)").getColumn(0);
        if (urls == null || urls.length == 0) {
            if (br.containsHTML("(?i)this site does not exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        for (final String relativeURL : urls) {
            ret.add(this.createDownloadlink(br.getURL(relativeURL).toExternalForm()));
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlGallery(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        try {
            // best to get the original parameter, as the page could contain blocks due to forward or password
            String contenturl = param.getCryptedUrl();
            final boolean firstGetPageResult = getPage(contenturl, param);
            if (!firstGetPageResult) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (br._getURL().getPath().equalsIgnoreCase("/main/search.php")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            username = br.getRegex(">\\s*more\\s*photos\\s*from\\s*(.*?)\\s*<").getMatch(0);
            if (username == null) {
                username = br.getRegex(">\\s*Add\\s*(.*?)\\s*to\\s*your").getMatch(0);
                if (username == null) {
                    username = br.getRegex("/main/user\\.php\\?user=(.*?)'").getMatch(0);
                }
            }
            if (username == null) {
                /* 2024-03-19 */
                username = br.getRegex("class='tomato'[^>]*>([^<]+)</a>").getMatch(0);
            }
            if (username == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            username = Encoding.htmlDecode(username).trim();
            final UrlQuery query = UrlQuery.parse(contenturl);
            final String galleryTitle = getGalleryName(br);
            if (galleryTitle == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // final String album_id_old = new Regex(contenturl, "(?i)/a(\\d+)\\.html").getMatch(0);
            String aid = query.get("ad");
            if (aid == null) {
                aid = query.get("aid");
            }
            String uid = query.get("id");
            if (uid == null) {
                uid = new Regex(contenturl, "(?i)/(\\d+)\\.html").getMatch(0);
            }
            if (uid == null && aid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /**
             * 2024-03-18: Change image view to be able to find all items. </br>
             * And no, this not give us all images on one page, just lets us paginate over the gallery.
             */
            final String allImagesOnOnePage = br.getRegex("'(/main/tape\\.php\\?aid=\\d+&id=\\d+&pwd=[^']*)'").getMatch(0);
            if (allImagesOnOnePage != null && !br.getURL().endsWith(allImagesOnOnePage)) {
                getPage(allImagesOnOnePage, param);
            }
            final String title = Encoding.htmlDecode(username.trim()) + " @ " + Encoding.htmlDecode(galleryTitle).trim();
            final FilePackage fp = FilePackage.getInstance();
            fp.setAllowMerge(true);
            fp.setName(Encoding.htmlDecode(title).trim());
            final String quotedUsername = Pattern.quote(username);
            final Set<String> pagesDone = new HashSet<String>();
            final List<String> pagesTodo = new ArrayList<String>();
            int page = 1;
            pagination: do {
                final int numberofResultsOld = ret.size();
                final ArrayList<DownloadLink> thisPageResults = this.crawlImages(param);
                for (final DownloadLink result : thisPageResults) {
                    result.setProperty("username", username);
                    if (galleryTitle != null) {
                        result.setProperty("gallery", galleryTitle.trim());
                    }
                    result._setFilePackage(fp);
                    distribute(result);
                }
                ret.addAll(thisPageResults);
                final int numberofNewItemsThisPage = ret.size() - numberofResultsOld;
                logger.info("Crawled page " + page + " | New items this page: " + numberofNewItemsThisPage + " | Total: " + ret.size());
                if (numberofNewItemsThisPage == 0) {
                    if (ret.isEmpty()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Found zero new results");
                    } else {
                        logger.info("Stopping because: Found zero new results");
                        break pagination;
                    }
                }
                /* Look for next page */
                final String nextPage = br.getRegex("<a [^>]*href=\\s*(\"|'|)?(/" + quotedUsername + "/\\d+\\.html(?:\\?pwd=[a-z0-9]{32}[^>]*?|\\?)?)\\1\\s*>\\s*(▶|&#9654;?|&#9658;?|<i\\s*class\\s*=\\s*\"material-icons\"\\s*>\\s*&#xe5cc;?)").getMatch(1);
                final String previousPage = br.getRegex("<a [^>]*href=\\s*(\"|'|)?(/" + quotedUsername + "/\\d+\\.html(?:\\?pwd=[a-z0-9]{32}[^>]*?|\\?)?)\\1\\s*>\\s*(◄|&#9664;?|&#9668;?|<i\\s*class\\s*=\\s*\"material-icons\"\\s*>\\s*&#xe5cb;?)").getMatch(1);
                if (previousPage != null && !pagesTodo.contains(previousPage) && !pagesDone.contains(previousPage)) {
                    pagesTodo.add(previousPage);
                }
                if (nextPage != null && !pagesTodo.contains(nextPage) && !pagesDone.contains(nextPage)) {
                    pagesTodo.add(nextPage);
                }
                final String[] allPossibleNextPages = br.getRegex("'(\\?aid=\\d+&id=\\d+&skip=\\d+&pwd=[^']*)'").getColumn(0);
                if (allPossibleNextPages != null && allPossibleNextPages.length > 0) {
                    for (final String possibleNextPage : allPossibleNextPages) {
                        if (!pagesTodo.contains(previousPage) && !pagesDone.contains(possibleNextPage)) {
                            pagesTodo.add(possibleNextPage);
                        }
                    }
                }
                nextpagefinder: while (true) {
                    if (pagesTodo.size() > 0) {
                        final String thisNextPage = pagesTodo.remove(0);
                        if (pagesDone.add(thisNextPage)) {
                            /* Continue to next page */
                            getPage(thisNextPage, param);
                            break nextpagefinder;
                        } else {
                            logger.info("Stopping because: Failed to find any new next page");
                            break pagination;
                        }
                    } else {
                        logger.info("Stopping because: Failed to find any next page");
                        break pagination;
                    }
                }
                /* Continue to crawl next page */
                page++;
            } while (!this.isAbort());
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            logger.info("Time to crawl : " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds. Returning " + ret.size() + " DownloadLinks for " + param.getCryptedUrl());
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlImages(final CryptedLink param) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] imageIDRegexes = new String[] { "id='p(\\d+)'", "/imgsrc\\.ru_(\\d+)" };
        final HashSet<String> allimageids = new HashSet<String>();
        for (final String imageIDRegex : imageIDRegexes) {
            final String[] imageids = br.getRegex(imageIDRegex).getColumn(0);
            if (imageids == null || imageids.length == 0) {
                continue;
            }
            for (final String imageID : imageids) {
                allimageids.add(imageID);
            }
        }
        if (allimageids.isEmpty()) {
            logger.warning("Possible plugin error: Please confirm in your webbrowser that this album " + param.getCryptedUrl() + " contains more than one image. If it does please report this issue to JDownloader Development Team.");
            return ret;
        }
        final String currentLink = br.getURL();
        final String extDefault = "." + ImgSrcRu.getPreferredImageFormat();
        for (final String imageid : allimageids) {
            final String guessedFileExtension;
            if (br.containsHTML("_" + imageid + "[A-Za-z0-9]+\\.gif'")) {
                guessedFileExtension = ".gif";
            } else {
                guessedFileExtension = extDefault;
            }
            String url = "/" + username + "/" + imageid + ".html";
            final DownloadLink img = createDownloadlink("https://decryptedimgsrc.ru" + url);
            img.setReferrerUrl(currentLink);
            img.setName(imageid + guessedFileExtension);
            img.setAvailable(true);
            if (password != null) {
                img.setDownloadPassword(password);
            }
            ret.add(img);
        }
        return ret;
    }

    private boolean isPasswordProtected(final Browser br) {
        return jd.plugins.hoster.ImgSrcRu.isPasswordProtected(br);
    }

    public static String getPage(Browser br, final String url) throws Exception {
        br.setAllowedResponseCodes(400);
        br.getPage(url);
        handleIFrameContainer(br);
        return br.toString();
    }

    public static String submitForm(Browser br, Form form) throws Exception {
        br.submitForm(form);
        handleIFrameContainer(br);
        return br.toString();
    }

    public static void handleIFrameContainer(Browser br) throws Exception {
        if (StringUtils.contains(br.getURL(), "dlp.imgsrc.ru")) {
            final String iframeContainer = br.getRegex("<div\\s*class\\s*=\\s*\"iframe-container\"\\s*>\\s*<iframe\\s*src\\s*=\\s*\"(https?://imgsrc.ru/.*?)\"").getMatch(0);
            if (iframeContainer != null) {
                br.getPage(iframeContainer);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    // TODO: reduce duplicated code with hoster
    private boolean getPage(String url, final CryptedLink param) throws Exception {
        if (url == null) {
            return false;
        }
        if (pwd != null && !url.matches(".+?pwd=[a-z0-9]{32}")) {
            url += "?pwd=" + pwd;
        }
        boolean failed = false;
        final int repeat = 4;
        for (int i = 0; i <= repeat; i++) {
            if (isAbort()) {
                throw new DecrypterException("Task Aborted");
            }
            if (failed) {
                long meep = new Random().nextInt(4) * 1000;
                Thread.sleep(meep);
                failed = false;
            }
            try {
                getPage(br, url);
                /* Adult content confirmation */
                final String over18 = br.getRegex("(/main/warn[^\"']*over18[^\"']*)").getMatch(-1);
                if (over18 != null) {
                    getPage(br, over18);
                } else if (br.containsHTML(">\\s*This album has not been checked by the moderators yet\\.|<u>\\s*Proceed at your own risk\\s*</u>")) {
                    // /main/passcheck.php?ad=\d+ links can not br.getURL + "?warned=yeah"
                    // lets look for the link
                    final String yeah = br.getRegex("/[^/]+/a\\d+\\.html\\?warned=yeah").getMatch(-1);
                    if (yeah != null) {
                        getPage(br, yeah);
                    } else {
                        // fail over
                        getPage(br, br.getURL() + "?warned=yeah");
                    }
                }
                // login required
                if (br._getURL().getPath().equalsIgnoreCase("/main/login.php")) {
                    logger.warning("You need to login! Currently not supported, ask for support to be added");
                    throw new AccountRequiredException();
                }
                // needs to be before password
                if (br.containsHTML("Continue to album(?: >>)?")) {
                    Form continueForm = br.getFormByRegex("value\\s*=\\s*'Continue");
                    boolean didSubmitContinueForm = false;
                    if (continueForm != null && !isPasswordProtected(br)) {
                        didSubmitContinueForm = true;
                        submitForm(br, continueForm);
                        continueForm = br.getFormByRegex("value\\s*=\\s*'Continue");
                    }
                    if (continueForm != null) {
                        if (isPasswordProtected(br)) {
                            tryDefaultPassword(param, br);
                            if (passwords.size() > 0) {
                                password = passwords.remove(0);
                            } else {
                                password = getUserInput("Enter password for link: " + param.getCryptedUrl(), param);
                                if (password == null || password.equals("")) {
                                    logger.info("User aborted/entered blank password");
                                    throw new DecrypterException(DecrypterException.PASSWORD);
                                }
                            }
                            continueForm.put("pwd", Encoding.urlEncode(password));
                        }
                        submitForm(br, continueForm);
                        if (isPasswordProtected(br)) {
                            password = null;
                            failed = true;
                            if (i == repeat) {
                                // using 'i' is probably not a good idea, as we could have had connection errors!
                                logger.warning("Exausted Password try");
                                throw new DecrypterException(DecrypterException.PASSWORD);
                            } else {
                                continue;
                            }
                        }
                        this.getPluginConfig().setProperty("lastusedpassword", password);
                        pwd = br.getRegex("\\?pwd=([a-z0-9]{32})").getMatch(0);
                    } else {
                        if (didSubmitContinueForm) {
                            return true;
                        } else {
                            String newLink = br.getRegex("\\((\"|')right\\1,function\\(\\) \\{window\\.location=('|\")(https?://imgsrc\\.ru/[^<>\"'/]+/[a-z0-9]+\\.html((\\?pwd=)?(\\?pwd=[a-z0-9]{32})?)?)\\2").getMatch(2);
                            if (newLink == null) {
                                /* This is also possible: "/blablabla/[0-9]+.html?pwd=&" */
                                newLink = br.getRegex("href=(/[^<>\"]+\\?pwd=[^<>\"/]*?)><br><br>Continue to album >></a>").getMatch(0);
                            }
                            if (newLink == null) {
                                logger.warning("Couldn't process Album forward: " + br.getURL());
                                return false;
                            }
                            getPage(br, newLink);
                        }
                    }
                }
                if (isPasswordProtected(br)) {
                    tryDefaultPassword(param, br);
                    Form pwForm = br.getFormbyProperty("name", "passchk");
                    if (pwForm == null) {
                        logger.warning("Decrypter broken for link: " + br.getURL());
                        return false;
                    }
                    if (passwords.size() > 0) {
                        password = passwords.remove(0);
                    } else {
                        password = getUserInput("Enter password for link: " + param.getCryptedUrl(), param);
                        if (StringUtils.isEmpty(password)) {
                            logger.info("User aborted/entered blank password");
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        }
                    }
                    pwForm.put("pwd", Encoding.urlEncode(password));
                    submitForm(br, pwForm);
                    pwForm = br.getFormbyProperty("name", "passchk");
                    if (pwForm != null) {
                        // nullify wrong storable to prevent retry loop of the same passwd multiple times.
                        password = null;
                        failed = true;
                        if (i == repeat) {
                            // using 'i' is probably not a good idea, as we could have had connection errors!
                            logger.warning("Exausted Password try");
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        } else {
                            continue;
                        }
                    }
                    this.getPluginConfig().setProperty("lastusedpassword", password);
                    pwd = br.getRegex("\\?pwd=([a-z0-9]{32})").getMatch(0);
                }
                if (br.getURL().matches("^https?://[^/]+/$")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (br.getURL().contains(url) || !failed) {
                    // because one page grab could have multiple steps, you can not break after each if statement
                    break;
                }
            } catch (final BrowserException e) {
                if (br.getHttpConnection().getResponseCode() == 410) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                failed = true;
                continue;
            }
        }
        if (failed) {
            logger.warning("Exausted retry getPage count");
            return false;
        }
        return true;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}