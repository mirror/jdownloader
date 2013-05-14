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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fshare.vn" }, urls = { "http://(www\\.)?(mega\\.1280\\.com|fshare\\.vn)/folder/[A-Z0-9]+" }, flags = { 0 })
public class FShareVnFolder extends PluginForDecrypt {

    public FShareVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("mega.1280.com", "fshare.vn");
        boolean failed = false;
        br.getPage(parameter);
        if (!br.containsHTML("filename")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String[] linkinformation = br.getRegex("(=\"http://(www\\.)?fshare\\.vn/file/[A-Z0-9]+/\"[^>]+><span class=\"filename\">[^<]+</span></a><br[^>]+>[\n\t\r ]+<span class=\"filesize\">[\\d\\.]+[^<]+</span>)").getColumn(0);
        if (linkinformation == null || linkinformation.length == 0) {
            failed = true;
            linkinformation = br.getRegex("(https?://(www\\.)?fshare\\.vn/file/[A-Z0-9]+)").getColumn(0);
        }
        if (linkinformation == null || linkinformation.length == 0) return null;
        for (String data : linkinformation) {
            if (failed) {
                decryptedLinks.add(createDownloadlink(data));
            } else {
                String filename = new Regex(data, "class=\"filename\">(.*?)</span").getMatch(0);
                String filesize = new Regex(data, "class=\"filesize\">(.*?)</span>").getMatch(0);
                String dlink = new Regex(data, "(http://(www\\.)?fshare\\.vn/file/[A-Z0-9]+)").getMatch(0);
                if (dlink == null) return null;
                DownloadLink aLink = createDownloadlink(dlink);
                if (filename != null) aLink.setName(filename.trim());
                if (filesize != null) aLink.setDownloadSize(SizeFormatter.getSize(filesize));
                if (filename != null && filesize != null) aLink.setAvailable(true);
                decryptedLinks.add(aLink);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}