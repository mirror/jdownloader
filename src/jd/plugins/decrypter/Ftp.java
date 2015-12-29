package jd.plugins.decrypter;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.SimpleFTP;
import jd.nutils.SimpleFTP.SimpleFTPListEntry;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision: 32330$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.*?\\.[a-zA-Z0-9]{2,}(:\\d+)?/([^\"\r\n ]+|$)" }, flags = { 0 })
public class Ftp extends PluginForDecrypt {

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CrawledLink link) throws Exception {
        final CrawledLink source = link.getSourceLink();
        if (source.getDownloadLink() != null && canHandle(source.getURL())) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(source.getDownloadLink());
            return ret;
        }
        return super.decryptIt(link);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cLink, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SimpleFTP ftp = new SimpleFTP();
        ftp.setLogger(logger);
        try {
            final URL url = new URL(cLink.getCryptedUrl());
            ftp.connect(url);
            final String currentDir = ftp.getDir();
            String filePath = URLDecoder.decode(url.getPath(), "UTF-8");
            if (!filePath.startsWith("/")) {
                filePath = "/" + filePath;
            }
            if (!filePath.startsWith(currentDir)) {
                if (currentDir.endsWith("/")) {
                    filePath = currentDir + filePath.substring(1);
                } else {
                    filePath = currentDir + filePath;
                }
            }
            String name = filePath.substring(filePath.lastIndexOf("/") + 1);
            if (StringUtils.isEmpty(name)) {
                name = null;
            }
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            String packageName = new Regex(filePath, "/([^/]+)(/$|$)").getMatch(0);
            if (ftp.cwd(filePath)) {
                final SimpleFTPListEntry[] entries = ftp.listEntries();
                if (entries != null) {
                    final String auth;
                    if (!StringUtils.equals("anonymous", ftp.getUser()) || !StringUtils.equals("anonymous", ftp.getUser())) {
                        auth = ftp.getUser() + ":" + ftp.getPass() + "@";
                    } else {
                        auth = "";
                    }
                    /*
                     * logic for only adding a given file, ie. ftp://domain/directory/file.exe, you could also have subdirectory of the same
                     * name ftp.../file.exe/file.exe -raztoki
                     */
                    for (final SimpleFTPListEntry entry : entries) {
                        if (entry.isFile() && StringUtils.equals(entry.getName(), name)) {
                            final DownloadLink link = createDownloadlink("ftp://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + entry.getFullPath());
                            link.setAvailable(true);
                            if (entry.getSize() >= 0) {
                                link.setVerifiedFileSize(entry.getSize());
                            }
                            link.setFinalFileName(entry.getName());
                            ret.add(link);
                        }
                    }
                    /*
                     * logic for complete directory! will not walk aka recursive! - raztoki
                     */
                    if (ret.isEmpty()) {
                        /*
                         * if 'name' == file then packagename == correct, ELSE if name != file then it should be packagename! -raztoki
                         */
                        if (name != null) {
                            packageName = name;
                        }
                        for (final SimpleFTPListEntry entry : entries) {
                            if (entry.isFile()) {
                                final DownloadLink link = createDownloadlink("ftp://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + entry.getFullPath());
                                link.setAvailable(true);
                                if (entry.getSize() >= 0) {
                                    link.setVerifiedFileSize(entry.getSize());
                                }
                                link.setFinalFileName(entry.getName());
                                ret.add(link);
                            }
                        }
                    }
                }
            }
            if (name != null && ret.size() == 0) {
                /*
                 * wrong, folder could be empty, or contain sub directories this shouldn't be a defect! -raztoki
                 */
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (packageName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(packageName);
                fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, Boolean.TRUE);
                fp.addLinks(ret);
            }
        } finally {
            ftp.disconnect();
        }
        return ret;
    }

}