
/*--------------------------------------------------------

1. Name / Date:
   Kaz Schmanski, 10/03/2018

2. Java version used, if not the official version for the class:
   Version 10.0.2 (build 10.0.2+13)

3. Precise command-line compilation examples / instructions:

> javac MyWebServer.java

4. Precise examples / instructions to run this program:

> java MyWebServer

5. List of files needed for running the program.

e.g.:

 a. MyWebServer.java

5. Notes:

----------------------------------------------------------*/

import java.io.*;  // Input/Output Libraries
import java.net.*; 

class MyJSP {
	
}


class Worker extends Thread {   
	Socket sock;			
	Worker (Socket s) {		
		sock = s;
	}
	
	//attempt to open and read the current_request file and parse the filename the client is requesting
	public void readFile(PrintStream out) throws FileNotFoundException {
		
		//out.println("hi Kaz");
		File f = new File("./current_request.txt"); 
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String current_line = "";
		
		//reads every line from current_request.txt, one at a time until we find "GET", 
		//which will help us get the name of the file the client is requesting
		
		try {
		current_line = br.readLine();
		}
		catch(IOException x) {
			x.printStackTrace();
		}
		while (!current_line.contains("GET")) {
			try {
			current_line = br.readLine();
			}
			catch(IOException x) {
				x.printStackTrace();
			}
		}
		
		// now we parse out the file name which comes after the GET and before the HTTP strings
		String file_name = current_line.substring(current_line.indexOf("GET") + 5, current_line.indexOf("HTTP"));
		
		File file_to_open = new File(file_name.trim()); 
		
		if (file_to_open.getName().contains("fake-cgi"))
			runCGI(file_to_open, out);
		
		// if the file we're trying to open is a directory, display the contents appropriately
		else if (file_to_open.isDirectory() || !file_to_open.exists()) 
			displayDirectory(file_to_open, out);
		
		// if the file we're looking to open is not a directory, we'll open it as a file
		else 	
			displayFile(file_to_open, out);	
	} 
	
	public String parseCGI(String input, int mode) {
		
		int name_start = input.lastIndexOf("person=") + 7;
		int name_end = input.indexOf("&", name_start);
		
		int num1_start = name_end + 6;
		int num1_end = input.indexOf("&", num1_start);
		
		int num2_start = num1_end + 6;
		
		if (mode == 0) 
			return input.substring(name_start, name_end);
			
		else if (mode == 1) 
			return input.substring(num1_start, num1_end);
		
		else if (mode == 2) 
			return input.substring(num2_start);
		
		else
			return "";
	}
	
	public int addnums(String num1, String num2) {
		return Integer.parseInt(num1) + Integer.parseInt(num2);
	}
	
	public void runCGI(File file_to_open, PrintStream out) throws FileNotFoundException {
		
		String input = file_to_open.getName();
		String input_name = parseCGI(input, 0);
		String num1 = parseCGI(input, 1);
		String num2 = parseCGI(input, 2);
		int sum = addnums(num1, num2);
		printCGI(input_name, num1, num2, sum, out);
		
		//System.out.println("name is " + input_name + " and num1 is " + num1 + " and num2 is " + num2);
	}
	
	public void printCGI(String name, String num1, String num2, int sum, PrintStream out) throws FileNotFoundException {
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + 10000);
		out.println("Content-Type: text/html");
		out.print("\r\n\r\n");
		out.println("Dear " + name + ", the sum of " + num1 + " and " + num2 + " is " + sum);
	}
	
	public void displayDirectory(File file_to_open, PrintStream out) throws FileNotFoundException {
		
		
		if (!file_to_open.exists())
			file_to_open = new File("./");
		
		File[] strFilesDirs = file_to_open.listFiles();
		//directory headers
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + 10000);
		out.println("Content-Type: text/html");
		out.print("\r\n\r\n");
		
		int count = 0;
		while (count < strFilesDirs.length) {
				strFilesDirs[count] = new File(strFilesDirs[count].getName());
			count++;
		}
		

		//print a link for the sub directory before this one
		try {
			String current_folder = file_to_open.getCanonicalPath().substring(file_to_open.getCanonicalPath().lastIndexOf("/") + 1);
			
			// as long as the current folder isn't our root directory, print a reference to the previous directory
			if (current_folder.compareTo("MyWebServer") != 0) 
				out.println("<a href=" + ".." + "/>" + "Parent Directory" + "</a> <br>");
			
		}
		catch(IOException x) {
			x.printStackTrace();
		}
		
		//iterates through each of the files in the directory, displaying them properly on the web page
		for (int i = 0; i < strFilesDirs.length; i++) {
			if (strFilesDirs[i].isDirectory())
				out.println("<a href=" + strFilesDirs[i] + "/>" + strFilesDirs[i] + "/</a> <br>");
			else if (strFilesDirs[i].isFile())
				out.println("<a href=" + strFilesDirs[i].getName() + ">" + strFilesDirs[i].getName() + " ("  + strFilesDirs[i].length() + ") " + "</a> <br>");
			else
				out.println("<a href=" + strFilesDirs[i] + "/>" + strFilesDirs[i] + "/</a> <br>");
		} 
	}
	
	public void displayFile(File file_to_open, PrintStream out) throws FileNotFoundException {
		FileReader my_fr = new FileReader(file_to_open);
		BufferedReader my_br = new BufferedReader(my_fr);
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + file_to_open.length() + 1000);
		
		String extension = "";

		int i = file_to_open.getName().lastIndexOf('.');
		if (i > 0) {
		    extension = file_to_open.getName().substring(i+1);
		}

		//if the file we're requesting is HTML, encode it properly to display on the webpage
		if (extension.compareTo("html") == 0)
			out.println("Content-Type: text/html");
		else
			out.println("Content-Type: text/plain");
			
		out.print("\r\n\r\n");

		String toRead = "";
		
		//read out the file to the web page until the end of the file
		try {
		while ((toRead = my_br.readLine()) != null) 
				out.println(toRead);
		}
		
		catch(IOException x) {
			x.printStackTrace();
			}
		}
	
	
	public void run() {
		PrintStream out = null;  // set output stream to null
		BufferedReader in = null;  // set input stream to null
		try {
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));  // reads input from the socket
			out = new PrintStream(sock.getOutputStream());  // sets up a new output stream

			String sockdata;
			FileWriter writer = new FileWriter("current_request.txt");
			PrintWriter fileout = new PrintWriter(writer, true);
			 
			 sockdata = in.readLine();
			 //reads in the entirety of our request into current_request.txt
			 while (!sockdata.isEmpty()) {
				 System.out.println(sockdata); //prints to the server console what the request looks like
				 System.out.flush();
				// out.println(sockdata);  //prints to the client what the request looks like
				 fileout.print(sockdata + "\n");
				 sockdata = in.readLine();
			 }
			 
			 fileout.close();
			 //now we need to determine what our file name is going to be, so we call readFile
			 readFile(out);
		      
			}
			
			catch (IOException x) { // error handling
				System.out.println("Server read error");
				x.printStackTrace();
			}
		try {
			sock.close();
		}
		catch (IOException x) {
			System.out.println("Socket error");
			x.printStackTrace();
			} 
		} 
		
	// Conversion function not the focus of the assignment
	static String toText (byte ip[]) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < ip.length; ++i) {
			if (i > 0)
				result.append(".");
			result.append(0xff & ip[i]); 
		}
		return result.toString();
	}
}


public class WebServer {
  public static boolean control = true;

	public static void main (String args[]) throws IOException {
		
		int q_len = 6; // maximum queue length of connections coming in
		int port = 2540;
		
		Socket sock;	
		ServerSocket servsock = new ServerSocket(port, q_len);  // creates a new ServerSocket called "servsock"
		
		System.out.println("Kaz Schmanski's WebServer starting up, listening at port " + port);
		while (control) {
			sock = servsock.accept(); // listens for a connection and accepts it
			new Worker(sock).start(); // calls a new worker to start
			
		}
		
	}
	
}

