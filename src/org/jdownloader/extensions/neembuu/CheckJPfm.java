/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import jpfm.FormatterEvent;
import jpfm.MountListener;
import jpfm.fs.SimpleReadOnlyFileSystem;
import jpfm.mount.Mount;
import jpfm.mount.MountParams;
import jpfm.mount.MountParamsBuilder;
import jpfm.mount.Mounts;
import jpfm.operations.readwrite.SimpleReadRequest;
import jpfm.util.PreferredMountTypeUtil;
import jpfm.volume.VeryBigFile;
import jpfm.volume.vector.VectorRootDirectory;

/**
 * 
 * @author Shashank Tulsyan
 */
final class CheckJPfm {

    static final boolean checkVirtualFileSystemCompatibility(final Logger logger) {
        try {
            VectorRootDirectory vrd = new VectorRootDirectory();
            VeryBigFile vbf = new VeryBigFile(vrd);
            vrd.add(vbf);
            SimpleReadOnlyFileSystem fs = new SimpleReadOnlyFileSystem(vrd);

            String mntLoc = System.getProperty("java.io.tmpdir");
            if (mntLoc.endsWith(File.separator))
                ;
            else
                mntLoc = mntLoc + File.separatorChar;
            mntLoc = mntLoc + Math.random() + ".neembuutest";
            File f = new File(mntLoc);
            f.deleteOnExit();
            if (!f.exists()) {
                if (!PreferredMountTypeUtil.isFolderAPreferredMountLocation()) {
                    f.createNewFile();
                } else {
                    f.mkdir();
                }
            } else {
                throw new IllegalStateException("Mountlocation already exists");
                // this would never happen
            }
            Mount m = Mounts.mount(new MountParamsBuilder().set(MountParams.ParamType.MOUNT_LOCATION, mntLoc).set(MountParams.ParamType.FILE_SYSTEM, fs).set(MountParams.ParamType.LISTENER, new MountListener() {

                // @Override
                public void eventOccurred(FormatterEvent event) {
                    /*
                     * logger.log(Level.INFO, event.toString(),
                     * event.getException());
                     */
                }
            }).build());

            Thread.sleep(100);

            if (m.isMounted()) {
                File virtualVeryBigFile = new File(m.getMountLocation().getAsFile(), vbf.getName());
                FileChannel fc = new RandomAccessFile(virtualVeryBigFile, "r").getChannel();
                ByteBuffer bb_virtual = ByteBuffer.allocate(1024);
                long offset = (long) (Math.random() * vbf.getFileSize());
                if (offset != 0) {
                    offset = -1;// to avoid read at filesize
                }
                offset = 0;// vbf is extremely large. java cannot handle it so
                           // we read at 0
                fc.position(offset);
                int r = fc.read(bb_virtual);
                if (r == -1) { return false; }

                ByteBuffer bb_actual = ByteBuffer.allocate(1);
                SimpleReadRequest readRequest = new SimpleReadRequest(bb_actual, offset);
                vbf.read(readRequest);

                m.unMount();
                logger.log(Level.INFO, "at offset = " + offset + " read actual=" + bb_actual.get(0) + " virtual=" + bb_virtual.get(0));
                if (bb_actual.get(0) == bb_virtual.get(0)) { return true; }
            }

            return false;
        } catch (Exception a) {
            logger.log(Level.SEVERE, "JPfm not working for some reason", a);
            return false;
        }
    }
}
