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
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "odrive.com" }, urls = { "https?://(?:www\\.)?odrive\\.com/(s/[a-f0-9\\-]+|folder/(.+))" })
public class OdriveCom extends PluginForDecrypt {
    public OdriveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String folderID = new Regex(param.getCryptedUrl(), "/(?:s|folder)/(.+)").getMatch(0);
        jd.plugins.hoster.OdriveCom.prepBR(this.br);
        // br.getPage("https://www.odrive.com/rest/weblink/get_metadata?weblinkUri=%2F" + this.getLinkID(link));
        int maxPasswordTries = 2;
        int usedPasswordTries = 0;
        String passCode = "";
        final UrlQuery query = UrlQuery.parse(param.getCryptedUrl());
        if (query.get("password") != null) {
            /* E.g. user adds folder --> Password protected --> Contains subfolders which will all require the same password. */
            passCode = query.get("password");
            /* Correct folderID */
            folderID = folderID.replaceAll("(\\?|\\&)password=" + passCode, "");
        }
        String errorCode = null;
        boolean passwordFailure = false;
        do {
            br.getPage("https://www." + this.getHost() + "/rest/weblink/list_folder?weblinkUri=%2F" + Encoding.urlEncode(folderID) + "&password=" + Encoding.urlEncode(passCode));
            errorCode = PluginJSonUtils.getJson(br, "errorCode");
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (errorCode != null && errorCode.equals("404")) {
                passwordFailure = true;
                /* Try "stored" password on first attempt if available --> Ask used if no PW available or first try failed. */
                if (passCode == null || usedPasswordTries > 0) {
                    passCode = getUserInput("Password?", param);
                }
            } else {
                passwordFailure = false;
            }
            usedPasswordTries++;
        } while (usedPasswordTries <= maxPasswordTries && passwordFailure);
        if (passwordFailure) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        // final String nextPageToken = (String) entries.get("nextPageToken");
        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) data.get("items");
        String subFolderPath = this.getAdoptedCloudFolderStructure();
        if (subFolderPath == null) {
            subFolderPath = "";
        }
        if (ressourcelist.isEmpty()) {
            if (!StringUtils.isEmpty(subFolderPath)) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderID + "_" + subFolderPath);
            } else {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderID);
            }
        }
        final UrlQuery querypw = new UrlQuery();
        querypw.add("password", passCode);
        for (final Map<String, Object> file : ressourcelist) {
            final String fileType = file.get("fileType").toString();
            final String title = file.get("name").toString();
            String linkUri = (String) file.get("linkUri");
            final long filesize = JavaScriptEngineFactory.toLong(file.get("size"), 0);
            if (fileType.equalsIgnoreCase("folder")) {
                /* Subfolder --> Goes back into decrypter */
                if (!StringUtils.isEmpty(passCode)) {
                    linkUri += "?" + querypw.toString();
                }
                final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/folder" + linkUri);
                dl.setRelativeDownloadFolderPath(subFolderPath + "/" + title);
                ret.add(dl);
            } else {
                /* Single file */
                final String directlink = file.get("downloadUrl").toString();
                final DownloadLink dl = this.createDownloadlink("http://odrivedecrypted" + linkUri);
                dl.setFinalFileName(title);
                dl.setAvailable(true);
                if (filesize > 0) {
                    dl.setDownloadSize(filesize);
                }
                dl.setLinkID(folderID + "_" + title);
                if (!StringUtils.isEmpty(directlink)) {
                    dl.setProperty("directlink", directlink);
                    dl.setContentUrl(directlink);
                }
                /* Properties required to find this item later in order to find our final downloadlink */
                dl.setProperty("folderid", folderID);
                dl.setProperty("directfilename", title);
                if (StringUtils.isNotEmpty(subFolderPath)) {
                    dl.setRelativeDownloadFolderPath(subFolderPath);
                }
                if (!StringUtils.isEmpty(passCode)) {
                    dl.setDownloadPassword(passCode);
                }
                ret.add(dl);
            }
        }
        // String fpName = br.getRegex("").getMatch(0);
        // final String[] links = br.getRegex("").getColumn(0);
        // if (links == null || links.length == 0) {
        // logger.warning("Decrypter broken for link: " + parameter);
        // return null;
        // }
        // for (final String singleLink : links) {
        // decryptedLinks.add(createDownloadlink(singleLink));
        // }
        // if (fpName != null) {
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName.trim()));
        // fp.addLinks(decryptedLinks);
        // }
        return ret;
    }

    public static boolean isOffline(final Browser br) {
        final String errorCode = PluginJSonUtils.getJson(br, "errorCode");
        if (errorCode != null) {
            if (errorCode.equals("202")) {
                return true;
            }
        }
        return false;
    }
}
