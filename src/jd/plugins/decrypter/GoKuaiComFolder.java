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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gokuai.com" }, urls = { "https?://(www\\.)?gokuai\\.com/a/[a-zA-Z0-9]{16}(/[a-z0-9]{40})?" }, flags = { 0 })
public class GoKuaiComFolder extends PluginForDecrypt {

    public GoKuaiComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();

        // if link is a file within (directory|folder) we only have to return
        // link to hoster plugin via gokuais?://
        if (parameter.matches("https?://(www\\.)?gokuai\\.com/a/[a-zA-Z0-9]{16}/[a-z0-9]{40}")) {
            if (parameter.contains("https://")) {
                parameter = parameter.replace("https://", "gokuais://");
            }
            if (parameter.contains("http://")) {
                parameter = parameter.replace("http://", "gokuai://");
            }
            decryptedLinks.add(createDownloadlink(parameter));
        }

        // if link is a folder, find links within.
        if (parameter.matches("https?://(www\\.)?gokuai\\.com/a/[a-zA-Z0-9]{16}")) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            br.setFollowRedirects(false);

            // return error message for invalid url
            if (br.containsHTML("<div class=\"activate\\_error\">[\r\n\\s]+<div class=\"activate\\_title\">该文件夹不存在或已被取消了发布</div>")) {
                logger.warning("Invalid URL: " + parameter);
                return decryptedLinks;
            }

            // find a package name, set null value to prevent unnamed packages
            // breaking plugin
            String fpName = br.getRegex("<h2><i class=\"icon_folder\"></i><span>(.*?)</span></h2>").getMatch(0);
            if (fpName == null) fpName = "Untitled";

            // find the links
            String[] links = br.getRegex("<div class=\"filename\">[\r\n\\s]+<a target=\"\\_blank\" href=\"(.*?)\"").getColumn(0);

            // return error message for empty folders
            if ((links == null || links.length == 0)) {
                logger.warning("Empty folder OR this Decrypter plugin could be broken, please check in your browser: " + parameter);
                return null;
            }

            // return found links with the according protocol
            if (links != null && links.length != 0) {
                for (String dl : links)
                    if (parameter.contains("https://")) {
                        decryptedLinks.add(createDownloadlink("gokuais://www.gokuai.com" + dl));
                    } else if (parameter.contains("http://")) {
                        decryptedLinks.add(createDownloadlink("gokuai://www.gokuai.com" + dl));
                    }
            }

            // return links within package name
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}