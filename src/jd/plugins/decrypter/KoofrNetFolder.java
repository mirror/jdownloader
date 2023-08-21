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

import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.KoofrNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KoofrNetFolder extends PluginForDecrypt {
    public KoofrNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "koofr.net", "k00.fr", "koofr.eu" });
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/(links/[a-f0-9\\-]+.+|[A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().endsWith("/notfound")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String contentID = new Regex(br.getURL(), "(?i)/links/([a-f0-9\\-]+)").getMatch(0);
        if (contentID == null) {
            logger.info("Invalid URL or broken plugin");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        PluginForHost hosterplugin = null;
        String pathInternal = UrlQuery.parse(br.getURL()).get("path");
        if (pathInternal != null) {
            pathInternal = Encoding.htmlDecode(pathInternal);
        } else {
            /* Root */
            pathInternal = "/";
        }
        String passCode = param.getDecrypterPassword();
        final String host = br.getHost();
        final UrlQuery query = new UrlQuery();
        int passwordAttempt = 0;
        String url = null;
        boolean passwordSuccess = false;
        do {
            passwordAttempt++;
            if (passwordAttempt > 1) {
                passCode = getUserInput("Password?", param);
            }
            query.addAndReplace("password", passCode != null ? Encoding.urlEncode(passCode) : "");
            url = "https://app." + host + "/api/v2/public/links/" + contentID;
            br.getPage(url + "?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 401) {
                logger.info("Password required or previously entered password was wrong");
                continue;
            } else {
                passwordSuccess = true;
                break;
            }
        } while (!this.isAbort() && passwordAttempt < 4);
        if (!passwordSuccess) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        if (!Boolean.TRUE.equals(entries.get("isOnline"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String nameOfCurrentFolder = entries.get("name").toString();
        final String parentFolderNameStored = this.getAdoptedCloudFolderStructure();
        final String pathForJD;
        /* Try to always restore full path even if user adds subfolder */
        if (parentFolderNameStored == null && pathInternal.startsWith("/") && pathInternal.length() > 1 && !pathInternal.contains("/" + nameOfCurrentFolder)) {
            pathForJD = nameOfCurrentFolder + pathInternal;
        } else if (parentFolderNameStored != null) {
            pathForJD = parentFolderNameStored;
        } else {
            /* Root */
            pathForJD = nameOfCurrentFolder;
        }
        query.add("path", Encoding.urlEncode(pathInternal));
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(pathForJD);
        br.getPage(url + "/bundle?" + query.toString());
        final Map<String, Object> entries2 = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final List<Map<String, Object>> files = (List<Map<String, Object>>) entries2.get("files");
        for (final Map<String, Object> resource : files) {
            final String resourceName = resource.get("name").toString();
            final String resourceurl = "https://app.koofr.net/links/" + contentID + "?path=" + Encoding.urlEncode(pathInternal + "/" + resourceName);
            final String type = resource.get("type").toString();
            if (type.equalsIgnoreCase("dir")) {
                /* Folder */
                final DownloadLink folder = this.createDownloadlink(resourceurl);
                folder.setRelativeDownloadFolderPath(pathForJD + "/" + resourceName);
                ret.add(folder);
            } else {
                /* File */
                if (hosterplugin == null) {
                    /* Init hosterplugin */
                    hosterplugin = this.getNewPluginForHostInstance(this.getHost());
                }
                final DownloadLink file = new DownloadLink(hosterplugin, this.getHost(), resourceurl);
                KoofrNet.parseFileInfo(file, resource);
                file.setRelativeDownloadFolderPath(pathForJD);
                file.setAvailable(true);
                file._setFilePackage(fp);
                file.setProperty(KoofrNet.PROPERTY_FOLDER_ID, contentID);
                ret.add(file);
            }
        }
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + pathForJD);
        }
        /* Add additional information */
        for (final DownloadLink result : ret) {
            if (passCode != null) {
                result.setDownloadPassword(passCode, true);
            }
        }
        return ret;
    }
}
