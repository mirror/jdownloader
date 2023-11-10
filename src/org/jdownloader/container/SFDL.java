package org.jdownloader.container;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jd.controlling.linkcrawler.ArchiveInfo;
import jd.controlling.linkcrawler.CrawledLink;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginsC;

import org.appwork.utils.encoding.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SFDL extends PluginsC {
    /* Documentation: https://github.com/n0ix/SFDL.NET/wiki/How-it-Works-(SFDL-File-documentation) */
    public SFDL() {
        super("SFDL", "file:/.+\\.sfdl$", "$Revision: 13393 $");
    }

    public SFDL newPluginInstance() {
        return new SFDL();
    }

    /* Debug-test filename scheme containing a title and file-password. */
    public static final Pattern PATTERN_COMMON_FILENAME_SCHEME_WITH_PASSWORD = Pattern.compile("^([^\\{]+)\\{\\{(.*?)\\}\\}\\.sfdl$", Pattern.CASE_INSENSITIVE);

    public ContainerStatus callDecryption(final File sfdlFile) {
        final ContainerStatus cs = new ContainerStatus(sfdlFile);
        try {
            final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            /* Parse XML file */
            final Document document = documentBuilder.parse(sfdlFile);
            final boolean sfdl_AuthRequired = getNode(document, "AuthRequired").equalsIgnoreCase("true");
            final boolean sdfl_Encrypted = getNode(document, "Encrypted").equalsIgnoreCase("true");
            final boolean sfdl_BulkFolderMode = getNode(document, "BulkFolderMode").equalsIgnoreCase("true");
            String sfdl_Description = getNode(document, "Description");
            String sfdl_Uploader = getNode(document, "Uploader");
            String sfdl_Host = getNode(document, "Host");
            final int sfdl_Port = Integer.parseInt(getNode(document, "Port"));
            String sfdl_Username = getNode(document, "Username");
            String sfdl_Password = getNode(document, "Password");
            String sfdl_DefaultPath = getNode(document, "DefaultPath");
            final Regex filenameSchemeWithPassword = new Regex(sfdlFile.getName(), PATTERN_COMMON_FILENAME_SCHEME_WITH_PASSWORD);
            ArchiveInfo archiveInfo = null;
            final FilePackage fp = FilePackage.getInstance();
            String passwordFromFilename = null;
            if (filenameSchemeWithPassword.patternFind()) {
                fp.setName(filenameSchemeWithPassword.getMatch(0));
                archiveInfo = new ArchiveInfo();
                archiveInfo.addExtractionPassword(passwordFromFilename);
                passwordFromFilename = filenameSchemeWithPassword.getMatch(1);
            } else {
                fp.setName(sfdlFile.getName());
            }
            String password = null;
            if (sdfl_Encrypted) {
                logger.info("SFDL is password protected");
                if (passwordFromFilename == null) {
                    throw new Exception("Password is not given in filename");
                }
                password = passwordFromFilename;
                // TODO: Maybe use extraction password list here? Or a dedicated list?
                String decodedValue = decrypt(sfdl_Host, password);
                if (decodedValue == null) {
                    logger.info("Failed due to invalid password: " + password);
                    // TODO: Add container state STATUS_INVALID_PASSWORD
                    cs.setStatus(ContainerStatus.STATUS_FINISHED);
                    return cs;
                }
                sfdl_Host = decodedValue;
                /* Decrypt all other values */
                sfdl_Description = decrypt(sfdl_Description, password);
                sfdl_Uploader = decrypt(sfdl_Uploader, password);
                sfdl_Username = decrypt(sfdl_Username, password);
                sfdl_Password = decrypt(sfdl_Password, password);
                sfdl_DefaultPath = decrypt(sfdl_DefaultPath, password);
                if (sfdl_Username == null || sfdl_Password == null) {
                    // TODO: Not sure if this is necessary.
                    sfdl_Username = "anonymous";
                    sfdl_Password = "anonymous@anonymous.com";
                }
            }
            final List<String> sfdl_BulkFolderPathArray = new ArrayList<String>();
            final List<String> sfdl_FileListArray = new ArrayList<String>();
            final List<Long> sfdl_FileSizeArray = new ArrayList<Long>();
            if (sfdl_BulkFolderMode) {
                final NodeList downloadFiles = document.getElementsByTagName("BulkFolderPath");
                for (int i = 0; i < downloadFiles.getLength(); i++) {
                    String value = downloadFiles.item(i).getTextContent();
                    if (sdfl_Encrypted) {
                        value = decrypt(value, password);
                    }
                    sfdl_BulkFolderPathArray.add(value);
                }
            } else {
                final NodeList downloadFiles = document.getElementsByTagName("FileFullPath");
                final NodeList fileSizes = document.getElementsByTagName("FileSize");
                for (int i = 0; i < downloadFiles.getLength(); i++) {
                    String value = downloadFiles.item(i).getTextContent();
                    if (sdfl_Encrypted) {
                        value = decrypt(value, password);
                    }
                    sfdl_FileListArray.add(value);
                    sfdl_FileSizeArray.add(Long.valueOf(fileSizes.item(i).getTextContent()).longValue());
                }
            }
            // TODO: Evaluate "MaxDownloadThreads" (?)
            /* TODO: Add check to determine if sfdl_Host is a valid ipv4 address(?) */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            if (sfdl_BulkFolderMode) {
                /* FTP folders(?) */
                final NodeList downloadFiles = document.getElementsByTagName("BulkFolderPath");
                logger.info("Found " + downloadFiles.getLength() + " FTP folders");
                for (int i = 0; i < downloadFiles.getLength(); i++) {
                    String ftpFolderPath = downloadFiles.item(i).getTextContent();
                    if (sdfl_Encrypted) {
                        ftpFolderPath = decrypt(ftpFolderPath, password);
                    }
                    String ftpurl = "ftp://";
                    if (sfdl_AuthRequired) {
                        ftpurl += sfdl_Username;
                        if (sfdl_Password != null) {
                            ftpurl += ":" + sfdl_Password;
                        }
                    }
                    ftpurl += "@" + sfdl_Host + ":" + sfdl_Port;
                    ftpurl += ftpFolderPath;
                    logger.info("Result: " + ftpurl);
                    final DownloadLink ftpfolder = new DownloadLink(null, null, "FTP", ftpurl, true);
                    ret.add(ftpfolder);
                }
            } else {
                /* FTP files(?) */
                final NodeList downloadFiles = document.getElementsByTagName("FileFullPath");
                final NodeList fileSizes = document.getElementsByTagName("FileSize");
                logger.info("Found " + downloadFiles.getLength() + " FTP files");
                for (int i = 0; i < downloadFiles.getLength(); i++) {
                    String ftpFilePath = downloadFiles.item(i).getTextContent();
                    if (sdfl_Encrypted) {
                        ftpFilePath = decrypt(ftpFilePath, password);
                    }
                    String ftpurl = "ftp://" + sfdl_Username;
                    if (sfdl_AuthRequired) {
                        ftpurl += sfdl_Username;
                        if (sfdl_Password != null) {
                            ftpurl += ":" + sfdl_Password;
                        }
                    }
                    ftpurl += "@" + sfdl_Host + ":" + sfdl_Port;
                    ftpurl += ftpFilePath;
                    logger.info("Result: " + ftpurl);
                    final DownloadLink ftpfile = new DownloadLink(ftpurl, true);
                    if (fileSizes.getLength() == downloadFiles.getLength()) {
                        final long filesize = Long.valueOf(fileSizes.item(i).getTextContent()).longValue();
                        ftpfile.setDownloadSize(filesize);
                    }
                    // TODO: Add setting for this
                    ftpfile.setAvailable(true);
                    ret.add(ftpfile);
                }
            }
            final ArrayList<CrawledLink> crawledLinks = new ArrayList<CrawledLink>(ret.size());
            for (final DownloadLink result : ret) {
                final CrawledLink crawledLink = new CrawledLink(result);
                if (archiveInfo != null) {
                    crawledLink.setArchiveInfo(archiveInfo);
                }
                crawledLinks.add(crawledLink);
            }
            cls = crawledLinks;
            cs.setStatus(ContainerStatus.STATUS_FINISHED);
            return cs;
        } catch (final Exception e) {
            /* Most likely we got a broken SFDL file -> Parser failure */
            logger.log(e);
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        }
    }

    private String decrypt(final String encodedString, final String password) {
        try {
            byte[] data = Base64.decode(encodedString);
            final MessageDigest md5pass = MessageDigest.getInstance("MD5");
            final byte[] pass = md5pass.digest(password.getBytes("UTF-8"));
            final IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(data, 0, 16));
            final SecretKeySpec keyspec = new SecretKeySpec(pass, "AES");
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, keyspec, iv);
            final byte[] decrypted = cipher.doFinal(data);
            if (decrypted.length < 17) {
                return null;
            } else {
                byte[] return_byte = Arrays.copyOfRange(decrypted, 16, decrypted.length);
                return new String(return_byte, "UTF-8");
            }
        } catch (Exception e) {
            logger.log(e);
            return null;
        }
    }

    private static String getNode(final Document document, final String node) {
        final NodeList chk = document.getElementsByTagName(node);
        if (chk.getLength() > 0) {
            return new String(document.getElementsByTagName(node).item(0).getTextContent());
        } else {
            return null;
        }
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

    @Override
    public boolean hideLinks() {
        return true;
    }
}
