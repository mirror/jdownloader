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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import jd.nutils.io.FileSignatures;
import jd.nutils.io.Signature;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
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
import org.appwork.utils.formatter.StringFormatter;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.DummyDownloadLink;
import org.jdownloader.extensions.extraction.ExtractionConstants;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.IExtraction;

/**
 * Extracts rar, zip, 7z. tar.gz, tar.bz2.
 * 
 * @author botzi
 * 
 */
public class Multi extends IExtraction {

    private static final String      PatternRar         = "(?i).*\\.rar$";
    private static final String      PatternRarMulti    = "(?i).*\\.pa?r?t?\\.?\\d+.rar$";
    private static final String      PatternZip         = "(?i).*\\.zip$";
    private static final String      PatternTarGz       = "(?i).*\\.tar\\.gz$";
    private static final String      PatternTarBz2      = "(?i).*\\.tar\\.bz2$";
    private static final String      Pattern7z          = "(?i).*\\.7z$";
    private static final String      Pattern7zMulti     = "(?i).*\\.7z\\.\\d+$";

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
    public Archive buildArchive(DownloadLink link) {
        String file = link.getFileOutput();
        Archive archive = new Archive();
        archive.setExtractor(this);

        String pattern = "";
        ArrayList<DownloadLink> matches = new ArrayList<DownloadLink>();

        if (file.matches(PatternRarMulti)) {
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.pa?r?t?\\.?[0-9]+\\.rar$", "")) + "\\.pa?r?t?\\.?[0-9]+\\.rar$";
        } else if (file.matches(PatternRar)) {
            matches.add(link);
            pattern = "^" + Regex.escape(file.replaceAll("(?i)\\.rar$", "")) + "\\.r\\d+$";
        } else if (file.matches(Pattern7zMulti)) {
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

        if (!pattern.equals("")) {
            if (link instanceof DummyDownloadLink) {
                archive.setFirstDownloadLink(link);

                for (File f : new File(link.getFileOutput()).getParentFile().listFiles()) {
                    if (f.isDirectory()) continue;
                    if (new Regex(f.getAbsolutePath(), pattern, Pattern.CASE_INSENSITIVE).matches()) {
                        matches.add(buildDownloadLinkFromFile(f.getAbsolutePath()));
                    }
                }
            } else {
                for (DownloadLink l : JDUtilities.getController().getDownloadLinksByPathPattern(pattern)) {
                    matches.add(l);
                }
            }
        }

        archive.setDownloadLinks(matches);

        if (matches.size() == 1) {
            archive.setType(Archive.SINGLE_FILE);
            archive.setFirstDownloadLink(link);
        } else {
            for (DownloadLink l : matches) {
                if (l.getFileOutput().matches("(?i).*\\.pa?r?t?\\.?[0]*1.rar$")) {
                    archive.setType(Archive.MULTI_RAR);
                    archive.setFirstDownloadLink(l);
                    if (l.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFileOutput().matches("(?i).*\\.rar$") && !l.getFileOutput().matches("(?i).*\\.pa?r?t?\\.?[0-9]+.*?.rar$")) {
                    archive.setType(Archive.MULTI_RAR);
                    archive.setFirstDownloadLink(l);
                    if (l.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
                        /* this should help finding the link that got downloaded */
                        continue;
                    }
                    break;
                } else if (l.getFileOutput().matches("(?i).*\\.7z\\.001$")) {
                    archive.setType(Archive.MULTI);
                    archive.setFirstDownloadLink(l);
                    if (l.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
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
     * Builds an Dummydownloadlink from an file.
     * 
     * @param file
     *            The file from the harddisk.
     * @return The Downloadlink.
     */
    private DownloadLink buildDownloadLinkFromFile(String file) {
        File file0 = new File(file);
        DummyDownloadLink link = new DummyDownloadLink(file0.getName());
        link.setFile(file0);
        return link;
    }

    @Override
    public Archive buildDummyArchive(String file) {
        File file0 = new File(file);
        DownloadLink link = JDUtilities.getController().getDownloadLinkByFileOutput(file0, LinkStatus.FINISHED);
        if (link == null) {
            link = buildDownloadLinkFromFile(file);
        }
        return buildArchive(link);
    }

    @Override
    public boolean findPassword(String password) {
        crack++;
        controller.fireEvent(ExtractionConstants.WRAPPER_PASSWORT_CRACKING);

        try {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException e) {
                    logger.warning("Unable to close archive");
                }
            }

            if (archive.getType() == Archive.SINGLE_FILE) {
                inArchive = SevenZip.openInArchive(format, stream, password);
            } else if (archive.getType() == Archive.MULTI) {
                if (multiopener != null) {
                    multiopener.close();
                }

                multiopener = new MultiOpener(password);
                IInStream inStream = new VolumedArchiveInStream(archive.getFirstDownloadLink().getFileOutput(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == Archive.MULTI_RAR) {
                if (raropener != null) {
                    raropener.close();
                }

                raropener = new RarOpener(password);
                IInStream inStream = raropener.getStream(archive.getFirstDownloadLink().getFileOutput());
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
            CPUPriority priority = config.getCPUPriority();

            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                // Skip 0 Byte files (folders)
                if (item == null || item.getSize() == 0) {
                    continue;
                }

                if (filter(item.getPath())) {
                    logger.info("Filtering file " + item.getPath() + " in " + archive.getFirstDownloadLink().getFileOutput());
                    continue;
                }

                final File extractTo = new File(archive.getExtractTo().getAbsoluteFile() + File.separator + item.getPath());

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

                MultiCallback call = new MultiCallback(extractTo, controller, priority, item.getCRC() > 0 ? true : false);
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
                Date date = item.getLastWriteTime();
                if (date != null && date.getTime() >= 0) {
                    if (!extractTo.setLastModified(date.getTime())) {
                        logger.warning("Could not set last write/modified time for " + item.getPath());
                    }
                }

                // TODO: Write an proper CRC check
                // System.out.println(item.getCRC() + " : " +
                // Integer.toHexString(item.getCRC()) + " : " +
                // call.getComputedCRC());
                // if(item.getCRC() > 0 && !call.getComputedCRC().equals("0")) {
                // if(!call.getComputedCRC().equals(Integer.toHexString(item.getCRC())))
                // {
                // for(DownloadLink link :
                // getAffectedDownloadLinkFromArchvieFiles(item.getPath())) {
                // archive.addCrcError(link);
                // }
                // archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                // return;
                // }
                // }

                if (item.getSize() != extractTo.length()) {
                    for (DownloadLink link : getAffectedDownloadLinkFromArchvieFiles(item.getPath())) {
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
     * Returns the downloadlinks in which the given extracted file is present.
     * Works only with Rar multipart files.
     * 
     * @param path
     *            The extracted file.
     * @return The downloadlinks.
     * @throws FileNotFoundException
     * @throws SevenZipException
     */
    private List<DownloadLink> getAffectedDownloadLinkFromArchvieFiles(String path) throws FileNotFoundException, SevenZipException {
        ArrayList<DownloadLink> result = new ArrayList<DownloadLink>();

        if (archive.getType() == Archive.MULTI || archive.getType() == Archive.SINGLE_FILE) {
            result = archive.getDownloadLinks();
            return result;
        }

        ISevenZipInArchive in;
        for (DownloadLink link : archive.getDownloadLinks()) {
            in = SevenZip.openInArchive(null, new RandomAccessFileInStream(new RandomAccessFile(link.getFileOutput(), "r")));

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
            if (archive.getFirstDownloadLink() instanceof DummyDownloadLink) {
                Archive a = buildArchive(archive.getFirstDownloadLink());
                archive.setDownloadLinks(a.getDownloadLinks());
                archive.setType(a.getType());
            }

            String[] entries = config.getBlacklistPatterns();
            if (entries != null) {
                for (String entry : entries) {
                    if (entry.trim().length() != 0) {
                        filter.add(entry.trim());
                    }
                }
            }

            if (archive.getType() == Archive.SINGLE_FILE) {
                if (new Regex(archive.getFirstDownloadLink().getFileOutput(), "(?i)(.*)\\.rar$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.RAR;
                } else if (new Regex(archive.getFirstDownloadLink().getFileOutput(), "(?i)(.*)\\.7z$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.SEVEN_ZIP;
                } else if (new Regex(archive.getFirstDownloadLink().getFileOutput(), "(?i)(.*)\\.zip$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.ZIP;
                } else if (new Regex(archive.getFirstDownloadLink().getFileOutput(), "(?i)(.*)\\.tar\\.gz$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.GZIP;
                } else if (new Regex(archive.getFirstDownloadLink().getFileOutput(), "(?i)(.*)\\.tar\\.bz2$", Pattern.CASE_INSENSITIVE).matches()) {
                    format = ArchiveFormat.BZIP2;
                }

                stream = new RandomAccessFileInStream(new RandomAccessFile(archive.getFirstDownloadLink().getFileOutput(), "r"));
                inArchive = SevenZip.openInArchive(format, stream);
            } else if (archive.getType() == Archive.MULTI) {
                multiopener = new MultiOpener();
                IInStream inStream = new VolumedArchiveInStream(archive.getFirstDownloadLink().getFileOutput(), multiopener);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            } else if (archive.getType() == Archive.MULTI_RAR) {
                raropener = new RarOpener();
                IInStream inStream = raropener.getStream(archive.getFirstDownloadLink().getFileOutput());
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
                    return true;
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
                if (item.getSize() > 0) {
                    numberOfFiles++;
                }
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
    public String getArchiveName(DownloadLink link) {
        String match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.pa?r?t?\\.?[0-9]+.rar$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.rar$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.zip$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.7z$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.7z\\.\\d+$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.tar\\.gz$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.tar\\.bz2$", Pattern.CASE_INSENSITIVE).getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.r\\d+$", Pattern.CASE_INSENSITIVE).getMatch(0);
        return match;
    }

    @Override
    public boolean isArchivSupported(String file) {
        if (file.matches(PatternRarMulti)) return true;
        if (file.matches(PatternRar)) return true;
        if (file.matches(PatternZip)) return true;
        if (file.matches(Pattern7z)) return true;
        if (file.matches(Pattern7zMulti)) return true;
        if (file.matches(PatternTarGz)) return true;
        if (file.matches(PatternTarBz2)) return true;
        return false;
    }

    @Override
    public void close() {
        // Deleteing rar recovery volumes
        if (archive.getExitCode() == ExtractionControllerConstants.EXIT_CODE_SUCCESS && archive.getFirstDownloadLink().getFileOutput().matches(PatternRarMulti)) {
            for (DownloadLink link : archive.getDownloadLinks()) {
                File f = new File(link.getFileOutput().replace(".rar", ".rev"));
                if (f.exists()) {
                    logger.info("Deleteing rar recovery volume " + f.getAbsolutePath());
                    if (!f.delete()) {
                        logger.warning("Could not deleting rar recovery volume " + f.getAbsolutePath());
                    }
                }
            }
        }

        try {
            if (multiopener != null) {
                multiopener.close();
            }

            if (raropener != null) {
                raropener.close();
            }

            if (stream != null) {
                stream.close();
            }

            inArchive.close();
        } catch (SevenZipException e) {
            logger.warning("Tried to close a non existing archive");
        } catch (IOException e) {
            logger.warning("Tried to close an already closed archive");
        } catch (NullPointerException e) {
            logger.warning("Tried to close a non existing archive");
        }
    }

    @Override
    public boolean isArchivSupportedFileFilter(String file) {
        if (file.matches("(?i).*\\.pa?r?t?\\.?[0]*1.rar$")) return true;
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

        if (archive.getType() == Archive.SINGLE_FILE || archive.getDownloadLinks().size() < 2) return missing;

        int last = 1;
        int length = 0;
        int start = 2;
        String getpartid = "";
        String getwhole = "";
        String postfix = "";
        String first = "";
        DownloadLink firstdl = null;

        switch (archive.getType()) {
        case Archive.MULTI:
            firstdl = archive.getFirstDownloadLink();
            getpartid = "\\.7z\\.?(\\d+)";
            getwhole = "(\\.7z\\.?)(\\d+)";
            break;
        case Archive.MULTI_RAR:
            if (archive.getFirstDownloadLink().getFileOutput().matches("(?i).*\\.pa?r?t?\\.?[0]*1.rar$")) {
                getpartid = "\\.pa?r?t?\\.?(\\d+)\\.";
                getwhole = "(\\.pa?r?t?\\.?)(\\d+)\\.";
                postfix = ".rar";
                firstdl = archive.getFirstDownloadLink();
            } else if (archive.getFirstDownloadLink().getFileOutput().matches("(?i).*\\.rar$") && !archive.getFirstDownloadLink().getFileOutput().matches("(?i).*\\.pa?r?t?\\.?[0-9]+.*?.rar$")) {
                start = 0;
                getpartid = "\\.r(\\d+)";
                getwhole = "(\\.r)(\\d+)";
                for (DownloadLink l : archive.getDownloadLinks()) {
                    if (l.getFileOutput().endsWith(".r00")) {
                        firstdl = l;
                        break;
                    }
                }
            }
            break;
        default:
            return missing;
        }

        first = firstdl.getFileOutput();

        if (archive.getDownloadLinks().size() == 1) {
            String part = getArchiveName(firstdl) + new Regex(first, getwhole).getMatch(0) + StringFormatter.fillString(last++ + "", "0", "", length) + postfix;
            missing.add(part);
            return missing;
        }

        // Just get partnumbers to speed up the checking.
        List<Integer> erg = new ArrayList<Integer>();
        for (DownloadLink l : archive.getDownloadLinks()) {
            String e = "";
            if ((e = new Regex(l.getFileOutput(), getpartid).getMatch(0)) != null) {
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

        if (new File(first).length() <= new File(new File(first).getParent() + File.separator + getArchiveName(firstdl) + new Regex(first, getwhole).getMatch(0) + StringFormatter.fillString(last + "", "0", "", length) + postfix).length()) {
            String part = getArchiveName(firstdl) + new Regex(first, getwhole).getMatch(0) + StringFormatter.fillString(last++ + "", "0", "", length) + postfix;
            missing.add(part);
        }

        return missing;
    }

}