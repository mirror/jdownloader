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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafiremoviez.com" }, urls = { "http://(www\\.)?mediafiremoviez\\.com/paste/[a-zA-Z0-9]+" }, flags = { 0 })
public class MdfMvzCm extends PluginForDecrypt {

    public MdfMvzCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Error 404")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String pasteText = br.getRegex("data\\-offset=\"\\d+\"(.*?)</pre>").getMatch(0);
        if (pasteText == null) pasteText = br.toString();
        final String[] links = HTMLParser.getHttpLinks(pasteText, "");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links)
            if (!singleLink.contains("mediafiremoviez.com/")) decryptedLinks.add(createDownloadlink(singleLink));

        return decryptedLinks;
    }

}
