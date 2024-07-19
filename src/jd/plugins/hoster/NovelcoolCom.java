//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class NovelcoolCom extends PluginForHost {
    public NovelcoolCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private String             dllink                  = null;
    public static final String PROPERTY_BOOK_ID        = "book_id";
    public static final String PROPERTY_CHAPTER_ID     = "chapter_id";
    public static final String PROPERTY_SERIES_TITLE   = "series_title";
    public static final String PROPERTY_CHAPTER_NUMBER = "chapter_number";
    public static final String PROPERTY_PAGE_NUMBER    = "page_number";
    public static final String PROPERTY_PAGE_MAX       = "page_max";
    public static final String extDefault              = ".jpg";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "novelcool.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/chapter/[a-z\\-]+(\\d+(-\\d+)?)/(\\d+)-(\\d+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.novelcool.com/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String bookID = link.getStringProperty(PROPERTY_BOOK_ID);
        final String chapterID = link.getStringProperty(PROPERTY_CHAPTER_ID);
        final String pageNumber = link.getStringProperty(PROPERTY_PAGE_NUMBER);
        if (bookID != null && chapterID != null) {
            return "novelcool://book/" + bookID + "/chapter/" + chapterID + "/page/" + pageNumber;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        if (!link.isNameSet()) {
            link.setName(this.getLinkID(link) + extDefault);
        }
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        final String chapterNumber = urlinfo.getMatch(0);
        final String pageNumber = urlinfo.getMatch(3);
        link.setProperty(PROPERTY_CHAPTER_NUMBER, chapterNumber);
        link.setProperty(PROPERTY_PAGE_NUMBER, pageNumber);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String bookID = br.getRegex("cur_book_id = \"(\\d+)").getMatch(0);
        final String chapterID = br.getRegex("cur_chapter_id = \"(\\d+)").getMatch(0);
        link.setProperty(PROPERTY_BOOK_ID, bookID);
        link.setProperty(PROPERTY_CHAPTER_ID, chapterID);
        final String seriesTitle = findSeriesTitle(br);
        link.setProperty(PROPERTY_SERIES_TITLE, seriesTitle);
        dllink = br.getRegex("id=\"manga_picid_1\"[^>]*src=\"(https?://[^\"]+)").getMatch(0);
        link.setFinalFileName(formatFilename(link));
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, link.getFinalFileName(), null);
        }
        return AvailableStatus.TRUE;
    }

    public static String findSeriesTitle(final Browser br) {
        final String seriesTitle = br.getRegex("class=\"ifont-arrow-left ifont-white ifont-small\"></span>([^<]+)</a>").getMatch(0);
        if (seriesTitle != null) {
            return Encoding.htmlDecode(seriesTitle).trim();
        } else {
            return null;
        }
    }

    public static String formatFilename(final DownloadLink link) {
        final String seriesTitle = link.getStringProperty(PROPERTY_SERIES_TITLE);
        final String chapterNumber = link.getStringProperty(PROPERTY_CHAPTER_NUMBER);
        final String pageNumber = link.getStringProperty(PROPERTY_PAGE_NUMBER);
        String filename;
        if (seriesTitle != null) {
            link.setProperty(PROPERTY_SERIES_TITLE, seriesTitle);
            filename = seriesTitle + " - " + chapterNumber + " - " + pageNumber;
        } else {
            filename = chapterNumber + " - " + pageNumber;
        }
        filename += extDefault;
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}