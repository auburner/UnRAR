
/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors a specified directory for new files. If a newly added file is a rar
 * file, the file can be extracted to the appropriate folder.
 * 
 * This has transformed to be a service that starts with Windows boot, instead
 * of being executed by UTorrent.
 */
public class Watcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("Watcher.class");

	private static String zipper;
	private static Path torrentPath;
	private static Path torrentName;
	private static Path dir;
	private static String destination;
	private static Properties props;
	private static Watcher instance;

	public static Watcher getInstance() throws Exception {
		if (instance == null) {
			instance = new Watcher();
		}
		return instance;
	}

	private static Path getTorrentPath() {
		return torrentPath;
	}

	private void setTorrentPath(Path torrentPath) {
		Watcher.torrentPath = torrentPath;
	}

	private static Path getTorrentName() {
		return torrentName;
	}

	private void setTorrentName(Path torrentName) {
		Watcher.torrentName = torrentName;
	}

	private static Path getDir() {
		return dir;
	}

	private void setDir(Path dir) {
		Watcher.dir = dir;
	}

	private static String getDestination() {
		return destination;
	}

	private void setDestination(String destination) {
		Watcher.destination = destination;
	}

	private static String getZipper() {
		return zipper;
	}

	private void setZipper(String zipper) {
		Watcher.zipper = zipper;
	}

	/**
	 * Process all events for the key queued to the watcher.
	 */
	private void processEvents() {
		try {
			Files.walk(getTorrentPath()).filter(p -> p.toString().endsWith(".rar"))
					.forEach(p -> sendWindowsCommand(p.toString()));
			// .map(p ->
			// p.getParent().getParent()).distinct().forEach(System.out::println);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send command to Windows.
	private void sendWindowsCommand(String filename) {
		if (filename.toString().endsWith("rar")) {
			boolean exit = false;
			pause();
			LOGGER.info("Extracting " + filename);
			try {
				String quote = "\"";
				String command = quote + getZipper() + quote + " x " + quote + filename.toString() + quote + " -y -o"
						+ quote + getDestination() + quote;
				LOGGER.info("Executing: " + command);
				Process process = Runtime.getRuntime().exec(command);
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String s;
				while ((s = reader.readLine()) != null) {
					if (!s.equals("")) {
						LOGGER.info(s);
						exit = Boolean.valueOf(s.contains("Compressed"));
					}
				}
				if (exit) {
					LOGGER.info("End of output..bye!");
					System.exit(0);
				}

			} catch (IOException e) {
				String command2 = "\"C:\\Program Files\\7-Zip\\7z.exe\" x \"C:\\devTools\\extractTest\\watched\\extractTest.rar\" -y -o\"C:\\devTools\\extractTest\\unZipped\"";
				LOGGER.error(e.getMessage());
				LOGGER.error("This is what a good command looks like: ");
				LOGGER.error(command2);
			}
		}
	}

	TimerTask exitApp = new TimerTask() {
		public void run() {
			LOGGER.info("Closing Watcher - Timeout");
			System.exit(0);
		}
	};

	private static void pause() {
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			LOGGER.debug(e.getMessage());
		}
	}

	static void usage() {
		System.err.println(
				"Parameters: SevenZip(in props file) | torrentPath | torrentName | UnRAR_Destination(in props file");
		LOGGER.info(
				"Parameters: SevenZip(in props file) | torrentPath | torrentName | UnRAR_Destination(in props file");

		System.exit(-1);
	}

	public static void main(String[] args) throws IOException {
		// parse arguments
		if (args.length < 3)
			usage();
		try {
			props = PropertiesLoader.getInstance().initProperties();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		Path torrentPath = Paths.get(args[0]);
		Path torrentName = Paths.get(args[1]);
		//Deluge doesn't pass labels atm..
		String label = args[2].isEmpty() ? "unZipped" : args[2];
		String destination = props.getProperty("unrardirectory") + "\\" + label;
		Path wathcedDirectory = torrentPath;

		LOGGER.info("Starting watcher with the following settings:");
		LOGGER.info("Zipper " + props.getProperty("sevenzip"));
		LOGGER.info("Watched directory " + torrentPath);
		LOGGER.info("torrentName " + torrentName);
		LOGGER.info("Label " + label);
		LOGGER.info("Destination " + destination);

		Watcher watcher;
		try {
			watcher = Watcher.getInstance();
			watcher.setZipper(props.getProperty("sevenzip"));
			watcher.setDestination(destination);
			watcher.setTorrentPath(torrentPath);
			watcher.setTorrentName(torrentName);
			watcher.setDir(wathcedDirectory);
			watcher.processEvents();
		} catch (Exception e) {
			LOGGER.error("Something bad happend \n" + e);
		}

	}

}
