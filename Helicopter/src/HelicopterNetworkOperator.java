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
 * 	The main aim of this class is to communicate with camp devices and 
 *  air company servers
 *  
 *  Helicopter can process requests from camp devices and air company
 *  devices.
 *   
 *  It is possible that air company or camp device might say "takeOff",
 *  which will cause helicopter retrieve data and start journey.
 * 
 *  Protocol for accepting commands
 *  1st message: command {giveMeRequests, giveMeTimetable, takeMyRequests, takeMyTimetable and takeOff}
 *  2nd message: number of objects ready to transfer
 *  after:		objects (object's type is command-dependent)
 *  last: 		initiator says "goodbye"
 * 				helicopter says "goodbye"
 * 
 *  If command is "takeOff" there is no objects transferred.
 */

/**
 * @author ruben
 *
 */

public class HelicopterNetworkOperator extends Thread {
	private int 				_port;
	private ServerSocket 		_acceptSocket;
	private HelicopterDevice 	_helicopterDevice = null;
	
	HelicopterNetworkOperator()
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
		while ( !_acceptSocket.isClosed() )
		{
			accept();
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
				// maybe stopAccepting() was called
				return;
			}
			
			System.out.println("Helicopter got connection from: " + connection.getInetAddress().getHostAddress());
			
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
				case "takeMyTimeTable":
					receiveTimetable(in, out);	
					break;
				case "takeOff":					
					takeOff();
					break;
				default:
					System.err.println("Unknown socket command.");												
				}
				
				String goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye"))
				{
					out.writeObject(new String("goodbye"));
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
	 * This function sets the pointer to the helicopter device object 
	 * with a purpose to send incoming messages to him.
	 */
	public void setHelicopterDevice(HelicopterDevice devicePtr)
	{
		_helicopterDevice = devicePtr;
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
	
	public synchronized int getPort()
	{
		return _port;
	}
		
	private void flushRequests(ObjectInputStream in, ObjectOutputStream out) throws IOException 
	{
		System.out.println("Helicopter was asked to give booking and cancelling records");		
		ArrayList<RequestRecord> requests = _helicopterDevice.getRequests();
		// send number of requests
		int number = requests.size();
		out.writeInt(number);
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
		System.out.println("Helicopter was asked to give timetable");	
		ArrayList<TimeTableEntry> timetable = _helicopterDevice.getTimetable();
		// send number of entries
		int number = timetable.size();
		out.writeInt(number);
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
		System.out.println("Helicopter was asked to take incoming requests");	
		// receive number of entries
		int number = (int)in.readObject();					
		ArrayList<RequestRecord> requests = new ArrayList<RequestRecord>();				
		// receive timetable
		for (int i = 0; i < number; ++i)
		{
			RequestRecord record = (RequestRecord)in.readObject();
			requests.add(record);
		}
		
		_helicopterDevice.setRequests(requests);
	}
	
	private void receiveTimetable(ObjectInputStream in, ObjectOutputStream out) throws IOException,
		ClassNotFoundException 
	{
		System.out.println("Helicopter was asked to take incoming timetable");
		// receive number of entries
		int number = (int)in.readObject();					
		ArrayList<TimeTableEntry> timetable = new ArrayList<TimeTableEntry>();				
		// receive timetable
		for (int i = 0; i < number; ++i)
		{
			TimeTableEntry entry = (TimeTableEntry)in.readObject();
			timetable.add(entry);
		}
		
		_helicopterDevice.setTimetable(timetable);
	}	
	
	/**
	 * This function will force helicopter to request necessary data and 
	 * after this data has been received it will start journey.
	 */
	private void takeOff() 
	{
		if (_helicopterDevice != null) {
			_helicopterDevice.takeOff();	
		}
	}	
}
