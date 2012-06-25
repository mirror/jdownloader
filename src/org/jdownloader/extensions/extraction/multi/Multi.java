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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.nutils.io.Signature;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.VolumedArchiveInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStreamPool;
import org.appwork.utils.ReusableByteArrayOutputStreamPool.ReusableByteArrayOutputStream;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.StringFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;

/**
 * Extracts rar, zip, 7z. tar.gz, tar.bz2.
 * 
 * @author botzi
 * 
 */
public class Multi extends IExtraction {

    private static final String Z_D_$ = "(\\.z)(\\d+)$";

    /**
     * Helper for the passwordfinding method.
     * 
     * @author botzi
     * 
     */
    private static class BooleanHelper {
        private boolean bool;

        BooleanHelper() {
            bool = false;
        }

        /**
         * Marks that the boolean was found.
         */
        void found() {
            bool = true;
        }

        /**
         * Returns the result.
         * 
         * @return The result.
         */
        boolean getBoolean() {
            return bool;
        }
    }

    private static final String      _7Z_D                                                       = "\\.7z\\.?(\\d+)$";

    private static final String      _7Z$                                                        = "\\.7z$";
    private static final String      BZ2$                                                        = "\\.bz2$";
    private static final String      GZ$                                                         = "\\.gz$";
    private static final String      REGEX_FIRST_PART_7ZIP                                       = "(?i).*\\.7z\\.001$";
    private static final String      REGEX_ZERO_PART_MULTI_RAR                                   = "(?i).*\\.pa?r?t?\\.?[0]*0\\.rar$";
    private static final String      REGEX_FIRST_PART_MULTI_RAR                                  = "(?i).*\\.pa?r?t?\\.?[0]*1.*?\\.rar$";
    private static final String      REGEX_ANY_MULTI_RAR_PART_FILE                               = "(?i).*\\.pa?r?t?\\.?[0-9]+.*?.rar$";
    private static final String      REGEX_ANY_DOT_R19_FILE                                      = "(?i).*\\.r\\d+$";
    private static final String      REGEX_ENDS_WITH_DOT_RAR                                     = "(?i).*\\.rar$";
    private static final String      PA_R_T_0_9_RAR$                                             = "\\.pa?r?t?\\.?[0-9]+.*?\\.rar$";
    private static final String      REGEX_FIND_PARTNUMBER_MULTIRAR                              = "\\.pa?r?t?\\.?(\\d+)\\.rar$";
    private static final String      REGEX_FIND_MULTIPARTRAR_FULL_PART_EXTENSION_AND_PART_NUMBER = "(\\.pa?r?t?\\.?)(\\d+)\\.";
    private static final String      REGEX_ANY_7ZIP_PART                                         = "(?i).*\\.7z\\.\\d+$";
    public static final String       PATTERN_RAR_MULTI                                           = "(?i).*\\.pa?r?t?\\.?\\d+.*?\\.rar$";
    private static final String      REGEX_FIND_PARTNUMBER                                       = "\\.r(\\d+)$";
    private static final String      REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER                    = "(\\.r)(\\d+)$";

    private static final String      REGEX_EXTENSION_RAR                                         = "\\.rar$";
    private static final String      TAR_BZ2$                                                    = "\\.tar\\.bz2$";

    private static final String      TAR_GZ$                                                     = "(\\.tar\\.gz|\\.tgz)$";

                                                                                                                                          ;
    private static final String      TAR$                                                        = "\\.tar$";

    private static final String      REGEX_ZIP$                                                  = "(?i).*\\.zip$";
    private static final String      ZIP$                                                        = "\\.zip$";
    private int                      crack;
    private ArrayList<String>        filter                                                      = new ArrayList<String>();

    private ArchiveFormat            format;

    private ISevenZipInArchive       inArchive;
    /** For 7z */
    private MultiOpener              multiopener;

    /** For rar */
    private RarOpener                raropener;

    /** For all single files */
    private RandomAccessFileInStream stream;

    private long                     progressInBytes;

    public Multi() {
        crack = 0;
        inArchive = null;
    }

    @Override
    public Archive buildArchive(ArchiveFactory link) throws ArchiveException {
        String file = link.getFilePath();
        Archive archive = link.createArchive();
        archive.setExtractor(this);
        archive.setName(getArchiveName(link));
        String pattern = "";
        boolean canBeSingleType = true;
        if (file.matches(PATTERN_RAR_MULTI)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.pa?r?t?\\.?[0-9]+.*?\\.rar$", "")) + "\\.pa?r?t?\\.?[0-9]+.*?\\.rar$";
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = false;
        } else if (file.matches(REGEX_ENDS_WITH_DOT_RAR)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.rar$", "")) + REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER;
            archive.setArchiveFiles(link.createPartFileList(file, pattern));

        } else if (file.matches(REGEX_ANY_DOT_R19_FILE)) {
            // matches.add(link);
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.r\\d+$", "")) + "\\.(r\\d+|rar)$";
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = false;
        } else if (file.matches(REGEX_ANY_7ZIP_PART)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.7z\\.\\d+$", "")) + _7Z_D;
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = false;
        } else if (file.matches(REGEX_ZIP$)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.zip$", "")) + ZIP$;
            archive.setArchiveFiles(link.createPartFileList(file, pattern));
            canBeSingleType = true;
        } else {
            throw new ArchiveException("Unsupported Archive: " + link.getFilePath());
        }

        if (archive.getArchiveFiles().size() == 1 && canBeSingleType) {
            archive.setType(ArchiveType.SINGLE_FILE);
            archive.setFirstArchiveFile(link);
        } else {

            for (ArchiveFile l : archive.getArchiveFiles()) {
                if (archive.getFirstArchiveFile() == null && l.getFilePath().matches(REGEX_FIRST_PART_MULTI_RAR)) {
                    archive.setType(ArchiveType.MULTI_RAR);
                    archive.setFirstArchiveFile(l);
                    // l.isValid()
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFilePath().matches(REGEX_ZERO_PART_MULTI_RAR)) {
                    archive.setType(ArchiveType.MULTI_RAR);
                    archive.setFirstArchiveFile(l);
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFilePath().matches(REGEX_ENDS_WITH_DOT_RAR) && !l.getFilePath().matches(REGEX_ANY_MULTI_RAR_PART_FILE)) {
                    if (archive.getArchiveFiles().size() == 1) {
                        archive.setType(ArchiveType.SINGLE_FILE);
                        archive.setFirstArchiveFile(link);
                    } else {
                        archive.setType(ArchiveType.MULTI_RAR);
                        archive.setFirstArchiveFile(l);
                    }
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFilePath().matches(REGEX_FIRST_PART_7ZIP)) {
                    archive.setType(ArchiveType.MULTI);
                    archive.setFirstArchiveFile(l);
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                }
                // TODO several multipart archive types are missing in this loop
            }
            if (archive.getType() == null) {
                // maybe first part missing? try to get archive type without
                // first part

                for (ArchiveFile l : archive.getArchiveFiles()) {
                    if (l.getFilePath().matches(REGEX_ANY_MULTI_RAR_PART_FILE)) {

                        archive.setType(ArchiveType.MULTI_RAR);

                        if (!l.isValid()) {
                            /*
                             * this should help finding the link that got downloaded
                             */
                            continue;
                        }
                        break;
                    } else if (l.getFilePath().matches(REGEX_ANY_7ZIP_PART)) {
                        archive.setType(ArchiveType.MULTI);

                        if (!l.isValid()) {
                            /*
                             * this should help finding the link that got downloaded
                             */
                            continue;
                        }
                        break;
                    } else if (l.getFilePath().matches(REGEX_ANY_DOT_R19_FILE)) {
                        archive.setType(ArchiveType.MULTI_RAR);
                        if (!l.isValid()) {
                            /*
                             * this should help finding the link that got downloaded
                             */
                            continue;
                        }
                        break;
                    }
                    // TODO several multipart archive types are missing in this
                    // loop
                }
            }
        }
        if (archive.getType() == null) throw new ArchiveException("Unsupported Archive: " + link.getFilePath());
        return archive;
    }

    @Override
    public boolean checkCommand() {
        File tmp = null;
        String libID = null;
        try {
            String s = System.getProperty("os.arch");
            String s1 = System.getProperty("os.name").split(" ")[0];
            libID = new StringBuilder().append(s1).append("-").append(s).toString();
            logger.finer("Lib ID: " + libID);
            tmp = Application.getResource("tmp/7zip");
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
                    if (SevenZip.isInitializedSuccessfully()) return true;
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
            DummyArchive ret = new DummyArchive(archive);

            if (archive.getType() == ArchiveType.SINGLE_FILE) {
                ret.add(new DummyArchiveFile(archive.getArchiveFiles().get(0)));
                return ret;
            }

            int last = 0;
            int length = 0;
            int start = 2;
            String getpartid = "";

            String format = null;
            HashMap<Integer, ArchiveFile> erg = new HashMap<Integer, ArchiveFile>();
            switch (archive.getType()) {
            case MULTI:

                getpartid = _7Z_D;
                start = 0;
                format = archive.getArchiveFiles().get(0).getName().replace("%", "\\%").replace("$", "\\$").replaceAll("(\\.7z\\.?)(\\d+)$", "$1%s");
                length = new Regex(archive.getArchiveFiles().get(0).getName(), "\\.7z\\.?(\\d+)$").getMatch(0).length();
                // Just get partnumbers to speed up the checking.

                for (ArchiveFile l : archive.getArchiveFiles()) {
                    String e = "";
                    // String name = l.getName();
                    if ((e = new Regex(l.getFilePath(), getpartid).getMatch(0)) != null) {
                        int p = Integer.parseInt(e);
                        if (p > last) last = p;
                        if (p < start) start = p;
                        erg.put(p, l);
                    }
                }

                break;
            case MULTI_RAR:

                for (ArchiveFile af : archive.getArchiveFiles()) {
                    if (af.getFilePath().matches(REGEX_ANY_MULTI_RAR_PART_FILE)) {
                        getpartid = REGEX_FIND_PARTNUMBER_MULTIRAR;
                        start = 1;
                        format = af.getName().replace("%", "\\%").replace("$", "\\$").replaceAll(REGEX_FIND_MULTIPARTRAR_FULL_PART_EXTENSION_AND_PART_NUMBER, "$1%s.");
                        length = new Regex(af.getName(), "\\.pa?r?t?\\.?(\\d+).*?\\.").getMatch(0).length();
                        // Just get partnumbers to speed up the checking.

                        for (ArchiveFile l : archive.getArchiveFiles()) {
                            String e = "";
                            // String name = l.getName();
                            if ((e = new Regex(l.getFilePath(), getpartid).getMatch(0)) != null) {
                                int p = Integer.parseInt(e);
                                if (p > last) last = p;
                                if (p < start) start = p;
                                erg.put(p, l);
                            }
                        }
                        break;
                    } else if (af.getFilePath().matches(REGEX_ANY_DOT_R19_FILE)) {
                        start = 0;

                        getpartid = REGEX_FIND_PARTNUMBER;
                        format = af.getName().replace("%", "\\%").replace("$", "\\$").replaceAll(REGEX_FIND_PARTNUMBER, ".r%s");
                        length = new Regex(af.getName(), REGEX_FIND_PARTNUMBER).getMatch(0).length();
                        // Just get partnumbers to speed up the checking.

                        for (ArchiveFile l : archive.getArchiveFiles()) {
                            String e = "";
                            // String name = l.getName();
                            if ((e = new Regex(l.getFilePath(), getpartid).getMatch(0)) != null) {
                                int p = Integer.parseInt(e);
                                if (p > last) last = p;
                                if (p < start) start = p;
                                erg.put(p, l);

                            } else if (l.getFilePath().endsWith(".rar")) {
                                ret.add(new DummyArchiveFile(l));

                            }
                        }
                        if (!erg.containsKey(0)) start = 1;
                        if (ret.getSize() == 0) {
                            // .rar missing
                            ret.add(new DummyArchiveFile(archive.getName() + ".rar", archive.getFolder()));

                        }
                        break;
                    }

                }

            }
            if (format == null) throw new CheckException("Cannot check Archive " + archive.getName());

            for (int i = start; i <= last; i++) {

                ArchiveFile af = erg.get(i);

                if (af != null) {
                    // available file
                    ret.add(new DummyArchiveFile(af));
                } else {
                    // missing file
                    String part = String.format(format, StringFormatter.fillString(i + "", "0", "", length));
                    ret.add(new DummyArchiveFile(part, archive.getFolder()));
                }

            }

            return ret;
        } catch (Throwable e) {
            logger.log(e);
            throw new CheckException("Cannot check Archive " + archive.getName(), e);
        }
    }

    @Override
    public void close() {
        // Deleteing rar recovery volumes
        try {
            if (archive.getExitCode() == ExtractionControllerConstants.EXIT_CODE_SUCCESS && archive.getFirstArchiveFile().getFilePath().matches(PATTERN_RAR_MULTI)) {
                for (ArchiveFile link : archive.getArchiveFiles()) {
                    File f = archive.getFactory().toFile(link.getFilePath().replace(REGEX_EXTENSION_RAR, ".rev"));
                    if (f.exists() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(".rev")) {
                        logger.info("Deleting rar recovery volume " + f.getAbsolutePath());
                        if (!f.delete()) {
                            logger.warning("Could not deleting rar recovery volume " + f.getAbsolutePath());
                        }
                    }
                }
            }
        } finally {
            try {
                multiopener.close();
            } catch (final Throwable e) {
            }
            try {
                raropener.close();
            } catch (final Throwable e) {
            }
            try {
                stream.close();
            } catch (final Throwable e) {
            }
            try {
                inArchive.close();
            } catch (final Throwable e) {
            }
        }

    }

    @Override
    public String createID(ArchiveFactory f) {

        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, ZIP$, _7Z$, _7Z_D, TAR_GZ$, TAR_BZ2$, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, TAR$, Z_D_$ };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(f.getName());
            if (matcher.find()) {
                //

                if (p.equals(REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER)) {
                    // we cannot distingish between *.rar and *.rar -> *.r00
                    // archives when looking only at one single filename. that's
                    // why the get the same id
                    return matcher.replaceAll(REGEX_EXTENSION_RAR.replace("\\", "_").replace("$", "_"));
                } else if (p.equals(Z_D_$)) {
                    // we cannot distingish between *.zip and *.zip -> *.z01
                    // archives when looking only at one single filename. that's
                    // why the get the same id
                    return matcher.replaceAll(ZIP$.replace("\\", "_").replace("$", "_"));
                } else {
                    return matcher.replaceAll(p.replace("\\", "_").replace("$", "_"));
                }

            }
        }
        return null;
    }

    @Override
    public void extract(final ExtractionController ctrl) {
        try {
            ctrl.setProgress(0.0d);
            final double size = archive.getContentView().getTotalSize() / 100.0d;
            progressInBytes = 0l;
            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                // Skip folders
                if (item == null || item.isFolder()) {
                    continue;
                }
                if (ctrl.gotKilled()) { throw new SevenZipException("Extraction has been aborted"); }
                String path = item.getPath();
                if (StringUtils.isEmpty(path)) {
                    // example: test.tar.gz contains a test.tar file, that has
                    // NO name. we create a dummy name here.
                    String archivename = archive.getFactory().toFile(archive.getFirstArchiveFile().getFilePath()).getName();
                    int in = archivename.lastIndexOf(".");
                    if (in > 0) {
                        path = archivename.substring(0, in);
                    }
                }
                if (filter(item.getPath())) {
                    logger.info("Filtering file " + item.getPath() + " in " + archive.getFirstArchiveFile().getFilePath());
                    progressInBytes += item.getSize();
                    ctrl.setProgress(progressInBytes / size);
                    continue;
                }
                if (CrossSystem.isWindows()) {
                    String pathParts[] = path.split("\\" + File.separator);
                    /* remove invalid path chars */
                    for (int pathIndex = 0; pathIndex < pathParts.length - 1; pathIndex++) {
                        pathParts[pathIndex] = CrossSystem.alleviatePathParts(pathParts[pathIndex]);
                    }
                    StringBuilder sb = new StringBuilder();
                    for (String pathPartItem : pathParts) {
                        if (sb.length() > 0) sb.append(File.separator);
                        sb.append(pathPartItem);
                    }
                    path = sb.toString();
                }
                String lastTryFilename = archive.getExtractTo().getAbsoluteFile() + File.separator + path;
                String filename = lastTryFilename;
                try {
                    filename = new String(filename.getBytes(), Charset.defaultCharset());
                } catch (Exception e) {
                    logger.warning("Encoding " + Charset.defaultCharset().toString() + " not supported. Using default filename.");
                }

                File extractTo = new File(filename);
                if (extractTo.exists()) {
                    /* file already exists */

                    if (controller.isOverwriteFiles()) {
                        if (!extractTo.delete()) {
                            setException(new Exception("Could not overwrite(delete) " + extractTo));
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                            return;
                        }
                    } else {
                        /* skip file */
                        progressInBytes += item.getSize();
                        ctrl.setProgress(progressInBytes / size);
                        continue;
                    }
                }
                if ((!extractTo.getParentFile().exists() && !extractTo.getParentFile().mkdirs())) {
                    setException(new Exception("Could not create folder for File " + extractTo));
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                    return;
                }
                String fixedFilename = null;
                while (true) {
                    try {
                        if (!extractTo.createNewFile()) {
                            setException(new Exception("Could not create File " + extractTo));
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                            return;
                        }
                        break;
                    } catch (final IOException e) {
                        logger.log(e);
                        if (fixedFilename == null) {
                            /* first try, we try again with lastTryFilename */
                            fixedFilename = lastTryFilename;
                            extractTo = new File(fixedFilename);
                            continue;
                        } else if (fixedFilename == lastTryFilename) {
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
                            return;
                        }
                    }
                }

                archive.addExtractedFiles(extractTo);

                // MultiCallback call = new MultiCallback(extractTo, controller,
                // config, item.getCRC() > 0 ? true : false);

                MultiCallback call = new MultiCallback(extractTo, controller, config, false) {

                    @Override
                    public int write(byte[] data) throws SevenZipException {
                        try {
                            if (ctrl.gotKilled()) throw new SevenZipException("Extraction has been aborted");
                            return super.write(data);
                        } finally {
                            progressInBytes += data.length;
                            ctrl.setProgress(progressInBytes / size);
                        }
                    }

                };
                ExtractOperationResult res;
                try {
                    if (item.isEncrypted()) {
                        res = item.extractSlow(call, archive.getSettings().getPassword());
                    } else {
                        res = item.extractSlow(call);
                    }
                } finally {
                    /* always close files, thats why its best in finally branch */
                    call.close();
                }
                // Set last write time
                if (config.isUseOriginalFileDate()) {
                    Date date = item.getLastWriteTime();
                    if (date != null && date.getTime() >= 0) {
                        if (!extractTo.setLastModified(date.getTime())) {
                            logger.warning("Could not set last write/modified time for " + item.getPath());
                        }
                    }
                } else {
                    extractTo.setLastModified(System.currentTimeMillis());
                }

                if (item.getSize() != extractTo.length()) {
                    if (ExtractOperationResult.OK == res) {
                        logger.info("Size missmatch, but Extraction returned OK?! Archive seems incomplete");
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR);
                        return;
                    }
                    for (ArchiveFile link : getAffectedArchiveFileFromArchvieFiles(item.getPath())) {
                        archive.addCrcError(link);
                    }
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                    return;
                }
                switch (res) {
                case OK:
                    /* extraction successfully ,continue with next file */
                    break;
                case CRCERROR:
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                    return;
                default:
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    return;
                }
            }
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
        } finally {
            controller.setProgress(100.0d);
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
        for (String entry : filter) {
            if (file.contains(entry)) { return true; }
        }

        return false;
    }

    /**
     * Builds an DummyArchiveFile from an file.
     * 
     * @param file
     *            The file from the harddisk.
     * 
     * @return The ArchiveFile.
     */
    // private ArchiveFile buildArchiveFileFromFile(String file) {
    // File file0 = new File(file);
    // DummyArchiveFile link = new DummyArchiveFile(file0.getName());
    // link.setFile(file0);
    // return link;
    // }
    //

    @Override
    public boolean findPassword(final ExtractionController ctl, String password, boolean optimized) {
        crack++;
        ReusableByteArrayOutputStream buffer = null;
        try {
            buffer = ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(64 * 1024, false);
            try {
                inArchive.close();
            } catch (Throwable e) {
            }
            try {
                multiopener.close();
            } catch (Throwable e) {
            }
            try {
                raropener.close();
            } catch (Throwable e) {
            }

            if (archive.getType() == ArchiveType.SINGLE_FILE) {
                inArchive = SevenZip.openInArchive(format, stream, password);
            } else if (archive.getType() == ArchiveType.MULTI) {

                multiopener = new MultiOpener(password);
                IInStream inStream = new VolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == ArchiveType.MULTI_RAR) {
                raropener = new RarOpener(archive, password);
                IInStream inStream = raropener.getStream(archive.getFirstArchiveFile());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            }

            final BooleanHelper passwordfound = new BooleanHelper();
            HashSet<String> checkedExtensions = new HashSet<String>();
            for (final ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isFolder() || (item.getSize() == 0 && item.getPackedSize() == 0)) {
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
                    if (!passwordfound.getBoolean()) {
                        try {
                            buffer.reset();
                            final ReusableByteArrayOutputStream signatureBuffer = buffer;
                            final long maxPWCheckSize = config.getMaxPasswordCheckSize() * 1024;

                            final int signatureMinLength;
                            if (new Regex(path, ".+\\.iso").matches()) {
                                signatureMinLength = 37000;
                            } else if (new Regex(path, ".+\\.mp3").matches()) {
                                signatureMinLength = 512;
                            } else {
                                signatureMinLength = 32;
                            }
                            logger.fine("Tray to crack " + path);
                            ExtractOperationResult result = item.extractSlow(new ISequentialOutStream() {
                                public int write(byte[] data) throws SevenZipException {
                                    int toWrite = Math.min(signatureBuffer.free(), data.length);
                                    if (toWrite > 0) {
                                        /* we still have enough buffer left to write the data */
                                        signatureBuffer.write(data, 0, toWrite);
                                    }
                                    if (signatureBuffer.size() >= signatureMinLength) {
                                        /* we have enough data available for a signature check */
                                        StringBuilder sigger = new StringBuilder();
                                        for (int i = 0; i < signatureBuffer.size() - 1; i++) {
                                            String s = Integer.toHexString(signatureBuffer.getInternalBuffer()[i]);
                                            s = (s.length() < 2 ? "0" + s : s);
                                            s = s.substring(s.length() - 2);
                                            sigger.append(s);
                                        }
                                        Signature signature = ctl.getFileSignatures().getSignature(sigger.toString());
                                        if (signature != null) {
                                            if (signature.getExtensionSure() != null && signature.getExtensionSure().matcher(path).matches()) {
                                                /* signature matches, lets abort PWFinding now */
                                                passwordfound.found();
                                                return 0;
                                            }
                                        }
                                    }
                                    if (item.getSize() <= maxPWCheckSize) {
                                        /* we still allow further extraction as the itemSize <= maxPWCheckSize */
                                        return data.length;
                                    } else {
                                        /* this will throw SevenZipException */
                                        return 0;
                                    }
                                }
                            }, password);
                            if (ExtractOperationResult.OK.equals(result)) {
                                passwordfound.found();
                            }
                        } catch (SevenZipException e) {

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
            if (!passwordfound.getBoolean()) return false;
            archive.getSettings().setPassword(password);
            updateContentView(inArchive.getSimpleInterface());
            return true;
        } catch (SevenZipException e) {
            return false;
        } catch (Throwable e) {
            logger.log(e);
            return false;
        } finally {
            try {
                ReusableByteArrayOutputStreamPool.reuseReusableByteArrayOutputStream(buffer);
            } catch (Throwable e) {
            } finally {
                buffer = null;
            }
        }
    }

    /**
     * Returns the ArchiveFiles in which the given extracted file is present. Works only with Rar multipart files.
     * 
     * @param path
     *            The extracted file.
     * @return The ArchiveFiles.
     * @throws FileNotFoundException
     * @throws SevenZipException
     */
    private List<ArchiveFile> getAffectedArchiveFileFromArchvieFiles(String path) throws FileNotFoundException, SevenZipException {
        ArrayList<ArchiveFile> result = new ArrayList<ArchiveFile>();

        if (archive.getType() == ArchiveType.MULTI || archive.getType() == ArchiveType.SINGLE_FILE) {
            result = archive.getArchiveFiles();
            return result;
        }

        ISevenZipInArchive in = null;
        RandomAccessFile raf = null;
        RandomAccessFileInStream rafi = null;
        for (ArchiveFile link : archive.getArchiveFiles()) {
            try {
                in = SevenZip.openInArchive(null, rafi = new RandomAccessFileInStream(raf = new RandomAccessFile(link.getFilePath(), "r")));
                for (ISimpleInArchiveItem item : in.getSimpleInterface().getArchiveItems()) {
                    if (item.getPath().equals(path)) {
                        result.add(link);
                    }
                }
            } finally {
                try {
                    in.close();
                } catch (final Throwable e) {
                }
                try {
                    rafi.close();
                } catch (final Throwable e) {
                }
                try {
                    raf.close();
                } catch (final Throwable e) {
                }
            }
        }

        return result;
    }

    @Override
    public String getArchiveName(ArchiveFactory factory) {

        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, ZIP$, _7Z$, _7Z_D, TAR_GZ$, TAR_BZ2$, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, TAR$, Z_D_$ };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(factory.getName());
            if (matcher.find()) {
                //

                return matcher.replaceAll("");

            }
        }
        return null;
    }

    @Override
    public int getCrackProgress() {
        return crack;
    }

    @Override
    public boolean isArchivSupported(ArchiveFactory factory) {
        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, ZIP$, _7Z$, _7Z_D, TAR_GZ$, TAR_BZ2$, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, TAR$, Z_D_$ };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(factory.getName());
            if (matcher.find()) { return true;

            }
        }
        return false;
    }

    private boolean matches(String filePath, String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(filePath).find();
    }

    @Override
    public boolean prepare() {
        try {
            // if (archive.getFirstArchiveFile() instanceof DummyArchiveFile) {
            // Archive a = buildArchive(archive.getFirstArchiveFile());
            // archive.setArchiveFiles(a.getArchiveFiles());
            // archive.setType(a.getType());
            // }

            String[] entries = config.getBlacklistPatterns();
            if (entries != null) {
                for (String entry : entries) {
                    if (entry.trim().length() != 0) {
                        filter.add(entry.trim());
                    }
                }
            }

            if (archive.getType() == ArchiveType.SINGLE_FILE) {
                if (matches(archive.getFirstArchiveFile().getFilePath(), REGEX_EXTENSION_RAR)) {
                    format = ArchiveFormat.RAR;

                } else if (matches(archive.getFirstArchiveFile().getFilePath(), _7Z$)) {
                    format = ArchiveFormat.SEVEN_ZIP;

                } else if (matches(archive.getFirstArchiveFile().getFilePath(), ZIP$)) {

                    format = ArchiveFormat.ZIP;
                } else if (matches(archive.getFirstArchiveFile().getFilePath(), GZ$)) {
                    format = ArchiveFormat.GZIP;
                } else if (matches(archive.getFirstArchiveFile().getFilePath(), BZ2$)) {
                    format = ArchiveFormat.BZIP2;
                }

                stream = new RandomAccessFileInStream(new RandomAccessFile(archive.getFirstArchiveFile().getFilePath(), "r"));
                inArchive = SevenZip.openInArchive(format, stream);
            } else if (archive.getType() == ArchiveType.MULTI) {
                multiopener = new MultiOpener();
                IInStream inStream = new VolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == ArchiveType.MULTI_RAR) {
                raropener = new RarOpener(archive);
                IInStream inStream = raropener.getStream(archive.getFirstArchiveFile());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            } else {
                return false;
            }

            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {

                if (item.isEncrypted()) {
                    archive.setProtected(true);
                    // return true;
                }

            }
            updateContentView(inArchive.getSimpleInterface());
        } catch (SevenZipException e) {
            // There are password protected multipart rar files
            archive.setProtected(true);
            return true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void updateContentView(ISimpleInArchive simpleInterface) {
        try {
            ContentView newView = new ContentView();
            for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                try {
                    if (item.getPath().trim().equals("") || filter(item.getPath())) continue;
                    newView.add(new PackedFile(item.isFolder(), item.getPath(), item.getSize()));
                } catch (SevenZipException e) {
                    logger.log(e);
                }
            }
            archive.setContentView(newView);
        } catch (SevenZipException e) {
            logger.log(e);
        }
    }

    @Override
    public boolean isMultiPartArchive(ArchiveFactory factory) {
        // rememver *.rar archives may be the start of a multiarchive, too
        String[] patterns = new String[] { PA_R_T_0_9_RAR$, REGEX_EXTENSION_RAR, _7Z_D, REGEX_FIND_PART_EXTENSION_AND_PARTNUMBER, Z_D_$, ZIP$ };

        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(factory.getName());
            if (matcher.find()) { return true;

            }
        }
        return false;
    }
}