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
 * By providing the original plugin regular expression(s) we do not have to rely on directhttp plugin for linkchecking, or surrounding issues with 'silent errors' within the linkgrabber if the file extension isn't matched against directhttp.
 * 
 * - raztoki
 */

/* Set interfaceVersion to 3 to avoid old Stable trying to load this Plugin */

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zeusupload.com", "filedino.com", "filedude.com", "uptal.org", "uptal.net", "file-bit.net", "zefile.com", "xtshare.com", "cosaupload.org", "sharing-online.com", "filestrack.com", "shareator.net", "azushare.net", "filecosy.com", "monsteruploads.eu", "vidhuge.com", "doneshare.com", "cixup.com", "animegoon.com", "supermov.com", "ufliq.com", "vidreel.com", "deditv.com", "supershare.net", "shareshared.com", "uploadville.com", "fileserver.cc", "bebasupload.com", "savefile.ro", "ovfile.com", "divxbase.com", "gptfile.com", "dudupload.com", "eyvx.com", "farshare.to", "azsharing.com", "freefilessharing.com", "elitedisk.com", "freakmov.com", "cloudnator.com", "filesavr.com", "saveufile.in.th", "migahost.com", "fastfreefilehosting.com", "files2k.eu", "shafiles.me", "jalurcepat.com", "divload.org", "refile.net", "oron.com",
        "wupload.com", "filesonic.com", "xxlupload.com", "cumfox.com", "pyramidfiles.com", "nahraj.cz", "jsharer.com", "annonhost.net", "filekeeper.org", "dynyoo.com", "163pan.com", "imagehost.org", "4us.to", "yabadaba.ru", "madshare.com", "diglo.com", "tubeload.to", "tunabox.net", "yourfilehost.com", "uploadegg.com", "brsbox.com", "amateurboobtube.com", "good.net", "freeload.to", "netporn.nl", "przeklej.pl", "alldrives.ge", "allshares.ge", "holderfile.com", "megashare.vnn.vn", "link.ge", "up.jeje.ge", "up-4.com", "cloudcache", "ddlanime.com", "mountfile.com", "platinshare.com", "ishare.iask.sina.com.cn", "megavideo.com", "megaupload.com", "cum.com", "zshare.net", "uploading4u.com", "megafree.kz", "batubia.com", "upload24.net", "files.namba.kz", "datumbit.com", "fik1.com", "fileape.com", "filezzz.com", "imagewaste.com", "fyels.com", "gotupload.com", "sharehub.com", "sharehut.com",
        "filesurf.ru", "openfile.ru", "letitfile.ru", "tab.net.ua", "uploadbox.com", "supashare.net", "usershare.net", "skipfile.com", "10upload.com", "x7.to", "multiupload.com", "uploadking.com", "uploadhere.com", "fileshaker.com", "vistaupload.com", "groovefile.com", "enterupload.com", "xshareware.com", "xun6.com", "yourupload.de", "youshare.eu", "mafiaupload.com", "addat.hu", "archiv.to", "bigupload.com", "biggerupload.com", "bitload.com", "bufiles.com", "cash-file.net", "combozip.com", "duckload.com", "exoshare.com", "file2upload.net", "filebase.to", "filebling.com", "filecrown.com", "filefrog.to", "filefront.com", "filehook.com", "filestage.to", "filezup.com", "fullshare.net", "gaiafile.com", "keepfile.com", "kewlshare.com", "lizshare.net", "loaded.it", "loadfiles.in", "megarapid.eu", "megashare.vn", "metahyper.com", "missupload.com", "netstorer.com", "nextgenvidz.com",
        "piggyshare.com", "profitupload.com", "quickload.to", "quickyshare.com", "share.cx", "sharehoster.de", "shareua.com", "speedload.to", "upfile.in", "ugotfile.com", "upload.ge", "uploadmachine.com", "uploady.to", "uploadstore.net", "vspace.cc", "web-share.net", "yvh.cc", "x-files.kz" }, urls = { "https?://(www\\.)?zeusupload\\.com/[a-z0-9]{12}", "https?://(www\\.)?filedino\\.com/[a-z0-9]{12}", "http://(www\\.)?(appscene\\.org|filedude\\.com)/(download/[0-9a-zA-Z]+|download\\.php\\?id=\\d+)", "http://(www\\.)?(new\\.)?uptal\\.(com|org)/\\?d=[A-Fa-f0-9]+", "http://(www\\.)?uptal\\.net/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?file-bit\\.net/[a-z0-9]{12}", "https?://(www\\.)?zefile\\.com/[a-z0-9]{12}", "http://(www\\.)?xtshare\\.com/toshare\\.php\\?Id=\\d+(\\&view=[0-9a-f]+)?",
        "http://(www\\.)?cosaupload\\.org/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es|de)/)?file/[0-9]+/)", "http://(www\\.)?sharing\\-online\\.com/[a-z0-9]{12}", "http://(www\\.)?filestrack\\.com/[a-z0-9]{12}", "http://(www\\.)?shareator\\.net/[0-9a-z]+", "http://(www\\.)?azushare\\.net/[A-Za-z0-9]+/", "https?://(www\\.)?filecosy\\.com/[a-z0-9]{12}", "https?://(www\\.)?monsteruploads\\.eu/[a-z0-9]{12}(/[^<>\"/]*?)?\\.html", "https?://(www\\.)?vidhuge\\.com/[a-z0-9]{12}", "http://(www\\.)?doneshare\\.com/files/details/[a-z0-9\\-]+\\.html", "http://(www\\.)?cixup\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?animegoon\\.com/[a-z0-9]{12}", "http://(www\\.)?supermov\\.com/(rc|video)/[0-9]+", "https?://(www\\.)?ufliq\\.com/[a-z0-9]{12}", "http://(www\\.)?vidreel\\.com/video/[0-9a-zA-Z]+/",
        "http://(www\\.)?deditv\\.com/(play|gate\\-way)\\.php\\?v=[0-9a-f]+", "http://(www\\.)?(hotshare|supershare)\\.net/(.+/)?(file|audio|video)/.+", "https?://(www\\.)?shareshared\\.com/[a-z0-9]{12}", "http://(www\\.)?uploadville\\.com/[a-z0-9]{12}", "http://(www\\.)?fileserver\\.cc/[a-z0-9]{12}", "http://(www\\.)?bebasupload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?savefile\\.ro/[\\w]+/?", "http://(www\\.)?ovfile\\.com/[a-z0-9]{12}", "https?://(www\\.)?divxbase\\.com/[a-z0-9]{12}", "https?://(www\\.)?gptfile\\.com/[a-z0-9]{12}", "https?://(www\\.)?dudupload\\.com/[a-z0-9]{12}", "http://(www\\.)?eyvx\\.com/[a-z0-9]{12}", "http://(www\\.)?farshare\\.to/[a-z0-9]{12}", "http://(www\\.)?azsharing\\.com/[a-z0-9]{12}/", "http://(www\\.)?freefilessharing\\.com/[a-z0-9]{12}", "https?://(www\\.)?elitedisk\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?freakmov\\.com/(rc|video)/[0-9]+",
        "http://[\\w\\.]*?(shragle|cloudnator)\\.(com|de)/files/[\\w]+/.*", "http://[\\w\\.]*?filesavr\\.com/[A-Za-z0-9]+(_\\d+)?", "http://(www\\.)?saveufile\\.(in\\.th|com)/car\\.php\\?file=[a-z0-9]+", "https?://(www\\.)?migahost\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?fastfreefilehosting\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "https?://(www\\.)?files2k\\.eu/[a-z0-9]{12}", "https?://(www\\.)?shafiles\\.me/[a-z0-9]{12}", "http://(www\\.)?jalurcepat\\.com/[a-z0-9]{12}", "https?://(www\\.)?(divload|divupload)\\.org/(embed\\-)?[a-z0-9]{12}", "http://(www\\.)?refile\\.net/(d|f)/\\?[\\w]+", "http://[\\w\\.]*?oron\\.com/[a-z0-9]{12}", "http://(www\\.)?wupload\\.[a-z]{1,5}/file/([0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?)", "http://[\\w\\.]*?(sharingmatrix|filesonic)\\..*?/.*?file/([a-zA-Z0-9]+(/.+)?|[a-z0-9]+/[0-9]+(/.+)?|[0-9]+(/.+)?)",
        "https?://(www\\.)?xxlupload\\.com/[a-z0-9]{12}", "http://(www\\.)?cumfox\\.com/videos/.*?-\\d+\\.html", "http://(www\\.)?pyramidfiles\\.com/[a-z0-9]{12}", "http://(www\\.)?nahraj\\.cz/content/(view|download)/[a-z0-9]+\\-[a-z0-9]+\\-[a-z0-9]+\\-[a-z0-9]+\\-[a-z0-9]+", "http://(www\\.)?jsharer\\.com/download/[a-z0-9]+\\.htm", "https?://(www\\.)?annonhost\\.net/[a-z0-9]{12}", "http://(www\\.)?filekeeper\\.org/download/[0-9a-zA-Z]+/([\\(\\)0-9A-Za-z\\.\\-_% ]+|[/]+/[\\(\\)0-9A-Za-z\\.\\-_% ])", "http://(www\\.)?dynyoo\\.com/\\?goto=dl\\&id=[a-z0-9]{32}", "http://[\\w\\.]*?163pan\\.com/files/[a-z0-9]+\\.html", "http://[\\w\\.]*?imagehost\\.org/(download/[0-9]+/.+|[0-9]+/.+)", "http://[\\w\\.]*?4us\\.to/download\\.php\\?id=[A-Z0-9]+", "http://[\\w\\.]*?yabadaba\\.ru/files/[0-9]+", "http://(www\\.)?madshare\\.com/(en/)?download/[a-zA-Z0-9]+/",
        "http://(www\\.)?diglo\\.com/download/[a-z0-9]+", "http://(www\\.)?tubeload\\.to/file(\\d+)?\\-.+", "http://(www\\.)?tunabox\\.net/files/[A-Za-z0-9]+\\.html", "http://[\\w\\.]*?yourfilehost\\.com/media\\.php\\?cat=.*?\\&file=.+", "https?://(www\\.)?uploadegg\\.com/[a-z0-9]{12}", "http://(www\\.)?brsbox\\.com/filebox/down/fc/[a-z0-9]{32}", "http://(www\\.)?amateurboobtube\\.com/videos/\\d+/.*?\\.html", "http://(www\\.)?good\\.net/.+", "http://(www\\.)*?(freeload|mcload)\\.to/(divx\\.php\\?file_id=|\\?Mod=Divx\\&Hash=)[a-z0-9]+", "http://(www\\.)?netporn\\.nl/watch/[a-z0-9]+/.{1}", "http://(www\\.)?przeklej\\.pl/(d/\\w+/|\\d+|plik/)[^\\s]+", "http://(www\\.)?alldrives\\.ge/main/linkform\\.php\\?f=[a-z0-9]+", "http://(www\\.)?allshares\\.ge/(\\?d|download\\.php\\?id)=[A-Z0-9]+", "https?://(www\\.)?holderfile\\.com/[a-z0-9]{12}",
        "http://(www\\.)?megashare\\.vnn\\.vn/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?link\\.ge/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?up\\.jeje\\.ge//((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?up\\-4\\.com/(\\?d|download\\.php\\?id)=[A-Z0-9]+", "https?://(www\\.)?cloudcache\\.cc/[a-z0-9]{12}", "https?://(www\\.)?(ddlanime\\.com|ddlani\\.me)/[a-z0-9]{12}", "http://(www\\.)?mountfile\\.com/file/[a-z0-9]+/[a-z0-9]+", "http://(www\\.)?platinshare\\.com/files/[A-Za-z0-9]+", "http://(www\\.)?ishare\\.iask\\.sina\\.com\\.cn/f/\\d+\\.html", "http://(www\\.)?megavideo\\.com/(.*?(v|d)=|v/)[a-zA-Z0-9]+", "http://(www\\.)?megaupload\\.com/.*?(\\?|&)d=[0-9A-Za-z]+", "http://(www\\.)?(cum|megaporn|megarotic|sexuploader)\\.com/(.*?v=|v/)[a-zA-Z0-9]+",
        "http://(www\\.)?zshare\\.net/(download|video|image|audio|flash)/.*", "http://(www\\.)?uploading4u\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?megafree\\.kz/file\\d+", "http://(www\\.)?batubia\\.com/[a-z0-9]{12}", "http://(www\\.)?upload24\\.net/[a-z0-9]+\\.[a-z0-9]+", "http://(www\\.)?download\\.files\\.namba\\.kz/files/\\d+", "http://(www\\.)?datumbit\\.com/file/.*?/", "http://(www\\.)?fik1\\.com/[a-z0-9]{12}", "http://(www\\.)?fileape\\.com/(index\\.php\\?act=download\\&id=|dl/)\\w+", "http://(www\\.)?filezzz\\.com/download/[0-9]+/", "http://(www\\.)?imagewaste\\.com/pictures/\\d+/.{1}", "http://(www\\.)?fyels\\.com/[A-Za-z0-9]+", "http://(www\\.)?gotupload\\.com/[a-z0-9]{12}", "http://(go.sharehub.com|sharehub.me|follow.to|kgt.com|krt.com)/.*", "http://(www\\.)?sharehut\\.com/[a-z0-9]{12}",
        "http://[\\w\\.]*?(filesurf|4ppl|files\\.youmama)\\.ru/[0-9]+", "http://[\\w\\.]*?openfile\\.ru/[0-9]+", "http://[\\w\\.]*?letitfile\\.(ru|com)/download/id\\d+", "http://[\\w\\.]*?tab\\.net\\.ua/sites/files/site_name\\..*?/id\\.\\d+/", "http://[\\w\\.]*?uploadbox\\.com/.*?files/[0-9a-zA-Z]+", "http://(www\\.)?supashare\\.net/[a-z0-9]{12}", "https?://(www\\.)?usershare\\.net/[a-z0-9]{12}", "http://(www\\.)?skipfile\\.com/[a-z0-9]{12}", "http://(www\\.)?10upload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?x7\\.to/(?!list)[a-zA-Z0-9]+(/(?!inList)[^/\r\n]+)?", "http://(www\\.)?multiuploaddecrypted\\.com/([A-Z0-9]{2}_[A-Z0-9]+|[0-9A-Z]+)", "http://(www\\.)?uploadking\\.com/[A-Z0-9]+", "http://(www\\.)?uploadhere\\.com/[A-Z0-9]+", "http://[\\w\\.]*?fileshaker\\.com/.+", "http://(www\\.)?vistaupload\\.com/[a-z0-9]{12}", "https?://(www\\.)?groovefile\\.com/[a-z0-9]{12}",
        "http://[\\w\\.]*?enterupload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?xshareware\\.com/[\\w]+/.*", "http://[\\w\\.]*?xun6\\.(com|net)/file/[a-z0-9]+", "http://(www\\.)?yourupload\\.de/[a-z0-9]{12}", "http://(www\\.)?youshare\\.eu/[a-z0-9]{12}", "http://(www\\.)?mafiaupload\\.com/do\\.php\\?id=\\d+", "http://[\\w\\.]*?addat.hu/.+/.+", "http://(www\\.)?archiv\\.to/((\\?Module\\=Details\\&HashID\\=|GET/)FILE[A-Z0-9]+|view/divx/[a-z0-9]+)", "http://[\\w\\.]*?bigupload\\.com/(d=|files/)[A-Z0-9]+", "http://(www\\.)?biggerupload\\.com/[a-z0-9]{12}", "http://(www\\.)?(bitload\\.com/(f|d)/\\d+/[a-z0-9]+|mystream\\.to/file-\\d+-[a-z0-9]+)", "https?://(www\\.)?bufiles\\.com/[a-z0-9]{12}", "http://(www\\.)?cash-file\\.(com|net)/[a-z0-9]{12}", "http://[\\w\\.]*?combozip\\.com/[a-z0-9]{12}",
        "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/(download/[a-z0-9]+|(divx|play)/[A-Z0-9\\.-]+|[a-zA-Z0-9\\.]+)", "http://(www\\.)?exoshare\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?file2upload\\.(net|com)/download/[0-9]+/", "http://[\\w\\.]*?filebase\\.to/(files|download)/\\d{1,}/.*", "http://[\\w\\.]*?filebling\\.com/[a-z0-9]{12}", "http://(www\\.)?filecrown\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?filefrog\\.to/download/\\d+/[a-zA-Z0-9]+", "http://[\\w\\.]*?filefront\\.com/[0-9]+", "http://(www\\.)?filehook\\.com/[a-z0-9]{12}", "http://(www\\.)?filestage\\.to/watch/[a-z0-9]+/", "http://(www\\.)?(filezup|divxupfile)\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?fullshare\\.net/show/[a-z0-9]+/.+", "http://(www\\.)?gaiafile\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?keepfile\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?kewlshare\\.com/dl/[\\w]+/", "http://[\\w\\.]*?lizshare\\.net/[a-z0-9]{12}",
        "http://(www\\.)?loaded\\.it/(show/[a-z0-9]+/[A-Za-z0-9_\\-% \\.]+|(flash|divx)/[a-z0-9]+/)", "http://[\\w\\.]*?loadfiles\\.in/[a-z0-9]{12}", "(http://[\\w\\.]*?megarapid\\.eu/files/\\d+/.+)|(http://[\\w\\.]*?megarapid\\.eu/\\?e=403\\&m=captcha\\&file=\\d+/.+)", "http://[\\w\\.]*?(megashare\\.vn/(download\\.php\\?uid=[0-9]+\\&id=[0-9]+|dl\\.php/\\d+)|share\\.megaplus\\.vn/dl\\.php/\\d+)", "http://(www\\.)?metahyper\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?missupload\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?netstorer\\.com/[a-zA-Z0-9]+/.+", "http://[\\w\\.]*?nextgenvidz\\.com/view/\\d+", "http://(www\\.)?piggyshare\\.com/file/[a-z0-9]+", "http://(www\\.)?profitupload\\.com/files/[A-Za-z0-9]+\\.html", "http://[\\w\\.]*?quickload\\.to/\\?Go=Player\\&HashID=FILE[A-Z0-9]+", "http://[\\w\\.]*?quickyshare\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?share\\.cx/(files/)?\\d+",
        "http://[\\w\\.]*?sharehoster\\.(de|com|net)/(dl|wait|vid)/[a-z0-9]+", "http://[\\w\\.]*?shareua.com/get_file/.*?/\\d+", "http://[\\w\\.]*?speedload\\.to/FILE[A-Z0-9]+", "http://(www\\.)?upfile\\.in/[a-z0-9]{12}", "http://[\\w\\.]*?ugotfile.com/file/\\d+/.+", "http://[\\w\\.]*?upload\\.ge/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://[\\w\\.]*?uploadmachine\\.com/(download\\.php\\?id=[0-9]+&type=[0-9]{1}|file/[0-9]+/)", "http://[\\w\\.]*?uploady\\.to/dl/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)", "http://(www\\.)?uploadstore\\.net/[a-z0-9]{12}", "http://[\\w\\.]*?vspace\\.cc/file/[A-Z0-9]+\\.html", "http://[\\w\\.]*?web-share\\.net/download/file/item/.*?_[0-9]+", "http://(www\\.)?yvh\\.cc/video\\.php\\?file=[a-z0-9_]+", "http://[\\w\\.]*?x-files\\.kz/[a-z0-9]+" }, flags = { 0 })
public class Offline extends PluginForHost {

    public Offline(PluginWrapper wrapper) {
        super(wrapper);
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

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permanently Offline: Host provider no longer exists");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}