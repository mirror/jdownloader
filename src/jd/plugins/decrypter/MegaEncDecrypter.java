package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mega.enc" }, urls = { "mega://f?enc\\d*\\?[a-zA-Z0-9-_]+" })
public class MegaEncDecrypter extends PluginForDecrypt {
    public MegaEncDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String MEGAENC_KEY  = "6B316F36416C2D316B7A3F217A30357958585858585858585858585858585858";
    private final String MEGAENC_KEY2 = "ed1f4c200b35139806b260563b3d3876f011b4750f3a1a4a5efd0bbe67554b44";
    private final String MEGAENC_IV   = "79F10A01844A0B27FF5B2D4E0ED3163E";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String MEGAENC_KEY;
        try {
            if (parameter.matches("mega://f?enc2\\?.+")) {
                MEGAENC_KEY = MegaEncDecrypter.this.MEGAENC_KEY2;
            } else {
                MEGAENC_KEY = MegaEncDecrypter.this.MEGAENC_KEY;
            }
        } catch (Throwable e) {
            logger.log(e);
            return decryptedLinks;
        }
        final boolean isFolder = parameter.contains("/fenc");
        String enc = new Regex(parameter, "enc\\d*\\?(.+)").getMatch(0);
        if (enc == null) {
            return null;
        }
        enc = enc.replaceAll("_", "/").replaceAll("-", "+");
        if (enc.length() % 4 != 0) {
            final int max = 4 - enc.length() % 4;
            for (int i = 0; i < max; i++) {
                enc += "=";
            }
        }
        final byte[] decrypted = decrypt(Base64.decode(enc), HexFormatter.hexToByteArray(MEGAENC_KEY), HexFormatter.hexToByteArray(MEGAENC_IV));
        final String dec = new String(decrypted, "UTF-8");
        final DownloadLink link;
        if (isFolder) {
            link = this.createDownloadlink("https://mega.co.nz/#F" + dec);
        } else {
            link = this.createDownloadlink("https://mega.co.nz/#" + dec);
        }
        decryptedLinks.add(link);
        return decryptedLinks;
    }

    private byte[] decrypt(byte[] cipher, byte[] key, byte[] iv) throws Exception {
        final PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        final CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        aes.init(false, ivAndKey);
        return cipherData(aes, cipher);
    }

    private byte[] cipherData(PaddedBufferedBlockCipher cipher, byte[] data) throws Exception {
        byte[] result = null;
        try {
            final int minSize = cipher.getOutputSize(data.length);
            final byte[] outBuf = new byte[minSize];
            final int length1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
            final int length2 = cipher.doFinal(outBuf, length1);
            final int actualLength = length1 + length2;
            result = new byte[actualLength];
            System.arraycopy(outBuf, 0, result, 0, result.length);
        } catch (Exception e) {
            // try catch used for breakpointing.
            throw e;
        }
        return result;
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}
