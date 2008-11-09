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

package jd.unrar;

import jd.controlling.ProgressController;

public class ProgressUnrar {
    int pos = 0;
    ProgressController progress = null;

    public ProgressUnrar(String string, int i, boolean progressInTerminal) {
        System.out.println(progressInTerminal);
        if (progressInTerminal) {
            System.out.println(string);
        } else {
            progress = new ProgressController(string, i);
        }
    }

    public void addToMax(int i) {
        if (progress != null) {
            progress.addToMax(i);
        }

    }

    public void finalize() {
        if (progress != null) {
            progress.finalize();
        } else {
            pos = 100;
            System.out.println(100 + " %");

        }
    }

    public void increase(int i) {
        if (progress != null) {
            progress.increase(i);
        } else {
            pos += i;
            System.out.println(pos + " %");

        }

    }

    public void setRange(int i) {
        if (progress != null) {
            progress.setRange(i);
        }

    }

    public void setStatusText(int i) {
        if (progress != null) {
            progress.setStatus(i);
        } else {
            System.out.println(i + " %");
            pos = i;
        }
    }

    public void setStatusText(String string) {
        if (progress != null) {
            progress.setStatusText(string);
        } else {
            System.out.println(string);
        }

    }
}
