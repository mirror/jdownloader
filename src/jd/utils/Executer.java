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

package jd.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Executer extends Thread {
    class StreamObserver extends Thread {

        private BufferedReader reader;
        private StringBuffer sb;

        public StreamObserver(InputStream stream, StringBuffer sb) {
            reader = new BufferedReader(new InputStreamReader(stream));
            this.sb = sb;
        }

        @Override
        public void run() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {

                    sb.append(line + "\r\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private String command;
    private Logger logger = JDUtilities.getLogger();
    private ArrayList<String> parameter;
    private String runIn;
    private StringBuffer sb;
    private StringBuffer sbe;

    private int waitTimeout = 60;
    private int exitValue = -1;

    public Executer(String command) {
        this.command = command;
        parameter = new ArrayList<String>();
        sb = new StringBuffer();
        sbe = new StringBuffer();
        setName("Executer: " + command);

    }

    public void addParameter(String par) {
        parameter.add(par);
    }

    public void addParameters(String[] par) {
        for (String p : par) {
            parameter.add(p);
        }
    }

    public String getCommand() {
        return command;
    }

    public String getErrorStream() {
        return sbe.toString();
    }

    public ArrayList<String> getParameter() {
        return parameter;
    }

    public String getRunin() {
        return runIn;
    }

    public String getStream() {
        return sb.toString();
    }

    public int getWaitTimeout() {
        return waitTimeout;
    }

    @Override
    public void run() {
        if (command == null || command.trim().length() == 0) {
            logger.severe("Execute Parameter error: No Command");
            return;
        }

        ArrayList<String> params = new ArrayList<String>();
        params.add(command);
        params.addAll(parameter);

        // r.info("RUN: " + params);
        ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[] {}));
        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                if (new File(params.get(0)).getParentFile().exists()) {
                    // logger.info("Run in: " + new
                    // File(params.get(0)).getParentFile());
                    pb.directory(new File(params.get(0)).getParentFile());
                } else {
                    logger.severe("Working drectory " + runIn + " does not exist!");
                }
            }
        }

        Process process;

        try {
            process = pb.start();

            if (waitTimeout == 0) { return; }
            StreamObserver sbeObserver = new StreamObserver(process.getErrorStream(), sbe);
            StreamObserver sbObserver = new StreamObserver(process.getInputStream(), sb);
            sbeObserver.start();
            sbObserver.start();

            long waiter = System.currentTimeMillis() + waitTimeout * 1000;
            while (waiter > System.currentTimeMillis() && (sbeObserver.isAlive() || sbObserver.isAlive())) {

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            if (sbObserver.isAlive()) {
                logger.severe("Timeout " + waitTimeout + " kill observerthread(input)");
                sbObserver.interrupt();
            }
            if (sbeObserver.isAlive()) {
                logger.severe("Timeout " + waitTimeout + " kill observerthread(error)");
                sbeObserver.interrupt();
            }
            exitValue = process.exitValue();

            try {
                process.destroy();
            } catch (Exception e) {
            }
        } catch (IOException e1) {

            e1.printStackTrace();
            return;
        }
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setParameter(ArrayList<String> parameter) {
        this.parameter = parameter;
    }

    public void setRunin(String runin) {
        runIn = runin;
    }

    public void setWaitTimeout(int waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public void waitTimeout() {
        while (isAlive()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }

        }
    }

    public int getExitValue() {
        return exitValue;
    }
}
