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
import java.util.LinkedHashMap;

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
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "odrive.com" }, urls = { "https?://(?:www\\.)?odrive\\.com/(s/[a-f0-9\\-]+|folder/(.+))" })
public class OdriveCom extends PluginForDecrypt {
    public OdriveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String folderID = new Regex(parameter, "/(?:s|folder)/(.+)").getMatch(0);
        jd.plugins.hoster.OdriveCom.prepBR(this.br);
        // br.getPage("https://www.odrive.com/rest/weblink/get_metadata?weblinkUri=%2F" + this.getLinkID(link));
        int maxtries = 2;
        int tries = 0;
        String passCode = "";
        final UrlQuery query = new UrlQuery().parse(parameter);
        if (query.get("password") != null) {
            /* E.g. user adds folder --> Password protected --> Contains subfolders which will all require the same password. */
            passCode = query.get("password");
            /* Correct folderID */
            folderID = folderID.replaceAll("(\\?|\\&)password=" + passCode, "");
        }
        String errorCode = null;
        boolean passwordFailure = false;
        do {
            br.getPage("https://www.odrive.com/rest/weblink/list_folder?weblinkUri=%2F" + Encoding.urlEncode(folderID) + "&password=" + Encoding.urlEncode(passCode));
            errorCode = PluginJSonUtils.getJson(br, "errorCode");
            if (isOffline(br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (errorCode != null && errorCode.equals("404")) {
                passwordFailure = true;
                passCode = getUserInput("Password?", param);
            } else {
                passwordFailure = false;
            }
            tries++;
        } while (tries <= maxtries && passwordFailure);
        if (passwordFailure) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("data");
        // final String nextPageToken = (String) entries.get("nextPageToken");
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("items");
        if (ressourcelist.size() == 0) {
            logger.info("Empty folder?");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String subFolder = this.getAdoptedCloudFolderStructure();
        if (subFolder == null) {
            subFolder = "";
        }
        final UrlQuery querypw = new UrlQuery();
        querypw.add("password", passCode);
        for (final Object fileO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) fileO;
            final String fileType = (String) entries.get("fileType");
            final String title = (String) entries.get("name");
            String linkUri = (String) entries.get("linkUri");
            final String directlink = (String) entries.get("downloadUrl");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            if (StringUtils.isEmpty(title) || StringUtils.isEmpty(linkUri) || StringUtils.isEmpty(fileType)) {
                /* Skip invalid items */
                continue;
            }
            if (fileType.equalsIgnoreCase("folder")) {
                /* Subfolder --> Goes back into decrypter */
                if (!StringUtils.isEmpty(passCode)) {
                    linkUri += "?" + query.toString();
                }
                final DownloadLink dl = this.createDownloadlink("https://odrive.com/folder" + linkUri);
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder + "/" + title);
                decryptedLinks.add(dl);
            } else {
                /* Single file */
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
                if (StringUtils.isNotEmpty(subFolder)) {
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
                }
                if (!StringUtils.isEmpty(passCode)) {
                    dl.setDownloadPassword(passCode);
                }
                decryptedLinks.add(dl);
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
        return decryptedLinks;
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
