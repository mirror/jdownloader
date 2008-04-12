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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.awt.event.ActionListener;

public abstract class PluginOptional extends Plugin implements ActionListener {

    @Override
    public boolean doBotCheck(File file) { return false; }

    @Override public PluginStep doStep(PluginStep step, Object parameter) { return null; }
    @Override public String getHost()            { return null; }
    @Override public String getLinkName()        { return null; }
    @Override public Pattern getSupportedLinks() { return null; }
    @Override
    public String getPluginName() {
        return "JDTrayIcon";
    }


    public abstract void enable(boolean enable) throws Exception;
    public abstract String getRequirements();
    public abstract boolean isExecutable();
    public abstract boolean execute();
    public abstract ArrayList<String> createMenuitems();
}
