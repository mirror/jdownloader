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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amazon.com" }, urls = { "https://(www\\.)?amazon\\.(de|es|com\\.au|co\\.uk|fr)/clouddrive/share/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class AmazonCloudDecrypter extends PluginForDecrypt {

    public AmazonCloudDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);
        try {
            ret.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_INTERNAL_URL);
        } catch (Throwable e) {
            // jd09
        }
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String plain_folder_id = new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0);
        final String plain_domain = new Regex(parameter, "(amazon\\.(de|es|com\\.au|co\\.uk|fr))").getMatch(0);
        final DownloadLink main = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        main.setProperty("plain_folder_id", plain_folder_id);
        main.setProperty("mainlink", parameter);

        prepBR();
        br.getPage("https://www." + plain_domain + "/drive/v1/shares/" + plain_folder_id + "?customerId=0&ContentType=JSON&asset=ALL");
        if (br.containsHTML("\"message\":\"ShareId does not exist")) {
            main.setFinalFileName(new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String linktext = br.getRegex("\"nodeInfo\":(\\{.*?)\\}$").getMatch(0);
        if (linktext == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        final String[] links = linktext.split("\\}\\},[\t\n\r ]+\\{");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleinfo : links) {
            final DownloadLink dl = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            final String filesize = getJson(singleinfo, "size");
            String filename = getJson(singleinfo, "name");
            final String finallink = getJson(singleinfo, "tempLink");
            final String md5 = getJson(singleinfo, "md5");
            if (filesize == null || filename == null || finallink == null || md5 == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            final long cursize = Long.parseLong(filesize);
            dl.setDownloadSize(cursize);
            dl.setFinalFileName(filename);
            dl.setProperty("plain_name", filename);
            dl.setProperty("plain_size", filesize);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plain_directlink", finallink);
            dl.setProperty("plain_folder_id", plain_folder_id);
            dl.setProperty("plain_domain", plain_domain);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(plain_folder_id);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
    }

}
