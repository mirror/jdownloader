package org.jdownloader.extensions.extraction.multi;

import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionControllerConstants;
import org.jdownloader.extensions.extraction.Item;

public class Seven7ExtractCallback implements IArchiveExtractCallback, ICryptoGetTextPassword {

    protected final ISequentialOutStream[]   outStreams;
    protected final ExtractionController     ctrl;
    protected final ISevenZipInArchive       inArchive;
    protected final String                   password;
    protected long                           progressInBytes           = 0;
    protected final Archive                  archive;
    protected final long                     size2;
    protected final ExtractionConfig         config;
    protected int                            lastIndex                 = -1;
    protected final ExtractOperationResult[] results;
    protected final Multi                    multi;
    protected final ISimpleInArchiveItem[]   items;
    protected final AtomicBoolean            error                     = new AtomicBoolean(false);
    protected final LogSource                logger;
    protected final boolean                  slowDownWorkaroundNeeded;
    protected final static long              SLOWDOWNWORKAROUNDTIMEOUT = 100;

    public ExtractOperationResult getResult(int index) {
        return results[index];
    }

    public boolean isResultMissing() {
        for (int index = 0; index < results.length; index++) {
            ExtractOperationResult result = results[index];
            if (result == null) {
                ISimpleInArchiveItem item = items[index];
                try {
                    if (item == null || item.isFolder()) continue;
                } catch (SevenZipException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    public Seven7ExtractCallback(Multi multi, ISevenZipInArchive inArchive, ExtractionController ctrl, Archive archive, ExtractionConfig config) throws SevenZipException {
        outStreams = new ISequentialOutStream[inArchive.getNumberOfItems()];
        results = new ExtractOperationResult[inArchive.getNumberOfItems()];
        items = new ISimpleInArchiveItem[inArchive.getNumberOfItems()];
        this.inArchive = inArchive;
        slowDownWorkaroundNeeded = ArchiveFormat.SEVEN_ZIP == inArchive.getArchiveFormat();
        this.ctrl = ctrl;
        this.archive = archive;
        if (StringUtils.isEmpty(archive.getFinalPassword())) {
            this.password = "";
        } else {
            this.password = archive.getFinalPassword();
        }
        this.config = config;
        this.multi = multi;
        logger = multi.getLogger();
        size2 = archive.getContentView().getTotalSize();
    }

    @Override
    public void setCompleted(long arg0) throws SevenZipException {
    }

    @Override
    public void setTotal(long arg0) throws SevenZipException {
    }

    @Override
    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }

    @Override
    public ISequentialOutStream getStream(final int index, ExtractAskMode extractaskmode) throws SevenZipException {
        if (lastIndex >= 0) {
            ISequentialOutStream ret = outStreams[lastIndex];
            if (lastIndex == index) return ret;
            if (ret != null) {
                /* close previous opened MultiCallback */
                outStreams[lastIndex] = null;
                try {
                    if (ret instanceof MultiCallback) ((MultiCallback) ret).close();
                } catch (final Throwable e) {
                }
            }
        }
        lastIndex = index;
        if (ctrl.gotKilled()) { throw new SevenZipException("Extraction has been aborted"); }
        if (error.get()) throw new SevenZipException("Extraction error");
        ISequentialOutStream ret = outStreams[index];
        if (ret == null) {
            if (extractaskmode != ExtractAskMode.EXTRACT) {
                /* null is only permitted in non extract mode */
                return null;
            }
            final Boolean isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
            final String path = (String) inArchive.getProperty(index, PropID.PATH);
            if (StringUtils.isEmpty(path)) { throw new SevenZipException("path is null"); }
            final Long itemSize = (Long) inArchive.getProperty(index, PropID.SIZE);
            final Date lastWriteTime = (Date) inArchive.getProperty(index, PropID.LAST_WRITE_TIME);
            final Boolean itemEncrypted = (Boolean) inArchive.getProperty(index, PropID.ENCRYPTED);
            ISimpleInArchiveItem item = new ISimpleInArchiveItem() {

                @Override
                public String getPath() throws SevenZipException {
                    return path;
                }

                @Override
                public Long getSize() throws SevenZipException {
                    return itemSize;
                }

                @Override
                public Long getPackedSize() throws SevenZipException {
                    return null;
                }

                @Override
                public boolean isFolder() throws SevenZipException {
                    return isFolder;
                }

                @Override
                public Integer getAttributes() throws SevenZipException {
                    return null;
                }

                @Override
                public Date getCreationTime() throws SevenZipException {
                    return null;
                }

                @Override
                public Date getLastAccessTime() throws SevenZipException {
                    return lastWriteTime;
                }

                @Override
                public Date getLastWriteTime() throws SevenZipException {
                    return null;
                }

                @Override
                public boolean isEncrypted() throws SevenZipException {
                    return itemEncrypted;
                }

                @Override
                public Boolean isCommented() throws SevenZipException {
                    return null;
                }

                @Override
                public Integer getCRC() throws SevenZipException {
                    return null;
                }

                @Override
                public String getMethod() throws SevenZipException {
                    return null;
                }

                @Override
                public Integer getPosition() throws SevenZipException {
                    return null;
                }

                @Override
                public String getHostOS() throws SevenZipException {
                    return null;
                }

                @Override
                public String getUser() throws SevenZipException {
                    return null;
                }

                @Override
                public String getGroup() throws SevenZipException {
                    return null;
                }

                @Override
                public String getComment() throws SevenZipException {
                    return null;
                }

                @Override
                public ExtractOperationResult extractSlow(ISequentialOutStream isequentialoutstream) throws SevenZipException {
                    return null;
                }

                @Override
                public ExtractOperationResult extractSlow(ISequentialOutStream isequentialoutstream, String s) throws SevenZipException {
                    return null;
                }

                @Override
                public int getItemIndex() {
                    return index;
                }

            };
            items[index] = item;
            try {
                if (Boolean.TRUE.equals(isFolder)) {
                    /* we need dummy outstream, null may crash java */
                    ret = getNullOutputStream();
                } else {
                    AtomicBoolean skipped = new AtomicBoolean(false);
                    final File extractTo = multi.getExtractFilePath(item, ctrl, skipped, size2);
                    ctrl.setCurrentActiveItem(new Item(path, item.getSize(), extractTo));
                    if (skipped.get()) {
                        ret = getNullOutputStream();
                    } else {
                        if (extractTo == null) throw new SevenZipException("Extraction error, extractTo == null");
                        archive.addExtractedFiles(extractTo);
                        ret = new MultiCallback(extractTo, ctrl, config, false) {
                            {
                                if (slowDownWorkaroundNeeded) {
                                    priority = null;
                                }
                            }

                            @Override
                            public int write(byte[] data) throws SevenZipException {
                                try {
                                    if (slowDownWorkaroundNeeded) {
                                        synchronized (this) {
                                            try {
                                                wait(SLOWDOWNWORKAROUNDTIMEOUT);
                                            } catch (InterruptedException e) {
                                                throw new SevenZipException(e);
                                            }
                                        }
                                    }
                                    if (ctrl.gotKilled()) throw new SevenZipException("Extraction has been aborted");
                                    return super.write(data);
                                } finally {
                                    progressInBytes += data.length;
                                    ctrl.setProgress(progressInBytes / (double) size2);
                                }
                            }

                        };
                    }
                }
                outStreams[index] = ret;
            } catch (Throwable e) {
                error.set(true);
                if (e instanceof SevenZipException) throw (SevenZipException) e;
                throw new SevenZipException(e);
            }
        }
        return ret;
    }

    protected ISequentialOutStream getNullOutputStream() {
        if (slowDownWorkaroundNeeded) {
            return new ISequentialOutStream() {
                @Override
                public int write(byte[] data) throws SevenZipException {
                    try {
                        synchronized (this) {
                            try {
                                wait(SLOWDOWNWORKAROUNDTIMEOUT);
                            } catch (InterruptedException e) {
                                throw new SevenZipException(e);
                            }
                        }
                        if (ctrl.gotKilled()) throw new SevenZipException("Extraction has been aborted");
                        return data.length;
                    } finally {
                        progressInBytes += data.length;
                        ctrl.setProgress(progressInBytes / (double) size2);
                    }
                }
            };
        } else {
            return new ISequentialOutStream() {
                @Override
                public int write(byte[] data) throws SevenZipException {
                    try {
                        if (ctrl.gotKilled()) throw new SevenZipException("Extraction has been aborted");
                        return data.length;
                    } finally {
                        progressInBytes += data.length;
                        ctrl.setProgress(progressInBytes / (double) size2);
                    }
                }
            };
        }

    }

    public boolean hasError() {
        return error.get();
    }

    @Override
    public void prepareOperation(ExtractAskMode arg0) throws SevenZipException {
    }

    @Override
    public void setOperationResult(ExtractOperationResult res) throws SevenZipException {
        try {
            if (lastIndex >= 0) {
                ISequentialOutStream ret = outStreams[lastIndex];
                if (ret != null) {
                    /* close previous opened MultiCallback */
                    outStreams[lastIndex] = null;
                    try {
                        if (ret instanceof MultiCallback) ((MultiCallback) ret).close();
                    } catch (final Throwable e) {
                    }
                }
                results[lastIndex] = res;
                ISimpleInArchiveItem item = items[lastIndex];
                MultiCallback callback = null;
                if (item != null && ret != null) {
                    if (ret instanceof MultiCallback) callback = (MultiCallback) ret;
                    if (callback != null && item.getSize() != callback.getWritten()) {
                        logger.info("Size missmatch for " + item.getPath() + " is " + callback.getWritten() + " but should be " + item.getSize());
                        if (ExtractOperationResult.OK == res) {
                            logger.info("Size missmatch for " + item.getPath() + ", but Extraction returned OK?! Archive seems incomplete");
                            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_INCOMPLETE_ERROR);
                            throw new SevenZipException("Extraction error");
                        }
                    }
                }
                switch (res) {
                case OK:
                    /* extraction successfully ,continue with next file */
                    if (callback != null) multi.setLastModifiedDate(item, callback.getFile());
                    break;
                case CRCERROR:
                    if (item != null) {
                        logger.info("CRC Error in " + item.getPath());
                    }
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_CRC_ERROR);
                    if (item != null) {
                        throw new SevenZipException("CRC-Extraction error in " + item.getPath());
                    } else {
                        throw new SevenZipException("CRC-Extraction error");
                    }
                case UNSUPPORTEDMETHOD:
                    if (item != null) {
                        logger.info("Unsupported Method " + item.getMethod() + " in " + item.getPath());
                    }
                    /* seven7binding does not support all compression methods! */
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    if (item != null) {
                        throw new SevenZipException("Unsupported Method " + item.getMethod() + " in " + item.getPath());
                    } else {
                        throw new SevenZipException("Unsupported Method");
                    }
                default:
                    archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
                    throw new SevenZipException("Extraction error");
                }
            }
        } catch (final Throwable e) {
            error.set(true);
            if (e instanceof SevenZipException) throw (SevenZipException) e;
            throw new SevenZipException(e);
        }
    }

    public void close() {
        if (outStreams != null) {
            for (ISequentialOutStream outStream : outStreams) {
                if (outStream != null) {
                    try {
                        if (outStream instanceof MultiCallback) ((MultiCallback) outStream).close();
                    } catch (Throwable e) {
                    }
                }
            }
        }
    }

}
