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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "loshare.com" }, urls = { "http://[\\w\\.]*?loshar(e|a)\\.com/folders\\.php\\?fid=([a-zA-Z0-9_-]+\\&s=[0-9]+|[a-zA-Z0-9_-]+)" }, flags = { 0 })
public class LoShareComFolder extends PluginForDecrypt {

    public LoShareComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter.replace("loshara", "loshare"));
        if (br.containsHTML("Folder does not exist")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] links = br.getRegex("(download\\.php\\?id=[a-zA-Z0-9_-]+)\"").getColumn(0);
        if (links.length == 0) return null;
        for (String hosterlink : links)
            decryptedLinks.add(createDownloadlink("http://loshare.com/" + hosterlink));

        return decryptedLinks;
    }
}
