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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 8859 $", interfaceVersion = 2, names = { "minilink.us" }, urls = { "http://[\\w\\.]*?minilink\\.us/[A-Zaz0-9-_]+" }, flags = { 0 })
public class MnlnkUs extends PluginForDecrypt {

    public MnlnkUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (!br.containsHTML("Sorry, this short URL is not valid")) {
            String linkurl = br.getRegex("<iframe.*?src=\"(.*?)\".*?>").getMatch(0);
            if (linkurl == null) return null;
            decryptedLinks.add(createDownloadlink(linkurl));
        }
        return decryptedLinks;
    }

    // @Override
}
