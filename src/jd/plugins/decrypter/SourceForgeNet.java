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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sourceforge.net" }, urls = { "https?://(www\\.)?sourceforge\\.net/projects/[^/]+/files/[^\\?<>\"]{0,}" }, flags = { 0 })
public class SourceForgeNet extends PluginForDecrypt {

    public SourceForgeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String target_filename = null;
        final String list_url;
        String parameter = param.toString().replace("https://", "http://");
        // We get downloadlinks depending on our useragent
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:14.0) Gecko/20100101 Firefox/14.0.1");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final Regex info = new Regex(parameter, "projects/([^/]+)/files/([^/]+)/([^/]+)/download");
        final String project_name = new Regex(parameter, "projects/([^/]+)").getMatch(0);
        final String project_version = info.getMatch(1);
        target_filename = info.getMatch(2);
        if (project_name != null && project_version != null) {
            list_url = "http://sourceforge.net/projects/" + project_name + "/files/" + project_version + "//list";
        } else {
            list_url = parameter + "/list";
        }
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(list_url);
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        for (Entry<String, Object> entry : entries.entrySet()) {
            final DownloadLink dl;
            final LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) entries.get(entry.getKey());
            final String filename = (String) data.get("name");
            final String sha1 = (String) data.get("sha1");
            final String md5 = (String) data.get("md5");
            String url = (String) data.get("url");
            final String dloadlink = (String) data.get("download_url");
            final boolean downloadable = ((Boolean) data.get("downloadable")).booleanValue();
            if (filename == null || url == null || sha1 == null || md5 == null || dloadlink == null) {
                logger.warning("Decrypter broken, link: " + parameter);
                return null;
            }
            if (url.startsWith("/")) {
                url = "http://sourceforge.net" + url;
            }
            /* Only decrypt the version wished by the user */
            if (target_filename != null && !filename.equals(target_filename)) {
                continue;
            }
            if (downloadable) {
                /* Downloadable filelink --> Goes into the host plugin */
                dl = createDownloadlink(dloadlink.replace("sourceforge.net/", "sourceforgedecrypted.net/"));
                try {
                    dl.setContentUrl(url);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                    dl.setBrowserUrl(url);
                }
                if (!inValidate(sha1)) {
                    dl.setSha1Hash(sha1);
                }

                if (!inValidate(md5)) {
                    dl.setMD5Hash(md5);
                }
                dl.setFinalFileName(filename);
            } else {
                /* Folderlink - goes back into the decrypter */
                dl = createDownloadlink(url);
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}