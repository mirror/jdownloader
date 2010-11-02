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

package jd.plugins.optional.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.nutils.jobber.JDRunnable;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

/**
 * Responsible for the coorect procedure of the extraction process.
 * Contains one IExtraction instance.
 * 
 * @author botzi
 *
 */
public class ExtractionController extends Thread implements JDRunnable {
    private ArrayList<ExtractionListener> listener = new ArrayList<ExtractionListener>();
    private ArrayList<String> passwordList;

    private SubConfiguration config = null;

    private Exception exception;
    private boolean removeAfterExtraction;
    private ProgressController progressController;
    
    private Archive archive;
    
    private IExtraction extractor;
    
    ExtractionController(Archive archiv, IExtraction extractor) {
        this.archive = archiv;
        this.extractor = extractor;
        
        extractor.setArchiv(archiv);
        extractor.setExtractionController(this);
        
        config = SubConfiguration.getConfig(JDL.L("plugins.optional.extraction.name", "Extraction"));
    }

    /**
     * Adds an listener to the current unpack process.
     * 
     * @param listener
     */
    public void addExtractionListener(ExtractionListener listener) {
        this.removeExtractionListener(listener);
        this.listener.add(listener);
    }

    /**
     * Removes an listener from the current unpack process.
     * 
     * @param listener
     */
    private void removeExtractionListener(ExtractionListener listener) {
        this.listener.remove(listener);
    }
    
    /**
     * Checks if the extracted file(s) has enough space. Only wokrs with Java 6 or higher.
     * 
     * @return True if it's enough space.
     */
    private boolean checkSize() {
        if(System.getProperty("java.version").contains("1.5")) {
            return true;
        }
        
        File f = archive.getExtractTo();
        
        while(!f.exists()) {
            f = f.getParentFile();
            
            if(f == null) return false;
        }

        //TODO: Make Buffer setable over the configuration
        //Set 500MB extra Buffer
        long size = 1024 * 1024 * 1024 * 500;
        
        for(DownloadLink dlink : DownloadWatchDog.getInstance().getRunningDownloads()) {
            size += dlink.getDownloadSize() - dlink.getDownloadCurrent();
        }
        
        if(f.getUsableSpace() < size + archive.getSize()) {
            return false;
        }
       
        return true;
    }

    @Override
    public void run() {
        try {
            fireEvent(ExtractionConstants.WRAPPER_START_OPEN_ARCHIVE);
            if (extractor.prepare()) {
                if(!checkSize()) {
                    fireEvent(ExtractionConstants.NOT_ENOUGH_SPACE);
                    return;
                }
                
                if (archive.isProtected() && archive.getPassword().equals("")) {
                    fireEvent(ExtractionConstants.WRAPPER_CRACK_PASSWORD);

                    extractor.crackPassword();
                    if (archive.getPassword().equals("")) {
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        return;
                    } else {
                        fireEvent(ExtractionConstants.WRAPPER_PASSWORD_FOUND);
                    }
                }
                fireEvent(ExtractionConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS);
                
                if(!archive.getExtractTo().exists()) {
                    archive.getExtractTo().mkdirs();
                }
                
                fireEvent(ExtractionConstants.WRAPPER_ON_PROGRESS);
                extractor.extract();
                
                switch (archive.getExitCode()) {
                    case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                        if (!archive.getGotInterrupted() && removeAfterExtraction) {
                            removeArchiveFiles();
                        }
                        fireEvent(ExtractionConstants.WRAPPER_FINISHED_SUCCESSFULL);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                        JDLogger.getLogger().warning("A CRC error occurred when unpacking");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED_CRC);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                        JDLogger.getLogger().info(" User interrupted extraction");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                        JDLogger.getLogger().warning("Could not create Outputfile");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_MEMORY_ERROR:
                        JDLogger.getLogger().warning("Not enough memory for operation");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_USER_ERROR:
                        JDLogger.getLogger().warning("Command line option error");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_OPEN_ERROR:
                        JDLogger.getLogger().warning("Open file error");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                        JDLogger.getLogger().warning("Write to disk error");
                        this.exception = new ExtractionException("Write to disk error");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_LOCKED_ARCHIVE:
                        JDLogger.getLogger().warning("Attempt to modify an archive previously locked");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                        JDLogger.getLogger().warning("A fatal error occurred");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    case ExtractionControllerConstants.EXIT_CODE_WARNING:
                        JDLogger.getLogger().warning("Non fatal error(s) occurred");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                    default:
                        JDLogger.getLogger().warning("Unknown Error");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                        break;
                }
                return;
            } else {
                fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
            }
        } catch (Exception e) {
            this.exception = e;
            JDLogger.exception(e);
            fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
        } finally {
            extractor.close();
        }
    }

    /**
     * Deletes the archive files.
     */
    private void removeArchiveFiles() {
        for(DownloadLink link : archive.getDownloadLinks()) {
            new File(link.getFileOutput()).delete();
        }
    }

    /**
     * Returns a thrown exception.
     * 
     * @return The thrown exception.
     */
    public Exception getException() {
        return exception;
    }
    
    /**
     * 
     * Returns the current password finding process.
     * @return
     */
    public int getCrackProgress() {
        return extractor.getCrackProgress();
    }

    /**
     * Fires an event.
     * 
     * @param status
     */
    public void fireEvent(int status) {
        for (ExtractionListener listener : this.listener) {
            listener.onExtractionEvent(status, this);
        }
    }
    
    /**
     * Sets the password list for extraction.
     * 
     * @param passwordList
     */
    public void setPasswordList(ArrayList<String> passwordList) {
        this.passwordList = passwordList;
    }
    
    /**
     * Gets the passwordlist
     * 
     * @return
     */
    public ArrayList<String> getPasswordList() {
        return passwordList;
    }

    /**
     * Returns the ProgressController
     * 
     * @return
     */
    public ProgressController getProgressController() {
        return progressController;
    }

    /**
     * Should the archives be deleted after extracting.
     * 
     * @param setProperty
     */
    public void setRemoveAfterExtract(boolean setProperty) {
        this.removeAfterExtraction = setProperty;
    }

    /**
     * Sets the ProgressController.
     * 
     * @param progress
     */
    public void setProgressController(ProgressController progress) {
        progressController = progress;
    }

    /**
     * Starts the extracting progress.
     */
    public void go() throws Exception {
        run();
    }
    
    /**
     * Returns the {@link Archive}.
     * 
     * @return
     */
    public Archive getArchiv() {
        return archive;
    }
    
    /**
     * Returns the Configuration.
     * 
     * @return
     */
    public SubConfiguration getConfig() {
        return config;
    }
    
    /**
     * Returns the extracted files.
     * 
     * @return
     */
    public List<String> getPostProcessingFiles() {
        return extractor.filesForPostProcessing();
    }
}