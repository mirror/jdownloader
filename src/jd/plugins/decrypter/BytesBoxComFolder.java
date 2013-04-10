//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision: 17642 $", interfaceVersion = 2, names = { "bytesbox.com" }, urls = { "https?://(www\\.)?bytesbox\\.com/\\!{2}/[A-Za-z0-9]+" }, flags = { 0 })
public class BytesBoxComFolder extends PluginForDecrypt {

    public BytesBoxComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getPage(parameter);
        String protocol = new Regex(parameter, "(https?)://").getMatch(0);
        String id = new Regex(parameter, "bytesbox\\.com/\\!{2}/([A-Za-z0-9]+)").getMatch(0);
        if (br.containsHTML("Folder doesn't exist\\!")) {
            logger.info("Folder invalid or does not exist any longer. " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h3[^>]+>(.*?)</h3>").getMatch(0);
        String reg = "(<div[^>]+><span[^>]+>(\\d+(\\.\\d+)? ?(KB|MB|GB))[^\r\n\t]+<a href=\"(/\\!/[a-zA-Z0-9]+/?)\"> ([^\r\n\t]+) </a></div>)";
        String[] results = br.getRegex(reg).getColumn(0);
        if (results != null && results.length != 0) {
            for (String result : results) {
                String filesize = new Regex(result, reg).getMatch(1);
                String url = new Regex(result, reg).getMatch(4);
                String filename = new Regex(result, reg).getMatch(5);

                if (url.matches("/\\!/[a-zA-Z0-9]+/?")) url = protocol + "://bytesbox.com" + url;

                DownloadLink dl = createDownloadlink(url);
                dl.setName(filename.trim());
                dl.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

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