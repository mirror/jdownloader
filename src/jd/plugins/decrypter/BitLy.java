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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bit.ly" }, urls = { "https?://(?:www\\.)?bit\\.ly/[\\w]+" })
public class BitLy extends PluginForDecrypt {
    public BitLy(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String finallink = br.getRedirectLocation();
        // bit.ly (hosts many short link domains via dns alias), they have protection/warnings
        if (StringUtils.isNotEmpty(finallink) && new Regex(finallink, "https?://bitly\\.com/a/warning\\?hash=[a-z0-9]+.+").matches()) {
            final String newlink = new Regex(finallink, "&url=(.+)(?!&)?").getMatch(0);
            if (StringUtils.isNotEmpty(newlink)) {
                finallink = Encoding.urlDecode(newlink, false);
            }
        }
        if (StringUtils.isEmpty(finallink)) {
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">Something's wrong here|>Uh oh, bitly couldn't find a link for the|Page Not Found")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
