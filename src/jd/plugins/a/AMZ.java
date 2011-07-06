package jd.plugins.a;

import java.io.File;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.controlling.LinkGrabberController;
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

    public AMZ(PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public ContainerStatus callDecryption(File file) {
        ContainerStatus cs = new ContainerStatus(file);
        String base64AMZ = JDIO.readFileToString(file);
        byte[] byteAMZ = Base64.decode(base64AMZ);
        /* google and you will find these keys public */

        byte[] iv = null;
        byte[] seckey = null;
        try {
            iv = (byte[]) getClass().forName("jd.plugins.a.Config").getField("AMZ_IV").get(null);
            seckey = (byte[]) getClass().forName("jd.plugins.a.Config").getField("AMZ_SEC").get(null);

        } catch (Throwable e) {

        }

        SecretKey key = new SecretKeySpec(seckey, "DES");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        try {
            Cipher dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            byte[] decryptedbyteAMZ = dcipher.doFinal(byteAMZ);
            String decrytpedAMZ = new String(decryptedbyteAMZ);
            String[][] tracks = new Regex(decrytpedAMZ, "<location>(http://.*?)</.*?<album>(.*?)</album>.*?<title>(.*?)</title>.*?<trackNum>(\\d+)<.*?fileSize\">(\\d+)<.*?trackType\">(.*?)</met").getMatches();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            HashMap<String, FilePackage> fps = new HashMap<String, FilePackage>();
            for (String track[] : tracks) {
                PluginForHost plg = JDUtilities.getPluginForHost("DirectHTTP");
                if (plg == null) continue;
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
            }
            LinkGrabberController.getInstance().addLinks(links, false, false);
            cs.setStatus(ContainerStatus.STATUS_FINISHED);
            return cs;
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

    @Override
    public long getVersion() {
        return 1;
    }

}
