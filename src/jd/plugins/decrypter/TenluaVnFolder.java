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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tenlua.vn" }, urls = { "https?://(www\\.)?tenlua\\.vn/[^<>\"]+" }, flags = { 0 })
public class TenluaVnFolder extends antiDDoSForDecrypt {

    public TenluaVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private long req_num = 0;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        final String fid = new Regex(parameter, "(?:(?:#|/)download|/folder)/?([a-z0-9]+)").getMatch(0);
        if (fid == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        postPageRaw("http://api2.tenlua.vn/", "[{\"a\":\"filemanager_builddownload_getinfo\",\"n\":\"" + fid + "\",\"r\":0." + System.currentTimeMillis() + "}]");
        final String type = PluginJSonUtils.getJsonValue(br, "type");
        if ("none".equals(type)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        } else if ("folder".equals(type)) {
            final String fpName = PluginJSonUtils.getJsonValue(br, "folder_name");

            /* Check for empty folder */
            if ("0".equals(PluginJSonUtils.getJsonValue(br, "totalfile"))) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }

            final String jsonArray = PluginJSonUtils.getJsonArray(br.toString(), "content");
            final String[] links = PluginJSonUtils.getJsonResultsFromArray(jsonArray);

            for (final String singleinfo : links) {
                String name = PluginJSonUtils.getJsonValue(singleinfo, "name");
                if (name == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                name = Encoding.htmlDecode(name.trim());
                final DownloadLink dl = createDownloadlink("http://tenluadecrypted.vn/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filesize = PluginJSonUtils.getJsonValue(singleinfo, "real_size");
                final String url = PluginJSonUtils.getJsonValue(singleinfo, "link");
                if (filesize == null || url == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setFinalFileName(name);
                dl.setProperty("plain_name", name);
                // dl.setProperty("LINKDUPEID", "tenluavn" + fid + "_" + name);
                dl.setProperty("plain_size", filesize);
                dl.setProperty("mainlink", parameter);
                dl.setContentUrl(url);
                dl.setProperty("specified_link", url);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        } else {
            String name = PluginJSonUtils.getJsonValue(br, "n");
            final String filesize = PluginJSonUtils.getJsonValue(br, "real_size");
            /* Mainlink = single link */
            final String url = parameter;
            if (filesize == null || url == null || name == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("http://tenluadecrypted.vn/" + System.currentTimeMillis() + new Random().nextInt(100000));
            name = Encoding.htmlDecode(name.trim());
            dl.setDownloadSize(Long.parseLong(filesize));
            dl.setFinalFileName(name);
            dl.setProperty("plain_name", name);
            dl.setProperty("LINKDUPEID", "tenluavn" + fid + "_" + name);
            dl.setProperty("plain_size", filesize);
            dl.setProperty("mainlink", parameter);
            dl.setContentUrl(url);
            dl.setProperty("specified_link", url);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

    @Override
    protected void postPageRaw(final String url, final String postData) throws IOException {
        if (req_num == 0) {
            req_num = (long) Math.ceil(Math.random() * 1000000000);
        } else {
            req_num++;
        }
        br.postPageRaw(url + req_num, postData);
    }

}
