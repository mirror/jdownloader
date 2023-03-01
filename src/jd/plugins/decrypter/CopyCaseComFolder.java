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

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.CopyCaseCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { CopyCaseCom.class })
public class CopyCaseComFolder extends PluginForDecrypt {
    public CopyCaseComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return CopyCaseCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/(([A-Za-z0-9]+)(/folder/[A-Za-z0-9]+)?)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final CopyCaseCom hosterplugin = (CopyCaseCom) this.getNewPluginForHostInstance(this.getHost());
        if (account != null) {
            hosterplugin.login(br, account, false);
        }
        final String folderPathURL = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final UrlQuery query = new UrlQuery();
        String passCode = param.getDecrypterPassword();
        boolean askedUserForPassword = false;
        boolean userHasEnteredCorrectPassword = false;
        FilePackage fp = null;
        String path = null;
        int page = 1;
        do {
            int passwordAttempts = 0;
            Map<String, Object> resp = null;
            do {
                if (passCode != null) {
                    query.addAndReplace("password", Encoding.urlEncode(passCode));
                }
                final GetRequest req = new GetRequest(hosterplugin.getAPIBase() + "/file-folders/" + folderPathURL + "?" + query.toString());
                resp = hosterplugin.callAPI(br, param, account, req, true);
                if (br.getHttpConnection().getResponseCode() == 403) {
                    if (userHasEnteredCorrectPassword) {
                        /* This should never happen */
                        logger.warning("Looks like password is invalid although we know the correct password -> Serverside bug or password was changed while we were crawling this folder");
                        break;
                    }
                    if (passCode == null) {
                        logger.info("Folder is password protected");
                    } else {
                        logger.info("User entered invalid password: " + passCode);
                    }
                    if (passwordAttempts >= 3) {
                        logger.info("Too many wrong password attempts");
                        break;
                    } else {
                        passCode = getUserInput("Password?", param);
                        askedUserForPassword = true;
                        passwordAttempts++;
                        continue;
                    }
                } else {
                    if (passCode != null && askedUserForPassword) {
                        logger.info("User has entered correct password: " + passCode);
                        userHasEnteredCorrectPassword = true;
                    }
                    break;
                }
            } while (true);
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            final Map<String, Object> data = (Map<String, Object>) resp.get("data");
            final Map<String, Object> pagination = (Map<String, Object>) resp.get("pagination");
            if (fp == null) {
                final String currentFolderName = data.get("name").toString();
                path = currentFolderName;
                final List<Map<String, Object>> breadcrumbs = (List<Map<String, Object>>) data.get("breadcrumbs");
                if (breadcrumbs != null && breadcrumbs.size() > 0) {
                    for (final Map<String, Object> breadcrumb : breadcrumbs) {
                        path = breadcrumb.get("name") + "/" + path;
                    }
                }
                fp = FilePackage.getInstance();
                fp.setName(path);
                logger.info("Crawling folder: " + currentFolderName + " | Full path: " + path);
            }
            final List<Map<String, Object>> files = (List<Map<String, Object>>) resp.get("files");
            final List<Map<String, Object>> subfolders = (List<Map<String, Object>>) resp.get("subfolders");
            if ((files == null || files.isEmpty()) && (subfolders == null || subfolders.isEmpty())) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + path);
            }
            int numberofItemsOnThisPage = 0;
            if (files != null) {
                numberofItemsOnThisPage += files.size();
                for (final Map<String, Object> file : files) {
                    final DownloadLink link = this.createDownloadlink("https://" + this.getHost() + "/folder/" + file.get("folder_share_key") + "/file/" + file.get("key"));
                    link.setFinalFileName(file.get("name").toString());
                    link.setVerifiedFileSize(((Number) file.get("size")).longValue());
                    link.setAvailable(true);
                    link.setRelativeDownloadFolderPath(path);
                    if (passCode != null) {
                        /*
                         * Download password can be different from folder password or even none but let's set this password here so if
                         * needed, it will at least be tried first.
                         */
                        link.setDownloadPassword(passCode);
                    }
                    link._setFilePackage(fp);
                    ret.add(link);
                    distribute(link);
                }
            }
            if (subfolders != null) {
                numberofItemsOnThisPage += subfolders.size();
                for (final Map<String, Object> subfolder : subfolders) {
                    // final int total_files = ((Number) subfolder.get("total_files")).intValue();
                    final DownloadLink link = this.createDownloadlink("https://" + this.getHost() + "/folder/" + subfolder.get("folder_share_key") + "/folder/" + subfolder.get("key"));
                    if (passCode != null) {
                        link.setDownloadPassword(passCode);
                    }
                    ret.add(link);
                    distribute(link);
                }
            }
            final int pageMax = ((Number) pagination.get("pages")).intValue();
            logger.info("Crawled page " + page + "/" + pageMax + " | Items on this page: " + numberofItemsOnThisPage + " of max " + pagination.get("per_page") + " | Found items so far: " + ret.size() + " / " + pagination.get("total"));
            if (page >= pageMax) {
                logger.info("Stopping because: Reached last page: " + page);
                break;
            } else {
                page++;
                query.addAndReplace("page", Integer.toString(page));
            }
        } while (true);
        return ret;
    }
}
