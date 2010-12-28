package jd.plugins.optional.extraction.hjsplit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.config.ConfigContainer;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.nutils.Formatter;
import jd.nutils.io.FileSignatures;
import jd.nutils.io.Signature;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.optional.extraction.Archive;
import jd.plugins.optional.extraction.ExtractionController;
import jd.plugins.optional.extraction.ExtractionControllerConstants;
import jd.plugins.optional.extraction.IExtraction;
import jd.plugins.optional.extraction.hjsplit.jaxe.JAxeJoiner;
import jd.plugins.optional.extraction.hjsplit.jaxe.JoinerFactory;
import jd.plugins.optional.extraction.hjsplit.jaxe.ProgressEvent;
import jd.plugins.optional.extraction.hjsplit.jaxe.ProgressEventListener;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

public class HJSplt implements IExtraction {
    private static final String DUMMY_HOSTER = "dum.my";

    private Archive             archive;
    private List<String>        postprocessing;

    public HJSplt() {
        postprocessing = new ArrayList<String>();
    }

    public Archive buildArchive(DownloadLink link) {
        Archive a = new Archive();

        File file = new File(link.getFileOutput());

        file = this.getStartFile(file);

        ArrayList<File> files = getFileList(file);
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String startfile = getStartFile(new File(link.getFileOutput())).getAbsolutePath();

        for (File f : files) {
            DownloadLink l = JDUtilities.getController().getDownloadLinkByFileOutput(f.getAbsoluteFile(), LinkStatus.FINISHED);
            if (l == null) {
                l = buildDownloadLinkFromFile(f.getAbsolutePath());
            }

            if (startfile.equals(f.getAbsolutePath())) {
                a.setFirstDownloadLink(l);
            }

            ret.add(l);
        }

        a.setDownloadLinks(ret);
        a.setProtected(false);

        return a;
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
        return true;
    }

    public void extract() {
        File first = new File(archive.getFirstDownloadLink().getFileOutput());
        JAxeJoiner join = JoinerFactory.getJoiner(first);

        String cutKillerExt = getCutkillerExtension(first, archive.getDownloadLinks().size());
        join.setCutKiller(cutKillerExt);
        join.setProgressEventListener(new ProgressEventListener() {
            public void handleEvent(ProgressEvent pe) {
                archive.setExtracted(pe.getCurrent());
                archive.setSize(pe.getMax());
            }
        });
        join.overwriteExistingFile(archive.isOverwriteFiles());

        join.run();

        if (!join.wasSuccessfull()) {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        }

        postprocessing.add(getOutputFile(first).getAbsolutePath());
    }

    public boolean checkCommand() {
        return true;
    }

    public int getCrackProgress() {
        return 100;
    }

    public boolean prepare() {
        return true;
    }

    public void initConfig(ConfigContainer config, SubConfiguration subConfig) {
    }

    public void setArchiv(Archive archive) {
        this.archive = archive;
    }

    public void setExtractionController(ExtractionController controller) {
    }

    public String getArchiveName(DownloadLink link) {
        return null;
    }

    public boolean isArchivSupported(String file) {
        switch (getArchiveType(new File(file))) {
        case NONE:
            return false;
        default:
            return true;
        }
    }

    public boolean isArchivSupportedFileFilter(String file) {
        return isStartVolume(new File(file));
    }

    public List<String> filesForPostProcessing() {
        return postprocessing;
    }

    public void setConfig(SubConfiguration config) {
    }

    public void close() {
    }

    private static enum ARCHIV_TYPE {
        NONE, NORMAL, UNIX, CUTKILLER, XTREMSPLIT
    }

    /**
     * Gibt die zu entpackende Datei zurück.
     * 
     * @param file
     * @return
     */
    private File getOutputFile(File file) {
        ARCHIV_TYPE type = getArchiveType(file);
        switch (type) {
        case XTREMSPLIT:
            /* maybe parse header here */
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.\\d+\\.xtm$", ""));
        case UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ""));
        case NORMAL:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", ""));
        default:
            return null;
        }
    }

    /**
     * Validiert ein Archiv. Archive haben zwei formen: unix: *.aa..*.ab..*.ac .
     * das zweite a wird hochgezählt normal: *.001...*.002
     * 
     * Die Funktion versucht zu prüfen ob das Archiv komplett heruntergeladen
     * wurde und ob es ein gültoges Archiv ist.
     * 
     * @param file
     * @return
     */
    private boolean validateArchive(File file) {
        File startFile = getStartFile(file);
        if (startFile == null || !startFile.exists() || !startFile.isFile()) return false;
        ARCHIV_TYPE type = getArchiveType(file);

        switch (type) {
        case UNIX:
            return typeCrossCheck(validateUnixType(startFile)) != null;
        case NORMAL:
            return typeCrossCheck(validateNormalType(startFile)) != null;
        default:
            return false;
        }
    }

    /**
     * Gibt alle files die zum Archiv von file gehören zurück
     * 
     * @param file
     * @return
     */
    private ArrayList<File> getFileList(File file) {

        File startFile = getStartFile(file);
        if (startFile == null || !startFile.exists() || !startFile.isFile()) return null;
        ARCHIV_TYPE type = getArchiveType(file);

        switch (type) {
        case UNIX:
            return validateUnixType(startFile);
        case NORMAL:
            return validateNormalType(startFile);
        default:
            return null;
        }
    }

    /**
     * Validiert typ normal (siehe validateArchiv)
     * 
     * @param file
     * @return
     */
    private ArrayList<File> validateNormalType(File file) {
        final String matcher = file.getName().replaceAll("\\[|\\]|\\(|\\)|\\?", ".").replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\\\.[\\\\d]+$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getDownloadLinksByNamePattern(matcher);
        if (missing == null) return null;
        for (DownloadLink miss : missing) {
            /* do not continue if we have an unfinished file here */
            if (!miss.getLinkStatus().isFinished()) return null;
            File par1 = new File(miss.getFileOutput()).getParentFile();
            File par2 = file.getParentFile();
            if (par1.equals(par2)) {

                if (!new File(miss.getFileOutput()).exists()) { return null; }
            }
        }
        File[] files = file.getParentFile().listFiles(new java.io.FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().matches(matcher)) { return true; }
                return false;
            }
        });
        int c = 1;
        ArrayList<File> ret = new ArrayList<File>();
        for (int i = 0; i < files.length; i++) {
            String volume = Formatter.fillString(c + "", "0", "", 3);
            File newFile;
            if ((newFile = new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\." + volume + "$1"))).exists()) {
                c++;
                ret.add(newFile);
            } else {
                return null;
            }
        }
        /*
         * securitycheck for missing file on disk but in downloadlist, will
         * check for next possible filename
         */
        String volume = Formatter.fillString(c + "", "0", "", 3);
        if (JDUtilities.getController().getDownloadLinkByFileOutput(new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\." + volume + "$1")), null) != null) return null;
        return ret;
    }

    private ArrayList<File> typeCrossCheck(ArrayList<File> files) {
        if (files == null) return null;
        int ArchiveCheckFailed = 0;
        for (File file : files) {
            try {
                Signature fs = FileSignatures.getFileSignature(file);
                if (fs != null && (fs.getId().equals("RAR") || fs.getId().equals("﻿7Z"))) {
                    ArchiveCheckFailed++;
                }
            } catch (IOException e) {
            }
        }
        if (ArchiveCheckFailed > 1) {
            /*
             * mehr als 1 mal sollte kein Rar oder 7zip header signatur gefunden
             * werden
             */
            return null;
        }
        return files;
    }

    /**
     * Validiert das archiv auf 2 arten 1. wird ind er downloadliste nach
     * passenden unfertigen archiven gesucht 2. wird das archiv durchnummeriert
     * und geprüft ob es lücken/fehlende files gibts siehe (validateArchiv)
     * 
     * @param file
     * @return
     */
    private ArrayList<File> validateUnixType(File file) {

        final String matcher = file.getName().replaceAll("\\[|\\]|\\(|\\)|\\?", ".").replaceFirst("\\.a.($|\\..*)", "\\\\.a.$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getDownloadLinksByNamePattern(matcher);
        if (missing == null) return null;
        for (DownloadLink miss : missing) {
            /* do not continue if we have an unfinished file here */
            if (!miss.getLinkStatus().isFinished()) return null;
            if (new File(miss.getFileOutput()).exists() && new File(miss.getFileOutput()).getParentFile().equals(file.getParentFile())) continue;
            return null;
        }
        File[] files = file.getParentFile().listFiles(new java.io.FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().matches(matcher)) { return true; }
                return false;
            }
        });
        ArrayList<File> ret = new ArrayList<File>();
        char c = 'a';
        for (int i = 0; i < files.length; i++) {
            File newFile;
            if ((newFile = new File(file.getParentFile(), file.getName().replaceFirst("\\.a.($|\\..*)", "\\.a" + c + "$1"))).exists()) {
                ret.add(newFile);
                c++;
            } else {
                return null;
            }
        }
        /*
         * securitycheck for missing file on disk but in downloadlist , will
         * check for next possible filename
         */
        if (JDUtilities.getController().getDownloadLinkByFileOutput(new File(file.getParentFile(), file.getName().replaceFirst("\\.a.($|\\..*)", "\\.a" + c + "$1")), null) != null) return null;
        return ret;
    }

    /**
     * Sucht den Dateinamen und den Pfad der des Startvolumes heraus
     * 
     * @param file
     * @return
     */
    private File getStartFile(File file) {
        ARCHIV_TYPE type = getArchiveType(file);
        switch (type) {
        case XTREMSPLIT:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.\\d+\\.xtm$", ".001.xtm"));
        case UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ".aa"));
        case NORMAL:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", ".001$1"));
        default:
            return null;
        }
    }

    /**
     * Gibt zurück ob es sich bei der Datei um ein hjsplit Startvolume handelt.
     * 
     * @param file
     * @return
     */
    private boolean isStartVolume(File file) {
        if (file.getName().matches(".*\\.aa$")) return true;
        if (file.getName().matches(".*\\.001\\.xtm$")) return true;
        if (file.getName().matches(".*\\.001(\\.[^\\d]*)?$")) return true;
        if (file.getName().matches(".*\\.001$")) return true;
        return false;
    }

    /**
     * Gibt den Archivtyp zurück. möglich sind: ARCHIVE_TYPE_7Z (bad)
     * ARCHIVE_TYPE_NONE (bad) ARCHIVE_TYPE_UNIX ARCHIVE_TYPE_NORMAL
     * 
     * @param file
     * @return
     */
    private ARCHIV_TYPE getArchiveType(File file) {
        String name = file.getName();
        if (name.matches(".*\\.a.$")) return ARCHIV_TYPE.UNIX;
        if (name.matches(".*\\.\\d+\\.xtm$")) return ARCHIV_TYPE.XTREMSPLIT;
        /* eg. bla.001.rar */
        if (name.matches(".*\\.[\\d]+($|\\.[^\\d]{1,5}$)")) return ARCHIV_TYPE.NORMAL;
        return ARCHIV_TYPE.NONE;
    }

    /**
     * returns String with fileextension if we find a valid cutkiller fileheader
     * returns null if no cutkiller fileheader found
     * 
     * @param file
     * @return
     */
    private String getCutkillerExtension(File file, int filecount) {
        File startFile = getStartFile(file);
        if (startFile == null) return null;
        String sig = null;
        try {
            sig = JDHexUtils.toString(FileSignatures.readFileSignature(startFile));
        } catch (IOException e) {
            JDLogger.exception(e);
            return null;
        }
        if (new Regex(sig, "[\\w]{3}  \\d+").matches()) {
            String count = new Regex(sig, ".*?  (\\d+)").getMatch(0);
            if (count == null) return null;
            if (filecount != Integer.parseInt(count)) return null;
            String ext = new Regex(sig, "(.*?) ").getMatch(0);
            return ext;
        }
        return null;
    }
}