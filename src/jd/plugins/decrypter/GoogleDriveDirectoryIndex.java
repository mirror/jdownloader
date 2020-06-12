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
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

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
        String parameter = param.toString();
        if (parameter.contains("?")) {
            /* Remove all parameters */
            parameter = parameter.substring(0, parameter.lastIndexOf("?"));
        }
        br.setAllowedResponseCodes(new int[] { 500 });
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null) {
            final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
            plg.setBrowser(this.br);
            ((jd.plugins.hoster.GoogleDriveDirectoryIndex) plg).login(acc, false);
            this.br = plg.getBrowser();
        }
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        br.postPageRaw(parameter, "{\"password\":\"null\"}");
        if (br.getHttpConnection().getResponseCode() == 401) {
            if (acc != null) {
                /* We cannot check accounts so the only way we can find issues is by just trying with the login credentials here ... */
                logger.info("Existing account is invalid (?)");
                acc.setError(AccountError.INVALID, 5 * 60, null);
            }
            decryptedLinks.add(this.createOfflinelink(parameter, "account_required", "account_required"));
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        doThis(decryptedLinks, parameter);
        return decryptedLinks;
    }

    private void doThis(ArrayList<DownloadLink> decryptedLinks, String parameter) throws Exception {
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        final boolean isParameterFile = !parameter.endsWith("/");
        String subFolder = getAdoptedCloudFolderStructure();
        if (subFolder == null) {
            subFolder = "";
        }
        final FilePackage fp = FilePackage.getInstance();
        /*
         * ok if the user imports a link just by itself should it also be placed into the correct packagename? We can determine this via url
         * structure, else base folder with files wont be packaged together just on filename....
         */
        if ("".equals(subFolder)) {
            final String[] split = parameter.split("/");
            final String fpName = Encoding.urlDecode(split[split.length - (isParameterFile ? 2 : 1)], false);
            fp.setName(fpName);
            subFolder = fpName;
        } else {
            final String fpName = subFolder.substring(subFolder.lastIndexOf("/") + 1);
            fp.setName(fpName);
        }
        final String baseUrl;
        // urls can already be encoded which breaks stuff, only encode non-encoded content
        if (!new Regex(parameter, "%[a-z0-9]{2}").matches()) {
            baseUrl = Encoding.urlEncode_light(parameter);
        } else {
            baseUrl = parameter;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Object filesO = entries.get("files");
        final ArrayList<Object> ressourcelist;
        if (filesO != null) {
            /* Multiple files */
            ressourcelist = (ArrayList<Object>) entries.get("files");
        } else {
            /* Probably single file */
            ressourcelist = new ArrayList<Object>();
            ressourcelist.add(entries);
        }
        for (final Object fileO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) fileO;
            final String name = (String) entries.get("name");
            final String type = (String) entries.get("mimeType");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
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
                dl = new DownloadLink(plg, name, this.getHost(), url, true);
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
    }
}
