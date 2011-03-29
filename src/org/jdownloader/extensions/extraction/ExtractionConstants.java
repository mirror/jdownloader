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

package org.jdownloader.extensions.extraction;

/**
 * Various constants for the extraction system.
 * 
 * @author botzi
 * 
 */
public class ExtractionConstants {
    public static final int    WRAPPER_ON_PROGRESS                             = 6;
    public static final int    WRAPPER_STARTED                                 = 7;
    public static final int    WRAPPER_START_OPEN_ARCHIVE                      = 8;
    public static final int    WRAPPER_OPEN_ARCHIVE_SUCCESS                    = 9;
    public static final int    WRAPPER_PASSWORD_FOUND                          = 10;
    public static final int    WRAPPER_EXTRACTION_FAILED                       = 12;
    public static final int    WRAPPER_START_EXTRACTION                        = 13;
    public static final int    WRAPPER_PASSWORD_NEEDED_TO_CONTINUE             = 14;
    public static final int    WRAPPER_FINISHED_SUCCESSFULL                    = 15;
    public static final int    WRAPPER_EXTRACTION_FAILED_CRC                   = 16;
    public static final int    WRAPPER_CRACK_PASSWORD                          = 17;
    public static final int    WRAPPER_PASSWORT_CRACKING                       = 18;
    public static final int    NOT_ENOUGH_SPACE                                = 19;
    public static final int    WRAPPER_FILE_NOT_FOUND                          = 20;
    public static final int    REMOVE_ARCHIVE_METADATA                         = 98;
    public static final String CONFIG_KEY_UNPACKPATH                           = "UNPACKPATH";
    public static final String CONFIG_KEY_USE_SUBPATH                          = "USE_SUBPATH";
    public static final String CONFIG_KEY_SUBPATH                              = "SUBPATH";
    public static final String CONFIG_KEY_SUBPATH_NO_FOLDER                    = "SUBPATH_NO_FOLDER";
    public static final String CONFIG_KEY_REMVE_AFTER_EXTRACT                  = "REMOVE_AFTER_EXTRACT";
    public static final String CONFIG_KEY_ASK_UNKNOWN_PASS                     = "ASK_UNKNOWN_PASS";
    public static final String CONFIG_KEY_OVERWRITE                            = "OVERWRITE";
    public static final String CONFIG_KEY_USE_EXTRACT_PATH                     = "USE_EXTRACT_PATH";
    public static final String CONFIG_KEY_DEEP_EXTRACT                         = "DEEP_EXTRACT";
    public static final String CONFIG_KEY_REMOVE_INFO_FILE                     = "REMOVE_INFO_FILE";
    public static final String CONFIG_KEY_SUBPATH_MINNUM                       = "KEY_SUBPATH_MINNUM";
    public static final String CONFIG_KEY_ADDITIONAL_SPACE                     = "CONFIG_KEY_ADDITIONAL_SPACE";
    public static final String CONFIG_KEY_COPY_FILES_TO_BASE_DIR_AFTER_EXTRACT = "COPY_FILES_TO_BASE_DIR_AFTER_EXTRACT";
    public static final String CONFIG_KEY_MATCHER                              = "MATCHER";
    public static final String DOWNLOADLINK_KEY_EXTRACTTOPATH                  = "EXTRAXT_TO_PATH";
    public static final String DOWNLOADLINK_KEY_EXTRACTEDPATH                  = "EXTRACTEDPATH";
    public static final String CONFIG_KEY_LIST                                 = "LIST";
}