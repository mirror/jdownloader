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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bbc.com" }, urls = { "https?://(?:www\\.)?(bbc\\.com|bbc\\.co\\.uk)/.+" }, flags = { 0 })
public class BbcComDecrypter extends PluginForDecrypt {

    public BbcComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        /* Check if maybe we have video-IDs inside our url */
        final String[] urlids = new Regex(parameter, "([pb][a-z0-9]{7})").getColumn(0);
        if (urlids != null && urlids.length > 0) {
            for (final String vpid : urlids) {
                final DownloadLink dl = createDownloadlink("http://bbcdecrypted/" + vpid);
                dl.setAvailable(true);
                dl.setName(vpid + ".mp4");
                decryptedLinks.add(dl);
            }
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final String[] jsons = this.br.getRegex("data\\-playable=\\'(.*?)\\'>").getColumn(0);
        if (jsons == null) {
            logger.info("Failed to find any playable content");
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = null;
        for (final String json : jsons) {
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
            entries = (LinkedHashMap<String, Object>) DummyScriptEnginePlugin.walkJson(entries, "settings/playlistObject");
            String title = (String) entries.get("title");
            final String description = (String) entries.get("summary");
            entries = (LinkedHashMap<String, Object>) DummyScriptEnginePlugin.walkJson(entries, "items/{0}");
            final String vpid = (String) entries.get("vpid");
            if (inValidate(title) || inValidate(vpid)) {
                continue;
            }

            title = encodeUnicode(title);

            final DownloadLink dl = createDownloadlink("http://bbcdecrypted/" + vpid);
            dl.setLinkID(vpid);
            dl.setName(title + ".mp4");
            dl.setProperty("decrypterfilename", title);

            if (!inValidate(description)) {
                dl.setComment(description);
            }

            dl.setAvailable(true);

            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
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
    protected boolean inValidate(final String s) {
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
