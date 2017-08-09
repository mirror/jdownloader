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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amazon.com" }, urls = { "https://amazondecrypted\\.com/\\d+" })
public class AmazonCloud extends PluginForHost {
    public AmazonCloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.amazon.de/gp/help/customer/display.html/ref=ap_footer_condition_of_use?ie=UTF8&nodeId=505048&pop-up=1";
    }

    public static final String   JSON_KIND_FILE     = "FILE";
    /* Connection stuff */
    private static final boolean FREE_RESUME        = true;
    private static final int     FREE_MAXCHUNKS     = 0;
    private static final int     FREE_MAXDOWNLOADS  = -1;
    /* Don't touch this! */
    public static int            max_items_per_page = 200;

    public AvailableStatus requestFileInformationOld(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String mainlink = link.getStringProperty("mainlink", null);
        final String plain_folder_id = link.getStringProperty("plain_folder_id");
        if (mainlink == null && plain_folder_id == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url = (mainlink != null && mainlink.contains("/gp/drive/share") ? mainlink : "https://www.amazon.com/clouddrive/share?s=" + plain_folder_id);
        getPage(br, url);
        if (br.containsHTML("=\"error_page\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("fileName = \"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = this.br.getRegex("class='file_name'>([^<>\"]*?)<").getMatch(0);
        }
        String filesize = br.getRegex("fSize = \"(\\d+)\"").getMatch(0);
        if (filesize == null) {
            filesize = this.br.getRegex("class=\"file_size\">([^<>\"]*?)<").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final AvailableStatus status;
        if (isOldType(link)) {
            status = requestFileInformationOld(link);
        } else {
            if (!link.getDownloadURL().matches("https://amazondecrypted\\.com/\\d+")) {
                /* Check if user still has VERY old links in his list --> Invalid */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.setBrowserExclusive();
            prepBR();
            final String plain_folder_id = link.getStringProperty("plain_folder_id", null);
            final String plain_domain = link.getStringProperty("plain_domain", null);
            getPage(br, "https://www." + plain_domain + "/drive/v1/shares/" + plain_folder_id + "?customerId=0&ContentType=JSON&asset=ALL");
            if (br.containsHTML("id=\"error_page\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String filename = link.getStringProperty("plain_name", null);
            final String filesize = link.getStringProperty("plain_size", null);
            link.setName(Encoding.htmlDecode(filename.trim()));
            link.setDownloadSize(Long.parseLong(filesize));
            status = AvailableStatus.TRUE;
        }
        return status;
    }

    private static void getPage(final Browser br, final String url) throws IOException, PluginException {
        br.getPage(url);
        if (br.getRequest().getHttpConnection().getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many Requests", 60 * 60 * 1000l);
        }
    }

    public void handleFreeOld(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformationOld(downloadLink);
        String dllink;
        if ("old20140922".equals(downloadLink.getStringProperty("type", null))) {
            /* Old url */
            dllink = this.br.getRegex("downloadUrl: encodeURI\\(\"(/[^<>\"]*?)\"\\)").getMatch(0);
        } else {
            /* New url */
            final String deviceserial = br.getRegex("sNum = \"([^<>\"]*?)\"").getMatch(0);
            if (deviceserial == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String domain = new Regex(br.getURL(), "(amazon\\.[a-z]+)/").getMatch(0);
            final String shareid = downloadLink.getStringProperty("plain_folder_id");
            final String getlink = "http://www." + domain + "/gp/drive/share/downloadFile.html?_=" + System.currentTimeMillis() + "&sharedId=" + Encoding.urlEncode(shareid) + "&download=TRUE&deviceType=ubid&deviceSerialNumber=" + deviceserial;
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage(br, getlink);
            dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        if (isOldType(link)) {
            handleFreeOld(link);
            return;
        }
        requestFileInformation(link);
        String dllink = link.getStringProperty("plain_directlink", null);
        boolean needs_new_directlink = false;
        if (dllink == null) {
            needs_new_directlink = true;
        } else {
            final URLConnectionAdapter con = this.br.openGetConnection(dllink);
            needs_new_directlink = !con.isOK() || con.getContentType().contains("html");
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (needs_new_directlink) {
            dllink = refreshDirectlink(link);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Store (new) directlink */
        link.setProperty("plain_directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private String refreshDirectlink(final DownloadLink dl) throws PluginException, IOException {
        LinkedHashMap<String, Object> entries = null;
        ArrayList<Object> resource_data_list = null;
        final String plain_domain = dl.getStringProperty("plain_domain", null);
        final String plain_folder_id = dl.getStringProperty("plain_folder_id", null);
        final String subfolder_id = dl.getStringProperty("subfolder_id", null);
        final String plain_name = dl.getStringProperty("plain_name", null);
        final String linkid_target = getLinkid(plain_folder_id, dl.getMD5Hash(), plain_name);
        String linkid_temp = null;
        String finallink = null;
        logger.info("Refreshing directlink");
        if (plain_folder_id == null) {
            /* Should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (subfolder_id != null) {
            /* Access subfolder/node */
            resource_data_list = getListFromNode(this.br, plain_domain, plain_folder_id, subfolder_id);
            if (isOffline(this.br)) {
                /* Offline check */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /* Access main folder */
            accessFolder(this.br, plain_domain, plain_folder_id);
            if (isOffline(this.br)) {
                /* Offline check */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            try {
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                final LinkedHashMap<String, Object> nodeInfo = jsonGetNodeInfo(entries);
                final String kind = jsonGetKind(entries);
                if (kind.equals(JSON_KIND_FILE)) {
                    resource_data_list = new ArrayList<Object>();
                    resource_data_list.add(nodeInfo);
                }
            } catch (final Throwable e) {
            }
        }
        try {
            for (final Object data_o : resource_data_list) {
                final LinkedHashMap<String, Object> nodeInfo = (LinkedHashMap<String, Object>) data_o;
                final LinkedHashMap<String, Object> contentProperties = jd.plugins.hoster.AmazonCloud.jsonGetContentProperties(nodeInfo);
                final String kind = jd.plugins.hoster.AmazonCloud.jsonGetKind(nodeInfo);
                if (!kind.equals(jd.plugins.hoster.AmazonCloud.JSON_KIND_FILE)) {
                    /* We want files (our file!), not folders! */
                    continue;
                }
                final String md5 = jd.plugins.hoster.AmazonCloud.jsonGetMd5(contentProperties);
                final String name_temp = jd.plugins.hoster.AmazonCloud.jsonGetName(nodeInfo);
                linkid_temp = getLinkid(plain_folder_id, md5, name_temp);
                if (linkid_temp.equals(linkid_target)) {
                    /* Yey we found our link - now we can finally refresh the directlink! */
                    finallink = jsonGetFinallink(nodeInfo);
                    break;
                }
            }
        } catch (final Throwable e) {
        }
        if (finallink == null) {
            logger.warning("Either something went terribly wrong or maybe the file we're trying to download is offline (or owner changed rights / filename / folder structure)");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: Failed to refresh final downloadurl");
        }
        return finallink;
    }

    public static String getLinkid(final String plain_folder_id, final String md5, final String name) {
        return plain_folder_id + "_" + md5 + "_" + name;
    }

    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, Object> jsonGetNodeInfo(final LinkedHashMap<String, Object> entries) {
        return (LinkedHashMap<String, Object>) entries.get("nodeInfo");
    }

    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, Object> jsonGetContentProperties(final LinkedHashMap<String, Object> entries) {
        return (LinkedHashMap<String, Object>) entries.get("contentProperties");
    }

    public static String jsonGetKind(final LinkedHashMap<String, Object> entries) {
        return (String) entries.get("kind");
    }

    public static String jsonGetMd5(final LinkedHashMap<String, Object> entries) {
        return (String) entries.get("md5");
    }

    public static String jsonGetName(final LinkedHashMap<String, Object> entries) {
        return (String) entries.get("name");
    }

    public static String jsonGetFinallink(final LinkedHashMap<String, Object> entries) {
        return (String) entries.get("tempLink");
    }

    public static void accessFolder(final Browser br, final String domain, final String plain_folder_id) throws IOException, PluginException {
        getPage(br, "https://www." + domain + "/drive/v1/shares/" + plain_folder_id + "?customerId=0&resourceVersion=V2&ContentType=JSON&asset=ALL");
    }

    /* Access nodes/subfolders. Does pagination if needed (e.g. more than max_items_per_page items)! */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public static ArrayList<Object> getListFromNode(final Browser br, final String domain, final String plain_folder_id, final String nodeid) throws IOException, PluginException {
        ArrayList<Object> resource_data_list_all = new ArrayList<Object>();
        ArrayList<Object> resource_data_list_tmp = null;
        LinkedHashMap<String, Object> entries_tmp = null;
        int numberof_found_items = 0;
        int offset = 0;
        do {
            numberof_found_items = 0;
            getPage(br, "https://www." + domain + "/drive/v1/nodes/" + nodeid + "/children?customerId=0&resourceVersion=V2&ContentType=JSON&offset=" + offset + "&limit=" + max_items_per_page + "&sort=%5B%22kind+DESC%22%2C+%22name+ASC%22%5D&tempLink=true&shareId=" + plain_folder_id);
            try {
                entries_tmp = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                resource_data_list_tmp = (ArrayList) entries_tmp.get("data");
                for (final Object fileo : resource_data_list_tmp) {
                    resource_data_list_all.add(fileo);
                    numberof_found_items++;
                    offset++;
                }
            } catch (final Throwable e) {
                break;
            }
        } while (numberof_found_items >= max_items_per_page);
        return resource_data_list_all;
    }

    public static boolean isOffline(final Browser br) {
        if (br.containsHTML("\"message\":\"ShareId does not exist") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
    }

    /** Check if we have the old- or new linktype. */
    private boolean isOldType(final DownloadLink dl) {
        return "old20140922".equals(dl.getStringProperty("type"));
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.addAllowedResponseCodes(429);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}