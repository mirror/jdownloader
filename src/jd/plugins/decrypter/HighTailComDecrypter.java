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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hightail.com" }, urls = { "https?://(?:www\\.)?(?:yousendit|hightail)\\.com/download/[A-Za-z0-9\\-_]+|https?://[a-z]+\\.hightail\\.com/[A-Za-z]+\\?phi_action=app/orchestrate[A-Za-z]+\\&[A-Za-z0-9\\-_\\&=]+" })
public class HighTailComDecrypter extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public HighTailComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_OLD = "https?://(?:www\\.)?(?:yousendit|hightail)\\.com/download/[A-Za-z0-9\\-_]+";

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        String folderID = new Regex(parameter, "\\&folderid=([A-Za-z0-9\\-_]+)").getMatch(0);
        try {
            br.getPage(parameter);
        } catch (final UnknownHostException e) {
            if (parameter.matches(TYPE_OLD)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            throw e;
        }
        if (br.containsHTML(">Access to this file has been blocked")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (folderID == null) {
            folderID = br.getRegex("NYSI\\.WS\\.currentFolderId = \\'([^<>\"\\']*?)\\';").getMatch(0);
        }
        final String fid = new Regex(this.br.getURL(), "(?:\\&|%26)(?:id|batch_id)(?:=|%3D)([A-Za-z0-9\\-_]+)").getMatch(0);
        if (fid == null) {
            return null;
        }
        if (folderID != null) {
            /* New system */
            this.br.postPage("https://de.hightail.com/folders", "phi_action=app%2FgetFolderContent&fId=" + folderID + "&encInviteId=" + fid);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("wsItems");
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                entries = (LinkedHashMap<String, Object>) entry.getValue();
                final String folderID_single = (String) entries.get("fId");
                if (folderID_single == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final boolean isFile = ((Boolean) entries.get("isFile")).booleanValue();
                if (isFile) {
                    final DownloadLink dl = createDownloadlink("http://yousenditdecrypted.com/download/" + System.currentTimeMillis() + new Random().nextInt(100000));
                    final String filename = (String) entries.get("text");
                    final long filesize = JavaScriptEngineFactory.toLong(entries.get("sizeInBytes"), -1);
                    if (filename == null || filesize == -1) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    dl.setFinalFileName(filename);
                    dl.setDownloadSize(filesize);
                    dl.setLinkID(folderID_single);
                    dl.setProperty("directname", Encoding.htmlDecode(filename.trim()));
                    dl.setProperty("directsize", filesize);
                    dl.setProperty("fileurl_new", folderID_single);
                    dl.setProperty("mainlink", parameter);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                } else {
                    final String folderlink_new = "https://de.hightail.com/sharedFolder?phi_action=app/orchestrateSharedFolder&id=" + fid + "&folderid=" + folderID_single;
                    final DownloadLink dl = createDownloadlink(folderlink_new);
                    decryptedLinks.add(dl);
                }
            }
        } else {
            /* Old system */
            final String[] linkInfo = br.getRegex("<div class=\"fileContainer list\"(.*?)<div class=\"downloadFiletype list\"").getColumn(0);
            if (linkInfo != null && linkInfo.length != 0) {
                // Multiple links
                for (final String singleLink : linkInfo) {
                    final DownloadLink dl = createDownloadlink("http://yousenditdecrypted.com/download/" + System.currentTimeMillis() + new Random().nextInt(100000));
                    final String filename = new Regex(singleLink, "class=\"downloadFilename list\"><span[^<>]*?>([^<>\"]*?)</span>").getMatch(0);
                    final String filesize = new Regex(singleLink, "class=\"downloadFilesize list\">([^<>\"]*?)</div>").getMatch(0);
                    final String fileurl = new Regex(singleLink, "file_url=\"([A-Za-z0-9]+)\"").getMatch(0);
                    if (filename == null || filesize == null || fileurl == null) {
                        logger.info("filename: " + filename + " filesize: " + filesize + " fileurl: " + fileurl);
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    dl.setName(Encoding.htmlDecode(filename.trim()));
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    dl.setContentUrl(parameter);
                    dl.setProperty("directname", Encoding.htmlDecode(filename.trim()));
                    dl.setProperty("directsize", filesize);
                    dl.setProperty("fileurl", fileurl);
                    dl.setProperty("mainlink", parameter);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            } else {
                // Single link
                if (br.containsHTML("Bitte loggen Sie sich ein,")) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                String download_id = PluginJSonUtils.getJsonValue(br, "file_download_link");
                if (download_id == null) {
                    download_id = fid;
                }
                final DownloadLink dl = createDownloadlink("http://yousenditdecrypted.com/download/" + System.currentTimeMillis() + new Random().nextInt(100000));
                dl.setLinkID(download_id);
                dl.setProperty("mainlink", parameter);
                if (br.containsHTML("Download link is invalid|Download link is invalid|>Access has expired<|class=\"fileIcons disabledFile\"")) {
                    dl.setProperty("offline", true);
                    dl.setAvailable(false);
                } else {
                    final String filename = br.getRegex("id=\"downloadSingleFilename\">([^<>\"]*?)</span>").getMatch(0);
                    final String filesize = br.getRegex("id=\"downloadSingleFilesize\">([^<>\"]*?)<span>").getMatch(0);
                    if (filename != null && filesize != null) {
                        dl.setName(Encoding.htmlDecode(filename.trim()));
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                        dl.setProperty("directname", Encoding.htmlDecode(filename.trim()));
                        dl.setProperty("directsize", filesize);
                        dl.setProperty("fileurl", download_id);
                        dl.setProperty("mainlink", parameter);
                        dl.setAvailable(true);
                    }
                }
                decryptedLinks.add(dl);

            }
        }

        return decryptedLinks;
    }

}
