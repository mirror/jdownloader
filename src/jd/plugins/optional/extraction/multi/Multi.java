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

package jd.plugins.optional.extraction.multi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.optional.extraction.Archive;
import jd.plugins.optional.extraction.ExtractionConstants;
import jd.plugins.optional.extraction.ExtractionController;
import jd.plugins.optional.extraction.ExtractionControllerConstants;
import jd.plugins.optional.extraction.IExtraction;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.VolumedArchiveInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

/**
 * Extracts rar, zip, 7z. tar.gz, tar.bz2.
 * 
 * @author botzi
 * 
 */
public class Multi implements IExtraction {
    private static final String  DUMMY_HOSTER = "dum.my";
    private static final String  PRIORITY     = "PRIORITY";

    private Archive              archive;
    private int                  crack;
    private ExtractionController con;
    private ISevenZipInArchive   inArchive;
    private List<String>         postprocessing;
    private SubConfiguration     conf;
    private ArchiveFormat        format;

    public Multi() {
        crack = 0;
        postprocessing = new ArrayList<String>();
        inArchive = null;
    }

    public Archive buildArchive(DownloadLink link) {
        String file = link.getFileOutput();
        Archive archive = new Archive();
        // archive.addDownloadLink(link);

        String pattern = file.replaceAll("(?i)\\.pa?r?t?\\.?[0-9]+.*?.rar$", "");
        pattern = pattern.replaceAll("(?i)\\.rar$", "");
        pattern = pattern.replaceAll("(?i)\\.r\\d+$", "");
        pattern = pattern.replaceAll("(?i)\\.zip$", "");
        pattern = pattern.replaceAll("(?i)\\.tar\\.gz$", "");
        pattern = pattern.replaceAll("(?i)\\.tar\\.bz2$", "");
        pattern = pattern.replaceAll("(?i)\\.7z$", "");
        pattern = pattern.replaceAll("(?i)\\.7z\\.\\d+$", "");
        pattern = "^" + Regex.escape(pattern) + ".*";

        ArrayList<DownloadLink> matches = new ArrayList<DownloadLink>();

        if (!link.getHost().equals(DUMMY_HOSTER)) {
            matches = JDUtilities.getController().getDownloadLinksByPathPattern(pattern);
        } else {
            archive.setFirstDownloadLink(link);

            for (File f : new File(file).getParentFile().listFiles()) {
                if (f.isDirectory()) continue;
                if (new Regex(f.getAbsolutePath(), pattern, Pattern.CASE_INSENSITIVE).matches()) {
                    matches.add(buildDownloadLinkFromFile(f.getAbsolutePath()));
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
                    break;
                } else if (l.getFileOutput().matches("(?i).*\\.rar$") && !l.getFileOutput().matches("(?i)\\.pa?r?t?\\.?[0-9]+.*?.rar$")) {
                    archive.setType(Archive.MULTI_RAR);
                    archive.setFirstDownloadLink(l);
                    break;
                } else if (l.getFileOutput().matches("(?i).*\\.7z\\.001$")) {
                    archive.setType(Archive.MULTI);
                    archive.setFirstDownloadLink(l);
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
        DownloadLink link = new DownloadLink(null, file0.getName(), DUMMY_HOSTER, "", true);
        link.setDownloadSize(file0.length());
        FilePackage fp = FilePackage.getInstance();
        fp.setDownloadDirectory(file0.getParent());
        link.setFilePackage(fp);

        return link;
    }

    public Archive buildDummyArchive(String file) {
        File file0 = new File(file);
        DownloadLink link = JDUtilities.getController().getDownloadLinkByFileOutput(file0, LinkStatus.FINISHED);
        if (link == null) {
            link = buildDownloadLinkFromFile(file);
        }
        return buildArchive(link);
    }

    public boolean findPassword(String password) {
        crack++;
        con.fireEvent(ExtractionConstants.WRAPPER_PASSWORT_CRACKING);

        try {
            if (archive.getType() == Archive.SINGLE_FILE) {
                inArchive = SevenZip.openInArchive(format, new RandomAccessFileInStream(new RandomAccessFile(archive.getFirstDownloadLink().getFileOutput(), "r")), password);
            } else if (archive.getType() == Archive.MULTI) {
                MultiOpener multiopener = new MultiOpener(password);
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, new VolumedArchiveInStream(archive.getFirstDownloadLink().getFileOutput(), multiopener));
            } else if (archive.getType() == Archive.MULTI_RAR) {
                RarOpener raropener = new RarOpener(password);
                IInStream inStream = raropener.getStream(archive.getFirstDownloadLink().getFileOutput());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            }

            long size = 0;
            final BooleanHelper passwordfound = new BooleanHelper();
            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                size += item.getSize();
                if (!passwordfound.getBoolean()) {
                    try {
                        item.extractSlow(new ISequentialOutStream() {
                            public int write(byte[] data) throws SevenZipException {
                                passwordfound.found();
                                return 0;
                            }
                        }, password);
                    } catch (SevenZipException e) {
                        // An error will be thrown if the write method returns
                        // 0.
                        // It's not a problem, because it only signals the a
                        // password was accepted.
                    }
                }
            }

            if (!passwordfound.getBoolean()) return false;

            archive.setSize(size);

            archive.setPassword(password);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (SevenZipException e) {
            return false;
        }
    }

    public void extract() {
        try {
            int priority = conf.getIntegerProperty(PRIORITY);

            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                final File extractTo = new File(archive.getExtractTo().getAbsoluteFile() + File.separator + item.getPath());

                // Skip 0 Byte files (folders)
                if (item.getSize() == 0) {
                    continue;
                }

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

                MultiCallback call = new MultiCallback(extractTo, con, priority, item.getCRC() > 0 ? true : false);
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

                if (archive.getExitCode() != 0) { return; }

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

                postprocessing.add(extractTo.getAbsolutePath());
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

    public boolean checkCommand() {
        return SevenZip.isInitializedSuccessfully();
    }

    public int getCrackProgress() {
        return crack;
    }

    public boolean prepare() {
        try {
            if (archive.getFirstDownloadLink().getHost().equals(DUMMY_HOSTER)) {
                Archive a = buildArchive(archive.getFirstDownloadLink());
                archive.setDownloadLinks(a.getDownloadLinks());
                archive.setType(a.getType());
            }

            if (archive.getType() == Archive.SINGLE_FILE) {
                if (archive.getFirstDownloadLink().getFileOutput().endsWith(".rar")) {
                    format = ArchiveFormat.RAR;
                } else if (archive.getFirstDownloadLink().getFileOutput().endsWith(".7z")) {
                    format = ArchiveFormat.SEVEN_ZIP;
                } else if (archive.getFirstDownloadLink().getFileOutput().endsWith(".zip")) {
                    format = ArchiveFormat.ZIP;
                } else if (archive.getFirstDownloadLink().getFileOutput().endsWith(".tar.gz")) {
                    format = ArchiveFormat.GZIP;
                } else if (archive.getFirstDownloadLink().getFileOutput().endsWith(".tar,bz2")) {
                    format = ArchiveFormat.BZIP2;
                }

                inArchive = SevenZip.openInArchive(format, new RandomAccessFileInStream(new RandomAccessFile(archive.getFirstDownloadLink().getFileOutput(), "r")));
            } else if (archive.getType() == Archive.MULTI) {
                MultiOpener multiopener = new MultiOpener();
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, new VolumedArchiveInStream(archive.getFirstDownloadLink().getFileOutput(), multiopener));
            } else if (archive.getType() == Archive.MULTI_RAR) {
                RarOpener raropener = new RarOpener();
                IInStream inStream = raropener.getStream(archive.getFirstDownloadLink().getFileOutput());
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, raropener);
            } else {
                return false;
            }

            long size = 0;
            int numberOfFiles = 0;
            for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
                if (item.getPath().trim().equals("")) continue;
                if (item.isEncrypted()) {
                    archive.setProtected(true);
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

    public void initConfig(ConfigContainer config, SubConfiguration subConfig) {
        config.setGroup(new ConfigGroup(JDL.L("plugins.optional.extraction.multi.config", "Multi unpacker settings"), "gui.images.addons.unrar"));

        String[] priorities = new String[] { JDL.L("plugins.optional.extraction.multi.priority.high", "High"), JDL.L("plugins.optional.extraction.multi.priority.middle", "Middle"), JDL.L("plugins.optional.extraction.multi.priority.low", "Low") };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, PRIORITY, priorities, JDL.L("plugins.optional.extraction.multi.priority", "Priority")).setDefaultValue(0));
    }

    public void setArchiv(Archive archive) {
        this.archive = archive;
    }

    /**
     * Retruns the {@link Archive} for this unpack process.
     * 
     * @return The {@link Archive}.
     */
    Archive getArchive() {
        return archive;
    }

    public void setExtractionController(ExtractionController controller) {
        con = controller;
    }

    public String getArchiveName(DownloadLink link) {
        String match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.pa?r?t?\\.?[0-9]+.rar$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.rar$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.zip$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.7z$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.7z\\.\\d+$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.tar\\.gz$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.tar\\.bz2$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(?i)(.*)\\.r\\d+$").getMatch(0);
        return match;
    }

    public boolean isArchivSupported(String file) {
        if (file.matches("(?i).*\\.pa?r?t?\\.?\\d+.rar$")) return true;
        if (file.matches("(?i).*\\.rar$")) return true;
        if (file.matches("(?i).*\\.zip$")) return true;
        if (file.matches("(?i).*\\.7z$")) return true;
        if (file.matches("(?i).*\\.7z\\.\\d+$")) return true;
        if (file.matches("(?i).*\\.tar\\.gz$")) return true;
        if (file.matches("(?i).*\\.tar\\.bz2$")) return true;
        return false;
    }

    public void close() {
        try {
            inArchive.close();
        } catch (SevenZipException e) {
            e.printStackTrace();
        }
    }

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

    public List<String> filesForPostProcessing() {
        return postprocessing;
    }

    public void setConfig(SubConfiguration config) {
        conf = config;
    }

    /**
     * Helper for the passwordfinding method.
     * 
     * @author botzi
     * 
     */
    private class BooleanHelper {
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
}