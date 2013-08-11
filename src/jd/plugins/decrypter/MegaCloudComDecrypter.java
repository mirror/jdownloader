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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megacloud.com" }, urls = { "https?://(www\\.)?megacloud\\.com/s/[a-zA-Z0-9]{10,}(/[^/<> ]+)?|http://mc\\.tt/[a-zA-Z0-9]{10,}" }, flags = { 0 })
public class MegaCloudComDecrypter extends PluginForDecrypt {

    public MegaCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String SINGLELINK   = "https?://(www\\.)?megacloud\\.com/s/[a-zA-Z0-9]{10,}/[^/<> ]+";
    private static final String STANDARDLINK = "https?://(www\\.)?megacloud\\.com/s/[a-zA-Z0-9]{10,}(/[^/<> ]+)?";
    private static final String SHORTLINK    = "http://mc\\.tt/[a-zA-Z0-9]{10,}";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        if (parameter.matches(SINGLELINK)) {
            final DownloadLink dl = createDownloadlink(parameter.replace("megacloud.com/", "megaclouddecrypted.com/"));
            if (br.containsHTML(">Error 404<")) {
                dl.setAvailable(false);
                dl.setProperty("offline", true);
            } else {
                final String filter = br.getRegex("(\"type\":\"FILE\",.*?</script>)").getMatch(0);
                if (filter != null) {
                    String filename = new Regex(filter, "filename\":\"([^\"]+)").getMatch(0);
                    if (filename == null) br.getRegex("<h1 class=\"shareable_name\">([^<]+)</h1>").getMatch(0);
                    String filesize = new Regex(filter, "\"size\":(\\d+),").getMatch(0);
                    if (filename != null && filesize != null) {
                        dl.setName(Encoding.htmlDecode(filename.trim()));
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                        dl.setAvailable(true);
                    }
                }
            }
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else {
            br.getPage(parameter);
            if (br.containsHTML(">Error 404<")) {
                final DownloadLink offline = createDownloadlink(parameter.replace("megacloud.com/", "megaclouddecrypted.com/"));
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            if (parameter.matches(SHORTLINK) && !br.getURL().matches(STANDARDLINK)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            } else if (parameter.matches(SHORTLINK)) {
                parameter = br.getURL();
            }
            final String fpName = br.getRegex("\"filename\":\"([^<>\"]*?)\"").getMatch(0);
            final String systemfileid = br.getRegex("\"fileSystemId\":(\\d+)").getMatch(0);
            final String userid = br.getRegex("\"user_id\":(\\d+)").getMatch(0);
            final String hash = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
            if (fpName == null || systemfileid == null || userid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("https://www.megacloud.com/ajax/get_shareable_filelist.php?p=1&ps=10000&sf=1&so=0&sfsid=" + systemfileid + "&efsid=" + systemfileid + "&uid=" + userid + "&hash=" + hash);
            final String[] finfo = br.getRegex("(\"type\":\"FILE\",\"size\".*?\"filename\".*?\"file_system_id\":\\d+)").getColumn(0);
            if (finfo == null || finfo.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : finfo) {
                final String filesize = new Regex(singleLink, "\"size\":(\\d+)").getMatch(0);
                final String filename = new Regex(singleLink, "\"filename\":\"([^<>\"]*?)\"").getMatch(0);
                final DownloadLink dl = createDownloadlink(parameter.replace("megacloud.com/", "megaclouddecrypted.com/") + "/" + filename);
                dl.setName(filename);
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

}
