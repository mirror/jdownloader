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

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vine.co" }, urls = { "https?://(?:www\\.)?vine\\.co/(?!v/)[^\\s]+" })
public class VineCoDecrypter extends PluginForDecrypt {
    public VineCoDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        long count_total = 0;
        long count_decrypted = 0;
        final long count_per_page = 99;
        int page_current = 1;
        LinkedHashMap<String, Object> entries = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<Object> resource_data_list = null;
        final String parameter = param.toString().replace("http://", "https://");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.containsHTML("class=\"?profile-details\"?")) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        String userid = br.getRegex("vine://tw/user/(\\d+)").getMatch(0);
        if (userid == null) {
            userid = br.getRegex("android\\-app://co\\.vine\\.android/vine/user\\-id/(\\d+)").getMatch(0);
        }
        if (userid == null) {
            return null;
        }
        /* Prepare for huge json answer */
        this.br.setLoadLimit(this.br.getLoadLimit() * 5);
        final String fpName = parameter.substring(parameter.lastIndexOf("/") + 1);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        do {
            if (this.isAbort()) {
                logger.info("Decryption process aborted by user");
                return decryptedLinks;
            }
            /* Max API return = 99 */
            this.br.getPage("https://vine.co/api/timelines/users/" + userid + "?page=" + page_current + "&anchor=00&size=" + count_per_page);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            resource_data_list = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "data/records");
            count_total = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "data/count"), -1);
            if (count_total < 1) {
                /* The user did not post any videos --> Link offline */
                decryptedLinks.add(this.createDownloadlink(parameter));
                return decryptedLinks;
            }
            if (resource_data_list == null || resource_data_list.size() == 0) {
                /* Fail safe */
                break;
            }
            for (final Object o : resource_data_list) {
                entries = (LinkedHashMap<String, Object>) o;
                final String description = (String) entries.get("description");
                final String permalinkUrl = (String) entries.get("permalinkUrl");
                if (permalinkUrl == null) {
                    return null;
                }
                if (!permalinkUrl.matches("https?://(www\\.)?vine\\.co/v/[A-Za-z0-9]+")) {
                    /* Skip invalid urls */
                    continue;
                }
                final String fid = permalinkUrl.substring(permalinkUrl.lastIndexOf("/") + 1);
                final DownloadLink dl = this.createDownloadlink(permalinkUrl);
                dl.setContentUrl(permalinkUrl);
                if (description != null) {
                    dl.setComment(description);
                }
                String fname;
                if (description != null && !"".equals(description)) {
                    fname = encodeUnicode(description);
                } else {
                    fname = fid;
                }
                fname += ".mp4";
                dl.setName(fname);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                dl.setLinkID(getHost() + "://" + fid);
                distribute(dl);
                decryptedLinks.add(dl);
                count_decrypted++;
            }
            page_current++;
        } while (count_decrypted < count_total);
        return decryptedLinks;
    }
}
