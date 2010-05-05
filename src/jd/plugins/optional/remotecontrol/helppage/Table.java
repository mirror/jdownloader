//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.remotecontrol.helppage;

import java.util.Vector;

public class Table {

    private int index = -1;
    private String name = "";
    private String id = "";

    private Vector<Entry> entries = new Vector<Entry>();

    public Table(String name) {
        this.setName(name);
    }

    public Table(String name, String id) {
        this(name);
        this.setId(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setCommand(String command) {
        entries.add(new Entry(command));
        index++;
    }

    public void setInfo(String info) {
        entries.get(index).setInfo(info);
    }

    public Vector<Entry> getEntries() {
        return entries;
    }

    public int size() {
        return (index + 1);
    }
}
