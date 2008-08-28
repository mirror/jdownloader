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
