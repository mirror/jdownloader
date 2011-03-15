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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiup.org" }, urls = { "http://[\\w\\.]*?multiup\\.org/(\\?lien=.+|fichiers/download.+)" }, flags = { 0 })
public class MultiupOrg extends PluginForDecrypt {

    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("(Sorry but your file does not exist or no longer exists|The file does not exist any more|It was deleted either further to a complaint or further to a not access for several weeks|<h2>Not Found</h2>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        Thread.sleep(1000l);
        br.postPage(br.getURL(), "_method=POST&data%5BFichier%5D%5Bsecurity_code%5D=");
        String[] links = br.getRegex("<a target=\"_blank\" href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            if (!dl.contains("multiup.org/")) decryptedLinks.add(createDownloadlink(dl));

        return decryptedLinks;
    }

}
