package jd.plugins.optional.extraction;

/**
 * Constants for the {@link ExtractionController}
 * 
 * @author botzi
 *
 */
public class ExtractionControllerConstants {
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
}