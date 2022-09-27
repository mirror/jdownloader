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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fshare.vn" }, urls = { "https?://(?:www\\.)?fshare\\.vn/folder/([A-Z0-9]+)" })
public class FShareVnFolder extends PluginForDecrypt {
    public FShareVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (folderid == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        jd.plugins.hoster.FShareVn.prepBrowserWebsite(br);
        /* Important or we'll get XML ;) */
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getPage("https://www." + getHost() + "/api/v3/files/folder?linkcode=" + folderid + "&sort=type,name");
        do {
            final Map<String, Object> map = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final List<Object> ressourcelist = (List<Object>) map.get("items");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (ressourcelist.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderid);
            }
            final Map<String, Object> entries = (Map<String, Object>) map.get("current");
            String fpName = (String) entries.get("name");
            if (StringUtils.isEmpty(fpName)) {
                fpName = folderid;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            for (final Object linkO : ressourcelist) {
                final Map<String, Object> linkInfo = (Map<String, Object>) linkO;
                // final String path = (String) entries2.get("path");
                final String linkcode = (String) linkInfo.get("linkcode");
                if (dupe.add(linkcode)) {
                    final String mimetype = (String) linkInfo.get("mimetype");
                    final String filename = (String) linkInfo.get("name");
                    final String description = (String) linkInfo.get("descrption");
                    String currentFolderPath = (String) linkInfo.get("path");
                    final long size = JavaScriptEngineFactory.toLong(linkInfo.get("size"), 0);
                    if (StringUtils.isEmpty(linkcode)) {
                        /* This should never happen */
                        continue;
                    }
                    final DownloadLink dl;
                    if (mimetype == null && size == 0) {
                        /* Folder */
                        dl = this.createDownloadlink("https://www." + this.getHost() + "/folder/" + linkcode);
                        if (StringUtils.isEmpty(currentFolderPath)) {
                            /* Workaround for bad serverside information */
                            currentFolderPath = filename;
                        }
                    } else {
                        /* File */
                        dl = this.createDownloadlink("https://www." + this.getHost() + "/file/" + linkcode);
                        if (size > 0) {
                            dl.setVerifiedFileSize(size);
                        }
                        if (!StringUtils.isEmpty(description)) {
                            dl.setComment(description);
                        }
                        dl.setFinalFileName(filename);
                        dl.setAvailable(true);
                        dl._setFilePackage(fp);
                    }
                    if (!StringUtils.isEmpty(currentFolderPath)) {
                        dl.setRelativeDownloadFolderPath(currentFolderPath);
                    }
                    ret.add(dl);
                    distribute(dl);
                }
            }
            final Map<String, Object> links = (Map<String, Object>) map.get("_links");
            if (links != null) {
                final String next = (String) links.get("next");
                if (next != null && dupe.add(next)) {
                    br.getPage("https://www." + this.getHost() + "/api" + next);
                    continue;
                }
            }
            break;
        } while (!this.isAbort());
        return ret;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}