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

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anime-stream24.com" }, urls = { "http://(www\\.)?(anime\\-stream24\\.(com|tv)/\\d+/\\d+/.*?|anime\\-stream24\\.info/s/[a-z0-9]+/[a-z0-9]+_\\d+)\\.html" }, flags = { 0 })
public class NmStrm24Com extends PluginForDecrypt {

    public NmStrm24Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("Seite nicht gefunden<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches("http://(www\\.)?anime-stream24\\.info/s/[a-z0-9]+/[a-z0-9]+_\\d+\\.html")) {
            // So far only tested with a vidbux.com link
            final String finallink = br.getRegex("flashvars=\\'plugins=/pl/plugins/proxy\\.swf\\&proxy\\.link=(http://[^<>\"]*?)\\&stretching=").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String fpName = br.getRegex("\\'pageName\\': \\'([^<>\"\\']+)\\'").getMatch(0);
            if (fpName == null) fpName = br.getRegex("class=\\'post\\-title entry\\-title\\'>[\t\n\r ]+<a href=\\'http://[^<>\"\\']+\\'>([^<>\"\\']+)</a>").getMatch(0);
            if (fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName.trim());
            final String[] fragments = br.getRegex("<div id=\"fragment\\-\\d+\"(.*?)<br />").getColumn(0);
            if ((fragments == null || fragments.length == 0) && !br.containsHTML(">Mirror")) {
                logger.warning("Link doesn't contain any downloadable links: " + parameter);
                return decryptedLinks;
            }
            if (fragments == null || fragments.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String fragment : fragments) {
                String dllink = findLink(fragment);
                if (dllink == null) {
                    logger.warning("Couldn't 'findLink' within selected text, possible plugin error!");
                    logger.warning("Inform JDownloader Development Team if you think that's the case.");
                } else if (dllink.equalsIgnoreCase("offline")) {
                    // we don't want to return null as this will, indicate plugin defect.
                    logger.info("A offline hoster was referenced. Continuing anyway....");
                } else {
                    final DownloadLink dl = createDownloadlink(dllink);
                    if (dl != null) decryptedLinks.add(dl);
                }
            }
            final String[] specialFragments = br.getRegex("\"(http://embed\\.publicvideohost\\.org/v\\.php\\?w=\\d+\\&h=\\d+\\&v=\\d+)\"").getColumn(0);
            if (specialFragments != null && specialFragments.length != 0) {
                for (final String specialFragment : specialFragments) {
                    br.getPage(specialFragment);
                    final String finallink = br.getRegex("file: \"(http://[^<>\"]*?)\",").getMatch(0);
                    if (finallink != null) {
                        decryptedLinks.add(createDownloadlink(finallink));
                    }
                }
            }
            if (decryptedLinks == null || decryptedLinks.size() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String findLink(final String fragment) throws IOException {
        String externID = new Regex(fragment, "\"(http://dai\\.ly/[A-Za-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            final String finallink = br.getRegex("\"stream_h264_url\":\"(http:[^<>\"\\']+)\"").getMatch(0);
            if (finallink != null)
                return ("directhttp://" + finallink.replace("\\", ""));
            else
                return null;
        }
        externID = new Regex(fragment, "/pl/mod\\.php\\?id=([a-z0-9]+)\"").getMatch(0);
        if (externID == null) externID = new Regex(fragment, "modovideo\\.com/frame\\.php\\?v=([a-z0-9]+)\\&").getMatch(0);
        if (externID != null) {
            // hoster offline
            return "offline";
        }
        externID = new Regex(fragment, "/pl/y\\.php\\?id=([a-z0-9]+)\"").getMatch(0);
        if (externID != null) { return "http://yourupload.com/file/" + externID; }
        externID = new Regex(fragment, "nowvideo\\.(eu|co)/embed\\.php\\?width=\\d+\\&height=\\d+\\&v=([a-z0-9]+)\\'").getMatch(1);
        if (externID != null) { return "http://www.nowvideo.co/video/" + externID; }
        externID = br.getRegex("dwn\\.so/player/embed\\.php\\?v=([A-Z0-9]+)\\&width=\\d+.*?").getMatch(0);
        if (externID != null) {
            br.getPage("http://dwn.so/player/embed.php?v=" + externID);
            final String yk = br.getRegex("dwn\\.so/player/play4\\.swf\\?v=" + externID + "\\&yk=([a-z0-9]+)\'").getMatch(0);
            if (yk != null) {
                br.getPage("http://dwn.so/xml/videolink.php?v=" + externID + "&yk=" + yk);
                final String finallink = br.getRegex("downloadurl=\"(http://dwn\\.so/[^<>\"]*?)\"").getMatch(0);
                if (finallink != null) return finallink;
            }
        }
        externID = new Regex(fragment, "player\\.mixturecloud\\.com/video/([A-Za-z0-9]+)\\.swf\"").getMatch(0);
        if (externID != null) { return "http://www.mixturecloud.com/media/" + externID; }
        // For videozer.com, rutube.ru and probably more
        externID = new Regex(fragment, "name=\"movie\" value=\"(http[^<>\"]*?)\"").getMatch(0);
        if (externID != null) { return externID; }
        // Redirect to other domain?
        externID = new Regex(fragment, "\"(http://(www\\.)?anime\\-stream24\\.tv/[^<>\"]*?)\" target=\"_blank\">Hier klicken um die Folge anzuschauen</a>").getMatch(0);
        if (externID != null) { return externID; }
        // Many directlinks or embed links are in here
        externID = new Regex(fragment, "flashvars=\\'file=(http://[^<>\"]*?)\\&").getMatch(0);
        if (externID != null) { return externID; }
        // Most links are in the iframes
        externID = new Regex(fragment, Pattern.compile("<iframe(.*?)</iframe>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (externID != null) { return externID; }
        return null;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}