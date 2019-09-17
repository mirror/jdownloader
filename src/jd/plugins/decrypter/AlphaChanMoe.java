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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision: svn:Revision $", interfaceVersion = 3, names = { "alphachan.moe" }, urls = { "https?://(?:www\\.)?alphachan\\.moe/(?:play2|player)/.+" })
public class AlphaChanMoe extends antiDDoSForDecrypt {
    public AlphaChanMoe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        if (parameter.contains("/player/link/")) {
            decryptedLinks.add(createDownloadlink("directhttp://" + parameter));
            return decryptedLinks;
        }
        getPage(parameter);
        String[] links = br.getRegex("<source[^>]+src=\"([^\"]+)\"").getColumn(0);
        if (links != null && links.length > 0) {
            for (String link : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        return decryptedLinks;
    }
}