package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "oceanus.ch" }, urls = { "http://oceanus.ch/u/[0-9a-zA-Z\\-]*/[0-9a-zA-Z\\-]*" }, flags = { 0 })
public class Oceanus extends PluginForDecrypt {
    private final String ENCODING_DICTIONARY = "ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghjkmnopqrstuvwxyz023456789";
    private final int    ENCODING_SIZE_64    = (int) Math.ceil(64 / (Math.log(ENCODING_DICTIONARY.length()) / Math.log(2))); // 11

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        PluginForHost oceanus = JDUtilities.getNewPluginForHostInstance("oceanus.ch");
        oceanus.setLogger(getLogger());
        oceanus.setBrowser(getBrowser());

        Long uploadID = getUploadId(parameter.getCryptedUrl());
        if (uploadID != null) {
            String downloadXML = ((jd.plugins.hoster.Oceanus) oceanus).sendDownloadRequest(uploadID);
            if (((jd.plugins.hoster.Oceanus) oceanus).DOWNLOAD_INVALID.equals(downloadXML)) { return ret; }
            if (!StringUtils.isEmpty(downloadXML)) {
                jd.plugins.hoster.Oceanus.Downloader downloader = ((jd.plugins.hoster.Oceanus) oceanus).parseXML(downloadXML);
                FilePackage fp = FilePackage.getInstance();
                for (jd.plugins.hoster.Oceanus.OCFile file : downloader.getFileList()) {
                    DownloadLink link = createDownloadlink("oceanus://" + uploadID + "," + file.getFileId() + "," + getDecryptionKey(parameter.getCryptedUrl()));
                    link.setFinalFileName(file.getName());
                    link.setVerifiedFileSize(file.getSize());
                    link.setAvailable(true);
                    ret.add(link);
                }
                if (ret.size() > 1) {
                    fp.setName("Oceanus Folder");
                    fp.addLinks(ret);
                }
                return ret;
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private String getDecryptionKey(String oceanusLink) throws Exception {
        String idString = oceanusLink.split("/u/")[1];
        if (!StringUtils.isEmpty(idString)) { return idString.split("/")[1]; }
        return null;
    }

    /**
     * get uploadID from the link
     * 
     * @param oceanusLink
     *            - the oceanus download link
     * @return uploadID
     * @throws Exception
     */
    public Long getUploadId(String uploadLink) throws Exception {
        String idString = uploadLink.split("/u/")[1];
        String encodedUploadID = null;
        if (!StringUtils.isEmpty(idString)) {
            if (!StringUtils.isEmpty(idString.split("/")[1])) {
                encodedUploadID = idString.split("/")[0];
                if (!StringUtils.isEmpty(encodedUploadID)) {
                    // Decode the uploadID
                    return decode64(encodedUploadID);
                }
            }
        }
        return null;
    }

    /**
     * decodes the encoded uploadID into long.
     * 
     * @param toDecode
     * @return long
     * @throws Exception
     */
    private long decode64(String toDecode) throws Exception {
        if (toDecode.length() > ENCODING_SIZE_64) throw new Exception("Passed string '" + toDecode + "' has " + "an invalid length.");
        long ret = 0l;
        int skip = ENCODING_SIZE_64 - toDecode.length();
        for (int i = 0; i < ENCODING_SIZE_64; i++) {
            ret *= ENCODING_DICTIONARY.length();
            if (i < skip) continue;
            int idx = ENCODING_DICTIONARY.indexOf(toDecode.charAt(i - skip));
            if (idx == -1) throw new Exception("Passed string '" + toDecode + "' contains illegal " + "characters");
            ret += idx;
        }
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}