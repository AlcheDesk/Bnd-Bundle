package com.meowlomo.ci.ems.bundle.jmeter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConsoleCaller {
	public static String jmeterCmd = "D:\\apache-jmeter-5.0\\bin\\jmeter -n -t %s -r -l D:\\apache-jmeter-5.0\\bin\\result%d.csv -e -o D:\\apache-jmeter-5.0\\bin\\tmp\\report%d";
	
	static void doJmeterOnce(String jmxFile) {
		
		// TODO Auto-generated method stub
		String jmeterDir = "D:\\apache-jmeter-5.0\\bin\\";
		File dir = new File(jmeterDir);
		
		int index = 30;
		File indexFile = new File(jmeterDir + "\\remoteindex.ini");
		if (indexFile.exists() && indexFile.canRead()) {
			BufferedReader br = null;
			do {
				try {
					br = new BufferedReader(new FileReader(indexFile));
					String line = br.readLine();
					if (null == line || line.isEmpty())
						break;
					index = Integer.valueOf(line);
					++index;
				} catch (NumberFormatException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} while (false);
		}

		try {
			OutputStream os = new FileOutputStream(indexFile);
			os.write(String.format("%d", index).getBytes());
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		doCmdCall(dir, jmxFile, index);
	}
	
	static void doCall(int index) {
		try {
			Runtime.getRuntime().exec("cmd /c " + String.format(jmeterCmd, index, index));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static void doCmdCall(File dir, String jmxFile, int index) {
		ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "D:\\apache-jmeter-5.0\\bin\\jmeter", "-n", "-t",
				jmxFile, "-r", "-l",
				String.format("D:\\apache-jmeter-5.0\\bin\\result%d.csv", index), "-e", "-o",
				String.format("D:\\apache-jmeter-5.0\\bin\\tmp\\report%d", index));

		pb.directory(dir);
		try {
			Process process = pb.start();
			BufferedReader cmdStreamReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String cmdOutput;
			while ((cmdOutput = cmdStreamReader.readLine()) != null) {
				System.out.println(cmdOutput);
			}
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
