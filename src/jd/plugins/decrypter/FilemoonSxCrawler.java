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

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.config.XFSConfigVideoFilemoonSx;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { FilemoonSxCrawler.class })
public class FilemoonSxCrawler extends PluginForDecrypt {
    public FilemoonSxCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "filemoon.sx" });
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

    public static final String getDefaultAnnotationPatternPartFilemoon() {
        return "/(?:e|d)/[a-z0-9]+(/[^/]+)?";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + FilemoonSxCrawler.getDefaultAnnotationPatternPartFilemoon());
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
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
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(link);
        final XFSConfigVideoFilemoonSx cfg = PluginJsonConfig.get(XFSConfigVideoFilemoonSx.class);
        if (cfg.isCrawlSubtitle()) {
            /* Look for subtitles if user wants them. */
            try {
                final String cryptedScripts[] = br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                if (cryptedScripts != null && cryptedScripts.length != 0) {
                    for (String s : cryptedScripts) {
                        String decoded = null;
                        try {
                            Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
                            String p = params.getMatch(0).replaceAll("\\\\", "");
                            int a = Integer.parseInt(params.getMatch(1));
                            int c = Integer.parseInt(params.getMatch(2));
                            String[] k = params.getMatch(3).split("\\|");
                            while (c != 0) {
                                c--;
                                if (k[c].length() != 0) {
                                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                                }
                            }
                            decoded = p;
                        } catch (Exception e) {
                            logger.log(e);
                        }
                        final String subtitleJS = new Regex(decoded, "tracks\\s*:\\s*(\\[\\{[^\\]]+\\])").getMatch(0);
                        final List<Map<String, Object>> tracks = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(subtitleJS);
                        final List<Map<String, Object>> subtitles = new ArrayList<Map<String, Object>>();
                        for (final Map<String, Object> track : tracks) {
                            final String trackType = track.get("kind").toString();
                            if (!trackType.equalsIgnoreCase("captions")) {
                                /* Skip e.g. "thumbnails" */
                                continue;
                            }
                            subtitles.add(track);
                        }
                        for (final Map<String, Object> subtitleInfo : subtitles) {
                            final String subtitleURL = subtitleInfo.get("file").toString();
                            // final String subtitleLanguage = subtitleInfo.get("label").toString();
                            final URL subtitleURLFull = br.getURL(subtitleURL);
                            final DownloadLink subtitle = createDownloadlink(subtitleURLFull.toString());
                            if (subtitles.size() == 1) {
                                /* There is only one subtitle --> Set same title as video-file. */
                                subtitle.setFinalFileName(packagename + ".vtt");
                            } else {
                                /* There are multiple subtitles available -> Set different name for each */
                                subtitle.setFinalFileName(packagename + "_" + Plugin.getFileNameFromURL(subtitleURLFull));
                            }
                            subtitle.setAvailable(true);
                            ret.add(subtitle);
                        }
                    }
                }
            } catch (final Throwable ignore) {
            }
        }
        fp.addLinks(ret);
        return ret;
    }
}
