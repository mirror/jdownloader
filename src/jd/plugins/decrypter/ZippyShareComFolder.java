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
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zippyshare.com" }, urls = { "https?://(?:(?:www|m)\\.)?zippyshare\\.com/([a-z0-9\\-_%,]+)(/([a-z0-9\\-_%]+)/dir\\.html)?" })
public class ZippyShareComFolder extends PluginForDecrypt {
    public ZippyShareComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        br.addAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // offline ?
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">User [^<]+ does not exist\\.</div>")) {
            // not worth adding offline link
            return decryptedLinks;
        }
        final String uploader = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String folderID = new Regex(parameter, this.getSupportedLinks()).getMatch(2);
        String currentFolderName = null;
        /* Do not search for name of current folder if we're crawling the root- folder of a user -> Use the username as folder name then. */
        if (folderID != null) {
            logger.info("Trying to find name of current folder");
            try {
                final Browser brc = br.cloneBrowser();
                brc.getPage("/rest/public/getTree?user=" + uploader + "&ident=" + Encoding.urlEncode(folderID) + "&id=%23");
                Map<String, Object> entries;
                final List<Object> foldersO = JSonStorage.restoreFromString(brc.toString(), TypeRef.LIST);
                for (final Object folderO : foldersO) {
                    entries = (Map<String, Object>) folderO;
                    currentFolderName = findNameOfCurrentFolder(folderID, entries);
                    if (currentFolderName != null) {
                        break;
                    }
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
            if (!StringUtils.isEmpty(currentFolderName)) {
                logger.info("Successfully found name of current folder: " + currentFolderName);
            } else {
                logger.info("Failed to find name of current folder");
            }
        }
        /* Over 250 items? Check if there is more ... */
        final int pageSize = 250;
        int page = 0;
        do {
            logger.info("Crawling page: " + (page + 1));
            final Browser br = this.br.cloneBrowser();
            final int dsize = decryptedLinks.size();
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/fragments/publicDir/filetable.jsp", "page=" + (dsize / pageSize) + "&user=" + uploader + "&dir=" + (folderID != null ? folderID : "0") + "&sort=nameasc&pageSize=" + pageSize + "&search=&viewType=default");
            if (br.getHttpConnection().getResponseCode() == 500) {
                // invalid directory
                return decryptedLinks;
            }
            parseLinks(decryptedLinks, br);
            if (decryptedLinks.size() != dsize + pageSize) {
                break;
            }
            page += 1;
        } while (!this.isAbort());
        // user directory
        final FilePackage fp = FilePackage.getInstance();
        if (!StringUtils.isEmpty(currentFolderName)) {
            fp.setName(currentFolderName);
        } else if (uploader != null && folderID != null) {
            fp.setName("User Directory - " + uploader + " - " + folderID);
        } else {
            fp.setName("User Directory - " + uploader);
        }
        fp.addLinks(decryptedLinks);
        if (decryptedLinks.isEmpty()) {
            logger.info("Failed to find any downloadable content");
        }
        return decryptedLinks;
    }

    /** Recursive function which finds the name of the currently open folder inside the folder tree. */
    private String findNameOfCurrentFolder(final String currentFolderID, final Map<String, Object> entries) {
        final String folderIDTmp = (String) JavaScriptEngineFactory.walkJson(entries, "li_attr/ident");
        if (folderIDTmp != null && folderIDTmp.equals(currentFolderID)) {
            return (String) entries.get("text");
        } else {
            final List<Object> childrenO = (List<Object>) entries.get("children");
            for (final Object childO : childrenO) {
                final String foldername = this.findNameOfCurrentFolder(currentFolderID, (Map<String, Object>) childO);
                if (foldername != null) {
                    return foldername;
                }
            }
        }
        return null;
    }

    private void parseLinks(final ArrayList<DownloadLink> decryptedLinks, final Browser br) throws PluginException {
        // lets parse each of the results and keep them trusted as online... this will reduce server loads
        String[] results = br.getRegex("<tr[^>]+class=(\"|')filerow even\\1.*?</tr>").getColumn(-1);
        if (results == null || results.length == 0) {
            results = br.getRegex("<div style[^>]+>\\s*<div style.*?</div>\\s*</div>").getColumn(-1);
        }
        if (results != null) {
            for (final String result : results) {
                final String link = new Regex(result, "\"((?:https?:)?(?://www\\d+\\.zippyshare\\.com)?/v/[a-zA-Z0-9]+/file\\.html)\"").getMatch(0);
                final String name = new Regex(result, ">([^\r\n]+)</a>").getMatch(0);
                final String size = new Regex(result, ">\\s*(\\d+(?:[\\.,]\\d+)?\\s+(?:B(?:yte)?|KB|MB|GB))\\s*</td>").getMatch(0);
                if (link == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink dl = createDownloadlink(Request.getLocation(link, br.getRequest()));
                if (name != null) {
                    dl.setName(Encoding.htmlOnlyDecode(name));
                }
                if (size != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(size.replace(",", ".")));
                }
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}