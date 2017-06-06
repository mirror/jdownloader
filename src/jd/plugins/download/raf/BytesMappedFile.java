package jd.plugins.download.raf;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jd.plugins.download.SparseFile;

import org.appwork.utils.Application;
import org.appwork.utils.IO;

public class BytesMappedFile implements FileBytesCacheFlusher {

    public static interface BytesMappedFileCallback {
        public void onFlush(BytesMappedFile bytesMappedFile, IOException ioException);
    }

    protected final File                                          file;

    protected final FileBytesMap                                  bytesMap;

    protected volatile RandomAccessFile                           raf         = null;

    protected final CopyOnWriteArrayList<BytesMappedFileCallback> callBacks   = new CopyOnWriteArrayList<BytesMappedFileCallback>();

    protected final AtomicInteger                                 locks       = new AtomicInteger();

    protected volatile IOException                                ioException = null;

    protected volatile boolean                                    flushFlag   = false;

    private final boolean                                         trySparse;

    public BytesMappedFile(File file) {
        this(file, new FileBytesMap(), true);
    }

    public BytesMappedFile(File file, boolean trySparse) {
        this(file, new FileBytesMap(), trySparse);
    }

    public BytesMappedFile(File file, FileBytesMap fileBytesMap, boolean trySparse) {
        this.file = file;
        bytesMap = fileBytesMap;
        this.trySparse = trySparse;
        final long fileSize = file.length();
        final long finalFileSize = fileBytesMap.getFinalSize();
        final long markedBytes = fileBytesMap.getMarkedBytes();
        if (fileSize < markedBytes || finalFileSize >= 0 && fileSize > finalFileSize) {
            //
            throw new IllegalStateException("File:" + file + "|Length:" + file.length() + "|FileBytesMap:" + fileBytesMap);
        }
    }

    public synchronized void lock() {
        locks.incrementAndGet();
    }

    public synchronized boolean unlock() {
        if (locks.get() > 0) {
            locks.decrementAndGet();
            return true;
        }
        return false;
    }

    public synchronized boolean open(BytesMappedFileCallback callback) throws IOException {
        if (raf == null) {
            try {
                if (trySparse && Application.getJavaVersion() >= Application.JAVA17) {
                    SparseFile.createSparseFile(file);
                }
            } catch (IOException e) {
            }
            raf = IO.open(file, "rw");
            callBacks.add(callback);
            return true;
        } else {
            callBacks.add(callback);
            return false;
        }
    }

    public synchronized boolean isOpen() {
        return raf != null && callBacks.size() > 0;
    }

    public synchronized boolean isLocked() {
        return locks.get() > 0;
    }

    public File getFile() {
        return file;
    }

    public FileBytesMap getFileBytesMap() {
        return bytesMap;
    }

    public synchronized boolean close(BytesMappedFileCallback callback) throws IOException {
        if (callBacks.remove(callback) && callBacks.size() == 0) {
            try {
                if (raf != null) {
                    raf.close();
                }
                return true;
            } finally {
                ioException = null;
                raf = null;
            }
        }
        return false;
    }

    @Override
    public synchronized void flushed() {
        if (raf != null && flushFlag) {
            try {
                if (ioException == null) {
                    raf.getChannel().force(true);
                    onFlushed();
                }
            } catch (IOException e) {
                if (this.ioException != null) {
                    this.ioException = e;
                }
            } finally {
                flushFlag = false;
                for (BytesMappedFileCallback callback : callBacks) {
                    if (callback != null) {
                        callback.onFlush(this, this.ioException);
                    }
                }
            }
        }
    }

    protected void onFlushed() throws IOException {
    }

    @Override
    public synchronized void flush(byte[] writeCache, int writeCachePosition, int length, long fileWritePosition) {
        if (raf != null && ioException == null) {
            try {
                final long skippable = bytesMap.skippable(fileWritePosition, length);
                if (skippable < length) {
                    final int cachePosition = writeCachePosition + (int) skippable;
                    final int writeLength = length - (int) skippable;
                    final long filePosition = fileWritePosition + skippable;
                    raf.seek(filePosition);
                    raf.write(writeCache, cachePosition, writeLength);
                    bytesMap.mark(filePosition, writeLength);
                    flushFlag = true;
                }
            } catch (IOException e) {
                ioException = e;
            }
        }
    }

}
