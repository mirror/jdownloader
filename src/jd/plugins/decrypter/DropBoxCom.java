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

import java.awt.Dialog.ModalityType;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DropboxCom.DropboxConfig;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://(?:www\\.)?dropbox\\.com/(?:(?:sh|sc|s)/[^<>\"]+|l/[A-Za-z0-9]+)(?:\\&crawl_subfolders=(?:true|false))?|https?://(www\\.)?db\\.tt/[A-Za-z0-9]+" })
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

        CrawledLink current = getCurrentLink();
        String subfolder = "";
        while (current != null) {
            if (current.getDownloadLink() != null && getSupportedLinks().matcher(current.getURL()).matches()) {
                final String path = current.getDownloadLink().getStringProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, null);
                if (path != null) {
                    subfolder = path;
                }
                break;
            }
            current = current.getSourceLink();
        }
        decryptedLinks.addAll(decryptLink(parameter, subfolder));

        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing to download: " + parameter);
            final DownloadLink dl = this.createOfflinelink(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptLink(String link, String subfolder) throws Exception {
        final String crawl_subfolder_string = new Regex(link, "(\\&crawl_subfolders=(?:true|false))").getMatch(0);
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
        if (crawl_subfolder_string != null) {
            link = link.replace(crawl_subfolder_string, "");
        }

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link);
            if (con.getResponseCode() == 429) {
                logger.info("URL's downloads are disabled due to it generating too much traffic");
                return decryptedLinks;
            } else if (con.getResponseCode() == 460) {
                logger.info("Restricted Content: This file is no longer available. For additional information contact Dropbox Support.");
                return decryptedLinks;
            } else if (con.getResponseCode() == 509) {
                /* Temporarily unavailable links */
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
            br.setFollowRedirects(true);
            br.followConnection();
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                br.getPage(redirect);
            }
            if (this.br.containsHTML("sharing/error_shmodel|class=\"not-found\">")) {
                final DownloadLink dl = this.createOfflinelink(link);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Throwable e) {
            }
        }
        // Decrypt file- and folderlinks
        String fpName = br.getRegex("content=\"([^<>/]*?)\" property=\"og:title\"").getMatch(0);

        if (fpName != null) {
            if (fpName.contains("\\")) {
                fpName = Encoding.unicodeDecode(fpName);
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
        boolean decryptSubfolders = crawl_subfolder_string != null && crawl_subfolder_string.contains("crawl_subfolders=true");

        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
        final ArrayList<Object> ressourcelist_folders = getFoldersList(entries);
        ArrayList<Object> ressourcelist_files = getFilesList(entries);
        isSingleFile = ressourcelist_files != null && ressourcelist_files.size() == 1;
        if (ressourcelist_folders != null && ressourcelist_folders.size() > 0 && !decryptSubfolders) {
            /* Only ask user if we actually have subfolders that can be decrypted! */
            final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, link, "For this URL JDownloader can crawl the files inside the current folder or crawl subfolders as well. What would you like to do?", null, "Add files of current folder AND subfolders?", "Add only files of current folder?") {
                @Override
                public ModalityType getModalityType() {
                    return ModalityType.MODELESS;
                }

                @Override
                public boolean isRemoteAPIEnabled() {
                    return true;
                }
            };
            try {
                UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                decryptSubfolders = true;
            } catch (DialogCanceledException e) {
                decryptSubfolders = false;
            } catch (DialogClosedException e) {
                decryptSubfolders = false;
            }
        }
        if (ressourcelist_files != null) {
            for (final Object o : ressourcelist_files) {
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
            }
        }

        if (decryptSubfolders) {
            for (final Object o : ressourcelist_folders) {
                entries = (LinkedHashMap<String, Object>) o;
                final boolean is_dir = ((Boolean) entries.get("is_dir")).booleanValue();
                String url = (String) entries.get("href");
                if (!is_dir || url == null || url.equals("")) {
                    continue;
                }
                url += "&crawl_subfolders=true";
                final DownloadLink subFolderDownloadLink = this.createDownloadlink(url);
                subFolderDownloadLink.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolder);
                decryptedLinks.add(subFolderDownloadLink);
            }
        }

        return decryptedLinks;
    }

    public static ArrayList<Object> getFoldersList(LinkedHashMap<String, Object> entries) {
        if (!entries.containsKey("props")) {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "components/{0}");
        }
        final ArrayList<Object> foldersList = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "props/contents/folders");
        return foldersList;
    }

    public static ArrayList<Object> getFilesList(LinkedHashMap<String, Object> entries) {
        ArrayList<Object> filesList;
        if (!entries.containsKey("props")) {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "components/{0}");
        }
        filesList = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "props/contents/files");
        /* Null? Then we probably have a single file */
        if (filesList == null) {
            filesList = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "props/files");
        }
        return filesList;
    }

    @Override
    protected DownloadLink createDownloadlink(final String link) {
        final DownloadLink ret = super.createDownloadlink(link);
        return ret;
    }

    public static String getJsonSource(final Browser br) {
        String json_source = br.getRegex("InitReact\\.mountComponent\\(mod,\\s*?(\\{.*?\\})\\)").getMatch(0);
        if (json_source == null) {
            json_source = br.getRegex("mod\\.initialize_module\\((\\{\"components\".*?)\\);\\s+").getMatch(0);
        }
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