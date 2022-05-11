package org.jdownloader.container;

import java.io.File;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.controlling.linkcrawler.CrawledLink;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

public class AMZ extends PluginsC {
    public AMZ() {
        super("Amazon Mp3", "file:/.+\\.amz$", "$Revision$");
    }

    public AMZ newPluginInstance() {
        return new AMZ();
    }

    private final byte[] AMZ_IV  = new byte[] { (byte) 0x5E, 0x72, (byte) 0xD7, (byte) 0x9A, 0x11, (byte) 0xB3, 0x4F, (byte) 0xEE };
    private final byte[] AMZ_SEC = new byte[] { 0x29, (byte) 0xAB, (byte) 0x9D, 0x18, (byte) 0xB2, 0x44, (byte) 0x9E, 0x31 };

    @Override
    public ContainerStatus callDecryption(File file) {
        ContainerStatus cs = new ContainerStatus(file);
        String base64AMZ = JDIO.readFileToString(file);
        byte[] byteAMZ = Base64.decode(base64AMZ);
        /* google and you will find these keys public */
        cls = new ArrayList<CrawledLink>();
        final byte[] iv = AMZ_IV;
        final byte[] seckey = AMZ_SEC;
        SecretKey key = new SecretKeySpec(seckey, "DES");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        try {
            Cipher dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            byte[] decryptedbyteAMZ = dcipher.doFinal(byteAMZ);
            String decrytpedAMZ = new String(decryptedbyteAMZ, "UTF-8");
            String[][] tracks = new Regex(decrytpedAMZ, "<location>(http://.*?)</.*?<album>(.*?)</album>.*?<title>(.*?)</title>.*?<trackNum>(\\d+)<.*?fileSize\">(\\d+)<.*?trackType\">(.*?)</met").getMatches();
            java.util.List<DownloadLink> links = new ArrayList<DownloadLink>();
            HashMap<String, FilePackage> fps = new HashMap<String, FilePackage>();
            for (String track[] : tracks) {
                PluginForHost plg = JDUtilities.getPluginForHost("DirectHTTP");
                if (plg == null) {
                    continue;
                }
                String name = track[3] + "." + Encoding.htmlDecode(track[2]) + "." + track[5];
                DownloadLink link = new DownloadLink(plg, name, "DirectHTTP", Encoding.htmlOnlyDecode(track[0]), true);
                link.setAvailable(true);
                link.setDownloadSize(SizeFormatter.getSize(track[4]));
                /* add link to album package */
                if (fps.containsKey(track[1])) {
                    fps.get(track[1]).add(link);
                } else {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(track[1]);
                    fp.add(link);
                    fps.put(track[1], fp);
                }
                links.add(link);
                cls.add(new CrawledLink(link));
            }
            cs.setStatus(ContainerStatus.STATUS_FINISHED);
            return cs;
        } catch (Exception e) {
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            logger.log(e);
        }
        return null;
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }
}
