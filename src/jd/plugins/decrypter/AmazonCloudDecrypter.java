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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amazon.com" }, urls = { "https?://(www\\.)?amazon\\.(de|es|com|com\\.au|co\\.uk|fr)/clouddrive/share/.+|https?://(?:www\\.)?amazon\\.com/clouddrive/share.+" }, flags = { 0 })
public class AmazonCloudDecrypter extends PluginForDecrypt {

    public AmazonCloudDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter       = null;
    private String plain_folder_id = null;
    private String plain_domain    = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();

        plain_folder_id = new Regex(parameter, "[\\?\\&]s=([A-Za-z0-9\\-_^&]+)").getMatch(0);
        plain_domain = new Regex(parameter, "(amazon\\.(de|es|com|com\\.au|co\\.uk|fr))").getMatch(0);
        if (plain_folder_id == null) {

            // there are dummy ?md5=..&name=... links. see below
            plain_folder_id = new Regex(parameter, "share/([A-Za-z0-9\\-_]+)").getMatch(0);
            // linkType https://www.amazon.es/clouddrive/share/THB_Rik<...ID....>
            return handleNewType(decryptedLinks, param, progress);

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

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    private ArrayList<DownloadLink> handleNewType(ArrayList<DownloadLink> decryptedLinks, CryptedLink parameter, ProgressController progress) throws Exception {
        LinkedHashMap<String, Object> entries = null;
        prepBR();
        ArrayList<Object> resource_data_list = null;
        final String subfolder_id = new Regex(parameter, "/folder/([^/]+)").getMatch(0);
        if (subfolder_id != null) {
            /* Subfolders-/files */
            br.setAllowedResponseCodes(400);
            br.getPage("https://www.amazon.com/drive/v1/nodes/" + subfolder_id + "/children?customerId=0&resourceVersion=V2&ContentType=JSON&limit=200&sort=%5B%22kind+DESC%22%2C+%22name+ASC%22%5D&tempLink=true&shareId=" + plain_folder_id);
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            resource_data_list = (ArrayList) entries.get("data");
        } else {
            final DownloadLink main = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            String requestedFilename = new Regex(parameter.getCryptedUrl(), "name=(.+)").getMatch(0);
            if (requestedFilename != null) {
                requestedFilename = Encoding.urlDecode(requestedFilename, false);
            }
            main.setProperty("plain_folder_id", plain_folder_id);
            main.setProperty("mainlink", parameter);
            main.setContentUrl(" https://www." + plain_domain + "/clouddrive/share/" + plain_folder_id);

            br.getPage("https://www." + this.plain_domain + "/drive/v1/shares/" + plain_folder_id + "?customerId=0&resourceVersion=V2&ContentType=JSON&asset=ALL");
            if (br.containsHTML("\"message\":\"ShareId does not exist") || this.br.getHttpConnection().getResponseCode() == 404) {
                main.setFinalFileName(new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0));
                main.setAvailable(false);
                main.setProperty("offline", true);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final LinkedHashMap<String, Object> nodeInfo = (LinkedHashMap<String, Object>) entries.get("nodeInfo");
            final String kind = (String) nodeInfo.get("kind");
            if (kind.equals("FILE")) {
                resource_data_list = new ArrayList<Object>();
                resource_data_list.add(nodeInfo);
            } else {
                final String id = (String) nodeInfo.get("id");
                br.getPage("https://www.amazon.com/drive/v1/nodes/" + id + "/children?customerId=0&resourceVersion=V2&ContentType=JSON&limit=200&sort=%5B%22kind+DESC%22%2C+%22name+ASC%22%5D&tempLink=true&shareId=" + plain_folder_id);
                entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                resource_data_list = (ArrayList) entries.get("data");
            }
        }
        for (final Object o : resource_data_list) {
            decryptedLinks.add(crawlSingleObject(o));
        }

        return decryptedLinks;
    }

    @SuppressWarnings("unchecked")
    private DownloadLink crawlSingleObject(final Object o) throws IOException {
        final LinkedHashMap<String, Object> nodeInfo = (LinkedHashMap<String, Object>) o;
        final LinkedHashMap<String, Object> contentProperties = (LinkedHashMap<String, Object>) nodeInfo.get("contentProperties");
        final String kind = (String) nodeInfo.get("kind");
        final String id = (String) nodeInfo.get("id");
        final DownloadLink dl;
        if (kind.equals("FILE")) {
            dl = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            String filename = (String) nodeInfo.get("name");
            final String finallink = (String) nodeInfo.get("tempLink");
            final long filesize = ((Number) contentProperties.get("size")).longValue();
            final String md5 = (String) contentProperties.get("md5");
            filename = Encoding.htmlDecode(filename).trim();
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
            dl.setContentUrl(parameter);
            dl.setContainerUrl("https://www." + plain_domain + "/clouddrive/share/" + plain_folder_id);
            dl.setLinkID(fid);
        } else {
            /* Add url to crawl subfolders/files */
            dl = createDownloadlink("https://www.amazon.com/clouddrive/share/" + plain_folder_id + "/folder/" + id);
        }
        return dl;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
    }

}
