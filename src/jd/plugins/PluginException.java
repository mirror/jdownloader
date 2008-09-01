//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

    private static final long serialVersionUID = -413339039711789194L;
    private int linkStatus = -1;
    private String errorMessage = null;
    private long value = -1;

    public PluginException(int linkStatus) {
        this.linkStatus = linkStatus;
    }

    public PluginException(int linkStatus, String errorMessage, long value) {
        this(linkStatus);
        this.errorMessage = errorMessage;
        this.value = value;
    }

    public PluginException(int linkStatus, String errorMessage) {
        this(linkStatus);
        this.errorMessage = errorMessage;

    }

    public int getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(int linkStatus) {
        this.linkStatus = linkStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void fillLinkStatus(LinkStatus linkStatus) {
        if (this.linkStatus >= 0) linkStatus.addStatus(this.linkStatus);
        if (value >= 0) linkStatus.setValue(value);
        if (errorMessage != null) linkStatus.setErrorMessage(errorMessage);
    }

}
