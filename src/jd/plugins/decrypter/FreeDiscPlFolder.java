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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.FreeDiscPlConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.FreeDiscPl;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "freedisc.pl" }, urls = { "https?://(?:(?:www|m)\\.)?freedisc\\.pl/([A-Za-z0-9_\\-]+),d-(\\d+)(,([\\w\\-]+))?" })
public class FreeDiscPlFolder extends PluginForDecrypt {
    public FreeDiscPlFolder(PluginWrapper wrapper) {
        super(wrapper);
        try {
            Browser.setBurstRequestIntervalLimitGlobal("freedisc.pl", 1000, 20, 60000);
        } catch (final Throwable ignore) {
        }
    }

    // private static final String TYPE_FOLDER = "https?://(www\\.)?freedisc\\.pl/[A-Za-z0-9\\-_]+,d-\\d+";
    protected static Cookies botSafeCookies = new Cookies();

    private Browser prepBR(final Browser br, final Account account) {
        FreeDiscPl.prepBRStatic(br);
        /* In account mode we're using account cookies thus we only need those whenthere is not account available. */
        if (account == null) {
            synchronized (botSafeCookies) {
                if (!botSafeCookies.isEmpty()) {
                    br.setCookies(this.getHost(), botSafeCookies);
                }
            }
        }
        return br;
    }

    /* 2017-01-06: Avoid anti-bot captchas. */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex dir = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String user = dir.getMatch(0);
        final String folderID = dir.getMatch(1);
        // final String folderSlug = dir.getMatch(3);
        br.setFollowRedirects(true);
        prepBR(this.br, account);
        /* Login whenever possible. The only benefit we got from this in this crawler is that we should not get any antiBot captchas. */
        if (account != null) {
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((FreeDiscPl) plg).login(account, false);
        }
        FreeDiscPl.prepBRAjax(this.br);
        /* First let's find the absolute path to the folder we want. If we fail to do so we can assume that it is offline. */
        getPage("https://" + this.getHost() + "/directory/directory_data/get_tree/" + user, param, account);
        final Map<String, Object> responseFolderTree = restoreFromString(br.toString(), TypeRef.MAP);
        final Map<String, Map<String, Object>> folderTree = (Map<String, Map<String, Object>>) JavaScriptEngineFactory.walkJson(responseFolderTree, "response/data");
        final Map<String, Object> folderInfo = findFolderMap(folderTree, folderID);
        if (folderInfo == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String folderPath = findFolderPath(folderTree, folderID);
        final String dir_count = folderInfo.get("dir_count").toString();
        final String file_count = folderInfo.get("file_count").toString();
        if (dir_count.equals("0") && file_count.equals("0")) {
            /* Folder is empty --> Return dummy item */
            final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "EMPTY_FOLDER_" + folderPath, "This folder is empty");
            ret.add(dummy);
            return ret;
        }
        getPage("https://" + this.getHost() + "/directory/directory_data/get/" + user + "/" + folderID, param, account);
        final boolean crawlSubfolders = PluginJsonConfig.get(FreeDiscPlConfig.class).isCrawlSubfolders();
        final Map<String, Object> responseFolder = restoreFromString(br.toString(), TypeRef.MAP);
        final Map<String, Map<String, Object>> data = (Map<String, Map<String, Object>>) JavaScriptEngineFactory.walkJson(responseFolder, "response/data/data");
        for (final Map<String, Object> resource : data.values()) {
            final String type = resource.get("type").toString();
            final boolean isFolder = type.equalsIgnoreCase("d");
            if (isFolder && !crawlSubfolders) {
                continue;
            }
            final String id = resource.get("id").toString();
            final String title = resource.get("name").toString();
            final String name_url = resource.get("name_url").toString();
            final String filesize = resource.get("size_format").toString();
            final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/" + user + "," + type + "-" + id + "," + name_url);
            if (!isFolder) {
                /* Try to fix extension in filename as their filenames would usually end like "-<ext>". */
                final String realExtension = FreeDiscPl.getExtensionFromNameInFileURL(name_url);
                if (realExtension != null && !title.toLowerCase(Locale.ENGLISH).endsWith(realExtension.toLowerCase(Locale.ENGLISH))) {
                    dl.setName(title + realExtension);
                } else {
                    dl.setName(title);
                }
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setAvailable(true);
            }
            dl.setRelativeDownloadFolderPath(folderPath);
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderPath);
        fp.addLinks(ret);
        return ret;
    }

    private Map<String, Object> findFolderMap(final Map<String, Map<String, Object>> folderTree, final String folderID) {
        for (final Map<String, Object> folders : folderTree.values()) {
            for (final Object folderO : folders.values()) {
                final Map<String, Object> folder = (Map<String, Object>) folderO;
                if (folder.get("id").toString().equals(folderID)) {
                    return folder;
                }
            }
        }
        return null;
    }

    private String findFolderPath(final Map<String, Map<String, Object>> folderTree, final String currentFolderID) {
        final ArrayList<String> dupes = new ArrayList<String>();
        Map<String, Object> folderInfo = null;
        String path = null;
        int depth = 0;
        do {
            final String parent_id;
            if (depth == 0) {
                parent_id = currentFolderID;
            } else {
                parent_id = folderInfo.get("parent_id").toString();
            }
            if (dupes.contains(parent_id)) {
                /* Root has parent_id of itself --> Prevent infinite root - we're done! */
                return path;
            }
            dupes.add(parent_id);
            folderInfo = findFolderMap(folderTree, parent_id);
            final String folderName = folderInfo.get("name").toString();
            if (path == null) {
                path = folderName;
            } else {
                /* Build path from inside to root. */
                path = folderName + "/" + path;
            }
            depth++;
        } while (!this.isAbort());
        return path;
    }

    private void getPage(final String url, final CryptedLink param, final Account account) throws Exception {
        this.br.getPage(url);
        handleAntiBot(br, param, account);
    }

    private void handleAntiBot(final Browser br, final CryptedLink param, final Account account) throws Exception {
        final Object lock = account != null ? account : botSafeCookies;
        synchronized (lock) {
            final Future<Boolean> abort = new Future<Boolean>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Boolean get() throws InterruptedException, ExecutionException {
                    return FreeDiscPlFolder.this.isAbort();
                }

                @Override
                public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return FreeDiscPlFolder.this.isAbort();
                }
            };
            FreeDiscPl.handleAntiBot(this, br, abort, null, param);
            FreeDiscPl.saveSession(botSafeCookies, br, account);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}