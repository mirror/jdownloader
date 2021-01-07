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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.YetiShareCoreSpecialOxycloud;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GenericYetiShareFolder extends antiDDoSForDecrypt {
    public GenericYetiShareFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** 2020-11-13: Preventive measure */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        /* Please try to sort website based on old/new style */
        /* websites using the OLD style: */
        ret.add(new String[] { "upfordown.xyz" });
        ret.add(new String[] { "fireload.com" });
        /* Websites using the NEW style: */
        ret.add(new String[] { "oxycloud.com" });
        ret.add(new String[] { "erai-ddl3.info" });
        ret.add(new String[] { "vishare.pl" });
        ret.add(new String[] { "letsupload.io", "letsupload.org", "letsupload.to", "letsupload.co" });
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
            // final String annotationName = domains[0];
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/([a-f0-9]{32}|\\d+(?:/[^<>\"]+)?(?:\\?sharekey=[A-Za-z0-9\\-_]+)?)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_OLD = "https?://[^/]+/folder/(\\d+)(?:/[^<>\"]+)?(?:\\?sharekey=[A-Za-z0-9\\-_]+)?";
    private static final String TYPE_NEW = "https?://[^/]+/folder/([a-f0-9]{32})";

    /**
     * Generic Crawler for YetiShare file-hosts. <br />
     * 2019-04-29: So far, letsupload.co is the only supported host(well they all have folders but it is the first YetiShare host of which
     * we know that has public folders).
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_NEW)) {
            return this.crawlFolderNEW(param);
        } else {
            return this.crawlFolderOLD(param);
        }
    }

    private ArrayList<DownloadLink> crawlFolderNEW(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String currentFolderHash = new Regex(parameter, TYPE_NEW).getMatch(0);
        if (currentFolderHash == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        /* TODO: Make login work for all supported hosts */
        /*
         * 2020-11-13: erai-ddl3.info only allows one active session. If the user e.g. logs in via JD, he will get logged out in browser and
         * the other way around!
         */
        if (account != null && this.getHost().equals("erai-ddl3.info")) {
            synchronized (account) {
                final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
                plg.setBrowser(this.br);
                plg.setLogger(getLogger());
                try {
                    final boolean validatedCookies = ((jd.plugins.hoster.EraiDdlthreeInfo) plg).loginWebsiteSpecial(account, false);
                    br.setFollowRedirects(true);
                    br.getPage(parameter);
                    if (!validatedCookies && !((jd.plugins.hoster.EraiDdlthreeInfo) plg).isLoggedinSpecial()) {
                        logger.info("Session expired? Trying again, this time with cookie validation");
                        ((jd.plugins.hoster.EraiDdlthreeInfo) plg).loginWebsiteSpecial(account, true);
                        br.setFollowRedirects(true);
                        br.getPage(parameter);
                        /* Assume that we are logged in now. */
                    }
                } catch (PluginException e) {
                    handleAccountException(account, e);
                }
            }
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (!br.getURL().contains(currentFolderHash)) {
            /* 2020-11-12: Invalid folderHash --> Redirect to mainpage */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // final String expectedMaxNumberofItemsPerPage = br.getRegex("var perPage = (\\d+);").getMatch(0);
        final String folderID = br.getRegex("loadImages\\('folder', '(\\d+)'").getMatch(0);
        if (folderID == null) {
            /* Most likely we've been logged-out and/or account is required to view this folder! */
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            throw new AccountRequiredException();
        }
        final String folderPostData = "pageType=folder&nodeId=" + folderID + "&perPage=0&filterOrderBy=&additionalParams%5BsearchTerm%5D=&additionalParams%5BfilterUploadedDateRange%5D=&pageStart=";
        br.postPage("/account/ajax/load_files", folderPostData + "1");
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String fpName = (String) entries.get("page_title");
        String htmlInsideJson = (String) entries.get("html");
        br.getRequest().setHtmlCode(htmlInsideJson);
        boolean passwordSuccess = true;
        String passCode = null;
        Form pwForm = null;
        int counter = 0;
        do {
            /* Grab Form on first loop, then re-use it */
            if (pwForm == null) {
                pwForm = br.getFormbyProperty("id", "folderPasswordForm");
                if (pwForm == null) {
                    /* Folder is not password protected */
                    break;
                }
            }
            pwForm.setMethod(MethodType.POST);
            passCode = getUserInput("Password?", param);
            pwForm.put("folderPassword", Encoding.urlEncode(passCode));
            br.submitForm(pwForm);
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            passwordSuccess = ((Boolean) entries.get("success")).booleanValue();
            counter++;
        } while (!this.isAbort() && !passwordSuccess && counter < 3);
        if (!passwordSuccess) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        } else if (passCode != null) {
            /* Re-do request to access folder content */
            br.postPage("/account/ajax/load_files", folderPostData + "1");
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            htmlInsideJson = (String) entries.get("html");
            br.getRequest().setHtmlCode(htmlInsideJson);
        }
        int page = 1;
        do {
            logger.info("Crawling page: " + page);
            final String[] fileHTMLSnippets = br.getRegex("<div[^>]*(dttitle.*?)</span></div>").getColumn(0);
            if (fileHTMLSnippets.length > 0) {
                /* Try to construct absolute path */
                String subfolderPath = "";
                final String[] subfolderParts = br.getRegex("class=\"btn btn-white mid-item\">([^<>\"]+)<").getColumn(0);
                for (final String subfolderPart : subfolderParts) {
                    if (subfolderPath.length() > 0) {
                        subfolderPath += "/";
                    }
                    subfolderPath += subfolderPart;
                }
                final FilePackage fp = FilePackage.getInstance();
                if (!StringUtils.isEmpty(fpName)) {
                    fp.setName(fpName);
                } else {
                    /* Fallback */
                    fp.setName(currentFolderHash);
                }
                for (final String html : fileHTMLSnippets) {
                    final String url = new Regex(html, "dtfullurl\\s*=\\s*\"(https?[^\"]+)\"").getMatch(0);
                    final String filename = new Regex(html, "dtfilename\\s*=\\s*\"([^\"]+)\"").getMatch(0);
                    final String uploaddateStr = new Regex(html, "dtuploaddate\\s*=\\s*\"([^\"]+)\"").getMatch(0);
                    final String filesizeStr = new Regex(html, "dtsizeraw\\s*=\\s*\"(\\d+)\"").getMatch(0);
                    final String internalFileID = new Regex(html, "fileId\\s*=\\s*\"(\\d+)\"").getMatch(0);
                    if (StringUtils.isEmpty(url) || StringUtils.isEmpty(internalFileID)) {
                        /* Skip invalid items */
                        continue;
                    }
                    final DownloadLink dl = createDownloadlink(url);
                    if (!StringUtils.isEmpty(filename)) {
                        dl.setName(filename);
                    }
                    if (!StringUtils.isEmpty(filesizeStr)) {
                        dl.setDownloadSize(Long.parseLong(filesizeStr));
                    }
                    dl.setProperty(jd.plugins.hoster.YetiShareCoreSpecialOxycloud.PROPERTY_INTERNAL_FILE_ID, internalFileID);
                    if (uploaddateStr != null) {
                        /* 2020-11-26: For Packagizer/EventScripter - not used anywhere else. */
                        dl.setProperty(YetiShareCoreSpecialOxycloud.PROPERTY_UPLOAD_DATE_RAW, uploaddateStr);
                    }
                    /* We know for sure that this file is online! */
                    dl.setAvailable(true);
                    if (subfolderPath.length() > 0) {
                        dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath);
                    }
                    if (passCode != null) {
                        dl.setDownloadPassword(passCode);
                    }
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
            }
            /* Now crawl subfolders inside this folder */
            final String[] folderHashes = br.getRegex("(/folder/[a-f0-9]{32})").getColumn(0);
            for (String folderHash : folderHashes) {
                if (folderHash.equalsIgnoreCase(currentFolderHash)) {
                    /* Don't re-add the folder we're just crawling! */
                    continue;
                }
                final String folderURL = br.getURL(folderHash).toString();
                final DownloadLink folder = this.createDownloadlink(folderURL);
                /*
                 * 2020-11-13: Not required. If a "root" folder is password-protected, all files within it are usually not password
                 * protected (WTF) and/or can require another password which can be different. Also subfolders inside folders will usually
                 * not require a password at all but users CAN set a (different) password on them.
                 */
                // folder.setDownloadPassword(passCode);
                decryptedLinks.add(folder);
                distribute(folder);
            }
            if (fileHTMLSnippets.length == 0 && folderHashes.length == 0) {
                logger.info("Stopping because failed to find any items on current page");
                break;
            }
            final String nextpageStr = br.getRegex("onClick=\"loadImages\\('folder', '\\d+', (\\d+),[^\\)]+\\); return false;\"><span>Next</span>").getMatch(0);
            /* Only continue if found page matches expected page! */
            if (nextpageStr != null && nextpageStr.matches(page + 1 + "")) {
                page++;
                br.postPage("/account/ajax/load_files", folderPostData + page);
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                htmlInsideJson = (String) entries.get("html");
                br.getRequest().setHtmlCode(htmlInsideJson);
            } else {
                logger.info("Failed to find nextpage -> Stopping");
                break;
            }
        } while (!this.isAbort());
        if (decryptedLinks.size() == 0) {
            decryptedLinks.add(this.createOfflinelink(parameter, "empty_folder_" + currentFolderHash, "Empty folder?"));
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlFolderOLD(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderID = new Regex(parameter, TYPE_OLD).getMatch(0);
        if (folderID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        getPage(parameter);
        // /*
        // * 2019-06-12: TODO: Their html contains json containing all translations. We might be able to use this for us for better
        // * errorhandling in the future ...
        // */
        // final String there_are_no_files_within_this_folderTEXT = PluginJSonUtils.getJson(br, "there_are_no_files_within_this_folder");
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(folderID)) {
            /* 2019-04-29: E.g. letsupload.co offline folder --> Redirect to /index.html */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.containsHTML("<strong>- There are no files within this folder\\.</strong>")) {
            logger.info("Folder is empty");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form folderPasswordForm = oldStyleGetFolderPasswordForm();
        String folderPassword = null;
        if (folderPasswordForm != null) {
            logger.info("Folder seems to be password protected");
            int counter = 0;
            do {
                folderPassword = getUserInput("Password?", param);
                folderPasswordForm.put("folderPassword", folderPassword);
                this.submitForm(folderPasswordForm);
                folderPasswordForm = oldStyleGetFolderPasswordForm();
                counter++;
            } while (counter <= 2 && folderPasswordForm != null);
            if (folderPasswordForm != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String fpNameFallback = new Regex(parameter, "/folder/\\d+/([^/\\?]+)").getMatch(0);
        if (fpNameFallback != null) {
            /* Nicer fallback packagename */
            fpNameFallback = fpNameFallback.replace("_", " ");
        }
        String fpName = br.getRegex("<h2>Files Within Folder \\'([^<>\"\\']+)\\'</h2>").getMatch(0);
        if (fpName == null) {
            fpName = fpNameFallback;
        }
        final String tableHTML = br.getRegex("<table id=\"fileData\".*?</table>").getMatch(-1);
        final String[] urls;
        if (tableHTML != null) {
            urls = new Regex(tableHTML, "<tr>.*?</tr>").getColumn(-1);
        } else {
            urls = br.getRegex("href=\"(https?://[^<>/]+/[A-Za-z0-9]+(?:/[^<>/]+)?)\" target=\"_blank\"").getColumn(0);
        }
        if (urls == null || urls.length == 0) {
            logger.warning("Failed to find any content");
            return null;
        }
        for (final String urlInfo : urls) {
            String url = null, filename = null, filesize = null;
            if (urlInfo.startsWith("http")) {
                url = urlInfo;
            } else {
                final Regex finfo = new Regex(urlInfo, "target=\"_blank\">([^<>\"]+)</a>\\&nbsp;\\&nbsp;\\((\\d+(?:\\.\\d{1,2})? [A-Z]+)\\)<br/>");
                url = new Regex(urlInfo, "href=\"(https?://[^<>/]+/[A-Za-z0-9]+(?:/[^<>/\"]+)?)\"").getMatch(0);
                filename = finfo.getMatch(0);
                filesize = finfo.getMatch(1);
            }
            if (url == null) {
                continue;
            } else if (new Regex(url, this.getSupportedLinks()).matches()) {
                /* Skip URLs which would go into this crawler again */
                continue;
            }
            final DownloadLink dl = createDownloadlink(url);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename);
                dl.setName(filename);
            } else {
                final String url_name = YetiShareCore.getFilenameFromURL(url);
                /* No filename information given? Use either fuid or name from inside URL. */
                if (url_name != null) {
                    dl.setName(url_name);
                }
            }
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            /* 2019-07-28: Given filename + filesize information does not imply that URLs are online. Example: firedrop.com */
            // if (filename != null && filesize != null) {
            // /* 2019-04-29: Assume all files in a folder with filename&filesize are ONline - TODO: Verify this assumption! */
            // dl.setAvailable(true);
            // }
            if (folderPassword != null) {
                /*
                 * 2019-06-12: URLs in password protected folders are not necessarily password protected (which is kinda stupid) as well but
                 * chances are there so let's set the folder password as single download password just in case.
                 */
                dl.setDownloadPassword(folderPassword);
            }
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private Form oldStyleGetFolderPasswordForm() {
        return br.getFormbyKey("folderPassword");
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }
}
