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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.PixeldrainCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PixeldrainCom.class })
public class PixeldrainComFolder extends PluginForDecrypt {
    public PixeldrainComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return PixeldrainCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/l/([A-Za-z0-9]+)((?:\\?embed)?#item=(\\d+))?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String folderID = urlinfo.getMatch(0);
        PixeldrainCom.prepBR(this.br);
        br.getPage(PixeldrainCom.API_BASE + "/list/" + folderID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* 2020-10-01: E.g. {"success":false,"value":"not_found","message":"The entity you requested could not be found"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> folder = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String folderName = (String) folder.get("title");
        final List<Map<String, Object>> files = (List<Map<String, Object>>) folder.get("files");
        if (files.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "EMPTY_FOLDER_l_" + folderID, "This folder exists but is empty.", null);
        }
        final Number targetIndex;
        final String targetIndexStr = urlinfo.getMatch(2);
        if (targetIndexStr != null) {
            targetIndex = Integer.parseInt(targetIndexStr);
        } else {
            targetIndex = null;
        }
        int index = 0;
        for (final Map<String, Object> file : files) {
            final DownloadLink dl = this.createDownloadlink(generateFileURL(file.get("id").toString()));
            dl.setContentUrl(generateContentURL(folderID, index));
            dl.setContainerUrl(param.getCryptedUrl());
            PixeldrainCom.setDownloadLinkInfo(this, dl, null, file);
            if (targetIndex != null && index == targetIndex.intValue()) {
                /* User wants only one item within that folder */
                logger.info("Found target-file at index: " + index + " | " + dl.getFinalFileName());
                ret.clear();
                ret.add(dl);
                break;
            } else {
                ret.add(dl);
                index += 1;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        if (!StringUtils.isEmpty(folderName)) {
            fp.setName(folderName);
        } else {
            /* Fallback */
            fp.setName(folderID);
        }
        fp.addLinks(ret);
        return ret;
    }

    private String generateFileURL(final String fileID) {
        return "https://" + this.getHost() + "/u/" + fileID;
    }

    private String generateContentURL(final String folderID, final int folderIndex) {
        return "https://" + this.getHost() + "/l/" + folderID + "#item=" + folderIndex;
    }
}
