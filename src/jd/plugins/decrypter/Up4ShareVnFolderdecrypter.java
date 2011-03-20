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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up.4share.vn" }, urls = { "http://(www\\.)?up\\.4share\\.vn/(d|dlist)/[a-z0-9]+" }, flags = { 0 })
public class Up4ShareVnFolderdecrypter extends PluginForDecrypt {

    public Up4ShareVnFolderdecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("/d/", "/dlist/");
        br.getPage(parameter);
        System.out.print(br.toString());
        if (br.containsHTML(">Error: Not valid ID")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String linktext = br.getRegex("<div class=\\'content\\'><br /><br />(.*?)</div><br /><br").getMatch(0);
        if (linktext == null) return null;
        String[] links = linktext.split("<br />");
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));

        return decryptedLinks;
    }

}
