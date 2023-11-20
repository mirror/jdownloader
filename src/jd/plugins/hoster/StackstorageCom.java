//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.encoding.URLEncode;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stackstorage.com" }, urls = { "https?://(?:www\\.)?stackstorage\\.com/fileid/(\\d+)" })
public class StackstorageCom extends PluginForHost {
    public StackstorageCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String PROPERTY_SHARETOKEN = "stackstorage_sharetoken";
    public static final String PROPERTY_CSRFTOKEN  = "stackstorage_csrftoken";
    // public static final String PROPERTY_FILE_ID = "fileid";
    public static final String PROPERTY_FILENAME   = "stackstorage_filename";

    @Override
    public String getAGBLink() {
        return "https://www.transip.nl/stack/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fileid = getFileID(link);
        if (fileid != null) {
            return "stackstorage://file/" + fileid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFileID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final boolean useNewHandling2023 = true;
        if (useNewHandling2023) {
            return AvailableStatus.UNCHECKABLE;
        } else {
            br.getPage(link.getContainerUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Relevant information has been set in crawler */
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String subdomain = new Regex(link.getContainerUrl(), "(?i)https?://([a-z0-9]+)\\.").getMatch(0);
        final String folderid = new Regex(link.getContainerUrl(), "(?i)/s/([A-Za-z0-9]+)").getMatch(0);
        // TODO: 2023-11-20: Add handling to refresh csrftoken and sharetoken
        final String sharetoken = link.getStringProperty(PROPERTY_SHARETOKEN);
        final String csrftoken = link.getStringProperty(PROPERTY_CSRFTOKEN);
        final boolean useNewHandling2023 = true;
        if (useNewHandling2023) {
            final String fileid = this.getFileID(link);
            final String filename = link.getStringProperty(PROPERTY_FILENAME);
            if (sharetoken == null || csrftoken == null || filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie(this.getHost(), "ShareSession", sharetoken);
            // br.getHeaders().put("X-CSRF-Token", csrftoken);
            final String directurl = "https://" + subdomain + ".stackstorage.com/api/v2/share/" + folderid + "/files/" + fileid + "/download/" + Encoding.urlEncode(filename) + "?contentDisposition=1&CSRF-Token=" + csrftoken;
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, true, 1);
        } else {
            /* Old handling */
            final String path = link.getStringProperty("download_path");
            if (csrftoken == null || path == null || folderid == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Upgrade-Insecure-Requests", "1");
            br.getHeaders().put("Origin", "https://" + subdomain + "." + this.getHost() + "/");
            final Form dlform = new Form();
            dlform.setMethod(MethodType.POST);
            dlform.setAction(String.format("https://%s.stackstorage.com/public-share/%s/download", subdomain, folderid));
            /*
             * This must be the type of archive you get if you want to download multiple files as archive - for single files, this is not
             * relevant and default = 'zip'.
             */
            dlform.put("archive", "zip");
            dlform.put("all", "false");
            dlform.put("query", "");
            dlform.put("CSRF-Token", csrftoken);
            dlform.put(URLEncode.encodeURIComponent("paths[]"), Encoding.urlEncode(path));
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, true, 1);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}