
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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors a specified directory for new files. If a newly added file
 * is a plain text file, the file can be emailed to the appropriate alias. The
 * emailing details are left to the reader. What the example actually does is
 * print the details to standard out.
 */

public class watcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("watcher.class");

	private final WatchService watcher;
	private final Path zipper;
	private Path torrentPath;
	private Path torrentName;
	private String destination;

	/**
	 * Creates a WatchService and registers the given directory
	 * 
	 * @param destination
	 * @param logfile
	 * @param torrentName
	 * @param torrentPath
	 */
	watcher(Path dir, Path zipper, Path torrentPath, Path torrentName, String destination)
			throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		dir.register(watcher, ENTRY_CREATE);
		this.zipper = zipper;
		this.torrentPath = torrentPath;
		this.torrentName = torrentName;
		this.destination = destination;
	}

	/**
     * Process all events for the key queued to the watcher.
     */
    @SuppressWarnings("unchecked")
	void processEvents() {
        for (;;) {

            // wait for key to be signaled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                //The filename is the context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path filename = ev.context();

                //Send command to Windows.
                System.out.format("Extracting %s%n", filename);
                try {
                	String quote = "\"";
                	String command = quote + zipper + quote + " x " + quote + torrentPath + "\\" + torrentName + ".zip" + quote + " -y -o" + quote + destination + quote;
                    Process process = Runtime.getRuntime().exec(command);
                    BufferedReader reader=new BufferedReader( new InputStreamReader(process.getInputStream()));
                    String s; 
                    while ((s = reader.readLine()) != null){
                    	if (!s.equals("")) {
                    		LOGGER.info(s);
                    	}
                    }                   
                	
				} catch (IOException e) {
					String command2 = "\"C:\\Program Files\\7-Zip\\7z.exe\" x \"C:\\devTools\\extractTest\\watched\\extractTest.zip\" -y -o\"C:\\devTools\\extractTest\\unZipped\"";
					LOGGER.error(e.getMessage());
					LOGGER.error("This is what a good command looks like: ");
					LOGGER.error(command2);
				}
            }

            //Reset the key -- this step is critical if you want to receive
            //further watch events. If the key is no longer valid, the directory
            //is inaccessible so exit the loop.
            boolean valid = key.reset();
            if (!valid) {
                    break;
            }
        }
        //Go home, your work is done
        System.exit(0);
    }

	static void usage() {
		System.err.println("zipper | torrentPath | torrentName | destination");
		System.exit(-1);
	}

	public static void main(String[] args) throws IOException {
		// parse arguments
		if (args.length < 4)
			usage();

		// register directory and process its events
		Path zipper = Paths.get(args[0]);
		Path torrentPath = Paths.get(args[1]);
		Path torrentName = Paths.get(args[2]);
		String destination = args[3];

		Path wathcedDirectory = torrentPath;

		new watcher(wathcedDirectory, zipper, torrentPath, torrentName, destination).processEvents();
	}

}
