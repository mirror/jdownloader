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

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "lezlezlez.com" }, urls = { "http://(?:www\\.)?lezlezlez\\.com/mediaswf\\.php\\?type=vid\\&name=[^<>\"/]+\\.flv" })
public class LezlezlezCom extends PluginForDecrypt {
    public LezlezlezCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String name = UrlQuery.parse(param.getCryptedUrl()).get("name");
        final String finallink = "http://www.lezlezlez.com/vidz/" + name.replace(".flv", ".mp4");
        final String finalfilename = this.correctOrApplyFileNameExtension(name, ".mp4");
        final DownloadLink dl = createDownloadlink(finallink);
        dl.setFinalFileName(finalfilename);
        decryptedLinks.add(dl);
        return decryptedLinks;
    }
}
