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
import java.util.Set;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sourceforge.net" }, urls = { "https?://(www\\.)?sourceforge\\.net/projects/[^/]+/files/[^\\?<>\"]{0,}" })
public class SourceForgeNet extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public SourceForgeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String target_filename = null;
        String list_url = null;
        String parameter = param.toString().replace("http://", "https://").replaceAll("(/stats/?(timeline)?)$", "");
        /* We get downloadlinks depending on our useragent Update 07.04.2015: Really? Do we? Who added this comment? Me? */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:14.0) Gecko/20100101 Firefox/14.0.1");
        br.setLoadLimit(br.getLoadLimit() * 5);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">This folder has no files")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        } else if (!br.getURL().contains(this.getHost() + "/")) {
            /* Redirect to external website (extremely rare case) */
            final DownloadLink dl = createDownloadlink(br.getURL());
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        DownloadLink fallBackDownloadLink = null;
        if (br.containsHTML("File may contain malware|Your download will start shortly")) {
            target_filename = br.getRegex("<div\\s*class\\s*=\\s*\"file-name\"\\s*>\\s*(.*?)\\s*</").getMatch(0);
            parameter = parameter.replaceAll("(/download)$", "");
            // TODO: optimize to avoid /stats->list_url
            br.getPage(parameter + "/stats/timeline");
            list_url = br.getRegex("<a data-type\\s*=\\s*\"file\"\\s*id\\s*=\\s*\"stat-file-picker-trigger\"\\s*href\\s*=\\s*\"(.*?)(/[^/]+/?)\"").getMatch(0);
            fallBackDownloadLink = (createDownloadlink(parameter.replace("sourceforge.net/", "sourceforgedecrypted.net/")));
            fallBackDownloadLink.setAvailable(true);
            fallBackDownloadLink.setContentUrl(parameter);
            if (target_filename == null) {
                final String url_filename = new Regex(parameter, "/([^/]+)(/?|/download)$").getMatch(0);
                if (url_filename != null) {
                    target_filename = URLEncode.decodeURIComponent(url_filename);
                }
            }
            if (target_filename != null) {
                fallBackDownloadLink.setName(target_filename);
            }
            if (list_url == null || target_filename == null) {
                decryptedLinks.add(fallBackDownloadLink);
                return decryptedLinks;
            }
        }
        /*
         * RegEx the current url as redirects might happen, especially if users add links to single files, the "/download" which is needed
         * below might be missing in the added CryptedLink (source url) but is then available inside the URL which it redirects to.
         */
        String json = br.getRegex("net\\.sf\\.files\\s*?=\\s*?(\\{.*?\\}\\});").getMatch(0);
        if (json == null && list_url == null) {
            final Regex info = new Regex(br.getURL(), "projects/([^/]+)/files/([^/]+)/([^/]+)(/download)?");
            final String project_name = new Regex(parameter, "projects/([^/]+)").getMatch(0);
            final String project_version = info.getMatch(1);
            if (target_filename == null) {
                target_filename = info.getMatch(2);
            }
            if (project_name != null && project_version != null) {
                list_url = "https://sourceforge.net/projects/" + project_name + "/files/" + project_version;
            } else {
                /*
                 * Fix that URL.
                 */
                final String toRemove = new Regex(parameter, "/(?:download|latest|stats).+").getMatch(-1);
                if (toRemove != null) {
                    list_url = parameter.replace(toRemove, "");
                } else {
                    list_url = parameter;
                }
            }
        }
        if (json == null && list_url != null) {
            br.getPage(list_url);
            json = br.getRegex("net\\.sf\\.files\\s*?=\\s*?(\\{.*?\\}\\});").getMatch(0);
        }
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String title = br.getRegex("<title>[^\r\n]*Browse\\s*\\/\\s*(.*?)\\s*(at SourceForge.net)?\\s*</title>").getMatch(0);
        FilePackage fp = null;
        if (title != null) {
            fp = FilePackage.getInstance();
            fp.setName(title);
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final Set<Entry<String, Object>> entryset = entries.entrySet();
        for (Entry<String, Object> entry : entryset) {
            final DownloadLink dl;
            final LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) entries.get(entry.getKey());
            final String filename = (String) data.get("name");
            final String sha1 = (String) data.get("sha1");
            final String md5 = (String) data.get("md5");
            String url = (String) data.get("url");
            final String dloadlink = (String) data.get("download_url");
            final boolean downloadable = ((Boolean) data.get("downloadable")).booleanValue();
            if (filename == null || url == null || sha1 == null || md5 == null || dloadlink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (target_filename != null && !filename.equals(target_filename)) {
                continue;
            }
            if (url.startsWith("/")) {
                url = "https://sourceforge.net" + url;
            }
            /*
             * Decide whether we got a folder that contains more files (=goes into the decrypter again) of we got single downloadable files
             * (goes into host plugin).
             */
            if (downloadable) {
                /* Downloadable filelink --> Goes into the host plugin */
                dl = createDownloadlink(dloadlink.replace("sourceforge.net/", "sourceforgedecrypted.net/"));
                dl.setContentUrl(url);
                /* Hashes should actually always be available! */
                if (!inValidate(sha1)) {
                    dl.setSha1Hash(sha1);
                } else if (!inValidate(md5)) {
                    dl.setMD5Hash(md5);
                }
                final String fileDetails = br.getRegex("<(tr\\s*title\\s*=\\s*\"" + Pattern.quote(filename) + "\"\\s*class\\s*=\\s*\"file.*?)\\s*</tr>").getMatch(0);
                final String fileSize = new Regex(fileDetails, "<td\\s*headers\\s*=\\s*\"files_size_h\"[^>]*>\\s*(.*?)\\s*</td>").getMatch(0);
                if (fileSize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(fileSize));
                    dl.setAvailable(true);
                } else if (target_filename == null && entryset.size() > 1) {
                    /*
                     * If the user adds a lot of links they should be available very quickly in the linkgrabber (without filesize). If he
                     * only adds a single file it should be checked so he can see the filesize.
                     */
                    dl.setAvailable(true);
                }
                dl.setFinalFileName(filename);
                if (fp != null) {
                    fp.add(dl);
                }
            } else {
                /* Folderlink - goes back into the decrypter */
                dl = createDownloadlink(url);
            }
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() == 0 && fallBackDownloadLink != null) {
            decryptedLinks.add(fallBackDownloadLink);
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
     */
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