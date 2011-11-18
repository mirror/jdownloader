//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/*
 * The idea behind this is to speed up linkchecking for host providers that go permanently offline. URLs tend to stay cached/archived on the intrawebs longer than host provider.
 * By providing the original plugin regular expression(s) we do not have to rely on directhttp plugin for linkchecking, or surrounding issues with 'silent errors' within the linkgrabber, if the file extension isn't matched against directhttp.
 * 
 * - raztoki
 */
/*Set interfaceVersion to 3 to avoid old Stable trying to load this Plugin*/
@HostPlugin(revision = "$Revision: 15297 $", interfaceVersion = 3, names = { "addat.hu", "archiv.to", "bigupload.com", "combozip.com", "duckload.com", "exoshare.com", "file2upload.net", "filebase.to", "filebling.com", "filestage.to", "keepfile.com", "kewlshare.com", "lizshare.net", "loaded.it", "megashare.vn", "metahyper.com", "missupload.com", "netstorer.com", "nextgenvidz.com", "piggyshare.com", "profitupload.com", "quickload.to", "quickyshare.com", "share.cx", "sharehoster.de", "shareua.com", "speedload.to", "upload.ge", "uploadmachine.com", "uploady.to", "vspace.cc", "web-share.net", "yvh.cc", "x-files.kz", "bufiles.com" }, urls = { "http://[\\w\\.]*?addat.hu/.+/.+", "http://(www\\.)?archiv\\.to/((\\?Module\\=Details\\&HashID\\=|GET/)FILE[A-Z0-9]+|view/divx/[a-z0-9]+)", "http://[\\w\\.]*?bigupload\\.com/(d=|files/)[A-Z0-9]+", "http://[\\w\\.]*?combozip\\.com/[a-z0-9]{12}",
        "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/(download/[a-z0-9]+|(divx|play)/[A-Z0-9\\.-]+|[a-zA-Z0-9\\.]+)", "http://(www\\.)?exoshare\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?file2upload\\.(net|com)/download/[0-9]+/", "http://[\\w\\.]*?filebase\\.to/(files|download)/\\d{1,}/.*", "http://[\\w\\.]*?filebling\\.com/[a-z0-9]{12}", "http://(www\\.)?filestage\\.to/watch/[a-z0-9]+/", "http://[\\w\\.]*?keepfile\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?kewlshare\\.com/dl/[\\w]+/", "http://[\\w\\.]*?lizshare\\.net/[a-z0-9]{12}", "http://(www\\.)?loaded\\.it/(show/[a-z0-9]+/[A-Za-z0-9_\\-% \\.]+|(flash|divx)/[a-z0-9]+/)", "http://[\\w\\.]*?(megashare\\.vn/(download\\.php\\?uid=[0-9]+\\&id=[0-9]+|dl\\.php/\\d+)|share\\.megaplus\\.vn/dl\\.php/\\d+)", "http://(www\\.)?metahyper\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?missupload\\.com/[a-z0-9]{12}",
        "http://[\\w\\.]*?netstorer\\.com/[a-zA-Z0-9]+/.+", "http://[\\w\\.]*?nextgenvidz\\.com/view/\\d+", "http://(www\\.)?piggyshare\\.com/file/[a-z0-9]+", "http://(www\\.)?profitupload\\.com/files/[A-Za-z0-9]+\\.html", "http://[\\w\\.]*?quickload\\.to/\\?Go=Player\\&HashID=FILE[A-Z0-9]+", "http://[\\w\\.]*?quickyshare\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?share\\.cx/(files/)?\\d+", "http://[\\w\\.]*?sharehoster\\.(de|com|net)/(dl|wait|vid)/[a-z0-9]+", "http://[\\w\\.]*?shareua.com/get_file/.*?/\\d+", "http://[\\w\\.]*?speedload\\.to/FILE[A-Z0-9]+", "http://[\\w\\.]*?upload\\.ge/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://[\\w\\.]*?uploadmachine\\.com/(download\\.php\\?id=[0-9]+&type=[0-9]{1}|file/[0-9]+/)", "http://[\\w\\.]*?uploady\\.to/dl/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)",
        "http://[\\w\\.]*?vspace\\.cc/file/[A-Z0-9]+\\.html", "http://[\\w\\.]*?web-share\\.net/download/file/item/.*?_[0-9]+", "http://(www\\.)?yvh\\.cc/video\\.php\\?file=[a-z0-9_]+", "http://[\\w\\.]*?x-files\\.kz/[a-z0-9]+", "https?://(www\\.)?bufiles\\.com/[a-z0-9]{12}" }, flags = { 0 })
public class Offline extends PluginForHost {

    public Offline(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls != null) {
            for (DownloadLink link : urls) {
                link.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                link.getLinkStatus().setErrorMessage("Permanently Offline: Host provider no longer exists");
                link.setAvailable(false);
            }
        }
        return true;
    }

}