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

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.config.XFSConfigVideoVoeSx;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VoeSxCrawler extends PluginForDecrypt {
    public VoeSxCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "voe.sx", "voe-unblock.com", "voe-unblock.net", "voeunblock.com", "voeunblk.com", "voeunblck.com", "voe-un-block.com", "un-block-voe.net", "voeunbl0ck.com", "voeunblock1.com", "voeunblock2.com", "voeunblock3.com", "voeunblock4.com", "voeunblock5.com", "voeunblock6.com", "voeun-block.net", "v-o-e-unblock.com", "audaciousdefaulthouse.com", "launchreliantcleaverriver.com", "reputationsheriffkennethsand.com", "fittingcentermondaysunday.com", "housecardsummerbutton.com", "fraudclatterflyingcar.com", "bigclatterhomesguideservice.com", "uptodatefinishconferenceroom.com", "realfinanceblogcenter.com", "tinycat-voe-fashion.com", "20demidistance9elongations.com", "telyn610zoanthropy.com", "toxitabellaeatrebates306.com", "greaseball6eventual20.com", "745mingiestblissfully.com", "19turanosephantasia.com", "30sensualizeexpression.com", "321naturelikefurfuroid.com",
                "449unceremoniousnasoseptal.com", "cyamidpulverulence530.com", "boonlessbestselling244.com", "antecoxalbobbing1010.com", "matriculant401merited.com", "scatch176duplicities.com", "35volitantplimsoles5.com", "tummulerviolableness.com", "tubelessceliolymph.com", "availedsmallest.com", "counterclockwisejacky.com", "monorhinouscassaba.com", "tummulerviolableness.com", "urochsunloath.com", "simpulumlamerop.com", "wolfdyslectic.com", "metagnathtuggers.com", "gamoneinterrupted.com", "chromotypic.com", "crownmakermacaronicism.com", "generatesnitrosate.com", "yodelswartlike.com", "figeterpiazine.com", "cigarlessarefy.com", "valeronevijao.com", "apinchcaseation.com", "nectareousoverelate.com", "phenomenalityuniform.com", "nonesnanking.com", "troyyourlead.com", "stevenimaginelittle.com", "edwardarriveoften.com", "lukecomparetwo.com", "bradleyviewdoctor.com" });
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

    public static final String getDefaultAnnotationPatternPartVoeSx() {
        return "/(?:embed-|e/)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + VoeSxCrawler.getDefaultAnnotationPatternPartVoeSx());
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final XFSConfigVideoVoeSx cfg = PluginJsonConfig.get(XFSConfigVideoVoeSx.class);
        if (!cfg.isCrawlSubtitle()) {
            /* User does not want subtitle -> Let hosterplugin do the linkcheck as crawler handling is not needed. */
            ret.add(this.createDownloadlink(param.getCryptedUrl()));
            return ret;
        }
        final PluginForHost hosterPlugin = this.getNewPluginForHostInstance(this.getHost());
        final DownloadLink link = new DownloadLink(hosterPlugin, this.getHost(), param.getCryptedUrl(), true);
        hosterPlugin.setDownloadLink(link);
        final AvailableStatus status = hosterPlugin.requestFileInformation(link);
        link.setAvailableStatus(status);
        final String videoFilename = link.getName();
        final String packagename;
        if (videoFilename.contains(".")) {
            packagename = videoFilename.substring(0, videoFilename.lastIndexOf("."));
        } else {
            packagename = videoFilename;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(packagename);
        ret.add(link);
        String[] subtitleHTMLs = br.getRegex("<track kind=\"(captions|subtitles)\"[^<]+/>").getColumn(-1);
        if (subtitleHTMLs == null || subtitleHTMLs.length == 0) {
            /* 2023-09-25 */
            subtitleHTMLs = br.getRegex("<track[^<]+kind=\"(captions|subtitles)\"[^<]*/>").getColumn(-1);
        }
        if (subtitleHTMLs != null && subtitleHTMLs.length > 0) {
            for (final String subtitleHTML : subtitleHTMLs) {
                final String subtitleURL = new Regex(subtitleHTML, "src=\"([^\"]+\\.vtt)\"").getMatch(0);
                final URL subtitleURLFull = br.getURL(subtitleURL);
                final DownloadLink subtitle = createDownloadlink(subtitleURLFull.toString());
                if (subtitleHTMLs.length == 1) {
                    /* There is only one subtitle --> Set same title as video-file. */
                    subtitle.setFinalFileName(packagename + ".vtt");
                } else {
                    /* There are multiple subtitles available -> Set different filename for each */
                    subtitle.setFinalFileName(packagename + "_" + Plugin.getFileNameFromURL(subtitleURLFull));
                }
                subtitle.setAvailable(true);
                ret.add(subtitle);
            }
            logger.info("Found numberof subtitles: " + subtitleHTMLs.length);
        } else {
            logger.info("This item does not have any subtitles");
        }
        fp.addLinks(ret);
        return ret;
    }
}
