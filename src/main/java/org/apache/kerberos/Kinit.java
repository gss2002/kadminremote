package org.apache.kerberos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Kinit implements Runnable {
	String kinitExecutable = "/usr/bin/kinit";
	String kadminTGS = "kadmin/admin";
	String kadminPrincipal;
	String ticketCache;
	String kadminKeytab;
	long kinitDelay = 28800L;

	public Kinit(String kadminPrincipal, String ticketCache, String kadminKeytab) {
		this.kadminPrincipal = kadminPrincipal;
		this.ticketCache = ticketCache;
		this.kadminKeytab = kadminKeytab;
	}

	private final Object lock = new Object();

	public void run() {
		synchronized (lock) {
			while (true) {
				try {
					List<String> command = new ArrayList<String>();

					command.add(this.kinitExecutable);
					command.add("-c");
					command.add(this.ticketCache);
					command.add("-kt");
					command.add(this.kadminKeytab);
					command.add(this.kadminPrincipal);
					command.add("-S");
					command.add(this.kadminTGS);

					String[] commandList = command.toArray(new String[command.size()]);
					ProcessBuilder kadminListPB = new ProcessBuilder(commandList);
					kadminListPB.inheritIO();
					Process kadmin = null;
					try {
						kadmin = kadminListPB.start();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					try {
						kadmin.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					int exitCode = kadmin.exitValue();
					lock.wait(kinitDelay);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
