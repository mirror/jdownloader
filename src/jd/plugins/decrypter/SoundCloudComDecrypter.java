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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.SoundcloudCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https?://(www\\.)?(soundcloud\\.com/(?!you/|tour|signup|logout|login|premium|messages|settings|imprint|community\\-guidelines|videos|terms\\-of\\-use|sounds|jobs|press|mobile|search|upload|people|dashboard)[^<>\"\\']+(\\?format=html\\&page=\\d+|\\?page=\\d+)?|snd\\.sc/[A-Za-z09]+)" }, flags = { 0 })
public class SoundCloudComDecrypter extends PluginForDecrypt {

    public SoundCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CLIENTID = "b45b1aa10f1ac2941910a7f0d10f8e28";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("(/download|\\\\)", "").replace("www.", "").replace("http://", "https://");
        if (parameter.matches("http://(www\\.)?snd\\.sc/[A-Za-z09]+")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            final String newparameter = br.getRedirectLocation();
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                return null;
            }
            parameter = newparameter;
        }
        br.setFollowRedirects(true);
        boolean decryptList = parameter.matches(".*?soundcloud\\.com/[a-z\\-_0-9]+/(tracks|favorites)(\\?page=\\d+)?");
        if (!decryptList) {
            decryptList = !parameter.matches(".*?soundcloud\\.com/[a-z\\-_0-9]+/[a-z\\-_0-9]+(/)?");
            if (!decryptList) decryptList = parameter.contains("/groups/");
        }
        if (decryptList) {
            final String clientID = jd.plugins.hoster.SoundcloudCom.CLIENTID;
            br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(parameter) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + clientID);
            if (br.containsHTML("\"404 \\- Not Found\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final PluginForHost hostPlugin = JDUtilities.getPluginForHost("soundcloud.com");
            String fpName = null;
            // For sets ("/set/" links)
            if (parameter.contains("/sets/")) {
                fpName = (((SoundcloudCom) hostPlugin).getXML("username", br.toString())) + " - " + new Regex(parameter, "/sets/(.+)$").getMatch(0);
                final String[] items = br.getRegex("<track>(.*?)</track>").getColumn(0);
                final String usernameOfSet = new Regex(parameter, "soundcloud\\.com/(.*?)/sets/").getMatch(0);
                if (items == null || items.length == 0 || usernameOfSet == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String item : items) {
                    final String permalink = ((SoundcloudCom) hostPlugin).getXML("permalink", item);
                    if (permalink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + usernameOfSet + "/" + permalink);
                    final AvailableStatus status = ((SoundcloudCom) hostPlugin).checkStatus(dl, item);
                    dl.setAvailableStatus(status);
                    decryptedLinks.add(dl);
                }
            } else {
                // Decrypt all tracks of a user
                fpName = ((SoundcloudCom) hostPlugin).getXML("username", br.toString());
                final String userID = br.getRegex("soundcloud\\.com/users/(\\d+)</uri>").getMatch(0);
                if (userID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage("https://api.sndcdn.com/e1/users/" + userID + "/sounds?limit=10000&offset=0&linked_partitioning=1&client_id=" + clientID);
                final String[] items = br.getRegex("<stream\\-item>(.*?)</stream\\-item>").getColumn(0);
                if (items == null || items.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String item : items) {
                    final String url = ((SoundcloudCom) hostPlugin).getXML("permalink", item);
                    if (url == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted") + "/" + url);
                    final AvailableStatus status = ((SoundcloudCom) hostPlugin).checkStatus(dl, item);
                    dl.setAvailableStatus(status);
                    decryptedLinks.add(dl);
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else {
            decryptedLinks.add(createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted")));
        }
        return decryptedLinks;
    }
}
