//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.a;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.crypt.AESencrypt;
import jd.crypt.Base16Decoder;
import jd.crypt.BaseDecoder.IllegalAlphabetException;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

public class R extends PluginsC {
    public R(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    public static String asHex(byte buf[]) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;
        strbuf.append("new byte[]{");
        for (i = 0; i < buf.length; i++) {
            strbuf.append("(byte) 0x");
            if ((buf[i] & 0xff) < 0x10) {
                strbuf.append("0");
            }

            strbuf.append(Long.toString(buf[i] & 0xff, 16));
            if (i < buf.length - 1) {
                strbuf.append(", ");
            }
        }
        strbuf.append("};");

        return strbuf.toString();
    }

    // @Override
    public ContainerStatus callDecryption(File lc) {
        ContainerStatus cs = new ContainerStatus(lc);
        // byte[] k = getKey();
        // logger.info(asHex(getKey()));
        // for( int i=0; i<k.length;i++){
        // k[i]+=i;
        // }
        // logger.info(asHex(k));
        // logger.info("Parse file: "+lc.getAbsolutePath());

        try {

            cls = new ArrayList<DownloadLink>();
            DownloadLink newLink;
            dlU = new ArrayList<String>();
            String fileContent[] = loadFileContent(lc.getAbsolutePath());
            int c = 0;
            // logger.info(fileContent.length+" links found");
            for (String element : fileContent) {
                // logger.info(i+" - "+fileContent[i]);
                if (element != null && element.length() > 0) {
                    PluginForHost pHost = findHostPlugin(element);
                    // logger.info("pHost: "+pHost);
                    if (pHost != null) {

                        newLink = new DownloadLink((PluginForHost) pHost.getWrapper().getPlugin(), element.substring(element.lastIndexOf("/") + 1), pHost.getHost(), null, true);
                        newLink.setLoadedPluginForContainer(this);
                        newLink.setContainerFile(lc.getAbsolutePath());
                        newLink.setContainerIndex(c);
                        cls.add(newLink);
                        dlU.add(element);
                        c++;
                    }
                }
            }
            logger.info("Links: " + cls);
            if (cls.size() > 0) {

                cs.setStatus(ContainerStatus.STATUS_FINISHED);
            } else {
                cs.setStatus(ContainerStatus.STATUS_FAILED);
            }
            return cs;

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            JDLogger.exception(e);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            JDLogger.exception(e);
        }
        cs.setStatus(ContainerStatus.STATUS_FAILED);
        return cs;

    }

    // private byte[] getDKey() {
    // try {
    // // byte[] k = new byte[] { (byte) 0x8C, (byte) 0x35, (byte) 0x19, (byte)
    // 0x2D, (byte) 0x96, (byte) 0x4D, (byte) 0xC3, (byte) 0x18, (byte) 0x2C,
    // (byte) 0x6F, (byte) 0x84, (byte) 0xF3, (byte) 0x25, (byte) 0x22, (byte)
    // 0x39, (byte) 0xEB, (byte) 0x4A, (byte) 0x32, (byte) 0x0D, (byte) 0x25,
    // (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    // byte[] k = new byte[] { (byte) 0x8C, (byte) 0x35, (byte) 0x19, (byte)
    // 0x2D, (byte) 0x96, (byte) 0x4D, (byte) 0xC3, (byte) 0x18, (byte) 0x2C,
    // (byte) 0x6F, (byte) 0x84, (byte) 0xF3, (byte) 0x25, (byte) 0x22, (byte)
    // 0x39, (byte) 0xEB, (byte) 0x4A, (byte) 0x32, (byte) 0x0D, (byte) 0x25,
    // (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    // getEKey(k);
    // // verschlüsselter Key
    // byte[] b = new byte[] { (byte) 0xbe, (byte) 0xd3, (byte) 0x89, (byte)
    // 0x4e, (byte) 0x93, (byte) 0x07, (byte) 0xb8, (byte) 0xd7, (byte) 0x37,
    // (byte) 0xa4, (byte) 0x6f, (byte) 0xfd, (byte) 0xb1, (byte) 0xdc, (byte)
    // 0xc4, (byte) 0x43, (byte) 0x79, (byte) 0x26, (byte) 0x6e, (byte) 0x0c,
    // (byte) 0x0e, (byte) 0x8f, (byte) 0x95, (byte) 0x11, (byte) 0x82, (byte)
    // 0x91, (byte) 0x3c, (byte) 0xc9, (byte) 0x80, (byte) 0xd4, (byte) 0x27,
    // (byte) 0x17 };
    // // Signatur hash auslesen
    // byte[] dsa = readJarSig();
    //
    // // Key decrypten
    // SecretKeySpec skeySpec = new SecretKeySpec(dsa, "AES");
    // // Instantiate the cipher
    //
    // Cipher cipher = Cipher.getInstance("AES");
    //
    // cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
    //
    // byte[] original = cipher.doFinal(k);
    // logger.info(asHex(original));
    // return original;
    // }
    // catch (Exception e) {
    // return null;
    // }
    // }

    private String[] decrypt(String rsdf) throws IOException, IllegalAlphabetException {
        String links = "";
        String input64 = new String(new Base16Decoder().decode(rsdf.toCharArray()));
        byte[] input, output;

        byte[] k = null;

        try {
            k = (byte[]) getClass().forName("jd.plugins.a.Config").getField("RSDF").get(null);

        } catch (Throwable e) {

        }

        getEKey(k);
        if (k == null) {
            logger.severe("RSDF Decryption failed.");
            return new String[0];
        }
        AESencrypt aes = new AESencrypt(k, 6);
        int i, j;
        byte[] temp = new byte[16];
        byte[] iv = null;
        try {
            iv = (byte[]) getClass().forName("jd.plugins.a.Config").getField("RSDFIV").get(null);

        } catch (Throwable e) {

        }

        String[] lines = Regex.getLines(input64);
        for (String line : lines) {
            input = Base64.decode(line);

            // byte[] k=null;

            output = new byte[input.length];
            for (i = 0; i < input.length; i++) {
                aes.Cipher(iv, temp);
                output[i] = (byte) (input[i] ^ temp[0]);
                for (j = 1; j < 16; j++) {
                    iv[j - 1] = iv[j];
                }
                iv[15] = input[i];
            }
            String str = new String(output);

            str = str.replaceAll("CCF\\:", " CCF: ");
            str = str.replaceAll("http:/", "\r\nhttp:/");
            links += str;

        }
        return HTMLParser.getHttpLinks(links, null);
    }

    // @Override
    public String[] encrypt(String plain) {

        return null;
    }

    private String filterRSDF(String rsdf) {
        // rsdf.split("\r")
        rsdf = rsdf.trim().toUpperCase();
        logger.info("RSDF length: " + rsdf.length() + "   ");
        String ret = "";
        for (int i = 0; i < rsdf.length(); i++) {
            // logger.info(new String(new char[] { rsdf.charAt(i) }));
            switch (rsdf.charAt(i)) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                // case '\r':
                // case '\n':
                ret += rsdf.charAt(i);
                break;
            default:
                if (i > 0) {
                    logger.info("iii" + i);

                    return ret;
                }

            }

        }
        return rsdf;
    }

    // @Override
    public String getCoder() {
        return "astaldo/JD-Team";
    }

    private void getEKey(byte[] key) {
        for (int i = 0; i < key.length; i++) {
            key[i] -= (byte) i;
        }

    }

    private String getLocalFile(File file) {
        BufferedReader f;
        StringBuffer buffer = new StringBuffer();
        try {
            f = new BufferedReader(new FileReader(file));

            String line;

            while ((line = f.readLine()) != null) {
                buffer.append(line + "\r\n");
            }
            f.close();
            return buffer.toString();
        } catch (IOException e) {

            JDLogger.exception(e);
        }
        return "";
    }

    // @Override
    public long getVersion() {
        return 1002;
    }

    private String[] loadFileContent(String filename) {

        String rsdf = filterRSDF(getLocalFile(new File(filename)).trim());

        if (rsdf == null) { return new String[] {}; }

        String[] links;
        try {
            links = decrypt(rsdf);

            return links;
        } catch (Exception e) {

            JDLogger.exception(e);
        }
        return null;
    }

}