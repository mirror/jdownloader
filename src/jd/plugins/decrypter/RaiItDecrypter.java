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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.decrypter.GenericM3u8Decrypter.HlsContainer;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rai.tv" }, urls = { "https?://[A-Za-z0-9\\.]*?rai\\.tv/dl/replaytv/replaytv\\.html\\?day=\\d{4}\\-\\d{2}\\-\\d{2}(?:\\&ch=\\d+)?" }, flags = { 0 })
public class RaiItDecrypter extends PluginForDecrypt {

    public RaiItDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.br.setFollowRedirects(true);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String date = new Regex(parameter, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        String chnumber_str = new Regex(parameter, "ch=(\\d+)").getMatch(0);
        if (chnumber_str == null) {
            /* Small fallback */
            chnumber_str = "1";
        }
        final String date_underscore = date.replace("-", "_");
        LinkedHashMap<String, Object> tempmap = null;
        LinkedHashMap<String, Object> entries = null;
        ArrayList<Object> ressourcelist = null;
        /* Find the name of our channel */
        this.br.getPage("http://www.rai.tv/dl/RaiTV/iphone/android/smartphone/advertising_config.html");
        String channel_name = null;
        try {
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            ressourcelist = (ArrayList<Object>) entries.get("Channels");
            for (final Object channelo : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) channelo;
                final String channelnumber = (String) entries.get("id");
                if (channelnumber.equals(chnumber_str)) {
                    channel_name = (String) entries.get("tag");
                    break;
                }
            }
        } catch (final Throwable e) {
        }
        if (channel_name == null || channel_name.equals("")) {
            channel_name = "RaiUno";
        }

        this.br.getPage("/dl/portale/html/palinsesti/replaytv/static/" + channel_name + "_" + date_underscore + ".html?_=" + System.currentTimeMillis());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get(chnumber_str);
        entries = (LinkedHashMap<String, Object>) entries.get(date);

        final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            final Entry<String, Object> entry = it.next();
            tempmap = (LinkedHashMap<String, Object>) entry.getValue();
            final String title = (String) tempmap.get("t");
            final String relinker = (String) tempmap.get("r");
            final String description = (String) tempmap.get("d");
            if (title == null || title.equals("") || relinker == null || relinker.equals("")) {
                continue;
            }
            final String cont = jd.plugins.hoster.RaiTv.getContFromRelinkerUrl(relinker);
            jd.plugins.hoster.RaiTv.accessCont(this.br, cont);
            final String dllink = jd.plugins.hoster.RaiTv.getDllink(this.br);
            if (!jd.plugins.hoster.RaiTv.dllinkIsDownloadable(dllink)) {
                continue;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(date + " - " + title);
            if (dllink.contains(".m3u8")) {
                this.br.getPage(dllink);
                final ArrayList<HlsContainer> allqualities = jd.plugins.decrypter.GenericM3u8Decrypter.getHlsQualities(this.br);
                for (final HlsContainer singleHlsQuality : allqualities) {
                    final DownloadLink dl = this.createDownloadlink(singleHlsQuality.downloadurl);
                    final String filename = title + " - " + description + "_" + singleHlsQuality.getStandardFilename();
                    dl.setFinalFileName(filename);
                    dl._setFilePackage(fp);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    decryptedLinks.add(dl);
                }
            } else {
                final DownloadLink dl = this.createDownloadlink("directhttp://" + dllink);
                dl.setFinalFileName(title + " - " + description + ".mp4");
                dl._setFilePackage(fp);
                if (description != null) {
                    dl.setComment(description);
                }
                decryptedLinks.add(dl);
            }
        }

        return decryptedLinks;
    }

}
