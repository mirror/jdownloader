package org.jdownloader.extensions.streaming.dataprovider.rar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.parser.Regex;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.ReusableByteArrayOutputStreamPool;
import org.appwork.utils.ReusableByteArrayOutputStreamPool.ReusableByteArrayOutputStream;
import org.appwork.utils.StringUtils;
import org.appwork.utils.io.streamingio.StreamingChunk;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ArchiveItem;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.FileSignatures;
import org.jdownloader.extensions.extraction.Signature;
import org.jdownloader.extensions.extraction.content.ContentView;
import org.jdownloader.extensions.extraction.content.PackedFile;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.dataprovider.DataProvider;
import org.jdownloader.logging.LogController;

public class RarArchiveDataProvider implements DataProvider<Archive>, IArchiveOpenVolumeCallback, IArchiveOpenCallback, ICryptoGetTextPassword {

    private String           file;
    private DataProvider[]   dataProviders;
    private ExtractionConfig extractionSettings;

    public RarArchiveDataProvider(Archive archive, String subpath, DataProvider... dataProviders) {
        this.archive = archive;
        this.file = subpath;
        this.dataProviders = dataProviders;
        extractionSettings = (ExtractionConfig) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getSettings();
        if (password == null) password = "";

        map = new HashMap<String, ArchiveFile>();
        logger = LogController.getInstance().getLogger(RarArchiveDataProvider.class.getName());
        // support for test.part01-blabla.tat archives.
        // we have to create a rename matcher map in this case because 7zip cannot handle this type
        logger.info("Init Map:");
        if (archive.getFirstArchiveFile().getFilePath().matches("(?i).*\\.pa?r?t?\\.?\\d+\\D.*?\\.rar$")) {
            for (ArchiveFile af : archive.getArchiveFiles()) {
                String name = archive.getName() + "." + new Regex(af.getFilePath(), ".*(part\\d+)").getMatch(0) + ".rar";

                logger.info(af.getFilePath() + " name: " + name);
                if (af == archive.getFirstArchiveFile()) {
                    firstName = name;
                    logger.info(af.getFilePath() + " FIRSTNAME name: " + name);
                }
                if (map.put(name, af) != null) {
                    //
                    throw new WTFException("Cannot handle " + af.getFilePath());
                }
            }

        }
    }

    private Map<String, IInStream>       openStreamMap   = new HashMap<String, IInStream>();
    private String                       name;
    private String                       password;
    private Archive                      archive;
    private HashMap<String, ArchiveFile> map;
    private String                       firstName;
    private LogSource                    logger;
    private RarFromDataproviderStream               latestAccessedStream;

    private boolean                      readyForExtract = false;
    private ISevenZipInArchive           rarArchive;

    public boolean isReadyForExtract() {
        return readyForExtract;
    }

    public Archive getArchive() {
        return archive;
    }

    public Logger getLogger() {
        return logger;
    }

    public Object getProperty(PropID propID) throws SevenZipException {
        switch (propID) {
        case NAME:
            return name;
        }
        return null;
    }

    public boolean isStreamOpen(String filename) {
        return openStreamMap.containsKey(filename);
    }

    public IInStream getStream(ArchiveFile firstArchiveFile) throws SevenZipException {
        return getStream(firstName == null ? firstArchiveFile.getFilePath() : firstName);
    }

    public IInStream getStream(String filename) throws SevenZipException {
        try {

            logger.info("Stream request: " + filename);
            IInStream stream = openStreamMap.get(filename);
            ArchiveFile af = map.get(filename);
            if (stream != null) {

                name = filename;
                return stream;
            }

            logger.info("New RandomAccess: " + (af == null ? filename : af.getFilePath()));
            name = filename;
            ArchiveFile archiveFile = af == null ? archive.getArchiveFileByPath(filename) : af;
            if (archiveFile == null) return null;
            for (DataProvider dp : dataProviders) {
                if (dp.canHandle(archiveFile, dataProviders)) {
                    stream = new RarFromDataproviderStream(archiveFile, filename, this, dp);
                    openStreamMap.put(filename, stream);
                    break;
                }
            }

            return stream;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setCompleted(Long files, Long bytes) throws SevenZipException {
        System.out.println(1);
    }

    public void setTotal(Long files, Long bytes) throws SevenZipException {
        System.out.println(2);
    }

    public String cryptoGetTextPassword() throws SevenZipException {

        return password;
    }

    public void setLatestAccessedStream(RarFromDataproviderStream extRandomAccessFileInStream) {
        if (extRandomAccessFileInStream != latestAccessedStream) {
            logger.info("Extract from: " + extRandomAccessFileInStream.getFilename());
            latestAccessedStream = extRandomAccessFileInStream;
        }
    }

    public RarFromDataproviderStream getLatestAccessedStream() {
        return latestAccessedStream;
    }

    public void setReadyForExtract(boolean b) {
        readyForExtract = b;

    }

    public RarFromDataproviderStream getPart1Stream() throws SevenZipException {
        return (RarFromDataproviderStream) getStream(getArchive().getFirstArchiveFile());

    }

    private ISimpleInArchiveItem itemToExtract       = null;
    private long                 sizeOfItemToExtract = -1l;
    private FileSignatures       fileSignatures;
    private boolean              silent              = true;
    private Thread               extractionThread;
    private StreamingChunk       streamingChunk;
    private IOException          exception;

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public FileSignatures getFileSignatures() {
        if (fileSignatures == null) fileSignatures = new FileSignatures();
        return fileSignatures;
    }

    protected boolean checkIfPasswordIsCorrect(String password) throws ExtractionException {

        ReusableByteArrayOutputStream buffer = null;
        try {
            buffer = ReusableByteArrayOutputStreamPool.getReusableByteArrayOutputStream(64 * 1024, false);
            try {
                close();
            } catch (final Throwable e) {
            }
            try {
                rarArchive.close();
                rarArchive = null;
            } catch (Throwable e) {
            }
            try {
                this.streamingChunk.close();
            } catch (Throwable e) {
            }
            IInStream rarStream = getStream(archive.getFirstArchiveFile());
            rarArchive = SevenZip.openInArchive(ArchiveFormat.RAR, rarStream, this);

            final boolean[] passwordfound = new boolean[] { false };
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
                    if (!passwordfound[0]) {
                        try {
                            buffer.reset();
                            final ReusableByteArrayOutputStream signatureBuffer = buffer;
                            final long maxPWCheckSize = extractionSettings.getMaxPasswordCheckSize() * 1024;

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
                                                passwordfound[0] = true;
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
                                passwordfound[0] = true;
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
            if (!passwordfound[0]) return false;
            archive.setFinalPassword(password);

            return true;
        } catch (SevenZipException e) {
            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                return false;
            }
            throw new ExtractionException(e, getLatestAccessedStream() == null ? null : getLatestAccessedStream().getArchiveFile());
        } catch (Throwable e) {
            throw new ExtractionException(e, getLatestAccessedStream() == null ? null : getLatestAccessedStream().getArchiveFile());
        } finally {
            try {
                ReusableByteArrayOutputStreamPool.reuseReusableByteArrayOutputStream(buffer);
            } catch (Throwable e) {
            } finally {
                buffer = null;
            }
        }

    }

    private boolean isSilent() {
        return silent;
    }

    protected String askPassword() throws PasswordNotFoundException {
        if (isSilent()) { throw new PasswordNotFoundException("Silent"); }
        HashSet<String> passwords = new HashSet<String>();
        HashSet<String> settings = archive.getSettings().getPasswords();
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
        try {
            // if (StringUtils.isEmpty(password)) {
            if (passwords.size() > 0) {
                password = Dialog.getInstance().showInputDialog(Dialog.LOGIC_COUNTDOWN, T._.enter_password(archive.getName()), T._.enter_passwordfor(passwords.toString()), password, null, null, null);
            } else {
                password = Dialog.getInstance().showInputDialog(Dialog.LOGIC_COUNTDOWN, T._.enter_password(archive.getName()), T._.enter_passwordfor2(), password, null, null, null);
            }
        } catch (Throwable e) {
            throw new PasswordNotFoundException(e);
        }
        return password;
    }

    protected void onWarning(String wrong_password) {
        Dialog.getInstance().showMessageDialog(wrong_password);
    }

    public void openArchive() throws ExtractionException, InterruptedException, PasswordNotFoundException {
        open();
        checkException();
        if (archive.isProtected()) {

            HashSet<String> spwList = archive.getSettings().getPasswords();
            HashSet<String> passwordList = new HashSet<String>();
            if (spwList != null) {
                passwordList.addAll(spwList);
            }
            passwordList.addAll(archive.getFactory().getGuessedPasswordList(archive));
            passwordList.add(archive.getName());
            ArrayList<String> pwList = extractionSettings.getPasswordList();
            if (pwList == null) pwList = new ArrayList<String>();
            passwordList.addAll(pwList);

            logger.info("Start password finding for " + archive);

            String correctPW = null;

            for (String password : passwordList) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                checkException();
                checkException();
                if (checkIfPasswordIsCorrect(password)) {
                    correctPW = password;
                    break;
                }
            }

            if (correctPW == null) {
                String password = askPassword();
                while (true) {
                    if (checkIfPasswordIsCorrect(password)) {
                        break;
                    } else {
                        onWarning(T._.wrong_password());

                    }
                    checkException();
                }
            }
            try {
                if (itemToExtract == null) {
                    logger.info("Path: " + file);
                    for (ISimpleInArchiveItem item : rarArchive.getSimpleInterface().getArchiveItems()) {
                        if (file == null) {
                            if (itemToExtract == null && !item.isFolder()) {
                                itemToExtract = item;
                                sizeOfItemToExtract = item.getSize();
                            } else if (!item.isFolder() && itemToExtract != null && itemToExtract.getSize() < item.getSize()) {
                                itemToExtract = item;
                                sizeOfItemToExtract = item.getSize();
                            }
                        } else {
                            if (file.replace("/", "\\").equals(item.getPath().replace("/", "\\"))) {
                                itemToExtract = item;
                                sizeOfItemToExtract = item.getSize();
                            } else {
                                logger.info(file + "!=" + item.getPath());
                            }
                        }

                        if (item.isEncrypted()) {
                            archive.setProtected(true);
                        }
                    }
                    logger.info("best item: " + itemToExtract.getPath() + " size: " + itemToExtract.getSize());
                }

            } catch (Throwable e) {
                logger.log(e);
                throwException(e);
            }
            logger.info("Found password for " + archive + "->" + archive.getFinalPassword());
            /* avoid duplicates */
            pwList.remove(archive.getFinalPassword());
            pwList.add(0, archive.getFinalPassword());

            extractionSettings.setPasswordList(pwList);
        }
    }

    private void checkException() throws ExtractionException {
        Throwable e = getException();
        if (e != null) throwException(e);
    }

    private void throwException(Throwable e) throws ExtractionException {
        throw new ExtractionException(e, getLatestAccessedStream() != null ? getLatestAccessedStream().getArchiveFile() : null);
    }

    private void open() throws ExtractionException {

        // password0

        String password = archive.getFinalPassword();
        if (password == null) password = "";
        // }

        try {
            IInStream rarStream = getStream(archive.getFirstArchiveFile());
            rarArchive = SevenZip.openInArchive(ArchiveFormat.RAR, rarStream, this);
            for (ISimpleInArchiveItem item : rarArchive.getSimpleInterface().getArchiveItems()) {
                if (file == null) {
                    if (itemToExtract == null && !item.isFolder()) {
                        itemToExtract = item;
                        sizeOfItemToExtract = item.getSize();
                    } else if (!item.isFolder() && itemToExtract != null && itemToExtract.getSize() < item.getSize()) {
                        itemToExtract = item;
                        sizeOfItemToExtract = item.getSize();
                    }
                } else {
                    if (file.replace("/", "\\").equals(item.getPath().replace("/", "\\"))) {
                        itemToExtract = item;
                        sizeOfItemToExtract = item.getSize();
                    }
                }

                if (item.isEncrypted()) {
                    archive.setProtected(true);
                }
            }
            updateContentView(rarArchive.getSimpleInterface());
            if (itemToExtract != null) logger.info("best item: " + itemToExtract.getPath() + " size: " + itemToExtract.getSize());
            return;
        } catch (SevenZipException e) {
            logger.log(e);

            if (e.getMessage().contains("HRESULT: 0x80004005") || e.getMessage().contains("HRESULT: 0x1 (FALSE)") || e.getMessage().contains("can't be opened") || e.getMessage().contains("No password was provided")) {
                /* password required */
                archive.setProtected(true);
                return;
            } else {
                throw new ExtractionException(e, getLatestAccessedStream() != null ? getLatestAccessedStream().getArchiveFile() : null);
            }
        } catch (Throwable e) {
            logger.log(e);
            throw new ExtractionException(e, getLatestAccessedStream() != null ? getLatestAccessedStream().getArchiveFile() : null);
        }

    }

    private void updateContentView(ISimpleInArchive simpleInterface) {
        try {
            ContentView newView = new ContentView();
            ArrayList<ArchiveItem> files = new ArrayList<ArchiveItem>();
            for (ISimpleInArchiveItem item : simpleInterface.getArchiveItems()) {
                try {
                    files.add(ArchiveItem.create(item));
                    String p = item.getPath();
                    if (p.trim().equals("")) continue;
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

    @Override
    public boolean isRangeRequestSupported(Archive archive) {
        return true;
    }

    @Override
    public long getFinalFileSize(Archive archive) {
        return sizeOfItemToExtract;
    }

    public void extract() throws IOException, SevenZipException {
        File tmp = Application.getResource("/tmp/streaming/extract" + System.currentTimeMillis());
        tmp.getParentFile().mkdirs();
        streamingChunk = new StreamingChunk(tmp, 0);

        extractionThread = new Thread() {
            public void run() {
                try {
                    streamingChunk.setCanGrow(true);
                    if (archive.isProtected()) {
                        rarArchive.extractSlow(itemToExtract.getItemIndex(), new ISequentialOutStream() {

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
                        rarArchive.extractSlow(itemToExtract.getItemIndex(), new ISequentialOutStream() {

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

    @Override
    public InputStream getInputStream(Archive archive, final long startPosition, long stopPosition) throws IOException {
        try {
            if (!isOpen()) {

                openArchive();

            }
            if (extractionThread == null) {
                extract();
            }

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

        } catch (Throwable e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    private boolean isOpen() {
        return rarArchive != null;
    }

    @Override
    public boolean canHandle(Archive link, DataProvider<?>... dataProviders) {
        return link instanceof Archive;
    }

    @Override
    public void close() throws IOException {
        Iterator<Entry<String, IInStream>> it = openStreamMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, IInStream> next = it.next();
            if (next.getValue() instanceof RarFromDataproviderStream) {
                try {
                    ((RarFromDataproviderStream) next.getValue()).close();
                } catch (final Throwable e) {
                }
            }
            it.remove();
        }
        for (DataProvider<?> dp : dataProviders) {
            dp.close();
        }
    }

    @Override
    public Throwable getException() {
        if (exception != null) return exception;
        for (DataProvider<?> db : dataProviders) {
            if (db.getException() != null) { return db.getException(); }
        }
        return null;
    }

    public void setException(IOException e) {
        this.exception = e;
    }

}
