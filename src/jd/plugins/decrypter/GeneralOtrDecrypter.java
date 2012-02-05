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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "die-schnelle-kuh.de" }, urls = { "http://(www\\.)?die\\-schnelle\\-kuh\\.de/\\?file=[^<>\"\\']+" }, flags = { 0 })
public class GeneralOtrDecrypter extends PluginForDecrypt {

    public GeneralOtrDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("die-schnelle-kuh.de/")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            final String contnue = br.getRegex("onclick=\"window\\.location\\.href=\\'(\\?[^<>\"\\']+)\\'\"").getMatch(0);
            if (contnue == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("http://die-schnelle-kuh.de/" + contnue);
            String finallink = br.getRegex("type=\"text\" style=\"width:100px;\" value=\"(http://die\\-schnelle\\-kuh\\.de/[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://die\\-schnelle\\-kuh\\.de/index\\.php\\?kick\\&fileid=\\d+\\&ticket=\\d+\\&hash=[a-z0-9]+\\&filename=[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(finallink);
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.contains("tivootix.co.cc/")) {
        } else if (parameter.contains("otr-drive.com/")) {
        } else if (parameter.contains("otr-share.de/")) {
        } else if (parameter.contains("otr.seite.com/")) {
        }

        return decryptedLinks;
    }
}
