import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * This class implements air company network interface.
 */

/**
 * @author ruben
 *
 */
public class AirCompanyNetworkOperator extends Thread {
	private int 				_port;
	private ServerSocket 		_acceptSocket;
	private AirCompanyDevice 	_airCompany = null;
	private DatabaseConnector 	_sqlite = null;
	
	public AirCompanyNetworkOperator()
	{		
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
		_sqlite = new DatabaseConnector("aircompany.sqlite");
		
		while ( !_acceptSocket.isClosed() )
		{
			accept();
		}	
		
		_sqlite.closeConnection();
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
				// maybe stopAccepting() was called
				return;
			}
			
			System.out.println("Air company got connection from: " + connection.getInetAddress().getHostAddress());
			
			ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
			
			try
			{
				String command = (String)in.readObject();		
				System.out.println("Incoming command: " + command);
				switch (command) {
				case "giveMeRequests":
					flushRequests(in, out);
					break;
				case "giveMeTimetable":
					flushTimetable(in, out);
					break;
				case "takeMyRequests":
					receiveRequests(in, out);
					_airCompany.helicopterLanded();
					break;
				case "broadcast":
					receiveBroadcast(in, out);					
					break;
				case "takeMyRequest":					
					receiveRequests(in, out);
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
				e.printStackTrace();
				System.err.println("Data received in unknown format");
			}			
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.err.println("Some IO error in accept()");
		}				
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
	
	public void setListener(AirCompanyDevice airCompany)
	{
		_airCompany = airCompany;
	}	
	
	public synchronized int getPort()
	{
		return _port;
	}
	
	private void flushRequests(ObjectInputStream in, ObjectOutputStream out) throws IOException 
	{				
		ArrayList<RequestRecord> requests = _sqlite.getAllRequests();
		// send number of requests
		int number = requests.size();
		out.writeObject(number);
		out.flush();		
		// send requests list
		for (int i = 0; i < number; ++i)
		{
			RequestRecord record = requests.get(i);
			out.writeObject(record);
		}
		out.flush();
	}
	
	private void flushTimetable(ObjectInputStream in, ObjectOutputStream out) throws IOException 
	{			
		ArrayList<TimeTableEntry> timetable = _sqlite.getTimeTable();
		// send number of entries
		int number = timetable.size();
		out.writeObject(number);
		out.flush();
		// send timetable entries
		for (int i = 0; i < number; ++i)
		{
			TimeTableEntry entry = timetable.get(i);
			out.writeObject(entry);
		}
		out.flush();
	}
	
	private void receiveRequests(ObjectInputStream in, ObjectOutputStream out) throws IOException,
		ClassNotFoundException 
	{			
		// receive number of entries
		int number = (int)in.readObject();					
		ArrayList<RequestRecord> requests = new ArrayList<RequestRecord>();				
		// receive requests
		for (int i = 0; i < number; ++i)
		{
			RequestRecord record = (RequestRecord)in.readObject();
			requests.add(record);
			//_sqlite.addRequestRecord(record);			
		}		
		_sqlite.mergeRequests(requests);
	}
		
	private void receiveTimetable(ObjectInputStream in, ObjectOutputStream out) throws IOException,
		ClassNotFoundException 
	{		
		// receive number of entries
		int number = (int)in.readObject();					
		ArrayList<TimeTableEntry> timetable = new ArrayList<TimeTableEntry>();				
		// receive timetable
		for (int i = 0; i < number; ++i)
		{
			TimeTableEntry entry = (TimeTableEntry)in.readObject();
			timetable.add(entry);
			//_sqlite.addTimeTableEntry(entry);			
		}	
		_sqlite.mergeTimetable(timetable);
	}	
	
	private void receiveBroadcast(ObjectInputStream in, ObjectOutputStream out) throws ClassNotFoundException, IOException
	{
		receiveRequests(in, out);
		receiveTimetable(in, out);
		System.out.println("Broadcast received.");
	}
}
