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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "icloud.com" }, urls = { "https?://(?:www\\.)?icloud\\.com/sharedalbum/[A-Za-z\\-]+/#[A-Za-z0-9]+" }) 
public class IcloudCom extends PluginForDecrypt {

    public IcloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* on = will try to grab final 'nice' filenames right away, off = will leave out this step! */
    private static final boolean extendedMode = true;

    /**
     * Crawls photos & videos from public iCloud URLs. It is possible to really speed this up and directly show final filenames via the
     * "/webassets" request, see host plugin.
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        LinkedHashMap<String, DownloadLink> hashmap_checksum = new LinkedHashMap<String, DownloadLink>();
        String jsonphotoGuids = "{\"photoGuids\":[";
        String jsonderivatives = "\"derivatives\":{";
        final String jsonAll;

        final String parameter = param.toString();
        final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        /* Not necessarily needed! */
        br.getHeaders().put("Referer", "https://www.icloud.com/sharedalbum/");
        br.postPageRaw("https://p41-sharedstreams.icloud.com/" + folderid + "/sharedstreams/webstream", "{\"streamCtag\":null}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        LinkedHashMap<String, Object> entries_tmp = null;
        ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("photos");
        final String userFirstName = (String) entries.get("userFirstName");
        final String userLastName = (String) entries.get("userLastName");
        final String streamName = (String) entries.get("streamName");
        if (userFirstName == null || userLastName == null || streamName == null) {
            return null;
        }
        final String contributorFullName = userFirstName + " " + userLastName;
        String fpName = contributorFullName + " - " + streamName;
        int counter = 0;

        for (final Object photoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) photoo;
            final String photoGuid = (String) entries.get("photoGuid");
            final String batchGuid = (String) entries.get("batchGuid");
            final String mediaAssetType = (String) entries.get("mediaAssetType");
            final String ext_temp;
            entries = (LinkedHashMap<String, Object>) entries.get("derivatives");
            if (photoGuid == null || batchGuid == null || entries == null) {
                continue;
            }
            String checksum = null;
            /* Get best quality picture/video */
            if ("video".equalsIgnoreCase(mediaAssetType)) {
                ext_temp = jd.plugins.hoster.IcloudCom.default_ExtensionVideo;
            } else {
                ext_temp = jd.plugins.hoster.IcloudCom.default_ExtensionImage;
            }
            /* Find highest quality photo/video */
            long filesizeMax = 0;
            long filesizeTemp;
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            Entry<String, Object> entrytemp = null;
            while (it.hasNext()) {
                entrytemp = it.next();
                entries_tmp = (LinkedHashMap<String, Object>) entrytemp.getValue();
                filesizeTemp = JavaScriptEngineFactory.toLong(entries_tmp.get("fileSize"), 0);
                if (filesizeTemp > filesizeMax) {
                    filesizeMax = filesizeTemp;
                    checksum = (String) entries_tmp.get("checksum");
                }
            }

            if (checksum == null) {
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("http://iclouddecrypted.com/" + photoGuid + "_" + checksum);
            if (filesizeMax > 0) {
                dl.setDownloadSize(filesizeMax);
            }
            dl.setName(photoGuid + "_" + checksum + ext_temp);
            dl.setAvailable(true);
            dl.setProperty("folderid", folderid);
            if (mediaAssetType != null) {
                dl.setProperty("type", mediaAssetType);
            }
            /* Build our POST-json for later. */
            if (counter > 0) {
                jsonphotoGuids += ",";
                jsonderivatives += ",";
            }
            jsonphotoGuids += "\"" + photoGuid + "\"";
            jsonderivatives += String.format("\"%s\":[\"%s\"]", photoGuid, checksum);

            if (!extendedMode) {
                /* Only add links to List here, if we're not in extended mode!! */
                decryptedLinks.add(dl);
            }
            hashmap_checksum.put(checksum, dl);
            counter++;
        }

        jsonphotoGuids += "],";
        jsonderivatives += "}}";
        jsonAll = jsonphotoGuids + jsonderivatives;

        if (extendedMode) {
            /* Try to find final 'nice' filenames right away! */
            this.br.postPageRaw("/" + folderid + "/sharedstreams/webasseturls", jsonAll);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("items");

            DownloadLink dl = null;
            final Iterator<Entry<String, DownloadLink>> it = hashmap_checksum.entrySet().iterator();
            Entry<String, DownloadLink> entrytemp = null;
            String checksum_tmp = null;
            String finallink_tmp = null;
            String final_filename_tmp = null;
            /* Iterate through our DownloadLinks and find the good filenames! */
            while (it.hasNext()) {
                entrytemp = it.next();
                checksum_tmp = entrytemp.getKey();
                dl = entrytemp.getValue();
                entries_tmp = (LinkedHashMap<String, Object>) entries.get(checksum_tmp);
                /* Usually 'entries_tmp' should be != null, containing the filename information we want! */
                if (entries_tmp != null) {
                    finallink_tmp = jd.plugins.hoster.IcloudCom.getDirectlink(entries_tmp);
                    final_filename_tmp = jd.plugins.hoster.IcloudCom.getFilenameFromDirectlink(finallink_tmp);
                    if (finallink_tmp != null) {
                        dl.setProperty("directlink", finallink_tmp);
                    }
                    if (final_filename_tmp != null) {
                        dl.setFinalFileName(final_filename_tmp);
                    }
                }
                decryptedLinks.add(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
