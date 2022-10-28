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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "embedy.cc" }, urls = { "https?://(?:www\\.)?embedy\\.cc/movies/([A-Za-z0-9=]+)" })
public class EmbedyCc extends PluginForDecrypt {
    public EmbedyCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String linkid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/movies/")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = linkid;
        }
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* Pretend that we're using their browser-extension -> Adds 1080p quality for some streams. */
        this.br.getHeaders().put("embedyExt", "1");
        /* 2020-05-29: New , 2022-04-14, doesn't work reliable */
        final boolean forceOfficialDownload = false;
        if (forceOfficialDownload) {
            Browser brc = br.cloneBrowser();
            brc.postPage("https://embedy.cc/down/", "id=" + Encoding.urlEncode(linkid));
            String[] qualities = brc.getRegex("(\\d{3,}p)").getColumn(0);
            String[] urls = brc.getRegex("(https://[^/]+/download/[^<>\"]+)\"").getColumn(0);
            if (urls.length == 0) {
                sleep(1000, param);
                brc = br.cloneBrowser();
                brc.postPage("https://embedy.cc/down/", "id=" + Encoding.urlEncode(linkid));
                qualities = brc.getRegex("(\\d{3,}p)").getColumn(0);
                urls = brc.getRegex("(https://[^/]+/download/[^<>\"]+)\"").getColumn(0);
            }
            int index = -1;
            for (final String url : urls) {
                index += 1;
                final DownloadLink dl = createDownloadlink("directhttp://" + url);
                final UrlQuery query = new UrlQuery().parse(url);
                final String fname_crypted = query.get("title");
                if (fname_crypted != null) {
                    String fname_decrypted = Encoding.Base64Decode(fname_crypted);
                    /* Remove .mp4 extension if present */
                    if (fname_decrypted.toLowerCase(Locale.ENGLISH).endsWith(".mp4")) {
                        fname_decrypted = fname_decrypted.substring(0, fname_decrypted.lastIndexOf("."));
                    }
                    /* Add quality identifier if possible */
                    if (qualities.length == urls.length) {
                        fname_decrypted += "_" + qualities[index];
                    }
                    /* Add file-extension */
                    fname_decrypted += ".mp4";
                    dl.setFinalFileName(fname_decrypted);
                }
                dl.setAvailable(true);
                /* 2021-01-14: Chunkload will break the videofiles! */
                dl.setProperty(DirectHTTP.FORCE_NOCHUNKS, true);
                /* Hoster by embedy.cc and/or vk.com[vk.me] */
                // dl.setFinalFileName(fpName + "_" + quality + ".mp4");
                // dl.setAvailable(true);
                // dl.setReferrerUrl(parameter);
                // dl.setProperty("requestType", "GET");
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            final Browser brc = br.cloneBrowser();
            brc.postPage("https://" + this.getHost() + "/video.get/", "video=" + Encoding.urlEncode(linkid));
            if (brc.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "response/1/files");
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> ipentry = it.next();
                final String quality = ipentry.getKey();
                final String url = (String) ipentry.getValue();
                if (url == null || !url.startsWith("http") || quality == null) {
                    continue;
                }
                final DownloadLink dl;
                if (url.contains(this.getHost())) {
                    dl = createDownloadlink("directhttp://" + url);
                    /* Hoster by embedy.cc and/or vk.com[vk.me] */
                    dl.setFinalFileName(fpName + "_" + quality + ".mp4");
                    dl.setAvailable(true);
                    dl.setReferrerUrl(parameter);
                    dl.setProperty("requestType", "GET");
                } else {
                    dl = createDownloadlink(url);
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
