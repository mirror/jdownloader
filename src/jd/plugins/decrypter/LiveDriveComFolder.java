//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import org.appwork.utils.Regex;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livedrive.com" }, urls = { "https?://(?:[A-Za-z0-9\\-]+\\.)?livedrive\\.com/.+" })
public class LiveDriveComFolder extends PluginForDecrypt {
    public LiveDriveComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /**
         * TODO: </br>
         * - check pagination handling </br>
         * - add support for subfolders </br>
         * - set appropriate package names and subfolderPaths </br>
         * - add errorhandling for empty folders
         */
        final String user = new Regex(br.getURL(), "https?://[^/]+/portal/public-shares/([^/]+)").getMatch(0);
        if (user == null) {
            /* Redirect to unsupported URL -> Must be offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        do {
            br.getPage("https://public.livedrive.com/portal/account/sharing/withme/" + user + "/files?count=50&includePublicShares=true&includePrivateShares=false");
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final List<Map<String, Object>> resourcelist = (List<Map<String, Object>>) entries.get("resourceList");
            for (final Map<String, Object> resource : resourcelist) {
                final String resourceID = resource.get("fileId").toString();
                final int resourceType = ((Number) resource.get("type")).intValue();
                if (resourceType == 1) {
                    final DownloadLink file = this.createDownloadlink("https://" + br.getHost(true) + "/portal/public-shares/" + user + "/file/*_" + Encoding.Base64Encode(resourceID));
                    file.setFinalFileName(resource.get("name").toString());
                    file.setVerifiedFileSize(((Number) resource.get("size")).intValue());
                    file.setAvailable(true);
                    ret.add(file);
                } else if (resourceType == 2) {
                    /* Folder */
                    // TODO
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final int pageCount = ((Number) entries.get("pageCount")).intValue();
            logger.info("Crawled page " + page + "/" + entries.get("pageCount") + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                /* Aborted by user */
                break;
            } else if (page == pageCount) {
                logger.info("Stopping because: Reached end");
                break;
            }
        } while (true);
        return ret;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}