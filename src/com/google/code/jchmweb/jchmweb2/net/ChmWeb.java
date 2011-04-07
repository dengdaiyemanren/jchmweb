/* ChmWeb.java 2006/05/25
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */

package com.google.code.jchmweb.jchmweb2.net;

import com.google.code.jchmweb.jchmweb2.Configuration.ParamsClass;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.code.jchmweb.jchmweb2.ChmFile;

/**
 * ChmWeb is a simple web server which can be used to view CHM files.
 * <p>
 * 
 * Here is a bash script called <b>jchmweb</b>:
 * 
 * <pre>
 *    #! /bin/sh
 *    export JCHMLIB=/home/chimen/jchmlib
 *    export CLASSPATH=$CLASSPATH:$JCHMLIB/bin/jchmlib.jar
 *    java -Djchmweb.template=$JCHMLIB/bin org.jchmlib.net.ChmWeb &quot;$@&quot; 
 * </pre>
 * 
 * You may have to modify the value of <b>JCHMLIB</b> to the actual path of
 * <i>jchmlib</i>.
 * <p>
 * 
 * Put the script under a directory within $PATH, and use the following command
 * to start the server:
 * 
 * <pre>
 *    jchmweb &lt;PORT&gt; &lt;CHMFILE&gt;
 * </pre>
 * 
 * For example:
 * 
 * <pre>
 *    jchmweb 8080 /home/chimen/java.chm
 * </pre>
 * 
 * Then, open your favorite web browser and point it to
 * <pre>http://localhost:&lt;PORT&gt;</pre> or
 * <pre>http://localhost:&lt;PORT&gt;/@index.html</pre>
 * 
 */
public class ChmWeb extends Thread {
    ServerSocket listen_socket;

    ChmFile      chmFile;

    String       httpRootDir;

    public ChmWeb(String port, String chmFileName) {
        int servPort;
        try {
            servPort = Integer.parseInt(port);
            listen_socket = new ServerSocket(servPort);
            if (listen_socket == null) {
                System.err.println("Could not bind to port:"
                        + servPort);
                return;
            }

            chmFile = new ChmFile(chmFileName);
        } catch (IOException e) {
            System.err.println("Error encountered while starting"
                    + " server:\n" + e);
            return;
        }

    }

    @Override
    public void run() {
        ParamsClass.logger.info("Server started. Now open your browser "
                + "and point it to\n\t http://"  
                + listen_socket.getInetAddress().getHostName()
                + ":" + listen_socket.getLocalPort());

        try {
            while (true) {
                // listen for a request. When a request comes in,
                // accept it, then create a ClientHandler object to
                // service the request and go back to listening on
                // the port.

                Socket client_socket = listen_socket.accept();
                new ClientHandler(client_socket, chmFile).start();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void main(String[] argv) {
        if (argv.length < 2) {
            ParamsClass.logger.fatal("usage: "
                    + "java -Djchmweb.template=$JCHMLIB/bin "
                    + "org.jchmlib.net.ChmWeb <port> <chmfile>");
            return;
        }
        ParamsClass PC=new ParamsClass();
        // Start running Server thread
        new ChmWeb(argv[0], argv[1]).start();
    }
}

