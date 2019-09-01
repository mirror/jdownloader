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
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 41201 $", interfaceVersion = 2, names = { "sketchfab.com" }, urls = { "https?://(www\\.)?sketchfab\\.com/3d-models/.+" })
public class Sketchfab extends antiDDoSForDecrypt {
    public Sketchfab(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = null;
        if (br.containsHTML("<div class=\"viewer-viewport\">")) {
            fpName = br.getRegex("class=\"model-name__label\">([^<]+)</span").getMatch(0);
            String archiveLink = br.getRegex("(http[^#;]+ile.osgjs.gz)").getMatch(0);
            if (archiveLink != null && archiveLink.length() > 0) {
                String decodedLink = br.getURL(Encoding.htmlDecode(archiveLink)).toString().replace("file.osgjs.gz", "model_file.bin.gz");
                decryptedLinks.add(createDownloadlink(decodedLink));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}