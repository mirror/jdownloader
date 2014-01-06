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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "proxer.me" }, urls = { "http://(www\\.)?proxer\\.me/watch/\\d+/\\d+/(ger|eng)sub" }, flags = { 0 })
public class ProxrMe extends PluginForDecrypt {

    public ProxrMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String replace = br.getRegex("\"replace\":\"(http[^<>\"]*?)\"").getMatch(0);
        String code = br.getRegex("\"code\":\"([^<>\"]*?)\"").getMatch(0);
        if (replace == null || code == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        replace = replace.replace("\\", "").replace("#", "");
        code = code.replace("\\", "").replace("#", "");
        final String finallink = replace + code;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }
}
