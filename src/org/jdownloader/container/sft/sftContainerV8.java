package org.jdownloader.container.sft;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.appwork.utils.encoding.Base64;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.params.KeyParameter;

public class sftContainerV8 extends sftContainer {
    public static final byte[]      SENSITIVPASSWORD = "memory".getBytes();
    protected byte[]                bHeader;
    protected boolean               decryptedState;
    protected final DelphiFormEntry sft_root;
    protected final byte[]          magicDecryptionKey;
    protected ArrayList<String>     linkList;

    public sftContainerV8(DelphiFormBinLoader dfm, byte[] magicDecryptionKey) throws IOException, NoSuchAlgorithmException {
        super(dfm);
        sft_root = dfm.getRoot().find(NODE_ITEMS).get(0);
        byte[] eHeader = Base64.decode(sft_root.find(NODE_HEADER).getValue());
        RC4 rc = new RC4(Arrays.copyOf(magicDecryptionKey, 128));
        rc.encode(eHeader);
        this.bHeader = eHeader;
        this.magicDecryptionKey = magicDecryptionKey;
        this.setPassword(null);
    }

    @Override
    public boolean setPassword(char[] cs) {
        final char[] MAGIC_SN = MAGIC_SERIAL.toCharArray();
        try {
            this.decryptedState = false;
            byte[] password;
            if (cs == null) {
                password = new byte[MAGIC_SN.length];
                for (int i = 0; i < password.length; ++i) {
                    password[i] = (byte) MAGIC_SN[i];
                }
            } else {
                password = new byte[MAGIC_SN.length < cs.length ? cs.length : MAGIC_SN.length];
                int i;
                for (i = 0; i < cs.length; ++i) {
                    password[i] = (byte) cs[i];
                }
                for (; i < password.length; ++i) {
                    password[i] = (byte) MAGIC_SN[i];
                }
            }
            byte[] headerCheck = new byte[bHeader.length];
            headerCheck[0] = bHeader[0];
            for (int i = 1; i < headerCheck.length; ++i) {
                byte xor = password[i];
                xor ^= bHeader[i];
                xor ^= bHeader[i - 1];
                headerCheck[i] = xor;
            }
            Pattern decryptPattern = Pattern.compile("^SFT#(\\d)#(\\d+)#SFT##$");
            /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
            Matcher decryptMatcher = decryptPattern.matcher(new String(headerCheck));
            if (decryptMatcher.matches()) {
                int passwordNeeded = Integer.parseInt(decryptMatcher.group(1));
                this.passwordNeeded = passwordNeeded > 0;
                byte[] sha256 = MessageDigest.getInstance(CRYPT_SHA256).digest(password);
                RC4 rc = new RC4(sha256);
                DelphiFormEntry node;
                node = sft_root.find(NODE_DESCRIPT);
                if (node != null) {
                    byte[] bValue = Base64.decode(node.getValue());
                    rc.encode(bValue);
                    this.strDescription = new String(bValue, "ISO-8859-1");
                }
                node = sft_root.find(NODE_UPLOADER);
                if (node != null) {
                    byte[] bValue = Base64.decode(node.getValue());
                    rc.encode(bValue);
                    this.strUploader = new String(bValue, "ISO-8859-1");
                }
                node = sft_root.find(NODE_COMMENT);
                if (node != null) {
                    byte[] bValue = Base64.decode(node.getValue());
                    rc.encode(bValue);
                    this.strComment = new String(bValue, "ISO-8859-1");
                }
                byte[] magicStringCopy = null;
                if (passwordNeeded > 0) {
                    magicStringCopy = new byte[cs.length];
                    for (int i = 0; i < magicStringCopy.length; ++i) {
                        magicStringCopy[i] = (byte) cs[i];
                    }
                } else {
                    magicStringCopy = Arrays.copyOfRange(sftBinary.MAGIC, 1, sftBinary.MAGIC.length);
                }
                ArrayList<byte[]> HostList = new ArrayList<byte[]>();
                ArrayList<byte[]> UsernameList = new ArrayList<byte[]>();
                ArrayList<byte[]> PasswordList = new ArrayList<byte[]>();
                ArrayList<byte[]> DirList = new ArrayList<byte[]>();
                ArrayList<byte[]> FileList = new ArrayList<byte[]>();
                linkList = new ArrayList<String>();
                node = sft_root.find(NODE_FTPDOWNLOAD);
                for (int i = node.getChildLength() - 1; i >= 0; i--) {
                    byte[] bHost = null;
                    // byte[] bPort = null;
                    byte[] bUsername = null;
                    byte[] bPassword = null;
                    byte[] bDirname = null;
                    byte[] bFilename = null;
                    {
                        DelphiFormEntry eHost = node.get(i);
                        DelphiFormEntry eDir = eHost.find(NODE_DIRS).get(0);
                        DelphiFormEntry eFile = eDir.find(NODE_FILES).get(0);
                        DelphiFormEntry neHost = eHost.find(NODE_HOST);
                        // DelphiFormEntry nePort = eHost.find("Port");
                        DelphiFormEntry neUsername = eHost.find(NODE_USERNAME);
                        DelphiFormEntry nePassword = eHost.find(NODE_PASSWORD);
                        DelphiFormEntry neDirname = eDir.find(NODE_DIRNAME);
                        DelphiFormEntry neFilename = eFile.find(NODE_FILENAME);
                        String Host = neHost == null ? null : neHost.getValue();
                        // String Port = nePort == null ? null : nePort.getValue();
                        String Username = neUsername == null ? null : neUsername.getValue();
                        String Password = nePassword == null ? null : nePassword.getValue();
                        String Dirname = neDirname == null ? null : neDirname.getValue();
                        String Filename = neFilename == null ? null : neFilename.getValue();
                        if (Host != null) {
                            bHost = DatatypeConverter.parseHexBinary(Host);
                        }
                        // if (Port != null) bPort = Port.getBytes();
                        if (Username != null) {
                            bUsername = DatatypeConverter.parseHexBinary(Username);
                        }
                        if (Password != null) {
                            bPassword = DatatypeConverter.parseHexBinary(Password);
                        }
                        if (Dirname != null) {
                            bDirname = DatatypeConverter.parseHexBinary(Dirname);
                        }
                        if (Filename != null) {
                            bFilename = Filename.getBytes("UTF-8");
                        }
                    }
                    RCx rcx = new RCx(null);
                    rcx.init2(magicDecryptionKey);
                    rcx.encode2(bHost);
                    rcx.init2(magicDecryptionKey);
                    rcx.encode2(bUsername);
                    rcx.init2(magicDecryptionKey);
                    rcx.encode2(bPassword);
                    rcx.init2(magicDecryptionKey);
                    rcx.encode2(bDirname);
                    HostList.add(bHost);
                    UsernameList.add(bUsername);
                    PasswordList.add(bPassword);
                    DirList.add(bDirname);
                    FileList.add(bFilename);
                    if (bHost != null) {
                        manipulateMagicString(magicStringCopy, bHost[bHost.length - 1]);
                    }
                    if (bUsername != null) {
                        manipulateMagicString(magicStringCopy, bUsername[bUsername.length - 1]);
                    }
                    if (bPassword != null) {
                        manipulateMagicString(magicStringCopy, bPassword[bPassword.length - 1]);
                    }
                    if (bDirname != null) {
                        manipulateMagicString(magicStringCopy, bDirname[bDirname.length - 1]);
                    }
                }
                for (int i = 0; i < node.getChildLength(); ++i) {
                    byte[] bHost = HostList.get(i);
                    byte[] bUsername = UsernameList.get(i);
                    byte[] bPassword = PasswordList.get(i);
                    byte[] bDirname = DirList.get(i);
                    byte[] bFilename = FileList.get(i);
                    decodeFilename(magicStringCopy, bFilename);
                    if (bDirname != null) {
                        if (!toBase64String(magicStringCopy, bDirname)) {
                            return false;
                        }
                    }
                    if (bPassword != null) {
                        if (!toBase64String(magicStringCopy, bPassword)) {
                            return false;
                        }
                    }
                    if (bUsername != null) {
                        if (!toBase64String(magicStringCopy, bUsername)) {
                            return false;
                        }
                    }
                    if (bHost != null) {
                        if (!toBase64String(magicStringCopy, bHost)) {
                            return false;
                        }
                    }
                    if (bHost != null) {
                        bHost = decodeCoreInformation(bHost);
                    }
                    if (bUsername != null) {
                        bUsername = decodeCoreInformation(bUsername);
                    }
                    if (bPassword != null) {
                        bPassword = decodeCoreInformation(bPassword);
                    }
                    if (bDirname != null) {
                        bDirname = decodeCoreInformation(bDirname);
                    }
                    String link = buildFTPLink(bHost, (short) 0, bUsername, bPassword, bDirname, bFilename);
                    linkList.add(link);
                }
                this.decryptedState = true;
                return true;
            } else {
                this.passwordNeeded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.decryptedState = false;
        }
        return false;
    }

    private byte[] decodeCoreInformation(byte[] encrypted) throws NoSuchAlgorithmException, IOException, Exception {
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        byte[] sha256 = MessageDigest.getInstance(CRYPT_SHA256).digest(SENSITIVPASSWORD);
        byte[] data = Base64.decode(new String(encrypted));
        byte[] IV = new byte[8];
        BlowfishEngine cipher = new BlowfishEngine();
        cipher.init(true, new KeyParameter(sha256));
        cipher.processBlock(IV, 0, IV, 0);
        for (int i = 0; i < data.length; ++i) {
            byte[] BlowFishRound = new byte[IV.length];
            cipher.init(true, new KeyParameter(sha256));
            cipher.processBlock(IV, 0, BlowFishRound, 0);
            byte temp = data[i];
            data[i] ^= BlowFishRound[0];
            for (int q = 1; q < 8; ++q) {
                IV[q - 1] = IV[q];
            }
            IV[7] = temp;
        }
        return data;
    }

    private boolean isBase64(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            char chr = (char) data[i];
            boolean valid = (chr >= 'A' && chr <= 'Z') || (chr >= 'a' && chr <= 'z') || (chr >= '0' && chr <= '9') || (chr == '+') || (chr == '/') || (chr == '=');
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private boolean toBase64String(byte[] magicString, byte[] encrypted) {
        int magicLen = magicString.length;
        byte a = encrypted[encrypted.length - 1];
        magicString[a % magicLen] ^= a;
        for (int i = 1; i < encrypted.length; i++) {
            a = magicString[i % magicString.length];
            a |= sftBinary.MAGIC[(i % 0xFF) + 1]; // FIX
            encrypted[i - 1] ^= a;
        }
        if (!isBase64(encrypted)) {
            return false;
        }
        return true;
    }

    private void decodeFilename(byte[] key, byte[] filename) {
        int keyLength = key.length;
        for (int i = 1; i <= filename.length; i++) {
            byte a = key[i % keyLength];
            byte b = sftBinary.MAGIC[(i % 0xFF) + 1];
            filename[i - 1] = (byte) ((a | b) ^ filename[i - 1]);
        }
    }

    private void manipulateMagicString(byte[] magicStringCopy, byte lastByte) {
        int p = (lastByte & 0xFF) % magicStringCopy.length;
        magicStringCopy[p] ^= lastByte;
    }

    @Override
    public boolean isDecrypted() {
        return this.decryptedState;
    }

    @Override
    public ArrayList<String> getFormatedLinks() {
        return linkList;
    }
}
