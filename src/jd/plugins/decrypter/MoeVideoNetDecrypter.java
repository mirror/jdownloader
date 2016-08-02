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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moevideo.net" }, urls = { "https?://(?:www\\.)?(?:(?:moevideos|moevideo|videochart)\\.net|playreplay\\.me)/.+" }, flags = { 0 })
public class MoeVideoNetDecrypter extends PluginForDecrypt {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "moevideo.net", "videochart.net", "playreplay.me" };
        // moevideos.net parked.
    }

    public MoeVideoNetDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.contains("moevideos.net/.+")) {
            /* 2016-03-01: This domain is parked */
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* uid */
        String uid = new Regex(parameter, "uid=(.*?)$").getMatch(0);
        if (uid == null) {
            uid = new Regex(parameter, "(video/|file=)(.*?)$").getMatch(1);
        }
        if (uid == null) {
            br.setFollowRedirects(true);
            br.getPage(parameter);

            if (br.containsHTML("Vídeo no existe posiblemente") || !this.br.getHttpConnection().getContentType().contains("html") || this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }

            final Form iAmHuman = br.getFormbyProperty("name", "formulario");
            if (iAmHuman != null) {
                iAmHuman.remove("enviar");

                /* Waittime is still skippable */
                // String waittime = br.getRegex("var tiempo = (\\d+);").getMatch(0);
                // int wait = 5;
                // if (waittime != null) wait = Integer.parseInt(waittime);
                // sleep(wait * 1001l, downloadLink);

                br.submitForm(iAmHuman);
            }

            if (br.containsHTML("Vídeo no existe posiblemente") || this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String vidframe = br.getRegex("\"(https?://moevideo\\.net/framevideo[^<>\"]*?)\"").getMatch(0);
            if (vidframe != null) {
                br.getPage(vidframe);
            }
            uid = br.getRegex("(?:file=|/framevideo/)([0-9a-f\\.]+)(\\&|\"|\\'|\\?)").getMatch(0);
            if (uid == null) {
                final String externID = this.br.getRegex("framevideo\\?vh=youtu\\&amp;id=([^<>\"\\'=\\&]+)").getMatch(0);
                if (externID != null) {
                    decryptedLinks.add(this.createDownloadlink("https://www.youtube.com/watch?v=" + externID));
                    return decryptedLinks;
                }
                if (!this.br.containsHTML("class=\"video\\-content video__footer\"")) {
                    decryptedLinks.add(createOfflinelink(parameter));
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

        }
        // no longer to api.letitbit.net/
        // br.postPage("http://api.letitbit.net/", "r=[\"tVL0gjqo5\",[\"preview/flv_image\",{\"uid\":\"" + uid +
        // "\"}],[\"preview/flv_link\",{\"uid\":\"" + uid + "\"}]]");

        // we should do some gets otherwise cookies and post request could turn up wrong!
        if (br.getURL() == null) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
        }
        br.postPage("/data", "r=[[\"file/flv_image\",{\"uid\":\"" + uid + "\"}],[\"file/flv_link2\",{\"uid\":\"" + uid + "\"}]]");
        String letilink = br.getRegex("\"link\":\\[\"([^\"]+)").getMatch(0);
        if (br.containsHTML("\"not_found\"") && (letilink == null || letilink.equals(""))) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (letilink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        letilink = letilink.replaceAll("\\\\", "");
        if (StringUtils.contains(letilink, "/video/404.mp4")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = null;
        DownloadLink fina;
        if (letilink.contains("moevideo.net/") || letilink.contains("videochart.net/")) {
            fina = createDownloadlink("directhttp://" + letilink);
        } else {
            filename = new Regex(letilink, "/[0-9a-f]+_\\d+_(.*?)\\.(?:mp4|flv)\\?").getMatch(0);
            if (filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            fina = createDownloadlink("http://letitbit.net/download/" + uid + "/" + filename + ".html");
            fina.setAvailable(true);
        }
        final String fsize = br.getRegex("\"convert_size\":\"(\\d+)\"").getMatch(0);
        if (fsize != null) {
            fina.setDownloadSize(Long.parseLong(fsize));
        }
        if (filename != null) {
            fina.setName(filename);
        }
        decryptedLinks.add(fina);
        return decryptedLinks;

    }
}
