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
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example monitors a specified directory for new files.
 * If a newly added file is a plain text file, the file can
 * be emailed to the appropriate alias.  The emailing details
 * are left to the reader.  What the example actually does is
 * print the details to standard out.
 */

public class watcher {
	private static final Logger LOGGER = Logger.getLogger( watcher.class.getName());

    private final WatchService watcher;
    private final Path dir;
    private final Path zipper;
    private String torrentPath;
    private String torrentName;
    private String logfile;
    private String destination;

    /**
     * Creates a WatchService and registers the given directory
     * @param destination 
     * @param logfile 
     * @param torrentName 
     * @param torrentPath 
     */
    watcher(Path dir, Path zipper, String torrentPath, String torrentName, String logfile, String destination) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        dir.register(watcher, ENTRY_CREATE);
        this.dir = dir;
        this.zipper = zipper;
        this.torrentPath = torrentPath;
        this.torrentName = torrentName;
        this.logfile = logfile;
        this.destination = destination;
    }

    /**
     * Process all events for the key queued to the watcher.
     */
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
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                //The filename is the context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path filename = ev.context();

                //Verify that the new file is a text file.
//                try {
//                    Path child = dir.resolve(filename);
//                    String what = Files.probeContentType(child);
//                    System.err.println("what " + what);
//                    if (!Files.probeContentType(child).equals("text/plain")) {
//                        System.err.format("New file '%s' is not a plain text file.%n", filename);
//                        continue;
//                    }
//                } catch (IOException x) {
//                    System.err.println(x);
//                    continue;
//                }

                //Send command to Windows.
                System.out.format("Extracting %s%n", filename);
                try {
                	String quote = "\"";
                	String command = quote + zipper + quote + " x " + quote + torrentPath + "\\" + torrentName + ".zip" + quote + " -o" + quote + destination + quote;
                	String command2 = "\"C:\\Program Files\\7-Zip\\7z.exe\" x \"C:\\devTools\\extractTest\\watched\\extractTest.zip\" -o\"C:\\devTools\\extractTest\\unZipped\"";
                	System.out.println(command.equals(command2));
                    Process process = Runtime.getRuntime().exec(command);
                    System.out.println("the output stream is "+process.getOutputStream());
                    BufferedReader reader=new BufferedReader( new InputStreamReader(process.getInputStream()));
                    String s; 
                    while ((s = reader.readLine()) != null){
                    	if (!s.equals("")) {
                    		System.out.println(s);
                    		LOGGER.log( Level.FINE, s);
                    	}
                    }                   
                	
				} catch (IOException e) {
					e.printStackTrace();
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
    }

    static void usage() {
        System.err.println("directory | zipper | torrentPath | torrentName | logfile | destination");
        System.exit(-1);
    }

    public static void main(String[] args) throws IOException {
        //parse arguments
        if (args.length < 5)
            usage();

        //register directory and process its events
        Path wathcedDirectory = Paths.get(args[0]);
        Path zipper = Paths.get(args[1]);
        String torrentPath = args[2];
        String torrentName = args[3];
        String logfile = args[4];
        String destination = args[5];
        
        new watcher(wathcedDirectory, zipper, torrentPath, torrentName, logfile, destination).processEvents();
    }
}