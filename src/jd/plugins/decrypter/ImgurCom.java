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
import jd.http.Browser.BrowserException;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgur.com" }, urls = { "https?://((www|i)\\.)?imgur\\.com(/gallery|/a|/download)?/[A-Za-z0-9]{5,}" }, flags = { 0 })
public class ImgurCom extends PluginForDecrypt {

    public ImgurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String  TYPE_GALLERY = "https?://((www|i)\\.)?imgur\\.com/a/[A-Za-z0-9]{5,}";
    private static Object ctrlLock     = new Object();

    /* IMPORTANT: Make sure that we're always using the current version of their API: https://api.imgur.com/ */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("https://", "http://").replace("/all$", "");
        synchronized (ctrlLock) {
            br.getHeaders().put("Authorization", jd.plugins.hoster.ImgUrCom.getAuthorization());
            final String lid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
            if (parameter.matches(TYPE_GALLERY)) {
                try {
                    br.getPage("https://api.imgur.com/3/album/" + lid);
                } catch (final BrowserException e) {
                    if (br.getHttpConnection().getResponseCode() == 429) {
                        logger.info("API limit reached, cannot decrypt link: " + parameter);
                        return decryptedLinks;
                    }
                    throw e;
                }
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                if (br.containsHTML("\"status\":404")) {
                    final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                    offline.setAvailable(false);
                    offline.setProperty("offline", true);
                    decryptedLinks.add(offline);
                    return decryptedLinks;
                }
                final int imgcount = Integer.parseInt(getJson(br.toString(), "images_count"));
                if (imgcount == 0) {
                    final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                    offline.setAvailable(false);
                    offline.setProperty("offline", true);
                    decryptedLinks.add(offline);
                    return decryptedLinks;
                }
                String fpName = getJson(br.toString(), "title");
                if (fpName == null || fpName.equals("null")) {
                    fpName = "imgur.com gallery " + lid;
                }
                /*
                 * using links (i.imgur.com/imgUID(s)?.extension) seems to be problematic, it can contain 's' (imgUID + s + .extension), but
                 * not always! imgUid.endswith("s") is also a valid uid, so you can't strip them!
                 */
                final String jsonarray = br.getRegex("\"images\":\\[(\\{.*?\\})\\]").getMatch(0);
                String[] items = jsonarray.split("\\},\\{");
                /* We assume that the API is always working fine */
                if (items == null || items.length == 0) {
                    logger.info("Empty album: " + parameter);
                    return decryptedLinks;
                }
                for (final String item : items) {
                    final String directlink = getJson(item, "link");
                    final String title = getJson(item, "title");
                    final String filesize = getJson(item, "size");
                    final String imgUID = getJson(item, "id");
                    if (imgUID == null || filesize == null || directlink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    String filetype = new Regex(item, "\"type\":\"image/([^<>\"]*?)\"").getMatch(0);
                    if (filetype == null) {
                        filetype = "jpeg";
                    }
                    String filename;
                    if (title != null) {
                        filename = title + "." + filetype;
                    } else {
                        filename = imgUID + "." + filetype;
                    }
                    final DownloadLink dl = createDownloadlink("http://imgurdecrypted.com/download/" + imgUID);
                    dl.setFinalFileName(filename);
                    dl.setDownloadSize(Long.parseLong(filesize));
                    dl.setAvailable(true);
                    dl.setProperty("imgUID", imgUID);
                    dl.setProperty("filetype", filetype);
                    dl.setProperty("decryptedfinalfilename", filename);
                    dl.setProperty("directlink", directlink);
                    /* No need to hide directlinks */
                    dl.setBrowserUrl("http://imgur.com/download/" + imgUID);
                    decryptedLinks.add(dl);
                }

                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            } else {
                final DownloadLink dl = createDownloadlink("http://imgurdecrypted.com/download/" + lid);
                dl.setProperty("imgUID", lid);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

}
