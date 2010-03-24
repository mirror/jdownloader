//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.jdunrar;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.nutils.DynByteBuffer;
import jd.nutils.Executer;
import jd.nutils.ProcessListener;
import jd.nutils.io.FileSignatures;
import jd.nutils.io.Signature;
import jd.nutils.jobber.JDRunnable;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.utils.EditDistance;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Die klasse dient zum verpacken der Unrar binary.
 * 
 * @author coalado
 * 
 */
public class UnrarWrapper extends Thread implements JDRunnable {

    /**
     * User stopped the process
     */
    public static final int EXIT_CODE_USER_BREAK = 255;
    /**
     * Create file error
     */
    public static final int EXIT_CODE_CREATE_ERROR = 9;
    /**
     * Not enough memory for operation
     */
    public static final int EXIT_CODE_MEMORY_ERROR = 8;
    /**
     * Command line option error
     */
    public static final int EXIT_CODE_USER_ERROR = 7;

    /**
     * Open file error
     */
    public static final int EXIT_CODE_OPEN_ERROR = 6;
    /**
     * Write to disk error
     */
    public static final int EXIT_CODE_WRITE_ERROR = 5;
    /**
     * Attempt to modify an archive previously locked by the 'k' command
     */
    public static final int EXIT_CODE_LOCKED_ARCHIVE = 4;
    /**
     * A CRC error occurred when unpacking
     */
    public static final int EXIT_CODE_CRC_ERROR = 3;
    /**
     * A fatal error occurred
     */
    public static final int EXIT_CODE_FATAL_ERROR = 2;
    /**
     * Non fatal error(s) occurred
     * 
     */
    public static final int EXIT_CODE_WARNING = 1;
    /**
     * Successful operation
     */
    public static final int EXIT_CODE_SUCCESS = 0;

    private static final boolean DEBUG = true;
    private ArrayList<UnrarListener> listener = new ArrayList<UnrarListener>();
    private DownloadLink link;
    private String unrarCommand;
    private ArrayList<String> passwordList;
    private File file;
    private int statusid;
    private String password;
    private boolean isProtected = false;
    private ArrayList<ArchivFile> files;

    private boolean overwriteFiles = false;

    private long totalSize;
    private ArchivFile currentlyWorkingOn;

    public void setCurrentlyWorkingOn(ArchivFile currentlyWorkingOn) {
        this.currentlyWorkingOn = currentlyWorkingOn;
    }

    private int currentVolume = 1;
    private long startTime;
    private SubConfiguration config = null;
    private long speed = 10000000;
    private boolean exactProgress = false;
    private int volumeNum = 1;
    private Exception exception;
    private File extractTo;
    private boolean removeAfterExtraction;

    private ArrayList<String> archiveParts;
    private int crackProgress;
    private int exitCode;
    private boolean gotInterrupted;
    private Logger logger;
    private ProgressController progressController;

    public UnrarWrapper(DownloadLink link) {
        this.link = link;
        logger = JDLogger.getLogger();
        config = SubConfiguration.getConfig(JDL.L("plugins.optional.jdunrar.name", "JD-Unrar"));
        speed = config.getIntegerProperty("SPEED", 10000000);
        if (link == null) { throw new IllegalArgumentException("link==null"); }
        this.file = new File(link.getFileOutput());
        archiveParts = new ArrayList<String>();
    }

    public UnrarWrapper(DownloadLink link, File file) {
        this.link = link;
        config = SubConfiguration.getConfig(JDL.L("plugins.optional.jdunrar.name", "JD-Unrar"));
        if (link == null) { throw new IllegalArgumentException("link==null"); }
        this.file = file;
        archiveParts = new ArrayList<String>();
    }

    public void addUnrarListener(UnrarListener listener) {
        this.removeUnrarListener(listener);
        this.listener.add(listener);
    }

    private void removeUnrarListener(UnrarListener listener) {
        this.listener.remove(listener);
    }

    public ArrayList<ArchivFile> getFiles() {
        return files;
    }

    /**
     * Checks if the extracted file(s) has enough space.
     * 
     * @param dlLink
     * @return
     */
    private boolean checkSize() {
        if (JDUtilities.getJavaVersion() < 1.6) return true;

        File f = extractTo;

        if (f == null) f = file;

        while (!f.exists()) {
            f = f.getParentFile();

            if (f == null) return false;
        }

        // Set 500MB extra Buffer
        long size = 1024 * 1024 * 1024 * 500;

        for (DownloadLink dlink : DownloadWatchDog.getInstance().getRunningDownloads()) {
            size += dlink.getDownloadSize() - dlink.getDownloadCurrent();
        }

        if (f.getUsableSpace() < size + totalSize) return false;

        return true;
    }

    @Override
    public void run() {
        try {
            fireEvent(JDUnrarConstants.WRAPPER_STARTED);
            if (open()) {
                if (!checkSize()) {
                    fireEvent(JDUnrarConstants.NOT_ENOUGH_SPACE);
                    return;
                }

                if (this.isProtected && this.password == null) {
                    fireEvent(JDUnrarConstants.WRAPPER_CRACK_PASSWORD);

                    if (this.isProtected && this.password == null) {
                        crackPassword();
                        if (password == null) {
                            fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE);
                            if (password != null) {
                                this.passwordList.clear();
                                passwordList.add(password);
                                password = null;
                                crackPassword();
                            }
                            if (password != null) {
                                fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);
                            }

                        } else {
                            fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);

                        }

                        if (password == null) {

                            fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                            return;
                        }

                    } else {
                        fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);
                    }
                }
                fireEvent(JDUnrarConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS);

                boolean okay = extract();
                this.checkSizes();
                switch (exitCode) {
                case EXIT_CODE_SUCCESS:
                    if (!gotInterrupted && removeAfterExtraction && okay) {
                        removeArchiveFiles();
                    }
                    fireEvent(JDUnrarConstants.WRAPPER_FINISHED_SUCCESSFULL);
                    break;
                case EXIT_CODE_CRC_ERROR:
                    JDLogger.getLogger().warning("A CRC error occurred when unpacking");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC);
                    break;
                case EXIT_CODE_USER_BREAK:
                    JDLogger.getLogger().info(" User interrupted extraction");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_CREATE_ERROR:
                    JDLogger.getLogger().warning("Could not create Outputfile");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_MEMORY_ERROR:
                    JDLogger.getLogger().warning("Not enough memory for operation");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_USER_ERROR:
                    JDLogger.getLogger().warning("Command line option error");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_OPEN_ERROR:
                    JDLogger.getLogger().warning("Open file error");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_WRITE_ERROR:
                    JDLogger.getLogger().warning("Write to disk error");
                    this.exception = new UnrarException("Write to disk error");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_LOCKED_ARCHIVE:
                    JDLogger.getLogger().warning("Attempt to modify an archive previously locked by the 'k' command");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_FATAL_ERROR:
                    JDLogger.getLogger().warning("A fatal error occurred");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case EXIT_CODE_WARNING:
                    JDLogger.getLogger().warning("Non fatal error(s) occurred");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                default:
                    JDLogger.getLogger().warning("Unknown Error");
                    fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                }
                return;
            }
        } catch (Exception e) {
            this.exception = e;
            JDLogger.exception(e);
            fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
        }
    }

    private boolean checkSizes() {
        boolean c = true;
        for (ArchivFile f : files) {
            // System.out.println(f.getFilepath());
            // System.out.println(f.getFile().getAbsolutePath());
            System.err.println("Sizecheck: " + f.getFilepath() + "(" + f.getSize() + ") = " + f.getFile().length());
            if (f.getSize() != f.getFile().length()) {
                if (!f.getFile().isDirectory()) {
                    System.err.println("^ERROR^^");

                    c = false;
                } else {
                    f.setPercent(100);
                }
            } else {
                f.setPercent(100);
            }
        }
        return c;

    }

    private void removeArchiveFiles() {
        for (String file : archiveParts) {
            if (file != null && file.trim().length() > 0) {
                File tmpFile = new File(file);
                if (tmpFile.isAbsolute()) {
                    if (!tmpFile.delete()) tmpFile.deleteOnExit();
                    JDLogger.getLogger().warning("Deleted archive after extraction: " + new File(file));
                } else {
                    tmpFile = new File(this.file.getParentFile(), file);
                    if (!tmpFile.delete()) tmpFile.deleteOnExit();
                    logger.warning("Deleted archive after extraction: " + tmpFile);
                }
            }
        }

    }

    public Exception getException() {
        return exception;
    }

    public int getStatus() {
        return statusid;
    }

    private boolean extract() {

        fireEvent(JDUnrarConstants.WRAPPER_START_EXTRACTION);
        Executer exec = new Executer(unrarCommand);
        exec.setLogger(JDLogger.getLogger());
        exec.setDebug(DEBUG);
        exec.addParameter("x");

        exec.addParameter("-p");

        if (overwriteFiles) {
            exec.addParameter("-o+");
        } else {
            exec.addParameter("-o-");
        }
        exec.addParameter("-c-");
        exec.addParameter("-v");
        exec.addParameter("-ierr");
        exec.addParameter(file.getAbsolutePath());
        exec.setRunin(file.getParentFile().getAbsolutePath());
        if (extractTo != null) {
            if (extractTo.exists() || extractTo.mkdirs()) {
                exec.setRunin(extractTo.getAbsolutePath());
            } else {
                logger.severe("could not create " + extractTo.toString());
            }
        }
        exec.setWaitTimeout(-1);
        exec.addProcessListener(new ExtractListener(), Executer.LISTENER_ERRORSTREAM);
        exec.addProcessListener(new PasswordListener(password), Executer.LISTENER_ERRORSTREAM);

        exec.start();
        this.startTime = System.currentTimeMillis();
        Thread inter = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (!exactProgress) {
                        if (!exactProgress) {
                            long est = speed * ((System.currentTimeMillis() - startTime) / 1000);

                            for (ArchivFile f : files) {
                                long part = Math.min(est, f.getSize());
                                est -= part;
                                if (part == 0 && f.getSize() == 0) {
                                    f.setPercent(100);
                                } else {
                                    f.setPercent((int) ((part * 100) / f.getSize()));
                                }
                                if (est <= 0) break;
                            }
                            fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);

                        } else {
                            return;
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        statusid = -1;
        inter.start();
        exec.waitTimeout();
        this.exitCode = exec.getExitValue();
        this.gotInterrupted = exec.gotInterrupted();
        inter.interrupt();
        config.setProperty("SPEED", speed);
        config.save();
        if (statusid > 0) {
            switch (statusid) {
            case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC:
                this.exitCode = EXIT_CODE_CRC_ERROR;
                break;
            }
            return false;
        }
        return true;
    }

    private void crackPassword() {
        ArchivFile smallestFile = null;
        ArchivFile biggestFile = null;
        this.crackProgress = 0;
        fireEvent(JDUnrarConstants.WRAPPER_PASSWORT_CRACKING);
        // suche kleines passwortgeschützte Datei
        for (ArchivFile f : files) {
            if (f.isProtected()) {
                if (smallestFile == null) {
                    smallestFile = f;

                } else if (f.getSize() < smallestFile.getSize()) {
                    smallestFile = f;
                }
                if (biggestFile == null) {
                    biggestFile = f;

                } else if (f.getSize() > biggestFile.getSize()) {
                    biggestFile = f;
                }
            }
        }

        // File fileFile = new File(this.file.getParentFile(),
        // System.currentTimeMillis() + ".unrartmp");
        // String str=file.getFilepath();
        //
        //     
        // CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        // CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();
        //       
        // try {
        // // Convert a string to ISO-LATIN-1 bytes in a ByteBuffer
        // // The new ByteBuffer is ready to be read.
        // ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(str));
        //       
        // // Convert ISO-LATIN-1 bytes in a ByteBuffer to a character
        // ByteBuffer and then to a string.
        // // The new ByteBuffer is ready to be read.
        // CharBuffer cbuf = decoder.decode(bbuf);
        // str = cbuf.toString();
        // } catch (CharacterCodingException e) {
        // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE
        // ,"Exception occurred",e);
        // }
        //
        // JDUtilities.writeLocalFile(fileFile,str);
        //        
        //       
        //        
        // fileFile.deleteOnExit();

        if (smallestFile.getSize() < 2097152) {
            int c = 0;

            for (String pass : this.passwordList) {
                crackProgress = ((c++) * 100) / passwordList.size();
                fireEvent(JDUnrarConstants.WRAPPER_PASSWORT_CRACKING);
                Executer exec = new Executer(unrarCommand);
                exec.setLogger(JDLogger.getLogger());
                exec.setDebug(DEBUG);
                exec.addParameter("t");
                // exec.addParameter("-p");
                exec.addParameter("-sl" + (smallestFile.getSize() + 1));
                exec.addParameter("-sm" + (smallestFile.getSize() - 1));
                exec.addParameter("-c-");
                exec.addParameter(this.file.getName());
                exec.setRunin(this.file.getParentFile().getAbsolutePath());
                exec.setWaitTimeout(-1);

                exec.addProcessListener(new ProcessListener() {

                    public void onBufferChanged(Executer exec, DynByteBuffer totalBuffer, int latestReadNum) {
                    }

                    public void onProcess(Executer exec, String latestLine, DynByteBuffer totalBuffer) {
                        if ((new Regex(latestLine, "^\\s*?(OK)").getMatch(0)) != null) {
                            exec.interrupt();
                            System.out.println("loaded enough.... one file is enough");
                            totalBuffer.put("All OK".getBytes(), 6);
                        }

                    }

                }, Executer.LISTENER_STDSTREAM);
                PasswordListener pwl = null;
                exec.addProcessListener(pwl = new PasswordListener(pass), Executer.LISTENER_ERRORSTREAM);
                exec.start();
                exec.waitTimeout();
                String res = exec.getOutputStream() + " \r\n " + exec.getErrorStream();
                if (res.indexOf(" (password incorrect ?)") != -1 || res.contains("the file header is corrupt") || pwl.pwerror()) {
                    continue;
                } else if (res.matches("(?s).*[\\s]+All OK[\\s].*")) {
                    this.password = pass;
                    crackProgress = 100;
                    fireEvent(JDUnrarConstants.WRAPPER_PASSWORT_CRACKING);

                    return;
                } else {
                    continue;
                }
            }
        } else {
            int c = 0;
            for (String pass : this.passwordList) {
                crackProgress = ((c++) * 100) / passwordList.size();

                fireEvent(JDUnrarConstants.WRAPPER_PASSWORT_CRACKING);
                Executer exec = new Executer(unrarCommand);
                exec.setDebug(DEBUG);
                exec.addParameter("p");
                // exec.addParameter("-p");
                exec.addParameter("-sm" + (biggestFile.getSize() - 1));

                exec.addParameter("-c-");
                exec.addParameter("-ierr");
                exec.addParameter(this.file.getName());
                exec.setRunin(this.file.getParentFile().getAbsolutePath());
                exec.setWaitTimeout(-1);
                exec.addProcessListener(new PasswordListener(pass), Executer.LISTENER_ERRORSTREAM);

                exec.addProcessListener(new ProcessListener() {

                    public void onBufferChanged(final Executer exec, DynByteBuffer buffer, int latestNum) {
                        if (buffer.position() >= 50) {
                            System.out.println("loaded enough.... interrupt");
                            exec.interrupt();

                        }
                    }

                    public void onProcess(Executer exec, String latestLine, DynByteBuffer sb) {
                    }

                }, Executer.LISTENER_STDSTREAM);
                exec.start();
                // Wartet bis der process fertig ist,oder bis er abgebrochen
                // wurde
                exec.waitTimeout();
                String res = exec.getErrorStream();
                if (new Regex(res, "(CRC failed|Total errors: )").matches() || res.contains("the file header is corrupt")) {
                    continue;
                }

                StringBuilder sigger = new StringBuilder();
                DynByteBuffer buff = exec.getInputStreamBuffer();
                buff.flip();
                for (int i = 0; i < buff.limit(); i++) {
                    byte f = buff.get();
                    String s = Integer.toHexString(f);
                    s = (s.length() < 2 ? "0" + s : s);
                    s = s.substring(s.length() - 2);
                    sigger.append(s);
                }
                String sig = sigger.toString();
                // logger.finest(exec.getInputStreamBuffer() + " : " + sig);
                if (sig.trim().length() < 8) continue;
                Signature signature = FileSignatures.getSignature(sig);

                if (signature != null) {
                    if (signature.getExtension().matcher(smallestFile.getFilepath()).matches()) {
                        // signatur und extension passen
                        this.password = pass;
                        crackProgress = 100;
                        fireEvent(JDUnrarConstants.WRAPPER_PASSWORT_CRACKING);
                        return;
                    }
                }

            }

        }

        crackProgress = 100;
        fireEvent(JDUnrarConstants.WRAPPER_PASSWORT_CRACKING);

    }

    public int getCrackProgress() {
        return crackProgress;
    }

    void fireEvent(int status2) {
        for (UnrarListener listener : this.listener) {
            listener.onUnrarEvent(status2, this);
        }

    }

    /**
     * Prüft ob ein bestimmter Unrarbefehl gültig ist
     * 
     * @param path
     * @return
     */
    public static boolean isUnrarCommandValid(String path) {
        try {
            Executer exec = new Executer(path);
            exec.setLogger(JDLogger.getLogger());
            exec.setWaitTimeout(5);
            exec.start();
            exec.waitTimeout();
            String ret = exec.getErrorStream() + " " + exec.getOutputStream();

            if (new Regex(ret, "RAR.*?Alexander").matches()) {
                return true;
            } else if (new Regex(ret, "RAR.*?3\\.").matches()) {
                return true;
            } else {
                System.err.println("Wrong unrar: " + Regex.getLines(exec.getErrorStream())[0]);
                return false;
            }
        } catch (Exception e) {
            JDLogger.exception(e);
            return false;
        }
    }

    private boolean open() throws UnrarException {
        String pass = null;
        int i = 0;
        fireEvent(JDUnrarConstants.WRAPPER_START_OPEN_ARCHIVE);
        int c = 0;

        while (true) {
            Executer exec = new Executer(unrarCommand);
            exec.setDebug(DEBUG);
            if (i > 0) {
                if (passwordList.size() < i) {

                    fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE);

                    if (password == null) return false;
                    pass = password;
                    password = null;

                } else {
                    pass = this.passwordList.get(i - 1);
                }
            }

            if (c > 0) {
                crackProgress = ((c) * 100) / passwordList.size();
                fireEvent(JDUnrarConstants.WRAPPER_PASSWORT_CRACKING);
            }
            c++;
            i++;
            exec.addParameter("v");
            exec.addParameter("-p");
            PasswordListener pwl = null;
            exec.addProcessListener(pwl = new PasswordListener(pass), Executer.LISTENER_ERRORSTREAM);
            exec.addParameter("-v");
            exec.addParameter("-c-");
            exec.addParameter(file.getName());
            exec.setRunin(file.getParentFile().getAbsolutePath());
            exec.setWaitTimeout(-1);
            exec.setDebug(true);
            exec.start();
            exec.waitTimeout();
            if (exec.getException() != null && exec.getException().getMessage().contains("Cannot run")) {
                logger.severe(exec.getException().getMessage());
                fireEvent(JDUnrarConstants.INVALID_BINARY);
                return false;
            }
            String res = exec.getOutputStream() + " \r\n " + exec.getErrorStream();
            logger.finest(res);
            String match;
            if ((match = new Regex(res, Pattern.compile("Bad archive (.{5,})")).getMatch(0)) != null) {
                statusid = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;
                String filename = match;
                currentVolume = 0;
                match = new Regex(filename, "\\.part(\\d+)\\.").getMatch(0);
                if (match != null) {
                    currentVolume = Integer.parseInt(match.trim());
                } else {
                    match = new Regex(filename, "(.*?)\\.rar").getMatch(0);
                    if (match != null) {
                        currentVolume = 1;
                    } else {
                        match = new Regex(filename, "\\.r(\\d+)").getMatch(0);
                        if (match != null) currentVolume = Integer.parseInt(match.trim()) + 2;
                    }
                }
                throw new UnrarException("Bad archive " + filename);
            }
            if (res.contains("Cannot open ") || res.contains("Das System kann die angegebene Datei nicht finden")) { throw new UnrarException("File not found " + file.getAbsolutePath()); }
            if (res.contains("is not RAR archive")) {
                String message = new Regex(res, Pattern.compile("(^.*?is not RAR archive)", Pattern.MULTILINE)).getMatch(0);
                throw new UnrarException(message);
            }
            if (res.indexOf(" (password incorrect") != -1 || res.contains("the file header is corrupt") || pwl.pwerror()) {
                logger.finest("Password incorrect: " + file.getName() + " pw: " + pass);
                continue;
            } else {
                if (res.indexOf("Cannot find volume") != -1) {
                    String message = new Regex(res, Pattern.compile("(^.*?Cannot find volume.*?$)", Pattern.MULTILINE)).getMatch(0);
                    throw new UnrarException(message);
                }
                String[] volumes = Pattern.compile("Volume (.*?)Pathname/Comment", Pattern.DOTALL).split(res);
                ArchivFile tmp = null;
                String namen = "";
                this.files = new ArrayList<ArchivFile>();
                this.totalSize = 0;
                for (String volume : volumes) {
                    res = volume;

                    Pattern patternvolumes = Pattern.compile("(.+)\\s*?([\\d]+).*?[\\d]+\\-[\\d]+\\-[\\d]+.*?[\\d]+:[\\d]+.*?(.{1})(.{1})(.{1})", Pattern.CASE_INSENSITIVE);
                    Matcher matchervolumes = patternvolumes.matcher(res);

                    String vol = new Regex(res, "       volume (\\d+)").getMatch(0);
                    if (vol != null) {
                        volumeNum = Integer.parseInt(vol.trim());
                    }
                    while (matchervolumes.find()) {

                        String name = matchervolumes.group(1);

                        if (name.matches("\\*.*")) {
                            name = name.substring(1);

                            long size = Long.parseLong(matchervolumes.group(2));
                            this.isProtected = true;
                            if (pass != null && password != pass) {

                                this.password = pass;
                                fireEvent(JDUnrarConstants.WRAPPER_PASSWORD_FOUND);
                            }
                            if (!name.equals(namen) && !matchervolumes.group(4).equals("D")) {
                                tmp = new ArchivFile(name);
                                tmp.setSize(size);
                                tmp.setPath(this.getExtractTo());
                                tmp.setProtected(true);
                                tmp.addVolume(vol);
                                files.add(tmp);
                                namen = name;
                                totalSize += size;

                            } else if (name.equals(namen)) {
                                tmp.addVolume(vol);
                            }

                        } else {
                            name = name.substring(1);
                            if (!name.equals(namen) && !matchervolumes.group(4).equals("D")) {

                                tmp = new ArchivFile(name);

                                tmp.setPath(this.getExtractTo());
                                long size;
                                tmp.setSize(size = Long.parseLong(matchervolumes.group(2)));
                                totalSize += size;
                                tmp.setProtected(false);
                                tmp.addVolume(vol);
                                files.add(tmp);
                                namen = name;

                            } else if (name.equals(namen)) {
                                tmp.addVolume(vol);
                            }

                        }
                    }
                }
                return true;

            }

        }
    }

    /**
     * Setzt den Pfad zur unrar.exe
     * 
     * @param file
     */
    public void setUnrarCommand(String file) {
        this.unrarCommand = file;
    }

    public void setPasswordList(ArrayList<String> passwordList) {
        this.passwordList = passwordList;
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public long getExtractedSize() {
        long size = 0;
        for (ArchivFile af : files) {
            size += af.getSize() * (af.getPercent() / 100.0);
        }
        return size;
    }

    /**
     * Gibt das ArchiveFile zum lokalen Pfad currentWorkingFile zurück. Falls
     * sonderzeichen den namensmatch unmöglich machen, Wird über levensthein
     * veruscht den besten Trefer zu finden
     * 
     * @param currentWorkingFile
     * @return
     */
    ArchivFile getArchivFile(String currentWorkingFile) {

        for (ArchivFile af : files) {
            if (af.getFilepath().equals(currentWorkingFile)) return af;
        }
        ArchivFile best = null;
        int value = Integer.MAX_VALUE;
        int cur;
        for (ArchivFile af : files) {
            cur = EditDistance.getLevenshteinDistance(af.getFilepath(), currentWorkingFile);
            if (cur < value) {
                value = cur;
                best = af;
            }
        }

        return best;
    }

    public String getPassword() {
        return password;
    }

    public ProgressController getProgressController() {
        return progressController;
    }

    public long getTotalSize() {
        return this.totalSize;
    }

    public ArchivFile getCurrentFile() {
        return this.currentlyWorkingOn;
    }

    public File getFile() {
        return file;
    }

    public DownloadLink getDownloadLink() {
        return this.link;
    }

    public void setExtractTo(File dl) {
        this.extractTo = dl;
        if (files != null) for (ArchivFile af : files)
            af.setPath(dl);
    }

    public File getExtractTo() {
        return extractTo;
    }

    public void setRemoveAfterExtract(boolean setProperty) {
        this.removeAfterExtraction = setProperty;
    }

    public void setOverwrite(boolean booleanProperty) {
        this.overwriteFiles = booleanProperty;
    }

    public void setPassword(String pass) {
        this.password = pass;
    }

    public void setProgressController(ProgressController progress) {
        progressController = progress;
    }

    public class ExtractListener implements ProcessListener {
        private int lastLinePosition = 0;

        public void onBufferChanged(Executer exec, DynByteBuffer buffer, int latestReadNum) {
            String lastLine;
            try {
                lastLine = new String(buffer.getLast(buffer.position() - lastLinePosition), Executer.CODEPAGE);
            } catch (Exception e) {
                JDLogger.exception(e);
                lastLine = new String(buffer.getLast(buffer.position() - lastLinePosition));
            }
            if (new Regex(lastLine, Pattern.compile("Write error.*?bort ", Pattern.CASE_INSENSITIVE)).matches()) {
                exec.writetoOutputStream("A");
            }
        }

        public void onProcess(Executer exec, String latestLine, DynByteBuffer totalBuffer) {
            this.lastLinePosition = totalBuffer.position();
            String match = null;
            if (latestLine.length() > 0) {
                // Neue Datei wurde angefangen
                if ((match = new Regex(latestLine, "Extracting  (.*)").getMatch(0)) != null) {
                    String currentWorkingFile = match.trim();
                    currentlyWorkingOn = getArchivFile(currentWorkingFile);
                    fireEvent(JDUnrarConstants.WRAPPER_PROGRESS_NEW_SINGLE_FILE_STARTED);
                }
                if ((match = new Regex(latestLine, "Extracting from(.*)").getMatch(0)) != null) {
                    archiveParts.add(match.trim());
                }
                if ((match = new Regex(latestLine, "Extracting from.*part(\\d+)\\.").getMatch(0)) != null) {
                    currentVolume = Integer.parseInt(match.trim());
                    long ext = totalSize / volumeNum * (currentVolume - 1);
                    if (ext == 0) { return; }
                    try {
                        speed = ext / ((System.currentTimeMillis() - startTime) / 1000);
                    } catch (Exception e) {
                    }
                }
                // ruft die prozentangaben der aktuellen datei
                if ((match = new Regex(latestLine, "(\\d+)\\%").getMatch(0)) != null) {
                    if (currentlyWorkingOn != null) {
                        exactProgress = true;
                        currentlyWorkingOn.setPercent(Integer.parseInt(match));
                        fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);
                    }
                    return;
                }

                // datei ok
                if ((match = new Regex(latestLine, "^\\s*?(OK)").getMatch(0)) != null) {
                    if (currentlyWorkingOn != null) {
                        currentlyWorkingOn.setPercent(100);
                        fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);
                        fireEvent(JDUnrarConstants.WRAPPER_PROGRESS_SINGLE_FILE_FINISHED);
                    }
                }

                if ((match = new Regex(latestLine, "Bad archive (.{5,})").getMatch(0)) != null) {
                    statusid = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;
                    String currentWorkingFile = match.trim();
                    currentlyWorkingOn = getArchivFile(currentWorkingFile);
                    String filename = latestLine;
                    match = new Regex(filename, "\\.part(\\d+)\\.").getMatch(0);
                    if (match != null) {
                        currentVolume = Integer.parseInt(match.trim());
                    } else {
                        match = new Regex(filename, "(.*?)\\.rar").getMatch(0);
                        if (match != null) {
                            currentVolume = 1;
                        } else {
                            match = new Regex(filename, "\\.r(\\d+)").getMatch(0);
                            if (match != null) currentVolume = Integer.parseInt(match.trim()) + 2;
                        }
                    }
                    exec.interrupt();
                }

                if ((match = new Regex(latestLine, " Total errors:").getMatch(0)) != null) {
                    statusid = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED;
                    exec.interrupt();
                }

                if ((match = new Regex(latestLine, "(No files to extract)").getMatch(0)) != null) {
                    exception = new Exception("Files already exist!");
                }
                if ((match = new Regex(latestLine, "CRC failed in (.*?) \\(").getMatch(0)) != null) {
                    statusid = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;
                    exec.interrupt();
                }

                if ((match = new Regex(latestLine, "packed data CRC failed in volume(.{5,})").getMatch(0)) != null) {
                    statusid = JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC;
                    String currentWorkingFile = new Regex(latestLine, "(.*?): packed").getMatch(0);
                    currentlyWorkingOn = getArchivFile(currentWorkingFile.trim());
                    currentVolume = 0;
                    String filename = match;
                    match = new Regex(filename, "\\.part(\\d+)\\.").getMatch(0);
                    if (match != null) {
                        currentVolume = Integer.parseInt(match.trim());
                    } else {
                        match = new Regex(filename, "(.*?)\\.rar").getMatch(0);
                        if (match != null) {
                            currentVolume = 1;
                        } else {
                            match = new Regex(filename, "\\.r(\\d+)").getMatch(0);
                            if (match != null) currentVolume = Integer.parseInt(match.trim()) + 2;
                        }
                    }
                    exec.interrupt();
                }

            }
        }
    }

    public void go() throws Exception {
        run();
    }
}
