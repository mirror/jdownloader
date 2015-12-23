package jd.plugins.decrypter;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.SimpleFTP;
import jd.nutils.SimpleFTP.SimpleFTPListEntry;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "noidea", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.*?\\.[a-zA-Z0-9]{2,}(:\\d+)?/([^\"\r\n ]+|$)" }, flags = { 0 })
public class Ftp extends PluginForDecrypt {

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
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
            if (ftp.cwd(filePath)) {
                final SimpleFTPListEntry[] entries = ftp.listEntries();
                if (entries != null) {
                    final String auth;
                    if (!StringUtils.equals("anonymous", ftp.getUser()) || !StringUtils.equals("anonymous", ftp.getUser())) {
                        auth = ftp.getUser() + ":" + ftp.getPass() + "@";
                    } else {
                        auth = "";
                    }
                    for (final SimpleFTPListEntry entry : entries) {
                        if (name != null) {
                            // specific file is requested
                            if (StringUtils.equals(entry.getName(), name) && entry.isFile()) {
                                final DownloadLink link = createDownloadlink("ftp://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + entry.getFullPath());
                                link.setAvailable(true);
                                if (entry.getSize() >= 0) {
                                    link.setVerifiedFileSize(entry.getSize());
                                }
                                link.setFinalFileName(entry.getName());
                                ret.add(link);
                                break;
                            }
                        } else {
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
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            ftp.disconnect();
        }
        return ret;
    }
}