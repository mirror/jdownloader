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

package jd.plugins.optional.extraction.split;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import jd.nutils.io.FileSignatures;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.optional.extraction.Archive;
import jd.plugins.optional.extraction.DummyDownloadLink;
import jd.plugins.optional.extraction.ExtractionController;
import jd.plugins.optional.extraction.ExtractionControllerConstants;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;

/**
 * Utils for the joiner.
 * 
 * @author botzi
 * 
 */
class SplitUtil {
    private final static int BUFFER = 1024;

    /**
     * Abstract method to build an archive from. The DownloadLink has to be in
     * the DownloadList.
     * 
     * @param link
     *            DownloadLink from the event.
     * @param pattern
     *            Regex to find all files.
     * @param startfile
     *            Regex for the startfile.
     * @return The archive.
     */
    static Archive buildArchive(DownloadLink link, String pattern, String startfile) {
        Archive archive = new Archive();

        ArrayList<DownloadLink> matches = new ArrayList<DownloadLink>();

        if (link instanceof DummyDownloadLink) {
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

        archive.setDownloadLinks(matches);

        for (DownloadLink l : matches) {
            if (new Regex(l.getFileOutput(), startfile, Pattern.CASE_INSENSITIVE).matches()) {
                archive.setFirstDownloadLink(l);
                break;
            }
        }

        return archive;
    }

    private static DownloadLink buildDownloadLinkFromFile(String file) {
        File file0 = new File(file);
        DummyDownloadLink link = new DummyDownloadLink(file0.getName());
        link.setFile(file0);
        return link;
    }

    /**
     * Abstract method to build an archive from. The file has to come from Menu.
     * 
     * @param file
     *            Filepath
     * @param pattern
     *            Regex to find all files.
     * @param startfile
     *            Regex for the startfile.
     * @return The archive.
     */
    static Archive buildDummyArchive(String file, String pattern, String startfile) {
        DownloadLink link = JDUtilities.getController().getDownloadLinkByFileOutput(new File(file), LinkStatus.FINISHED);
        if (link == null) {
            link = buildDownloadLinkFromFile(file);
        }
        return buildArchive(link, pattern, startfile);
    }

    /**
     * Merges the files from the archive. The filepaths need to be sortable.
     * 
     * @param controller
     * @param file
     *            The outputfile.
     * @param start
     *            The amount of bytes that should be skipped.
     * @return
     */
    static boolean merge(ExtractionController controller, File file, int start) {
        byte[] buffer = new byte[BUFFER];
        Archive archive = controller.getArchiv();

        List<String> files = new ArrayList<String>();

        for (DownloadLink l : archive.getDownloadLinks()) {
            files.add(l.getFileOutput());
        }

        Collections.sort(files);

        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            if (file.exists()) {
                if (archive.isOverwriteFiles()) {
                    if (!file.delete()) {
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                        return false;
                    }
                } else {
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_OUTPUTFILE_EXIST);
                    return false;
                }
            }

            if ((!file.getParentFile().exists() && !file.getParentFile().mkdirs()) || !file.createNewFile()) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                return false;
            }

            archive.addExtractedFiles(file);

            out = new BufferedOutputStream(new FileOutputStream(file));

            for (int i = 0; i < files.size(); i++) {
                File source = new File(files.get(i));
                in = new BufferedInputStream(new FileInputStream(source));

                if (start > 0) {
                    in.skip(start);
                }

                long length = source.length();
                long read = 0;

                if (start > 0) {
                    length = length - start;
                }

                while (length > read) {
                    int buf = ((length - read) < BUFFER) ? (int) (length - read) : BUFFER;
                    int l = in.read(buffer, 0, buf);
                    out.write(buffer, 0, l);
                    out.flush();
                    archive.setExtracted(archive.getExtracted() + l);
                    read += l;
                }

                in.close();
            }
        } catch (FileNotFoundException e) {
            controller.setExeption(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            return false;
        } catch (IOException e) {
            controller.setExeption(e);
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            return false;
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException e) {
                controller.setExeption(e);
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR);
            }
        }

        return true;
    }

    /**
     * Returns the Cutkillerextension.
     * 
     * @param file
     *            Startarchive.
     * @param filecount
     *            Number of files of the archive.
     * @return The extension. Null if the file is not a CutKliller file.
     */
    static String getCutKillerExtension(File file, int filecount) {
        String sig = null;
        try {
            sig = JDHexUtils.toString(FileSignatures.readFileSignature(file));
        } catch (IOException e) {
            return null;
        }

        if (sig == null) return null;

        if (new Regex(sig, "[\\w]{3}  \\d+").matches()) {
            String count = new Regex(sig, ".*?  (\\d+)").getMatch(0);
            if (count == null) return null;
            if (filecount != Integer.parseInt(count)) return null;
            String ext = new Regex(sig, "(.*?) ").getMatch(0);
            if (ext == null) return null;
            return ext;
        }
        return null;
    }
}