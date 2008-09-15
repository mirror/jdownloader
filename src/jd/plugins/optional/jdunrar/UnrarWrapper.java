package jd.plugins.optional.jdunrar;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.utils.Executer;
import jd.utils.ProcessListener;

/**
 * Die klasse dient zum verpacken der Unrar binary.
 * 
 * @author coalado
 * 
 */
public class UnrarWrapper extends Thread implements ProcessListener {

    private static final int CANNOT_FIND_VOLUME = 1;
    private static final int COULD_NOT_FIND_PASSWORD = 1 << 1;
    private static final int OPENED_SUCCESSFULL = 1 << 2;
    private static final int START_EXTRACTION = 1 << 3;
    private static final int NO_FILES_FOUND = 1 << 4;
    private ArrayList<UnrarListener> listener = new ArrayList<UnrarListener>();
    private DownloadLink link;
    private String unrarCommand;
    private String[] passwordList;
    private File file;
    private int status;
    private String password;
    private boolean isProtected = false;
    private ArrayList<ArchivFile> files;
    private boolean overwriteFiles = true;

    private int totalSize;
    private ArchivFile currentlyWorkingOn;
    private String latestStatus;

    public UnrarWrapper(DownloadLink link) {
        this.link = link;
        this.file = new File(link.getFileOutput());
    }

    public void addUnrarListener(UnrarListener listener) {
        this.removeUnrarListener(listener);
        this.listener.add(listener);

    }

    private void removeUnrarListener(UnrarListener listener) {
        this.listener.remove(listener);

    }

    public void run() {

        fireEvent(JDUnrarConstants.EXTRACTION_STARTED);
        this.fireEvent(START_EXTRACTION);
        open();
        if (this.status == OPENED_SUCCESSFULL) {
            if (this.files.size() == 0) {
                this.fireEvent(NO_FILES_FOUND);
            }
            if (this.isProtected && this.password == null) {

                crackPassword();
                password = "test";
            }
            this.extract();
        } else {
            this.fireEvent(this.status);
            fireEvent(JDUnrarConstants.EXTRACTION_FAILED);

            return;
        }

    }

    private void extract() {

        fireEvent(JDUnrarConstants.EXTRACTION_START_EXTRACT);
        Executer exec = new Executer(unrarCommand);
        if (password != "" && password != null) {
            exec.addParameter("-p" + password);

        } else {
            exec.addParameter("-p-");
        }
        if (overwriteFiles) {
            exec.addParameter("-o+");
        } else {
            exec.addParameter("-o-");
        }
        exec.addParameter("-c-");
        exec.addParameter("-v");
        exec.addParameter("-ierr");

        exec.addParameter("x");
        exec.addParameter(file.getName());
        exec.setRunin(file.getParentFile().getAbsolutePath());
        exec.setWaitTimeout(-1);
        exec.addProcessListener(this);

        exec.start();
        exec.waitTimeout();
        String res = exec.getStream() + " \r\n " + exec.getErrorStream();

    }

    private void crackPassword() {
        ArchivFile file = null;
        // suche kleines passwortgeschützte Datei
        for (ArchivFile f : files) {
            if (f.isProtected()) {
                if (file == null) {
                    file = f;
                    continue;
                } else if (f.getSize() < file.getSize()) {
                    file = f;
                }
            }
        }
        if (file.getSize() < 2097152) {
            // Kleine Datei gefunden. Passwort wird anhand vond er kleinsten
            // geschützten Datei gesucht
        } else {
            // keine Kleinen files gefunden. password wird über Dateisignaturen
            // gesucht.

        }

    }

    private void fireEvent(int status2) {
        for (UnrarListener listener : this.listener) {
            listener.onUnrarEvent(status, this);
        }

    }

    /**
     * Öffnet das rararchiv und liest die files ein
     */
    private void open() {
        String pass = null;
        int i = 0;
        fireEvent(JDUnrarConstants.EXTRACTION_OPEN_ARCHIVE);
        while (true) {
            String[] params = new String[6];
            if (i > 0) {
                if (passwordList.length < i) {
                    this.status = COULD_NOT_FIND_PASSWORD;
                    return;
                }
                pass = this.passwordList[i - 1];
            }

            i++;
            if (pass == null || pass == "") {
                params[0] = "-p-";
            } else {
                params[0] = "-p" + pass;
            }

            params[1] = "-v";
            params[2] = "-c-";
            params[3] = "-ierr";
            params[4] = "v";
            params[5] = file.getName();
            Executer exec = new Executer(unrarCommand);
            exec.addParameters(params);
            exec.setRunin(file.getParentFile().getAbsolutePath());
            exec.setWaitTimeout(-1);
            exec.start();

            exec.waitTimeout();
            String res = exec.getStream() + " \r\n " + exec.getErrorStream();
            if (res.indexOf("Cannot find volume") != -1) {
                System.err.println(res);
                this.status = CANNOT_FIND_VOLUME;
                return;
            }
            if (res.indexOf(" (password incorrect ?)") != -1) {
                System.err.println("Password incorrect: " + file.getName() + " pw: " + pass);
                continue;
            } else {

                this.status = OPENED_SUCCESSFULL;
                fireEvent(JDUnrarConstants.EXTRACTION_OPEN_ARCHIVE_SUCCESS);
                Pattern patternvolumes = Pattern.compile("(.*)" + System.getProperty("line.separator") + ".*?([\\d]+).*?[\\d]+.*[\\d]+\\-[\\d]+\\-[\\d]+ [\\d]+:[\\d]+  (.{1})(.{1})(.{1})", Pattern.CASE_INSENSITIVE);
                Matcher matchervolumes = patternvolumes.matcher(res);
                this.files = new ArrayList<ArchivFile>();
                ArchivFile tmp;
                String namen = "";
                this.totalSize = 0;
                while (matchervolumes.find()) {

                    String name = matchervolumes.group(1);
                    // for (int h = 0; h < matchervolumes.groupCount() + 1; h++)
                    // {
                    // System.out.println(h + " : " + matchervolumes.group(h));
                    // }
                    if (name.matches("\\*.*")) {
                        name = name.replaceFirst(".", "");
                        long size = Long.parseLong(matchervolumes.group(2));
                        this.isProtected = true;
                        if (pass != null) {

                            this.password = pass;
                            fireEvent(JDUnrarConstants.EXTRACTION_PASSWORD_FOUND);
                        }
                        if (!name.equals(namen) && !matchervolumes.group(4).equals("D")) {
                            tmp = new ArchivFile(name);
                            tmp.setSize(size);
                            tmp.setProtected(true);
                         
                            files.add(tmp);
                            namen = name;
                            totalSize += size;

                        }
                    } else {
                        name = name.replaceFirst(".", "");
                        if (!name.equals(namen) && !matchervolumes.group(4).equals("D")) {
                         
                            tmp = new ArchivFile(name);
                            long size;
                            tmp.setSize(size = Long.parseLong(matchervolumes.group(2)));
                            totalSize += size;
                            tmp.setProtected(false);
                            files.add(tmp);
                            namen = name;

                        }
                    }
                }
                return;

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

    public void setPasswordList(String[] passwordStringtoArray) {
        this.passwordList = passwordStringtoArray;

    }

    public void onProcess(Executer exec, String err, String out, String latest) {

        if (latest.indexOf(" (password incorrect ?)") != -1) {
            exec.interrupt();
        }
        if (latest.length() > 0) {
            this.latestStatus = latest;
            fireEvent(JDUnrarConstants.EXTRACTION_NEW_STATUS);
        }

        if (latest.length() > 0) {

            // Neue Datei wurde angefangen
            if (latest.trim().matches("[\\s]*Extracting  .*")) {
                String currentWorkingFile = new Regex(latest, "Extracting  (.*)").getMatch(0).trim();
                this.currentlyWorkingOn = getArchivFile(currentWorkingFile);
                this.fireEvent(JDUnrarConstants.NEW_FILE_STARTED);

            }
            // ruft die prozentangaben der aktuellen datei
            if (latest.trim().matches("(\\d+)\\%")) {
                String percent = new Regex(latest, "(\\d+)\\%").getMatch(0);
                currentlyWorkingOn.setPercent(Integer.parseInt(percent));
                this.fireEvent(JDUnrarConstants.EXTRACTION_PROGRESS);
            }

            // datei ok
            if (latest.contains("  OK")) {
                currentlyWorkingOn.setPercent(100);
                this.fireEvent(JDUnrarConstants.EXTRACTION_PROGRESS);
                this.fireEvent(JDUnrarConstants.FILE_EXTRACTED);
            }

        }

    }

    public long getExtractedSize() {
        long size = 0;
        for (ArchivFile af : files) {
            size += af.getSize() * ((double) af.getPercent() / 100.0);
        }
        return size;
    }

    private ArchivFile getArchivFile(String currentWorkingFile) {
        for (ArchivFile af : files) {
            if (af.getFilepath().equals(currentWorkingFile)) { return af; }
        }
        return null;
    }

}
