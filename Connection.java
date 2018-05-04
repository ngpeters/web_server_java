/**
 * This is the separate thread that services each
 * Connection.java for WebServer implements logic that
 * was in Handler class.
 *
 * Natalie Peters
 */

import java.net.*;
import java.io.*;

public class Connection implements Runnable
{
	private Socket client;
	private Configuration configurator;
	public static final int BUFFER_SIZE = 256;
	
	public Connection(Socket client, Configuration configurator) {
		this.client = client;
		this.configurator = configurator;
	}

	public int findChar(char c, int pos, String s) {
        int index = pos;

        while (s.charAt(pos) != c && index < s.length())
            pos++;

        return pos;
    }

    public String parseInput(String line) {
        int firstBlank = findChar(' ', 0, line);
        int secondBlank = findChar(' ', firstBlank + 1, line);

        int firstSlash = findChar('/', 0, line);
        int secondSlash = findChar('/', firstSlash + 1, line);

        String resource;
        /* If it is a default query */
        if (secondBlank - firstBlank == 2) {
            resource = "/";
        }
        else {
            resource = line.substring(firstSlash + 1, secondBlank);
        }
        return resource;
    }

    public String resourceType(String line) {
        int len = line.length();
        int typeStart = findChar('.', 0, line);
        String type = line.substring(typeStart + 1, len);
        String resourceType;
        if (type.equals("html")){
            resourceType = "text/html";
        }
        else if(type.equals("txt")){
            resourceType = "text/plain";
        }
        else if(type.equals("jpg")){
            resourceType = "image/jpeg";
        }
        else if (type.equals("gif") || type.equals("png")) {
            resourceType = "image/" + type;
        }
        else{
            resourceType = "text/html";
        }
        return resourceType;
    }

    public void process(Socket client) throws java.io.IOException {
    	
		BufferedReader in = null;

		try {

			in = new BufferedReader(new InputStreamReader(client.getInputStream()));

			String requestLine = in.readLine();

            /* If we don't read a GET, just ignore it and close the socket */
            if ( requestLine == null || !requestLine.substring(0,3).equals("GET") ) {
                client.close();

                return;
            }

            /* Parse GET */
            String resource = parseInput(requestLine);

			DataOutputStream toClient = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));

			java.util.Date theDate = new java.util.Date();

			String existsPath = configurator.getDocumentRoot() + resource;
			String NotExistsPath = configurator.getFourOhFourDocument();

			File file = null;
			int status;
			if (resource.equals("/")){
				file = new File(configurator.getDefaultDocument());
			}
			else{
				file = new File(existsPath);
			}

			/* Construct headers */
			if (file.exists()){
				toClient.writeBytes("HTTP/1.1 200 OK");
				status = 200;
			}
			else{
				file = new File(NotExistsPath);
				toClient.writeBytes("HTTP/1.1 404 Not Found");
				status = 404;
			}
			/* write headers to client */
			toClient.writeBytes("Date: " + theDate.toString() + "\r\n");
			toClient.writeBytes("Server: " + configurator.getServerName() + "\r\n");
			toClient.writeBytes("Content-Type: "+ resourceType(file.getPath()) + "\r\n");
			toClient.writeBytes("Content-Length: "+ file.length() + "\r\n");
			toClient.writeBytes("Connection: close"+ "\r\n\r\n");

			/* write file to client */
			InputStream inFile = new BufferedInputStream(new FileInputStream(file));
			byte[] buffer = new byte[BUFFER_SIZE];
			int numBytes;
			while ( (numBytes = inFile.read(buffer)) != -1) {
                toClient.write(buffer,0,numBytes);
                toClient.flush();
            }

            /* Loging */
			File logFile = new File(configurator.getLogFile());
			OutputStream log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(logFile, true)));
			InetAddress ipAddr = client.getInetAddress();
			String logLine = ipAddr.getHostAddress()+" ["+ theDate.toString()+"] "+requestLine+" "+status+" "+file.length();
			
			//System.out.println(logLine);

			byte[] logLineBytes = logLine.getBytes();
			log.write(logLineBytes);
			log.flush();
		}

    	catch (IOException ioe) {
			System.err.println(ioe);
		}
		finally {
            in.close();
		}
    }

    /**
     * This method runs in a separate thread.
     */	
	public void run() { 
		try {
			process(client);
		}
		catch (java.io.IOException ioe) {
			System.err.println(ioe);
		}
	}
}
