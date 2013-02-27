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
 * 	The main aim of this class is to process requests from clients 
 * 	about booking and timetable.
 *
 *  Town device can only receive confirmation messages from air company servers. 
 *   
 *  Protocol for accept thread (just receive confirm notifications)
 *  1st message: command {confirmation}
 *  2nd message: number of objects ready to send
 *  after:		 objects (type depend on command)
 *  last: 		 air company says "goodbye"
 * 				 town says "goodbye"
 */

/**
 * @author ruben
 *
 */
public class TownNetworkOperator extends Thread{
	private int 			_port;
	private ServerSocket 	_acceptSocket;
	private TownDevice 		_townDevice = null;
	
	TownNetworkOperator()
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
			System.out.println("Connection from: " + connection.getInetAddress().getHostAddress());
			
			ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
			
			try
			{
				String command = (String)in.readObject();				
				switch (command) {
				case "confirmation":
					processConfirmation(in);
					break;				
				default:
					System.err.println("Unknown socket command.");
					break;
				}
				
				String goodbye = (String)in.readObject();
				if (goodbye.equals("goodbye"))
				{					
					out.writeObject("goodbye");
					out.flush();
					System.out.println("Communication finished successfully. Reply \"goodbye\".");
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
	
	public void setListener(TownDevice townDevice)
	{
		_townDevice = townDevice;
	}	
	
	public int getPort()
	{
		return _port;
	}
	
	private void processConfirmation(ObjectInputStream in) throws IOException, ClassNotFoundException 
	{
		if (_townDevice != null) {
			System.out.println("Receiving confirmation from air company:");
			// receive number of entries
			int number = (int)in.readObject();					
			// receive objects
			ArrayList<RequestRecord> requests = new ArrayList<RequestRecord>();				
			for (int i = 0; i < number; ++i)
			{
				RequestRecord record = (RequestRecord)in.readObject();
				requests.add(record);			
				_townDevice.confirm(record);
			}
		}
	}	
}
