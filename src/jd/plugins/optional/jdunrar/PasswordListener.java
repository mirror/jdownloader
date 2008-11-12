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

package jd.plugins.optional.jdunrar;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.utils.DynByteBuffer;
import jd.utils.Executer;
import jd.utils.JDHexUtils;
import jd.utils.ProcessListener;

public class PasswordListener extends ProcessListener {

    private String password;

    private int lastLinePosition = 0;

    public PasswordListener(String pass) {
        this.password = pass;
    }

    @Override 
    public void onBufferChanged(Executer exec, DynByteBuffer buffer, int latestNum) {
        String lastLine;
        try {
            lastLine = new String(buffer.getLast(buffer.position() - lastLinePosition),JDUnrar.CODEPAGE);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            lastLine = new String(buffer.getLast(buffer.position() - lastLinePosition));
            
        }
        
        if (new Regex(lastLine, Pattern.compile(".*?password.{0,200}: $", Pattern.CASE_INSENSITIVE)).matches()) {
            exec.writetoOutputStream(this.password);
        }
        if (new Regex(lastLine, Pattern.compile(".*?password incorrect", Pattern.CASE_INSENSITIVE)).matches()) {
            exec.interrupt();
        } else if (new Regex(lastLine, ".*?current.*?password.*?ll ").matches()) {
            exec.writetoOutputStream("A");
        }
        
       
    }

    @Override
    public void onProcess(Executer exec, String latestLine, DynByteBuffer sb) {
        this.lastLinePosition = sb.position();
    }

}
