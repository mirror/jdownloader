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

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.XML;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.gui.dialog.AskContainerPasswordDialog;
import org.jdownloader.gui.dialog.AskContainerPasswordDialogInterface;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import jd.controlling.container.ContainerConfig;
import jd.controlling.linkcrawler.ArchiveInfo;
import jd.controlling.linkcrawler.CrawledLink;
import jd.parser.Regex;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginsC;

public class SFDL extends PluginsC {
    /* Documentation: https://github.com/n0ix/SFDL.NET/wiki/How-it-Works-(SFDL-File-documentation) */
    public SFDL() {
        super("SFDL", "file:/.+\\.sfdl$", "$Revision: 13393 $");
    }

    public SFDL newPluginInstance() {
        return new SFDL();
    }

    /* Debug-test filename scheme containing a title and file-password. */
    public static final Pattern         PATTERN_COMMON_FILENAME_SCHEME_WITH_PASSWORD = Pattern.compile("^([^\\{]+)\\{\\{(.*?)\\}\\}\\.sfdl$", Pattern.CASE_INSENSITIVE);
    private final Object                PWLOCK                                       = new Object();
    public static final ContainerConfig CFG                                          = JsonConfig.create(ContainerConfig.class);

    public ContainerStatus callDecryption(final File sfdlFile) {
        final ContainerStatus cs = new ContainerStatus(sfdlFile);
        try {
            final DocumentBuilder documentBuilder = XML.newSecureFactory().newDocumentBuilder();
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
            String validpassword = null;
            if (sdfl_Encrypted) {
                logger.info("SFDL is password protected");
                if (passwordFromFilename == null) {
                    throw new Exception("Password is not given in filename");
                }
                final List<String> pwlist = CFG.getSFDLPasswordList();
                if (passwordFromFilename != null) {
                    /* If password is given inside filename, try this one first. */
                    pwlist.remove(passwordFromFilename);
                    pwlist.add(0, passwordFromFilename);
                }
                String decodedValue = null;
                if (pwlist.size() > 0) {
                    for (final String pw : pwlist) {
                        decodedValue = decrypt(sfdl_Host, pw);
                        if (decodedValue != null) {
                            validpassword = pw;
                            break;
                        }
                    }
                    if (validpassword == null) {
                        logger.info("Failed to find valid password in passwordlist");
                    }
                }
                if (decodedValue == null) {
                    /* Failed to find valid password in passwordlist -> Ask user */
                    int counter = 0;
                    do {
                        final String pw = this.getUserInputContainerPassword(sfdlFile);
                        if (pw != null) {
                            decodedValue = decrypt(sfdl_Host, pw);
                            if (decodedValue != null) {
                                validpassword = pw;
                                break;
                            }
                        }
                        counter++;
                    } while (counter <= 2);
                }
                if (decodedValue == null) {
                    logger.info("Failed to find password");
                    cs.setStatus(ContainerStatus.STATUS_INVALID_PASSWORD);
                    return cs;
                }
                /* Store valid password so we can re-use it next time an SFDL container is added. */
                this.addPassword(validpassword);
                sfdl_Host = decodedValue;
                /* Decrypt all other values */
                sfdl_Description = decrypt(sfdl_Description, validpassword);
                sfdl_Uploader = decrypt(sfdl_Uploader, validpassword);
                sfdl_Username = decrypt(sfdl_Username, validpassword);
                sfdl_Password = decrypt(sfdl_Password, validpassword);
                sfdl_DefaultPath = decrypt(sfdl_DefaultPath, validpassword);
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
                        value = decrypt(value, validpassword);
                    }
                    sfdl_BulkFolderPathArray.add(value);
                }
            } else {
                final NodeList downloadFiles = document.getElementsByTagName("FileFullPath");
                final NodeList fileSizes = document.getElementsByTagName("FileSize");
                for (int i = 0; i < downloadFiles.getLength(); i++) {
                    String value = downloadFiles.item(i).getTextContent();
                    if (sdfl_Encrypted) {
                        value = decrypt(value, validpassword);
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
                        ftpFolderPath = decrypt(ftpFolderPath, validpassword);
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
                        ftpFilePath = decrypt(ftpFilePath, validpassword);
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

    /** Asks user for container password. */
    public String getUserInputContainerPassword(final File file) throws PluginException {
        // TODO: Maybe throw exception on abort
        final AskContainerPasswordDialogInterface handle = UIOManager.I().show(AskContainerPasswordDialogInterface.class, new AskContainerPasswordDialog("Enter container password", "Enter password for container file: " + file.getName(), file));
        if (handle.getCloseReason() == CloseReason.OK) {
            final String password = handle.getText();
            return password;
        } else {
            return null;
        }
    }

    /** Adds valid SFDL container password to list of container passwords. */
    public void addPassword(final String pw) {
        if (StringUtils.isEmpty(pw)) {
            return;
        }
        synchronized (PWLOCK) {
            List<String> pwList = CFG.getSFDLPasswordList();
            if (pwList == null) {
                pwList = new ArrayList<String>();
            }
            /* avoid duplicates */
            pwList.remove(pw);
            /* Add valid password to first position */
            pwList.add(0, pw);
            CFG.setSFDLPasswordList(pwList);
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

    @Override
    protected boolean canBePasswordProtected() {
        return true;
    }
}
