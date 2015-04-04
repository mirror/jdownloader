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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sourceforge.net" }, urls = { "https?://(www\\.)?sourceforge\\.net/projects/([^/]+/files/.+/download|[^/]+/)" }, flags = { 0 })
public class SourceForgeNet extends PluginForDecrypt {

    public SourceForgeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("https://", "http://");
        // We get downloadlinks depending on our useragent
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:14.0) Gecko/20100101 Firefox/14.0.1");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final Regex info = new Regex(parameter, "projects/([^/]+)/files/([^/]+)/([^/]+)");
        final String project_name = new Regex(parameter, "projects/([^/]+)").getMatch(0);
        String project_version = info.getMatch(1);
        if (project_version == null) {
            project_version = br.getRegex("class=\"actions\"><a href=\"/projects/[/]+/rss\\?path=/([^<>\"/]*?)\"").getMatch(0);
        }
        final String target_filename = info.getMatch(2);
        if (project_version == null) {
            logger.warning("Decrypter broken, link: " + parameter);
            return null;
        }
        br.getPage("http://sourceforge.net/projects/" + project_name + "/files/" + project_version + "//list");
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        for (Entry<String, Object> entry : entries.entrySet()) {
            final LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) entries.get(entry.getKey());
            final String filename = (String) data.get("name");
            final String sha1 = (String) data.get("sha1");
            final String md5 = (String) data.get("md5");
            final String dloadlink = (String) data.get("download_url");
            if (filename == null || sha1 == null || md5 == null || dloadlink == null) {
                logger.warning("Decrypter broken, link: " + parameter);
                return null;
            }
            /* Only decrypt the version wished by the user */
            if (target_filename != null && !filename.equals(target_filename)) {
                continue;
            }
            final DownloadLink dl = createDownloadlink(dloadlink.replace("sourceforge.net/", "sourceforgedecrypted.net/"));
            try {
                dl.setContentUrl(dloadlink);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                dl.setBrowserUrl(dloadlink);
            }
            dl.setSha1Hash(sha1);
            dl.setMD5Hash(md5);
            dl.setFinalFileName(filename);
            decryptedLinks.add(dl);
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}