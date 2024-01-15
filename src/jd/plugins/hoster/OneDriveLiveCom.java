//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.OneDriveLiveComCrawler;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onedrive.live.com" }, urls = { "" })
public class OneDriveLiveCom extends PluginForHost {
    public OneDriveLiveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://windows.microsoft.com/de-de/windows-live/microsoft-services-agreement";
    }

    public static final String PROPERTY_FILE_ID             = "plain_item_id";
    public static final String PROPERTY_CID                 = "plain_cid";
    public static final String PROPERTY_FOLDER_ID           = "plain_id";
    public static final String PROPERTY_PARENT_FOLDER_ID    = "parent_folder_id";
    public static final String PROPERTY_AUTHKEY             = "plain_authkey";
    public static final String PROPERTY_ACCOUNT_ONLY        = "account_only";
    public static final String PROPERTY_DIRECTURL           = "plain_download_url";
    public static final String PROPERTY_VIEW_IN_BROWSER_URL = "view_in_browser_url";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String file_id = link.getStringProperty(PROPERTY_FILE_ID);
        if (file_id != null) {
            return getHost() + "://" + file_id;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return super.getPluginContentURL(link);
        }
        final String viewInBrowserUrl = link.getStringProperty(PROPERTY_VIEW_IN_BROWSER_URL);
        if (viewInBrowserUrl != null) {
            /* Return pre-given URL. */
            return viewInBrowserUrl;
        }
        final String cid = link.getStringProperty(PROPERTY_CID);
        final String file_id = link.getStringProperty(PROPERTY_FILE_ID);
        if (cid != null && file_id != null) {
            /* Manually build URL */
            final String authkey = link.getStringProperty(PROPERTY_AUTHKEY);
            final UrlQuery query = new UrlQuery();
            query.add("cid", Encoding.urlEncode(cid));
            query.add("resid", Encoding.urlEncode(file_id));
            if (authkey != null) {
                query.add("authkey", Encoding.urlEncode(authkey));
            }
            final String parentFolderID = link.getStringProperty(PROPERTY_PARENT_FOLDER_ID);
            if (parentFolderID != null) {
                query.add("parId", Encoding.urlEncode(parentFolderID));
                query.add("o", "OneUp");
            }
            return "https://onedrive.live.com/redir.aspx?" + query.toString();
        }
        return super.getPluginContentURL(link);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final String fileID = link.getStringProperty(PROPERTY_FILE_ID);
        final String cid = link.getStringProperty(PROPERTY_CID);
        // final String folder_id = link.getStringProperty(PROPERTY_FOLDER_ID);
        // final String authkey = link.getStringProperty(PROPERTY_AUTHKEY);
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Origin", "https://onedrive.live.com");
        brc.getHeaders().put("Referer", "https://onedrive.live.com/");
        final String authkey = link.getStringProperty(PROPERTY_AUTHKEY);
        final UrlQuery query = new UrlQuery();
        if (authkey != null) {
            query.add("authkey", Encoding.urlEncode(authkey));
        }
        query.add("select", "id%2C%40content.downloadUrl");
        brc.getPage("https://api.onedrive.com/v1.0/drives/" + cid.toLowerCase(Locale.ENGLISH) + "/items/" + fileID + "?" + query.toString());
        if (brc.getHttpConnection().getResponseCode() == 404) {
            /*
             * E.g. {"error":{"code":"itemNotFound","message":"Item does not exist"
             * ,"localizedMessage":"Das Element fehlt scheinbar. Möglicherweise wurde das Element von einer anderen Person gelöscht oder verschoben, oder Sie besitzen keine Berechtigungen, um es anzuzeigen."
             * }}
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> errormap = (Map<String, Object>) entries.get("error");
        if (errormap != null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, errormap.get("localizedMessage").toString());
        }
        final String directurl = entries.get("@content.downloadUrl").toString();
        link.setProperty(PROPERTY_DIRECTURL, directurl);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final String dllink;
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            requestFileInformation(link);
            if (link.getBooleanProperty(PROPERTY_ACCOUNT_ONLY, false)) {
                throw new AccountRequiredException();
            }
            dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* This header is especially important for smaller files! See DirectHTTP Host Plugin. */
        br.getHeaders().put("Accept-Encoding", "identity");
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isResumeable(link, null), 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    /* Misc error-cases */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File temporarily unavailable?");
                }
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(PROPERTY_DIRECTURL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired?", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    @Deprecated
    private AvailableStatus requestFileInformation_Legacy(final DownloadLink link) throws Exception {
        // TODO: Remove this in 2024-05-01
        final String fileID = link.getStringProperty(PROPERTY_FILE_ID);
        final String cid = link.getStringProperty(PROPERTY_CID);
        final String id = link.getStringProperty(PROPERTY_FOLDER_ID);
        final String authkey = link.getStringProperty(PROPERTY_AUTHKEY);
        /* Legacy */
        JDUtilities.getPluginForDecrypt(this.getHost());
        OneDriveLiveComCrawler.prepBrAPILegacy(br);
        final int maxItems;
        int startIndex = link.getIntegerProperty("plain_item_si", -1);
        if (startIndex == -1) {
            startIndex = 0;
            maxItems = 1000;// backwards compatibility
        } else {
            maxItems = OneDriveLiveComCrawler.MAX_ENTRIES_PER_REQUEST_LEGACY;
        }
        String contenturl = link.getStringProperty("original_link");
        if (contenturl == null) {
            contenturl = this.getPluginContentURL(link);
        }
        final String additional_data;
        if (authkey != null) {
            additional_data = "&authkey=" + Encoding.urlEncode(authkey);
        } else {
            additional_data = "";
        }
        OneDriveLiveComCrawler.accessItems_API(br, contenturl, cid, id, additional_data, startIndex, maxItems);
        if (br.getRequest().getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 30 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":154")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object error = entries.get("error");
        if (error != null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error.toString());
        }
        final List<Object> ressourcelist = (List) entries.get("items");
        if (fileID != null) {
            for (final Object ressource : ressourcelist) {
                final String ret = findDownloadURL_Legacy((Map<String, Object>) ressource, fileID);
                if (ret != null) {
                    link.setProperty(PROPERTY_DIRECTURL, ret);
                    break;
                }
            }
        }
        final String filename = link.getStringProperty("plain_name");
        if (filename != null) {
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Deprecated
    private String findDownloadURL_Legacy(Map<String, Object> item, final String id) throws Exception {
        if (StringUtils.equals(id, (String) item.get("id"))) {
            final Map<String, Object> urls = (Map<String, Object>) item.get("urls");
            final String downloadURL = urls != null ? (String) urls.get("download") : null;
            if (downloadURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                return downloadURL;
            }
        }
        final Map<String, Object> folder = (Map<String, Object>) item.get("folder");
        if (folder != null) {
            final List<Map<String, Object>> children = (List<Map<String, Object>>) folder.get("children");
            if (children != null) {
                for (Map<String, Object> child : children) {
                    final String ret = findDownloadURL_Legacy(child, id);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
        }
        return null;
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