package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@DecrypterPlugin(revision = "$Revision: 28474 $", interfaceVersion = 2, names = { "rpnet.biz" }, urls = { "https?://tor-.*?rpnet\\.biz/files/.*?/.+" })
public class RPNetBiz extends PluginForDecrypt {

    public RPNetBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String fileName = getFileNameFromURL(new URL(parameter.getCryptedUrl()));
        final URLConnectionAdapter con = br.openGetConnection(parameter.getCryptedUrl());
        try {
            if (con.getResponseCode() == 200 && (con.isContentDisposition() || (con.getLongContentLength() > 0 && StringUtils.isNotEmpty(fileName)))) {
                final DownloadLink link = new DownloadLink(null, null, null, "directhttp://" + parameter.getCryptedUrl(), true);
                if (con.getLongContentLength() > 0) {
                    link.setVerifiedFileSize(con.getLongContentLength());
                }
                ret.add(link);
            } else {
                br.followConnection();
                if (con.getResponseCode() == 403 || br.containsHTML("Access denied")) {
                    return ret;
                }
                ret.addAll(getAllNodes(br, null));
            }
        } finally {
            con.disconnect();
        }

        return ret;
    }

    public static List<DownloadLink> getAllNodes(Browser br, String currentPath) throws Exception {
        final List<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (currentPath == null) {
            currentPath = "";
        }
        final boolean addPath = StringUtils.isNotEmpty(currentPath);
        final FilePackage filePackage;
        if (addPath) {
            filePackage = FilePackage.getInstance();
            filePackage.setName(currentPath);
        } else {
            filePackage = null;
        }
        final String nodes[][] = br.getRegex("href=\"([^\"]*?)\">([^<]*?)</a>\\s*</td>\\s*<td>([^<]*?)</td>").getMatches();
        for (final String node[] : nodes) {
            final String path = node[0];
            final String name = node[1];
            final String size = node[2];
            if (StringUtils.startsWithCaseInsensitive(path, "..")) {
                continue;
            }
            if (StringUtils.equals(size, "-")) {
                // folder
                final Browser br2 = br.cloneBrowser();
                br2.getPage(path);
                ret.addAll(getAllNodes(br2, currentPath + "/" + CrossSystem.alleviatePathParts(name)));
            } else {
                // file
                final DownloadLink link = new DownloadLink(null, null, null, "directhttp://" + br.getURL(path).toString(), true);
                if (size != null) {
                    link.setDownloadSize(SizeFormatter.getSize(size + "b"));
                }
                link.setFinalFileName(name);
                if (filePackage != null) {
                    filePackage.add(link);
                }
                link.setAvailable(true);
                if (addPath) {
                    link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, CrossSystem.alleviatePathParts(currentPath));
                }
                ret.add(link);
            }
        }
        return ret;
    }

}
