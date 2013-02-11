//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 17655 $", interfaceVersion = 2, names = { "hornoxe.com" }, urls = { "http://(www\\.)?hornoxe\\.com/[^/]+/" }, flags = { 0 })
public class HrnOxCm extends PluginForDecrypt {

    public HrnOxCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);

        String pageName = br.getRegex("og:title\" content=\"(.*?)\" />").getMatch(0);
        if (pageName == null) pageName = br.getRegex("<title>(.*?) \\- Hornoxe\\.com</title>").getMatch(0);

        String file = br.getRegex("file\":\"(https?://videos\\.hornoxe\\.com/[^\"]+)").getMatch(0);

        String image = br.getRegex("image\":\"(https?://(www\\.)hornoxe\\.com/wp\\-content/images/\\d+[^\"]+)").getMatch(0);

        if (file == null || pageName == null) {
            return decryptedLinks;
        } else {
            pageName = Encoding.htmlDecode(pageName.trim());
            if (image != null && image.length() > 0) {
                DownloadLink img = createDownloadlink("directhttp://" + image);
                img.setFinalFileName(pageName + image.substring(image.lastIndexOf(".")));
                decryptedLinks.add(img);
            }
            DownloadLink vid = createDownloadlink(file.replace("hornoxe.com", "hornoxedecrypted.com"));
            vid.setFinalFileName(pageName + file.substring(file.lastIndexOf(".")));
            vid.setProperty("Referer", parameter);
            decryptedLinks.add(vid);
        }

        FilePackage fp = FilePackage.getInstance();
        fp.setName(pageName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
