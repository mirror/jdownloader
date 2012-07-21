package org.jdownloader.extensions.extraction.multi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

public class OpenMultipartArchiveRar {
    private static class ArchiveOpenVolumeCallback implements IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword {

        /**
         * Cache for opened file streams
         */
        private Map<String, RandomAccessFile> openedRandomAccessFileList = new HashMap<String, RandomAccessFile>();

        /**
         * Name of the last volume returned by {@link #getStream(String)}
         */
        private String                        name;

        /**
         * This method should at least provide the name of the last opened
         * volume (propID=PropID.NAME).
         * 
         * @see IArchiveOpenVolumeCallback#getProperty(PropID)
         */
        public Object getProperty(PropID propID) throws SevenZipException {
            switch (propID) {
            case NAME:
                return name;
            }
            return null;
        }

        /**
         * The name of the required volume will be calculated out of the name of
         * the first volume and a volume index. In case of RAR file, the
         * substring ".partNN." in the name of the volume file will indicate a
         * volume with id NN. For example:
         * <ul>
         * <li>test.rar - single part archive or multi-part archive with a
         * single volume</li>
         * <li>test.part23.rar - 23-th part of a multi-part archive</li>
         * <li>test.part001.rar - first part of a multi-part archive. "00"
         * indicates, that at least 100 volumes must exist.</li>
         * </ul>
         */
        public IInStream getStream(String filename) throws SevenZipException {
            try {
                // We use caching of opened streams, so check cache first
                RandomAccessFile randomAccessFile = openedRandomAccessFileList.get(filename);
                if (randomAccessFile != null) { // Cache hit.
                    // Move the file pointer back to the beginning
                    // in order to emulating new stream
                    randomAccessFile.seek(0);

                    // Save current volume name in case getProperty() will be
                    // called
                    name = filename;

                    return new RandomAccessFileInStream(randomAccessFile);
                }
                
                // Nothing useful in cache. Open required volume.
                randomAccessFile = new RandomAccessFile(filename, "r");

                // Put new stream in the cache
                openedRandomAccessFileList.put(filename, randomAccessFile);

                // Save current volume name in case getProperty() will be called
                name = filename;
                return new RandomAccessFileInStream(randomAccessFile);
            } catch (FileNotFoundException fileNotFoundException) {
                // Required volume doesn't exist. This happens if the volume:
                // 1. never exists. 7-Zip doesn't know how many volumes should
                // exist, so it have to try each volume.
                // 2. should be there, but doesn't. This is an error case.

                // Since normal and error cases are possible,
                // we can't throw an error message
                return null; // We return always null in this case
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Close all opened streams
         */
        void close() throws IOException {
            for (RandomAccessFile file : openedRandomAccessFileList.values()) {
                file.close();
            }
        }

        public void setCompleted(Long files, Long bytes) throws SevenZipException {
        }

        public void setTotal(Long files, Long bytes) throws SevenZipException {
        }

        @Override
        public String cryptoGetTextPassword() throws SevenZipException {
            return "123456789012345678901234567890";
        }
    }

    public static void main(String[] args) {

        ArchiveOpenVolumeCallback archiveOpenVolumeCallback = null;
        ISevenZipInArchive inArchive = null;
        try {

            archiveOpenVolumeCallback = new ArchiveOpenVolumeCallback();
            IInStream inStream = archiveOpenVolumeCallback.getStream("C:/Users/Thomas/Downloads/putty062.rar");
            inArchive = SevenZip.openInArchive(ArchiveFormat.RAR, inStream, archiveOpenVolumeCallback);

            System.out.println("   Size   | Compr.Sz. | Filename");
            System.out.println("----------+-----------+---------");
            int itemCount = inArchive.getNumberOfItems();
            for (int i = 0; i < itemCount; i++) {
                System.out.println(String.format("%9s | %9s | %s", inArchive.getProperty(i, PropID.SIZE), inArchive.getProperty(i, PropID.PACKED_SIZE), inArchive.getProperty(i, PropID.PATH)));
            }
        } catch (Exception e) {
            System.err.println("Error occurs: " + e);
            System.exit(1);
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException e) {
                    System.err.println("Error closing archive: " + e);
                }
            }
            if (archiveOpenVolumeCallback != null) {
                try {
                    archiveOpenVolumeCallback.close();
                } catch (IOException e) {
                    System.err.println("Error closing file: " + e);
                }
            }
        }
    }
}