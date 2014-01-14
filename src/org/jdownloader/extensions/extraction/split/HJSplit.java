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

package org.jdownloader.extensions.extraction.split;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.SevenZipException;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.StringFormatter;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtSevenZipException;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.Signature;

/**
 * Joins HJSplit files.
 * 
 * @author botzi
 * 
 */
public class HJSplit extends IExtraction {

    public Archive buildArchive(ArchiveFactory link) {
        String pattern = "^" + Regex.escape(link.getFilePath().replaceAll("(?i)\\.[\\d]+$", "")) + "\\.[\\d]+$";
        Archive a = SplitUtil.buildArchive(link, pattern, ".*\\.001$");
        a.setName(getArchiveName(link));
        return a;
    }

    // @Override
    // public Archive buildDummyArchive(String file) {
    // Archive a = SplitUtil.buildDummyArchive(file, ".*\\.[\\d]+$",
    // ".*\\.001$");
    // a.setExtractor(this);
    // return a;
    // }

    @Override
    public boolean findPassword(ExtractionController controller, String password, boolean optimized) {
        return true;
    }

    @Override
    public void extract(ExtractionController ctrl) {
        try {
            File f = archive.getFactory().toFile(archive.getFirstArchiveFile().getFilePath().replaceFirst("\\.[\\d]+$", ""));
            String extension = SplitUtil.getCutKillerExtension(archive.getFactory().toFile(archive.getFirstArchiveFile().getFilePath()), archive.getArchiveFiles().size());
            boolean ret;
            if (extension != null) {
                f = new File(f.getAbsolutePath() + "." + extension);
                ret = SplitUtil.merge(controller, f, 8, config);
            } else {
                ret = SplitUtil.merge(controller, f, 0, config);
            }
            if (ret) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_SUCCESS);
            }
        } catch (ExtSevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(e.getExitCode());
        } catch (SevenZipException e) {
            setException(e);
            logger.log(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        }
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

    public String getArchiveName(ArchiveFactory factory) {
        return createID(factory);

    }

    public boolean isArchivSupported(ArchiveFactory factory, boolean allowDeepInspection) {
        String file = factory.getFilePath();
        boolean ret = false;
        Archive a = null;
        if (file.matches(".*\\.[\\d]+$")) {
            if (file.matches("(?i).*\\.7z\\.\\d+$")) {
                ret = false;
            } else if (file.matches("(?i).*\\..+?\\.\\d+$")) {
                ret = true;
            } else {
                a = buildArchive(factory);
                if (a.getFirstArchiveFile() != null && a.getArchiveFiles().size() > 1) {
                    ret = true;
                }
            }
        }
        if (ret && allowDeepInspection) {
            if (a == null) {
                a = buildArchive(factory);
                if (a.getFirstArchiveFile() == null || a.getArchiveFiles().size() <= 1) {
                    ret = false;
                }
            }
            if (ret) {
                String path = a.getFirstArchiveFile().getFilePath();
                File firstFile = new File(path);
                if (firstFile.exists() && firstFile.isFile()) {
                    Signature signature = null;
                    try {
                        String sig = FileSignatures.readFileSignature(firstFile);
                        signature = new FileSignatures().getSignature(sig);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    if (signature != null) {
                        if ("7Z".equalsIgnoreCase(signature.getId())) ret = false;
                        if ("RAR".equalsIgnoreCase(signature.getId())) ret = false;
                    }
                }
            }
        }
        return ret;
    }

    // @Override
    // public boolean isArchivSupportedFileFilter(String file) {
    // if (file.matches(".*\\.001$")) {
    // if (file.matches("(?i).*\\.7z\\.001$")) {
    // return false;
    // } else {
    // return true;
    // }
    // }
    // return false;
    // }

    public void close() {
    }

    public DummyArchive checkComplete(Archive archive) {
        int last = 1;
        List<String> missing = new ArrayList<String>();
        List<Integer> erg = new ArrayList<Integer>();
        String path = archive.getFirstArchiveFile() == null ? archive.getArchiveFiles().get(0).getFilePath() : archive.getFirstArchiveFile().getFilePath();
        Regex r = new Regex(path, ".*\\.([\\d]+)$");
        int length = r.getMatch(0).length();
        String archivename = archive.getName();

        for (ArchiveFile l : archive.getArchiveFiles()) {
            String e = "";
            if ((e = new Regex(l.getFilePath(), ".*\\.([\\d]+)$").getMatch(0)) != null) {
                int p = Integer.parseInt(e);

                if (p > last) last = p;

                erg.add(p);
            }
        }

        for (int i = 1; i <= last; i++) {
            if (!erg.contains(i)) {
                String part = archivename + "." + StringFormatter.fillString(i + "", "0", "", length);
                missing.add(part);
            }
        }

        return DummyArchive.create(archive, missing);
    }

    @Override
    public String createID(ArchiveFactory factory) {
        return factory.getName().replaceFirst("\\.[\\d]+$", "");
    }

    @Override
    public boolean isMultiPartArchive(ArchiveFactory factory) {
        return true;
    }

}