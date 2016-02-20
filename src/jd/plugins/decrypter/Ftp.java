package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.SimpleFTP;
import jd.nutils.SimpleFTP.SimpleFTPListEntry;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.auth.Login;

@DecrypterPlugin(revision = "$Revision: 32330$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.*?\\.[a-zA-Z0-9]{2,}(:\\d+)?/([^\"\r\n ]+|$)" }, flags = { 0 })
public class Ftp extends PluginForDecrypt {

    private static final HashMap<String, Integer> LOCKS = new HashMap<String, Integer>();

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cLink, ProgressController progress) throws Exception {
        final String lockHost = Browser.getHost(cLink.getCryptedUrl());
        final Integer lock;
        synchronized (LOCKS) {
            lock = LOCKS.get(lockHost);
        }
        if (lock == null) {
            return internalDecryptIt(cLink, progress, -1);
        } else {
            synchronized (lock) {
                return internalDecryptIt(cLink, progress, lock);
            }
        }
    }

    private ArrayList<DownloadLink> internalDecryptIt(CryptedLink cLink, ProgressController progress, final int maxFTPConnections) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(final DownloadLink link) {
                if (maxFTPConnections >= 1) {
                    link.setProperty("MAX_FTP_CONNECTIONS", maxFTPConnections);
                }
                return super.add(link);
            }
        };
        final SimpleFTP ftp = new SimpleFTP();
        ftp.setLogger(logger);
        try {
            final URL url = new URL(cLink.getCryptedUrl());
            try {
                ftp.connect(url);
            } catch (IOException e) {
                if (e.getMessage().contains("Sorry, the maximum number of clients") && maxFTPConnections == -1) {
                    final String lockHost = Browser.getHost(cLink.getCryptedUrl());
                    final String maxConnections = new Regex(e.getMessage(), "Sorry, the maximum number of clients \\((\\d+)\\)").getMatch(0);
                    synchronized (LOCKS) {
                        if (maxConnections != null) {
                            LOCKS.put(lockHost, Integer.parseInt(maxConnections));
                        } else {
                            LOCKS.put(lockHost, new Integer(1));
                        }
                    }
                    return decryptIt(cLink, progress);
                } else if (e.getMessage().contains("was unable to log in with the supplied") || e.getMessage().contains("530 Login or Password incorrect")) {
                    final DownloadLink dummyLink = new DownloadLink(null, null, url.getHost(), cLink.getCryptedUrl(), true);
                    final Login login = requestLogins(org.jdownloader.translate._JDT.T.DirectHTTP_getBasicAuth_message(), dummyLink);
                    if (login != null) {
                        final String host = url.getHost();
                        int port = url.getPort();
                        if (port <= 0) {
                            port = 21;
                        }
                        ftp.connect(host, port, login.getUsername(), login.getPassword());
                    }
                } else {
                    throw e;
                }
            }
            final String currentDir = ftp.getDir();
            String filePath = url.getPath();
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
            final String finalFilePath = filePath;
            String nameString = filePath.substring(filePath.lastIndexOf("/") + 1);
            final byte[] nameBytes;
            if (StringUtils.isEmpty(nameString)) {
                nameString = null;
                nameBytes = null;
            } else {
                nameBytes = SimpleFTP.toRawBytes(nameString);
            }
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            String packageName = new Regex(filePath, "/([^/]+)(/$|$)").getMatch(0);
            final String auth;
            if (!StringUtils.equals("anonymous", ftp.getUser()) || !StringUtils.equals("anonymous", ftp.getUser())) {
                auth = ftp.getUser() + ":" + ftp.getPass() + "@";
            } else {
                auth = "";
            }
            if (ftp.cwd(filePath)) {
                SimpleFTPListEntry[] entries = ftp.listEntries();
                if (entries != null) {
                    /*
                     * logic for only adding a given file, ie. ftp://domain/directory/file.exe, you could also have subdirectory of the same
                     * name ftp.../file.exe/file.exe -raztoki
                     */
                    for (final SimpleFTPListEntry entry : entries) {
                        // we compare bytes because of hex encoding
                        if (Arrays.equals(SimpleFTP.toRawBytes(entry.getName()), nameBytes)) {
                            if (entry.isFile()) {
                                final DownloadLink link = createDownloadlink("ftpviajd://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + entry.getFullPath());
                                link.setAvailable(true);
                                if (entry.getSize() >= 0) {
                                    link.setVerifiedFileSize(entry.getSize());
                                }
                                link.setFinalFileName(BestEncodingGuessingURLDecode(entry.getName()));
                                ret.add(link);
                                break;
                            } else {
                                if (ftp.cwd(entry.getName())) {
                                    entries = ftp.listEntries();
                                    break;
                                } else {
                                    entries = new SimpleFTPListEntry[0];
                                }
                            }
                        }
                    }
                    /*
                     * logic for complete directory! will not walk aka recursive! - raztoki
                     */
                    if (ret.isEmpty()) {
                        /*
                         * if 'name' == file then packagename == correct, ELSE if name != file then it should be packagename! -raztoki
                         */
                        if (nameString != null) {
                            packageName = nameString;
                        }
                        for (final SimpleFTPListEntry entry : entries) {
                            if (entry.isFile()) {
                                final DownloadLink link = createDownloadlink("ftpviajd://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + entry.getFullPath());
                                link.setAvailable(true);
                                if (entry.getSize() >= 0) {
                                    link.setVerifiedFileSize(entry.getSize());
                                }
                                link.setFinalFileName(BestEncodingGuessingURLDecode(entry.getName()));
                                ret.add(link);
                            }
                        }
                    }
                }
            }
            if (ret.size() == 0 && nameString != null) {
                // sometimes dir listing/changing is not allowed but direct access is still possible
                if (ftp.bin()) {
                    final long size = ftp.getSize(finalFilePath);
                    if (size >= 0) {
                        final DownloadLink link = createDownloadlink("ftpviajd://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + finalFilePath);
                        link.setAvailable(true);
                        link.setVerifiedFileSize(size);
                        link.setFinalFileName(BestEncodingGuessingURLDecode(nameString));
                        ret.add(link);
                    }
                }
            }
            if (ret.size() > 0 && packageName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(BestEncodingGuessingURLDecode(packageName));
                fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, Boolean.TRUE);
                fp.addLinks(ret);
            }
        } finally {
            ftp.disconnect();
        }
        return ret;
    }

    // very simple and dumb guessing for the correct encoding, checks for 'Replacement Character'
    public static String BestEncodingGuessingURLDecode(String urlCoded) throws IOException {
        final LinkedHashMap<String, String> results = new LinkedHashMap<String, String>();
        for (final String encoding : new String[] { "cp1251", "UTF-8", "ISO-8859-5", "KOI8-R" }) {
            try {
                results.put(encoding, URLDecoder.decode(urlCoded, encoding));
            } catch (final Throwable ignore) {
                ignore.printStackTrace();
            }
        }
        final List<String> bestMatchRound1 = new ArrayList<String>();
        int bestCountRound1 = -1;
        for (final Entry<String, String> result : results.entrySet()) {
            int count = 0;
            for (int index = 0; index < result.getValue().length(); index++) {
                if ('\uFFFD' == result.getValue().charAt(index)) {
                    count++;
                }
            }
            if (bestCountRound1 == -1 || bestCountRound1 == count) {
                bestCountRound1 = count;
                bestMatchRound1.add(result.getKey());
            } else {
                bestCountRound1 = count;
                bestMatchRound1.clear();
                bestMatchRound1.add(result.getKey());
            }
        }
        final List<String> bestMatches = new ArrayList<String>();
        for (final String bestMatchEncoding : bestMatchRound1) {
            bestMatches.add(results.get(bestMatchEncoding));
        }
        return bestMatches.get(0);
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }
}