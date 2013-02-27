import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

/**
 *  This class is designed to implement common network requesting
 *  behavior. 
 */

/**
 * @author ruben
 *
 */
public class NetworkOperator {	
	/*
	 * Protocol for receiving and sending timetable and request records
	 * 1st message: command {takeMyRequests, takeMyTimetable, giveMeRequests or giveMeTimetable}
	 * 2nd message: number of objects ready to transfer
	 * after:		objects (object's type is command-dependent)
	 * last: 		initiator says "goodbye" to finish communication
	 * 				other device confirms by "goodbye"
	 */	

	public static boolean sendRequests(HostInfo info, ArrayList<RequestRecord> _requests) 
	{
		if (info == null) {
			System.out.println("sendRequests(): Host info is null.");
			return false;
		}

		Socket clientSocket = null;
		try
		{		
			// open new socket connection
			clientSocket = new Socket(info.getIp(), info.getPort());
			//System.out.println("Connected to: " + clientSocket.getInetAddress().getHostAddress() + ". OK");
		} catch(UnknownHostException unknownHost) {
			System.err.println("Unknown host: " + info.getIp() + ":" + info.getPort());	
			return false;			
		} catch (IOException ex) {
			System.out.println("Socket timeout");
			return false;
		}

		try
		{	
			boolean sentOk = false;							
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());			

			// (1) send command			
			out.writeObject("takeMyRequests");

			// (2) send number of objects
			int number = _requests.size();
			out.writeObject(number);

			// (3)send booking/canceling records				
			for (int i = 0; i < number; ++i) {
				out.writeObject(_requests.get(i));					
			}

			// (4) say "goodbye"			
			out.writeObject("goodbye");
			out.flush();

			// (5) receive ok reply "goodbye"
			String goodbye;
			try {
				goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye")) {
					System.out.println("Requests sending finished fine. Delivered to " + info.toString());
					sentOk = true;
				}
			} catch (ClassNotFoundException e) {
				System.out.println("String cast failed in the sendRequests()");
			}

			in.close();
			out.close();
			clientSocket.close();				

			return sentOk;
		} catch(Exception e){	
			System.out.println("Exception in sendRequests()");
		}	

		return false;		
	}

	public static boolean sendTimetable(HostInfo info, ArrayList<TimeTableEntry> _timetable)
	{
		if (info == null) {
			System.out.println("sendTimetable(): Host info is null.");
			return false;
		}

		Socket clientSocket = null;
		try
		{		
			// open new socket connection
			clientSocket = new Socket(info.getIp(), info.getPort());
			//System.out.println("Connected to: " + clientSocket.getInetAddress().getHostAddress() + ". OK");
		} catch(UnknownHostException unknownHost) {
			System.err.println("Unknown host: " + info.getIp() + ":" + info.getPort());	
			return false;			
		} catch (IOException ex) {
			System.out.println("Socket timeout");
			return false;
		}

		try
		{	boolean sentOk = false;	 							
		ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
		out.flush();
		ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());			

		// (1) send command			
		out.writeObject("takeMyTimetable");

		// (2) send number of objects
		int number = _timetable.size();
		out.writeObject(number);

		// (3)send booking/canceling records				
		for (int i = 0; i < number; ++i) {
			out.writeObject(_timetable.get(i));					
		}

		// (4) say "goodbye"			
		out.writeObject(new String("goodbye"));
		out.flush();

		// (5) receive ok reply "goodbye"
		String goodbye;
		try {
			goodbye = (String)in.readObject();
			if (goodbye.equals("goodbye")) {
				System.out.println("Timetable sending finished fine. Delivered to " + info.toString());
				sentOk = true;
			}
		} catch (ClassNotFoundException e) {
			System.out.println("String cast failed in the sendTimetable()");
		}

		in.close();
		out.close();
		clientSocket.close();				

		return sentOk;
		} catch(Exception e){	
			System.out.println("Exception in sendTimetable()");
		}	

		return false;				
	}

	/**
	 * This function requests booking/canceling records from destination host
	 * @param info destination host credentials
	 * @return list of request records or empty list in case of unreachable host
	 */
	public static ArrayList<RequestRecord> requestRecords(HostInfo info) 
	{
		if (info == null) {
			System.out.println("requestRecords(): Host info is null.");
			return null;
		}

		Socket clientSocket = null;
		try
		{		
			// open new socket connection
			clientSocket = new Socket(info.getIp(), info.getPort());
			System.out.println("Connected to: " + clientSocket.getInetAddress().getHostAddress() + ". OK");
		} catch(UnknownHostException unknownHost) {
			System.err.println("Unknown host: " + info.getIp() + ":" + info.getPort());	
			return null;			
		} catch (IOException ex) {
			System.out.println("Socket timeout");
			return null;
		}

		try
		{
			ArrayList<RequestRecord>  requests = new ArrayList<RequestRecord>();
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());			

			// (1) send command			
			out.writeObject("giveMeRequests");
			out.flush();

			// (2) receive number of objects
			int numberOfObjects = 0;
			try {
				numberOfObjects = (int)in.readObject();
			} catch (ClassNotFoundException e1) {
				System.out.println("Failed to cast number of objects");
				return null;
			}	

			// (3)receive booking/canceling records
			for (int i = 0; i < numberOfObjects; ++i)
			{
				try
				{
					RequestRecord record = (RequestRecord)in.readObject();
					requests.add(record);
				}
				catch(ClassNotFoundException e)
				{
					System.err.println("Data received in unknown format");
				}
			}

			// (4) say "goodbye"			
			out.writeObject(new String("goodbye"));
			out.flush();

			// (5) receive ok reply "goodbye"
			try 
			{
				String goodbye;
				goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye")) {
					System.out.println("Records requesting finished fine.");
				}
			} catch (ClassNotFoundException e) {
				System.out.println("Object cast failed in the requestRecords()");
			}

			in.close();
			out.close();
			clientSocket.close();

			return requests;
		}		
		catch(IOException ioException)
		{
			System.err.println("IOexception occured in requestRecords()");
			ioException.printStackTrace();
			return null;
		}		
	}

	/**
	 * This function requests timetable from destination host
	 * @param info destination host credentials
	 * @return list of timetable entries or empty list in case of unreachable host
	 */
	public static ArrayList<TimeTableEntry> requestTimetable(HostInfo info) 
	{	
		if (info == null) {
			System.out.println("requestTimetable(): Host info is null.");
			return null;
		}

		Socket clientSocket = null;
		try
		{		
			// open new socket connection
			clientSocket = new Socket(info.getIp(), info.getPort());			
			//System.out.println("Connected to: " + clientSocket.getInetAddress().getHostAddress() + ". OK");
		} catch(UnknownHostException unknownHost) {
			System.err.println("Unknown host: " + info.getIp() + ":" + info.getPort());	
			return null;			
		} catch (IOException ex) {
			System.out.println("Socket timeout");
			return null;
		}
		try 
		{
			ArrayList<TimeTableEntry> timetable = new ArrayList<TimeTableEntry>();
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());			

			// (1) send command			
			out.writeObject("giveMeTimetable");
			out.flush();

			// (2) receive number of objects
			int numberOfObjects = 0;
			try {
				numberOfObjects = (int)in.readObject();
			} catch (ClassNotFoundException e1) {
				System.out.println("Failed to cast number of objects");
				return null;
			}			

			// (3)receive booking/canceling records
			for (int i = 0; i < numberOfObjects; ++i)
			{
				try
				{
					TimeTableEntry record = (TimeTableEntry)in.readObject();
					timetable.add(record);
				}
				catch(ClassNotFoundException e)
				{
					System.err.println("Data received in unknown format");
				}
			}

			// (4) say "goodbye"			
			out.writeObject("goodbye");
			out.flush();

			// (5) receive ok reply "goodbye"
			try 
			{
				String goodbye;
				goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye")) {
					System.out.println("Timetable requesting finished fine.");
				}
			} catch (ClassNotFoundException e) {
				System.out.println("Object cast failed in the requestTimetable()");					
			}				

			in.close();
			out.close();
			clientSocket.close();

			return timetable;
		}		
		catch(IOException ioException)
		{
			System.err.println("IOexception occured in requestTimetable()");
			ioException.printStackTrace();
			return null;
		}				
	}	

	/* This function is to communicate between air companies only! */
	public static boolean sendBroadcast(LinkedList<HostInfo> recipients, ArrayList<RequestRecord> requests, ArrayList<TimeTableEntry> timetable)
	{		
		boolean sentOk = false;
		ListIterator<HostInfo> li = recipients.listIterator();
		while (li.hasNext()) {
			HostInfo recipient = li.next();			
			boolean currentOk = sendBroadcast(recipient, requests, timetable);
			sentOk = sentOk && currentOk;
		}		
		return sentOk;
	}

	public static boolean sendBroadcast(HostInfo recipient, ArrayList<RequestRecord> requests, ArrayList<TimeTableEntry> timetable)
	{		
		if (recipient == null) {
			System.out.println("sendBroadcast(): Host info is null.");
			return false;
		}		

		Socket clientSocket = null;
		try
		{				
			clientSocket = new Socket(recipient.getIp(), recipient.getPort());			
		} catch(UnknownHostException unknownHost) {
			System.err.println("Unknown airCompany: " + recipient.getIp() + ":" + recipient.getPort());	
			return false;			
		} catch (IOException ex) {
			System.out.println("Socket timeout");
			return false;
		}

		try
		{	
			boolean sentOk = false;
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

			out.writeObject("broadcast");			

			// flushing requests
			int reqs_number = (requests == null) ? 0 : requests.size();
			out.writeObject(reqs_number);

			for (int i = 0; i < reqs_number; ++i) {				
				out.writeObject(requests.get(i));
			}
			out.flush();

			// flushing timetable
			int tt_number = (timetable == null) ? 0 : timetable.size();
			out.writeObject(tt_number);

			for (int i = 0; i < tt_number; ++i) {				
				out.writeObject(timetable.get(i));
			}
			out.flush();

			out.writeObject("goodbye");
			out.flush();

			try {
				String goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye")) {
					System.out.println("Broadcast sent fine. Delivered to " + recipient.toString());
					sentOk = true;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.out.println("Object cast failed in the sendRequests()");
			}

			in.close();
			out.close();
			clientSocket.close();				

			return sentOk;
		} catch(Exception e){
			e.printStackTrace();
			System.out.println("Exception in sendBroadcast()");
		}	

		return false;		
	}	

	public static boolean sendTakeOff(HostInfo info)
	{
		if (info == null) {
			System.out.println("sendTakeOff(): Host info is null.");
			return false;
		}

		Socket clientSocket = null;
		try
		{				
			clientSocket = new Socket(info.getIp(), info.getPort());			
		} catch(UnknownHostException unknownHost) {
			System.err.println("Unknown host: " + info.getIp() + ":" + info.getPort());	
			return false;			
		} catch (IOException ex) {
			System.out.println("Socket timeout");
			return false;
		}

		try
		{	
			boolean sendedOk = false;										
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

			out.writeObject("takeOff");		
			out.flush();

			out.writeObject("goodbye");
			out.flush();

			try {
				String goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye")) {
					System.out.println("TakeOff comand sent fine. Delivered to " + info.toString());
					sendedOk = true;
				}
			} catch (ClassNotFoundException e) {
				System.out.println("Object cast failed in the sendRequests()");
			}

			in.close();
			out.close();
			clientSocket.close();				

			return sendedOk;
		} catch(Exception e){	
			System.out.println("Exception in sendRequests()");
		}	

		return false;		
	}	

	/**
	 * This function is designed to send request record to host
	 * @param info receiver host details
	 * @param record request record to send
	 */
	public static boolean sendRecord(HostInfo info, RequestRecord record) 
	{
		if (info != null) {
			try
			{					        	
				Socket clientSocket = null;
				try
				{		
					// open new socket connection
					clientSocket = new Socket(info.getIp(), info.getPort());
					System.out.println("Connected to: " + clientSocket.getInetAddress().getHostAddress() + ". OK");
				} catch(UnknownHostException unknownHost) {
					System.err.println("Unknown host: " + info.getIp() + ":" + info.getPort());	
					return false;			
				} catch (IOException ex) {
					System.out.println("Socket timeout");
					return false;
				}			
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.flush();

				// (1) send command			
				out.writeObject("takeMyRequest");

				// (2) send number of objects (one for our case)
				int number = 1;
				out.writeObject(number);

				// (3)send booking/canceling record
				out.writeObject(record);

				// (4) say "goodbye"			
				out.writeObject(new String("goodbye"));
				out.flush();

				out.close();
				clientSocket.close();			

				return true;
			} catch(Exception e){	
				System.out.println("Exception in sendRecord()");
			}	
		}

		return false;
	}

	/** Function for searching any available port for socket connection
	 *  between MIN_PORT_NUMBER and MAX_PORT_NUMBER specified in the function	
	 *  
	 * @return free port number
	 */
	public static int getAvailablePort() {
		int MIN_PORT_NUMBER = 1024;
		int MAX_PORT_NUMBER = 16384;

		int candidate;		
		Random generator = new Random();	
		do
		{			
			candidate = generator.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER - 1;			
			ServerSocket ss = null;
			DatagramSocket ds = null;
			try {
				ss = new ServerSocket(candidate);
				ss.setReuseAddress(true);
				ds = new DatagramSocket(candidate);
				ds.setReuseAddress(true);	            
				break;
			} 
			catch (IOException e) {
			} 
			finally {
				if (ds != null) {
					ds.close();
				}
				if (ss != null) {
					try {
						ss.close();
					} 
					catch (IOException e) {	                    
					}
				}
			}			
		} while(true);
		return candidate;
	}	

	/**
	 *  Request host name
	 *  @return either local host name or user input
	 */
	public static String getDeviceName() {
		String proposedName = null;
		try {
			proposedName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {			
			System.out.println("Input town device hostname (network name is " + proposedName + "): ");
			proposedName = readLine();			
		}
		return proposedName;
	}

	/** Function for reading input line 
	 * @return string containing user input
	 */
	public static String readLine()
	{
		String s = "";		
		try {
			InputStreamReader converter = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(converter);
			s = in.readLine();			
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}
}
