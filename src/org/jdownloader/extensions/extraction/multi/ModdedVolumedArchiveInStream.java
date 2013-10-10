package org.jdownloader.extensions.extraction.multi;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.VolumedArchiveInStream;

public class ModdedVolumedArchiveInStream implements IInStream {

    private long                             absoluteOffset;
    private long                             absoluteLength  = -1;
    private int                              currentIndex    = -1;
    private IInStream                        currentInStream;
    private long                             currentVolumeOffset;
    private long                             currentVolumeLength;
    private List<Long>                       volumePositions = new ArrayList<Long>();
    private final IArchiveOpenVolumeCallback archiveOpenVolumeCallback;
    private String                           cuttedVolumeFilename;

    public ModdedVolumedArchiveInStream(IArchiveOpenVolumeCallback archiveOpenVolumeCallback) throws SevenZipException {
        this((String) archiveOpenVolumeCallback.getProperty(PropID.NAME), archiveOpenVolumeCallback);
    }

    /**
     * Creates instance of {@link VolumedArchiveInStream} using {@link IArchiveOpenVolumeCallback}.
     * 
     * @param firstVolumeFilename
     *            the file name of the first volume.
     * @param archiveOpenVolumeCallback
     *            call back implementation used to access different volumes of archive. The file name should ends with <code>.7z.001</code> or SevenZipException
     *            will be thrown.
     * @throws SevenZipException
     *             in error case
     */
    public ModdedVolumedArchiveInStream(String firstVolumeFilename, IArchiveOpenVolumeCallback archiveOpenVolumeCallback) throws SevenZipException {
        this.archiveOpenVolumeCallback = archiveOpenVolumeCallback;
        volumePositions.add(Long.valueOf(0));
        cuttedVolumeFilename = firstVolumeFilename.substring(0, firstVolumeFilename.length() - 3);
        openVolume(1, true);
    }

    private void openVolume(int index, boolean seekToBegin) throws SevenZipException {
        if (currentIndex == index) { return; }
        for (int i = volumePositions.size(); i < index && absoluteLength == -1; i++) {
            openVolume(i, false);
        }
        if (absoluteLength != -1 && volumePositions.size() <= index) { return; }
        String volumeFilename = cuttedVolumeFilename + MessageFormat.format("{0,number,000}", Integer.valueOf(index));
        // Get new IInStream
        IInStream newInStream = archiveOpenVolumeCallback.getStream(volumeFilename);
        if (newInStream == null) {
            absoluteLength = volumePositions.get(volumePositions.size() - 1).longValue();
            return;
        }
        currentInStream = newInStream;
        if (volumePositions.size() == index) {
            // Determine volume size
            currentVolumeLength = currentInStream.seek(0, SEEK_END);
            if (currentVolumeLength == 0) { throw new RuntimeException("Volume " + index + " is empty"); }
            volumePositions.add(Long.valueOf(volumePositions.get(index - 1).longValue() + currentVolumeLength));
            if (seekToBegin) {
                currentInStream.seek(0, SEEK_SET);
            }
        } else {
            currentVolumeLength = volumePositions.get(index).longValue() - volumePositions.get(index - 1).longValue();
        }
        if (seekToBegin) {
            currentVolumeOffset = 0;
            absoluteOffset = volumePositions.get(index - 1).longValue();
        }
        currentIndex = index;
    }

    // 0------X------Y------Z length=4
    // 0______1______2______3 - list index
    // 1______2______3______4 - volume
    private void openVolumeToAbsoluteOffset() throws SevenZipException {
        int index = volumePositions.size() - 1;
        if (absoluteLength != -1 && absoluteOffset >= absoluteLength) { return; }
        while (volumePositions.get(index).longValue() > absoluteOffset) {
            index--;
        }
        if (index < volumePositions.size() - 1) {
            openVolume(index + 1, false);
            return;
        }
        do {
            index++;
            openVolume(index, false);
        } while ((absoluteLength == -1 || absoluteOffset < absoluteLength) && volumePositions.get(index).longValue() <= absoluteOffset);
    }

    /**
     * ${@inheritDoc}
     */
    public long seek(long offset, int seekOrigin) throws SevenZipException {
        long newOffset;
        boolean proceedWithSeek = false;
        switch (seekOrigin) {
        case SEEK_SET:
            newOffset = offset;
            break;
        case SEEK_CUR:
            newOffset = absoluteOffset + offset;
            break;
        case SEEK_END:
            if (absoluteLength == -1) {
                openVolume(Integer.MAX_VALUE, false);
                proceedWithSeek = true;
            }
            newOffset = absoluteLength + offset;
            break;
        default:
            throw new RuntimeException("Seek: unknown origin: " + seekOrigin);
        }
        if (newOffset == absoluteOffset && !proceedWithSeek) { return newOffset; }
        absoluteOffset = newOffset;
        openVolumeToAbsoluteOffset();
        if (absoluteLength != -1 && absoluteLength <= absoluteOffset) {
            absoluteOffset = absoluteLength;
            return absoluteLength;
        }
        currentVolumeOffset = absoluteOffset - volumePositions.get(currentIndex - 1).longValue();
        currentInStream.seek(currentVolumeOffset, SEEK_SET);
        return newOffset;
    }

    /**
     * ${@inheritDoc}
     */
    public int read(byte[] data) throws SevenZipException {
        if (absoluteLength != -1 && absoluteOffset >= absoluteLength) { return 0; }
        int read = currentInStream.read(data);
        absoluteOffset += read;
        currentVolumeOffset += read;
        if (currentVolumeOffset >= currentVolumeLength) {
            openVolume(currentIndex + 1, true);
        }
        return read;
    }
}
