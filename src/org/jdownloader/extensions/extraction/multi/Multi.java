//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.extraction.multi;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.ARCHFamily;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.Signature;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.extraction.gui.iffileexistsdialog.IfFileExistsDialog;
import org.jdownloader.settings.IfFileExistsAction;

/**
 * Extracts rar, zip, 7z. tar.gz, tar.bz2.
 *
 * @author botzi
 *
 */
public class Multi extends IExtraction {

    private volatile int        crack;
    private final List<Pattern> filter = new ArrayList<Pattern>();

    private ISevenZipInArchive  inArchive;
    private IInStream           inStream;
    private Closeable           closable;

    public Multi() {
        crack = 0;
        inArchive = null;
    }

    @Override
    public Archive buildArchive(ArchiveFactory link) throws ArchiveException {
        return ArchiveType.createArchive(link, false);
    }

    public static boolean checkRARSignature(File file) {
        try {
            final String sig = FileSignatures.readFileSignature(file);
            final Signature signature = new FileSignatures().getSignature(sig);
            if (signature != null) {
                if ("RAR".equalsIgnoreCase(signature.getId())) {
                    return true;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean check7ZSignature(File file) {
        try {
            final String sig = FileSignatures.readFileSignature(file);
            final Signature signature = new FileSignatures().getSignature(sig);
            if (signature != null) {
                if ("7Z".equalsIgnoreCase(signature.getId())) {
                    return true;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setPermissions(ISimpleInArchiveItem item, File extractTo) {
        if (config.isRestoreFilePermissions() && item != null && extractTo != null && extractTo.exists()) {
            try {
                FilePermissionSet filePermissionSet = null;
                final Integer attributesInteger = item.getAttributes();
                if (attributesInteger != null && attributesInteger != 0) {
                    final int attributes = attributesInteger.intValue();
                    int attributeIndex = 0;
                    switch (CrossSystem.getOSFamily()) {
                    case MAC:
                    case LINUX:
                        filePermissionSet = new FilePermissionSet();
                        attributeIndex = 16;
                        filePermissionSet.setOtherExecute((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setOtherWrite((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setOtherRead((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setGroupExecute((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setGroupWrite((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setGroupRead((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setUserExecute((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setUserWrite((attributes & 1 << attributeIndex++) != 0);
                        filePermissionSet.setUserRead((attributes & 1 << attributeIndex++) != 0);
                        break;
                    default:
                        return;
                    }
                }
                if (filePermissionSet != null) {
                    if (Application.getJavaVersion() >= Application.JAVA17 && false) {
                        /**
                         * disabled at the moment, because some attributes are != 0 but chmod-> 000
                         */
                        FilePermission17.setFilePermission(extractTo, filePermissionSet);
                    } else {
                        if (filePermissionSet.isUserExecute()) {
                            if (!extractTo.setExecutable(true, filePermissionSet.isOtherExecute() == false && filePermissionSet.isOtherExecute() == false)) {
                                throw new IOException("Failed to set " + filePermissionSet + " to " + extractTo);
                            }
                        }
                    }
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
    }

    public void setLastModifiedDate(ISimpleInArchiveItem item, File extractTo) {
        // Set last write time
        try {
            if (config.isUseOriginalFileDate()) {
                final Date date = item.getLastWriteTime();
                if (date != null && date.getTime() >= 0) {
                    if (!extractTo.setLastModified(date.getTime())) {
                        logger.warning("Could not set last write/modified time for " + item.getPath());
                        return;
                    }
                }
            } else {
                extractTo.setLastModified(System.currentTimeMillis());
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
    }

    private static boolean useARMPiLibrary() {
        if (CrossSystem.isRaspberryPi()) {
            return true;
        }
        if (CrossSystem.isLinux() && ARCHFamily.ARM.equals(CrossSystem.getARCHFamily())) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream("/proc/cpuinfo");
                final BufferedReader is = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                try {
                    String line = null;
                    while ((line = is.readLine()) != null) {
                        if (line.contains("Oxsemi NAS")) {
                            return true;
                        } else if (line.contains("ARM926EJ-S")) {
                            return true;
                        }
                    }
                } finally {
                    is.close();
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        return false;
    }

    @Override
    public boolean checkCommand() {
        File tmp = null;
        String libID = System.getProperty("sevenzipLibID");
        try {
            if (StringUtils.isEmpty(libID)) {
                libID = null;
                switch (CrossSystem.OS.getFamily()) {
                case LINUX:
                    switch (CrossSystem.getARCHFamily()) {
                    case ARM:
                        if (useARMPiLibrary()) {
                            libID = "Linux-armpi";
                        } else {
                            libID = "Linux-arm";
                        }
                        break;
                    case X86:
                        if (CrossSystem.is64BitOperatingSystem()) {
                            libID = "Linux-amd64";
                        } else {
                            libID = "Linux-i386";
                        }
                        break;
                    default:
                        return false;
                    }
                    break;
                case MAC:
                    if (CrossSystem.is64BitOperatingSystem()) {
                        libID = "Mac-x86_64";
                    } else {
                        libID = "Mac-i386";
                    }
                    break;
                case WINDOWS:
                    if (CrossSystem.is64BitOperatingSystem()) {
                        libID = "Windows-amd64";
                    } else {
                        libID = "Windows-x86";
                    }
                    break;
                default:
                    return false;
                }
            }
            logger.finer("Lib ID: " + libID);
            tmp = Application.getTempResource("7zip");
            try {
                org.appwork.utils.Files.deleteRecursiv(tmp);
            } catch (final Throwable e) {
            }
            logger.finer("Lib Path: " + tmp);
            tmp.mkdirs();
            SevenZip.initSevenZipFromPlatformJAR(libID, tmp);
        } catch (Throwable e) {
            if (e instanceof UnsatisfiedLinkError && CrossSystem.isWindows()) {
                try {
                    /* workaround for sevenzipjbinding, missing dll imports */
                    String path = new Regex(e.getMessage(), "(.:.*?\\.dll)").getMatch(0);
                    logger.severe("Unsatisfied path " + path);
                    File root = new File(path).getParentFile();
                    System.load(new File(root, "mingwm10.dll").toString());
                    System.load(new File(root, "libgcc_s_dw2-1.dll").toString());
                    System.load(new File(root, "libstdc++-6.dll").toString());
                    SevenZip.initSevenZipFromPlatformJAR(libID, tmp);
                    if (SevenZip.isInitializedSuccessfully()) {
                        return true;
                    }
                } catch (final Throwable e2) {
                    e2.printStackTrace();
                }
            }
            try {
                org.appwork.utils.Files.deleteRecursiv(tmp);
            } catch (final Throwable e1) {
            }
            logger.log(e);
            logger.warning("Could not initialize Multiunpacker #1");
            try {
                String s2 = System.getProperty("java.io.tmpdir");
                logger.finer("Lib Path: " + (tmp = new File(s2)));
                SevenZip.initSevenZipFromPlatformJAR(tmp);
            } catch (Throwable e2) {
                logger.log(e2);
                logger.warning("Could not initialize Multiunpacker #2");
                return false;
            }
        }
        return SevenZip.isInitializedSuccessfully();
    }

    @Override
    public DummyArchive checkComplete(Archive archive) throws CheckException {
        try {
            final DummyArchive ret = new DummyArchive(archive, archive.getArchiveType().name());
            boolean hasMissingArchiveFiles = false;
            for (ArchiveFile archiveFile : archive.getArchiveFiles()) {
                if (archiveFile instanceof MissingArchiveFile) {
                    hasMissingArchiveFiles = true;
                }
                ret.add(new DummyArchiveFile(archiveFile));
            }
            if (hasMissingArchiveFiles == false) {
                final ArchiveType archiveType = archive.getArchiveType();
                final String firstArchiveFile = archive.getFirstArchiveFile().getFilePath();
                final String partNumberOfFirstArchiveFile = archiveType.getPartNumberString(firstArchiveFile);
                if (archiveType.getFirstPartIndex() != archiveType.getPartNumber(partNumberOfFirstArchiveFile)) {
                    throw new CheckException("Wrong firstArchiveFile(" + firstArchiveFile + ") for Archive(" + archive.getName() + ")");
                }
            }
            return ret;
        } catch (CheckException e) {
            throw e;
        } catch (Throwable e) {
            throw new CheckException("Cannot check Archive(" + archive.getName() + ")", e);
        }
    }

    @Override
    public void close() {
        final Archive archive = getArchive();
        try {
            if (archive.getExitCode() == ExtractionControllerConstants.EXIT_CODE_SUCCESS && ArchiveType.RAR_MULTI.equals(archive.getArchiveType())) {
                // Deleteing rar recovery volumes
                final HashSet<String> done = new HashSet<String>();
                for (ArchiveFile link : archive.getArchiveFiles()) {
                    if (done.add(link.getName())) {
                        final String filePath = link.getFilePath().replaceFirst("(?i)\\.rar$", ".rev");
                        final File file = new File(filePath);
                        if (file.exists() && file.isFile()) {
                            logger.info("Deleting rar recovery volume " + file.getAbsolutePath());
                            if (!file.delete()) {
                                logger.warning("Could not deleting rar recovery volume " + file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } finally {
            try {
                inArchive.close();
            } catch (final Throwable e) {
            }
            try {
                closable.close();
            } catch (final Throwable e) {
            }
        }

    }

    @Override
    public String createID(ArchiveFactory factory) {
        return ArchiveType.createArchiveID(factory);
    }

    @Override
    public void extract(final ExtractionController ctrl) {
        final Archive archive = getArchive();
        final ArchiveFormat format = archive.getArchiveType().getArchiveFormat();
        try {
            ctrl.setCompleteBytes(archive.getContentView().getTotalSize());
            ctrl.setProcessedBytes(0);
            if (ArchiveFormat.SEVEN_ZIP == format || ArchiveFormat.BZIP2 == format) {
                ArrayList<Integer> allItems = new ArrayList<Integer>();
                for (int i = 0; i < inArchive.getNumberOfItems(); i++) {
                    final Boolean isFolder = (Boolean) inArchive.getProperty(i, PropID.IS_FOLDER);
                    if (Boolean.TRUE.equals(isFolder)) {
                        continue;
                    }
                    allItems.add(i);
                }
                final int maxItemsRound = 50;
                ArrayList<Integer> round = new ArrayList<Integer>();
                Iterator<Integer> it = allItems.iterator();
                while (it.hasNext()) {
                    Integer next = it.next();
                    round.add(next);
                    if (round.size() == maxItemsRound || it.hasNext() == false) {
                        int[] items = new int[round.size()];
                        int index = 0;
                        for (Integer item : round) {
                            items[index++] = item;
                        }
                        round.clear();
                        Seven7ExtractCallback callback = null;
                        try {
                            inArchive.extract(items, false, callback = new Seven7ExtractCallback(this, inArchive, ctrl, archive, config));
                        } catch (SevenZipException e) {
                            logger.log(e);
                            throw e;
                        } finally {
                            ctrl.setCurrentActiveItem(null);
                            if (callback != null) {
                                callback.close();
                            }
                        }
                        if (callback != null) {
                            if (callback.hasError()) {
                                throw new SevenZipException("callback encountered an error!");
                            }
                            if (callback.isResultMissing()) {
                                throw new SevenZipException("callback is missing results!");
                            }
                        }
                    }
                }
            } else {
                for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                    // Skip folders
                    if (item == null || item.isFolder()) {
                        continue;
                    }
                    if (ctrl.gotKilled()) {
                        throw new SevenZipException("Extraction has been aborted");
                    }
                    AtomicBoolean skipped = new AtomicBoolean(false);
                    File extractTo = getExtractFilePath(item, ctrl, skipped);
                    if (skipped.get()) {
                        /* file is skipped */
                        continue;
                    }
                    if (extractTo == null) {
                        /* error */
                        return;
                    }
                    final Long size = item.getSize();
                    ctrl.setCurrentActiveItem(new Item(item.getPath(), size, extractTo));
                    try {
                        MultiCallback call = new MultiCallback(extractTo, controller, config, false) {

                            @Override
                            public int write(byte[] data) throws SevenZipException {
                                try {
                                    if (ctrl.gotKilled()) {
                                        throw new SevenZipException("Extraction has been aborted");
                                    }
                                    return super.write(data);
                                } finally {
                                    ctrl.addAndGetProcessedBytes(data.length);
                                }
                            }
                        };
                        archive.addExtractedFiles(extractTo);
                        ExtractOperationResult res;
                        try {
                            if (item.isEncrypted()) {
                                String pw = archive.getFinalPassword();
                                if (pw == null) {
                                    throw new IOException("Password is null!");
                                }
                                res = item.extractSlow(call, pw);
                            } else {
                                res = item.extractSlow(call);
                            }
                        } finally {
                            /* always close files, thats why its best in finally branch */
                            call.close();
                        }

                        setLastModifiedDate(item, extractTo);
                        setPermissions(item, extractTo);
                        if (size != null && size != extractTo.length()) {
                            if (ExtractOperationResult.OK == res) {
                                logger.info("Size missmatch for " + item.getPath() + ", but Extraction returned OK?! Archive seems incomplete");
                                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR);
                                return;
                            }
                            logger.info("Size missmatch for " + item.getPath() + " is " + extractTo.length() + " but should be " + size);
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                            return;
                        }
                        switch (res) {
                        case OK:
                            /* extraction successfully ,continue with next file */
                            break;
                        case CRCERROR:
                            logger.info("CRC Error for " + item.getPath());
                            writeCrashLog("CRC Error in " + item.getPath());
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                            return;
                        case UNSUPPORTEDMETHOD:
                            logger.info("Unsupported Method " + item.getMethod() + " in " + item.getPath());
                            writeCrashLog("Unsupported Method " + item.getMethod() + " in " + item.getPath());
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                            return;
                        default:
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                            return;
                        }
                    } finally {
                        ctrl.setCurrentActiveItem(null);
                    }
                }
            }
        } catch (MultiSevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(e.getExitCode());
            return;
        } catch (SevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        } catch (IOException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
            return;
        }
        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
    }

    /**
     * Checks if the file should be upacked.
     *
     * @param file
     * @return
     */
    private boolean filter(String file) {
        file = "/" + file;
        for (Pattern regex : filter) {
            try {
                if (regex.matcher(file).matches()) {
                    return true;
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        return false;
    }

    public File getExtractFilePath(ISimpleInArchiveItem item, ExtractionController ctrl, AtomicBoolean skipped) throws SevenZipException {
        final Archive archive = getArchive();
        String path = item.getPath();
        if (StringUtils.isEmpty(path)) {
            // example: test.tar.gz / test.tgz contains a test.tar file, that has
            // NO name. we create a dummy name here.
            final String firstPartName = archive.getFactory().toFile(archive.getFirstArchiveFile().getFilePath()).getName();
            final int in = firstPartName.lastIndexOf(".");
            if (in > 0) {
                path = firstPartName.substring(0, in);
            } else {
                path = "UnknownExtractionFilename";
            }
            if (ArchiveType.TGZ_SINGLE.equals(ctrl.getArchiv().getArchiveType()) && !StringUtils.endsWithCaseInsensitive(path, ".tar")) {
                path = path + ".tar";
            }
        }
        final Long size = item.getSize();
        if (filter(item.getPath())) {
            logger.info("Filtering file " + item.getPath() + " in " + archive.getFirstArchiveFile().getFilePath());
            if (size != null) {
                ctrl.addAndGetProcessedBytes(size);
            }
            skipped.set(true);
            return null;
        }
        if (CrossSystem.isWindows()) {
            String pathParts[] = path.split("\\" + File.separator);
            /* remove invalid path chars */
            for (int pathIndex = 0; pathIndex < pathParts.length - 1; pathIndex++) {
                pathParts[pathIndex] = CrossSystem.alleviatePathParts(pathParts[pathIndex]);
            }
            StringBuilder sb = new StringBuilder();
            for (String pathPartItem : pathParts) {
                if (sb.length() > 0) {
                    sb.append(File.separator);
                }
                sb.append(pathPartItem);
            }
            path = sb.toString();
        }
        String filename = controller.getExtractToFolder().getAbsoluteFile() + File.separator + path;

        File extractTo = new File(filename);
        logger.info("Extract " + filename);
        if (extractTo.exists()) {
            /* file already exists */

            IfFileExistsAction action = controller.getIfFileExistsAction();
            while (action == null || action == IfFileExistsAction.ASK_FOR_EACH_FILE) {
                IfFileExistsDialog d = new IfFileExistsDialog(extractTo, new Item(path, size, extractTo), archive);
                d.show();

                try {
                    d.throwCloseExceptions();
                } catch (Exception e) {
                    throw new SevenZipException(e);
                }
                action = d.getAction();
                if (action == null) {
                    throw new SevenZipException("Cannot handle if file exists");
                }
                if (action == IfFileExistsAction.AUTO_RENAME) {
                    extractTo = new File(extractTo.getParentFile(), d.getNewName());
                    if (extractTo.exists()) {
                        action = IfFileExistsAction.ASK_FOR_EACH_FILE;
                    }
                }
            }
            switch (action) {
            case OVERWRITE_FILE:
                if (!FileCreationManager.getInstance().delete(extractTo, null)) {
                    setException(new Exception("Could not overwrite(delete) " + extractTo));
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    return null;
                }
                break;

            case SKIP_FILE:
                /* skip file */
                if (size != null) {
                    ctrl.addAndGetProcessedBytes(size);
                }
                archive.addSkippedFiles(extractTo);
                skipped.set(true);
                return null;
            case AUTO_RENAME:
                String extension = Files.getExtension(extractTo.getName());
                String name = StringUtils.isEmpty(extension) ? extractTo.getName() : extractTo.getName().substring(0, extractTo.getName().length() - extension.length() - 1);
                int i = 1;
                while (extractTo.exists()) {
                    if (StringUtils.isEmpty(extension)) {
                        extractTo = new File(extractTo.getParentFile(), name + "_" + i);

                    } else {
                        extractTo = new File(extractTo.getParentFile(), name + "_" + i + "." + extension);

                    }
                    i++;
                }

                break;

            }

        }
        if ((!extractTo.getParentFile().exists() && !extractTo.getParentFile().mkdirs())) {
            setException(new Exception("Could not create folder for File " + extractTo));
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
            return null;
        }
        String fixedFilename = null;
        while (true) {
            try {
                if (!extractTo.createNewFile()) {
                    setException(new Exception("Could not create File " + extractTo));
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    return null;
                }
                extractTo.delete();
                break;
            } catch (final IOException e) {
                logger.log(e);
                if (fixedFilename == null) {
                    /* first try, we try again with lastTryFilename */
                    fixedFilename = filename;
                    extractTo = new File(fixedFilename);
                    continue;
                } else if (fixedFilename == filename) {
                    /* second try, we try with modified filename */
                    /* Invalid Chars could have occured, try to remove them */
                    logger.severe("Invalid Chars could have occured, try to remove them");
                    File parent = extractTo.getParentFile();
                    String brokenFilename = extractTo.getName();
                    /* new String so == returns false */
                    fixedFilename = new String(brokenFilename.replaceAll("[^\\w\\s\\.\\(\\)\\[\\],]", ""));
                    logger.severe("Replaced " + brokenFilename + " with " + fixedFilename);
                    extractTo = new File(parent, fixedFilename);
                    continue;
                } else {
                    setException(e);
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    return null;
                }
            }
        }
        return extractTo;
    }

    @Override
    public boolean findPassword(final ExtractionController ctl, String password, boolean optimized) throws ExtractionException {
        final Archive archive = getArchive();
        final ArchiveFormat format = archive.getArchiveType().getArchiveFormat();
        crack++;
        if (StringUtils.isEmpty(password)) {
            /* This should never happen */
            password = "";
        }
        ReusableByteArrayOutputStream buffer = null;
        final AtomicBoolean passwordfound = new AtomicBoolean(false);
        try {
            buffer = new ReusableByteArrayOutputStream(64 * 1024);
            try {
                inArchive.close();
            } catch (Throwable e) {
            }
            try {
                closable.close();
            } catch (final Throwable e) {
            }

            final IArchiveOpenCallback callBack;
            if (archive.getArchiveFiles().size() == 1) {
                final RandomAccessFile raf = new RandomAccessFile(archive.getFirstArchiveFile().getFilePath(), "r");
                closable = raf;
                callBack = new DummyOpener(password);
                inStream = new RandomAccessFileInStream(raf);
            } else {
                switch (archive.getArchiveType()) {
                case RAR_MULTI:
                    final RarOpener rarOpener = new RarOpener(archive, password, logger);
                    closable = rarOpener;
                    callBack = rarOpener;
                    inStream = rarOpener.getStream(archive.getFirstArchiveFile());
                    break;
                case SEVENZIP_PARTS:
                    final MultiOpener sevenZipPartsOpener = new MultiOpener(password);
                    closable = sevenZipPartsOpener;
                    callBack = sevenZipPartsOpener;
                    inStream = new ModdedVolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), sevenZipPartsOpener);
                    break;
                default:
                    final MultiOpener multiOpener = new MultiOpener(password);
                    closable = multiOpener;
                    callBack = multiOpener;
                    inStream = multiOpener.getStream(archive.getFirstArchiveFile());
                    break;
                }
            }
            if (inStream != null && closable != null) {
                inArchive = SevenZip.openInArchive(format, inStream, callBack);
            } else {
                return false;
            }
            final HashSet<String> checkedExtensions = new HashSet<String>();

            if (ArchiveFormat.SEVEN_ZIP == format) {
                if (archive.isPasswordRequiredToOpen()) {
                    // archive is open. password seems to be ok.
                    passwordfound.set(true);
                    return true;
                }
                ArrayList<Integer> allItems = new ArrayList<Integer>();
                for (int i = 0; i < inArchive.getNumberOfItems(); i++) {
                    final Boolean isFolder = (Boolean) inArchive.getProperty(i, PropID.IS_FOLDER);
                    if (Boolean.TRUE.equals(isFolder)) {
                        continue;
                    }
                    allItems.add(i);
                }
                int[] items = new int[allItems.size()];
                int index = 0;
                for (Integer item : allItems) {
                    items[index++] = item;
                }
                try {
                    inArchive.extract(items, false, new Seven7PWCallback(inArchive, passwordfound, password, buffer, config.getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes(), ctl.getFileSignatures(), optimized));
                } catch (SevenZipException e) {
                    e.printStackTrace();
                    // An error will be thrown if the write method
                    // returns
                    // 0.
                }
            } else {
                SignatureCheckingOutStream signatureOutStream = new SignatureCheckingOutStream(passwordfound, ctl.getFileSignatures(), buffer, config.getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes(), optimized);
                ISimpleInArchiveItem[] items = inArchive.getSimpleInterface().getArchiveItems();
                // we found some rar archives, that throw an exception when we try to open it with no or an invalid password, but do not
                // throw any exceptions if we use - for example - their archive name as password.
                // in this case, the archive opens fine, but does not show any contents. let's catch this case here
                if (archive.isPasswordRequiredToOpen() && items != null && items.length > 0) {
                    // archive is open. password seems to be ok.
                    passwordfound.set(true);
                    return true;
                }
                for (final ISimpleInArchiveItem item : items) {
                    final Long size = item.getSize();
                    final long packedSize = item.getPackedSize();
                    if (item.isFolder() || size == null || (size == 0 && packedSize == 0)) {
                        /*
                         * we also check for items with size ==0, they should have a packedsize>0
                         */
                        continue;
                    }
                    if (ctl.gotKilled()) {
                        /* extraction got aborted */
                        break;
                    }
                    final String path = item.getPath();
                    String ext = Files.getExtension(path);
                    if (checkedExtensions.add(ext) || !optimized) {
                        if (!passwordfound.get()) {
                            try {
                                signatureOutStream.reset();
                                signatureOutStream.setSignatureLength(path, size);
                                logger.fine("Try to crack " + path);
                                ExtractOperationResult result = item.extractSlow(signatureOutStream, password);
                                if (ExtractOperationResult.DATAERROR.equals(result)) {
                                    /*
                                     * 100% wrong password, DO NOT CONTINUE as unrar already might have cleaned up (nullpointer in native ->
                                     * crash jvm)
                                     */
                                    return false;
                                }
                                if (ExtractOperationResult.OK.equals(result)) {
                                    passwordfound.set(true);
                                }
                            } catch (SevenZipException e) {
                                e.printStackTrace();
                                // An error will be thrown if the write method
                                // returns
                                // 0.
                            }
                        } else {
                            /* pw found */
                            break;
                        }
                    }
                    // if (filter(item.getPath())) continue;
                }
            }
            if (!passwordfound.get()) {
                return false;
            }

            return true;
        } catch (SevenZipException e) {
            // this happens if the archive has encrypted filenames as well and thus needs a password to open it
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                archive.setPasswordRequiredToOpen(true);
                return false;
            }
            throw new ExtractionException(e, null);
        } catch (Throwable e) {
            throw new ExtractionException(e, null);
        } finally {
            if (passwordfound.get()) {
                archive.setFinalPassword(password);
                if (inArchive != null) {
                    updateContentView(inArchive.getSimpleInterface());
                }
            }
        }
    }

    @Override
    public String getArchiveName(ArchiveFactory factory) {
        return ArchiveType.createArchiveName(factory);
    }

    @Override
    public int getCrackProgress() {
        return crack;
    }

    @Override
    public boolean isArchivSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        if (allowDeepInspection) {
            try {
                return ArchiveType.createArchive(factory, allowDeepInspection) != null;
            } catch (ArchiveException e) {
                getLogger().log(e);
                return false;
            }
        } else {
            return createID(factory) != null;
        }
    }

    @Override
    public boolean prepare() throws ExtractionException {
        final Archive archive = getArchive();
        try {
            final String[] patternStrings = config.getBlacklistPatterns();
            filter.clear();
            if (patternStrings != null && patternStrings.length > 0) {
                for (final String patternString : patternStrings) {
                    try {
                        if (StringUtils.isNotEmpty(patternString) && !patternString.startsWith("##")) {
                            filter.add(Pattern.compile(patternString));
                        }
                    } catch (final Throwable e) {
                        getLogger().log(e);
                    }
                }
            }
            final ArchiveFormat format = archive.getArchiveType().getArchiveFormat();
            try {
                final String sig = FileSignatures.readFileSignature(new File(archive.getFirstArchiveFile().getFilePath()));
                final Signature signature = new FileSignatures().getSignature(sig);
                if (signature != null) {
                    final ArchiveFormat signatureFormat;
                    if ("7Z".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.SEVEN_ZIP;
                    } else if ("RAR".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.RAR;
                    } else if ("ZIP".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.ZIP;
                    } else if ("GZ".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.GZIP;
                    } else if ("BZ2".equalsIgnoreCase(signature.getId())) {
                        signatureFormat = ArchiveFormat.BZIP2;
                    } else {
                        signatureFormat = null;
                    }
                    if (signatureFormat != null && !format.equals(signatureFormat)) {
                        logger.warning("Format missmatch:" + format + "!=" + signatureFormat);
                    }
                }
            } catch (Throwable e) {
                getLogger().log(e);
            }

            final IArchiveOpenCallback callBack;
            if (archive.getArchiveFiles().size() == 1) {
                final RandomAccessFile raf = new RandomAccessFile(archive.getFirstArchiveFile().getFilePath(), "r");
                closable = raf;
                callBack = new DummyOpener();
                inStream = new RandomAccessFileInStream(raf);
            } else {
                switch (archive.getArchiveType()) {
                case RAR_MULTI:
                    final RarOpener rarOpener = new RarOpener(archive, logger);
                    closable = rarOpener;
                    callBack = rarOpener;
                    inStream = rarOpener.getStream(archive.getFirstArchiveFile());
                    break;
                case SEVENZIP_PARTS:
                    final MultiOpener sevenZipPartsOpener = new MultiOpener();
                    closable = sevenZipPartsOpener;
                    callBack = sevenZipPartsOpener;
                    inStream = new ModdedVolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), sevenZipPartsOpener);
                    break;
                default:
                    final MultiOpener multiOpener = new MultiOpener();
                    closable = multiOpener;
                    callBack = multiOpener;
                    inStream = multiOpener.getStream(archive.getFirstArchiveFile());
                    break;
                }
            }
            if (inStream != null && closable != null) {
                inArchive = SevenZip.openInArchive(format, inStream, callBack);
            } else {
                return false;
            }
            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isEncrypted()) {
                    archive.setProtected(true);
                    break;
                }
            }
            updateContentView(inArchive.getSimpleInterface());
        } catch (SevenZipException e) {
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                archive.setProtected(true);
                archive.setPasswordRequiredToOpen(true);
                return true;
            } else {
                logger.log(e);
                throw new ExtractionException(e, null);
            }
        } catch (Throwable e) {
            logger.log(e);
            throw new ExtractionException(e, null);
        }
        return true;
    }

    private void updateContentView(ISimpleInArchive simpleInterface) {
        final Archive archive = getArchive();
        try {
            if (archive != null) {
                final ContentView newView = new ContentView();
                for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                    try {
                        if (StringUtils.isEmpty(item.getPath()) || filter(item.getPath())) {
                            continue;
                        }
                        newView.add(new PackedFile(item.isFolder(), item.getPath(), item.getSize()));
                    } catch (SevenZipException e) {
                        getLogger().log(e);
                    }
                }
                archive.setContentView(newView);
            }
        } catch (SevenZipException e) {
            getLogger().log(e);
        }
    }

    @Override
    public boolean isMultiPartArchive(ArchiveFactory factory) {
        return ArchiveType.isMultiPartArchive(factory);
    }

    @Override
    public boolean isFileSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        for (ArchiveType archiveType : ArchiveType.values()) {
            if (archiveType.matches(factory.getFilePath())) {
                return true;
            }
        }
        return false;
    }
}