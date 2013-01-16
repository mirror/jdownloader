package org.jdownloader.container.sft;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.Inflater;

public class sftBinary {

    public static final int    MAX_SIZE    = 1024 * 1024 * 8;
    public static final int    HEADER_SIZE = 0x40;
    public static final byte[] MAGIC       = "\0001F99569B3ZA94Q13A5DBKDKDTTISSSTFFBV5345HFUFHFGHKHKKGFJHHESASOOSIWOA5DB771GFGDFCTSDFF264RFFJKWJWR799CILDSL894DPOMCYWWR334343NG48DHRFQYL569POH11SUJR4334RFCTSDFF264RFFHS677HUZT567MNVX99CILDSLFCTSDFF264RFF894DPOMCYWWR3343OWBV4584JHHDGJFCCCSEWQ967HDHFGRKKFSDFJ".getBytes();

    public static sftContainer load(File file) throws Exception {
        if (file.length() > MAX_SIZE) throw new Exception("file size to big");

        InputStream is = new FileInputStream(file);
        byte[] rawFileData = new byte[(int) file.length()];
        is.read(rawFileData);
        is.close();

        String sftMagic = new String(Arrays.copyOfRange(rawFileData, 3, 6));
        sftContainer container = null;
        if (sftMagic.equals("SFT")) {
            int sftVersion = Integer.parseInt(new String(Arrays.copyOfRange(rawFileData, 7, 8)));
            switch (sftVersion) {
            case 4:
                container = sft04_decrypt(rawFileData);
                break;
            case 8:
                container = sft08_decrypt(rawFileData);
                break;
            default:
                throw new Exception("unsupported sft version: v" + Integer.toHexString(sftVersion));
            }
        } else
            throw new Exception("unknown file format");

        return container;
    }

    protected static sftContainer sft04_decrypt(byte[] rawFileData) throws Exception {
        // decompress data
        byte[] body = new byte[rawFileData.length * 10];

        Inflater decompresser = new Inflater();
        decompresser.setInput(rawFileData, HEADER_SIZE, rawFileData.length - HEADER_SIZE);
        int decompressedLength = decompresser.inflate(body);
        body = Arrays.copyOf(body, decompressedLength);

        // get sha1 key from header
        byte[] header = Arrays.copyOfRange(rawFileData, 1, HEADER_SIZE + 2);
        header[header.length - 1] = header[header.length - 2] = -1;
        byte[] sha1_key = MessageDigest.getInstance("SHA-1").digest(header);

        // encode the crypted content
        RC4 rc4 = new RC4(sha1_key);
        for (int dpos = 0; dpos < body.length; dpos += 0x2000) {
            int end = dpos + 0x2000;
            if (end > body.length) end = body.length;

            byte[] body_block = Arrays.copyOfRange(body, dpos, end);
            rc4.encode(body_block);

            for (int i = dpos; i < end; ++i)
                body[i] = body_block[i - dpos];
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(body);
        DataInputStream ois = new DataInputStream(bis);

        DelphiFormBinLoader dfm = new DelphiFormBinLoader(ois);

        return new sftContainerV4(dfm);
    }

    protected static sftContainer sft08_decrypt(byte[] rawFileData) throws Exception {
        byte[] header = Arrays.copyOf(rawFileData, HEADER_SIZE);
        byte[] body = Arrays.copyOfRange(rawFileData, HEADER_SIZE, rawFileData.length);
        byte[] magicDecryptionKey = sft08_getMagicDecryptionKeyFromHeader(header);

        RC4 rc4 = new RC4(Arrays.copyOf(magicDecryptionKey, 128));
        for (int dpos = 0; dpos < body.length; dpos += 0x2000) {
            int end = dpos + 0x2000;
            if (end > body.length) end = body.length;

            byte[] body_block = Arrays.copyOfRange(body, dpos, end);
            rc4.encode(body_block);

            for (int i = dpos; i < end; ++i)
                body[i] = body_block[i - dpos];
        }

        body = sft08_lmaaDudeDecrypt(body);

        ByteArrayInputStream bis = new ByteArrayInputStream(body);
        DataInputStream ois = new DataInputStream(bis);

        DelphiFormBinLoader dfm = new DelphiFormBinLoader(ois);

        return new sftContainerV8(dfm, magicDecryptionKey);
    }

    protected static byte[] sft08_getMagicDecryptionKeyFromHeader(byte[] header) {
        byte[] magicDecryptionKey = new byte[192];

        byte[] output1 = header.clone();
        byte[] output2 = header.clone();
        byte[] output3 = header.clone();

        sft08_keyRunExec(1, output1);
        sft08_keyRunExec(2, output2);
        sft08_keyRunExec(3, output3);

        int i = 0, resultIndex = 0;
        do {
            magicDecryptionKey[resultIndex] = (byte) (output1[i + 1] | 1);
            magicDecryptionKey[resultIndex + 1] = (byte) (output3[i + 1] | 3);
            magicDecryptionKey[resultIndex + 2] = (byte) (output2[i + 1] | 1);

            if ((i & 1) == 0)
                magicDecryptionKey[resultIndex + 3] = (byte) ((output1[i + 1] ^ output3[i + 1]) | 1);
            else
                magicDecryptionKey[resultIndex + 3] = (byte) (((output2[i + 1] & output3[i + 1]) | output1[i + 1]) | 3);

            resultIndex += 4;
            i++;
        } while (i != 0x30);

        return magicDecryptionKey;
    }

    protected static void sft08_keyRunExec(int i, byte[] header) {
        final int constMagicValue1 = 0x08;

        byte[] sftFileBackup = header.clone();
        sftFileBackup[0] = 0;
        header[0] = 0;

        int BL = 2;
        int a = (int) header[constMagicValue1] & 0xFF;
        byte result1 = (byte) (header[((int) (header[a % 0x33] & 0xFF) + a) % (constMagicValue1 + 0x20)] ^ header[constMagicValue1]);

        header[1] = result1;

        int e = 0, f = 0, g = 0, special = 0, AL = 0, DL = 0, result = 0;
        switch (i) {
        case 1: {
            do {
                e = ((((int) (header[1] & 0xFF) | constMagicValue1) % 0x1F) + constMagicValue1) & 0xFF;
                e = (e + MAGIC[(int) (header[1] & 0xFF)] % constMagicValue1) & 0xFF;
                f = sftFileBackup[(int) ((int) (header[BL - 1] & 0xFF) % e)] & 0xFF;
                g = ((((int) (header[1] & 0xFF) | constMagicValue1) % 0x1F) + constMagicValue1) & 0xFF;
                g = (g + (MAGIC[(int) (header[1] & 0xFF)] % constMagicValue1)) & 0xFF;
                g = (g ^ BL) & 0xFF;

                if (BL <= 2)
                    special = MAGIC[BL] & 0xFF;
                else
                    special = header[BL - 2] & 0xFF;

                result = MAGIC[f] ^ (MAGIC[g] & special);
                header[BL] = (byte) (result & 0xFF);

                BL++;

            } while (BL != 0x31);

        }
            break;
        case 2: {
            do {
                e = (((int) (header[1] & 0xFF) | constMagicValue1) % 0x1F) + constMagicValue1;
                e = (e + (MAGIC[(int) (header[1] & 0xFF)] % constMagicValue1)) & 0xFF;

                f = sftFileBackup[(BL % 0x2B) + constMagicValue1] & 0xFF;

                int multi = (int) (sftFileBackup[constMagicValue1] & 0xFF) * f;
                AL = (int) sftFileBackup[(int) (multi % e)] & 0xFF;
                DL = (int) MAGIC[(int) sftFileBackup[(0x3D - BL) & 0xFF] & 0xFF] & 0xFF;

                header[BL] = (byte) (AL | DL);

                BL++;

            } while (BL != 0x31);
        }
            break;
        case 3: {
            do {
                e = (((int) (header[1] & 0xFF) | constMagicValue1) % 0x1F) + constMagicValue1;
                e = (e + (MAGIC[(int) (header[1] & 0xFF)] % constMagicValue1)) & 0xFF;

                f = (MAGIC[(int) (sftFileBackup[(int) (BL + constMagicValue1) & 0xFF]) & 0xFF] % e) & 0xFF;

                result = (((int) sftFileBackup[f] & 0xFF) ^ ((int) (MAGIC[BL] | header[BL - 1]) & 0xFF)) & 0xFF;
                header[BL] = (byte) result;

                BL++;
            } while (BL != 0x31);
        }
            break;
        default:
            throw new UnsupportedOperationException();
        }
    }

    protected static byte[] sft08_lmaaDudeDecrypt(byte[] body) throws Exception {
        int blockPos = 0;
        int totalSize = 0;

        ArrayList<byte[]> blocks = new ArrayList<byte[]>();

        // decode blocks
        while (blockPos < body.length) {
            int blockSize = ((body[blockPos + 1] & 0xFF) << 8) | (body[blockPos] & 0xFF);
            blockPos += 2;

            byte[] block = Arrays.copyOfRange(body, blockPos, blockPos + blockSize);
            block = sft08_lmaaDudeDecryptBlock(block);
            blockPos += blockSize;

            totalSize += block.length;
            blocks.add(block);
        }

        // merge blocks
        byte[] body_decrypt = new byte[totalSize];
        blockPos = 0;
        for (byte[] block : blocks) {
            System.arraycopy(block, 0, body_decrypt, blockPos, block.length);
            blockPos += block.length;
        }

        return body_decrypt;
    }

    protected static byte[] sft08_lmaaDudeDecryptBlock(byte[] srcBuff) throws Exception {
        byte[] dstBuff = new byte[0x8000];
        int dstPos = 0;
        int srcPos = 0;

        if (srcBuff[srcPos++] == 0x80) {
            throw new UnsupportedOperationException("routine not implemented (id: 1)");
        } else {
            int EBP_C = 0; // 2 Byte
            byte EBP11 = 0; // 1 Byte

            while (srcPos < srcBuff.length) {
                if (EBP11 == 0) {
                    EBP_C = ((srcBuff[srcPos] & 0xFF) << 8) + (srcBuff[srcPos + 1] & 0xFF);
                    EBP_C &= 0xFFFF;
                    EBP11 = 0x10;
                    srcPos += 2;
                }

                if ((EBP_C & 0x8000) == 0) {
                    dstBuff[dstPos++] = srcBuff[srcPos++];
                } else {
                    int EBP_A = ((srcBuff[srcPos] & 0xFF) << 4) + ((srcBuff[srcPos + 1] & 0xFF) >> 4);

                    if (EBP_A == 0) {
                        throw new UnsupportedOperationException("routine not implemented (id: 2)");
                    } else {
                        int EBP_E = (srcBuff[srcPos + 1] & 0x0F) + 2;

                        for (int i = 0; i <= EBP_E; ++i) {
                            dstBuff[i + dstPos] = dstBuff[(dstPos - EBP_A) + i];
                        }

                        srcPos += 2;
                        dstPos += EBP_E + 1;
                    }
                }

                EBP_C = (EBP_C << 1) & 0xFFFF;
                --EBP11;
            }
        }

        return Arrays.copyOf(dstBuff, dstPos);
    }
}
