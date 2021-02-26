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

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "workers.dev" }, urls = { "https?://(?:[a-z0-9\\-\\.]+\\.)?workers\\.dev/.+" })
public class GoogleDriveDirectoryIndex extends PluginForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { getHost() };
    }

    public GoogleDriveDirectoryIndex(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.toString().contains("?")) {
            /* Remove all parameters */
            param.setCryptedUrl(param.getCryptedUrl().substring(0, param.getCryptedUrl().lastIndexOf("?")));
        }
        br.setAllowedResponseCodes(new int[] { 500 });
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null) {
            // TODO: add basicAuth support
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.GoogleDriveDirectoryIndex) plg).login(acc, false);
        }
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        final URLConnectionAdapter con = br.openPostConnection(param.getCryptedUrl(), "password=&page_token=&page_index=0");
        if (con.isContentDisposition()) {
            con.disconnect();
            final DownloadLink dl = new DownloadLink(null, null, this.getHost(), param.getCryptedUrl(), true);
            dl.setAvailable(true);
            dl.setName(Plugin.getFileNameFromHeader(con));
            dl.setDownloadSize(con.getCompleteContentLength());
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else {
            br.followConnection();
        }
        if (br.getHttpConnection().getResponseCode() == 401) {
            if (acc != null) {
                /* We cannot check accounts so the only way we can find issues is by just trying with the login credentials here ... */
                logger.info("Existing account is invalid (?)");
                acc.setError(AccountError.INVALID, 5 * 60, null);
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        } else if (br.containsHTML("\"rateLimitExceeded\"")) {
            throw new DecrypterRetryException(RetryReason.HOST, "Rate Limit Exceeded");
        }
        crawlFolder(decryptedLinks, param);
        return decryptedLinks;
    }

    private void crawlFolder(ArrayList<DownloadLink> decryptedLinks, final CryptedLink param) throws Exception {
        final FilePackage fp = FilePackage.getInstance();
        final boolean isParameterFile = !param.getCryptedUrl().endsWith("/");
        String subFolder = getAdoptedCloudFolderStructure();
        /*
         * ok if the user imports a link just by itself should it also be placed into the correct packagename? We can determine this via url
         * structure, else base folder with files wont be packaged together just on filename....
         */
        if (subFolder == null) {
            subFolder = "";
            final String[] split = param.getCryptedUrl().split("/");
            final String fpName = Encoding.urlDecode(split[split.length - (isParameterFile ? 2 : 1)], false);
            fp.setName(fpName);
            subFolder = fpName;
        } else {
            final String fpName = subFolder.substring(subFolder.lastIndexOf("/") + 1);
            fp.setName(fpName);
        }
        final String baseUrl;
        // urls can already be encoded which breaks stuff, only encode non-encoded content
        if (!new Regex(param.getCryptedUrl(), "%[a-z0-9]{2}").matches()) {
            baseUrl = Encoding.urlEncode_light(param.getCryptedUrl());
        } else {
            baseUrl = param.getCryptedUrl();
        }
        /* TODO: Add pagination */
        int page = 0;
        do {
            logger.info("Crawling page: " + (page + 1));
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final String nextPageToken = (String) entries.get("nextPageToken");
            if (nextPageToken != null) {
                logger.info("Pagination required for: " + param.getCryptedUrl());
            }
            final List<Object> ressourcelist;
            Object filesArray = JavaScriptEngineFactory.walkJson(entries, "data/files");
            if (filesArray == null) {
                filesArray = JavaScriptEngineFactory.walkJson(entries, "files");
            }
            if (filesArray != null) {
                /* Multiple files */
                ressourcelist = (List<Object>) filesArray;
            } else {
                /* Probably single file */
                ressourcelist = new ArrayList<Object>();
                ressourcelist.add(entries);
            }
            for (final Object fileO : ressourcelist) {
                final Map<String, Object> entry = (Map<String, Object>) fileO;
                final String name = (String) entry.get("name");
                final String type = (String) entry.get("mimeType");
                final long filesize = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
                if (StringUtils.isEmpty(name) || StringUtils.isEmpty(type)) {
                    /* Skip invalid objects */
                    continue;
                }
                String url = baseUrl;
                if (type.endsWith(".folder")) {
                    // folder urls have to END in "/" this is how it works in browser no need for workarounds
                    url += Encoding.urlEncode_light(name) + "/";
                } else if (!isParameterFile) {
                    // do not this if base is a file!
                    url += Encoding.urlEncode_light(name);
                }
                final DownloadLink dl;
                if (type.endsWith(".folder")) {
                    dl = this.createDownloadlink(url);
                    final String thisfolder = subFolder + "/" + name;
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, thisfolder);
                } else {
                    dl = new DownloadLink(null, name, this.getHost(), url, true);
                    dl.setAvailable(true);
                    dl.setFinalFileName(name);
                    if (filesize > 0) {
                        dl.setDownloadSize(filesize);
                    }
                    if (StringUtils.isNotEmpty(subFolder)) {
                        dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
                    }
                }
                fp.add(dl);
                decryptedLinks.add(dl);
            }
            if (this.isAbort()) {
                break;
            } else if (StringUtils.isEmpty(nextPageToken)) {
                logger.info("Stopping because: Reached end");
                break;
            } else {
                page += 1;
                final UrlQuery query = new UrlQuery();
                query.add("password", "");
                query.add("page_index", Integer.toString(page));
                query.appendEncoded("page_token", nextPageToken);
                br.postPage(br.getURL(), query);
            }
        } while (true);
    }
}
