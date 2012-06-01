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

package jd.nutils;

public interface ProcessListener {

    /**
     * 
     * @param exec
     *            Source Object
     * @param latestLine
     *            Die zuletzte gelesene zeile. \b chars werden als new line char
     *            angesehen
     * @param totalBuffer
     *            Der complette BUffer (exec.getInputStringBuilder()|
     *            exec.getErrorStringBuilder())
     */
    public void onProcess(Executer exec, String latestLine, DynByteBuffer totalBuffer);

    public void onBufferChanged(Executer exec, DynByteBuffer totalBuffer, int latestReadNum);

}
