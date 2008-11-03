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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Executer extends Thread {
    class StreamObserver extends Thread {

        private BufferedInputStream reader;
        
        private DynByteBuffer dynbuf;
        private boolean started;
        private boolean interruptRequestet = false;
        private boolean eof = false;

        public StreamObserver(InputStream stream, DynByteBuffer buffer) {
            reader = new BufferedInputStream(stream);
           
            dynbuf = buffer;
        }

        @Override
        public void run() {
            this.started = true;
            int num;
            try {
                fireEvent(dynbuf,0,this==Executer.this.sbeObserver?Executer.LISTENER_ERRORSTREAM:Executer.LISTENER_STDSTREAM);
                while (!eof) {
                    num = readLine();
                    if (!interruptRequestet) eof = false;
                    if (num <= 0) {
                        Thread.sleep(50);
                    } else {
                        String line = new String(dynbuf.getLast(num)).trim();

                        if (line.length() > 0) {
                            System.out.println(this + ": " + line);
                            // System.out.println(">"+line);
                            fireEvent(line, dynbuf,this==Executer.this.sbeObserver?Executer.LISTENER_ERRORSTREAM:Executer.LISTENER_STDSTREAM);
                           // dynbuf.clear();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
           
        }

        private int readLine() throws IOException, InterruptedException {
            int i = 0;
            byte[] buffer = new byte[1];
            for (;;) {

                if (this.isInterrupted()) {

                throw new InterruptedException(); }
                int read = 0;
                if ((read = reader.read(buffer)) < 0) {
                    this.eof = true;
                    return i;
                }
                i += read;
              
                dynbuf.put(buffer, read);

                if (buffer[0] == '\b' || buffer[0] == '\r' || buffer[0] == '\n') {

                return i; }
                fireEvent(dynbuf,read,this==Executer.this.sbeObserver?Executer.LISTENER_ERRORSTREAM:Executer.LISTENER_STDSTREAM);
            }

        }

        public boolean isStarted() {
            return started;
        }

        public void requestInterrupt() {
            this.interruptRequestet = true;

        }

    }

    public static int LISTENER_ERRORSTREAM = 1;
    public static int LISTENER_STDSTREAM = 1 << 1;

    private String command;
    private Logger logger = JDUtilities.getLogger();
    private ArrayList<String> parameter;
    private String runIn;
    private DynByteBuffer inputStreamBuffer;
    private DynByteBuffer errorStreamBuffer;
    private ArrayList<ProcessListener> listener = new ArrayList<ProcessListener>();
    private ArrayList<ProcessListener> elistener = new ArrayList<ProcessListener>();

    private int waitTimeout = 60;
    private int exitValue = -1;

    private Process process;
    private StreamObserver sbeObserver;
    private StreamObserver sboObserver;
    private OutputStream outputStream=null;
   

    public Executer(String command) {
        this.command = command;
        parameter = new ArrayList<String>();

        inputStreamBuffer = new DynByteBuffer(1024 * 4);
        errorStreamBuffer = new DynByteBuffer(1024 * 4);
        setName("Executer: " + command);

    }

    public void addParameter(String par) {
        parameter.add(par);
    }

    public void addParameters(String[] par) {
        if (par == null) return;
        for (String p : par) {
            parameter.add(p);
        }
    }

    public String getCommand() {
        return command;
    }

    public String getErrorStream() {
        return errorStreamBuffer.toString();
    }

    public ArrayList<String> getParameter() {
        return parameter;
    }

    public String getRunin() {
        return runIn;
    }

    public String getOutputStream() {
        return inputStreamBuffer.toString();
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

        String out = "";
        for (String p : params) {
            out += p + " ";
        }
        logger.finest("Execute: " + out);
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
                    logger.severe("Working directory " + runIn + " does not exist!");
                }
            }
        }

        try {

            process = pb.start();
           
            if (waitTimeout == 0) { return; }
            outputStream = process.getOutputStream();
            sbeObserver = new StreamObserver(process.getErrorStream(), errorStreamBuffer);
            sbeObserver.setName(this.getName()+" ERRstreamobserver");
            sboObserver = new StreamObserver(process.getInputStream(), inputStreamBuffer);
            sbeObserver.setName(this.getName()+" STDstreamobserver");
            sbeObserver.start();
            sboObserver.start();

            if (waitTimeout > 0) {
                Thread timeoutThread = new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(waitTimeout * 1000);
                        } catch (InterruptedException e) {
                            return;
                        }

                        try {
                            process.destroy();
                        } catch (Exception e) {
                        }
                    }
                };
                timeoutThread.start();
            }
            if (waitTimeout == 0) return;
            try {
                process.waitFor();
            } catch (InterruptedException e1) {
                process.destroy();
                e1.printStackTrace();
            }
            exitValue = process.exitValue();
            if (sboObserver.isAlive()) {
                sboObserver.requestInterrupt();
            }
            if (sbeObserver.isAlive()) {
                sbeObserver.requestInterrupt();
            }

        } catch (IOException e1) {

            e1.printStackTrace();
            return;
        }
    }

    public Process getProcess() {
        return process;
    }

    public void interrupt() {
        super.interrupt();
        if (sbeObserver != null) this.sbeObserver.interrupt();
        if (sboObserver != null) this.sboObserver.interrupt();
        process.destroy();
    }

    public DynByteBuffer getInputStreamBuffer() {
        return inputStreamBuffer;
    }

 

    public void writetoOutputStream(String data) {
        if (data == null || data.length() == 0) data="";
        data = data.trim() + "\n";
        try {
            outputStream.write(data.getBytes());
            System.out.println("Out>"+data);
            outputStream.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public DynByteBuffer getErrorStreamBuffer() {
        return errorStreamBuffer;
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
        while (isAlive() || this.sbeObserver.isAlive() || this.sboObserver.isAlive()) {
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

    public void addProcessListener(ProcessListener listener, int flag) {
        this.removeProcessListener(listener, flag);

        if ((flag & Executer.LISTENER_STDSTREAM) > 0) this.listener.add(listener);
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) this.elistener.add(listener);

    }

    private void fireEvent(String line, DynByteBuffer sb,int flag) {
        if ((flag & Executer.LISTENER_STDSTREAM) > 0)
        for (ProcessListener listener : this.listener) {
            listener.onProcess(this, line, sb);
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0)
        for (ProcessListener elistener : this.elistener) {
            elistener.onProcess(this, line, sb);
        }
    }

    private void fireEvent(DynByteBuffer buffer,int read,int flag) {
        if ((flag & Executer.LISTENER_STDSTREAM) > 0)
        for (ProcessListener listener : this.listener) {
            listener.onBufferChanged(this, buffer,read);
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0)
        for (ProcessListener elistener : this.elistener) {
            elistener.onBufferChanged(this,buffer,read);
        }
    }

    public void removeProcessListener(ProcessListener listener, int flag) {
        if ((flag & Executer.LISTENER_STDSTREAM) > 0) this.listener.remove(listener);
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) this.elistener.remove(listener);

    }

}
