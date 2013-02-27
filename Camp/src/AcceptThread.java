import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * 	This class contains acceptor thread for processing incoming connections
 * 	and uses ServerSocket class to communicate.
 * 
 * 	The main aim of this class is to send data on request from clients 
 * 	about booking and timetable.
 * 
 * 	Protocol:
 *  1st message: command
 *  2nd message: number of objects ready to send or receive
 *  after:		 objects (type depends on command)
 *  last: 		 initiator says "goodbye"
 *  			 camp replies "goodbye"
 */

/**
 * @author ruben
 *
 */
public class AcceptThread extends Thread{
	private int 				_port = 0;
	private ServerSocket 		_acceptSocket = null;
	private DatabaseConnector 	_sqlite = null;	
	
	public AcceptThread()
	{	
		//creating single file database connection		
		_sqlite = new DatabaseConnector("camp.sqlite");
		
		_port = NetworkOperator.getAvailablePort();		
		
		try {
			// create server socket with queue of 10 possible incoming connections 
			_acceptSocket = new ServerSocket(_port, 10);
			// set up blocking mode for accepting socket (wait INFINITE)
			_acceptSocket.setSoTimeout(0);
		} 
		catch(SocketException e){
			System.err.println("Socket exception occured: " + e.toString());			
		}
		catch (IOException e) {
			System.err.println("Unable to bind server to port " + _port);
		}
	}
	
	public void run()
	{
		while ( !_acceptSocket.isClosed() )
		{
			accept();
		}
		
		// important - close database connection at the end
		_sqlite.closeConnection();	
	}
	
	public synchronized int getPort()
	{
		return _port;
	}
	
	public synchronized void stopAccepting()
	{
		if (_acceptSocket != null && !_acceptSocket.isClosed())
		{
			try {
				_acceptSocket.close();
			} catch (IOException e) {
				System.out.println("Problems with accept socket");
				e.printStackTrace();
			}
		}
	}
	
	private void accept() 
	{	
		try
		{				
			// acknowledge incoming connection
			Socket connection = null;
			try {
				connection = _acceptSocket.accept();
			} catch (IOException ex) {				
				return; // maybe stopAccepting() was called
			}
			System.out.println("Connection from: " + connection.getInetAddress().getHostAddress());
			
			ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
			
			try
			{
				String command = (String)in.readObject();				
				switch (command) {
				case "giveMeRequests":
					flushRequests(in, out);	
					break;
				case "giveMeTimetable":
					flushTimetable(in, out);
					break;
				case "takeMyRequests":
					receiveRequests(in, out);
					break;
				case "takeMyTimetable":
					receiveTimetable(in, out);	
					break;
				case "broadcast":
					receiveBroadcast(in, out);
					break;
				default:
					System.err.println("Unknown socket command.");												
				}				
				
				String goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye"))
				{
					out.writeObject("goodbye");
					out.flush();
					System.out.println("Communication finished successfully");
				}	
				
				connection.close();
				in.close();
				out.close();				
			}
			catch(ClassNotFoundException e)
			{
				System.err.println("Data received in unknown format");
			}			
		}
		catch(IOException e)
		{
			System.err.println("Some IO error in accept()");
		}		
	}

	/**
	 * This function receives requests from input stream
	 */
	private void receiveRequests(ObjectInputStream in, ObjectOutputStream out) throws IOException,
		ClassNotFoundException 
	{	
		ArrayList<RequestRecord> requests = new ArrayList<RequestRecord>();				

		int number = (int)in.readObject();					
			
		for (int i = 0; i < number; ++i)
		{
			RequestRecord record = (RequestRecord)in.readObject();
			requests.add(record);
		}
		
		_sqlite.mergeRequests(requests);
	}

	/**
	 * This function receives timetable entries from input stream
	 */
	private void receiveTimetable(ObjectInputStream in, ObjectOutputStream out) throws IOException,
		ClassNotFoundException 
	{
		ArrayList<TimeTableEntry> timetable = new ArrayList<TimeTableEntry>();				

		int number = (int)in.readObject();					
		
		for (int i = 0; i < number; ++i)
		{
			TimeTableEntry entry = (TimeTableEntry)in.readObject();
			timetable.add(entry);
		}
		
		_sqlite.mergeTimetable(timetable);
	}

	/**
	 * This function flushes all requests to the output stream
	 */
	private void flushRequests(ObjectInputStream in, ObjectOutputStream out) throws IOException 
	{		
		ArrayList<RequestRecord> requests = _sqlite.getAllRequests();
		
		int number = requests.size();
		out.writeObject(number);
		out.flush();		
		
		for (int i = 0; i < number; ++i)
		{
			RequestRecord record = requests.get(i);
			out.writeObject(record);
		}
		out.flush();
	}
	
	/** 
	 * This function flushes all timetable entries to the output stream
	 */
	private void flushTimetable(ObjectInputStream in, ObjectOutputStream out) throws IOException 
	{
		ArrayList<TimeTableEntry> timetable = _sqlite.getTimeTable();
	
		int number = timetable.size();
		out.writeObject(number);
		out.flush();
	
		for (int i = 0; i < number; ++i)
		{
			TimeTableEntry entry = timetable.get(i);
			out.writeObject(entry);
		}
		out.flush();
	}

	private void receiveBroadcast(ObjectInputStream in, ObjectOutputStream out) throws ClassNotFoundException, IOException
	{
		receiveRequests(in, out);
		receiveTimetable(in, out);
		System.out.println("Broadcast received.");
	}
}
