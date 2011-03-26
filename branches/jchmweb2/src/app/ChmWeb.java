/* ChmEnumerator.java 06/05/25
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 * Modified by Arthur Khusnutdinov, March 2011.
 *
 */

package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;

import org.jchmlib.ChmEnumerator;
import org.jchmlib.ChmFile;
import org.jchmlib.ChmTopicsTree;
import org.jchmlib.ChmUnitInfo;
import org.jchmlib.util.UDecoder;

/**
 * A simple web server.
 * You can use it to view CHM files.
 */
public class ChmWeb extends Thread
{
    
    ServerSocket listen_socket;
    ChmFile chmFile;
    String httpRootDir;
    
    public ChmWeb(String port, String chmFileName)
    {
        try{
            //set instance variables from constructor args
            int servPort = Integer.parseInt(port);
            chmFile = new ChmFile(chmFileName);
            
            //create new ServerSocket
            listen_socket = new ServerSocket (servPort);
            if (listen_socket == null) {
                System.out.println("Could not bind to port:" + servPort);
                return;
            }
            System.out.println("Server started. Now open your browser " +
                "and type\n\t http://localhost:" + servPort);
        }
        catch(IOException e) {System.err.println(e);}
        
        //Start running Server thread
        this.start();
    }
    public void run()
    {
        try{
            while(true)
            {
                //listen for a request. When a request comes in,
                //accept it, then create a Connection object to
                //service the request and go back to listening on
                //the port.
                
                Socket client_socket = listen_socket.accept();
                // System.out.println("connection request received");
                new Connection (client_socket, chmFile);
            }
        }
        catch(IOException e) {System.err.println(e);}
    }
    
    //simple "main" procedure -- create a TeenyWeb object from cmd-line args
    public static void main(String[] argv)
    {
        if (argv.length < 2)
        {
            System.out.println("usage: java ChmWeb <port> <chm filename>");
            return;
        }
        new ChmWeb(argv[0], argv[1]);
    }
}

class MimeMapping
{
    final String ext;
    final String ctype;
    public MimeMapping(String ext, String ctype) {
        this.ext = ext;
        this.ctype = ctype;
    }
}

//The Connection class -- this is where HTTP requests are serviced

class Connection extends Thread
{
    protected Socket client;
    protected BufferedReader in;
    protected PrintStream out;
    String requestedFile;
    String queryString;
    ChmFile chmFile;
    int n; // used in printTopicsTree
    MimeMapping[] mimeTypes =
    { new MimeMapping(".htm",  "text/html"    ),
            new MimeMapping(".html", "text/html"    ),
            new MimeMapping(".css",  "text/css"     ),
            new MimeMapping(".gif",  "image/gif"    ),
            new MimeMapping(".jpg",  "image/jpeg"   ),
            new MimeMapping(".jpeg", "image/jpeg"   ),
            new MimeMapping(".jpe",  "image/jpeg"   ),
            new MimeMapping(".bmp",  "image/bitmap" ),
            new MimeMapping(".png",  "image/png"    )
    };
    
    public Connection (Socket client_socket, ChmFile file)
    {
        //set instance variables from args
        chmFile = file;
        client = client_socket;
        
        //create input and output streams for conversation with client
        
        try{
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintStream (client.getOutputStream());
        }
        catch(IOException e) 
        {
            System.err.println(e);
            try {client.close();} 
            catch (IOException e2) {};
            return;
        }
        //start this object's thread -- the rest of the action
        //takes place in the run() method, which is called by
        //the thread.
        this.start();
        
    }
    
    public void run()
    {
        String line = null;     //read buffer
        String req = null;      //first line of request
        
        try{
            //read HTTP request -- the request comes in
            //on the first line, and is of the form:
            //      GET <filename> HTTP/1.x
            line = in.readLine();
            if (line == null) return;
            int indexGET = line.indexOf("GET");
            if (indexGET < 0) return;
            int indexHTTP = line.indexOf(" HTTP/1");
            req = line.substring(4, indexHTTP);
            req = UDecoder.decode(req, false);
            
            //loop through and discard rest of request
            while (line.length() > 0)
                line = in.readLine();
            
            //parse request -- get filename
            int indexQueryString = req.indexOf("?");
            if ( indexQueryString < 0) {
                indexQueryString = req.length();
                queryString = null;
            } else {
                queryString = req.substring(indexQueryString);
            }
            requestedFile = req.substring(0, indexQueryString);
            // System.out.println(requestedFile);

            if (requestedFile.startsWith("/@")) {
                deliverSpecial();
            }
            else if (requestedFile.endsWith("/")) {// this is a directory
                deliverDir();
            }
            else { // this is a file 
                deliverFile();
            }
        }
        catch (IOException e) {System.out.println(e);}
        finally
        {
            try {client.close();}
            catch (IOException e) {};            
        }
    }
    
    String loopupMime(String ext) {        
        int len = mimeTypes.length;
        for (int i = 0; i < len; i++) {
            if (ext.equals(mimeTypes[i].ext)) {
                return mimeTypes[i].ctype;
            }
        }        
        return "application/octet-stream";
    }
    
    //send a HTTP header to the client
    //The first line is a status message from the server to the client.
    //The second line holds the mime type of the document
    void sendResponseHeader(String type)
    {
        out.println("HTTP/1.0 200 OK");
        out.println("Content-type: " +type+ "\n");
    }
    
    //write a string to the client. 
    void sendString(String str)
    {
        out.print(str);        
    }
    
    void deliverDir()
    {
        out.print("HTTP/1.1 200 OK\r\n" +
            "Connection: close\r\n" +
            /* "Content-Length: 1000000\r\n" + */
            "Content-Type: text/html\r\n\r\n" +
            
            "<html>\n<body>\n<h2><u>CHM Contents:</u></h2>\n" +
            "<body>\n<table width=\"100%\">\n" +
            "<tr>\n" +
            "    <td align=right><b>Size: &nbsp&nbsp<b><br><hr></td>\n" +
            "    <td><b>File:<b><br><hr></td>\n</tr>\n" +
        "<tt>\n");
        try {
            chmFile.enumerateDir(requestedFile, ChmFile.CHM_ENUMERATE_ALL,
                new DirChmEnumerator(out));
        } catch (IOException e) {
            out.print("<h2 color=red>Server Error!</h2>\n");
        }
        out.print("</tt>\n</table>\n</body>\n</html>\n");
    }
    
    void deliverFile() throws IOException {
        ByteBuffer buffer  = null;
        // resolve object
        ChmUnitInfo ui = chmFile.resolveObject(requestedFile);

        //check to see if file exists
        if (ui == null)
        {
            sendResponseHeader("text/html");
            sendString("404: not found: " + requestedFile);
            return;
        }

        // get the extention
        int indexDot = requestedFile.indexOf(".");
        if (indexDot == -1) indexDot = 0;
        String ext = requestedFile.substring(indexDot, requestedFile.length());

        sendResponseHeader(loopupMime(ext));
        
        /* pump the data out */
        buffer = chmFile.retrieveObject(ui);
        if (buffer == null) return;
            
        byte[] bytes = new byte[(int) ui.length];
        while ( buffer.hasRemaining()) {
            buffer.get(bytes);
            out.write(bytes, 0, (int) ui.length);
        }
        
    }

    void deliverSpecial() throws IOException
    {
        requestedFile = requestedFile.substring(2);
        
        if (requestedFile.equalsIgnoreCase("index.html")) {
            deliverIndex();
            return;
        }
        else if (requestedFile.equalsIgnoreCase("tree.html")) {
            deliverTree();
            return;
        } else if (requestedFile.equalsIgnoreCase("search.html")) {
            deliverSearch();
            return;
        }

        File f = new File("template/" + requestedFile);
        //check to see if file exists
        if (!f.canRead())
        {
            sendResponseHeader("text/plain");
            sendString("404: not found: " + f.getAbsolutePath());
            return;
        }

        // get the extention
        int indexDot = requestedFile.indexOf(".");
        if (indexDot == -1) indexDot = 0;
        String ext = requestedFile.substring(indexDot, requestedFile.length());

        sendResponseHeader(loopupMime(ext));

        RandomAccessFile rf = new RandomAccessFile("template/" +
            requestedFile, "r");
        ByteBuffer in = rf.getChannel().map(FileChannel.MapMode.READ_ONLY,
            0, rf.length());
        byte[] outbuf = new byte[(int) rf.length()];
        in.get(outbuf);
        out.write(outbuf);        
    }
    
    private void deliverIndex() {
        sendResponseHeader("text/html");
        sendString("<html><head><title>" + chmFile.title + "</title></head>\n" + 
            "<frameset cols=\"200, *\">\n" +
            "  <frame src=\"@tree.html\" name=\"treefrm\">\n" + 
            "  <frame src=\"" + chmFile.home_file + "\" name=\"basefrm\">\n" + 
            "</frameset>\n" + 
            "</html>\n");
    }

    private void deliverTree() {
        n = 1;
        sendResponseHeader("text/html");
        sendString("<html>\n" + 
                "<head>\n" + 
                "<title>TreeView</title>\n" + 
                "<link rel=\"STYLESHEET\" type=\"text/css\" href=\"@tree.css\">" +
                "<script type=\"text/javascript\" src=\"@tree.js\"></script>\n" + 
                "</head>\n" + 
                "<body>\n" +
                "<p><a href=\"@tree.html\"><b>Topics</b><a/> | \n" +
                "<a href=\"@search.html\">Search<a/></p>\n" +
                "<div class=\"directory\">\n" + 
                "<div style=\"display: block;\">\n");
        printTopicsTree(chmFile.getTopicsTree(), 0);
        sendString("</div>\n</div>\n\n");
    }
    
    private void deliverSearch() {
        sendResponseHeader("text/html");
        sendString("<html><head>\n" + 
                "</head>" +
                "<title>Search</title>\n" + 
                "<link rel=\"STYLESHEET\" type=\"text/css\" href=\"@tree.css\">" +
                "<body bgcolor=\"#e9f3fe\">\n" + 
                "<p><a href=\"@tree.html\">Topics<a/> | \n" +
                "<a href=\"@search.html\"><b>Search</b><a/></p>\n" +
                "<p style=\"font-family: Tahoma,Verdana; font-size: 8pt;\">" +
                "<b>Type in the word(s) to search for:</b></p>\n" +
                "<script type=\"text/JavaScript\" src=\"@search.js\">" +
                "</script>\n");
        sendString("<form name=\"searchform\">\n" + 
            "  <table width=\"95%\">\n" + 
            "  <tr>\n" +
            "    <td>\n" + 
            "      <input type=\"text\" name=\"searchdata\" style=\"width:100%\">" +
            "    </td>\n" + 
            "    <td nowrap width=\"50\">\n" + 
            "      <input type=\"submit\" name=\"searchbutton\" " +
            "           value=\"Search\" style=\"width:100%\">\n" + 
            "    </td>\n" +
            "  </tr>\n" + 
            "  </table>\n"); 

        if (queryString != null) {
            int index1 = queryString.indexOf("searchdata=");
            if (index1 < 0 ) return;
            index1 += "searchdata=".length();
            int index2 = queryString.indexOf("&");
            if (index2 < 0) index2 = queryString.length();
            if (index1  > index2) return;
            String key = queryString.substring(index1, index2);
            // System.out.println(key);
            try {
                HashMap<String, String> results 
                    = chmFile.indexSearch(key, true, false);
                if (results == null) {
                    // System.out.println("no match found for " + key);
                }
                else {
                    Iterator<String> it = results.keySet().iterator();
                    while (it.hasNext()) {
                        String url = it.next();
                        String topic = results.get(url);
                        sendString("<p><a class=\"el\" href=\"" + url +
                                "\" target=\"basefrm\">" +
                                topic + "</a></p>");
                        // System.out.println(topic + ":\t\t " + url);
                    }
                }
            } catch (IOException e) {
            }
        }
        sendString("</form></body></html>");
    }
    
    void printTopicsTree(ChmTopicsTree tree, int level) {
        sendString("<p>");
        for (int i=0; i<level-1; i++) {
            sendString("<img src=\"@ftv2blank.png\" alt=\"&nbsp;\" width=16 height=22 />");
        }
        if (level == 0) {
            Iterator<ChmTopicsTree> it = tree.children.iterator();
            while (it.hasNext()) {
                ChmTopicsTree child = (ChmTopicsTree) it.next();
                printTopicsTree(child, level+1);
            }
            sendString("</p>\n");
        }
        else if (!tree.children.isEmpty()){
            sendString("<img src=\"@ftv2folderclosed.png\" " +
                    "alt=\""+ tree.title+"\" width=24 height=22 " +
                    "onclick=\"toggleFolder(\'folder"+n+"\', this)\"/>" +
                    "<a class=\"el\" href=\"" + tree.path + "\" target=\"basefrm\">" +
                    tree.title +"</a></p>\n" + 
                    "<div id=\"folder"+n+"\">\n");
            n++;
            Iterator<ChmTopicsTree> it = tree.children.iterator();
            while (it.hasNext()) {
                ChmTopicsTree child = (ChmTopicsTree) it.next();
                printTopicsTree(child, level+1);
            }
            sendString("</div>\n");
        } else {
            sendString("<img src=\"@ftv2doc.png\" alt=\""+tree.title+"\" " +
                    "   width=24 height=22 />" +
                "<a class=\"el\" href=\"" + tree.path + "\" target=\"basefrm\">" +
                tree.title +"</a></p>\n"); 
        }
    }

}

class DirChmEnumerator implements ChmEnumerator {
    PrintStream out;
    public DirChmEnumerator(PrintStream out) {
        this.out = out;
    }
    public void enumerate(ChmUnitInfo ui) {
        out.println("<tr>\n" +
            "    <td align=right>" + ui.length + " &nbsp&nbsp</td>\n" +
            "    <td><a href=\"" + ui.path + "\">" + ui.path + "</a></td>\n" +
        "</tr>\n");
    }
}
