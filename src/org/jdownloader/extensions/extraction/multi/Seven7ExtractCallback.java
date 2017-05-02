package org.jdownloader.extensions.extraction.multi;

import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.ISequentialOutStream;
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

    protected final ISequentialOutStream[]     outStreams;
    protected final ExtractionController       ctrl;
    protected final SevenZipArchiveWrapper     archiveWrapper;
    protected final String                     password;
    protected final Archive                    archive;
    protected final ExtractionConfig           config;
    protected int                              lastIndex                 = -1;
    protected final ExtractOperationResult[]   results;
    protected final Multi                      multi;
    protected final ISimpleInArchiveItem[]     items;
    protected final AtomicReference<Throwable> error                     = new AtomicReference<Throwable>();
    protected final LogSource                  logger;
    protected final boolean                    slowDownWorkaroundNeeded;
    protected final static long                SLOWDOWNWORKAROUNDTIMEOUT = 150;

    public ExtractOperationResult getResult(int index) {
        return results[index];
    }

    public boolean isResultMissing() {
        for (int index = 0; index < results.length; index++) {
            ExtractOperationResult result = results[index];
            if (result == null) {
                ISimpleInArchiveItem item = items[index];
                try {
                    if (item == null || item.isFolder()) {
                        continue;
                    }
                } catch (SevenZipException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    public Seven7ExtractCallback(Multi multi, SevenZipArchiveWrapper archiveWrapper, ExtractionController ctrl, Archive archive, ExtractionConfig config) throws SevenZipException {
        final int numberOfItems = archiveWrapper.getNumberOfItems();
        outStreams = new ISequentialOutStream[numberOfItems];
        results = new ExtractOperationResult[numberOfItems];
        items = new ISimpleInArchiveItem[numberOfItems];
        this.archiveWrapper = archiveWrapper;
        slowDownWorkaroundNeeded = archiveWrapper.isSlowDownWorkaroundNeeded();
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
            if (lastIndex == index) {
                return ret;
            }
            if (ret != null) {
                /* close previous opened MultiCallback */
                outStreams[lastIndex] = null;
                try {
                    if (ret instanceof MultiCallback) {
                        ((MultiCallback) ret).close();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        lastIndex = index;
        if (ctrl.gotKilled()) {
            throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
        }
        if (error.get() != null) {
            throw new SevenZipException(error.get());
        }
        ISequentialOutStream ret = outStreams[index];
        if (ret == null) {
            final Integer attributes = archiveWrapper.getAttributes(index);
            final Boolean isFolder = archiveWrapper.isFolder(index);
            final String path = archiveWrapper.getPath(index);
            final Long itemSize = archiveWrapper.getSize(index);
            final Date lastWriteTime = archiveWrapper.getLastWriteTime(index);
            final Boolean itemEncrypted = archiveWrapper.isEncrypted(index);
            final String method = archiveWrapper.getMethod(index);
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
                    return Boolean.TRUE.equals(isFolder);
                }

                @Override
                public Integer getAttributes() throws SevenZipException {
                    return attributes;
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
                    return Boolean.TRUE.equals(itemEncrypted);
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
                    return method;
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
                if (Boolean.TRUE.equals(isFolder) || extractaskmode != ExtractAskMode.EXTRACT) {
                    /* we need dummy outstream, null may crash java */
                    ret = getNullOutputStream(false);
                } else {
                    final AtomicBoolean skippedFlag = new AtomicBoolean(false);
                    final File extractTo = multi.getExtractFilePath(item, ctrl, skippedFlag);
                    ctrl.setCurrentActiveItem(new Item(path, item.getSize(), extractTo));
                    if (skippedFlag.get()) {
                        ret = getNullOutputStream(true);
                    } else {
                        if (extractTo == null) {
                            throw new SevenZipException("Extraction error, extractTo == null");
                        }
                        ret = new MultiCallback(extractTo, ctrl, config) {

                            @Override
                            protected void waitCPUPriority() throws SevenZipException {
                                if (!slowDownWorkaroundNeeded) {
                                    super.waitCPUPriority();
                                } else {
                                    synchronized (this) {
                                        try {
                                            wait(SLOWDOWNWORKAROUNDTIMEOUT);
                                        } catch (InterruptedException e) {
                                            throw new SevenZipException(e);
                                        }
                                    }
                                }
                            }

                            @Override
                            public int write(final byte[] data) throws SevenZipException {
                                if (ctrl.gotKilled()) {
                                    throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                                }
                                final int ret = super.write(data);
                                ctrl.addAndGetProcessedBytes(ret);
                                return ret;
                            }

                        };
                        archive.addExtractedFiles(extractTo);
                    }
                }
                outStreams[index] = ret;
            } catch (Throwable e) {
                error.set(e);
                if (e instanceof SevenZipException) {
                    throw (SevenZipException) e;
                }
                throw new SevenZipException(e);
            }
        }
        return ret;
    }

    protected ISequentialOutStream getNullOutputStream(final boolean countBytesAsProcessed) {
        if (slowDownWorkaroundNeeded) {
            return new ISequentialOutStream() {
                @Override
                public int write(final byte[] data) throws SevenZipException {
                    synchronized (this) {
                        try {
                            wait(SLOWDOWNWORKAROUNDTIMEOUT);
                        } catch (InterruptedException e) {
                            throw new SevenZipException(e);
                        }
                    }
                    if (ctrl.gotKilled()) {
                        throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                    }
                    if (countBytesAsProcessed) {
                        ctrl.addAndGetProcessedBytes(data.length);
                    }
                    return data.length;
                }
            };
        } else {
            return new ISequentialOutStream() {
                @Override
                public int write(final byte[] data) throws SevenZipException {
                    if (ctrl.gotKilled()) {
                        throw new MultiSevenZipException("Extraction has been aborted", ExtractionControllerConstants.EXIT_CODE_USER_BREAK);
                    }
                    if (countBytesAsProcessed) {
                        ctrl.addAndGetProcessedBytes(data.length);
                    }
                    return data.length;
                }
            };
        }

    }

    public boolean hasError() {
        return getError() != null;
    }

    public Throwable getError() {
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
                        if (ret instanceof MultiCallback) {
                            ((MultiCallback) ret).close();
                        }
                    } catch (final Throwable e) {
                    }
                }
                results[lastIndex] = res;
                ISimpleInArchiveItem item = items[lastIndex];
                MultiCallback callback = null;
                if (item != null && ret != null) {
                    if (ret instanceof MultiCallback) {
                        callback = (MultiCallback) ret;
                    }
                    final Long size = item.getSize();
                    if (callback != null && size != null && size != callback.getWritten()) {
                        logger.info("Size missmatch for " + item.getPath() + " is " + callback.getWritten() + " but should be " + size);
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
                    if (callback != null) {
                        multi.setLastModifiedDate(item, callback.getFile());
                        multi.setPermissions(item, callback.getFile());
                    }
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
            error.set(e);
            if (e instanceof SevenZipException) {
                throw (SevenZipException) e;
            }
            throw new SevenZipException(e);
        }
    }

    public void close() {
        if (outStreams != null) {
            for (ISequentialOutStream outStream : outStreams) {
                if (outStream != null) {
                    try {
                        if (outStream instanceof MultiCallback) {
                            ((MultiCallback) outStream).close();
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        }
    }

}
