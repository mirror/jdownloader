//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DropboxCom.DropboxConfig;

import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://(?:www\\.)?dropbox\\.com/((sh|sc|s)/[^<>\"]+|l/[A-Za-z0-9]+)|https?://(www\\.)?db\\.tt/[A-Za-z0-9]+" })
public class DropBoxCom extends PluginForDecrypt {

    private boolean     pluginloaded;
    private FilePackage currentPackage;

    public DropBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_NORMAL     = "https?://(www\\.)?dropbox\\.com/(sh|sc)/.+";
    private static final String TYPE_S          = "https?://(www\\.)?dropbox\\.com/s/.+";
    private static final String TYPE_REDIRECT   = "https?://(www\\.)?dropbox\\.com/l/[A-Za-z0-9]+";
    private static final String TYPE_SHORT      = "https://(www\\.)?db\\.tt/[A-Za-z0-9]+";

    /* Unsupported linktypes which can occur during the decrypt process */
    private static final String TYPE_DIRECTLINK = "https?://dl\\.dropboxusercontent.com/.+";
    private static final String TYPE_REFERRAL   = "https?://(www\\.)?dropbox\\.com/referrals/.+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("?dl=1", "");
        if (parameter.matches(TYPE_S)) {
            decryptedLinks.add(createSingleDownloadLink(parameter));
            return decryptedLinks;
        }

        br.setFollowRedirects(false);
        br.setCookie("http://dropbox.com", "locale", "en");
        br.setLoadLimit(br.getLoadLimit() * 4);

        decryptedLinks.addAll(decryptLink(parameter, ""));

        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing to download: " + parameter);
            final DownloadLink dl = this.createOfflinelink(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptLink(String link, String subfolder) throws Exception {
        currentPackage = null;
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                if (currentPackage != null) {
                    currentPackage.add(e);
                }
                distribute(e);
                return super.add(e);
            }
        };

        link = link.replaceAll("\\?dl=\\d", "");

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link);
            if (con.getResponseCode() == 404) {
                final DownloadLink dl = this.createOfflinelink(link);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            if (con.getResponseCode() == 460) {
                logger.info("Restricted Content: This file is no longer available. For additional information contact Dropbox Support. " + link);
                return decryptedLinks;
            }
            // Temporarily unavailable links
            if (con.getResponseCode() == 509) {
                final DownloadLink dl = createDownloadlink(link.replace("dropbox.com/", "dropboxdecrypted.com/"));
                dl.setProperty("decrypted", true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            if (con.getResponseCode() == 302 && (link.matches(TYPE_REDIRECT) || link.matches(TYPE_SHORT))) {
                link = br.getRedirectLocation();
                if (link.matches(TYPE_DIRECTLINK)) {
                    final DownloadLink direct = createDownloadlink("directhttp://" + link);
                    decryptedLinks.add(direct);
                    return decryptedLinks;
                } else if (link.matches(TYPE_S)) {
                    decryptedLinks.add(createSingleDownloadLink(link));
                    return decryptedLinks;
                } else if (link.matches(TYPE_REFERRAL)) {
                    final DownloadLink dl = this.createOfflinelink(link);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                } else if (!link.matches(TYPE_NORMAL)) {
                    logger.warning("Decrypter broken or unsupported redirect-url: " + link);
                    return null;
                }
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        // Decrypt file- and folderlinks
        String fpName = br.getRegex("content=\"([^<>/]*?)\" property=\"og:title\"").getMatch(0);

        if (fpName != null) {
            if (fpName.contains("\\")) {
                fpName = Encoding.unescape(fpName);
            }
            currentPackage = FilePackage.getInstance();
            currentPackage.setName(Encoding.htmlDecode(fpName.trim()));
            subfolder += "/" + fpName;
        }

        /*
         * 2017-01-27: This does not work anymore - also their .zip downloads often fail so rather not do this!Decrypt "Download as zip"
         * link if available and wished by the user
         */
        if (br.containsHTML(">Download as \\.zip<") && PluginJsonConfig.get(DropboxConfig.class).isZipFolderDownloadEnabled()) {
            final DownloadLink dl = createDownloadlink(link.replace("dropbox.com/", "dropboxdecrypted.com/"));
            dl.setName(fpName + ".zip");
            dl.setProperty("decrypted", true);
            dl.setProperty("type", "zip");
            dl.setProperty("directlink", link.replaceAll("\\?dl=\\d", "") + "?dl=1");
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolder);
            decryptedLinks.add(dl);
        }

        final String json_source = getJsonSource(this.br);

        /* 2017-01-27 new */
        boolean isSingleFile = false;
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
        ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "components/{0}/props/contents/files");
        if (ressourcelist == null) {
            /* Null? Then we probably have a single file */
            ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "components/{0}/props/files");
            isSingleFile = true;
        }
        for (final Object o : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) o;
            String url = (String) entries.get("href");
            if (url == null && isSingleFile) {
                url = link;
            }
            final String filename = (String) entries.get("filename");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("bytes"), 0);

            if (url == null || url.equals("") || filename == null || filename.equals("")) {
                return null;
            }

            final DownloadLink dl = createSingleDownloadLink(url);

            if (filesize > 0) {
                dl.setDownloadSize(filesize);
            }
            dl.setName(filename);
            dl.setAvailable(true);
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolder);
            decryptedLinks.add(dl);

            if (isSingleFile) {
                /* Array should only contain 1 element in this case but let's make sure we don't get issues by serverside bugs. */
                break;
            }
        }

        return decryptedLinks;
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);

        return ret;
    }

    public static String getJsonSource(final Browser br) {
        String json_source = br.getRegex("mod\\.initialize_module\\((\\{\"components\".*?)\\);\\s+").getMatch(0);
        if (json_source == null) {
            json_source = br.getRegex("mod\\.initialize_module\\((\\{.*?)\\);\\s+").getMatch(0);
        }
        return json_source;
    }

    private DownloadLink createSingleDownloadLink(String parameter) {
        parameter = parameter.replace("www.", "");
        parameter = parameter.replace("dropbox.com/", "dropboxdecrypted.com/");
        final DownloadLink dl = createDownloadlink(parameter);
        dl.setProperty("decrypted", true);
        return dl;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}