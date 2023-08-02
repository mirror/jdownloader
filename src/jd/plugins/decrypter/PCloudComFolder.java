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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.PCloudCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcloud.com" }, urls = { "https?://(?:[a-z0-9]+\\.pcloud\\.(?:com|link)/#page=publink\\&code=|[a-z0-9]+\\.pcloud\\.(?:com|link)/publink/show\\?code=|pc\\.cd/)([A-Za-z0-9]+)" })
public class PCloudComFolder extends PluginForDecrypt {
    public PCloudComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        prepBR(br);
        return br;
    }

    public static void prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    private static final String DOWNLOAD_ZIP = "DOWNLOAD_ZIP_2";
    long                        totalSize    = 0;
    private String              foldercode   = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.getCryptedUrl();
        foldercode = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        if (foldercode == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String passCode = param.getDecrypterPassword();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String fid = getFID(parameter);
        int attempt = 0;
        Map<String, Object> entries = null;
        boolean passwordSuccess = false;
        final int errorcodeInvalidPasswordProvided = 1125;
        final int errorcodePasswordProtected = 2258;
        int result = 0;
        do {
            attempt++;
            if (attempt > 1) {
                passCode = getUserInput("Password?", param);
            }
            final UrlQuery query = new UrlQuery();
            query.add("code", fid);
            if (passCode != null) {
                query.add("linkpassword", Encoding.urlEncode(passCode));
            }
            br.getPage("https://" + PCloudCom.getAPIDomain(new URL(parameter).getHost()) + "/showpublink?" + query);
            entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.getRequest().getHtmlCode());
            result = ((Number) entries.get("result")).intValue();
            if (result == errorcodePasswordProtected || result == errorcodeInvalidPasswordProvided) {
                if (passCode != null) {
                    logger.info("User entered invalid password: " + passCode);
                } else {
                    logger.info("This item is password protected");
                }
                passwordSuccess = false;
                continue;
            } else {
                passwordSuccess = true;
                break;
            }
        } while (attempt <= 3);
        if (!passwordSuccess) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        final Map<String, Object> metadata = (Map<String, Object>) entries.get("metadata");
        if (metadata == null) {
            /* Looks like item is offline */
            final String errormsg = (String) entries.get("error");
            /* 7002 = deleted by the owner, 7003 = abused */
            if (result == 7002 || result == 7003) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                logger.info("Item is offline because: " + errormsg);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String folderNameMain = (String) metadata.get("name");
        addFolder(metadata, null, parameter);
        if (ret.size() > 1 && SubConfiguration.getConfig(this.getHost()).getBooleanProperty(DOWNLOAD_ZIP, false)) {
            /* = all files (links) of the folder as .zip archive */
            final DownloadLink main = createDownloadlink("http://pclouddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            main.setProperty("plain_code", foldercode);
            final String main_name = folderNameMain + ".zip";
            main.setFinalFileName(folderNameMain);
            main.setProperty("plain_name", main_name);
            main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty("complete_folder", true);
            main.setProperty("plain_code", foldercode);
            if (totalSize > 0) {
                main.setDownloadSize(totalSize);
            }
            main.setAvailable(true);
            ret.add(main);
        }
        /* Set additional properties */
        for (final DownloadLink thisresult : ret) {
            thisresult.setProperty("mainlink", parameter);
            thisresult.setContentUrl(parameter);
            if (passCode != null) {
                thisresult.setDownloadPassword(passCode, true);
            }
        }
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + folderNameMain);
        }
        return ret;
    }

    /** Recursive function to crawl all folders/subfolders */
    @SuppressWarnings("unchecked")
    private void addFolder(final Map<String, Object> entries, String lastFpname, final String containerURL) {
        List<Map<String, Object>> ressourcelist_temp = null;
        final boolean isFolder = ((Boolean) entries.get("isfolder"));
        if (isFolder) {
            /* Only update lastFoldername if we actually have a folder ... */
            lastFpname = (String) entries.get("name");
            ressourcelist_temp = (List<Map<String, Object>>) entries.get("contents");
            for (final Map<String, Object> ressource : ressourcelist_temp) {
                addFolder(ressource, lastFpname, containerURL);
            }
        } else {
            addSingleItem(entries, lastFpname, containerURL);
        }
    }

    private DownloadLink addSingleItem(final Map<String, Object> entries, final String fpName, final String containerURL) {
        final DownloadLink dl = createDownloadlink("http://pclouddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        String filename = entries.get("name").toString();
        final long fileid = JavaScriptEngineFactory.toLong(entries.get("fileid"), 0);
        filename = Encoding.htmlDecode(filename.trim());
        totalSize += filesize;
        dl.setDownloadSize(filesize);
        dl.setFinalFileName(filename);
        dl.setProperty("plain_name", filename);
        dl.setProperty("plain_size", filesize);
        dl.setProperty("plain_fileid", fileid);
        dl.setProperty("plain_code", foldercode);
        dl.setLinkID(foldercode + fileid);
        dl.setAvailable(true);
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            dl._setFilePackage(fp);
        }
        return dl;
    }

    private String getFID(final String link) {
        return new Regex(link, this.getSupportedLinks()).getMatch(0);
    }
}
