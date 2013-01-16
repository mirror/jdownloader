package org.jdownloader.container.sft;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import sun.misc.BASE64Decoder;

public class sftContainerV4 extends sftContainer {

    public static String            MAGIC_SHA512_CALL = "callstackapi";
    public static String            MAGIC_SHA512_RELO = "SFT Loader Reloaded";
    protected final DelphiFormEntry sft_root;
    protected boolean               decryptedState;
    protected String                strHeader;
    protected ArrayList<String>     linkList;

    public sftContainerV4(DelphiFormBinLoader dfm) throws IOException, NoSuchAlgorithmException {
        super(dfm);

        sft_root = dfm.getRoot().find(NODE_ITEMS).get(0);
        DelphiFormEntry node;

        MessageDigest sh512 = MessageDigest.getInstance(CRYPT_SHA512);
        byte[] sha512_callkey = sh512.digest(MAGIC_SHA512_CALL.getBytes());
        byte[] sha512_reloadedkey = sh512.digest(MAGIC_SHA512_RELO.getBytes());
        byte[] eHeader = null;

        BASE64Decoder base64 = new BASE64Decoder();

        RC4 rc4 = new RC4(sha512_callkey);
        eHeader = base64.decodeBuffer(sft_root.find(NODE_HEADER).getValue());
        rc4.encode(eHeader);
        this.strHeader = new String(eHeader);

        // decode header information
        rc4.init(sha512_reloadedkey);
        node = sft_root.find(NODE_DESCRIPT);
        if (node != null) {

            byte[] eValue = base64.decodeBuffer(node.getValue());
            rc4.encode(eValue);
            this.strDescription = new String(eValue);
        }

        node = sft_root.find(NODE_UPLOADER);
        if (node != null) {
            byte[] eValue = base64.decodeBuffer(node.getValue());
            rc4.encode(eValue);
            this.strUploader = new String(eValue);
        }

        node = sft_root.find(NODE_COMMENT);
        if (node != null) {
            byte[] eValue = base64.decodeBuffer(node.getValue());
            rc4.encode(eValue);
            this.strComment = new String(eValue);
        }

        // check if file is valid and check if password needed
        if (this.strHeader.substring(0, 3).equals("SFT")) {
            this.passwordNeeded = this.strHeader.charAt(3) == '1';
            int pos = this.strHeader.lastIndexOf('#') + 1;
            this.strHeader = this.strHeader.substring(pos);

            if (!this.passwordNeeded) {
                if (!this.setPassword(null)) throw new UnsupportedOperationException("decrypt error");
            }
        } else
            throw new UnsupportedOperationException("decrypt error");
    }

    @Override
    public boolean setPassword(char[] cs) {
        try {
            byte[] checkHeader = DatatypeConverter.parseHexBinary(this.strHeader);
            byte[] pass = ("callstackapi" + (cs == null ? "" : new String(cs))).getBytes();
            byte[] sha512_pass = MessageDigest.getInstance(CRYPT_SHA512).digest(pass);
            sha512_pass = Arrays.copyOf(sha512_pass, sha512_pass.length / 2);

            // YurryXOR
            for (int j = 0; j < sha512_pass.length; j++)
                sha512_pass[j] ^= pass[j % pass.length];

            RCx rcx = new RCx(sha512_pass);
            rcx.decode(checkHeader);

            checkHeader = Arrays.copyOfRange(checkHeader, checkHeader.length - 41, checkHeader.length);

            if (new String(checkHeader).equals(MAGIC_SERIAL)) {
                DelphiFormEntry node = sft_root.find("FTPDownloads");

                linkList = new ArrayList<String>();
                for (int i = 0; i < node.getChildLength(); i++) {
                    byte[] bHost = null;
                    byte[] bUsername = null;
                    byte[] bPassword = null;
                    byte[] bDirname = null;
                    byte[] bFilename = null;

                    DelphiFormEntry eHost = node.get(i);
                    DelphiFormEntry eDir = eHost.find(NODE_DIRS).get(0);
                    DelphiFormEntry eFile = eDir.find(NODE_FILES).get(0);

                    DelphiFormEntry neHost = eHost.find(NODE_HOST);
                    DelphiFormEntry neUsername = eHost.find(NODE_USERNAME);
                    DelphiFormEntry nePassword = eHost.find(NODE_PASSWORD);
                    DelphiFormEntry neDirname = eDir.find(NODE_DIRNAME);
                    DelphiFormEntry neFilename = eFile.find(NODE_FILENAME);

                    String Host = neHost == null ? null : neHost.getValue();
                    String Username = neUsername == null ? null : neUsername.getValue();
                    String Password = nePassword == null ? null : nePassword.getValue();
                    String Dirname = neDirname == null ? null : neDirname.getValue();
                    String Filename = neFilename == null ? null : neFilename.getValue();

                    if (Host != null) bHost = DatatypeConverter.parseHexBinary(Host);
                    if (Username != null) bUsername = DatatypeConverter.parseHexBinary(Username);
                    if (Password != null) bPassword = DatatypeConverter.parseHexBinary(Password);
                    if (Dirname != null) bDirname = DatatypeConverter.parseHexBinary(Dirname);
                    if (Filename != null) bFilename = DatatypeConverter.parseHexBinary(Filename);

                    sha512_pass[0] ^= (byte) i;
                    rcx.init(sha512_pass);

                    if (bHost != null) {
                        rcx.reset();
                        rcx.decode(bHost);
                        bHost = Arrays.copyOfRange(bHost, 12, bHost.length);
                    }
                    if (bUsername != null) {
                        rcx.reset();
                        rcx.decode(bUsername);
                        bUsername = Arrays.copyOfRange(bUsername, 12, bUsername.length);
                    }
                    if (bPassword != null) {
                        rcx.reset();
                        rcx.decode(bPassword);
                        bPassword = Arrays.copyOfRange(bPassword, 12, bPassword.length);
                    }
                    if (bDirname != null) {
                        rcx.reset();
                        rcx.decode(bDirname);
                        bDirname = Arrays.copyOfRange(bDirname, 12, bDirname.length);
                    }
                    if (bFilename != null) {
                        rcx.reset();
                        rcx.decode(bFilename);
                        bFilename = Arrays.copyOfRange(bFilename, 12, bFilename.length);
                    }

                    String[] hosts = new String(bHost).split("::");
                    String link = buildFTPLink(hosts[0].getBytes(), (short) (Integer.parseInt(hosts[1]) & 0xFFFF), bUsername, bPassword, bDirname, bFilename);
                    linkList.add(link);
                }

                decryptedState = true;
            } else
                decryptedState = false;
        } catch (Exception e) {
            decryptedState = false;
        }

        return decryptedState;
    }

    @Override
    public boolean isDecrypted() {
        return decryptedState;
    }

    @Override
    public ArrayList<String> getFormatedLinks() {
        if (decryptedState)
            return linkList;
        else
            return null;
    }
}
