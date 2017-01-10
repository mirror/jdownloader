//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "icerbox.com" }, urls = { "https?://(?:www\\.)?icerbox\\.com/folder/([A-Za-z0-9]{8})" })
public class IcerBoxCom extends antiDDoSForDecrypt {

    public IcerBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderid = new Regex(parameter, this.getSupportedLinks().pattern()).getMatch(0);
        getPage("https://icerbox.com/api/v1/folder?id=" + folderid);
        final String fpName = PluginJSonUtils.getJsonValue(br, "name");
        final String filesArray = PluginJSonUtils.getJsonArray(br, "files");
        if (!inValidate(filesArray)) {
            final String[] files = PluginJSonUtils.getJsonResultsFromArray(filesArray);
            if (files != null && files.length > 0) {
                for (String file : files) {
                    final String uid = PluginJSonUtils.getJsonValue(file, "id");
                    final String name = PluginJSonUtils.getJsonValue(file, "name");
                    final String size = PluginJSonUtils.getJsonValue(file, "size");
                    if (!inValidate(uid)) {
                        final DownloadLink d = createDownloadlink("https://icerbox.com/" + uid);
                        d.setVerifiedFileSize(Long.parseLong(size));
                        d.setName(name);
                        d.setAvailable(true);
                        decryptedLinks.add(d);
                    }
                }
            }
        }
        final String directoryArray = PluginJSonUtils.getJsonArray(br, "folders");
        if (!inValidate(directoryArray)) {
            final String[] files = PluginJSonUtils.getJsonResultsFromArray(directoryArray);
            if (files != null && files.length > 0) {
                for (String file : files) {
                    // for now just return uid, nitroflare mass linkcheck shows avialable status and other values we need!
                    final String uid = PluginJSonUtils.getJsonValue(file, "id");
                    if (!inValidate(uid)) {
                        decryptedLinks.add(createDownloadlink("https://icerbox.com/folder/" + uid));
                    }
                }
            }
        }
        if (br.containsHTML(">This folder is empty<")) {
            logger.info("Link offline (folder empty): " + parameter);
            return decryptedLinks;
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}