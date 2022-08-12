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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.config.IssuuComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "issuu.com" }, urls = { "https?://(?:www\\.)?issuu\\.com/[a-z0-9\\-_\\.]+/docs/[a-z0-9\\-_\\.]+|https?://e\\.issuu\\.com/embed\\.html#\\d+/\\d+" })
public class IssuuCom extends PluginForDecrypt {
    public IssuuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_NORMAL = "https?://(?:www\\.)?issuu\\.com/([a-z0-9\\-_\\.]+)/docs/([a-z0-9\\-_\\.]+)";
    private static final String TYPE_EMBED  = "https?://e\\.issuu\\.com/embed\\.html#(\\d+)/(\\d+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Regex embed = new Regex(param.getCryptedUrl(), TYPE_EMBED);
        final IssuuComConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        this.br.setFollowRedirects(true);
        String documentID = null;
        final String ownerUsername;
        final String documentURI;
        if (embed.matches()) {
            /* 2021-11-22: This internal ID is now only given for embed objects! Also we don't really need it anymore. */
            documentID = embed.getMatch(1);
            final boolean useNewMethod = true;
            if (useNewMethod) {
                final Browser brc = br.cloneBrowser();
                brc.getPage("https://e.issuu.com/config/" + documentID + ".json");
                if (isOffline(brc)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                ownerUsername = entries.get("ownerUsername").toString();
                documentURI = entries.get("documentURI").toString();
            } else {
                final String embedIDs = embed.getMatch(0) + "/" + embed.getMatch(1);
                /* 2017-01-21: New - added embed support */
                br.getPage("http://e." + this.getHost() + "/embed/" + embedIDs + ".json?v=1.0.0");
                if (isOffline(br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        } else {
            final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_NORMAL);
            ownerUsername = urlinfo.getMatch(0);
            documentURI = urlinfo.getMatch(1);
        }
        br.getPage("https://api.issuu.com/call/backend-reader3/dynamic/" + ownerUsername + "/" + documentURI);
        if (isOffline(br)) {
            /* E.g. {"message":"Document does not exist"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> docInfo = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> metadata = (Map<String, Object>) docInfo.get("metadata");
        br.getPage("https://reader3.isu.pub/" + ownerUsername + "/" + documentURI + "/reader3_4.json");
        Map<String, Object> docInfo2 = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> document = (Map<String, Object>) docInfo2.get("document");
        final List<Map<String, Object>> pages = (List<Map<String, Object>>) document.get("pages");
        // final String username = docInfo.get("userDisplayName").toString();
        final String title = metadata.get("title").toString();
        final DecimalFormat df = new DecimalFormat("0000");
        final String generalNaming = title + " by " + ownerUsername + " [" + title + "] (" + pages.size() + " pages)";
        if (!((Boolean) metadata.get("downloadable")) || cfg.isPreferImagesOverPDF()) {
            /* Old format of directURLs: "https://image.issuu.com/<documentID>/jpg/page_<pageNumberStartingFromOne>.jpg" */
            int pagenumber = 0;
            for (final Map<String, Object> page : pages) {
                pagenumber++;
                // final boolean isPagePaywalled = ((Boolean) page.get("isPagePaywalled")).booleanValue();
                final DownloadLink dl = createDownloadlink("https://" + page.get("imageUri"));
                dl.setFinalFileName(title + "_page_" + df.format(pagenumber) + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            final DownloadLink pdf = createDownloadlink(param.getCryptedUrl());
            pdf.setAvailable(true);
            final String pdfFilename = generalNaming + ".pdf";
            pdf.setFinalFileName(pdfFilename);
            pdf.setProperty(jd.plugins.hoster.IssuuCom.PROPERTY_FINAL_NAME, pdfFilename);
            if (documentID != null) {
                pdf.setProperty(jd.plugins.hoster.IssuuCom.PROPERTY_DOCUMENT_ID, documentID);
            }
            decryptedLinks.add(pdf);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(generalNaming);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404;
    }

    @Override
    public Class<? extends IssuuComConfig> getConfigInterface() {
        return IssuuComConfig.class;
    }
}
