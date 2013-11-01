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
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "https?://(www\\.)?save\\.tv/STV/M/obj/user/usShowVideoArchive\\.cfm(\\?iPageNumber=\\d+)?" }, flags = { 0 })
public class SaveTvDecrypter extends PluginForDecrypt {

    public SaveTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String GRABARCHIVE        = "GRABARCHIVE";
    private static final String GRABARCHIVE_FASTER = "GRABARCHIVE_FASTER";

    private final String        CONTAINSPAGE       = "https?://(www\\.)?save\\.tv/STV/M/obj/user/usShowVideoArchive\\.cfm\\?iPageNumber=\\d+";

    // TODO: Find a better solution than "param3=string:984899" -> Maybe try to use API if it has a function to get the whole archive
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final SubConfiguration cfg = SubConfiguration.getConfig("save.tv");
        if (!cfg.getBooleanProperty(GRABARCHIVE, false)) {
            logger.info("Decrypting save.tv archives is disabled, doing nothing...");
            return decryptedLinks;
        }
        if (!getUserLogin(false)) {
            logger.info("Failed to decrypt link because account is missing: " + parameter);
            return decryptedLinks;
        }
        final boolean fastLinkcheck = cfg.getBooleanProperty(GRABARCHIVE_FASTER, false);
        br.getPage(parameter);
        int maxPage = 1;
        if (!parameter.matches(CONTAINSPAGE)) {
            final String[] pages = br.getRegex("PageNumber=(\\d+)\\&bLoadLast=1\"").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String page : pages) {
                    final int currentpage = Integer.parseInt(page);
                    if (currentpage > maxPage) maxPage = currentpage;
                }
            }
        }
        final DecimalFormat df = new DecimalFormat("0000");
        final DecimalFormat df2 = new DecimalFormat("0000000000000");
        final String one = df.format(new Random().nextInt(10000));
        final String two = df2.format(new Random().nextInt(1000000000));
        for (int i = 1; i <= maxPage; i++) {
            logger.info("Decrypting page " + i + " of " + maxPage);
            int addedlinksnum = 0;
            if (i > 1) {
                br.getPage("https://www.save.tv/STV/M/obj/user/usShowVideoArchive.cfm?iPageNumber=" + i + "&bLoadLast=1");
            }
            final String[][] directSeriesLinks = br.getRegex("(\\d+)\" class=\"child\">([^<>\"]*?)</a>[\t\n\r ]+\\-(.*?)(\r|\t|\n]+)").getMatches();
            if (directSeriesLinks != null && directSeriesLinks.length != 0) {
                for (final String[] directserieslinkinfo : directSeriesLinks) {
                    final String telecastID = directserieslinkinfo[0];
                    final String seriesName = Encoding.htmlDecode(directserieslinkinfo[1].trim());
                    final String episodeTitle = Encoding.htmlDecode(directserieslinkinfo[2].trim());
                    final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(seriesName));
                    fp.addLinks(decryptedLinks);
                    dl.setFinalFileName(seriesName + " - " + episodeTitle + ".mp4");
                    dl._setFilePackage(fp);
                    if (fastLinkcheck) dl.setAvailable(true);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    decryptedLinks.add(dl);
                    addedlinksnum++;
                }
            }

            final String[][] directMovieLinks = br.getRegex("(\\d+)\" class=\"normal\">([^<>\"]*?)</a>[\t\n\r ]+\\-(.*?)(\r|\t|\n]+)").getMatches();
            if (directMovieLinks != null && directMovieLinks.length != 0) {
                for (final String[] directmovieslinkinfo : directMovieLinks) {
                    final String telecastID = directmovieslinkinfo[0];
                    final String seriesName = Encoding.htmlDecode(directmovieslinkinfo[1].trim());
                    final String episodeTitle = Encoding.htmlDecode(directmovieslinkinfo[2].trim());
                    final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(seriesName));
                    fp.addLinks(decryptedLinks);
                    dl.setFinalFileName(seriesName + " - " + episodeTitle + ".mp4");
                    dl._setFilePackage(fp);
                    if (fastLinkcheck) dl.setAvailable(true);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    decryptedLinks.add(dl);
                    addedlinksnum++;
                }
            }
            final String[][] dlInfo = br.getRegex("data\\-rownumber=\"(\\d+)\", data\\-title=\"([^<>\"]*?)\"").getMatches();
            if (dlInfo != null && dlInfo.length != 0) {
                for (final String[] dInfo : dlInfo) {
                    try {
                        if (this.isAbort()) {
                            logger.info("Decrypt process aborted by user: " + parameter);
                            return decryptedLinks;
                        }
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    final String dlid = dInfo[0];
                    final String dlname = dInfo[1];
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(dlname));
                    fp.addLinks(decryptedLinks);
                    try {
                        br.postPage("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveLoadEntries.cfm?null.GetVideoEntries", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetVideoEntries&c0-id=" + one + "_" + two + "&c0-param0=string:1&c0-param1=string:&c0-param2=string:1&c0-param3=string:984899&c0-param4=string:1&c0-param5=string:0&c0-param6=string:1&c0-param7=string:0&c0-param8=string:1&c0-param9=string:&c0-param10=string:" + Encoding.urlEncode(dlname) + "&c0-param11=string:" + dlid + "&c0-param12=string:toggleSerial&xml=true&extend=function (object) for (property in object) { this[property] = object[property]; } return this;}&");
                    } catch (final BrowserException e) {
                        logger.warning("Plugin broken for link: " + parameter);
                        logger.warning("Stopped at page " + i + " of " + maxPage);
                        return null;
                    }
                    br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                    final String[][] epinfos = br.getRegex("TelecastID=(\\d+)\" class=\"normal\">([^<>\"]*?)</a> \\- ([^<>\"]*?)</td>").getMatches();
                    if (epinfos == null || epinfos.length == 0) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        logger.warning("Stopped at page " + i + " of " + maxPage);
                        return null;
                    }
                    for (final String[] episodeinfo : epinfos) {
                        final String telecastID = episodeinfo[0];
                        final String seriesName = Encoding.htmlDecode(episodeinfo[1].trim());
                        final String episodeTitle = Encoding.htmlDecode(episodeinfo[2].trim());
                        final DownloadLink dl = createDownloadlink("https://www.save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=" + telecastID);
                        dl.setFinalFileName(seriesName + " - " + episodeTitle + ".mp4");
                        dl._setFilePackage(fp);
                        if (fastLinkcheck) dl.setAvailable(true);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            // Not available in old 0.9.581 Stable
                        }
                        decryptedLinks.add(dl);
                        addedlinksnum++;
                    }
                }
            }
            if (addedlinksnum == 0 && decryptedLinks.size() == 0) {
                logger.warning("Plugin broken for link: " + parameter);
                return null;
            } else if (addedlinksnum == 0) {
                logger.info("Can't find more links, stopping at page: " + i + " of " + maxPage);
                break;
            }
            logger.info("Found " + addedlinksnum + " links on page " + i + " of " + maxPage);
        }

        return decryptedLinks;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("save.tv");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) { return false; }
        try {
            ((jd.plugins.hoster.SaveTv) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        return true;
    }

}
