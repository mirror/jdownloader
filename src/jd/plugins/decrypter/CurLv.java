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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "coinurl.com" }, urls = { "http://(www\\.)?cur\\.lv/[a-z0-9]+" }, flags = { 0 })
public class CurLv extends PluginForDecrypt {

    public CurLv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("BANNED")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String lid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        br.getPage("http://schetu.net/h?cid=coinurl&a=t&r=");
        final String ticket = br.getRegex("schetunet\\(\\'([^<>\"]*?)\\'\\);").getMatch(0);
        if (ticket == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("http://cur.lv/redirect_curlv.php?code=" + lid + "&ticket=" + ticket + "&r=");
        final String continuelink = br.getRegex("\"(ntop\\.php[^<>\"]*?)\"").getMatch(0);
        if (continuelink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("http://cur.lv/" + continuelink);
        final String finallink = br.getRegex(">This short link redirects to <span style=\"font\\-weight: bolder;\">(http[^<>\"]*?)</span>").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
