//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moviez.to" }, urls = { "http://[\\w\\.]*?moviez\\.to/(nojs|ddl/#/popup/).*?/\\d+/" }, flags = { 0 })
public class MovzTo extends PluginForDecrypt {

    public MovzTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("sonstige")) throw new DecrypterException("Unsupported format");
        if (!parameter.contains("/nojs/")) {
            String linkpart = new Regex(parameter, "moviez\\.to/ddl/#/popup/(.*?/\\d+/)").getMatch(0);
            if (linkpart == null) return null;
            linkpart = linkpart.replace("spiele", "gamez").replace("filme", "moviez").replace("musik", "mp3").replace("programme", "appz").replace("erotik", "xxx");
            parameter = "http://www.moviez.to/nojs/" + linkpart;
        }
        br.getPage(parameter);
        if (br.containsHTML("Sorry, f\\&uuml;r diese File haben wir leider keine Beschreibung")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("class=\"kategorien1\" style=\"font-size: 12px; text-align:center;\">(.*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<a title=\"(.*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("style=\"font-weight:800;\">Jetzt (.*?) <br />kostenlos aus dem Usenet downloaden\\.</td>").getMatch(0);
            }
        }
        String[] links = br.getRegex("<td class=\"row1\"><a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(http://linksave\\.in/[a-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links) {
            if (dl.contains("FriendlyDuck.com/") || dl.contains("firstload.de/")) continue;
            decryptedLinks.add(createDownloadlink(dl));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
