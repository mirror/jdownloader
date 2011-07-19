package org.jdownloader.extensions.omnibox.awesome;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.event.EventListenerList;

import org.jdownloader.extensions.omnibox.awesome.proposal.AwesomeAlertListener;
import org.jdownloader.extensions.omnibox.awesome.proposal.AwesomeClipboardListener;
import org.jdownloader.extensions.omnibox.awesome.proposal.AwesomeReconnectListener;
import org.jdownloader.extensions.omnibox.awesome.proposal.AwesomeShoutListener;
import org.jdownloader.extensions.omnibox.awesome.proposal.AwesomeStartStopListener;


public class Awesome extends Observable {
    private Map<String, EventListenerList> keywords = new HashMap<String, EventListenerList>();
    private ThreadGroup proposalRequestThreads = new ThreadGroup("proposalRequestThreads");
    private LinkedList<AwesomeProposal> returnedProposals = new LinkedList<AwesomeProposal>();

    private String command = "";
    private String params = "";
    private boolean paramsChanged;
    private boolean commandChanged;

    ReentrantReadWriteLock commandLock = new ReentrantReadWriteLock();
    public ReentrantReadWriteLock returnedProposalsLock = new ReentrantReadWriteLock();

    public static enum EVENTTYPE {
        PROPOSALADDED, PROPOSALSCLEARED, PROPOSALSUPDATE
    }

    /**
     * Initialize new Awesome object
     */
    public Awesome() {
        /* TODO: Implement Plugin system */
        this.registerListener(new AwesomeAlertListener());
        this.registerListener(new AwesomeShoutListener());
        this.registerListener(new AwesomeStartStopListener());
        this.registerListener(new AwesomeReconnectListener());
        this.registerListener(new AwesomeClipboardListener());
    }

    // this.command stuff
    /**
     * @return the current command
     */
    public String getCommand() {
        return this.command;
    }

    /**
     * @return whether the command has changed since the last setInput()
     */
    private boolean isCommandChanged() {
        return commandChanged;
    }

    /**
     * This function sets a new command and stores whether it has changed or not
     * 
     * @param newCommand
     *            the new command to set
     */
    private void setCommand(String newCommand) {
        newCommand = newCommand.trim();
        try {
            commandLock.writeLock().lock();
            if (!this.command.equals(newCommand)) {
                this.command = newCommand;
                this.commandChanged = true;
            } else {
                this.commandChanged = false;
            }
        } finally {
            commandLock.writeLock().unlock();
        }
    }

    // this.params stuff
    /**
     * @return the current params
     */
    public String getParams() {
        return this.params;
    }

    /**
     * @return whether the params have changed since the last setInput()
     */
    private boolean isParamsChanged() {
        return paramsChanged;
    }

    /**
     * This function sets new params and stores whether they have changed or not
     * 
     * @param newParams
     *            new params to set
     */
    private void setParams(String newParams) {
        newParams = newParams.trim();
        if (!this.params.equals(newParams)) {
            this.params = newParams;
            this.paramsChanged = true;
        } else {
            this.paramsChanged = false;
        }
    }

    /**
     * This function sets command and params for a given input string
     * 
     * @see Awesome#getParams()
     * @see Awesome#getCommand()
     * @param in
     *            input string
     */
    private void setInput(String in) {
        in = in.trim();
        int i = in.indexOf(' ');
        if (i > 0) {
            this.setCommand(in.substring(0, i));
            this.setParams(in.substring(i));
        } else {
            this.setCommand(in);
            this.setParams("");
        }
    }

    /**
     * @param listener
     *            the listener which should listen to incoming
     *            AwesomeProposalRequests
     */
    public void registerListener(AwesomeProposalRequestListener listener) {
        for (String keyword : listener.getKeywords()) {
            for (int i = 1; i <= keyword.length(); i++) {
                String key = keyword.substring(0, i);
                if (keywords.containsKey(key)) {
                    keywords.get(keyword.substring(0, i)).add(AwesomeProposalRequestListener.class, listener);
                } else {
                    keywords.put(key, new EventListenerList());
                    keywords.get(keyword.substring(0, i)).add(AwesomeProposalRequestListener.class, listener);
                }

            }
        }
    }

    /**
     * This function requests proposals for the given input string. For a
     * notification of each returned proposal use {@link #addObserver(Observer)}
     * . In order to get all returned proposals use {@link #getProposals()}, but
     * please take care of locking the readLock with
     * {@link #returnedProposalsLock}.
     * 
     * @param in
     *            input string
     */
    public synchronized void requestProposal(String in) {
        this.setInput(in);

        LinkedList<AwesomeProposalRequestListener> necessaryNewRequests = new LinkedList<AwesomeProposalRequestListener>();

        if (!(this.isCommandChanged() || this.isParamsChanged())) {
            // Nothing changed, nothing to update
            return;
        }

        /*
         * If both params and command are empty, delete returned proposals. This
         * wouldn't be necessary of course, but it is a cosmetic fix. Also, it
         * works as a workaround for changed commands with unchanged params
         * because usually the first char changes and is removed as last part of
         * the command: (input one: "command-alias-1 param" returnedProposal:
         * "command-alias-1 param") (input two: "command-alias-2 param"
         * returnedProposal: still "command-alias-1 param") (working well
         * anyway, just displaying wrong keyword)
         */
        if (this.getParams().length() == 0) {
            try {
                commandLock.readLock().lock();
                if (this.getCommand().isEmpty()) {
                    try {
                        returnedProposalsLock.writeLock().lock();
                        this.returnedProposals.clear();
                    } finally {
                        returnedProposalsLock.writeLock().unlock();
                    }

                    setChanged();
                    notifyObservers(EVENTTYPE.PROPOSALSCLEARED);
                }
            } finally {
                commandLock.readLock().unlock();
            }
        }

        /*
         * Go down from command to comman to comma to comm, ... to find the best
         * matching commands TODO: Implement Caching?
         */
        String key;
        try {
            commandLock.readLock().lock();
            for (int i = this.getCommand().length(); (i > 0) && (necessaryNewRequests.size() < 5); i--) {
                key = this.getCommand().substring(0, i);
                if (keywords.containsKey(key)) {

                    for (AwesomeProposalRequestListener listener : keywords.get(key).getListeners(AwesomeProposalRequestListener.class)) {
                        if (!necessaryNewRequests.contains(listener)) {
                            necessaryNewRequests.add(listener);
                        }
                    }

                }
            }
        } finally {
            commandLock.readLock().unlock();
        }

        /*
         * If, and only if the params didn't change, remove returnedProposals
         * and the activeThreads out of the necessaryNewThreads. If params
         * changed, new proposals are required in order to guarantee correct
         * ranking
         */
        if (!this.isParamsChanged()) {

            /* Calculate score for existing listeners */
            try {
                returnedProposalsLock.writeLock().lock();
                Collections.sort(this.returnedProposals);
            } finally {
                returnedProposalsLock.writeLock().unlock();
            }

            /* Remove returnedProposals */
            try {
                returnedProposalsLock.readLock().lock();
                for (AwesomeProposal returnedProposal : returnedProposals) {
                    necessaryNewRequests.remove(returnedProposal.getSource());
                }
            } finally {
                returnedProposalsLock.readLock().unlock();
            }

            /* Remove activeThreads */
            if (this.proposalRequestThreads.activeCount() > 0) {
                AwesomeProposalRequestThread[] activeProposalRequestThreadList = new AwesomeProposalRequestThread[this.proposalRequestThreads.activeCount()];
                this.proposalRequestThreads.enumerate(activeProposalRequestThreadList, false);
                for (AwesomeProposalRequestThread activeProposalRequestThread : activeProposalRequestThreadList) {
                    necessaryNewRequests.remove(activeProposalRequestThread.getListener());
                }
            }

            if (necessaryNewRequests.size() == 0) {
                setChanged();
                notifyObservers(EVENTTYPE.PROPOSALSUPDATE);
            }

        } else {
            /*
             * The old active threads are not need anymore, this thread tries to
             * interrupt them first. If this fails, they get killed
             */
            if (proposalRequestThreads.activeCount() > 0) {
                Thread[] threadsToKill = new Thread[this.proposalRequestThreads.activeCount()];
                proposalRequestThreads.enumerate(threadsToKill);

                new AwesomeProposalRequestKiller(threadsToKill).start();
            }
            try {
                returnedProposalsLock.writeLock().lock();
                this.returnedProposals.clear();
            } finally {
                returnedProposalsLock.writeLock().unlock();
            }

            setChanged();
            notifyObservers(EVENTTYPE.PROPOSALSCLEARED);
        }

        /*
         * All unnecessary Threads have been removed out of the
         * necessaryNewThreads now. Let's start them now.
         */
        AwesomeProposalRequest event = new AwesomeProposalRequest(this, this.getCommand(), this.getParams());

        for (AwesomeProposalRequestListener newRequest : necessaryNewRequests) {
            new AwesomeProposalRequestThread(proposalRequestThreads, newRequest, event);
        }

    }

    /**
     * Adds a proposal to the list of the returned proposals as long as it is
     * not outdated (= not interrupted).
     * 
     * @param proposal
     *            the proposal to add
     * @see {@link #getProposals()}<br>
     *      Please take care of locking with {@link #returnedProposalsLock}
     */
    public void addProposal(AwesomeProposal proposal) {
        /*
         * Don't let interrupted threads enter data, they're already outdated!
         */
        if (Thread.currentThread().isInterrupted()) { return; }

        /*
         * Insertionsort for the new proposal - this is quicker than adding and
         * sorting
         */
        try {
            returnedProposalsLock.writeLock().lock();

            ListIterator<AwesomeProposal> iterator = this.returnedProposals.listIterator(this.returnedProposals.size());
            try {
                commandLock.readLock().lock();
                while (iterator.hasPrevious()) {
                    if (proposal.compareTo(iterator.previous()) <= 0) break;
                }
            } finally {
                commandLock.readLock().unlock();
            }
            iterator.add(proposal);
        } finally {
            returnedProposalsLock.writeLock().unlock();
        }
        setChanged();
        notifyObservers(EVENTTYPE.PROPOSALADDED);
    }

    /**
     * Please take care and use {@link #returnedProposalsLock} for locking!
     * 
     * @return list of returned proposal
     */
    public List<AwesomeProposal> getProposals() {
        return returnedProposals;
    }
}