//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.net.UnknownHostException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imageshack.us" }, urls = { "https?://(www\\.)?(img[0-9]{1,4}\\.imageshack\\.us/(g/|my\\.php\\?image=[a-z0-9]+|i/[a-z0-9]+)\\.[a-zA-Z0-9]{2,4}|imageshack\\.us/photo/[^<>\"\\'/]+/\\d+/[^<>\"\\'/]+|imageshack\\.(com|us)/user/[A-Za-z0-9\\-_]+)" }, flags = { 0 })
public class ImagesHackUs extends PluginForDecrypt {

    public ImagesHackUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_PHOTO = ".*?imageshack\\.(us|com)/photo/.+";
    private static final String TYPE_USER  = "https?://(www\\.)?imageshack\\.(com|us)/user/[A-Za-z0-9\\-_]+";

    @SuppressWarnings("unused")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString().replace("imageshack.us", "imageshack.com").replace("http://", "https://");
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            logger.info("Link offline (server error): " + parameter);
            return decryptedLinks;
        } catch (final UnknownHostException e) {
            logger.info("Link offline (server error): " + parameter);
            return decryptedLinks;
        }
        if (br.getURL().equals("http://imageshack.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getURL() != parameter) parameter = br.getURL();
        if (parameter.matches(TYPE_PHOTO)) {
            if (br.containsHTML("Looks like the image is no longer here")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String finallink = br.getRegex("<meta property=\"og:image\" content=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<link rel=\"image_src\" href=\"(http://[^<>\"\\']+)\"").getMatch(0);
                if (finallink == null) {
                    // video link (eg .mp4)
                    finallink = br.getRegex("file=(https?://[^&]+)").getMatch(0);
                    if (finallink != null) {
                        decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                    } else {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                }
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.matches(TYPE_USER)) {
            final String username = new Regex(parameter, "imageshack\\.com/user/(.+)").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(username);
            int addedlinks = 0;
            int counter = 1;
            int offset = 0;
            final int imagesPerOffset = 50;
            final boolean useAltHandling = false;
            do {
                String jsarray = null;
                if (counter == 1 && useAltHandling) {
                    jsarray = new Regex(br.toString().replace("\\", ""), "photos: \"\\[\\{(.*?)\\}\\]\"").getMatch(0);
                } else {
                    if (useAltHandling) {
                        br.getPage("https://imageshack.com/rest_api/v2/images?username=" + username + "&limit=" + imagesPerOffset + "&offset=" + offset + "&hide_empty=true&ts=" + System.currentTimeMillis());
                    } else {
                        br.getPage("https://imageshack.com/rest_api/v2/images?username=" + username + "&limit=10000&offset=0&hide_empty=true&ts=" + System.currentTimeMillis());
                    }
                    jsarray = br.toString().replace("\\", "");
                }
                final String[][] infos = new Regex(jsarray, "\"id\":\"([A-Za-z0-9]+)\",\"server\":\\d+,\"bucket\":\\d+,\"filename\":\"([^<>\"]*?)\",\"original_filename\":\"([^<>\"]*?)\"").getMatches();
                for (final String singleinfo[] : infos) {
                    final DownloadLink dl = createDownloadlink("https://imageshack.com/i/" + singleinfo[0]);
                    dl.setName(singleinfo[1]);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    addedlinks++;
                }
                counter++;
                offset += imagesPerOffset;
            } while (addedlinks == imagesPerOffset && useAltHandling);
            fp.addLinks(decryptedLinks);
        } else {
            /* Error handling */
            if (br.containsHTML(">Can not find album")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String fpName = br.getRegex("<div style=\"float:left\">(.*?)</div>").getMatch(0);
            if (fpName == null || fpName.trim().equals("My Album")) fpName = new Regex(parameter, "img(\\d+)\\.imageshack\\.us").getMatch(0);
            String allPics[] = br.getRegex("<div onclick=\"window\\.location\\.href=\\'(http://.*?)\\'\"").getColumn(0);
            if (allPics == null || allPics.length == 0) allPics = br.getRegex("<input type=\"text\" value=\"(http://.*?)\"").getColumn(0);
            if (allPics == null || allPics.length == 0) allPics = br.getRegex("'\\[URL=(http://.*?)\\]").getColumn(0);
            if (allPics == null || allPics.length == 0) return null;
            for (String aPic : allPics)
                decryptedLinks.add(createDownloadlink(aPic));
            // The String "fpName" should never be null at this point
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