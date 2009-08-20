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

package jd;

public class Benchmark {

    private static final long DURATION = 5000;

    public static void main(String[] args) {
        Runnable r = new Runnable() {
            public void run() {
                while (true) {
                    long start = System.currentTimeMillis();
                    long i = 0;
                    while (System.currentTimeMillis() - start < DURATION) {
                        i++;
                        if (i == Long.MAX_VALUE) {
                            i = 0;
                            System.out.println("overflow");
                        }
                    }
                    System.out.println(start + " : " + (i / DURATION));
                }
            }
        };

        new Thread(r).start();
        new Thread(r).start();
        new Thread(r).start();
        // new Thread(r).start();
    }
}
