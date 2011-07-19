package org.jdownloader.extensions.omnibox.awesome;

public class AwesomeProposalRequestKiller extends Thread {

	private final Thread[] threadstokill;

	AwesomeProposalRequestKiller(Thread[] threadstokill) {
		this.threadstokill = threadstokill;
		this.setDaemon(true);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		for (Thread killThread : this.threadstokill) {
			if(killThread.isAlive())
			{
				killThread.interrupt();
			}
		}
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (Thread killThread : this.threadstokill) {
			if (killThread.isAlive()) {
				//Interrupting the thread failed, so let's kill him the bad way
				killThread.stop();
			}
		}
	}
}