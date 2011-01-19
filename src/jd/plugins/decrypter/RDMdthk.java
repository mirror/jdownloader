//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 11183 $", interfaceVersion = 2, names = { "ardmediathek.de" }, urls = { "http://[\\w\\.]*?ardmediathek\\.de/ard/servlet/content/\\d+\\?documentId=\\d+" }, flags = { 0 })
public class RDMdthk extends PluginForDecrypt {

    public RDMdthk(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.toString());
        String[][] streams = br.getRegex("mediaCollection\\.addMediaStream\\((\\d+), (\\d+), \"(.*?)\", \"(.*?)\"\\);").getMatches();

        String title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        FilePackage filePackage = FilePackage.getInstance();
        filePackage.setName(title);
        for (String[] stream : streams) {
            // get quality id
            String q = new Regex(stream[3], "\\&amp;for=Web-(.)").getMatch(0);
            // create link
            // replace http with hrtmp to differ hoster links from

            DownloadLink link = createDownloadlink(param.toString().replace("http://", "hrtmp://") + "&q=" + q);
            link.setFilePackage(filePackage);
            if ("M".equals(q)) {
                link.setFinalFileName(title.trim() + "(medium_quality)" + ".mp4");
            } else if ("L".equals(q)) {
                link.setFinalFileName(title.trim() + "(high_quality)" + ".mp4");
            } else if ("S".equals(q)) {
                link.setFinalFileName(title.trim() + "(low_quality)" + ".mp4");
            }

            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }
}
