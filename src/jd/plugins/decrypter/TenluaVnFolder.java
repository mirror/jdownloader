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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tenlua.vn" }, urls = { "http://(www\\.)?tenlua\\.vn/[^<>\"]+" }, flags = { 0 })
public class TenluaVnFolder extends PluginForDecrypt {

    public TenluaVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private long    req_num      = 0;
    private boolean pluginloaded = false;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String fid = new Regex(parameter, "#download([a-z0-9]+)").getMatch(0);
        if (fid == null) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        postPageRaw("http://api.tenlua.vn/?id=", "[{\"a\":\"filemanager_builddownload_getinfo\",\"n\":\"" + fid + "\",\"r\":0." + System.currentTimeMillis() + "}]");
        final String type = getJson("type", br.toString());
        if ("none".equals(type)) {
            final DownloadLink dl = createDownloadlink("directhttp://" + parameter);
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if ("folder".equals(type)) {
            final String fpName = getJson("folder_name", br.toString());
            final String jsonArray = br.getRegex("\"content\":\\[(.*?)\\]\\}").getMatch(0);
            final String[] links = jsonArray.split("\\},\\{");

            for (final String singleinfo : links) {
                String name = getJson("name", singleinfo);
                if (name == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                name = unescape(Encoding.htmlDecode(name.trim()));
                final DownloadLink dl = createDownloadlink("http://tenluadecrypted.vn/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filesize = getJson("real_size", singleinfo);
                String url = getJson("link", singleinfo);
                if (filesize == null || url == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                url = url.replace("\\", "");
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setFinalFileName(name);
                dl.setProperty("plain_name", name);
                // dl.setProperty("LINKDUPEID", "tenluavn" + fid + "_" + name);
                dl.setProperty("plain_size", filesize);
                dl.setProperty("mainlink", parameter);
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
            String name = getJson("n", br.toString());
            if (name == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            name = unescape(Encoding.htmlDecode(name.trim()));
            final DownloadLink dl = createDownloadlink("http://tenluadecrypted.vn/" + System.currentTimeMillis() + new Random().nextInt(100000));
            final String filesize = getJson("real_size", br.toString());
            String url = getJson("link", br.toString());
            if (filesize == null || url == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            url = url.replace("\\", "");
            dl.setDownloadSize(Long.parseLong(filesize));
            dl.setFinalFileName(name);
            dl.setProperty("plain_name", name);
            dl.setProperty("LINKDUPEID", "tenluavn" + fid + "_" + name);
            dl.setProperty("plain_size", filesize);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("specified_link", url);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

    private void postPageRaw(final String url, final String postData) throws IOException {
        if (req_num == 0) {
            req_num = (long) Math.ceil(Math.random() * 1000000000);
        } else {
            req_num++;
        }
        br.postPageRaw(url + req_num, postData);
    }

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) {
                throw new IllegalStateException("youtube plugin not found!");
            }
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

}
