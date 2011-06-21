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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 11183 $", interfaceVersion = 2, names = { "ardmediathek.de" }, urls = { "http://[\\w\\.]*?ardmediathek\\.de/ard/servlet/content/\\d+\\?documentId=\\d+" }, flags = { PluginWrapper.DEBUG_ONLY })
public class RDMdthk extends PluginForDecrypt {

    public RDMdthk(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.toString());
        final String[][] streams = br.getRegex("mediaCollection\\.addMediaStream\\((\\d+), (\\d+), \"(.*?)\", \"(.*?)\"\\);").getMatches();

        final String title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        final FilePackage filePackage = FilePackage.getInstance();
        filePackage.setName(Encoding.htmlDecode(title));
        for (final String[] stream : streams) {
            // get quality id
            final int q = Integer.valueOf(stream[1]);
            // get streamtype id
            final int t = Integer.valueOf(stream[0]);
            // create link
            // replace http with hrtmp to differ hoster links from
            final DownloadLink link = createDownloadlink(param.toString().replace("http://", "hrtmp://") + "&q=" + q + "&t=" + t);
            filePackage.add(link);
            // parse file extension
            String ext = new Regex(stream[stream.length - 1], "(\\.\\w{3})$").getMatch(0);
            ext = ext == null ? ".mp4" : ext;
            if (q == 0) {
                link.setFinalFileName(title.trim() + "(low_quality)" + ext);
            } else if (q == 1) {
                link.setFinalFileName(title.trim() + "(medium_quality)" + ext);
            } else if (q == 2) {
                link.setFinalFileName(title.trim() + "(high_quality)" + ext);
            } else if (q == 3) {
                link.setFinalFileName(title.trim() + "(hd_quality)" + ext);
            }
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }
}
