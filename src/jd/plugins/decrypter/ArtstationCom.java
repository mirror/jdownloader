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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "artstation.com" }, urls = { "https?://(?:www\\.)?artstation\\.com/artist/[^/]+" }, flags = { 0 })
public class ArtstationCom extends PluginForDecrypt {

    public ArtstationCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String username = parameter.substring(parameter.lastIndexOf("/"));
        jd.plugins.hoster.ArtstationCom.setHeaders(this.br);
        this.br.getPage("https://www.artstation.com/users/" + username + ".json");
        LinkedHashMap<String, Object> json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        String full_name = (String) json.get("full_name");
        final short entries_per_page = 50;
        int entries_total = (int) DummyScriptEnginePlugin.toLong(json.get("projects_count"), 0);
        int offset = 0;
        int page = 1;

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(full_name);

        do {
            this.br.getPage("/users/" + username + "/projects.json?randomize=false&page=" + page);
            json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList) json.get("data");
            for (final Object resource : ressourcelist) {
                json = (LinkedHashMap<String, Object>) resource;
                final String title = (String) json.get("title");
                final String id = (String) json.get("hash_id");
                final String description = (String) json.get("description");
                if (inValidate(id)) {
                    return null;
                }
                final String url_content = "https://artstation.com/artwork/" + id;
                final DownloadLink dl = createDownloadlink(url_content);
                String filename;
                if (!inValidate(title)) {
                    filename = full_name + "_" + id + "_" + title + ".jpg";
                } else {
                    filename = full_name + "_" + id + ".jpg";
                }
                filename = encodeUnicode(filename);
                dl.setContentUrl(url_content);
                if (description != null) {
                    dl.setComment(description);
                }
                dl._setFilePackage(fp);
                dl.setLinkID(id);
                dl.setName(filename);
                dl.setProperty("full_name", full_name);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                offset++;
            }

            if (ressourcelist.size() < entries_per_page) {
                /* Fail safe */
                break;
            }

            page++;

        } while (decryptedLinks.size() < entries_total);

        return decryptedLinks;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    public static boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }
}
