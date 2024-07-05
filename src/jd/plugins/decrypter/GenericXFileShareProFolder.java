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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.FileupOrg;
import jd.plugins.hoster.TakefileLink;
import jd.plugins.hoster.UploadBoyCom;

@SuppressWarnings("deprecation")
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class GenericXFileShareProFolder extends antiDDoSForDecrypt {
    private static final String[] domains        = new String[] { "up-4.net", "up-4ever.com", "up-4ever.net", "subyshare.com", "brupload.net", "powvideo.net", "youwatch.org", "salefiles.com", "free-uploading.com", "rapidfileshare.net", "fireget.com", "mixshared.com", "novafile.com", "novafile.org", "qtyfiles.com", "free-uploading.com", "free-uploading.com", "uppit.com", "downloadani.me", "clicknupload.org", "isra.cloud", "world-files.com", "katfile.com", "filefox.cc", "cosmobox.org", "userupload.net", "tstorage.info", "fastfile.cc", "datanodes.to", "filestore.me", "ezvn.net", "filoz.net", "rapidbytez.com" };
    /* This list contains all hosts which need special Patterns (see below) - all other XFS hosts have the same folder patterns! */
    private static final String[] specialDomains = { "hotlink.cc", "ex-load.com", "imgbaron.com", "filespace.com", "spaceforfiles.com", "prefiles.com", "imagetwist.com", "file.al", "send.cm", "10gb.vn", "takefile.link" };

    public static String[] getAnnotationNames() {
        return getAllDomains();
    }

    @Override
    public String[] siteSupportedNames() {
        return getAllDomains();
    }

    private static String[] getFileUpDomains() {
        return FileupOrg.getPluginDomains().get(0);
    }

    private static String[] getUploadboyDomains() {
        return UploadBoyCom.getPluginDomains().get(0);
    }

    /* Returns Array containing all elements of domains + specialDomains. */
    public static String[] getAllDomains() {
        final List<String> ret = new ArrayList<String>();
        ret.addAll(Arrays.asList(domains));
        ret.addAll(Arrays.asList(getFileUpDomains()));
        ret.addAll(Arrays.asList(getUploadboyDomains()));
        ret.addAll(Arrays.asList(specialDomains));
        for (String[] takeFileVirtual : TakefileLink.getVirtualPluginDomains()) {
            ret.add(takeFileVirtual[0]);
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        /* First add domains with normal patterns! */
        for (int i = 0; i < domains.length; i++) {
            ret.add("https?://(?:www\\.)?" + Pattern.quote(domains[i]) + "/(users/[a-z0-9_]+(?:/[^\\?\r\n]+)?|folder/\\d+/[^\\?\r\n]+)");
        }
        for (final String fileupDomain : getFileUpDomains()) {
            ret.add("https?://(?:www\\.)?" + Pattern.quote(fileupDomain) + "/users/[a-z0-9_]+(?:/[^\\?\r\n]+)?");
        }
        for (final String uploadboyDomain : getUploadboyDomains()) {
            ret.add("https?://(?:www\\.)?" + Pattern.quote(uploadboyDomain) + "/(users/[a-z0-9_]+(?:/[^\\?\r\n]+)?|ur-.+)");
        }
        /*
         * Now add special patterns - this might be ugly but usually we do not get new specialDomains! Keep in mind that their patterns have
         * to be in order and the number of patterns has to be the same as the total number of domains!
         */
        /* hotlink.cc & ex-load.com */
        ret.add("https?://(?:www\\.)?hotlink\\.cc/folder/[a-f0-9\\-]+");
        ret.add("https?://(?:www\\.)?ex\\-load\\.com/folder/[a-f0-9\\-]+");
        /* imgbaron.com */
        ret.add("https?://(?:www\\.)?imgbaron\\.com/g/[A-Za-z0-9]+");
        /* filespace.com & spaceforfiles.com */
        ret.add("https?://filespace\\.com/dir/[a-z0-9]+");
        ret.add("https?://spaceforfiles\\.com/dir/[a-z0-9]+");
        /* prefiles.com */
        ret.add("https?://(?:www\\.)?prefiles\\.com/folder/\\d+[A-Za-z0-9\\-_]+");
        /* imagetwist.com (image galleries) */
        ret.add("https?://(?:www\\.)?imagetwist\\.com/p/[^/]+/\\d+/[^/]+");
        /* file.al */
        ret.add("https?://(?:www\\.)?file\\.al/public/\\d+/.+");
        /* send.cm */
        ret.add("https?://(?:www\\.)?(send\\.cm|sendit\\.cloud)/s/.+");
        /* 10gb.vn */
        ret.add("https?://(?:www\\.)?10gb\\.vn/(f/[a-z0-9]{32}|users/.+)");
        ret.add("https?://(?:www\\.)?takefile\\.link/folder/[a-f0-9\\-]+");
        for (String[] takeFileVirtual : TakefileLink.getVirtualPluginDomains()) {
            ret.add("https?://" + Pattern.quote(takeFileVirtual[0]) + "/folder/[a-f0-9\\-]+");
        }
        return ret.toArray(new String[0]);
    }

    // DEV NOTES
    // other: keep last /.+ for fpName. Not needed otherwise.
    // other: group sister sites or aliased domains together, for easy
    // maintenance.
    /*
     * Sets crawled file items as online right away. 2021-04-27: Enabled this for public testing purposes. Not sure if items inside a folder
     * are necessarily online but it would make sense!
     */
    int totalNumberofFiles = -1;

    /**
     * @author raztoki
     */
    public GenericXFileShareProFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        PluginForHost hostPlg = null;
        try {
            hostPlg = this.getNewPluginForHostInstance(this.getHost());
            if (hostPlg instanceof XFileSharingProBasic) {
                ((XFileSharingProBasic) hostPlg).prepBrowser(br, this.getHost());
            }
        } catch (final Exception ignore) {
            /*
             * Can happen if list of supported hosts here- and in host plugin are not aligned in the right way. As long as no login support
             * is required we don't care. This will generally happen more often for websites with a lot of domains and such that often
             * change their main domain e.g. file-upload.com.
             */
            logger.log(ignore);
            logger.warning("!!Developer!! Failed to find host plugin for: " + this.getHost());
        }
        br.setFollowRedirects(true);
        int loginCheckCounter = 0;
        final int maxLoginCheck = 1;
        boolean loggedIN = false;
        /* First check for offline and check if we need to be logged in to view this folder. */
        do {
            try {
                getPage(param.getCryptedUrl());
                if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("No such user exist|No such folder")) {
                    logger.info("Incorrect URL, Invalid user or empty folder");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.containsHTML(">\\s*?Guest access not possible")) {
                    /* 2019-08-13: Rare special case E.g. easybytez.com */
                    if (loggedIN) {
                        logger.info("We are loggedIN but still cannot view this folder --> Wrong account or crawler plugin failure");
                        throw new AccountRequiredException("Folder not accessible with this account");
                    }
                    logger.info("Cannot access folder without login --> Trying to login and retry");
                    if (!(hostPlg instanceof XFileSharingProBasic)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Account acc = AccountController.getInstance().getValidAccount(hostPlg);
                    if (acc == null) {
                        throw new AccountRequiredException("Folder not accessible without account");
                    }
                    try {
                        ((XFileSharingProBasic) hostPlg).loginWebsite(null, acc, false);
                        loggedIN = true;
                    } catch (final Exception e) {
                        ((XFileSharingProBasic) hostPlg).handleAccountException(acc, logger, e);
                    }
                    continue;
                } else {
                    /* Folder should be accessible without account --> Step out of loop */
                    break;
                }
            } finally {
                loginCheckCounter++;
            }
        } while (loginCheckCounter <= maxLoginCheck);
        final String packagename = getPackagename(param, this.br);
        final FilePackage fp = FilePackage.getInstance();
        if (packagename != null) {
            fp.setName(Encoding.htmlDecode(packagename).trim());
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        final ArrayList<String> dupes = new ArrayList<String>();
        dupes.add(param.getCryptedUrl());
        /* prevents continuous loop. */
        int page = 1;
        do {
            logger.info("Crawling page: " + page);
            final ArrayList<DownloadLink> newResults = parsePage(dupes, fp, param);
            if (newResults.isEmpty()) {
                /* Fail-safe */
                logger.info("Stopping because failed to find new items on current page");
                break;
            }
            ret.addAll(newResults);
            /* Increment to value of next page */
            page += 1;
            if (!this.accessNextPage(this.br, page)) {
                logger.info("Stopping because: Failed to find/access next page");
                break;
            } else {
                /* Loggers are different depending on whether we know the total number of expected items or not. */
                if (totalNumberofFiles == -1) {
                    logger.info("Found " + ret.size() + " items");
                } else {
                    logger.info("Found " + ret.size() + " / " + this.totalNumberofFiles + " items");
                }
                /* Now continue to crawl next page */
            }
        } while (!this.isAbort());
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, br._getURL().getPath());
        }
        return ret;
    }

    protected String getPackagename(final CryptedLink param, final Browser br) {
        final String username = new Regex(param.getCryptedUrl(), "/users/([^/]+)").getMatch(0);
        String fpName = regexPackagenameFromURL(param.getCryptedUrl());
        if (fpName == null) {
            fpName = regexPackagenameFromHTML(br);
        }
        if (fpName == null) {
            /* Final fallback */
            fpName = username;
        }
        return fpName;
    }

    protected String regexPackagenameFromURL(final String url) {
        String fpName = new Regex(url, "(?i)(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)[^/]+/(.+)").getMatch(1); // name
        if (fpName == null) {
            fpName = new Regex(url, "(?i)(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)(.+)").getMatch(1); // id
            if (fpName == null) {
                fpName = new Regex(url, "(?i)users/[a-z0-9_]+/[^/]+/(.+)").getMatch(0); // name
                if (fpName == null) {
                    fpName = new Regex(url, "(?i)users/[a-z0-9_]+/(.+)").getMatch(0); // id
                    if (fpName == null) {
                        fpName = new Regex(url, "(?i)/s/[^/]+/([^/]+)").getMatch(0); // name, send.cm
                    }
                }
            }
        }
        return fpName;
    }

    protected String regexPackagenameFromHTML(final Browser br) {
        String fpName;
        if ("hotlink.cc".equals(br.getHost())) {
            fpName = br.getRegex("<i class=\"glyphicon glyphicon-folder-open\"></i>\\s*(.*?)\\s*</span>").getMatch(0);
        } else if ("imagetwist.com".equals(br.getHost())) {
            /* 2023-11-09 */
            fpName = br.getRegex("page_main_title\"[^>]*>([^<]+)<").getMatch(0);
        } else {
            // ex-load.com
            fpName = br.getRegex("Files in\\s*(.*?)\\s*folder\\s*</title>").getMatch(0);
            if (fpName == null) {
                // file-al
                fpName = br.getRegex("Files of\\s*(.*?)\\s*folder\\s*</title>").getMatch(0);
            }
            if (fpName == null) {
                fpName = br.getRegex("<title>\\s*(.*?)\\s*folder\\s*</title>").getMatch(0);
            }
            if (fpName == null) {
                fpName = br.getRegex("<h1.*?</i>\\s*(.*?)\\s*</h1>").getMatch(0);
            }
            if (fpName == null) {
                /* 2019-02-08: E.g. for photo galleries (e.g. imgbaron.com) */
                fpName = br.getRegex("<H1>\\s*?(.*?)\\s*?</H1>").getMatch(0);
            }
        }
        return fpName;
    }

    private ArrayList<DownloadLink> parsePage(final ArrayList<String> dupes, final FilePackage fp, final CryptedLink param) throws PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int numberofNewItems = 0;
        final String pathPattern = "/[a-z0-9]{12}(?:\\.html|/.*?)?";
        String[] links = br.getRegex("href=(\"|')(https?://(?:www\\.)?" + Pattern.quote(br.getHost(true)) + pathPattern + ")\\1").getColumn(1);
        if (links == null || links.length == 0) {
            links = br.getRegex("href=(\"|')(https?://(?:www\\.)?" + Pattern.quote(br.getHost(false)) + pathPattern + ")\\1").getColumn(1);
        }
        if (links == null || links.length == 0) {
            /* Final attempt: Don't care about domain in links [e.g. uploadboy.com]. */
            links = br.getRegex("href=(\"|')(https?://(?:www\\.)?[^/]+/[a-z0-9]{12}(?:/.*?)?)\\1").getColumn(1);
        }
        if (links != null && links.length > 0) {
            String html = br.getRequest().getHtmlCode();
            html = html.replaceAll("</?font[^>]*>", "");
            html = html.replaceAll("</?b[^>]*>", "");
            // file.al, ex-load.com
            final ArrayList<String> tr_snippets = new ArrayList<String>(Arrays.asList(new Regex(html, "((<tr>)?<td.*?</tr>)").getColumn(0)));
            for (final String link : links) {
                final String linkid = new Regex(link, Pattern.compile("https?://[^/]+/([a-z0-9]{12})", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (!dupes.add(linkid)) {
                    /* Skip dupes */
                    continue;
                }
                /**
                 * TODO: Consider adding support for "fast linkcheck" option via XFS core (superclass) --> Set links as available here -
                 * maybe only if filename is given inside URL (which is often the case). In general, files inside a folder should be online!
                 */
                final DownloadLink dl = createDownloadlink(link);
                String html_snippet = null;
                final Iterator<String> it = tr_snippets.iterator();
                while (it.hasNext()) {
                    final String tr_snippet = it.next();
                    if (StringUtils.containsIgnoreCase(tr_snippet, linkid)) {
                        html_snippet = tr_snippet;
                        it.remove();
                        break;
                    }
                }
                if (StringUtils.isNotEmpty(html_snippet)) {
                    // split tr_snippet in case it contains multiple links, eg 3 in a row, eg brupload
                    final String link_tr_snippet = new Regex(html_snippet, "<TD>.*" + linkid + ".*?</TD>").getMatch(-1);
                    if (link_tr_snippet != null) {
                        final String rest_tr_snippet = html_snippet.replace(link_tr_snippet, "");
                        if (StringUtils.isNotEmpty(rest_tr_snippet)) {
                            tr_snippets.add(0, rest_tr_snippet);
                        }
                        html_snippet = link_tr_snippet;
                    }
                }
                if (StringUtils.isEmpty(html_snippet)) {
                    /* Works for e.g. world-files.com, brupload.net */
                    /* TODO: Improve this RegEx e.g. for katfile.com, brupload.net */
                    html_snippet = new Regex(html, "<tr>\\s*<td>\\s*<a[^<]*" + linkid + ".*</td>\\s*</tr>").getMatch(-1);
                    if (StringUtils.isEmpty(html_snippet)) {
                        /* 2020-02-04: E.g. userupload.net */
                        html_snippet = new Regex(html, "<TD>.*" + linkid + ".*</TD>").getMatch(-1);
                    }
                    if (StringUtils.isEmpty(html_snippet)) {
                        /* E.g. up-4.net */
                        /*
                         * TODO: Improve this RegEx. It will always pickup the first item of each page thus the found filename/filesize
                         * information will be wrong!
                         */
                        html_snippet = new Regex(html, "<div class=\"file\\-details\">\\s+<h3 class=\"file\\-ttl\"><a href=\"[^\"]+/" + linkid + ".*?</div>\\s+</div>").getMatch(-1);
                    }
                }
                /* Set ContentURL - VERY important for XFS (Mass-)Linkchecking! */
                dl.setContentUrl(link);
                final String link_quoted = Pattern.quote(link);
                String url_filename = new Regex(link, "(?i)[a-z0-9]{12}/(.+)\\.html$").getMatch(0);
                /* E.g. up-4.net */
                String html_filename = null;
                String filesizeStr = null;
                if (html_snippet != null) {
                    html_filename = new Regex(html_snippet, "target=\"_blank\">\\s*([^<>\"]+)\\s*</(a|td)>").getMatch(0);
                    if (html_filename == null) {
                        html_filename = new Regex(html_snippet, Pattern.quote(linkid) + "(?:/.*\\.html)?[^>]*>\\s*([^<>\"]+)\\s*</(a|td)>").getMatch(0);
                    }
                    filesizeStr = new Regex(html_snippet, "([\\d\\.]+ (?:KB|MB|GB))").getMatch(0);
                    if (filesizeStr == null) {
                        /* Only look for unit "bytes" as a fallback! */
                        filesizeStr = new Regex(html_snippet, "([\\d\\.]+ B)").getMatch(0);
                    }
                } else {
                    /* Other attempts without pre-found html-snippet */
                    html_filename = br.getRegex(link_quoted + "\">([^<]+)</a>").getMatch(0);
                }
                String filename;
                if (html_filename != null) {
                    filename = html_filename;
                } else {
                    filename = url_filename;
                }
                boolean incompleteFileName = false;
                if (!StringUtils.isEmpty(filename)) {
                    if (filename.endsWith("&#133;")) {
                        /*
                         * Indicates that this is not the complete filename but there is nothing we can do at this stage - full filenames
                         * should be displayed once a full linkcheck is performed or at least once a download starts.
                         */
                        incompleteFileName = true;
                    }
                    filename = Encoding.htmlDecode(filename);
                    dl.setName(filename);
                }
                if (!StringUtils.isEmpty(filesizeStr)) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                }
                // if (!incompleteFileName) {
                // dl.setAvailable(true);
                // }
                dl.setAvailable(true);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
                distribute(dl);
                numberofNewItems++;
            }
        }
        /* These should only be shown when its a /user/ decrypt task */
        final String currentFolderPath = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/(.+)").getMatch(0);
        String folders[] = br.getRegex("folder.?\\.gif.*?<a href=\"(.+?" + Pattern.quote(br.getHost(true)) + "[^\"]+users/[^\"]+)").getColumn(0);
        if (folders == null || folders.length == 0) {
            folders = br.getRegex("folder.?\\.gif.*?<a href=\"(.+?" + Pattern.quote(br.getHost(false)) + "[^\"]+users/[^\"]+)").getColumn(0);
            if (folders == null || folders.length == 0) {
                /* 2024-06-28: New attempt */
                folders = br.getRegex("\"([^\"]*/users/[^\"']+)").getColumn(0);
            }
        }
        if (folders != null && folders.length > 0) {
            for (final String folderlink : folders) {
                final String path;
                if (folderlink.startsWith("/")) {
                    path = folderlink;
                } else {
                    path = new Regex(folderlink, "https?://[^/]+/(.+)").getMatch(0);
                }
                /* Make sure that we're not grabbing the parent folder but only the folder that the user has added + eventual subfolders! */
                final boolean folderIsChildFolder = path.length() > currentFolderPath.length();
                if (this.canHandle(folderlink) && !dupes.contains(folderlink) && folderIsChildFolder) {
                    numberofNewItems++;
                    final DownloadLink dlfolder = createDownloadlink(folderlink);
                    ret.add(dlfolder);
                    distribute(dlfolder);
                    dupes.add(folderlink);
                } else {
                    logger.info("Skipping possible result: " + folderlink);
                }
            }
        }
        return ret;
    }

    UrlQuery folderQuery = null;

    protected boolean accessNextPage(final Browser br, final int nextPage) throws Exception {
        /* Make sure to get the next page so we don't accidently parse the same page multiple times! */
        String nextPageUrl = br.getRegex("<div class=(\"|')paging\\1>.*?<a href=('|\")([^']+\\&amp;page=" + nextPage + "|/go/[a-zA-Z0-9]{12}/\\d+/?)\\2>").getMatch(2);
        if (nextPageUrl == null) {
            // 2024-03-22: send.cm
            nextPageUrl = br.getRegex("<a class\\s*=\\s*(\"|')page-link\\1[^>]*href\\s*=\\s*('|\")(/\\?[^\"']*op=user_public[^\"']*page=" + nextPage + ")").getMatch(2);
        }
        if (nextPageUrl != null) {
            nextPageUrl = HTMLEntities.unhtmlentities(nextPageUrl);
            nextPageUrl = Request.getLocation(nextPageUrl, br.getRequest());
            // final String pageStr = UrlQuery.parse(nextPage).get("page");
            // if (pageStr != null && !pageStr.equalsIgnoreCase(i + "")) {
            // logger.info("NextPage doesn't match expected page: Next = " + pageStr + " Expected = " + i);
            // return false;
            // }
            getPage(br, nextPageUrl);
            return true;
        } else {
            if (folderQuery == null) {
                /* Pagination ? */
                final String pagination = br.getRegex("setPagination\\('.files_paging',.*?\\);").getMatch(-1); // "files_paging" or also
                // "#files_paging" (e.g. file.al)
                if (pagination == null) {
                    return false;
                }
                final String op = new Regex(pagination, "op:\\s*'(\\w+)'").getMatch(0);
                /* Either userID or userName should be given here! */
                final String usr_login = new Regex(pagination, "usr_login:\\s*'(\\w+)'").getMatch(0);
                final String usr_id = new Regex(pagination, "usr_id:\\s*'(\\d+)'").getMatch(0);
                final String totalNumberofFiles = new Regex(pagination, "total:\\s*'(\\d+)'").getMatch(0);
                String fld_id = new Regex(pagination, "fld_id:\\s*'(\\w+)'").getMatch(0);
                if ("user_public".equalsIgnoreCase(op) && fld_id == null) {
                    /* Decrypt all files of a user --> No folder_id given/required! Example: up-4-net */
                    fld_id = "";
                }
                if (op == null || (usr_login == null && usr_id == null) || fld_id == null) {
                    return false;
                }
                if (totalNumberofFiles != null) {
                    this.totalNumberofFiles = Integer.parseInt(totalNumberofFiles);
                }
                folderQuery = new UrlQuery();
                folderQuery.add("op", op);
                folderQuery.add("load", "files");
                folderQuery.add("fld_id", fld_id);
                if (usr_login != null) {
                    folderQuery.add("usr_login", usr_login);
                } else {
                    folderQuery.add("usr_id", usr_id);
                }
            }
            folderQuery.addAndReplace("page", Integer.toString(nextPage));
            // postData = "op=" + Encoding.urlEncode(op) + "&load=files&page=%s&fld_id=" + Encoding.urlEncode(fld_id) + "&usr_login=" +
            // Encoding.urlEncode(usr_login);
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage(br.getURL(), folderQuery.toString());
            return true;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}