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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.HighWayMe2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HighWayMeFolder2 extends PluginForDecrypt {
    public HighWayMeFolder2(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "high-way.me" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/pages/(?:center|torrent|usenet)/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            displayBubblenotifyMessage("Account benötigt", "Account benötigt, um Torrent und Usenet Dateien aus dem eigenen High-Way Account einfügen zu können.");
            throw new AccountRequiredException();
        }
        final HighWayMe2 hosterplugin = (HighWayMe2) this.getNewPluginForHostInstance(this.getHost());
        hosterplugin.login(account, false);
        boolean crawlTorrent = false;
        boolean crawlUsenet = false;
        if (StringUtils.containsIgnoreCase(param.getCryptedUrl(), "torrent")) {
            crawlTorrent = true;
        } else if (StringUtils.containsIgnoreCase(param.getCryptedUrl(), "usenet")) {
            crawlUsenet = true;
        } else {
            /* No specific category given in URL -> Crawl both */
            crawlTorrent = true;
            crawlUsenet = true;
        }
        int numberofSkippedItems = 0;
        torrentCrawler: if (crawlTorrent) {
            br.getPage(hosterplugin.getWebsiteBase() + "torrent.php?action=list&json&order=1&suche=");
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> items = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "arguments/torrents");
            if (items == null || items.isEmpty()) {
                logger.info("Failed to find any items for category: torrent");
                break torrentCrawler;
            }
            for (final Map<String, Object> item : items) {
                final String percentDownloaded = item.get("percentDone").toString();
                final String name = item.get("name").toString();
                // final String status = item.get("Status").toString();
                if (!percentDownloaded.equals("100")) {
                    logger.info("Skipping unfinished item: " + name + " | Percent: " + percentDownloaded);
                    numberofSkippedItems++;
                    continue;
                }
                final DownloadLink link = this.createDownloadlink(item.get("link").toString());
                // final String filesizeBytesStr = (String) item.get("Size");
                // if (filesizeBytesStr != null) {
                // link.setDownloadSize(Long.parseLong(filesizeBytesStr));
                // }
                link.setName(name);
                /*
                 * Do not set availablestatus as we most likely got a folder containing multiple items -> Needs to be crawled via
                 * HighWayMeFolder
                 */
                // link.setAvailable(true);
                link.setProperty(HighWayMeFolder.PROPERTY_ALTERNATIVE_ROOT_FOLDER_TITLE, name);
                ret.add(link);
                distribute(link);
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                return ret;
            }
        }
        usenetCrawler: if (crawlUsenet) {
            br.getPage(hosterplugin.getWebsiteBase() + "usenet.php?action=list&json&order=1&suche=");
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> items = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "arguments/usenet");
            if (items == null || items.isEmpty()) {
                logger.info("Failed to find any items for category: usenet");
                break usenetCrawler;
            }
            final boolean trustZipAsSingleFile = false;
            for (final Map<String, Object> item : items) {
                final String percentDownloaded = item.get("Prozent").toString();
                final String name = item.get("Name").toString();
                final String zip = item.get("zip").toString();
                // final String status = item.get("Status").toString();
                if (!percentDownloaded.equals("100")) {
                    logger.info("Skipping unfinished item: " + name + " | Percent: " + percentDownloaded);
                    numberofSkippedItems++;
                    continue;
                }
                final DownloadLink link = this.createDownloadlink(item.get("link").toString());
                if (StringUtils.equals(zip, "1") && trustZipAsSingleFile) {
                    /* We know we got only one file and it is downloadable. */
                    link.setName(Plugin.getCorrectOrApplyFileNameExtension(name, ".zip"));
                    final String filesizeBytesStr = (String) item.get("Size");
                    if (filesizeBytesStr != null) {
                        link.setDownloadSize(Long.parseLong(filesizeBytesStr));
                    }
                    link.setAvailable(true);
                } else {
                    link.setName(name);
                    /*
                     * Do not set availablestatus as we most likely got a folder containing multiple items -> Needs to be crawled via
                     * HighWayMeFolder
                     */
                    // link.setAvailable(true);
                    /* Save extra properties which can be useful for subsequent crawling processes. */
                    link.setProperty(HighWayMeFolder.PROPERTY_ALTERNATIVE_ROOT_FOLDER_TITLE, name);
                }
                ret.add(link);
                distribute(link);
            }
        }
        if (ret.isEmpty()) {
            logger.info("Failed to find any results | Number of skipped items: " + numberofSkippedItems);
            displayBubblenotifyMessage("Nichts gefunden", "Es konnten keine eigenen torrents/usenet Downloads gefunden werden.\r\nAnzahl übersprungener/unfertiger Elemente: " + numberofSkippedItems);
        }
        return ret;
    }

    private void displayBubblenotifyMessage(final String title, final String msg) {
        BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
            @Override
            public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                return new BasicNotify("High-Way: " + title, msg, new AbstractIcon(IconKey.ICON_INFO, 32));
            }
        });
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
