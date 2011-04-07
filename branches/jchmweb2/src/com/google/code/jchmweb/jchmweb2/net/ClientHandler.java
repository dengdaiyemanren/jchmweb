/* ClientHandler.java 2006/08/22
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */

package com.google.code.jchmweb.jchmweb2.net;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.code.jchmweb.jchmweb2.ChmEnumerator;
import com.google.code.jchmweb.jchmweb2.ChmFile;
import com.google.code.jchmweb.jchmweb2.ChmTopicsTree;
import com.google.code.jchmweb.jchmweb2.ChmUnitInfo;
import com.google.code.jchmweb.jchmweb2.search.SearchEnumerator;

/**
 * The ClientHandler class -- this is where HTTP requests are serviced
 */
public class ClientHandler extends Thread {

    protected Socket       client;

    protected HttpRequest  request;

    protected HttpResponse response;

    protected String       requestedFile;

    protected String       templatePath;

    protected ChmFile      chmFile;

    /* used in printTopicsTree */
    private int            n;

    public ClientHandler(Socket client_socket, ChmFile file) {
        chmFile = file;
        client = client_socket;

        try {
            request = new HttpRequest(client.getInputStream(),
                    chmFile.codec);
            response = new HttpResponse(client.getOutputStream(),
                    chmFile.codec);
        }
        catch (IOException e) {
            System.err.println(e);
            try {
                client.close();
            }
            catch (IOException e2) {
                System.err.println(e);
            }
            return;
        }

        requestedFile = request.getPath();

        templatePath = System.getProperty("jchmweb.template");
        if (templatePath == null) templatePath = ".";

    }

    public void run() {

        try {
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
        catch (IOException e) {
            System.err.println(e);
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
            }
            ;
        }
    }

    void deliverDir() {
        response.sendHeader("text/html");
        response.sendString("<html>\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; " +
                " charset=" + chmFile.codec + "\">\n" + 
                "<title>" + chmFile.title + "</title>" +
                "</head>" +
                "<body>\n" +
                "<table border=0 cellspacing=0 cellpadding=0 width=100%>\n" +
                "<tr><td align=right nowrap>" +
                "<a href=\"" + chmFile.home_file + "\">Main Page</a>&nbsp;\n" +
                "<a href=\"/@index.html\">Frame View</a> &nbsp;" +
                "</td></tr>\n" +
                "<tr><td align=left>" +
                "<h2><u>CHM Contents:</u></h2>" +
                "</td></tr>\n" +
                "<tr>\n" +
                "<table width=\"100%\">\n" +
                "<tr>\n" +
                "  <td align=right><b>Size: &nbsp&nbsp<b>\n" +
                "   <br><hr>\n" +
                "  </td>\n" +
                "  <td><b>File:<b><br><hr></td>\n" +
                "</tr>\n" +
                "<tt>\n");

        try {
            chmFile.enumerateDir(requestedFile,
                    ChmFile.CHM_ENUMERATE_USER,
                    new DirChmEnumerator(response.getWriter()));
        }
        catch (IOException e) {
            response.sendString("<h2 color=red>Server Error!</h2>\n");
        }

        response.sendString("</tt>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n");
    }

    protected void deliverFile() throws IOException {
        ByteBuffer buffer = null;
        
        // resolve object
        ChmUnitInfo ui = chmFile.resolveObject(requestedFile);

        // check to see if file exists
        if (ui == null) {
            response.sendHeader("text/html");
            response.sendString("404: not found: " + requestedFile);
            return;
        }

        response.sendHeader(request.getContentType());

        /* pump the data out */
        buffer = chmFile.retrieveObject(ui);

        response.write(buffer, (int) ui.length);
    }

    protected void deliverSpecial() throws IOException {
        requestedFile = requestedFile.substring(2);

        if (requestedFile.equalsIgnoreCase("index.html")) {
            deliverMain();
            return;
        }
        else if (requestedFile.equalsIgnoreCase("tree.html")) {
            deliverTree();
            return;
        }
        else if (requestedFile.equalsIgnoreCase("search.html")) {
            deliverSearch();
            return;
        }
        else if (requestedFile.equalsIgnoreCase("search2.html")) {
            deliverSearch2();
            return;
        }

        File f = new File(templatePath + "/template/" + requestedFile);
        // check to see if file exists
        if (!f.canRead()) {
            response.sendHeader("text/plain");
            response.sendString("404: not found: "
                    + f.getAbsolutePath());
            return;
        }

        response.sendHeader(request.getContentType());

        RandomAccessFile rf = new RandomAccessFile(templatePath
                + "/template/" + requestedFile, "r");
        ByteBuffer in = rf.getChannel()
                .map(FileChannel.MapMode.READ_ONLY, 0, rf.length());
        response.write(in, (int) rf.length());
    }

    private void deliverMain() {
        response.sendHeader("text/html");
        response.sendLine("<html>\n" 
                + "<head>\n"
                + "<meta http-equiv=\"Content-Type\" "
                + " content=\"text/html; charset=" + chmFile.codec
                + "\">\n" 
                + "<title>" + chmFile.title + "</title>\n"
                + "</head>\n"
                + "<frameset cols=\"200, *\">\n"
                + "  <frame src=\"@tree.html\" name=\"treefrm\">\n"
                + "  <frame src=\"" + chmFile.home_file + "\" name=\"basefrm\">\n" 
                + "</frameset>\n"
                + "</html>");
    }

    private void deliverTree() {
        int expandLevel = 2;
        String query = request.getParameter("expand");
        if (query != null) {
            expandLevel = Integer.parseInt(query);
        }

        n = 1;
        delieverMenu(1);
        response.sendString("<div class=\"directory\">\n"
                + "<div style=\"display: block;\">\n");
        printTopicsTree(chmFile.getTopicsTree(), 0, expandLevel);
        response.sendString("</div>\n</div>\n</div>\n\n");
        chmFile.releaseTopicsTree();
    }

    private void deliverSearch() {
        delieverMenu(2);
        response.sendString("<p>Type in the word(s) to search for:</p>\n");

        String query = request.getParameter("searchdata");
        if (query == null) {
            deliverSearchForm();
            response.sendLine("</div></div></body></html>");
            return;
        }
        
        deliverSearchForm(query);
        
        try {
            HashMap<String, String> results = chmFile
                    .indexSearch(query, true, false);
            if (results == null) {
                response.sendString("<p>No match found for " + query
                        + ".</p>");
                System.err.println("No match found for " + query);
            }
            else {
                Iterator<Entry<String, String>> it = results.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, String> entry = it.next(); 
                    String url = entry.getKey(); 
                    String topic = entry.getValue();
                    response.sendString("<p>"
                            + "<a class=\"el\" href=\"" + url + "\""
                            + "   target=\"basefrm\">" + topic
                            + "</a>" + "</p>");
                    url = null;
                    topic = null;
                }
            }
        }
        catch (IOException e) {
        }
        response.sendString("</div></div></body></html>");
    }

    private void deliverSearch2() {
        delieverMenu(3);
        response.sendString("<p>Type in the word(s) to search for:</p>\n");

        String query = request.getParameter("searchdata");
        if (query == null) {
            deliverSearchForm();
            response.sendLine("</div></body></html>");
            return;
        }

        deliverSearchForm(query);

        try {
            Collection<String> results = new ArrayList<String>();
            chmFile.enumerate(ChmFile.CHM_ENUMERATE_USER,
                    new SearchEnumerator(chmFile, query, results));
            int i = 0;
            Iterator<String> it = results.iterator();
            if (!it.hasNext()) {
                response.sendLine("<p>No match found for " + query
                        + ".</p>");
                return;
            }

            while (it.hasNext()) {
                i++;
                String url = it.next();
                String title = chmFile.getTitle(url);
                response.sendLine("<p><a class=\"el\" " + " "
                        + " href=\"" + url + "\" "
                        + " target=\"basefrm\">[" + i + "]" + title
                        + "</a></p>");
                url = null;
                title = null;
            }
            results = null;
        }
        catch (Exception e) {
            System.err.println(e);
        }
        response.sendLine("</div></div></body></html>");
    }
    
    private void delieverMenu(int selected) {
        response.sendHeader("text/html");
        response.sendString("<html>\n"
                + "<head>\n"
                + "<meta http-equiv=\"Content-Type\" "
                + " content=\"text/html; charset=" + chmFile.codec + "\">\n"
                + "<title>Search</title>\n"
                + "<link rel=\"STYLESHEET\" type=\"text/css\" href=\"@tree.css\"/>\n"
                + "<script type=\"text/javascript\" src=\"@search.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\"@tree.js\"></script>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div class=\"menu\">\n"
                + "<p>\n"
                + "<a href=\"@tree.html\">"
                + ((selected == 1) ? "<b>" : "")
                + "Topics"
                + ((selected == 1) ? "</b>" : "")
                + "<a/>|"
                + "<a href=\"@search.html\">"
                + ((selected == 2) ? "<b>" : "")
                + "Search"
                + ((selected == 2) ? "</b>" : "")
                + "<a/>|"
                + "<a href=\"@search2.html\">"
                + ((selected == 3) ? "<b>" : "")
                + "Search(slow)"
                + ((selected == 3) ? "</b>" : "")
                + "<a/>|\n"                
                + "<a href=\"/\" target=\"basefrm\">Direcroty Listing</a>\n"
                + "</p>\n"
                + "</div>\n"
		+ "<div class=\"side_content\">\n");        
    }
    
    private void deliverSearchForm() {
        response.sendLine(
                  "<form name=\"searchform\">\n"
                + "<table width=\"95%\">\n"
                + "  <tr>\n"
                + "    <td>\n"
                + "      <input type=\"text\" name=\"searchdata\" "
                + "        id=\"searchdata\" "
                + "        style=\"width:100%\">"
                + "    </td>\n"
                + "    <td nowrap width=\"50\">\n"
                + "      <input type=\"submit\" name=\"searchbutton\" "
                + "           value=\"Search\" style=\"width:100%\">\n"
                + "    </td>\n" + "  </tr>\n"
                + "</table>\n" + "</form>");
    }
    
    private void deliverSearchForm(String query) {
        if (query == null) deliverSearchForm();
        
        response.sendLine(
                  "<form name=\"searchform\">\n"
                + "<table width=\"95%\">\n"
                + "  <tr>\n"
                + "    <td>\n"
                + "      <input type=\"text\" name=\"searchdata\" "
                + "        id=\"searchdata\" "
                + "        value=\""+ HTMLEncode.encode(query) + "\""
                + "        style=\"width:100%\">"
                + "    </td>\n"
                + "    <td>\n"
                + "      <input type=\"submit\" name=\"searchbutton\" "
                + "           value=\"Search\" style=\"width:100%\">\n"
                + "    </td>\n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>\n"
                + "      <input type=button value=\"Remove Highlight\" "
                + "             onclick=\"unhighlight()\"/>\n"
                + "    </td>\n"
                + "    <td>\n"
                + "      <input type=button value=Highlight "
                + "             onclick=\"findIt()\">\n"
                + "    </td>\n"
                + "  </tr>\n"
                + "</table>\n" 
                + "</form>");        
    }
    
    private void printTopicsTree(ChmTopicsTree tree, int level, 
            int expandLevel) { 
        if (tree == null) return;
        
        response.sendLine("<p>");

        for (int i = 0; i < level - 1; i++) {
            response.sendLine("<img src=\"@ftv2blank.png\" "
                    + " title=\"&nbsp;\" width=16 height=22 />");
        } 

        if (level == 0) { // top level
            Iterator<ChmTopicsTree> it = tree.children.iterator();
            while (it.hasNext()) {
                ChmTopicsTree child = (ChmTopicsTree) it.next();
                printTopicsTree(child, level + 1, expandLevel);
                child = null;
            } 
            response.sendString("</p>\n");
        }
        else if (!tree.children.isEmpty()) {
            if (level >= expandLevel) {
                response.sendLine("<a href=\"@tree.html?expand=" 
                        + (expandLevel+1) + "\">"
                        + "<img src=@ftv2folderclosed.png "
                        + " title=\"Click to expand\" border=0></a> "
                        + "<a class=el href=\"" + tree.path + "\" "
                        + "   target=basefrm>" + tree.title + "</a>"
                        + "</p>");
                return;
            }

            response.sendLine("<img src=\"@ftv2folderclosed.png\" "
                    + " title=\"" + tree.title + "\" "
                    + " width=24 height=22 "
                    + " onclick=\"toggleFolder(\'folder" + n
                    + "\', this)\"/>" + "<a class=\"el\" href=\""
                    + tree.path + "\" " + " target=\"basefrm\">"
                    + tree.title + "" + "</a>" + "</p>\n");
            response.sendLine("<div id=\"folder" + n + "\">");

            n++; // n is used to identify folders (topics with sub-topics)

            Iterator<ChmTopicsTree> it = tree.children.iterator();
            while (it.hasNext()) {
                ChmTopicsTree child = (ChmTopicsTree) it.next();
                printTopicsTree(child, level + 1, expandLevel);
                child = null;
            }
            response.sendLine("</div>");
        }
        else { // leaf node
            response.sendLine("<img src=\"@ftv2doc.png\" "
                    + "   title=\"" + tree.title + "\" "
                    + "   width=24 height=22 />"
                    + "<a class=\"el\" href=\"" + tree.path + "\" "
                    + "   target=\"basefrm\">" + tree.title + "</a>"
                    + "</p>");
        }
    }
}

class DirChmEnumerator implements ChmEnumerator {

    PrintStream out;

    public DirChmEnumerator(PrintStream out) {
        this.out = out;
    }

    public void enumerate(ChmUnitInfo ui) {
        out.println("<tr>\n");
        out.println("\t<td align=right>" + ui.length
                + " &nbsp&nbsp</td>");
        out.println("\t<td><a href=\"" + ui.path + "\">" + ui.path
                + "</a></td>\n");
        out.println("</tr>");
    }
}

