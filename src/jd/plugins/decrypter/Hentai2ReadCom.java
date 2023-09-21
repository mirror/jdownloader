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
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hentai2read.com" }, urls = { "https?://(?:www\\.)?hentai2read.com/[a-z0-9]+(-|_)[a-z0-9\\-_]+(/\\d+)?" })
public class Hentai2ReadCom extends PluginForDecrypt {
    public Hentai2ReadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http:", "https:");
        final Regex urlRegex = new Regex(contenturl, "(?i)https?://[^/]+/(?!latest)([a-z0-9\\-_]+)(/(\\d+))?$");
        if (!urlRegex.patternFind()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String seriesNameSlug = urlRegex.getMatch(0);
        final String chapterNum = urlRegex.getMatch(2);
        if (chapterNum != null) {
            /* Crawl single chapter */
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">\\s*Sorry, this chapter is no longer available due to|>\\s*Sorry, this chapter is not available yet")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String seriesTitleCamelcase = StringUtils.toCamelCase(seriesNameSlug, true);
            final String fpName = seriesTitleCamelcase + " - Chapter " + chapterNum;
            final String jsonOld = br.getRegex("var wpm_mng_rdr_img_lst = \\[(.*?)\\]").getMatch(0);
            if (jsonOld != null) {
                final List<String> results = restoreFromString(jsonOld, TypeRef.STRING_LIST);
                for (final String pic : results) {
                    final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(pic));
                    dl.setAvailable(true);
                    ret.add(dl);
                }
            } else {
                // all links are now within json!
                String json = br.getRegex("var rff_imageList = (\\[.*?\\]);").getMatch(0);
                if (json == null) {
                    json = br.getRegex("'images'\\s*:\\s*(\\[.*?\\]),").getMatch(0);
                }
                final DecimalFormat df = new DecimalFormat("000");
                if (json != null) {
                    final List<String> results = restoreFromString(json, TypeRef.STRING_LIST);
                    String base = null;
                    for (int i = 0; i < results.size(); i++) {
                        final String result = results.get(i);
                        // for first one we need to decide base
                        if (base == null) {
                            base = br.getRegex("\"([^\"]+)" + Pattern.quote(result) + "[^\"]*\"").getMatch(0);
                            if (base == null) {
                                // better than returning null?
                                base = "//hentaicdn.com/hentai";
                            }
                        }
                        final String escaped = br.getURL(result).toExternalForm();
                        final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(escaped));
                        final String extension = getFileNameExtensionFromURL(escaped);
                        dl.setFinalFileName(seriesTitleCamelcase + "_CH" + chapterNum + "_" + df.format(i) + extension);
                        dl.setAvailable(true);
                        ret.add(dl);
                    }
                } else {
                    // old method
                    final Regex linkStructure = br.getRegex("(http://hentai2read\\.com/wp\\-content/[a-z0-9\\-_]+/\\d+/\\d+/p)\\d{3}\\.jpg");
                    final String lastpage = br.getRegex("value=\"(\\d+)\">\\d+</option></select></li><li>").getMatch(0);
                    if (linkStructure == null || lastpage == null) {
                        if (this.br.containsHTML("Chapter \\d+: Coming soon<")) {
                            logger.info("Unreleased content");
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    final String mainpart = linkStructure.getMatch(0);
                    for (int i = 1; i <= Integer.parseInt(lastpage); i++) {
                        final String finallink = "directhttp://" + mainpart + df.format(i) + ".jpg";
                        final DownloadLink dl = createDownloadlink(finallink);
                        dl.setFinalFileName(seriesTitleCamelcase + "_CH" + chapterNum + "_" + df.format(i) + ".jpg");
                        dl.setAvailable(true);
                        ret.add(dl);
                    }
                }
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(ret);
            }
        } else {
            /* Crawl all chapters of one series. */
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] relativeChapterURLs = br.getRegex("(/" + Pattern.quote(seriesNameSlug) + "/\\d+)").getColumn(0);
            if (relativeChapterURLs == null || relativeChapterURLs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String relativeChapterURL : relativeChapterURLs) {
                ret.add(this.createDownloadlink(br.getURL(relativeChapterURL).toExternalForm()));
            }
        }
        return ret;
    }
}
