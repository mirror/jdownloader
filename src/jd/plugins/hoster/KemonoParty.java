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
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.KemonoPartyCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
/** Helper plugin to download pre crawled text. */
public class KemonoParty extends PluginForHost {
    public KemonoParty(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String PROPERTY_TITLE              = "title";
    public static String PROPERTY_TEXT               = "text";
    public static String PROPERTY_PORTAL             = "portal";
    public static String PROPERTY_USERID             = "userid";
    public static String PROPERTY_POSTID             = "postid";
    public static String PROPERTY_DATE               = "date";
    public static String PROPERTY_POST_CONTENT_INDEX = "postContentIndex";

    @Override
    public String getAGBLink() {
        return "https://kemono.party/";
    }

    private static List<String[]> getPluginDomains() {
        return KemonoPartyCrawler.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([^/]+)/user/([^/]+)/post/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        final String portal = link.getStringProperty(PROPERTY_PORTAL);
        final String userid = link.getStringProperty(PROPERTY_USERID);
        final String postid = link.getStringProperty(PROPERTY_POSTID);
        if (portal != null && userid != null && postid != null) {
            return portal + "_" + userid + "_" + postid;
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link) + ".txt");
        }
        final String textContent = link.getStringProperty(PROPERTY_TEXT);
        if (StringUtils.isEmpty(textContent)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        try {
            link.setDownloadSize(textContent.getBytes("UTF-8").length);
        } catch (final UnsupportedEncodingException ignore) {
            ignore.printStackTrace();
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        /* Write text to file */
        final File dest = new File(link.getFileOutput());
        IO.writeToFile(dest, link.getStringProperty(PROPERTY_TEXT).getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
        /* Set filesize so user can see it in UI. */
        link.setVerifiedFileSize(dest.length());
        /* Set progress to finished - the "download" is complete. */
        link.getLinkStatus().setStatus(LinkStatus.FINISHED);
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}