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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.decrypter.AniLinkzCom.StringContainer;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixcloud.com" }, urls = { "http://(www\\.)?mixcloud\\.com/[A-Za-z0-9\\-]+/[A-Za-z0-9\\-_%]+/" }, flags = { 0 })
public class MxCloudCom extends PluginForDecrypt {

    public MxCloudCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String    INVALIDLINKS = "http://(www\\.)?mixcloud\\.com/((developers|categories|media|competitions|tag)/.+|[\\w\\-]+/(playlists|activity|followers|following|listens|favourites).+)";
    private static StringContainer agent        = new StringContainer();

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> tempLinks = new ArrayList<String>();
        final String parameter = param.toString();
        br.setReadTimeout(3 * 60 * 1000);
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }

        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent.string);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            logger.info("Unsupported or offline link: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("<title>404 Error page") || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Offline link: " + parameter);
            return decryptedLinks;
        }

        String theName = br.getRegex("class=\"cloudcast\\-name\" itemprop=\"name\">(.*?)</h1>").getMatch(0);
        if (theName == null) theName = br.getRegex("data-resourcelinktext=\"(.*?)\"").getMatch(0);
        if (theName == null) theName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (theName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        final String playInfo = br.getRegex("m\\-play\\-info=\"([^\"]+)\"").getMatch(0);
        if (playInfo == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String previewLink = br.getRegex("\"(http://stream\\d+\\.mixcloud\\.com/previews/[^<>\"]*?\\.mp3)\"").getMatch(0);
        if (previewLink != null) {
            final String mp3link = previewLink.replace("/previews/", "/c/originals/");
            tempLinks.add(mp3link);
        }

        String result = null;
        try {
            /*
             * CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8
             */
            result = decrypt(playInfo);
        } catch (final Throwable e) {
            return null;
        }

        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        final String[] links = new Regex(result, "\"(.*?)\"").getColumn(0);
        if (links != null && links.length != 0) {
            for (final String temp : links) {
                tempLinks.add(temp);
            }
        }
        if (tempLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final HashMap<String, Long> alreadyFound = new HashMap<String, Long>();
        for (final String dl : tempLinks) {
            if (!dl.endsWith(".mp3") && !dl.endsWith(".m4a")) {
                continue;
            }
            final DownloadLink dlink = createDownloadlink("directhttp://" + dl);
            dlink.setFinalFileName(Encoding.htmlDecode(theName).trim() + new Regex(dl, "(\\..{3}$)").getMatch(0));
            /* Nicht alle Links im Array sets[] sind verfügbar. */
            try {
                con = br.openGetConnection(dl);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    continue;
                }
                if (alreadyFound.get(dlink.getName()) != null && alreadyFound.get(dlink.getName()) == con.getLongContentLength()) {
                    continue;
                } else {
                    alreadyFound.put(dlink.getName(), con.getLongContentLength());
                    dlink.setAvailable(true);
                    dlink.setDownloadSize(con.getLongContentLength());
                    decryptedLinks.add(dlink);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(theName));
        fp.addLinks(decryptedLinks);
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private String decrypt(String e) {
        final byte[] key = JDHexUtils.getByteArray(JDHexUtils.getHexString(Encoding.Base64Decode("cGxlYXNlZG9udGRvd25sb2Fkb3VybXVzaWN0aGVhcnRpc3Rzd29udGdldHBhaWQ=")));
        final byte[] enc = jd.crypt.Base64.decode(e);
        byte[] plain = new byte[enc.length];
        int count = enc.length;
        int i = 0;
        int j = 0;
        while (i < count) {
            if (j > key.length - 1) j = 0;
            plain[i] = (byte) (0xff & (enc[i] ^ key[j]));
            i++;
            j++;
        }
        return new String(plain);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}