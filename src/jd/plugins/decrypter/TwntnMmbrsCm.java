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

import javax.swing.Icon;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.TwentyOneMembersCom;

import org.appwork.storage.Storable;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.linkcrawler.LinkVariant;

@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "21members.com" }, urls = { "http://(www\\.)?21members\\.com/members/scene/(info|photos)/\\d+/.+" }, flags = { 0 })
public class TwntnMmbrsCm extends PluginForDecrypt {

    public TwntnMmbrsCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static class TwentyOneMembersVariantInfo implements Storable, LinkVariant {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getShortType() {
            return shortType;
        }

        public void setShortType(String shortType) {
            this.shortType = shortType;
        }

        private String shortType;

        public TwentyOneMembersVariantInfo(/* Storable */) {
        }

        public TwentyOneMembersVariantInfo(String url, String shortType) {
            this.url = url;

            this.shortType = shortType;
        }

        @Override
        public String _getUniqueId() {
            return shortType;
        }

        public int _getQuality() {
            if (StringUtils.equals("fullhd", shortType)) {
                return 1000;
            }
            if (StringUtils.equals("hd", shortType)) {
                return 999;
            }
            if (StringUtils.equals("hq", shortType)) {
                return 998;
            }
            if (StringUtils.equals("phone480", shortType)) {
                return 997;
            }
            if (StringUtils.equals("phone272", shortType)) {
                return 996;
            }
            if (StringUtils.equals("ziph", shortType)) {
                return 0;
            }
            if (StringUtils.equals("hiresh", shortType)) {
                return 1;
            }
            return -1;
        }

        @Override
        public String _getName() {
            if (StringUtils.equals("fullhd", shortType)) {
                return "FullHD 1080p Video";
            }
            if (StringUtils.equals("hd", shortType)) {
                return "HD 720p Video";
            }
            if (StringUtils.equals("hq", shortType)) {
                return "HQ 540p Video";
            }
            if (StringUtils.equals("phone480", shortType)) {
                return "Phone 480p Video";
            }
            if (StringUtils.equals("phone272", shortType)) {
                return "Phone 272p Video";
            }
            if (StringUtils.equals("ziph", shortType)) {
                return "Low Quality Photos";
            }
            if (StringUtils.equals("hiresh", shortType)) {
                return "High Quality Photos";
            }
            return shortType;
        }

        @Override
        public Icon _getIcon() {
            return null;
        }

        @Override
        public String _getExtendedName() {
            return shortType;
        }

        @Override
        public String _getTooltipDescription() {
            return shortType;
        }

        public String _getExtension() {
            if (StringUtils.equals("ziph", shortType)) {
                return "zip";
            }
            if (StringUtils.equals("hiresh", shortType)) {
                return "zip";
            }
            return "mp4";
        }

        public boolean isPhoto() {
            if (StringUtils.equals("ziph", shortType)) {
                return true;
            }
            if (StringUtils.equals("hiresh", shortType)) {
                return true;
            }
            return false;
        }

    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (!TwentyOneMembersCom.login(br)) {
            UIOManager.I().showErrorMessage("An active 21members.com account is required for " + parameter);
        }
        String id = new Regex(parameter, "scene/(info|photos)/(\\d+)").getMatch(1);
        br.getPage("http://21members.com/members/scene/info/" + id + "/");
        ArrayList<TwentyOneMembersVariantInfo> variantInfos = TwentyOneMembersCom.parseVideoVariants(br);
        if (variantInfos == null || variantInfos.size() == 0) {
            UIOManager.I().showErrorMessage("No access to " + parameter);
            return decryptedLinks;
        }

        sortVariants(variantInfos);
        String title = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        DownloadLink link = createDownloadlink("http://21members.com/dummy/file/" + id);
        link.setProperty("title", title);
        link.setProperty("id", id);
        link.setVariantSupport(true);
        fp.add(link);
        link.setVariants(variantInfos);

        TwentyOneMembersCom.setVariant(link, variantInfos.get(0));

        decryptedLinks.add(link);

        br.getPage("http://21members.com/members/scene/photos/" + id + "/");
        variantInfos = TwentyOneMembersCom.parsePhotoVariants(br);
        if (variantInfos == null || variantInfos.size() == 0) {
            return decryptedLinks;
        }

        link = createDownloadlink("http://21members.com/dummy/file/" + id);
        link.setProperty("title", title);
        link.setProperty("id", id);
        link.setVariantSupport(true);

        sortVariants(variantInfos);
        link.setVariants(variantInfos);
        fp.add(link);
        TwentyOneMembersCom.setVariant(link, variantInfos.get(0));
        // TwentyOneMembersCom.update
        decryptedLinks.add(link);

        return decryptedLinks;
    }

    /**
     * @param variantInfos
     */
    private void sortVariants(ArrayList<TwentyOneMembersVariantInfo> variantInfos) {
        for (TwentyOneMembersVariantInfo v : variantInfos) {
            v.setUrl(null);

        }
        Collections.sort(variantInfos, new Comparator<TwentyOneMembersVariantInfo>() {

            @Override
            public int compare(TwentyOneMembersVariantInfo o1, TwentyOneMembersVariantInfo o2) {
                return new Integer(o2._getQuality()).compareTo(o1._getQuality());
            }
        });
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}