package org.jdownloader.downloader.hds;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

/*
 * Useful resources:
 *
 * https://github.com/K-S-V/Scripts/blob/master/AdobeHDS.php
 * http://download.macromedia.com/f4v/video_file_format_spec_v10_1.pdf

 * http://code.google.com/p/mp-onlinevideos2/source/browse/trunk/
 * http://svn.wordrider.net/svn/freerapid-plugins/trunk/src/adobehds/cz/vity/freerapid/plugins/services/adobehds/
 * 
 */
public class HDSDownloader extends DownloadInterface {

    private static final int                  PACKAGE_AUDIO          = 0x08;
    private static final int                  PACKAGE_VIDEO          = 0x09;
    private static final int                  PACKAGE_SCRIPT         = 0x12;
    private static final int                  CODEC_ID_AAC           = 0x0a;
    private static final int                  CODEC_ID_AVC           = 0x07;
    private static final int                  AVC_SEQUENCE_HEADER    = 0x00;
    private static final int                  AVC_NALU               = 0x01;
    private static final int                  AVC_SEQUENCE_END       = 0x02;
    private static final int                  FRAME_TYPE_INFO        = 0x05;
    private static final int                  FLV_PACKET_HEADER_SIZE = 11;
    private static final byte[]               FLV_HEADER             = new byte[] { 'F', 'L', 'V', 0x01, 0x05, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00 };

    private DataInputStream                   stream;
    private boolean                           finished;

    private boolean                           aacHeaderWritten;
    private boolean                           avcHeaderWritten;
    private Browser                           br;
    private String                            url;
    private int                               counter;

    private ByteBuffer                        buffer;
    private long                              bytesWritten           = 0l;
    private DownloadLinkDownloadable          downloadable;
    private DownloadLink                      link;
    private long                              startTimeStamp;
    private Logger                            logger;
    private URLConnectionAdapter              currentConnection;
    private ManagedThrottledConnectionHandler connectionHandler;
    private File                              outputCompleteFile;
    private File                              outputFinalCompleteFile;
    private File                              outputPartFile;
    private OutputStream                      outStream;
    private PluginException                   caughtPluginException;

    public HDSDownloader(final DownloadLink link, Browser br, String url) {
        this.br = br;
        this.url = url;
        counter = 1;

        this.link = link;

        connectionHandler = new ManagedThrottledConnectionHandler();
        downloadable = new DownloadLinkDownloadable(link) {
            @Override
            public boolean isResumable() {
                return link.getBooleanProperty("RESUME", true);
            }

            @Override
            public void setResumeable(boolean value) {
                link.setProperty("RESUME", value);
                super.setResumeable(value);
            }
        };
        downloadable.setDownloadInterface(this);
        logger = downloadable.getLogger();
    }

    protected void terminate() {
        if (terminated.getAndSet(true) == false) {
            if (!externalDownloadStop()) {
                logger.severe("A critical Downloaderror occured. Terminate...");
            }
        }
    }

    public void run() throws IOException, PluginException {

        outStream.write(FLV_HEADER);

        buffer = ByteBuffer.allocate(200 * 1024);
        while (true) {
            if (abort.get()) {
                return;
            }
            if (finished) {
                return;
            }
            ByteBuffer buffertoWrite = readAndWrite();
            if (buffertoWrite != null) {
                buffertoWrite.flip();
                outStream.write(buffertoWrite.array(), 0, buffertoWrite.remaining());

            }

        }
    }

    private ByteBuffer readAndWrite() throws IOException, PluginException {

        while (true) {
            if (abort.get()) {
                return null;
            }
            int type = refreshFragmentStream();
            if (finished) {
                return null;
            }
            // UI2 Reserved for FMS, should be 0
            int reserved = (type & 0xC0) >>> 6;
            // Filter UI1 Indicates if packets are filtered.
            // 0 = No pre-processing required.
            // 1 = Pre-processing (such as decryption) of the packet
            // is required before it can be rendered.
            // Shall be 0 in unencrypted files, and 1 for encrypted
            // tags. See Annex F. FLV Encryption for the use of
            // filters.
            int filter = (type & 0x20) >>> 5;
            if (filter > 0) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Preprocessor (Encryption?) is not supported " + (type & 0x1F) + "(" + type + ")", 24 * 60 * 60 * 1000l);
            }
            type = type & 0x1F;

            buffer.clear();
            // DataSize UI24 Length of the message. Number of bytes after
            // StreamID to end of packet (Equal to packet length –
            // 11)
            final int dataSize = readInt24();
            // Timestamp UI24 Time in milliseconds at which the data in this tag
            // applies. This value is relative to the first tag in the FLV
            // file, which always has a timestamp of 0.
            // TimestampExtended UI8 Extension of the Timestamp field to form a SI32 value.
            // This field represents the upper 8 bits, while the
            // previous Timestamp field represents the lower 24 bits
            // of the time in milliseconds.
            final int time = readInt24() | (stream.readUnsignedByte() << 24);
            // StreamID UI24 Always 0.
            final int streamId = readInt24();
            int payloadToRead = dataSize + FLV_PACKET_HEADER_SIZE;

            payloadToRead -= write(type);
            payloadToRead -= writeInt24(dataSize);
            payloadToRead -= writeInt24(time & 0xffffff);
            payloadToRead -= write((time >>> 24));
            payloadToRead -= writeInt24(streamId);
            switch (type) {
            case PACKAGE_AUDIO: {
                final int frameInfo = stream.readUnsignedByte();
                payloadToRead -= write(frameInfo);
                final int codecId = (frameInfo & 0xf0) >>> 4;
                // SoundRate UB [2] Sampling rate. The following values are defined:
                // 0 = 5.5 kHz
                // 1 = 11 kHz
                // 2 = 22 kHz
                // 3 = 44 kHz
                final int soundRate = (frameInfo & 0xC) >>> 2;
                // SoundSize UB [1] Size of each audio sample. This parameter only pertains to
                // uncompressed formats. Compressed formats always decode
                // to 16 bits internally.
                // 0 = 8-bit samples
                // 1 = 16-bit samples
                final int soundSize = (frameInfo & 0x2) >>> 1;

                final int SoundType = (frameInfo & 0x1);
                switch (codecId) {
                case AudioTag.AAC:
                    final int aacType = stream.readUnsignedByte();
                    payloadToRead -= write(aacType);
                    // AACPacketType IF SoundFormat == 10
                    // UI8
                    // The following values are defined:
                    // 0 = AAC sequence
                    if (aacType == AVC_SEQUENCE_HEADER) {
                        if (aacHeaderWritten) {
                            // System.out.println("Skipping AAC sequence header");
                            skipBytes(dataSize - 2 + 4);
                            continue;
                        }
                        aacHeaderWritten = true;
                        // System.out.println("Writing AAC sequence header");

                    }
                    handlePayload(dataSize, payloadToRead);
                    return buffer;
                default:
                    throw new IOException("Unsupported Audio Tag: " + codecId);
                }

            }
            case PACKAGE_VIDEO: {
                final int frameInfo = stream.readUnsignedByte();
                payloadToRead -= write(frameInfo);
                // Frame Type UB [4] Type of video frame. The following values are defined:
                // 1 = key frame (for AVC, a seekable frame)
                // 2 = inter frame (for AVC, a non-seekable frame)
                // 3 = disposable inter frame (H.263 only)
                // 4 = generated key frame (reserved for server use only)
                // 5 = video info/command frame
                final int frameType = (frameInfo & 0xf0) >>> 4;
                if (frameType == FRAME_TYPE_INFO) {
                    // 5 = video info/command frame
                    skipBytes(dataSize - 1 + 4);
                    continue;
                }
                // CodecID UB [4] Codec Identifier. The following values are defined:
                // 2 = Sorenson H.263
                // 3 = Screen video
                // 4 = On2 VP6
                // 5 = On2 VP6 with alpha channel
                // 6 = Screen video version 2
                // 7 = AVC
                final int codecId = frameInfo & 0x0f;
                switch (codecId) {
                case VideoTag.AVC:

                    // AVCPacketType IF CodecID == 7
                    // UI8
                    // The following values are defined:
                    // 0 = AVC sequence header
                    // 1 = AVC NALU
                    // 2 = AVC end of sequence (lower level NALU sequence ender is
                    // not required or supported)
                    final int avcType = stream.readUnsignedByte();
                    payloadToRead -= write(avcType);

                    if (avcType == AVC_SEQUENCE_HEADER) {
                        if (avcHeaderWritten) {
                            // System.out.println("Skipping AVC sequence header");
                            skipBytes(dataSize - 2 + 4);
                            continue;
                        }
                        avcHeaderWritten = true;
                        // System.out.println("Writing AVC sequence header");
                    }

                    handlePayload(dataSize, payloadToRead);
                    return buffer;
                default:
                    throw new IOException("Unsupported Video Tag: " + codecId);
                }

            }
            case 10:
            case 11: {
                throw new IOException("Akamai DRM not supported");
            }
            case PACKAGE_SCRIPT: {
                skipBytes(dataSize + 4);
                continue;
            }
            case 40:
            case 41: {
                throw new IOException("FlashAccess DRM not supported");
            }
            default: {
                throw new IOException("Unknown packet type: 0x" + Integer.toHexString(type));
            }
            }

        }
    }

    public void handlePayload(final int dataSize, int payloadToRead) throws IOException {
        ensureBufferCapacity(payloadToRead);
        stream.readFully(buffer.array(), buffer.position(), payloadToRead);
        buffer.position(buffer.position() + payloadToRead);
        skipBytes(4);
        writeInt32(dataSize + FLV_PACKET_HEADER_SIZE);
    }

    public void ensureBufferCapacity(int payloadToRead) {
        if (payloadToRead > buffer.capacity() - buffer.position()) {
            ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;

        }
    }

    public int refreshFragmentStream() throws IOException {
        int type;
        while (stream == null || (type = stream.read()) == -1) {

            final InputStream stream = nextFragment();
            if (stream == null) {
                finished = true;
                return -1;
            }
            this.stream = new DataInputStream(stream);
        }
        return type;
    }

    private void writeInt32(int i) throws IOException {
        writeBytes((byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i);
    }

    private InputStream nextFragment() throws IOException {
        if (currentConnection != null) {
            currentConnection.disconnect();
        }
        currentConnection = br.openGetConnection(url + "Seg1-Frag" + (counter++));
        if (currentConnection.isOK()) {

            MeteredThrottledInputStream input = new MeteredThrottledInputStream(new F4vInputStream(currentConnection), new AverageSpeedMeter(10));
            connectionHandler.addThrottledConnection(input);
            return input;
        } else {
            currentConnection.disconnect();
            return null;
        }
    }

    private int readInt24() throws IOException {
        int ch1 = stream.read();
        int ch2 = stream.read();
        int ch3 = stream.read();
        if ((ch1 | ch2 | ch3) < 0) {
            throw new EOFException();
        }
        return (ch1 << 16) | (ch2 << 8) | ch3;
    }

    private int writeInt24(final int i) throws IOException {
        return writeBytes((byte) (i >>> 16), (byte) (i >>> 8), (byte) i);

    }

    private int write(int i) throws IOException {
        return writeBytes((byte) i);
    }

    private int writeBytes(byte... bytes) throws IOException {
        buffer.put(bytes);

        return bytes.length;
    }

    private void skipBytes(final int num) throws IOException {
        if (stream.skipBytes(num) != num) {
            throw new EOFException();
        }
    }

    public long getBytesLoaded() {
        return bytesWritten;
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        throw new WTFException("Not needed");
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return getBytesLoaded();
    }

    @Override
    public boolean startDownload() throws Exception {
        try {
            DownloadPluginProgress downloadPluginProgress = null;
            downloadable.setConnectionHandler(this.getManagedConnetionHandler());
            final DiskSpaceReservation reservation = downloadable.createDiskSpaceReservation();
            try {
                if (!downloadable.checkIfWeCanWrite(new ExceptionRunnable() {

                    @Override
                    public void run() throws Exception {
                        downloadable.checkAndReserve(reservation);
                        createOutputChannel();
                        try {
                            downloadable.lockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
                        } catch (FileIsLockedException e) {
                            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                    }
                }, null)) {
                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                }
                startTimeStamp = System.currentTimeMillis();
                downloadPluginProgress = new DownloadPluginProgress(downloadable, this, Color.GREEN.darker());
                downloadable.addPluginProgress(downloadPluginProgress);
                downloadable.setAvailable(AvailableStatus.TRUE);

                run();

            } finally {
                try {
                    downloadable.free(reservation);
                } catch (final Throwable e) {
                    LogSource.exception(logger, e);
                }
                try {
                    downloadable.addDownloadTime(System.currentTimeMillis() - getStartTimeStamp());
                } catch (final Throwable e) {
                }
                downloadable.removePluginProgress(downloadPluginProgress);
            }
            onDownloadReady();

            return handleErrors();
        } finally {
            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
            cleanupDownladInterface();
        }

    }

    protected void error(PluginException pluginException) {
        synchronized (this) {
            /* if we recieved external stop, then we dont have to handle errors */
            if (externalDownloadStop()) {
                return;
            }
            LogSource.exception(logger, pluginException);
            if (caughtPluginException == null) {
                caughtPluginException = pluginException;
            }
        }
        terminate();
    }

    protected void onDownloadReady() throws Exception {

        cleanupDownladInterface();
        if (!handleErrors()) {
            return;
        }

        boolean renameOkay = downloadable.rename(outputPartFile, outputCompleteFile);
        if (!renameOkay) {

            error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR));
        }

    }

    protected void cleanupDownladInterface() {
        try {
            downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
        } catch (final Throwable e) {
        }
        try {
            if (currentConnection != null) {
                currentConnection.disconnect();
            }
        } catch (Throwable e) {
        }
        closeOutputChannel();
    }

    private void closeOutputChannel() {
        try {
            OutputStream loutputPartFileRaf = outStream;
            if (loutputPartFileRaf != null) {
                logger.info("Close File. Let AV programs run");
                loutputPartFileRaf.close();
            }
        } catch (Throwable e) {
            LogSource.exception(logger, e);
        } finally {
            outStream = null;
        }
    }

    private boolean handleErrors() throws PluginException {
        if (externalDownloadStop()) {
            return false;
        }

        if (caughtPluginException == null) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            downloadable.setVerifiedFileSize(outputCompleteFile.length());
            return true;
        } else {
            throw caughtPluginException;
        }
    }

    private void createOutputChannel() throws SkipReasonException {
        try {
            String fileOutput = downloadable.getFileOutput();
            logger.info("createOutputChannel for " + fileOutput);
            String finalFileOutput = downloadable.getFinalFileOutput();
            outputCompleteFile = new File(fileOutput);
            outputFinalCompleteFile = outputCompleteFile;
            if (!fileOutput.equals(finalFileOutput)) {
                outputFinalCompleteFile = new File(finalFileOutput);
            }
            outputPartFile = new File(downloadable.getFileOutputPart());
            outStream = new BufferedOutputStream(new FileOutputStream(outputPartFile)) {
                public synchronized void write(byte[] b, int off, int len) throws IOException {
                    super.write(b, off, len);

                    bytesWritten += len;
                    // byte[] bytes = new byte[len];
                    // System.arraycopy(b, off, bytes, 0, len);
                    // System.out.println(HexFormatter.byteArrayToHex(bytes));
                };
            };
        } catch (Exception e) {
            LogSource.exception(logger, e);
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
    }

    @Override
    public URLConnectionAdapter getConnection() {
        return currentConnection;
    }

    @Override
    public void stopDownload() {
        if (abort.getAndSet(true) == false) {
            logger.info("externalStop recieved");
            terminate();
        }
    }

    private AtomicBoolean abort      = new AtomicBoolean(false);
    private AtomicBoolean terminated = new AtomicBoolean(false);

    @Override
    public boolean externalDownloadStop() {
        return abort.get();
    }

    @Override
    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    @Override
    public void close() {
        if (currentConnection != null) {
            currentConnection.disconnect();
        }
    }

    @Override
    public Downloadable getDownloadable() {
        return downloadable;
    }

    @Override
    public boolean isResumedDownload() {
        return false;
    }

}
