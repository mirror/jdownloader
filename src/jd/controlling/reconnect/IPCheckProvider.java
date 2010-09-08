//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.controlling.reconnect;

public interface IPCheckProvider {

    /**
     * returns a String with Info about this Provider (eg Website url)
     * 
     * @return
     */
    public String getInfo();

    /**
     * this method should return current IP as String Object or
     * IPCheck.CheckStatus.FAILED if there has been an error or
     * IPCheck.CheckStatus.SEQFAILED if this method should be paused
     * 
     * @Deprecated Returning Object is Bad. Better: return IP, or throw
     *             exceptions
     * @return
     * @see IPCheck.CheckStatus
     */
    public String getIP() throws IPCheckException;

}
