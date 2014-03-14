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

package jd.plugins;

public class PluginException extends Exception {
    
    private static final long serialVersionUID              = -413339039711789194L;
    
    public static int         VALUE_ID_PREMIUM_TEMP_DISABLE = 0;
    public static int         VALUE_ID_PREMIUM_DISABLE      = 1;
    /* do not final it, as the compiler will replace Reference with value, no more Exceptions but broken ErrorHandling in stable */
    public static int         VALUE_ID_PREMIUM_ONLY         = 2;
    
    private final int         linkStatus;
    private final String      errorMessage;
    private final long        value;
    
    public PluginException(int linkStatus) {
        this(linkStatus, null, -1);
    }
    
    public PluginException(int linkStatus, String errorMessage, long value) {
        super(errorMessage);
        this.linkStatus = linkStatus;
        this.errorMessage = errorMessage;
        this.value = value;
    }
    
    public PluginException(int linkStatus, String errorMessage) {
        this(linkStatus, errorMessage, -1);
    }
    
    public PluginException(int linkStatus, long value) {
        this(linkStatus, null, value);
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getValue() {
        return value;
    }
    
    /**
     * JD2Only
     * 
     * @return
     */
    public int getLinkStatus() {
        return linkStatus;
    }
    
}
