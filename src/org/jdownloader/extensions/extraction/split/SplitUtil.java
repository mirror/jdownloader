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

import jd.utils.JDHexUtils;

import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.appwork.utils.ReusableByteArrayOutputStreamPool;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.content.PackedFile;

/**
 * Utils for the joiner.
 * 
 * @author botzi
 * 
 */
class SplitUtil {

    /**
     * Abstract method to build an archive from. The ArchiveFile has to be in the DownloadList.
     * 
     * @param link
     *            ArchiveFile from the event.
     * @param pattern
     *            Regex to find all files.
     * @param startfile
     *            Regex for the startfile.
     * @return The archive.
     */
    static Archive buildArchive(ArchiveFactory link, String pattern, String startfile) {
        Archive archive = link.createArchive();

        // if (link instanceof DummyArchiveFile) {
        // for (File f : new
        // File(link.getFileOutput()).getParentFile().listFiles()) {
        // if (f.isDirectory()) continue;
        // if (new Regex(f.getAbsolutePath(), pattern,
        // Pattern.CASE_INSENSITIVE).matches()) {
        // matches.add(buildArchiveFileFromFile(f.getAbsolutePath()));
        // }
        // }
        // } else {

        // final Pattern pat = Pattern.compile(pattern,
        // Pattern.CASE_INSENSITIVE);
        // List<ArchiveFile> links =
        // DownloadController.getInstance().getChildrenByFilter(new
        // AbstractPackageChildrenNodeFilter<ArchiveFile>() {
        //
        // public boolean isChildrenNodeFiltered(ArchiveFile node) {
        // return pat.matcher(node.getFileOutput()).matches();
        // }
        //
        // public int returnMaxResults() {
        // return 0;
        // }
        // });

        // }

        archive.setArchiveFiles(link.createPartFileList(startfile, pattern));

        for (ArchiveFile l : archive.getArchiveFiles()) {
            if (new Regex(l.getFilePath(), startfile, Pattern.CASE_INSENSITIVE).matches()) {
                archive.setFirstArchiveFile(l);
                break;
            }
        }

        return archive;
    }

    // private static ArchiveFile buildArchiveFileFromFile(String file) {
    // File file0 = new File(file);
    // DummyArchiveFile link = new DummyArchiveFile(file0.getName());
    // link.setFile(file0);
    // return link;
    // }

    // /**
    // * Abstract method to build an archive from. The file has to come from
    // Menu.
    // *
    // * @param file
    // * Filepath
    // * @param pattern
    // * Regex to find all files.
    // * @param startfile
    // * Regex for the startfile.
    // * @return The archive.
    // */
    // static Archive buildDummyArchive(final String file, String pattern,
    // String startfile) {
    // List<ArchiveFile> links =
    // DownloadController.getInstance().getChildrenByFilter(new
    // AbstractPackageChildrenNodeFilter<ArchiveFile>() {
    //
    // public boolean isChildrenNodeFiltered(ArchiveFile node) {
    // if (node.getFileOutput().equals(file)) {
    // if (node.getLinkStatus().hasStatus(LinkStatus.FINISHED)) return true;
    // }
    // return false;
    // }
    //
    // public int returnMaxResults() {
    // return 1;
    // }
    // });
    // ArchiveFile link = null;
    // if (links == null || links.size() == 0) {
    // link = buildArchiveFileFromFile(file);
    // } else {
    // link = links.get(0);
    // }
    // return buildArchive(link, pattern, startfile);
    // }

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
    static boolean merge(ExtractionController controller, File file, int start, ExtractionConfig config) {
        CPUPriority priority = config.getCPUPriority();
        if (priority == null || CPUPriority.HIGH.equals(priority)) {
            priority = null;
        }
        Archive archive = controller.getArchiv();
        List<String> files = new ArrayList<String>();
        long size = 0l;
        for (ArchiveFile l : archive.getArchiveFiles()) {
            files.add(l.getFilePath());
            size += new File(l.getFilePath()).length() - start;
        }
        controller.getArchiv().getContentView().add(new PackedFile(false, archive.getName(), size));
        controller.setProgress(0.0d);
        Collections.sort(files);
        long progressInBytes = 0l;
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        ReusableByteArrayOutputStream writeBuffer = null;
        ReusableByteArrayOutputStream readBuffer = null;
        try {
            /*
             * write buffer, use same as downloadbuffer, so we have a pool of same sized buffers
             */
            int maxbuffersize = config.getBufferSize() * 1024;
            writeBuffer = ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(Math.max(maxbuffersize, 10240), false);
            /* read buffer, we use 64kb here which should be okay */
            readBuffer = ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(64 * 1024, true);
            if (file.exists()) {
                if (controller.isOverwriteFiles()) {
                    if (!file.delete()) {
                        archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                        return false;
                    }
                } else {
                    archive.addExtractedFiles(file);
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_OUTPUTFILE_EXIST);
                    return false;
                }
            }

            if ((!file.getParentFile().exists() && !file.getParentFile().mkdirs()) || !file.createNewFile()) {
                archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR);
                return false;
            }

            archive.addExtractedFiles(file);
            fos = new FileOutputStream(file);
            final ReusableByteArrayOutputStream fwriteBuffer = writeBuffer;
            bos = new BufferedOutputStream(fos, 1) {
                {
                    this.buf = fwriteBuffer.getInternalBuffer();
                }
            };

            for (int i = 0; i < files.size(); i++) {
                File source = new File(files.get(i));
                FileInputStream in = null;
                try {
                    in = new FileInputStream(source);
                    if (start > 0) {
                        in.skip(start);
                    }

                    int l = 0;
                    while ((l = in.read(readBuffer.getInternalBuffer())) >= 0) {
                        if (l == 0) {
                            /* nothing read, we wait a moment and see again */
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                throw new IOException(e);
                            }
                            continue;
                        }
                        bos.write(readBuffer.getInternalBuffer(), 0, l);
                        progressInBytes += l;
                        controller.setProgress(progressInBytes / size);
                        if (controller.gotKilled()) throw new IOException("Extraction has been aborted!");
                        if (priority != null && !CPUPriority.HIGH.equals(priority)) {
                            try {
                                Thread.sleep(priority.getTime());
                            } catch (InterruptedException e) {
                                throw new IOException(e);
                            }
                        }
                    }
                } finally {
                    try {
                        in.close();
                    } catch (final Throwable e) {
                    }
                }
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
            controller.setProgress(100.0d);
            try {
                bos.flush();
            } catch (Throwable e) {
            }
            try {
                bos.close();
            } catch (Throwable e) {
            }
            try {
                ReusableByteArrayOutputStreamPool.reuseReusableByteArrayOutputStream(writeBuffer);
            } catch (Throwable e) {
            }
            try {
                ReusableByteArrayOutputStreamPool.reuseReusableByteArrayOutputStream(readBuffer);
            } catch (Throwable e) {
            }
            try {
                fos.flush();
            } catch (Throwable e) {
            }
            try {
                fos.close();
            } catch (Throwable e) {
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