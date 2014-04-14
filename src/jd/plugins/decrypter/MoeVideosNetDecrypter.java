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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moevideos.net" }, urls = { "http://(www\\.)?moevideos?\\.net/((\\?page=video\\&uid=|video/|video\\.php\\?file=|swf/letplayerflx3\\.swf\\?file=)[0-9a-f\\.]+|online/\\d+)" }, flags = { 0 })
public class MoeVideosNetDecrypter extends PluginForDecrypt {

    public MoeVideosNetDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        /* uid */
        String uid = new Regex(parameter, "uid=(.*?)$").getMatch(0);
        if (uid == null) uid = new Regex(parameter, "(video/|file=)(.*?)$").getMatch(1);
        if (uid == null) {
            br.getPage(parameter);

            if (br.containsHTML("Vídeo no existe posiblemente")) {
                final DownloadLink offline = createDownloadlink("http://moevideosdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(100000));
                offline.setName(new Regex(parameter, "moevideos\\.net/(.+)").getMatch(0));
                offline.setProperty("offline", true);
                offline.setAvailable(false);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }

            final Form iAmHuman = br.getFormbyProperty("name", "formulario");
            if (iAmHuman == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            iAmHuman.remove("enviar");

            /* Waittime is still skippable */
            // String waittime = br.getRegex("var tiempo = (\\d+);").getMatch(0);
            // int wait = 5;
            // if (waittime != null) wait = Integer.parseInt(waittime);
            // sleep(wait * 1001l, downloadLink);

            br.submitForm(iAmHuman);

            if (br.containsHTML("Vídeo no existe posiblemente")) {
                final DownloadLink offline = createDownloadlink("http://moevideosdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(100000));
                offline.setName(new Regex(parameter, "moevideos\\.net/(.+)").getMatch(0));
                offline.setProperty("offline", true);
                offline.setAvailable(false);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }

            uid = br.getRegex("file=([0-9a-f\\.]+)(\\&|\"|\\')").getMatch(0);
            if (uid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

        }
        br.postPage("http://api.letitbit.net/", "r=[\"tVL0gjqo5\",[\"preview/flv_image\",{\"uid\":\"" + uid + "\"}],[\"preview/flv_link\",{\"uid\":\"" + uid + "\"}]]");

        if (br.containsHTML("\"not_found\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        } else {
            String letilink = br.getRegex("\"link\":\"([^\"]+)").getMatch(0);
            if (letilink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            letilink = letilink.replaceAll("\\\\", "");
            String filename = new Regex(letilink, "/[0-9a-f]+_\\d+_(.*?)\\.flv").getMatch(0);
            if (filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            final DownloadLink fina = createDownloadlink("http://letitbit.net/download/" + uid + "/" + filename + ".html");
            final String fsize = br.getRegex("\"convert_size\":\"(\\d+)\"").getMatch(0);
            if (fsize != null) fina.setDownloadSize(Long.parseLong(fsize));
            fina.setName(filename);
            fina.setAvailable(true);
            decryptedLinks.add(fina);
            return decryptedLinks;
        }

    }

}
