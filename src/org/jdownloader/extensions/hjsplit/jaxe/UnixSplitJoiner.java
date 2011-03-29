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

public class UnixSplitJoiner extends JAxeJoiner {

    public UnixSplitJoiner(String sFile) {
        super(sFile);
        sDestDir = new File(sFile).getParent();
    }

    public UnixSplitJoiner(String sFile, String sDir) {
        super(sFile, sDir);
    }

    // @Override
    protected boolean checkNoOverwrite(File f) {
        File fTemp = new File(outputFile);

        return !fTemp.exists();
    }

    // @Override
    protected void computeJobSize() {
        long lReturn = 0;
        int i = 0;
        File fTemp;

        do {
            fTemp = new File(sJoinedFile + getSuffix(i));
            lReturn += fTemp.length();
            i++;
        } while (fTemp.exists());

        lJobSize = lReturn - (isCutkiller == true ? 8 : 0);
    }

    // @Override
    protected void doCleanup() {
        new File(outputFile).delete();
    }

    private String getSuffix(int n) {
        char[] ca = new char[2];

        ca[0] = (char) ('a' + n / 26);
        ca[1] = (char) ('a' + n % 26);

        return "." + new String(ca);
    }

    // @Override
    public void run() {
        File fToJoin, fTemp = null;
        InputStream is = null;
        BufferedOutputStream bos;
        int i = 1, nLength;
        byte[] ba = new byte[BUFFER_SIZE];

        bStopped = false;
        fToJoin = new File(sFileToJoin);

        if (!UnixSplitFileFilter.isSplitFile(sFileToJoin)) {
            dispatchEvent(new JobErrorEvent(this, "File to join does not seem a file split using Unix split"));
            return;
        }
        sJoinedFile = UnixSplitFileFilter.getJoinedFileName(sFileToJoin);
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

        try {
            bos = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException fnfe) {
            dispatchEvent(new JobErrorEvent(this, "Error while opening: " + outputFile + " (" + fnfe.getMessage() + ")"));
            return;
        }

        computeJobSize();
        System.out.println("Job size: " + lJobSize);
        initProgress();
        i = 0;
        try {
            do {
                if (is == null) {
                    if (i == 0) {
                        fTemp = new File(sFileToJoin);
                    } else {
                        fTemp = new File(sJoinedFile + getSuffix(i));
                    }

                    if (!fTemp.exists()) {
                        break;
                    } else {
                        is = new BufferedInputStream(new FileInputStream(fTemp));
                    }
                }

                if (!bStopped) {
                    nLength = is.read(ba, 0, BUFFER_SIZE);
                    if (nLength > 0) {
                        bos.write(ba, 0, nLength);
                        lCurrent += nLength;
                        dispatchProgress();
                    } else {
                        i++;
                        is.close();
                        is = null;
                    }
                }
            } while (!bStopped);
        } catch (FileNotFoundException fnfe) {
            dispatchEvent(new JobErrorEvent(this, "Error while opening: " + fTemp.getName()));
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
            dispatchProgress(lJobSize);
            dispatchEvent(new JobEndEvent(this, "Join terminated."));
        }
    }
}