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
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.config.YoutvDeConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.YoutvDe;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class YoutvDeCrawler extends PluginForDecrypt {
    public YoutvDeCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return YoutvDe.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/videorekorder.*");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final YoutvDeConfig cfg = PluginJsonConfig.get(YoutvDeConfig.class);
        if (!cfg.isEnableRecordingsCrawler()) {
            logger.info("Doing nothing because user has disabled recordings crawler");
            return ret;
        }
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            throw new AccountRequiredException();
        }
        final YoutvDe hosterplugin = (YoutvDe) this.getNewPluginForHostInstance(this.getHost());
        hosterplugin.login(account, false);
        br.setFollowRedirects(true);
        br.getPage(YoutvDe.WEBAPI_BASE + "/recs.json");
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object errorsO = entries.get("errors");
        if (errorsO != null) {
            /* Most likely a login failure. This should never happen! */
            logger.warning("WTF: " + errorsO);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("Meine Aufnahmen");
        // int numberofAddedItems = 0;
        int numberofQueuedItems = 0;
        final List<Map<String, Object>> recordings = (List<Map<String, Object>>) entries.get("recordings");
        if (recordings.size() == 0) {
            logger.info("User has no recordings at all");
            return ret;
        }
        for (final Map<String, Object> recording : recordings) {
            final String id = recording.get("id").toString();
            final String status = recording.get("status").toString();
            if (status.equals("queued")) {
                numberofQueuedItems++;
                if (!cfg.isRecordingsCrawlerAddQueuedRecordings()) {
                    logger.info("Skipping ID " + id + " because of status: " + status);
                    continue;
                }
            }
            final DownloadLink link = this.createDownloadlink("https://www." + this.getHost() + "/tv-sendungen/" + id + "-" + toSlug(recording.get("title").toString()));
            hosterplugin.parseFileInformation(link, recording);
            link._setFilePackage(fp);
            link.setAvailable(true);
            ret.add(link);
            distribute(link);
            // numberofAddedItems++;
        }
        if (ret.isEmpty()) {
            logger.info("User has only " + numberofQueuedItems + " queued recordings but disabled adding those via plugin settings");
        }
        return ret;
    }

    private String toSlug(final String str) {
        final String preparedSlug = str.toLowerCase(Locale.ENGLISH).replace("ü", "u").replace("ä", "a").replace("ö", "o");
        String slug = preparedSlug.replaceAll("[^a-z0-9]", "-");
        /* Remove double-minus */
        slug = slug.replaceAll("-{2,}", "-");
        /* Do not begin with minus */
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        /* Do not end with minus */
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug;
    }
}
