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
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.YetiShareCoreSpecialOxycloud;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GenericYetiShareFolderSpecialOxycloud extends PluginForDecrypt {
    public GenericYetiShareFolderSpecialOxycloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** 2020-11-13: Preventive measure */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "oxycloud.com" });
        ret.add(new String[] { "erai-ddl3.info" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/([a-f0-9]{32})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String currentFolderHash = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }
}
