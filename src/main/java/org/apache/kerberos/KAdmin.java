/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kerberos;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class KAdmin {

	private final static Object lock = new Object();

	/**
	 * A String containing the resolved path to the kdamin executable
	 */
	private String executableKadmin = "/usr/bin/kadmin";

	public void listPrincipals(String ticketCache) throws IOException, InterruptedException {
		List<String> command = new ArrayList<String>();

		command.add(executableKadmin);
		command.add("-c");
		command.add(ticketCache);
		command.add("-q");
		command.add("\"listprincs\"");
		String[] commandList = command.toArray(new String[command.size()]);
		ProcessBuilder kadminListPB = new ProcessBuilder(commandList);
		Process kadmin = kadminListPB.start();
		kadmin.waitFor();
		// Read out dir output
		InputStream is = kadmin.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}

		int exitCode = kadmin.exitValue();
	}

	public boolean getPrincipal(String principalName, String ticketCache) throws IOException, InterruptedException {
		List<String> command = new ArrayList();

		command.add(executableKadmin);
		command.add("-c");
		command.add(ticketCache);
		command.add("-q");
		command.add(String.format("get_principal %s", principalName));
		String[] commandList = command.toArray(new String[command.size()]);
		ProcessBuilder kadminListPB = new ProcessBuilder(commandList);
		Process kadmin = kadminListPB.start();
		kadmin.waitFor();
		// Read out dir output
		InputStream is = kadmin.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		StringBuilder builder = new StringBuilder();
		while ((line = br.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String result = builder.toString();

		int exitCode = kadmin.exitValue();
		return (result != null) && result.contains(String.format("Principal: %s", principalName));
	}

	public boolean createPrincipal(String principalName, String ticketCache) throws IOException, InterruptedException {
		List<String> command = new ArrayList<String>();

		command.add(executableKadmin);
		command.add("-c");
		command.add(ticketCache);
		command.add("-q");
		command.add(String.format("addprinc -randkey +ok_as_delegate %s", principalName));
		String[] commandList = command.toArray(new String[command.size()]);
		ProcessBuilder kadminListPB = new ProcessBuilder(commandList);
		Process kadmin = kadminListPB.start();
		kadmin.waitFor();
		// Read out dir output
		InputStream is = kadmin.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		StringBuilder builder = new StringBuilder();
		while ((line = br.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String result = builder.toString();
		int exitCode = kadmin.exitValue();
		return (result != null) && result
				.contains(String.format("Principal or policy already exists while creating \"%s\"", principalName));
	}

	public boolean createKeyTab(String principalName, String ticketCache, String tempOutputPath)
			throws IOException, InterruptedException {
		List<String> command = new ArrayList<String>();
		String host = principalName.split("/")[1].split("@")[0];
		File keytabFile = new File(tempOutputPath + "/" + host + ".keytab");
		System.out.println("KeyTabPathAndFile: "+keytabFile.getName());
		if (keytabFile.isFile()) {
			System.out.println("KeyTab Already Generated");
			return true;
		}
		command.add(executableKadmin);
		command.add("-c");
		command.add(ticketCache);
		command.add("-q");
		command.add(String.format("ktadd -k " + tempOutputPath + "/" + host + ".keytab %s", principalName));
		String[] commandList = command.toArray(new String[command.size()]);
		ProcessBuilder kadminListPB = new ProcessBuilder(commandList);
		Process kadmin = kadminListPB.start();
		kadmin.waitFor();
		// Read out dir output
		InputStream is = kadmin.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		StringBuilder builder = new StringBuilder();
		while ((line = br.readLine()) != null) {
			System.out.println(line);
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String result = builder.toString();

		int exitCode = kadmin.exitValue();
		return (result != null) && result
				.contains("added to keytab");
	}

	public static void main(String[] args) {
		KAdmin kadmin = new KAdmin();
		String principalIn = args[0];
		String kadminPrincipal = "kadminremote/admin@SENIA.ORG";
		String kadminKeytab = "/root/kadmin.keytab";
		String ticketCache = "/root/kadmin_cache";
		String keytabOutputPath = "/root/";
		Kinit kinit = new Kinit(kadminPrincipal, ticketCache, kadminKeytab);
		Thread kinitThread = new Thread(kinit);
		kinitThread.setDaemon(true); // important, otherwise JVM does not exit
										// at end of main()
		kinitThread.start();
		synchronized (lock) {
			try {
				lock.wait(1000L);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				kadmin.listPrincipals(ticketCache);
				System.out.println(kadmin.getPrincipal(principalIn, ticketCache));
				System.out.println(kadmin.createPrincipal(principalIn, ticketCache));
				System.out.println(kadmin.createKeyTab(principalIn, ticketCache, keytabOutputPath));

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
