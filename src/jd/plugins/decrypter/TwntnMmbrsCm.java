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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.TwentyOneMembersVariantInfo;
import jd.plugins.hoster.TwentyOneMembersCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "21members.com" }, urls = { "https://members.21members.com/[a-z]{2}/video/.+\\d+$" })
public class TwntnMmbrsCm extends PluginForDecrypt {
    public TwntnMmbrsCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (!TwentyOneMembersCom.login(br)) {
            UIOManager.I().showErrorMessage("An active 21members.com account is required for " + parameter);
            return decryptedLinks;
        }
        final String id = new Regex(parameter, "(\\d+)$").getMatch(0);
        br.getPage(parameter);
        if (jd.plugins.hoster.TwentyOneMembersCom.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final ArrayList<TwentyOneMembersVariantInfo> videoVariantInfos = TwentyOneMembersCom.parseVideoVariants(br);
        if (videoVariantInfos == null || videoVariantInfos.size() == 0) {
            UIOManager.I().showErrorMessage("No access to " + parameter);
            return decryptedLinks;
        }
        sortVariants(videoVariantInfos);
        String title = PluginJSonUtils.getJson(this.br, "sceneTitle");
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = id;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final DownloadLink videoDummy = createDownloadlink("http://21members.com/dummy/file/" + id);
        videoDummy.setProperty("title", title);
        videoDummy.setProperty("id", id);
        videoDummy.setVariantSupport(true);
        videoDummy.setVariants(videoVariantInfos);
        TwentyOneMembersCom.setVariant(videoDummy, videoVariantInfos.get(0));
        decryptedLinks.add(videoDummy);
        br.getPage("http://21members.com/members/scene/photos/" + id + "/");
        final ArrayList<TwentyOneMembersVariantInfo> photoVariantInfos = TwentyOneMembersCom.parsePhotoVariants(br);
        if (photoVariantInfos != null && photoVariantInfos.size() > 0) {
            sortVariants(photoVariantInfos);
            final DownloadLink photoDummy = createDownloadlink("http://21members.com/dummy/file/" + id);
            photoDummy.setProperty("title", title);
            photoDummy.setProperty("id", id);
            photoDummy.setVariantSupport(true);
            photoDummy.setVariants(photoVariantInfos);
            TwentyOneMembersCom.setVariant(photoDummy, photoVariantInfos.get(0));
            decryptedLinks.add(photoDummy);
        }
        // TwentyOneMembersCom.update
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /**
     * @param variantInfos
     */
    private void sortVariants(final List<TwentyOneMembersVariantInfo> variantInfos) {
        if (variantInfos != null) {
            for (final TwentyOneMembersVariantInfo v : variantInfos) {
                v.setUrl(null);
            }
            if (variantInfos.size() > 1) {
                Collections.sort(variantInfos, new Comparator<TwentyOneMembersVariantInfo>() {
                    public int compare(int x, int y) {
                        return (x < y) ? -1 : ((x == y) ? 0 : 1);
                    }

                    @Override
                    public int compare(TwentyOneMembersVariantInfo o1, TwentyOneMembersVariantInfo o2) {
                        return compare(o2._getQuality(), o1._getQuality());
                    }
                });
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}