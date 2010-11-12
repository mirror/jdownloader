package jd.plugins.optional.extraction;

import java.util.List;

import jd.config.ConfigContainer;
import jd.config.SubConfiguration;
import jd.plugins.DownloadLink;

/**
 * Interface for the individual extraction programs.
 * 
 * @author botzi
 *
 */
public interface IExtraction {
    /**
     * Builds an {@link Archive} with an finished {@link DownloadLink}. If the {@link DownloadLink} contains only a part of an multipart archive, it has not to be to first one (eg. test.part1.rar).
     * 
     * @param link An complete downloaded file.
     * @return An {@link Archive} that contains all {@link Downloadlink}s.
     */
    public Archive buildArchive(DownloadLink link);
    
    /**
     * Similar to the buildArchive, but contains only dummy {@link DownloadLink}s. It's used to extract arcvhivs that are not in the downloadlist.
     * 
     * @param file Path the first archive.
     * @return An {@link Archive} that contains all dummy {@link Downloadlink}s.
     */
    public Archive buildDummyArchive(String file);
    
    /**
     * Checks a single password if the archive is encrypted with it.
     * 
     * @param password The password.
     * @return True if the password is correct.
     */
    public boolean findPassword(String password);
    
    /**
     * Starts the extraction of an {@link Archive}.
     */
    public void extract();
    
    /**
     * Checks if the extraction method can be used on the system. If it's not possible the method should try to fix that. If that fails it should return false. The extraction method will not be used anymore in this session for extraction.
     * 
     * @return True if all works.
     */
    public boolean checkCommand();
    
    /**
     * Returns the percent of the tested passowords.
     * 
     * @return The percent of the tested passwords.
     */
    public int getCrackProgress();
    
    /**
     * Is used to let the extraction method prepare for the extraction. Will be called after the system started an extraction, but before the {@link crackPassword} and {@link extract}.
     * 
     * @return False  if it's not possible to extract that archive.
     */
    public boolean prepare();
    
    /**
     * Sets a configuration.
     * 
     * @param config
     * @param subConfig
     */
    public void initConfig(ConfigContainer config, SubConfiguration subConfig);
    
    /**
     * Sets the {@link Archive} which should be extracted.
     * 
     * @param archive The {@link Archive}.
     */
    public void setArchiv(Archive archive);
    
    /**
     * Sets the {@link ExtractionController} which controlls the extraction.
     * 
     * @param controller The {@link ExtractionController}.
     */
    public void setExtractionController(ExtractionController controller);
    
    /**
     * Returns the archivename of an {@link Archive}.
     * 
     * @param archive The {@link Archive}.
     * @return The name of the archive.
     */
    public String getArchiveName(DownloadLink link);
    
    /**
     * Checks if the file is supported.
     * 
     * @param file The file which should be checked
     * @return
     */
    public boolean isArchivSupported(String file);
    
    /**
     * Checks if the file from the filefilter is supported.
     * 
     * @param file The file which should be checked.
     * @return 
     */
    public boolean isArchivSupportedFileFilter(String file);
    
    /**
     * Returns the files which should be postprocessed, like additional extraction.
     * 
     * @return The files for postprocessing.
     */
    public List<String> filesForPostProcessing();
    
    /**
     * Sets the  pluginconfiguration.
     * 
     * @param config The configuration.
     */
    public void setConfig(SubConfiguration config);
    
    /**
     * Ends the extraction.
     */
    public void close();
}