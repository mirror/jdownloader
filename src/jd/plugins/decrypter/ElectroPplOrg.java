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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "electropeople.org" }, urls = { "http://(www\\.)?electropeople\\.org/[a-z0-9]+/\\d+.*?\\.html" }, flags = { 0 })
public class ElectroPplOrg extends PluginForDecrypt {

    public ElectroPplOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String fpName = br.getRegex("width=1 height=1 border=0></td><td bgcolor=\"#FFFFFF\" style=\"padding: 3px;\">(.*?)</td>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>(.*?) \\&raquo; ELECTROPEOPLE\\.ORG \\- Скачать MP3 бесплатно\\!</title>").getMatch(0);
        String theLinks = br.getRegex("type=\"application/x\\-shockwave-flash\" /></object><\\!\\-\\-dle_leech_begin\\-\\->(.*?)</div><br /><div class=\"title_quote\">").getMatch(0);
        if (theLinks == null) return null;
        String[] links = new Regex(theLinks, "\"(http://electropeople\\.org/engine/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String dl : links) {
            br.getPage(dl);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
