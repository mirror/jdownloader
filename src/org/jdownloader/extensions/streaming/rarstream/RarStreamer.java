package org.jdownloader.extensions.streaming.rarstream;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.ReusableByteArrayOutputStream;
import org.appwork.utils.StringUtils;
import org.appwork.utils.io.streamingio.StreamingChunk;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveItem;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.Signature;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.StreamingProvider;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.gui.views.ArraySet;
import org.jdownloader.logging.LogController;

public class RarStreamer implements Runnable {

    private Archive              archive;
    private ISevenZipInArchive   rarArchive;
    private LogSource            logger;
    protected StreamingProvider  streamProvider;
    private StreamingExtension   extension;
    private ISimpleInArchiveItem bestItem     = null;
    protected RarStreamProvider  rarStreamProvider;
    private StreamingChunk       streamingChunk;
    private Thread               extractionThread;
    private ExtractionExtension  extractor;
    private FileSignatures       fileSignatures;
    private String               pathToStream = null;
    private Throwable            exception;
    private long                 bestItemSize = -1l;

    public RarStreamer(Archive archive, StreamingExtension extension) {
        this(archive, extension, (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension());
    }

    public RarStreamer(Archive archive, StreamingExtension extension, ExtractionExtension extractionExtension) {
        this.archive = archive;
        this.extension = extension;
        this.streamProvider = extension.getStreamProvider();
        extractor = extractionExtension;
        logger = LogController.CL();
    }

    public void start() {
        new Thread(this, "RarStreamer").start();
    }

    @Override
    public void run() {
        try {

            String ID = getID();

            openArchiveInDialog();

            // extension.getVlcstreamingAPI().addHandler(ID, this);
            rarStreamProvider.setReadyForExtract(true);

            extract();

        } catch (Throwable e) {
            logger.log(e);
            exception = e;
            showWarning(e.getMessage());

        }

    }

    public Throwable getException() {
        return exception;
    }

    public String getID() {
        return "extract" + archive.getFactory().getID();
    }

    public void openArchive() throws ExtractionException, DialogClosedException, DialogCanceledException, InterruptedException {
        open();
        if (archive.isProtected()) {

            List<String> spwList = archive.getSettings().getPasswords();
            ArraySet<String> passwordList = new ArraySet<String>();
            if (spwList != null) {
                passwordList.addAll(spwList);
            }
            passwordList.addAll(archive.getFactory().getGuessedPasswordList(archive));
            passwordList.add(archive.getName());
            java.util.List<String> pwList = extractor.getSettings().getPasswordList();
            if (pwList == null) pwList = new ArrayList<String>();
            passwordList.addAll(pwList);

            logger.info("Start password finding for " + archive);

            String correctPW = null;

            for (String password : passwordList) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                if (checkPassword(password)) {
                    correctPW = password;
                    break;
                }
            }

            if (correctPW == null) {
                String password = askPassword();
                while (true) {
                    if (checkPassword(password)) {
                        break;
                    } else {
                        showWarning(T._.wrong_password());

                    }
                }
            }
            try {
                if (bestItem == null) {
                    logger.info("Path: " + pathToStream);
                    for (ISimpleInArchiveItem item : rarArchive.getSimpleInterface().getArchiveItems()) {
                        if (pathToStream == null) {
                            if (bestItem == null && !item.isFolder()) {
                                bestItem = item;
                                bestItemSize = item.getSize();
                            } else if (!item.isFolder() && bestItem != null && bestItem.getSize() < item.getSize()) {
                                bestItem = item;
                                bestItemSize = item.getSize();
                            }
                        } else {
                            if (pathToStream.replace("/", "\\").equals(item.getPath().replace("/", "\\"))) {
                                bestItem = item;
                                bestItemSize = item.getSize();
                            } else {
                                logger.info(pathToStream + "!=" + item.getPath());
                            }
                        }

                        if (item.isEncrypted()) {
                            archive.setProtected(true);
                        }
                    }
                    logger.info("best item: " + bestItem.getPath() + " size: " + bestItem.getSize());
                }

            } catch (Throwable e) {
                logger.log(e);
                throw new ExtractionException(e, streamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
            }
            logger.info("Found password for " + archive + "->" + archive.getFinalPassword());
            /* avoid duplicates */
            pwList.remove(archive.getFinalPassword());
            pwList.add(0, archive.getFinalPassword());

            extractor.getSettings().setPasswordList(pwList);
        }
    }

    protected void showWarning(String wrong_password) {
        Dialog.getInstance().showMessageDialog(wrong_password);
    }

    protected void openArchiveInDialog() throws DialogClosedException, DialogCanceledException, ExtractionException {
        ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            @Override
            public void run() throws Exception {
                openArchive();

            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };

        ProgressDialog d = new ProgressDialog(pg, UIOManager.BUTTONS_HIDE_CANCEL, T._.open_rar(), T._.open_rar_msg(), null, null, null) {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 40);
            }

        };
        Dialog.getInstance().showDialog(d);
        if (d.getThrowable() != null) {

        throw new ExtractionException(d.getThrowable(), streamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null); }

    }

    /**
     * Helper for the passwordfinding method.
     * 
     * @author botzi
     * 
     */
    private static class BooleanHelper {
        private boolean bool;

        BooleanHelper() {
            bool = false;
        }

        /**
         * Marks that the boolean was found.
         */
        void found() {
            bool = true;
        }

        /**
         * Returns the result.
         * 
         * @return The result.
         */
        boolean getBoolean() {
            return bool;
        }
    }

    public FileSignatures getFileSignatures() {
        if (fileSignatures == null) fileSignatures = new FileSignatures();
        return fileSignatures;
    }

    protected boolean checkPassword(String password) throws ExtractionException {

        ReusableByteArrayOutputStream buffer = null;
        try {
            buffer = new ReusableByteArrayOutputStream(64 * 1024);
            try {
                rarStreamProvider.close();
            } catch (final Throwable e) {
            }
            try {
                rarArchive.close();
            } catch (Throwable e) {
            }
            try {
                streamingChunk.close();
            } catch (Throwable e) {
            }

            rarStreamProvider = new RarStreamProvider(archive, password, this.streamProvider);
            IInStream rarStream = rarStreamProvider.getStream(archive.getFirstArchiveFile());
            rarArchive = SevenZip.openInArchive(ArchiveFormat.RAR, rarStream, rarStreamProvider);

            final BooleanHelper passwordfound = new BooleanHelper();
            HashSet<String> checkedExtensions = new HashSet<String>();
            for (final ISimpleInArchiveItem item : rarArchive.getSimpleInterface().getArchiveItems()) {
                if (item.isFolder() || (item.getSize() == 0 && item.getPackedSize() == 0)) {
                    /*
                     * we also check for items with size ==0, they should have a packedsize>0
                     */
                    continue;
                }
                if (Thread.currentThread().isInterrupted()) {
                    /* extraction got aborted */
                    throw new InterruptedException();
                }
                final String path = item.getPath();
                String ext = Files.getExtension(path);
                if (checkedExtensions.add(ext)) {
                    if (!passwordfound.getBoolean()) {
                        try {
                            buffer.reset();
                            final ReusableByteArrayOutputStream signatureBuffer = buffer;
                            final long maxPWCheckSize = extractor.getSettings().getMaxCheckedFileSizeDuringOptimizedPasswordFindingInBytes();

                            final int signatureMinLength;
                            if (new Regex(path, ".+\\.iso").matches()) {
                                signatureMinLength = 37000;
                            } else if (new Regex(path, ".+\\.mp3").matches()) {
                                signatureMinLength = 512;
                            } else {
                                signatureMinLength = 32;
                            }
                            logger.fine("Try to crack " + path);
                            ExtractOperationResult result = item.extractSlow(new ISequentialOutStream() {
                                public int write(byte[] data) throws SevenZipException {
                                    int toWrite = Math.min(signatureBuffer.free(), data.length);
                                    if (toWrite > 0) {
                                        /*
                                         * we still have enough buffer left to write the data
                                         */
                                        signatureBuffer.write(data, 0, toWrite);
                                    }
                                    if (signatureBuffer.size() >= signatureMinLength) {
                                        /*
                                         * we have enough data available for a signature check
                                         */
                                        StringBuilder sigger = new StringBuilder();
                                        for (int i = 0; i < signatureBuffer.size() - 1; i++) {
                                            String s = Integer.toHexString(signatureBuffer.getInternalBuffer()[i]);
                                            s = (s.length() < 2 ? "0" + s : s);
                                            s = s.substring(s.length() - 2);
                                            sigger.append(s);
                                        }
                                        Signature signature = getFileSignatures().getSignature(sigger.toString());
                                        if (signature != null) {
                                            if (signature.getExtensionSure() != null && signature.getExtensionSure().matcher(path).matches()) {
                                                /*
                                                 * signature matches, lets abort PWFinding now
                                                 */
                                                passwordfound.found();
                                                return 0;
                                            }
                                        }
                                    }
                                    if (item.getSize() <= maxPWCheckSize) {
                                        /*
                                         * we still allow further extraction as the itemSize <= maxPWCheckSize
                                         */
                                        return data.length;
                                    } else {
                                        /* this will throw SevenZipException */
                                        return 0;
                                    }
                                }
                            }, password);
                            if (ExtractOperationResult.OK.equals(result)) {
                                passwordfound.found();
                            }
                        } catch (SevenZipException e) {

                            // An error will be thrown if the write method
                            // returns
                            // 0.
                        }
                    } else {
                        /* pw found */
                        break;
                    }
                }
                // if (filter(item.getPath())) continue;
            }
            if (!passwordfound.getBoolean()) return false;
            archive.setFinalPassword(password);

            return true;
        } catch (SevenZipException e) {
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                return false;
            }
            throw new ExtractionException(e, rarStreamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
        } catch (Throwable e) {
            throw new ExtractionException(e, rarStreamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
        }
    }

    private void open() throws ExtractionException, DialogClosedException, DialogCanceledException {

        // password0

        String password = archive.getFinalPassword();
        if (password == null) password = "";
        // }

        rarStreamProvider = new RarStreamProvider(archive, password, this.streamProvider);
        try {
            IInStream rarStream = rarStreamProvider.getStream(archive.getFirstArchiveFile());
            rarArchive = SevenZip.openInArchive(ArchiveFormat.RAR, rarStream, rarStreamProvider);
            for (ISimpleInArchiveItem item : rarArchive.getSimpleInterface().getArchiveItems()) {
                if (pathToStream == null) {
                    if (bestItem == null && !item.isFolder()) {
                        bestItem = item;
                        bestItemSize = item.getSize();
                    } else if (!item.isFolder() && bestItem != null && bestItem.getSize() < item.getSize()) {
                        bestItem = item;
                        bestItemSize = item.getSize();
                    }
                } else {
                    if (pathToStream.replace("/", "\\").equals(item.getPath().replace("/", "\\"))) {
                        bestItem = item;
                        bestItemSize = item.getSize();
                    }
                }

                if (item.isEncrypted()) {
                    archive.setProtected(true);
                }
            }
            if (bestItem != null) logger.info("best item: " + bestItem.getPath() + " size: " + bestItem.getSize());
            return;
        } catch (SevenZipException e) {
            logger.log(e);
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                archive.setProtected(true);
                return;
            } else {
                throw new ExtractionException(e, streamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
            }
        } catch (Throwable e) {
            logger.log(e);
            throw new ExtractionException(e, streamProvider != null ? rarStreamProvider.getLatestAccessedStream().getArchiveFile() : null);
        }

    }

    protected String askPassword() throws DialogClosedException, DialogCanceledException {
        ArraySet<String> passwords = new ArraySet<String>();
        List<String> settings = archive.getSettings().getPasswords();
        if (settings != null) {
            passwords.addAll(settings);
        }
        passwords.addAll(archive.getFactory().getGuessedPasswordList(archive));
        String password = "";
        if (passwords.size() == 1) {
            password = passwords.iterator().next();
        }
        if (!StringUtils.isEmpty(archive.getFinalPassword())) {
            password = archive.getFinalPassword();
        }
        // if (StringUtils.isEmpty(password)) {
        if (passwords.size() > 0) {
            password = Dialog.getInstance().showInputDialog(UIOManager.LOGIC_COUNTDOWN, T._.enter_password(archive.getName()), T._.enter_passwordfor(passwords.toString()), password, null, null, null);
        } else {
            password = Dialog.getInstance().showInputDialog(UIOManager.LOGIC_COUNTDOWN, T._.enter_password(archive.getName()), T._.enter_passwordfor2(), password, null, null, null);
        }
        return password;
    }

    public void extract() throws IOException, SevenZipException {
        File tmp = Application.getResource("/tmp/streaming/extract" + System.currentTimeMillis());
        FileCreationManager.getInstance().mkdir(tmp.getParentFile());
        streamingChunk = new StreamingChunk(tmp, 0);

        extractionThread = new Thread() {
            public void run() {
                try {
                    streamingChunk.setCanGrow(true);
                    if (archive.isProtected()) {
                        rarArchive.extractSlow(bestItem.getItemIndex(), new ISequentialOutStream() {

                            @Override
                            public int write(byte[] abyte0) throws SevenZipException {
                                if (abyte0.length == 0) return 0;
                                try {
                                    streamingChunk.write(abyte0, 0, abyte0.length);
                                    return abyte0.length;
                                } catch (IOException e) {
                                    throw new SevenZipException(e);
                                }
                            }
                        }, archive.getFinalPassword());
                    } else {
                        rarArchive.extractSlow(bestItem.getItemIndex(), new ISequentialOutStream() {

                            @Override
                            public int write(byte[] abyte0) throws SevenZipException {
                                if (abyte0.length == 0) return 0;
                                try {
                                    streamingChunk.write(abyte0, 0, abyte0.length);
                                    return abyte0.length;
                                } catch (IOException e) {
                                    throw new SevenZipException(e);
                                }
                            }
                        });
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                } finally {
                    streamingChunk.setCanGrow(false);
                }
            };
        };
        extractionThread.start();

    }

    public Thread getExtractionThread() {
        return extractionThread;
    }

    public void close() {
        try {
            rarArchive.close();
        } catch (final Throwable e) {
        }
        try {
            rarStreamProvider.close();
        } catch (final Throwable e) {
        }
    }

    // @Override
    public boolean isRangeRequestSupported() {
        return true;
    }

    // @Override
    public long getFinalFileSize() {
        return bestItemSize;
    }

    // @Override
    public InputStream getInputStream(final long startPosition, final long stopPosition) throws IOException {

        logger.info("New Stream: " + startPosition + " -> " + stopPosition);
        return new InputStream() {
            long   currentPosition = startPosition;
            byte[] bufferByte      = new byte[1];

            @Override
            public int read() throws IOException {
                if (currentPosition != 0 && currentPosition > streamingChunk.getAvailableChunkSize()) return -1;
                int ret = read(bufferByte, 0, 1);
                if (ret == 1) return bufferByte[1];
                return -1;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                logger.info(" Read: " + len + " bytes at " + currentPosition);

                if (currentPosition != 0 && currentPosition > streamingChunk.getAvailableChunkSize()) {
                    System.out.println("-1 answer");
                    return -1;
                }
                try {
                    int ret;
                    ret = streamingChunk.read(b, off, len, currentPosition);
                    if (ret >= 0) currentPosition += ret;

                    return ret;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return -1;
                }

            }

            @Override
            public boolean markSupported() {
                return false;
            }
        };
    }

    private void updateContentView(ISimpleInArchive simpleInterface) {
        try {
            ContentView newView = new ContentView();
            ArrayList<ArchiveItem> files = new ArrayList<ArchiveItem>();
            for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                try {
                    files.add(ArchiveItem.create(item));
                    String p = item.getPath();
                    if (item.getPath().trim().equals("")) continue;
                    newView.add(new PackedFile(item.isFolder(), item.getPath(), item.getSize()));
                } catch (SevenZipException e) {
                    logger.log(e);
                }
            }
            archive.setContentView(newView);
            archive.getSettings().setArchiveItems(files);
        } catch (SevenZipException e) {
            logger.log(e);
        }
    }

    public void updateContentView() throws ExtractionException {
        if (rarArchive == null) throw new ExtractionException("Archive is not open");
        updateContentView(rarArchive.getSimpleInterface());
    }

    public void setPath(String subpath) {
        this.pathToStream = subpath;
    }
}
