package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rpnet.biz" }, urls = { "https?://tor-.*?rpnet\\.biz/files/.*?/.+" })
public class RPNetBiz extends PluginForDecrypt {

    public RPNetBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final URL url = new URL(parameter.getCryptedUrl());
        final String fileName = getFileNameFromURL(url);
        final boolean isFolder = url.getPath().endsWith("$/");
        final URLConnectionAdapter con = br.openGetConnection(parameter.getCryptedUrl());
        try {
            if (con.getResponseCode() == 200 && (con.isContentDisposition() || (!isFolder && con.getLongContentLength() > 0 && StringUtils.isNotEmpty(fileName)))) {
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
                ret.addAll(getAllNodes(br, null, null));
            }
        } finally {
            con.disconnect();
        }

        return ret;
    }

    public static List<DownloadLink> getAllNodes(Browser br, final String filePackageName, String currentPath) throws Exception {
        final List<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (currentPath == null) {
            currentPath = "";
        }
        final boolean addPath = StringUtils.isNotEmpty(currentPath);
        final FilePackage filePackage;
        if (addPath && filePackageName != null) {
            filePackage = FilePackage.getInstance();
            filePackage.setName(filePackageName);
        } else {
            filePackage = null;
        }
        final String nodes[][] = br.getRegex("href=\"([^\"]*?)\"(?:\\s*title=.*?)?>([^<]*?)</a>\\s*</td>\\s*<td>([^<]*?)</td>").getMatches();
        for (final String node[] : nodes) {
            final String path = node[0];
            final String name = node[1];
            final String size = node[2];
            if (StringUtils.startsWithCaseInsensitive(path, "..")) {
                continue;
            }
            if (StringUtils.equals(size, "-")) {
                // folder
                final URL url = br.getURL(path);
                final String pathParts[] = url.getPath().split("/");
                final String pathName;
                if (pathParts.length > 0) {
                    pathName = Encoding.htmlDecode(pathParts[pathParts.length - 1]);
                } else {
                    pathName = name;
                }
                final Browser br2 = br.cloneBrowser();
                br2.getPage(path);
                ret.addAll(getAllNodes(br2, pathName, currentPath + "/" + CrossSystem.alleviatePathParts(pathName)));
            } else {
                // file
                final URL url = br.getURL(path);
                final DownloadLink link = new DownloadLink(null, null, null, "directhttp://" + url.toString(), true);
                if (size != null) {
                    link.setDownloadSize(SizeFormatter.getSize(size + "b"));
                }
                link.setFinalFileName(getFileNameFromURL(url));
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
