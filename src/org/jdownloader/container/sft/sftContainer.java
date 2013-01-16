package org.jdownloader.container.sft;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public abstract class sftContainer {

    public static String             MAGIC_SERIAL     = "{1F945Z9B-3ZA9-4Q13-A5DB-KDKDKFLDSHGVFBV}";
    public static String             CRYPT_SHA256     = "SHA-256";
    public static String             CRYPT_SHA512     = "SHA-512";
    public static String             NODE_ITEMS       = "items";
    public static String             NODE_HEADER      = "Header";
    public static String             NODE_DESCRIPT    = "Description";
    public static String             NODE_UPLOADER    = "Uploader";
    public static String             NODE_COMMENT     = "Comment";
    public static String             NODE_DIRS        = "Directories";
    public static String             NODE_FILES       = "Files";
    public static String             NODE_HOST        = "Host";
    public static String             NODE_PORT        = "Port";
    public static String             NODE_USERNAME    = "Username";
    public static String             NODE_PASSWORD    = "Password";
    public static String             NODE_DIRNAME     = "Directoryname";
    public static String             NODE_FILENAME    = "Filename";
    public static String             NODE_FTPDOWNLOAD = "FTPDownloads";

    public final DelphiFormBinLoader dfm;
    protected boolean                passwordNeeded;
    protected String                 strDescription;
    protected String                 strUploader;
    protected String                 strComment;

    public sftContainer(DelphiFormBinLoader dfm) {
        this.dfm = dfm;
        this.dfm.releaseInput();
    }

    public final boolean needPassword() {
        return passwordNeeded;
    }

    public final String getDescription() {
        return strDescription;
    }

    public final String getUploader() {
        return strUploader;
    }

    public final String getComment() {
        return strComment;
    }

    public abstract boolean setPassword(char[] cs);

    public abstract boolean isDecrypted();

    public abstract ArrayList<String> getFormatedLinks();

    public static String buildFTPLink(byte[] host, short port, byte[] username, byte[] password, byte[] path, byte[] filename) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        builder.append("ftp://");

        if (username != null)
            builder.append(URLEncoder.encode(new String(username), "CP1252"));
        else
            builder.append("anonymous");
        builder.append(":");

        if (password != null)
            builder.append(URLEncoder.encode(new String(password), "CP1252"));
        else
            builder.append("anonymous");
        builder.append("@");

        builder.append(new String(host).trim());

        if (port != 0) {
            builder.append(":");
            builder.append(String.valueOf(((int) (port) & 0xFFFF)));
        }

        builder.append(urlEncode(new String(path)));
        builder.append(urlEncode(new String(filename)));

        return builder.toString();
    }

    private static String urlEncode(String str) throws UnsupportedEncodingException {
        String url = str.replace("%", "%25");
        url = url.replace("&", "%26");
        url = url.replace("@", "%40");
        url = url.replace(" ", "%20");
        return url;
    }
}
