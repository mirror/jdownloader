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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "minus.com" }, urls = { "http://(www\\.)?(minus\\.com|min\\.us)/[A-Za-z0-9]+" }, flags = { 0 })
public class MinUsComDecrypter extends PluginForDecrypt {

    public MinUsComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("min.us/", "minus.com/");
        br.getPage(parameter);
        final String mainid = new Regex(parameter, "minus\\.com/(.+)").getMatch(0);
        if (br.containsHTML("(<h2>Not found\\.</h2>|<p>Our records indicate that the gallery/image you are referencing has been deleted or does not exist|The page you requested does not exist)")) {
            DownloadLink dl = createDownloadlink("http://i.minusdecrypted.com/340609783585/VTjbgttT_QsH/" + mainid + ".offline");
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String[] linkinfo = br.getRegex("\\{(\"name\":[^\\}]+)\\}").getColumn(0);
        if (linkinfo == null || linkinfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singlelinkinfo : linkinfo) {
            final String filename = new Regex(singlelinkinfo, "\"name\": \"([^<>\"/]+\\.[A-Za-z0-9]{1,5})\"").getMatch(0);
            final String filesize = new Regex(singlelinkinfo, "\"filesize_bytes\": (\\d+)").getMatch(0);
            final String secureprefix = new Regex(singlelinkinfo, "\"secure_prefix\":\"(/\\d+/[A-Za-z0-9\\-_]+)\"").getMatch(0);
            final String linkid = new Regex(singlelinkinfo, "\"id\": \"([A-Za-z0-9\\-_]+)\"").getMatch(0);
            if (filename == null || filesize == null || secureprefix == null || linkid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String filelink = "http://i.minusdecrypted.com" + secureprefix + "/d" + linkid + filename.substring(filename.lastIndexOf("."));
            DownloadLink dl = createDownloadlink(filelink);
            dl.setFinalFileName(filename);
            dl.setDownloadSize(Long.parseLong(filesize));
            dl.setAvailable(true);
            dl.setProperty("mainid", mainid);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }
}
