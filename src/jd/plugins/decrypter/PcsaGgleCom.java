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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.simplejson.ParserException;
import org.codehaus.jackson.JsonProcessingException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "picasaweb.google.com" }, urls = { "https?://(www\\.)?picasaweb\\.google\\.com/(?!accounts|lh/(explore|view)).*?/.*?(\\?feat=(featured#[0-9]+|featured#)|#[0-9]+|#|\\?authkey=[A-Za-z0-9\\-]+|photo/[A-Za-z0-9\\-_]+)" }, flags = { 0 })
public class PcsaGgleCom extends PluginForDecrypt {

    public PcsaGgleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String      fpName = null;
    private String      auid   = null;
    private FilePackage fp     = FilePackage.getInstance();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("https://", "http://");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        // They sometimes have big pages
        try {
            br.setLoadLimit(4194304);
        } catch (final Throwable e) {
            // Not available in old 0.9.581 Stable
        }
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("(Hier gibt es nichts zu sehen|Entweder haben Sie keinen Zugriff auf diese Fotos oder es gibt unter dieser Adresse keine|>404 NOT_FOUND</)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        // set fp variables here
        fp.setProperty("ALLOW_MERGE", true);

        if (parameter.matches(".+(/lh/photo/[A-Za-z0-9\\-_]+|#[0-9]+)")) {
            parsePhoto(decryptedLinks, parameter, false);
        } else {
            parsePhoto(decryptedLinks, parameter, true);
        }
        if (fpName != null) {
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private void parsePhoto(ArrayList<DownloadLink> decryptedLinks, String item, boolean album) throws ParserException, JsonProcessingException, IOException {
        ArrayList<String> array = new ArrayList<String>();

        if (!album) {
            String url_uid = null;
            url_uid = new Regex(item, "/lh/photo/([A-Za-z0-9\\-_]+)").getMatch(0);
            if (url_uid == null) {
                url_uid = new Regex(item, "#(\\d+)$").getMatch(0);
            }
            auid = br.getRegex("'(\\d+)',[\r\n\t ]+\\{feedUrl").getMatch(0);
            array.add(getUIDarray(url_uid));
        } else {
            String[] test = br.getRegex("(\\$kind\".*?\\}(,\\{\"gd|\\]\\}))").getColumn(0);
            if (test != null && test.length != 0) {
                array.addAll(Arrays.asList(test));
            }
        }

        for (String heeray : array) {

            // this is in array, you would think that this isn't the largest image been within thumbnail..
            // might cause issues... /d/ link for download? also has "allowDownloads":"true" variable
            String dllink = getImage(heeray);

            String puid = getJson(heeray, "gphoto\\$id");
            // single images can belong to album!
            if (fpName == null) {
                fpName = getJson(heeray, "albumInfo\":\\[\\{\"id\":\"\\d+\",\"title");
                if (fpName == null) {
                    fpName = getJson(heeray, "title");
                }
            }
            // do this here, so we can still get album name
            if (heeray.contains("$kind\":\"photos#album\"")) continue;

            String filename = getJson(heeray, "title");

            if (dllink != null) {
                String name = puid;
                name += "-" + filename;
                DownloadLink dl = createDownloadlink("directhttp://" + dllink);
                if (!name.equals("") && !name.endsWith(dllink.substring(dllink.lastIndexOf(".")))) {
                    name = name + dllink.substring(dllink.lastIndexOf("."));
                }
                dl.setFinalFileName(name);
                if (album) dl.setAvailable(true);
                decryptedLinks.add(dl);
            } else
                logger.warning("Possible Plugin Defect, contintuing....");
        }
    }

    private String getUIDarray(String uid) {
        String array = br.getRegex("('" + uid + "',[^\\{]+\\{'preload':.+\\}\\})").getMatch(0);
        if (array == null && auid != null) {
            array = br.getRegex("(\\{\"gd\\$kind\":\"photos.*?/albumid/" + auid + "/photoid/" + uid + ".*?)").getMatch(0);
        }
        return array;
    }

    private String getJson(String source, String key) {
        String result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        if (result == null) {
            result = new Regex(source, key + ":'([^']+)").getMatch(0);
        }
        return result;
    }

    private String getImage(String source) {
        String result = new Regex(source, "(https?://\\w+\\.(googleusercontent|ggpht)\\.com/[a-zA-Z0-9_\\-/]+/d/[^\"'/]+)").getMatch(0);
        if (result == null) {
            String[] grabimage = new Regex(source, "(https?://\\w+\\.(googleusercontent|ggpht)\\.com/([a-zA-Z0-9_\\-]+/){4,5})([^\"']+)").getRow(0);
            result = grabimage[0].replaceFirst("/s\\d+[^/]+/$", "/") + "d/" + grabimage[3];
        }
        return result;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}