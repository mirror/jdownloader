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

package jd.http.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;

import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.Threader;
import jd.nutils.Threader.Worker;
import jd.nutils.Threader.WorkerListener;
import jd.nutils.jobber.JDRunnable;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class HTTPDownload extends DownloadInterface {
    /**
     * Flag indicates, that the server allows resuming
     */
    public static final int FLAG_RESUME = 1 << 0;
    /**
     * indicates, that the stored filesize is correct.
     */
    private static final int FLAG_FILESIZE_CORRECT = 1 << 1;

    private Request orgRequest;

    private int desiredChunkNum;
    private Threader chunks;

    private long fileSize;
    private RandomAccessFile outputRAF;
    private FileChannel outputChannel;
    // private long byteCounter;
    private long bandwidthLimit;
    private int activeChunks = 0;

    // private Thread writer;
    // private DownloadChunk chunkToWrite = null;
    private DownloadProgress downloadProgress;
    private Browser browser;

    // private static int STATUS = 0;

    public Browser getBrowser() {
        return browser;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public synchronized int getActiveChunks() {
        return activeChunks;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public HTTPDownload(Request request, Browser br, File file, int flags) {
        super(flags, file);
        this.orgRequest = request;
        this.browser = br;
        downloadProgress = new DownloadProgress(file);

    }

    public HTTPDownload(Request request, Browser br, File file) {
        super(0, file);
        this.orgRequest = request;
        this.browser = br;
        downloadProgress = new DownloadProgress(file);

    }

    public static void debug(String msg) {
        try {
            throw new Exception();
        } catch (Exception ee) {
            System.err.println(ee.getStackTrace()[1].getClassName() + "." + ee.getStackTrace()[1].getMethodName() + "[" + ee.getStackTrace()[1].getLineNumber() + "] " + msg);
        }
    }

    public static void main(String[] args) {
        try {

            String destPath = "c:/test.download";
            Browser br = new Browser();
            br.setDebug(true);
            Request request = br.createGetRequest("http://service.jdownloader.org/testfiles/25bmtest.zip");

            final HTTPDownload dl = new HTTPDownload(request, br, new File(destPath), HTTPDownload.FLAG_RESUME);
            // dl.setBandwidthlimit(1024*100);
            dl.setDesiredChunkNum(3);
            dl.addDownloadListener(new DownloadListener() {

                public void onStatus(DownloadEvent downloadEvent) {
                    System.out.print("New eventid: " + downloadEvent.getId());

                }

            });
            try {
                new Thread() {
                    public void run() {
                        try {

                            Thread.sleep(2000);
                            // dl.setBandwidthlimit(200 * 1000);
                            // System.out.println("Interrupt");
                            // th.interrupt();

                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                    }
                }.start();
                dl.start();
                long crc = JDUtilities.getCRC(new File(destPath));

                if ("862E7007".trim().endsWith(Long.toHexString(crc).toUpperCase())) {
                    System.out.println("CRC OK");
                } else {
                    System.out.println("CRC FAULT");
                }
            } catch (BrowserException e) {
                JDLogger.exception(e);
                if (e.getType() == BrowserException.TYPE_LOCAL_IO) {
                    new File(destPath).delete();
                    JDLogger.exception(e);
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    public void setBandwidthLimit(long bytesPerSecond) {
        this.bandwidthLimit = bytesPerSecond;

    }

    public void setDesiredChunkNum(int i) throws BrowserException, InterruptedException {
        if (i == desiredChunkNum) return;

        this.desiredChunkNum = i;
        updateChunks();

    }

    /**
     * Tries to start enough chunks to fullfill the desiredChunkNum.
     * 
     * @throws BrowserException
     * @throws InterruptedException
     */
    private void updateChunks() throws BrowserException, InterruptedException {
        if (chunks == null || !chunks.isHasStarted()) return;
        int num = getActiveChunks();
        if (num == desiredChunkNum) return;
        for (int i = chunks.size() - 1; i >= 0; i--) {
            DownloadChunk dc = (DownloadChunk) chunks.get(i);
            if (!dc.isConnectionRequested()) { return; }
        }

        if (num < desiredChunkNum) {
            System.out.println("AddChunk");
            addChunksDyn(1);
        }
        if (num > desiredChunkNum) {
            System.out.println("RemoveChunk");
            removeChunksDyn(1);
        }

    }

    private void removeChunksDyn(int i) throws InterruptedException {
        while (i-- > 0) {
            removeChunk();
        }

    }

    private void removeChunk() throws InterruptedException {
        DownloadChunk slowest = null;
        for (int i = chunks.size() - 1; i >= 0; i--) {
            DownloadChunk dc = (DownloadChunk) chunks.get(i);
            if (dc.isAlive()) {
                slowest = dc;
                break;
            }
        }
        if (slowest == null) return;
        System.out.println("Disconnect chunk " + slowest + " remaining: " + slowest.getRemainingChunkBytes() + " " + slowest.getChunkBytes());
        slowest.setChunkEnd(slowest.getChunkStart() + slowest.getChunkBytes());

        // Wait until Chunk got closed
        while (slowest.isAlive()) {
            System.out.print("|");
            Thread.sleep(50);
        }

        System.out.println("Disconnected");

    }

    private void addChunksDyn(int i) throws BrowserException {
        while (i-- > 0) {
            addChunk();
        }
    }

    private void addChunk() throws BrowserException {
        DownloadChunk biggestRemaining = null;
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println(chunks.get(i) + ":" + ((DownloadChunk) chunks.get(i)).getRemainingChunkBytes());
            if (biggestRemaining == null || biggestRemaining.getRemainingChunkBytes() < ((DownloadChunk) chunks.get(i)).getRemainingChunkBytes()) {
                biggestRemaining = ((DownloadChunk) chunks.get(i));
            }
        }
        /*
         * Started abhängig vonm aktuellen downloadspeed einen neuen chunk oder
         * nicht.
         */
        if (biggestRemaining.getRemainingChunkBytes() < Math.max(512 * 1024 * 1024l, this.getSpeed() * 10)) {
            System.out.println(biggestRemaining + " New size(to small)");
            return;
        }
        long newSize = biggestRemaining.getRemainingChunkBytes() / 2;
        System.out.println(biggestRemaining + " New size: " + newSize);
        long old = biggestRemaining.getChunkEnd();
        biggestRemaining.setChunkEnd(biggestRemaining.getChunkStart() + biggestRemaining.getChunkBytes() + newSize);

        DownloadChunk newChunk = new DownloadChunk(this, biggestRemaining.getChunkEnd() + 1, old);
        System.out.println("SPLIT: " + biggestRemaining + " + " + newChunk);
        chunks.add(newChunk);

    }

    public void start() throws IOException, BrowserException {

        // If resumeFlag is set and ResumInfo Import fails, initiate the Default
        // ChunkSetup
        this.fireEvent(DownloadEvent.STATUS_STARTED);
        this.initOutputChannel();
        if (hasStatus(FLAG_RESUME) || !importResumeInfos()) {

            this.initChunks();

        }
        this.fireEvent(DownloadEvent.STATUS_CONNECTED);
        // this.byteCounter = 0l;
        // this.startWriter();
        this.download();
        this.fireEvent(DownloadEvent.STATUS_DOWNLOAD_FISNISHED);
        System.out.println("Close and UNlock file");
        this.closeFileDiscriptors();
        this.clean();
        this.fireEvent(DownloadEvent.STATUS_FINISHED);

    }

    // private void startWriter() {
    // this.writer = new Thread() {
    // public void run() {
    //
    // while (!this.isInterrupted()) {
    //
    // if (chunks.isHasStarted() && chunks.isHasDied()) return;
    //
    // try {
    // writeWaitingChunk();
    // } catch (InterruptedException e) {
    // JDLogger.exception(e);
    // } catch (IOException e) {
    // JDLogger.exception(e);
    // }
    //
    // }
    // }
    //
    // };
    //
    // writer.start();
    // }

    private void closeFileDiscriptors() {
        try {
            outputChannel.force(false);
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        try {
            outputRAF.close();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        try {
            outputChannel.close();
        } catch (Exception e) {
            JDLogger.exception(e);
        }

    }

    /**
     * Renames all files and deletes tmp files
     * 
     * @throws BrowserException
     */
    private void clean() throws BrowserException {
        if (!new File(getOutputFile().getAbsolutePath() + ".part").renameTo(getOutputFile())) { throw new BrowserException(JDL.L("exceptions.browserexception.couldnotrenam", "Could not rename getOutputFile()"), BrowserException.TYPE_LOCAL_IO);

        }
        if (!new File(getOutputFile().getAbsolutePath() + ".jdp").delete()) {
            new File(getOutputFile().getAbsolutePath() + ".jdp").deleteOnExit();
        }
    }

    private void download() {
        chunks.startAndWait();

    }

    private void initOutputChannel() throws FileNotFoundException, BrowserException {
        if (getOutputFile().exists()) { throw new BrowserException(JDL.L("exceptions.browserexception.alreadyexists", "getOutputFile() already exists"), BrowserException.TYPE_LOCAL_IO);

        }

        if (new File(getOutputFile().getAbsolutePath() + ".part").exists() && !this.hasStatus(FLAG_RESUME)) {
            if (!new File(getOutputFile().getAbsolutePath() + ".part").delete()) { throw new BrowserException("Could not delete *.part file", BrowserException.TYPE_LOCAL_IO); }
        }
        if (!getOutputFile().getParentFile().exists()) {
            getOutputFile().getParentFile().mkdirs();
        }

        outputRAF = new RandomAccessFile(getOutputFile().getAbsolutePath() + ".part", "rw");
        outputChannel = outputRAF.getChannel();

    }

    /**
     * Funktion überprüft ob eventl noch chunks hinzugefügt werden müssen um die
     * datei zu ende zu laden. z.B. wenn chunks abgebrochen wurden.
     * 
     * @throws BrowserException
     */
    private synchronized void checkForMissingParts() throws BrowserException {
        ArrayList<Long[]> missing = getMissingParts();
        if (missing.size() == 0) return;
        int activeChunks = chunks.getAlive().size();
        System.out.println("Active chunks: " + activeChunks);
        System.out.println("Missing parts: " + missing.size());

        for (int i = 0; i < missing.size(); i++) {
            System.out.println("Missing: " + missing.get(i)[0] + "-" + missing.get(i)[1]);
        }
        int i = 0;
        while (activeChunks < this.desiredChunkNum && missing.size() > i) {
            DownloadChunk newChunk = new DownloadChunk(this, missing.get(i)[0], missing.get(i)[1]);
            System.out.println("New chunk: " + newChunk);
            chunks.add(newChunk);
            i++;
            activeChunks++;

        }
    }

    private ArrayList<Long[]> getMissingParts() {
        ArrayList<Long[]> missing = new ArrayList<Long[]>();
        chunks.sort(new Comparator<Worker>() {
            public int compare(Worker o1, Worker o2) {
                return Long.valueOf(((DownloadChunk) o1.getRunnable()).getChunkStart()).compareTo(Long.valueOf(((DownloadChunk) o2.getRunnable()).getChunkStart()));
            }
        });

        DownloadChunk lastChunk = (DownloadChunk) chunks.get(0);
        if (lastChunk.getChunkStart() > 0) {

            missing.add(new Long[] { 0l, lastChunk.getChunkStart() - 1 });
        }
        for (int i = 1; i < chunks.size(); i++) {
            DownloadChunk chunk = (DownloadChunk) chunks.get(i);
            if (chunk.getChunkStart() == lastChunk.getChunkEnd() + 1) {
                // all ok

            } else if (chunk.getChunkStart() < lastChunk.getChunkEnd() + 1) {
                System.err.println("Overlap  Chunks: " + chunk + " - " + lastChunk);
            } else {
                Long[] add;
                missing.add(add = new Long[] { lastChunk.getChunkEnd() + 1, ((DownloadChunk) chunks.get(i)).getChunkStart() - 1 });
                if (add[0] == 0 && add[1] == -1) {
                    add[0] = 0l;
                }
            }
            lastChunk = chunk;
            /*
             * 0 - 4377600 4377601 - 8755200 8755201 - 13132800 13132801 -
             * 17510400 17510401 - 18034688 18034689 - 21888000 21888001 -
             * 22412288 22412289 - 26265599
             */
        }
        if (lastChunk.getChunkEnd() != -1 && lastChunk.getChunkEnd() != this.fileSize - 1) {
            missing.add(new Long[] { lastChunk.getChunkEnd() + 1, -1l });

        }
        return missing;
    }

    /**
     * Chunk [17510401 - 17903617] Method creates the initial Chunks
     * 
     * @throws IOException
     * @throws BrowserException
     */
    private void initChunks() throws IOException, BrowserException {
        this.chunks = new Threader();
        chunks.getBroadcaster().addListener(new WorkerListener() {

            public void onThreadException(Threader th, JDRunnable job, Throwable e) {
                System.err.println(job);
                JDLogger.exception(e);
            }

            public void onThreadFinished(Threader th, JDRunnable job) {
                try {
                    updateActiveChunkCount(-1);
                    fireEvent(new DownloadEvent(DownloadEvent.PROGRESS_CHUNK_FINISHED, HTTPDownload.this, (DownloadChunk) job));

                    checkForMissingParts();

                    updateChunks();
                } catch (BrowserException e) {
                    JDLogger.exception(e);
                } catch (InterruptedException e) {
                    JDLogger.exception(e);
                }
                updateSpeedMeters();
            }

            private void updateSpeedMeters() {
                if (chunks == null || activeChunks == 0) return;
                for (int i = 0; i < chunks.size(); i++) {
                    ((DownloadChunk) chunks.get(i)).resetSpeedMeter();
                }
            }

            public void onThreadStarts(Threader threader, JDRunnable runnable) {
                updateActiveChunkCount(+1);
                fireEvent(new DownloadEvent(DownloadEvent.PROGRESS_CHUNK_STARTED, HTTPDownload.this, (DownloadChunk) runnable));

                try {
                    updateChunks();
                } catch (BrowserException e) {
                    JDLogger.exception(e);
                } catch (InterruptedException e) {
                    JDLogger.exception(e);
                }
            }

        });
        DownloadChunk chunk = new DownloadChunk(this);

        // 0-Chunk has to be Rangeless.
        orgRequest.getHeaders().remove("Range");
        chunk.connect();

        System.out.println(orgRequest.printHeaders());
        if (orgRequest.getContentLength() > 0) {
            this.fileSize = orgRequest.getContentLength();
            this.addStatus(FLAG_FILESIZE_CORRECT);
        }
        System.out.println("Chunksize: " + (fileSize / desiredChunkNum));
        chunk.setRange(0l, fileSize / desiredChunkNum + 1);
        chunks.add(chunk);

        for (int i = 1; i < desiredChunkNum; i++) {
            if (i < desiredChunkNum - 1) {
                chunk = new DownloadChunk(this, chunk.getChunkEnd(), fileSize * (i + 1) / desiredChunkNum + 1);
            } else {
                chunk = new DownloadChunk(this, chunk.getChunkEnd(), -1);

            }

            chunks.add(chunk);

        }
        // TODO
        // Fehler müssen hier noch abgefangen werden, z.B. über listener

    }

    protected synchronized void updateActiveChunkCount(int i) {
        this.activeChunks += i;
        System.out.println("Active CHunks " + activeChunks + "(" + i + ")");

    }

    /**
     * Liest die file.name.jdp (J_dD_ownloadP_rogress) Datei ein.
     */
    private boolean importResumeInfos() {
        return false;
    }

    //
    // public synchronized void writeWaitingChunk() throws InterruptedException,
    // IOException {
    // // Warte bis chunk zu schreiben anliegt
    // while (STATUS != 1) {
    // System.out.println("Nothing to write");
    // wait();
    //
    // }
    //
    // // Schreibe Chunk
    //
    // ByteBuffer buffer = chunkToWrite.getBuffer();
    // buffer.flip();
    //
    // outputRAF.seek(chunkToWrite.getWritePosition());
    // // System.out.println(chunkToWrite + "Write " +
    // chunkToWrite.getWritePosition() + "-" + (chunkToWrite.getWritePosition()
    // + buffer.limit()) + " : " + this.getSpeed() + " " + buffer.limit());
    // byteCounter += buffer.limit();
    //
    // outputChannel.write(buffer);
    //
    // // benachrichtige gib chunkwarteplatz wieder frei
    // STATUS = 2;
    // // debug("STATUS = " + STATUS);
    // notifyAll();
    //
    // }

    private synchronized void updateDownloadProgress() {

        downloadProgress.reset(chunks.size());
        downloadProgress.totalLoaded = 0;
        for (int i = 0; i < chunks.size(); i++) {
            DownloadChunk chunk = (DownloadChunk) chunks.get(i);
            ChunkProgress cp = chunk.getChunkProgress();
            downloadProgress.add(cp);
            downloadProgress.totalLoaded += cp.getEnd() - cp.getStart() - 1;
        }

    }

    public DownloadProgress getDownloadProgress() {
        return downloadProgress;
    }

    public FileChannel getOutputChannel() {
        return outputChannel;
    }

    // public synchronized void setChunkToWrite(DownloadChunk downloadChunk)
    // throws InterruptedException {
    // // warten bis schreibslot frei ist
    // while (STATUS != 0) {
    // System.out.println("No free Slot for " + downloadChunk);
    //
    // this.wait();
    //
    // }
    // // schreibslot belegen
    // System.out.println("Queued to write next: " + downloadChunk + " " +
    // downloadChunk.getBuffer().position());
    // chunkToWrite = downloadChunk;
    //
    // STATUS = 1;
    // // debug("STATUS = " + STATUS);
    // notifyAll();
    //
    // }

    // public synchronized void waitForWriter(DownloadChunk downloadChunk)
    // throws InterruptedException {
    // // warten bis schreibslot gelöscht
    // while (STATUS != 2) {
    // System.out.println("Waiting :" + downloadChunk);
    // this.wait();
    //
    // }
    // STATUS = 0;
    // // debug("STATUS = " + STATUS);
    // notifyAll();
    //
    // }

    public Request getRequest() {

        return this.orgRequest;
    }

    public long getChunkBandwidth() {
        if (this.bandwidthLimit <= 0) return -1;
        return Math.max(1, bandwidthLimit / this.getActiveChunks() + bandwidthLimit);

    }

    protected void onBufferWritten(DownloadChunk downloadChunk) {
        fireEvent(new DownloadEvent(DownloadEvent.PROGRESS_CHUNK_BUFFERWRITTEN, this, downloadChunk));
        updateDownloadProgress();
    }

    public long getSpeed() {
        long speed = 0;
        if (chunks == null || this.activeChunks == 0) return 0;
        for (int i = 0; i < chunks.size(); i++) {
            speed += ((DownloadChunk) chunks.get(i)).getSpeed();
        }

        return speed;
    }

    // @Override
    public long getBandwidthLimit() {
        return this.bandwidthLimit;
    }

}
