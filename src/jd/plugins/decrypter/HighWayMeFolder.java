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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.hoster.HighWayMe2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HighWayMeFolder extends GenericHTTPDirectoryIndexCrawler {
    public HighWayMeFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "high-way.me", "dwld.link" });
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
            ret.add("https?://\\w+\\." + buildHostsPatternPart(domains) + "/(dl(?:u|t|[0-9]+)/[a-z0-9]+(?:/$|/.+)|dav/.+)");
        }
        return ret.toArray(new String[0]);
    }

    public static final String PROPERTY_ALTERNATIVE_ROOT_FOLDER_TITLE = "alternative_root_folder_title";
    private String             betterRootFolderName                   = null;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        betterRootFolderName = param.getDownloadLink() != null ? param.getDownloadLink().getStringProperty(PROPERTY_ALTERNATIVE_ROOT_FOLDER_TITLE) : null;
        final String domainHW = "high-way.me";
        final Account account = AccountController.getInstance().getValidAccount(domainHW);
        final HighWayMe2 plg = (HighWayMe2) this.getNewPluginForHostInstance(domainHW);
        if (account != null) {
            /* Required for WebDAV URLs. */
            plg.login(account, false);
        }
        final boolean accountRequired;
        if (param.getCryptedUrl().matches("(?i)https?://[^/]+/dav/.+")) {
            accountRequired = true;
        } else {
            accountRequired = false;
        }
        if (accountRequired && account == null) {
            logger.info("Looks like account required and no account available -> This one will most likely fail");
        }
        final ArrayList<DownloadLink> crawledItems = super.decryptIt(param, progress);
        if (param.getCryptedUrl().matches("(?i)^https?://(?:torrentarchiv|torrent)\\.[^/]+/dlt/[a-z0-9]+/$")) {
            /*
             * Root of the torrent -> Can sometimes contain one single .zip file which contains all items again -> Filter that to avoid
             * downloading everything twice!
             */
            final List<DownloadLink> remove = new ArrayList<DownloadLink>();
            for (final DownloadLink link : crawledItems) {
                if (link.getName() != null && link.getName().matches("([a-f0-9]{40}|[a-f0-9]{64})\\.zip")) {
                    remove.add(link);
                }
            }
            if (remove.size() > 0) {
                logger.info("Found- and removed complete zip(s) | " + remove);
                crawledItems.removeAll(remove);
            } else {
                logger.info("Could not find a .zip file to remove");
            }
        }
        final List<DownloadLink> remove2 = new ArrayList<DownloadLink>();
        for (final DownloadLink link : crawledItems) {
            if (StringUtils.endsWithCaseInsensitive(link.getPluginPatternMatcher(), "/__ADMIN__/") && !link.isAvailabilityStatusChecked()) {
                remove2.add(link);
            }
        }
        /* Remove folders which will end up in error 403 anyways. */
        if (remove2.size() > 0 && remove2.size() < crawledItems.size()) {
            logger.info("Removing additional unusable items: " + remove2);
            crawledItems.removeAll(remove2);
        } else if (remove2.size() > 0 && remove2.size() == crawledItems.size()) {
            logger.info("All results look to be unusable items and will most likely be displayed as offline");
        }
        /*
         * Workaround! We want directURLs to be handled by our high-way.me host plugin, not directhttp a it's usually expected to happen
         * with results of the parent plugin "GenericHTTPDirectoryIndexCrawler".
         */
        final ArrayList<DownloadLink> correctedResults = new ArrayList<DownloadLink>();
        for (final DownloadLink link : crawledItems) {
            if (StringUtils.startsWithCaseInsensitive(link.getPluginPatternMatcher(), "directhttp://")) {
                /* Ugly workaround */
                final String directurl = link.getPluginPatternMatcher().replaceFirst("(?i)directhttp://", "");
                final DownloadLink newlink = new DownloadLink(plg, null, plg.getHost(), directurl, true);
                // link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("(?i)directhttp://", ""));
                // link.setHost("high-way.me");
                // link.setLivePlugin(plg);
                newlink.setFinalFileName(link.getFinalFileName());
                newlink.setVerifiedFileSize(link.getVerifiedFileSize());
                newlink.setRelativeDownloadFolderPath(link.getRelativeDownloadFolderPath());
                newlink.setAvailable(true);
                if (link.getFilePackage() != null) {
                    newlink._setFilePackage(link.getFilePackage());
                }
                correctedResults.add(newlink);
            } else {
                correctedResults.add(link);
            }
        }
        /* Set additional properties */
        for (final DownloadLink link : correctedResults) {
            link.setProperty(PROPERTY_ALTERNATIVE_ROOT_FOLDER_TITLE, this.betterRootFolderName);
        }
        return correctedResults;
    }

    @Override
    protected String getCurrentDirectoryPath(final Browser br) {
        String path = super.getCurrentDirectoryPath(br);
        if (path == null) {
            return null;
        } else {
            /* Remove internal base path as it's not required for the user. */
            final String removeThis1 = new Regex(path, "(?i)^(/torrent/([a-f0-9]{40}|[a-f0-9]{64}))/").getMatch(0);
            if (removeThis1 != null) {
                path = path.replaceFirst(removeThis1, "");
            }
            final String removeThis2 = new Regex(path, "(?i)^(/usenet/(incomplete/)?[^/]+)/").getMatch(0);
            if (removeThis2 != null) {
                path = path.replaceFirst(removeThis2, "");
            }
            final String rootFolderName;
            if (betterRootFolderName != null) {
                rootFolderName = betterRootFolderName;
            } else {
                rootFolderName = new Regex(br.getURL(), "/dl(?:u|t|[0-9]+)/([a-z0-9]+)").getMatch(0);
            }
            if (path.equals("/")) {
                return rootFolderName;
            }
            if (rootFolderName != null && !path.startsWith(rootFolderName) && !path.startsWith("/" + rootFolderName)) {
                if (path.startsWith("/")) {
                    path = rootFolderName + path;
                } else {
                    path = rootFolderName + "/" + path;
                }
            }
            return path;
        }
    }

    @Override
    protected String getCurrentDirectoryPath(final String url) throws UnsupportedEncodingException {
        final Regex pathregex = new Regex(url, "(?i)^https?://[^/]+/dl(?:u|t|[0-9]+)/([a-z0-9]+)/(.+)");
        if (pathregex.patternFind()) {
            final String internalRootFolder = pathregex.getMatch(0);
            final String path = pathregex.getMatch(1);
            if (betterRootFolderName != null) {
                return betterRootFolderName + "/" + URLDecoder.decode(path, "UTF-8");
            } else {
                return internalRootFolder + "/" + URLDecoder.decode(path, "UTF-8");
            }
        } else {
            return super.getCurrentDirectoryPath(url);
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
