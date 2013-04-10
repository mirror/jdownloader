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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kino-dom.tv" }, urls = { "http://(www\\.)?kino-dom.tv/\\w+/\\d+-[a-zA-Z0-9-]+?\\.html" }, flags = { 0 })
public class KinoDomTv extends PluginForDecrypt {
    public KinoDomTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCustomCharset("windows-1251");
        br.getPage(parameter);

        final String fpName = br.getRegex("<title>(.*?)(?:&raquo;.*)</title>").getMatch(0);
        String xmlLinks = br.getRegex("<param [^>]*?value=\"[^\"]*?file=([a-zA-Z0-9:/\\.\\-]+)[^\"]*?\" */>").getMatch(0);
        if (xmlLinks == null) xmlLinks = br.getRegex("<embed [^>]*?flashvars=\"[^\"]*?file=([a-zA-Z0-9:/\\.\\-]+)[^\"]*?\" *>").getMatch(0);
        if (xmlLinks == null) return decryptedLinks;

        br.getPage(xmlLinks);
        final String xmlFile = br.toString();
        if (xmlFile == null) return decryptedLinks;

        String[] links = HTMLParser.getHttpLinks(xmlFile, "");
        if (links == null || links.length == 0) return null;

        DownloadLink dl;
        for (String elem : links)
            decryptedLinks.add(dl = createDownloadlink(elem));

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}