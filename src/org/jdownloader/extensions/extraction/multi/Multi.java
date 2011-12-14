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
import java.util.List;
import java.util.regex.Pattern;

import jd.nutils.io.FileSignatures;
import jd.nutils.io.Signature;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.VolumedArchiveInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.StringFormatter;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.IExtraction;

/**
 * Extracts rar, zip, 7z. tar.gz, tar.bz2.
 * 
 * @author botzi
 * 
 */
public class Multi extends IExtraction {

    private static final String      PATTERN_RAR        = "(?i).*\\.rar$";
    private static final String      PATTERN_RAR_MULTI  = "(?i).*\\.pa?r?t?\\.?\\d+.rar$";
    private static final String      PATTERN_RAR_MULTI2 = "(?i).*\\.r\\d+$";
    private static final String      PATTERN_ZIP        = "(?i).*\\.zip$";
    private static final String      PATTERN_TAR        = "(?i).*\\.tar$";
    private static final String      PATTERN_TAR_GZ     = "(?i).*\\.tar\\.gz$";
    private static final String      PATTERB_TAR_BZ2    = "(?i).*\\.tar\\.bz2$";
    private static final String      PATTERN_7Z         = "(?i).*\\.7z$";
    private static final String      PATTERN_7Z_PART    = "(?i).*\\.7z\\.\\d+$";

    private int                      crack;
    private ISevenZipInArchive       inArchive;
    private ArchiveFormat            format;

    // Indicates that the passwordcheck works with extracting the archive.
    private boolean                  passwordExtracting = false;

    /** For 7z */
    private MultiOpener              multiopener;
    /** For rar */
    private RarOpener                raropener;
    /** For all single files */
    private RandomAccessFileInStream stream;

    private ArrayList<String>        filter             = new ArrayList<String>();

    public Multi() {
        crack = 0;
        inArchive = null;
    }

    @Override
    public Archive buildArchive(ArchiveFactory link) {
        String file = link.getFilePath();
        Archive archive = link.createArchive();
        archive.setExtractor(this);

        String pattern = "";
        ArrayList<ArchiveFile> matches = new ArrayList<ArchiveFile>();

        if (file.matches(PATTERN_RAR_MULTI)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.pa?r?t?\\.?[0-9]+\\.rar$", "")) + "\\.pa?r?t?\\.?[0-9]+\\.rar$";
        } else if (file.matches(PATTERN_RAR)) {
            matches.add(link);
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.rar$", "")) + "\\.r\\d+$";
        } else if (file.matches(PATTERN_RAR_MULTI2)) {
            // matches.add(link);
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.r\\d+$", "")) + "\\.(r\\d+|rar)$";
        } else if (file.matches(PATTERN_7Z_PART)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.7z\\.\\d+$", "")) + "\\.7z\\.\\d+$";
        } else {
            matches.add(link);
        }

        // String pattern = file.replaceAll("(?i)\\.pa?r?t?\\.?[0-9]+.*?.rar$",
        // "");
        // pattern = pattern.replaceAll("(?i)\\.rar$", "");
        // pattern = pattern.replaceAll("(?i)\\.r\\d+$", "");
        // pattern = pattern.replaceAll("(?i)\\.zip$", "");
        // pattern = pattern.replaceAll("(?i)\\.tar\\.gz$", "");
        // pattern = pattern.replaceAll("(?i)\\.tar\\.bz2$", "");
        // pattern = pattern.replaceAll("(?i)\\.7z$", "");
        // pattern = pattern.replaceAll("(?i)\\.7z\\.\\d+$", "");
        // pattern = "^" + Regex.escape(pattern) + ".*";

        // if (!pattern.equals("")) {
        // // if (link instanceof DummyArchiveFile) {
        // // for (File f : new
        // File(link.getFilePath()).getParentFile().listFiles()) {
        // // if (f.isDirectory()) continue;
        // // if (new Regex(f.getAbsolutePath(), pattern,
        // Pattern.CASE_INSENSITIVE).matches()) {
        // // matches.add(buildArchiveFileFromFile(f.getAbsolutePath()));
        // // }
        // // }
        // // } else {

        if (!StringUtils.isEmpty(pattern)) matches.addAll(link.createPartFileList(pattern));

        archive.setArchiveFiles(matches);

        if (matches.size() == 1) {
            archive.setType(Archive.SINGLE_FILE);
            archive.setFirstArchiveFile(link);
        } else {
            for (ArchiveFile l : matches) {
                if (archive.getFirstArchiveFile() == null && l.getFilePath().matches("(?i).*\\.pa?r?t?\\.?[0]*1.rar$")) {
                    archive.setType(Archive.MULTI_RAR);
                    archive.setFirstArchiveFile(l);
                    // l.isValid()
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFilePath().matches("(?i).*\\.pa?r?t?\\.?[0]*0.rar$")) {
                    archive.setType(Archive.MULTI_RAR);
                    archive.setFirstArchiveFile(l);
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFilePath().matches("(?i).*\\.rar$") && !l.getFilePath().matches("(?i).*\\.pa?r?t?\\.?[0-9]+.*?.rar$")) {
                    archive.setType(Archive.MULTI_RAR);
                    archive.setFirstArchiveFile(l);
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFilePath().matches("(?i).*\\.7z\\.001$")) {
                    archive.setType(Archive.MULTI);
                    archive.setFirstArchiveFile(l);
                    if (!l.isValid()) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                }
            }
        }

        return archive;
    }

    /**
     * Builds an DummyArchiveFile from an file.
     * 
     * @param file
     *            The file from the harddisk.
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
    public boolean findPassword(String password) {
        crack++;

        try {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException e) {
                    // logger.warning("Unable to close archive");
                }
            }

            if (archive.getType() == Archive.SINGLE_FILE) {
                inArchive = SevenZip.openInArchive(format, stream, password);
            } else if (archive.getType() == Archive.MULTI) {
                if (multiopener != null) {
                    multiopener.close();
                }

                multiopener = new MultiOpener(password);
                IInStream inStream = new VolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == Archive.MULTI_RAR) {
                if (raropener != null) {
                    raropener.close();
                }

                raropener = new RarOpener(password);
                IInStream inStream = raropener.getStream(archive.getFirstArchiveFile().getFilePath());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            }

            long size = 0;
            final BooleanHelper passwordfound = new BooleanHelper();
            String folder = "";
            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isFolder() || item.getSize() == 0) {
                    continue;
                }

                if (!passwordfound.getBoolean()) {
                    try {
                        final String path = item.getPath();
                        item.extractSlow(new ISequentialOutStream() {
                            public int write(byte[] data) throws SevenZipException {
                                if (passwordExtracting) {
                                    passwordfound.found();
                                    return 0;
                                }

                                int length = 0;
                                if (new Regex(path, ".+\\.iso").matches()) {
                                    length = 37000;
                                } else if (new Regex(path, ".+\\.mp3").matches()) {
                                    length = 512;
                                } else {
                                    length = 32;
                                }

                                if (length > data.length) {
                                    length = data.length;
                                }

                                StringBuilder sigger = new StringBuilder();
                                for (int i = 0; i < length - 1; i++) {
                                    String s = Integer.toHexString(data[i]);
                                    s = (s.length() < 2 ? "0" + s : s);
                                    s = s.substring(s.length() - 2);
                                    sigger.append(s);
                                }

                                Signature signature = FileSignatures.getSignature(sigger.toString());

                                if (signature != null) {
                                    if (signature.getExtensionSure() != null && signature.getExtensionSure().matcher(path).matches()) {
                                        passwordfound.found();
                                    }
                                }
                                return 0;
                            }
                        }, password);

                        passwordExtracting = true;
                        return false;

                    } catch (SevenZipException e) {
                        // An error will be thrown if the write method returns
                        // 0.
                        // It's not a problem, because it only signals the a
                        // password was accepted.
                    }
                }

                if (filter(item.getPath())) continue;

                String[] erg = item.getPath().split("/");
                if (archive.isNoFolder() && erg.length > 1) {
                    if (folder.equals("")) {
                        folder = erg[0];
                    } else {
                        if (!folder.equals(erg[0])) {
                            archive.setNoFolder(false);
                        }
                    }
                } else {
                    archive.setNoFolder(false);
                }

                size += item.getSize();
            }

            if (!passwordfound.getBoolean()) return false;

            archive.setSize(size);

            archive.setPassword(password);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (SevenZipException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void extract() {
        try {

            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                // Skip folders
                if (item == null || item.isFolder()) {
                    continue;
                }
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
                    continue;
                }

                String filename = archive.getExtractTo().getAbsoluteFile() + File.separator + path;

                try {
                    filename = new String(filename.getBytes(), Charset.defaultCharset());
                } catch (Exception e) {
                    logger.warning("Encoding " + Charset.defaultCharset().toString() + " not supported. Using default filename.");
                }

                final File extractTo = new File(filename);

                if (!extractTo.exists()) {
                    if ((!extractTo.getParentFile().exists() && !extractTo.getParentFile().mkdirs()) || !extractTo.createNewFile()) {
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                        return;
                    }
                } else {
                    if (archive.isOverwriteFiles()) {
                        if (!extractTo.delete()) {
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                            return;
                        }
                    } else {
                        archive.setExtracted(archive.getExtracted() + item.getSize());
                        continue;
                    }
                }

                archive.addExtractedFiles(extractTo);

                // MultiCallback call = new MultiCallback(extractTo, controller,
                // config, item.getCRC() > 0 ? true : false);
                MultiCallback call = new MultiCallback(extractTo, controller, config, false);
                ExtractOperationResult res;
                try {
                    if (item.isEncrypted()) {
                        res = item.extractSlow(call, archive.getPassword());
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

                // TODO: Write an proper CRC check
                // System.out.println(item.getCRC() + " : " +
                // Integer.toHexString(item.getCRC()) + " : " +
                // call.getComputedCRC());
                // if(item.getCRC() > 0 && !call.getComputedCRC().equals("0")) {
                // if(!call.getComputedCRC().equals(Integer.toHexString(item.getCRC())))
                // {
                // for(ArchiveFile link :
                // getAffectedArchiveFileFromArchvieFiles(item.getPath())) {
                // archive.addCrcError(link);
                // }
                // archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                // return;
                // }
                // }

                if (item.getSize() != extractTo.length()) {
                    for (ArchiveFile link : getAffectedArchiveFileFromArchvieFiles(item.getPath())) {
                        archive.addCrcError(link);
                    }
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                    return;
                }

                if (res != ExtractOperationResult.OK) {
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    return;
                }
            }
        } catch (SevenZipException e) {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        } catch (IOException e) {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
            return;
        }

        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
    }

    /**
     * Returns the ArchiveFiles in which the given extracted file is present.
     * Works only with Rar multipart files.
     * 
     * @param path
     *            The extracted file.
     * @return The ArchiveFiles.
     * @throws FileNotFoundException
     * @throws SevenZipException
     */
    private List<ArchiveFile> getAffectedArchiveFileFromArchvieFiles(String path) throws FileNotFoundException, SevenZipException {
        ArrayList<ArchiveFile> result = new ArrayList<ArchiveFile>();

        if (archive.getType() == Archive.MULTI || archive.getType() == Archive.SINGLE_FILE) {
            result = archive.getArchiveFiles();
            return result;
        }

        ISevenZipInArchive in;
        for (ArchiveFile link : archive.getArchiveFiles()) {
            in = SevenZip.openInArchive(null, new RandomAccessFileInStream(new RandomAccessFile(link.getFilePath(), "r")));

            for (ISimpleInArchiveItem item : in.getSimpleInterface().getArchiveItems()) {
                if (item.getPath().equals(path)) {
                    result.add(link);
                }
            }
        }

        return result;
    }

    @Override
    public boolean checkCommand() {
        try {
            SevenZip.initSevenZipFromPlatformJAR();
        } catch (SevenZipNativeInitializationException e) {
            logger.warning("Could not initialize Multiunpacker");
            return false;
        }
        return SevenZip.isInitializedSuccessfully();
    }

    @Override
    public int getCrackProgress() {
        return crack;
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

            if (archive.getType() == Archive.SINGLE_FILE) {
                if (new Regex(archive.getFirstArchiveFile().getFilePath(), "(?i)(.*)\\.rar$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.RAR;
                } else if (new Regex(archive.getFirstArchiveFile().getFilePath(), "(?i)(.*)\\.7z$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.SEVEN_ZIP;
                } else if (new Regex(archive.getFirstArchiveFile().getFilePath(), "(?i)(.*)\\.zip$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.ZIP;
                } else if (new Regex(archive.getFirstArchiveFile().getFilePath(), "(?i)(.*)\\.tar\\.gz$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.GZIP;
                } else if (new Regex(archive.getFirstArchiveFile().getFilePath(), "(?i)(.*)\\.tar\\.bz2$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.BZIP2;
                }

                stream = new RandomAccessFileInStream(new RandomAccessFile(archive.getFirstArchiveFile().getFilePath(), "r"));
                inArchive = SevenZip.openInArchive(format, stream);
            } else if (archive.getType() == Archive.MULTI) {
                multiopener = new MultiOpener();
                IInStream inStream = new VolumedArchiveInStream(archive.getFirstArchiveFile().getFilePath(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == Archive.MULTI_RAR) {
                raropener = new RarOpener();
                IInStream inStream = raropener.getStream(archive.getFirstArchiveFile().getFilePath());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            } else {
                return false;
            }

            long size = 0;
            int numberOfFiles = 0;
            String folder = "";
            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isFolder()) {
                    archive.setNoFolder(false);
                    continue;
                }

                if (item.isEncrypted()) {
                    archive.setProtected(true);
                    // return true;
                }

                if (item.getPath().trim().equals("") || filter(item.getPath())) continue;

                String[] erg = item.getPath().split("/");
                if (archive.isNoFolder() && erg.length > 1) {
                    if (folder.equals("")) {
                        folder = erg[0];
                    } else {
                        if (!folder.equals(erg[0])) {
                            archive.setNoFolder(false);
                        }
                    }
                } else {
                    archive.setNoFolder(false);
                }

                size += item.getSize();
                numberOfFiles++;
            }
            archive.setSize(size);
            archive.setNumberOfFiles(numberOfFiles);
        } catch (SevenZipException e) {
            // There are password protected multipart rar files
            archive.setProtected(true);
            return true;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public String getArchiveName(ArchiveFile link) {
        String match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.pa?r?t?\\.?[0-9]+.rar$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.rar$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.zip$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.7z$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.7z\\.\\d+$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.tar\\.gz$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.tar\\.bz2$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.r\\d+$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFilePath()).getName(), "(?i)(.*)\\.tar+$", Pattern.CASE_INSENSITIVE).getMatch(0);
        return match;
    }

    @Override
    public boolean isArchivSupported(String file) {
        if (file.matches(PATTERN_RAR_MULTI)) return true;
        if (file.matches(PATTERN_RAR)) return true;
        if (file.matches(PATTERN_RAR_MULTI2)) return true;
        if (file.matches(PATTERN_ZIP)) return true;
        if (file.matches(PATTERN_7Z)) return true;
        if (file.matches(PATTERN_7Z_PART)) return true;
        if (file.matches(PATTERN_TAR)) return true;
        if (file.matches(PATTERN_TAR_GZ)) return true;
        if (file.matches(PATTERB_TAR_BZ2)) return true;
        return false;
    }

    @Override
    public void close() {
        // Deleteing rar recovery volumes
        if (archive.getExitCode() == ExtractionControllerConstants.EXIT_CODE_SUCCESS && archive.getFirstArchiveFile().getFilePath().matches(PATTERN_RAR_MULTI)) {
            for (ArchiveFile link : archive.getArchiveFiles()) {
                File f = archive.getFactory().toFile(link.getFilePath().replace(".rar", ".rev"));
                if (f.exists()) {
                    logger.info("Deleteing rar recovery volume " + f.getAbsolutePath());
                    if (!f.delete()) {
                        logger.warning("Could not deleting rar recovery volume " + f.getAbsolutePath());
                    }
                }
            }
        }

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

    @Override
    public boolean isArchivSupportedFileFilter(String file) {
        if (file.matches("(?i).*\\.pa?r?t?\\.?[0]*1.rar$")) return true;
        if (file.matches("(?i).*\\.pa?r?t?\\.?[0]*0.rar$")) return true;
        if (file.matches("(?i).*\\.pa?r?t?\\.?[0-9]+.rar$")) return false;
        if (file.matches("(?i).*\\.rar$")) return true;
        if (file.matches("(?i).*\\.zip$")) return true;
        if (file.matches("(?i).*\\.7z$")) return true;
        if (file.matches("(?i).*\\.7z\\.001$")) return true;
        if (file.matches("(?i).*\\.tar\\.gz$")) return true;
        if (file.matches("(?i).*\\.tar\\.bz2$")) return true;
        return false;
    }

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

    @Override
    public List<String> checkComplete(Archive archive) {
        List<String> missing = new ArrayList<String>();

        if (archive.getType() == Archive.SINGLE_FILE || archive.getArchiveFiles().size() < 2) return missing;

        int last = 1;
        int length = 0;
        int start = 2;
        String getpartid = "";
        String getwhole = "";
        String postfix = "";
        String first = "";
        ArchiveFile firstdl = null;

        switch (archive.getType()) {
        case Archive.MULTI:
            firstdl = archive.getFirstArchiveFile();
            getpartid = "\\.7z\\.?(\\d+)";
            getwhole = "(\\.7z\\.?)(\\d+)";
            break;
        case Archive.MULTI_RAR:
            if (archive.getFirstArchiveFile().getFilePath().matches("(?i).*\\.pa?r?t?\\.?[0]*1.rar$") || archive.getFirstArchiveFile().getFilePath().matches("(?i).*\\.pa?r?t?\\.?[0]*0.rar$")) {
                getpartid = "\\.pa?r?t?\\.?(\\d+)\\.";
                getwhole = "(\\.pa?r?t?\\.?)(\\d+)\\.";
                postfix = ".rar";
                firstdl = archive.getFirstArchiveFile();
            } else if (archive.getFirstArchiveFile().getFilePath().matches("(?i).*\\.rar$") && !archive.getFirstArchiveFile().getFilePath().matches("(?i).*\\.pa?r?t?\\.?[0-9]+.*?.rar$")) {
                start = 0;
                getpartid = "\\.r(\\d+)";
                getwhole = "(\\.r)(\\d+)";
                for (ArchiveFile l : archive.getArchiveFiles()) {
                    if (l.getFilePath().endsWith(".r00")) {
                        firstdl = l;
                        break;
                    }
                }
            }
            break;
        default:
            return missing;
        }

        first = firstdl.getFilePath();

        if (archive.getArchiveFiles().size() == 1) {
            String part = getArchiveName(firstdl) + new Regex(first, getwhole).getMatch(0) + StringFormatter.fillString(last++ + "", "0", "", length) + postfix;
            missing.add(part);
            return missing;
        }

        // Just get partnumbers to speed up the checking.
        List<Integer> erg = new ArrayList<Integer>();
        for (ArchiveFile l : archive.getArchiveFiles()) {
            String e = "";
            String name = l.getName();
            if ((e = new Regex(l.getFilePath(), getpartid).getMatch(0)) != null) {
                int p = Integer.parseInt(e);

                if (p > last) last = p;

                erg.add(p);
            }
        }

        length = new Regex(first, getpartid).getMatch(0).length();

        for (int i = start; i <= last; i++) {
            if (!erg.contains(i)) {
                String part = getArchiveName(firstdl) + new Regex(first, getwhole).getMatch(0) + StringFormatter.fillString(i + "", "0", "", length) + postfix;
                missing.add(part);
            }
        }
        File firstFile = archive.getFactory().toFile(first);
        File lastFile = new File(firstFile.getParent() + File.separator + getArchiveName(firstdl) + new Regex(first, getwhole).getMatch(0) + StringFormatter.fillString(last + "", "0", "", length) + postfix);
        // ignore this check if both files do not exist
        if ((firstFile.exists() || lastFile.exists()) && firstFile.length() <= lastFile.length()) {
            String part = getArchiveName(firstdl) + new Regex(first, getwhole).getMatch(0) + StringFormatter.fillString(last++ + "", "0", "", length) + postfix;
            missing.add(part);
        }

        return missing;
    }
}