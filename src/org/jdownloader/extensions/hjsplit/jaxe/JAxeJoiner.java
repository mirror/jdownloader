/*
 * Copyright (C) 2002 - 2005 Leonardo Ferracci
 *
 * This file is part of JAxe.
 *
 * JAxe is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * JAxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with JAxe; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.  Or, visit http://www.gnu.org/copyleft/gpl.html
 */

package org.jdownloader.extensions.hjsplit.jaxe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class JAxeJoiner extends AxeWriterWorker {
    protected final int BUFFER_SIZE = 1024;
    private boolean bZipped = false;
    protected final int CHUNK_UNIT = 1024;
    protected String sDestDir;
    protected String sFileToJoin;
    protected String sJoinedFile;
    protected String outputFile;
    protected boolean successfull = false;
    protected boolean isCutkiller = false;
    protected String CutKillerExt = null;
    private boolean overwrite = false;

    public void overwriteExistingFile(boolean b) {
        overwrite = b;
    }

    public void setCutKiller(String extension) {
        if (extension == null) {
            isCutkiller = false;
        } else {
            CutKillerExt = extension;
            isCutkiller = true;
        }
    }

    public JAxeJoiner(String sFile) {
        sFileToJoin = sFile;
        sDestDir = new File(sFile).getParent();
    }

    public JAxeJoiner(String sFile, String sDir) {
        sFileToJoin = sFile;
        sDestDir = sDir;
    }

    // @Override
    protected boolean checkNoOverwrite(File f) {
        File fTemp = new File(outputFile);
        boolean b = fTemp.exists();
        if (b && overwrite) {
            fTemp.delete();
            return true;
        }
        return !fTemp.exists();
    }

    // @Override
    protected void computeJobSize() {
        long lReturn = 0;
        int i = 1;
        File fTemp;
        do {
            fTemp = new File(i == 1 ? sFileToJoin : sJoinedFile + "." + formatWidth(i, 3) + (bZipped ? ".zip" : ""));
            lReturn += fTemp.length();
            i++;
        } while (fTemp.exists());

        lJobSize = lReturn - (isCutkiller == true ? 8 : 0);
    }

    protected void doCleanup() {
        new File(outputFile).delete();
    }

    public boolean wasSuccessfull() {
        return successfull;
    }

    // @Override
    public void run() {
        File fToJoin, fTemp = null;
        InputStream is = null;
        ZipInputStream zis;
        ZipEntry ze;
        CountingInputStream cis = null;
        BufferedOutputStream bos;
        int i = 1, nLength;
        byte[] ba = new byte[BUFFER_SIZE];

        bStopped = false;
        fToJoin = new File(sFileToJoin);

        if (!SplitFileFilter.isSplitFile(sFileToJoin) && !SplitFileFilter.isZippedSplitFile(sFileToJoin)) {
            dispatchEvent(new JobErrorEvent(this, "File to join does not seem a split file"));
            return;
        }
        sJoinedFile = SplitFileFilter.getJoinedFileName(sFileToJoin);
        if (isCutkiller) {
            outputFile = sJoinedFile + "." + CutKillerExt;
        } else {
            outputFile = sJoinedFile;
        }
        if (!fToJoin.exists() || fToJoin.isDirectory()) {
            dispatchEvent(new JobErrorEvent(this, "File to join does not exist or is a directory"));
            return;
        }

        if (!checkNoOverwrite(fToJoin)) {
            dispatchEvent(new JobErrorEvent(this, "Error: destination file already exists!"));
            return;
        }

        bZipped = SplitFileFilter.isZippedSplitFile(sFileToJoin);

        try {
            bos = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException fnfe) {
            dispatchEvent(new JobErrorEvent(this, "Error while opening: " + outputFile + " (" + fnfe.getMessage() + ")"));
            return;
        }

        computeJobSize();
        initProgress();
        i = 1;
        try {
            do {
                if (is == null) {
                    if (i == 1) {
                        fTemp = new File(sFileToJoin);
                    } else {
                        fTemp = new File(sJoinedFile + "." + formatWidth(i, 3) + (bZipped ? ".zip" : ""));
                    }

                    if (!fTemp.exists()) {
                        break;
                    } else if (!bZipped) {
                        is = new BufferedInputStream(new FileInputStream(fTemp));
                        if (isCutkiller && i == 1) {
                            /*
                             * cutkiller support, skip first 8 bytes, 3 bytes
                             * extension, 2 byte spaces, 3 bytes number
                             */
                            is.read(ba, 0, 8);
                        }
                    } else {
                        cis = new CountingInputStream(new FileInputStream(fTemp));
                        cis.setTotal(fTemp.length());
                        zis = new ZipInputStream(cis);

                        do {
                            ze = zis.getNextEntry();
                        } while (ze != null && !ze.getName().endsWith("." + formatWidth(i, 3)));

                        if (ze != null) {
                            is = zis;
                        } else {
                            throw new ZipException("Unable to find split entry in zip file");
                        }
                    }
                }

                if (!bStopped) {
                    nLength = is.read(ba, 0, BUFFER_SIZE);
                    if (nLength > 0) {
                        bos.write(ba, 0, nLength);
                        lCurrent += bZipped ? cis.getLastReadAndReset() : nLength;
                        dispatchProgress();
                    } else {
                        i++;
                        is.close();
                        is = null;
                        if (bZipped) {
                            dispatchIncrementalProgress(cis.getDiff());
                        }
                    }
                }
            } while (!bStopped);
        } catch (FileNotFoundException fnfe) {
            dispatchEvent(new JobErrorEvent(this, "Error while opening: " + fTemp.getName()));
            return;
        } catch (ZipException zex) {
            dispatchEvent(new JobErrorEvent(this, "Zip error with file " + fTemp.getName() + " (" + zex.getMessage() + ")"));
            return;
        } catch (IOException ioe) {
            dispatchEvent(new JobErrorEvent(this, "I/O error with file " + fTemp.getName() + " (" + ioe.getMessage() + ")"));
            return;
        } finally {
            try {
                bos.close();
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
            }
        }

        if (bStopped) {
            doCleanup();
            dispatchEvent(new JobEndEvent(this, "Join stopped by user."));
        } else {
            successfull = true;
            dispatchProgress(lJobSize);
            dispatchEvent(new JobEndEvent(this, "Join terminated."));
        }

    }
}