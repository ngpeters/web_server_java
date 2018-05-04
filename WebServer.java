/**
 * A web server listening on port 8080. 
 *
 * Natalie Peters
 *
 */

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class WebServer
{
	public static final int DEFAULT_PORT = 8080;

    // construct a thread pool for concurrency	
	private static final Executor exec = Executors.newCachedThreadPool();

	public static Configuration configurator;

	public static String location;
	
	public static void main(String[] args) throws IOException {
		ServerSocket sock = null;
		
		try {
			location = args[0];
			configurator = new Configuration(location);
			// establish the socket
			sock = new ServerSocket(DEFAULT_PORT);
			
			while (true) {
				/**
				 * now listen for connections
				 * and service the connection in a separate thread.
				 */

				//pass configurations to connection task
				Runnable task = new Connection(sock.accept(), configurator);
				exec.execute(task);
			}
		}
		catch (IOException ioe) { }
		catch (ConfigurationException ce) {
			System.out.println(ce);
			System.exit(0);
		}
		finally {
			if (sock != null)
				sock.close();
		}
	}
}