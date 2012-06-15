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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kindgirls.com" }, urls = { "http://(www\\.)?kindgirls\\.com/(video|gallery)/[a-z0-9_-]+(/[a-zA-Z0-9_-]+/\\d+/?)?" }, flags = { 0 })
// http://www.kindgirls.com/gallery/errotica/raisa/5948
public class KndGrlsCom extends PluginForDecrypt {

    public KndGrlsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = null;
        if (parameter.contains("com/gallery")) {
            // it's a gallery
            String[] links = br.getRegex("/></a><br /><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(/gal-\\d+/[a-z0-9]+_\\d+/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String finallink : links) {
                DownloadLink dlLink = createDownloadlink("directhttp://http://www.kindgirls.com" + finallink);
                // rename the files if the numbering is incorrect
                String fileName = dlLink.getName();
                Regex regex = new Regex(fileName, "(.*_)(\\d\\.[a-zA-Z0-9]+)$");
                if (regex.matches()) {
                    dlLink.forceFileName(regex.getMatch(0) + "0" + regex.getMatch(1));
                }
                decryptedLinks.add(dlLink);
            }
            // set the filepackage name
            String girlsname = br.getRegex("<h3>Photo.*<a href='.*'>([a-zA-Z0-9\\S-]+)</a>.*</h3>").getColumn(0)[0];
            if (girlsname != null) fpName = "Kindgirls - " + girlsname.trim();
        } else {
            // it's a video
            String link = br.getRegex("so\\.addParam\\('flashvars',.*file=(http://www\\.kindgirls\\.com//videos\\d+/[a-zA-Z0-9]+\\.m4v).*volume=.*").getMatch(0);
            if (link == null || link.length() == 0) {
                logger.warning("Variable 'link' not found, Please report issue to JDownloader Developement.");
                return null;
            }
            decryptedLinks.add(createDownloadlink("directhttp://" + link));
            String girlsname = br.getRegex("<h3>Video\\s.\\s([a-zA-Z0-9-_]+).*").getMatch(0);
            if (girlsname != null) fpName = "Kindgirls - " + girlsname.trim();
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
