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
import org.appwork.utils.parser.UrlQuery;
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
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

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
        /* Websites using the NEW style: */
        ret.add(new String[] { "letsupload.io", "letsupload.org", "letsupload.to", "letsupload.co" });
        ret.add(new String[] { "truefile.cc" });
        ret.add(new String[] { "devdrive.cloud" });
        ret.add(new String[] { "przeslij.com" });
        ret.add(new String[] { "fhscript.com" });
        ret.add(new String[] { "bowfile.com" });
        ret.add(new String[] { "shareplace.org" });
        ret.add(new String[] { "hostmy.de" });
        ret.add(new String[] { "1cloudfile.com" });
        ret.add(new String[] { "cyberfile.me", "cyberfile.su" });
        ret.add(new String[] { "filestank.com" });
        ret.add(new String[] { "wrzucaj.pl" });
        ret.add(new String[] { "egofiles.net" });
        ret.add(new String[] { "rapidshare.io" });
        ret.add(new String[] { "sharewith.click" });
        ret.add(new String[] { "covyn.net" });
        ret.add(new String[] { "uploadcorner.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:folder/([a-f0-9]{32}|\\d+(?:/[^<>\"]+)?(?:\\?sharekey=[A-Za-z0-9\\-_]+)?)|shared/[a-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_OLD                  = "https?://[^/]+/folder/(\\d+)(?:/[^<>\"]+)?(?:\\?sharekey=[A-Za-z0-9\\-_]+)?";
    private static final String TYPE_NEW                  = "https?://[^/]+/folder/([a-f0-9]{32})";
    private static final String TYPE_NEW_NONACCOUNTSHARED = "https?://[^/]+/shared/([a-z0-9\\-_]+)";

    /**
     * Generic Crawler for YetiShare file-hosts. <br />
     * 2019-04-29: So far, letsupload.co is the only supported host(well they all have folders but it is the first YetiShare host of which
     * we know that has public folders).
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_OLD)) {
            return this.crawlFolderOLD(param);
        } else {
            return this.crawlFolderNEW(param);
        }
    }

    private ArrayList<DownloadLink> crawlFolderNEW(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String currentFolderID;
        if (param.getCryptedUrl().matches(TYPE_NEW)) {
            currentFolderID = new Regex(parameter, TYPE_NEW).getMatch(0);
        } else {
            currentFolderID = new Regex(parameter, TYPE_NEW_NONACCOUNTSHARED).getMatch(0);
        }
        if (currentFolderID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        /*
         * 2020-11-13: E.g. erai-ddl3.info only allows one active session. If the user e.g. logs in via JD, he will get logged out in
         * browser and the other way around!
         */
        if (account != null) {
            synchronized (account) {
                final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
                try {
                    if (!(plg instanceof YetiShareCore)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final boolean validatedCookies = ((YetiShareCore) plg).loginWebsite(account, false);
                    br.setFollowRedirects(true);
                    getPage(parameter);
                    if (!validatedCookies && !((YetiShareCore) plg).isLoggedin(br, account)) {
                        logger.info("Session expired? Trying again, this time with cookie validation");
                        ((YetiShareCore) plg).loginWebsite(account, true);
                        br.setFollowRedirects(true);
                        getPage(parameter);
                        /* Assume that we are logged in now. */
                        if (!((YetiShareCore) plg).isLoggedin(br, account)) {
                            logger.warning("Possible login failure");
                        }
                    }
                } catch (PluginException e) {
                    handleAccountException(account, e);
                }
            }
        } else {
            br.setFollowRedirects(true);
            getPage(parameter);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(currentFolderID)) {
            /* 2020-11-12: Invalid folderHash --> Redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final String expectedMaxNumberofItemsPerPage = br.getRegex("var perPage = (\\d+);").getMatch(0);
        final UrlQuery folderquery = new UrlQuery();
        if (param.getCryptedUrl().matches(TYPE_NEW)) {
            final String folderID = br.getRegex("loadImages\\('folder', '(\\d+)'").getMatch(0);
            if (folderID == null) {
                /* Most likely we've been logged-out and/or account is required to view this folder! */
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                throw new AccountRequiredException();
            }
            folderquery.add("pageType", "folder");
            folderquery.add("nodeId", folderID);
        } else {
            folderquery.add("pageType", "nonaccountshared");
            folderquery.add("nodeId", "");
        }
        folderquery.add("perPage", "0");
        folderquery.add("filterOrderBy", "");
        folderquery.add("additionalParams%5BsearchTerm%5D", "");
        folderquery.add("additionalParams%5BfilterUploadedDateRange%5D", "");
        folderquery.add("pageStart", "1");
        postPage("/account/ajax/load_files", folderquery.toString());
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (entries.containsKey("javascript")) {
            final String js = (String) entries.get("javascript");
            /*
             * 2021-04-22: E.g. showErrorNotification("Error",
             * "Folder is not publicly shared. Please contact the owner and request they update the privacy settings.");
             */
            if (js.contains("showErrorNotification")) {
                /* Subsequent handling will most likely detect this folder as offline */
                logger.info("Possible error-reason for offline folder: " + js);
            }
        }
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
            this.submitForm(pwForm);
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            passwordSuccess = ((Boolean) entries.get("success")).booleanValue();
            counter++;
        } while (!this.isAbort() && !passwordSuccess && counter < 3);
        if (!passwordSuccess) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        } else if (passCode != null) {
            /* Re-do request to access folder content */
            postPage("/account/ajax/load_files", folderquery.toString());
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            htmlInsideJson = (String) entries.get("html");
            br.getRequest().setHtmlCode(htmlInsideJson);
        }
        int page = 1;
        FilePackage fp = null;
        if (param.getCryptedUrl().matches(TYPE_NEW) && !StringUtils.isEmpty(fpName)) {
            fp = FilePackage.getInstance();
            fp.setName(fpName);
        }
        /* Try to construct absolute path to current folder */
        String subfolderPath = "";
        final String[] subfolderParts = br.getRegex("<a href=\"[^\"]+\"[^>]*class=\"btn btn-white[^\"]*\">([^<>]+)<").getColumn(0);
        if (subfolderParts.length > 0) {
            for (String subfolderPart : subfolderParts) {
                if (subfolderPath.length() > 0) {
                    subfolderPath += "/";
                }
                if (Encoding.isHtmlEntityCoded(subfolderPart)) {
                    subfolderPart = Encoding.htmlDecode(subfolderPart);
                }
                subfolderPath += subfolderPart;
            }
        } else {
            /* We'll allow this to happen but it should never happen not even if we're in the root folder! */
            logger.warning("Failed to find absolute folder path");
        }
        do {
            logger.info("Crawling page: " + page);
            /* 2021-04-30: "</div></div>" when loggedin */
            final String[] fileHTMLSnippets = br.getRegex("<div[^>]*(dttitle.*?)(?:</span></div>|</div></div>)").getColumn(0);
            if (fileHTMLSnippets.length > 0) {
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
                    dl.setProperty(org.jdownloader.plugins.components.YetiShareCore.PROPERTY_INTERNAL_FILE_ID, internalFileID);
                    if (uploaddateStr != null) {
                        /* 2020-11-26: For Packagizer/EventScripter - not used anywhere else. */
                        dl.setProperty(YetiShareCore.PROPERTY_UPLOAD_DATE_RAW, uploaddateStr);
                    }
                    /* We know for sure that this file is online! */
                    dl.setAvailable(true);
                    if (subfolderPath.length() > 0) {
                        dl.setRelativeDownloadFolderPath(subfolderPath);
                    }
                    if (passCode != null) {
                        dl.setDownloadPassword(passCode);
                    }
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    ret.add(dl);
                    distribute(dl);
                }
            }
            /* Now crawl subfolders inside this folder */
            final String[] folderHashes = br.getRegex("(/folder/[a-f0-9]{32})").getColumn(0);
            for (String folderHash : folderHashes) {
                if (folderHash.equalsIgnoreCase(currentFolderID)) {
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
                ret.add(folder);
                distribute(folder);
            }
            if (fileHTMLSnippets.length == 0 && folderHashes.length == 0) {
                logger.info("Stopping because failed to find any items on current page");
                break;
            }
            final String nextpageStr = br.getRegex("onClick=\"loadImages\\('(?:folder|nonaccountshared)', '\\d+', (\\d+),[^\\)]+\\); return false;\"><span>Next</span>").getMatch(0);
            /* Only continue if found page matches expected page! */
            if (nextpageStr != null && nextpageStr.equals(Integer.toString(page + 1))) {
                page++;
                folderquery.add("pageStart", Integer.toString(page));
                postPage("/account/ajax/load_files", folderquery.toString());
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                htmlInsideJson = (String) entries.get("html");
                br.getRequest().setHtmlCode(htmlInsideJson);
            } else {
                /* We should be on the last page */
                logger.info("Failed to find nextpage -> Stopping");
                break;
            }
        } while (!this.isAbort());
        if (ret.isEmpty()) {
            String offlineName = currentFolderID;
            if (!StringUtils.isEmpty(subfolderPath)) {
                offlineName += subfolderPath;
            }
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, offlineName);
        }
        return ret;
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
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(folderID)) {
            /* 2019-04-29: E.g. letsupload.co offline folder --> Redirect to /index.html */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)<strong>-\\s*There are no files within this folder\\.\\s*</strong>")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
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
