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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "copy.com" }, urls = { "https?://(www\\.)?copy\\.com/[^<>\"]+" }, flags = { 0 })
public class CopyComDecrypter extends PluginForDecrypt {

    public CopyComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String  INVALIDLINKS   = "https?://(www\\.)?copy\\.com/(price|about|barracuda|bigger|install|developer|browse|home|auth|signup|policies)";
    private static final boolean ENABLE_API     = true;
    private static final String  TYPE_SUBFOLDER = "https?://(www\\.)?copy\\.com/s/[A-Za-z0-9]+/[^<>\"/]+/[^<>\"]+";

    private static final String  VALID_URL      = "https?://(www\\.)?copy\\.com/s/[A-Za-z0-9]+(/[^<>\"]+)?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = Encoding.htmlDecode(param.toString());
        final DownloadLink offline = createDownloadlink("http://copydecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        try {
            offline.setContentUrl(parameter);
        } catch (Throwable e) {

        }
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        offline.setName(new Regex(parameter, "copy\\.com/(.+)").getMatch(0));
        if (parameter.matches(INVALIDLINKS)) {
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        if (!parameter.matches(VALID_URL)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(parameter);
                if (con.isContentDisposition()) {
                    // ddlink!
                    final DownloadLink dl = createDownloadlink(br.getURL().replace("copy.com", "copydecrypted.com"));

                    if (dl.getFinalFileName() == null) {
                        dl.setFinalFileName(getFileNameFromHeader(con));
                    }
                    dl.setVerifiedFileSize(con.getLongContentLength());
                    dl.setProperty("ddlink", true);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                } else {
                    br.followConnection();
                }
            } finally {
                try {
                    /* make sure we close connection */
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            final String newparameter = br.getRedirectLocation();
            if (newparameter == null || !newparameter.matches(VALID_URL)) {
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            parameter = newparameter;
        }

        final String additionalPath = new Regex(parameter, "copy\\.com(/s/.+)").getMatch(0);
        String linktext = null;
        if (ENABLE_API) {
            br.getHeaders().put("X-Client-Version", "1.0.00");
            br.getHeaders().put("X-Client-Type", "API");
            br.getHeaders().put("X-Api-Version", "1.0");
            try {
                br.getPage("https://apiweb.copy.com/rest/meta" + additionalPath + "?offset=0&limit=2000&order=asc");
            } catch (final BrowserException e) {
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            linktext = br.getRegex("\"children\":\\[(\\{.*?\\})\\],\"").getMatch(0);
            /* Check if we have a single file */
            if (linktext == null && br.containsHTML("\"children_count\":0")) {
                linktext = br.toString();
            }
        } else {
            /* Old code, avoid using it! */
            br.getPage(parameter);
            if (br.containsHTML(">You\\&rsquo;ve found a page that doesn\\&rsquo;t exist")) {
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            if (parameter.matches(TYPE_SUBFOLDER)) {
                /* Subfolder */
                linktext = br.getRegex("previous_parent = Browser\\.Models\\.Obj\\.findOrCreate\\((\\{\"id\":\"" + additionalPath + "\".*?\\})\\],\"children_count\":\\d+").getMatch(0);
            } else {
                /* Root */
                linktext = br.getRegex("\"share\":null,\"children\":\\[(.*?)\\],\"children_count\":").getMatch(0);
            }
            /* For single links */
            if (linktext == null || linktext.equals("")) {
                linktext = br.getRegex("previous_parent = Browser\\.Models\\.Obj\\.findOrCreate\\((\\{\"id\":\"/s/.*?\\})\\],\"stub\":false,\"children_count\":1").getMatch(0);
            }

            if (linktext == null) {
                /* Maybe invalid subfolder that leads to the root --> re-add root */
                if (br.containsHTML("\"children\":\\[\\]\\},")) {
                    final String public_root = "https://www.copy.com/s/" + new Regex(parameter, "/s/([A-Za-z0-9]+)").getMatch(0) + "/Public";

                    decryptedLinks.add(createDownloadlink(public_root));
                    return decryptedLinks;
                }
            }
        }

        if (linktext == null) {
            /* Probably offline - or plugin broken */
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String[] links = linktext.split("\\},\\{");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleinfo : links) {
            final String name = getJson(singleinfo, "name");
            if (additionalPath.endsWith("/" + Encoding.urlEncode(name))) {

            }
            if (name == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (!"file".equals(getJson(singleinfo, "type"))) {
                final DownloadLink dl = createDownloadlink(parameter + "/" + name);

                decryptedLinks.add(dl);
            } else {
                final DownloadLink dl = createDownloadlink("http://copydecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filesize = getJson(singleinfo, "size");
                String url = getJson(singleinfo, "url");
                final String path = getJson(singleinfo, "path");
                if (filesize == null || url == null || path == null) {
                    logger.warning("Decrypter broken for: " + parameter);
                    return null;
                }
                url = url.replace("\\", "");
                try {
                    dl.setContentUrl(url);
                } catch (Throwable e) {

                }
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setFinalFileName(name);
                dl.setProperty("plain_name", name);
                dl.setProperty("LINKDUPEID", "copycom" + additionalPath + "_" + name);
                dl.setProperty("plain_size", filesize);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("specified_link", url);
                dl.setProperty("plain_path", path);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        return decryptedLinks;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

}
