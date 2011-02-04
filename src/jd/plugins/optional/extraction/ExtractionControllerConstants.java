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
    public static final int EXIT_CODE_USER_BREAK   = 255;
    /**
     * Create file error
     */
    public static final int EXIT_CODE_CREATE_ERROR = 9;
    /**
     * Write to disk error
     */
    public static final int EXIT_CODE_WRITE_ERROR  = 5;
    /**
     * A CRC error occurred when unpacking
     */
    public static final int EXIT_CODE_CRC_ERROR    = 3;
    /**
     * A fatal error occurred
     */
    public static final int EXIT_CODE_FATAL_ERROR  = 2;
    /**
     * Non fatal error(s) occurred
     * 
     */
    public static final int EXIT_CODE_WARNING      = 1;
    /**
     * Successful operation
     */
    public static final int EXIT_CODE_SUCCESS      = 0;
}