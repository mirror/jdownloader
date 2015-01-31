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
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amazon.com" }, urls = { "https?://(www\\.)?amazon\\.(de|es|com|com\\.au|co\\.uk|fr)/(clouddrive/share/[A-Za-z0-9\\-_]+\\?md5=[A-Fa-f0-9]+\\&name=.+|clouddrive/share/[A-Za-z0-9\\-_]+|clouddrive/share[\\?\\&]s=[A-Za-z0-9\\-_]+|gp/drive/share.*?[\\?\\&]s=[A-Za-z0-9\\-_]+)" }, flags = { 0 })
public class AmazonCloudDecrypter extends PluginForDecrypt {

    public AmazonCloudDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        String plain_folder_id = new Regex(parameter, "[\\?\\&]s=([A-Za-z0-9\\-_^&]+)").getMatch(0);
        final String plain_domain = new Regex(parameter, "(amazon\\.(de|es|com|com\\.au|co\\.uk|fr))").getMatch(0);
        if (plain_folder_id == null) {

            // there are dummy ?md5=..&name=... links. see below
            plain_folder_id = new Regex(parameter, "share/([A-Za-z0-9\\-_]+)").getMatch(0);
            // linkType https://www.amazon.es/clouddrive/share/THB_Rik<...ID....>
            return handleNewType(decryptedLinks, param, plain_folder_id, plain_domain, progress);

        } else {
            // Link Type: https://www.amazon.com/clouddrive/share?s=yr-4blQrTV8hRaCZ8zOOPg
            // LinkType: http://www.amazon.com/gp/drive/share/177-7117023-0446256?ie=UTF8&s=yr-4blQrTV8hRaCZ8zOOPg

            final DownloadLink main = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            main.setProperty("type", "old20140922");
            main.setProperty("plain_folder_id", plain_folder_id);
            main.setProperty("mainlink", parameter);
            main.setProperty("plain_domain", plain_domain);
            try {
                main.setContentUrl(" https://www." + plain_domain + "/clouddrive/share?s=" + plain_folder_id);
            } catch (Throwable e) {

            }

            decryptedLinks.add(main);
        }

        return decryptedLinks;

    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private ArrayList<DownloadLink> handleNewType(ArrayList<DownloadLink> decryptedLinks, CryptedLink parameter, String plain_folder_id, String plain_domain, ProgressController progress) throws Exception {

        final DownloadLink main = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        String requestedMd5 = new Regex(parameter.getCryptedUrl(), "md5=([a-zA-z0-9]{32})").getMatch(0);
        String requestedFilename = new Regex(parameter.getCryptedUrl(), "name=(.+)").getMatch(0);
        if (requestedFilename != null) {
            requestedFilename = Encoding.urlDecode(requestedFilename, false);
        }
        main.setProperty("plain_folder_id", plain_folder_id);
        main.setProperty("mainlink", parameter);
        try {
            main.setContentUrl(" https://www." + plain_domain + "/clouddrive/share/" + plain_folder_id);
        } catch (Throwable e) {

        }

        prepBR();
        br.getPage("https://www." + plain_domain + "/drive/v1/shares/" + plain_folder_id + "?customerId=0&ContentType=JSON&asset=ALL");
        if (br.containsHTML("\"message\":\"ShareId does not exist")) {
            main.setFinalFileName(new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final LinkedHashMap<String, Object> nodeInfo = (LinkedHashMap<String, Object>) entries.get("nodeInfo");
        final LinkedHashMap<String, Object> contentProperties = (LinkedHashMap<String, Object>) nodeInfo.get("contentProperties");

        final DownloadLink dl = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        final String kind = (String) nodeInfo.get("kind");
        String filename = (String) nodeInfo.get("name");
        final String finallink = (String) nodeInfo.get("tempLink");
        final long filesize = ((Number) contentProperties.get("size")).longValue();
        final String md5 = (String) contentProperties.get("md5");

        if (!kind.equals("FILE")) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename).trim();
        final String content_url = "https://www." + plain_domain + "/clouddrive/share/" + plain_folder_id + "?md5=" + md5 + "&name=" + Encoding.urlEncode(filename);
        final String fid = filename + "_" + md5;

        dl.setDownloadSize(filesize);
        dl.setFinalFileName(filename);
        dl.setProperty("plain_name", filename);
        dl.setProperty("plain_size", filesize);
        dl.setProperty("mainlink", parameter);
        dl.setProperty("plain_directlink", finallink);
        dl.setProperty("plain_folder_id", plain_folder_id);
        dl.setProperty("plain_domain", plain_domain);
        dl.setAvailable(true);
        try {
            /*
             * the contenturl should be a valid browser url, and if we add it to jd, we should get the copied link, and not the full folder
             */
            dl.setContentUrl(content_url);
            dl.setContainerUrl("https://www." + plain_domain + "/clouddrive/share/" + plain_folder_id);
            dl.setLinkID(fid);
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
            dl.setBrowserUrl(content_url);
            dl.setProperty("LINKDUPEID", fid);
        }
        decryptedLinks.add(dl);

        /* Do not set any packagename as long as we only handle single links. */
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(plain_folder_id);
        // fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
    }

}
