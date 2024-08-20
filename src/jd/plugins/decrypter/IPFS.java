package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;

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
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ipfs.io", "ipfs.io" }, urls = { "https?://(cloudflare-ipfs.com|ipfs.io|ipfs.video|gateway.ipfs.io)/ipfs/[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+(\\?filename=.+|/.+)?", "ipfs://[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+(/.+)?" })
public class IPFS extends PluginForDecrypt {
    // https://developers.cloudflare.com/distributed-web/ipfs-gateway
    // https://docs.ipfs.io/concepts/ipfs-gateway/
    public IPFS(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String base58Pattern = "[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String cid = new Regex(parameter.getCryptedUrl(), "(?:/ipfs/|ipfs://)(" + base58Pattern + ")").getMatch(0);
        final String filename = URLEncode.decodeURIComponent(new Regex(parameter.getCryptedUrl(), "\\?filename=(.+)").getMatch(0));
        final String resourcePath = new Regex(parameter.getCryptedUrl(), "(?:/ipfs/|ipfs://)" + base58Pattern + "/(.+)").getMatch(0);
        String ipfsGateWay = StringUtils.startsWithCaseInsensitive(parameter.getCryptedUrl(), "ipfs://") ? "ipfs.io" : Browser.getHost(parameter.getCryptedUrl());
        if (StringUtils.equalsIgnoreCase(ipfsGateWay, "ipfs.video")) {
            ipfsGateWay = "ipfs.io";// or cloudflare-ipfs.com
        }
        final String probeURL;
        if (filename != null) {
            probeURL = "https://" + ipfsGateWay + "/ipfs/" + cid + "/";
        } else if (resourcePath != null) {
            probeURL = "https://" + ipfsGateWay + "/ipfs/" + cid + "/" + resourcePath;
        } else {
            probeURL = "https://" + ipfsGateWay + "/ipfs/" + cid;
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter con = br.openGetConnection(probeURL);
        if (this.looksLikeDownloadableContent(con)) {
            final DownloadLink singleFile = createDownloadlink(DirectHTTP.createURLForThisPlugin(probeURL));
            if (con.getCompleteContentLength() > 0) {
                singleFile.setVerifiedFileSize(con.getCompleteContentLength());
            }
            String name = filename;
            if (name == null) {
                if (resourcePath != null) {
                    name = getFileNameFromURL(br._getURL());
                }
                if (name == null) {
                    final String extension = getExtensionFromConnection(con);
                    name = this.applyFilenameExtension(cid, extension);
                }
            }
            if (name != null) {
                singleFile.setFinalFileName(name);
                singleFile.setProperty(DirectHTTP.FIXNAME, name);
            }
            singleFile.setAvailable(true);
            ret.add(singleFile);
            return ret;
        }
        br.followConnection();
        if (br.containsHTML("Index\\s*of\\s*/ipfs/.*?>\\s*" + cid + "\\s*<")) {
            final String folderTitle = br.getRegex("<title>([^<]+)</title>").getMatch(0);
            FilePackage fp = null;
            if (folderTitle != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(folderTitle).trim());
            }
            final String files[][] = br.getRegex("/ipfs/(" + base58Pattern + ")\\?filename=(.*?)\"").getMatches();
            DownloadLink foundMatch = null;
            for (final String[] file : files) {
                final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin("https://" + ipfsGateWay + "/ipfs/" + file[0] + "/"));
                final String name = URLEncode.decodeURIComponent(file[1]);
                link.setFinalFileName(Encoding.htmlDecode(name));
                link.setProperty(DirectHTTP.FIXNAME, name);
                link.setAvailable(true);
                ret.add(link);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                if (StringUtils.equals(filename, name)) {
                    foundMatch = link;
                    break;
                }
            }
            if (foundMatch != null) {
                ret.clear();
                ret.add(foundMatch);
            }
        } else {
            final DownloadLink downloadLink = createDownloadlink(DirectHTTP.createURLForThisPlugin(probeURL));
            String name = filename;
            if (name == null && resourcePath != null) {
                name = getFileNameFromURL(br._getURL());
            }
            if (name != null) {
                downloadLink.setFinalFileName(name);
                downloadLink.setProperty(DirectHTTP.FIXNAME, name);
            }
            downloadLink.setProperty(DirectHTTP.TRY_ALL, Boolean.TRUE);
            downloadLink.setAvailable(true);
            ret.add(downloadLink);
        }
        return ret;
    }
}
