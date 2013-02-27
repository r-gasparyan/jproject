import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;

/** 
 *  This class contains main code for camp computer behavior
 */

/**
 * @author ruben
 *
 */

public class CampDevice extends Thread {	
	private int 				_port = 0;	
	private ActiveDeviceFinder 	_finder = null;
	private AcceptThread 		_accept = null;
	private DatabaseConnector 	_sqlite = null;	
			
	public void run()
	{		
		/* launch device finder and acceptor threads before starting */
		initiate_helper_threads();
		
		/* Main menu */
		try
		{			
			String response = null;
			do
			{					
				printMainMenu();
				response = readLine();				
				processResponse(response);			
			} while ( !response.equals("0") );			
			
		} 
		catch(Exception e){
			e.printStackTrace();
		}	
		finally{
			/* stop executing helper threads */
			deinitiate_helper_threads();					
		}	
	}	

	private void initiate_helper_threads() 
	{
		System.out.println("Camp device application initialization...");
		
		//creating single file database connection
		_sqlite = new DatabaseConnector("camp.sqlite");		
		
		/* 	Creating acceptor thread for incoming connections:
			ask or give data about requests and timetable */		
		_accept = new AcceptThread();
		_accept.start();
		
		/* Start finding switched-on devices in _finderThread */		
		_finder = new ActiveDeviceFinder("_camp._tcp.local.", NetworkOperator.getDeviceName(), _accept.getPort());			
		LinkedList<HostInfo> list = _finder.getSimilarDeviceList();
		for (int i = 0; i < list.size(); ++i) {
			System.out.println("Found CAMP device: " + list.get(i).toString());
		}
		
		/* ask request records and timetable from first device,
		   considering they all synchronized */
		if (!list.isEmpty()) {
			HostInfo info = list.getFirst();
			System.out.println("Requesting data from " + info.getName() + " about booking and timetable");			
			_sqlite.mergeRequests( NetworkOperator.requestRecords(info) );			
			_sqlite.mergeTimetable( NetworkOperator.requestTimetable(info) );
			System.out.println("Requesting finished");
		}	
		
		System.out.println("Initialization finished.");
	}
	
	private void deinitiate_helper_threads() {
		System.out.println("");
		System.out.println("Closing application...");
		/* stop jmdns service in the _finderThread */ 
		_finder.closeJmDNS();
				
		/* stop acceptor thread */
		_accept.stopAccepting();
				
		try {		
			_accept.join();
		} catch (InterruptedException e) {
			System.out.println("join() interrupted");
		}

		// important - close database connection at the end
		_sqlite.closeConnection();	
		
		System.out.println("Application closed.");
	}	

	public void printMainMenu()
	{
		System.out.println("");
		System.out.println("Select menu item:");
		System.out.println("1. Make a booking request");
		System.out.println("2. Make a canceling request");
		System.out.println("3. Show all requests");
		System.out.println("4. Show timetable");
		System.out.println("5. See the list of CAMP devices");	
		System.out.println("6. Send take off command to helicopter");
		System.out.println("0. Exit");
	}
	
	private void processResponse(String response) {
		switch (response){
		case "1": 
			makeBooking();
			break;
		case "2":
			makeCanceling();
			break;
		case "3":
			showAllRequests();
			break;
		case "4":
			showTimetable();
			break;
		case "5":
			showCampDevices();
			break;	
		case "6":
			sendTakeOff(); 
			break;
		default:
			break;
		}
	}

	private void showAllRequests() {
		System.out.println("ALL REQUESTS LIST:");
		ArrayList<RequestRecord> reqs = _sqlite.getAllRequests();
		for (int i = 0; i < reqs.size(); ++i)
		{
			System.out.println(i + ". " + reqs.get(i).toString());
		}
	}	

	private void showTimetable() {
		System.out.println("Select direction (0 - TO CAMP, 1 - TO TOWN):");
		int direction = readInt();
		if (direction == 0 || direction == 1) {
			printTimetable(direction);				
		}
		else {
			System.out.println("Incorrect direction.");
		}
	}

	private void showCampDevices() {
		LinkedList<HostInfo> devices = _finder.requestSimilarDeviceList();
		if (devices.size() == 0) {
			System.out.println("Other camp devices not found");
		}
		else {
			ListIterator<HostInfo> li = devices.listIterator();
			while (li.hasNext()) {
				HostInfo info = li.next();        	
				System.out.println("Device " + info.toString());
			}
		}
	}

	/**
	 * This function is responsible for making a booking.
	 */
	private void makeBooking() {
		System.out.println("*** MAKE A BOOKING ***");
		// direction part
		int direction = -1;  
		do{
			System.out.println("Select direction (0 - TO CAMP, 1 - TO TOWN):");
			direction = readInt();
		} while (direction != 0 && direction != 1);
		
		printTimetable(direction);
		
		System.out.println("Select flight by number: ");
		TimeTableEntry flight = null;
		int flight_number = -1; 
		do{								
			flight_number = readInt();
			flight = _sqlite.getTimeTableEntry(flight_number);
			if (flight == null || flight.getDirection() != direction){
				System.out.println("Sorry, invalid flight number. Retry:");
				flight = null;
			}			
		} while (flight == null);			
		
		// flight time part
		String date;
		boolean ok = false;
		Date expected_flight_time = null;		
		SimpleDateFormat ft = new SimpleDateFormat ("HH:mm dd/MM/yyyy");		
		do{			
			System.out.println("Selected flight: " + flight.getTime());
			System.out.println("Closest booking date: not earlier than " + ft.format(closestDate(direction)));
			System.out.println("Input date of flight like '14/05/2012' or 0 to EXIT and change flight): ");
			date = readLine();
			if (date.equals("0")){
				System.out.println("Exit.");
				return;
			}					
			try {
				expected_flight_time = ft.parse(flight.getTime() + " " + date);				
				ok = expected_flight_time.after(closestDate(direction)) || expected_flight_time.equals(closestDate(direction));
			}
			catch(ParseException e) {
				System.out.println("Unable to parse " + date);					
			}			
		} while (!ok);	
		System.out.println("Expected departure time: " + ft.format(expected_flight_time));
		
		System.out.println("Passenger name: ");
		String passenger = readLine();
		
		// type - ON_DEMAND or SPECIFIED_FLIGHT
		int ticket_type = -1;
		do{
			System.out.println("Select ticket type (0 - SPECIFIED_TIME_FLIGHT, 1 - ON_DEMAND):");
			ticket_type = readInt();
		} while (ticket_type != 0 && ticket_type != 1);
		
		RequestRecord record = new RequestRecord(passenger, flight_number, 
				expected_flight_time, direction, 0, ticket_type);
		System.out.println("New request has been created: ");
		System.out.println(record.toString());	
		
		// put request into database
		_sqlite.addRequestRecord(record);
		
		// notify other camp devices
		notifyAllDevices(record);
	}	
	
	private void sendTakeOff()
	{
		LinkedList<HostInfo> list = _finder.jmdnsRequestDevices("_helicopter._tcp.local.");
		if (list.isEmpty()) {
			System.out.println("No helicopters in the system.");
		} else {
			System.out.println("HELICOPTERS: ");
			for (int i = 0; i < list.size(); ++i) {
				System.out.println(i + ". " + list.get(i).toString());
			}
			
			System.out.println("Select helicopter number to take off: ");
			int number = readInt();
			
			if (number < 0 || number >= list.size()) {
				System.out.println("Invalid number.");
				return;
			}
			
			HostInfo helicopter = list.get(number);
			NetworkOperator.sendTakeOff(helicopter);
		}		
	}

	private Date closestDate(int direction) {
		int one_way_flight_duration = 60*60*1000; // one hour in milliseconds 
		Date from_camp_request = closestFlight(new Date(), 1);
		Date reached_town = new Date(from_camp_request.getTime() + one_way_flight_duration);		
		if (direction == 0) {
			// closest date for flying TO_CAMP
			// need to send request with nearest helicopter
			// and do not wait for confirmation (maybe he will fly with request)
			return reached_town;
		}
		else {
			// closest date for flying TO_TOWN
			// need to send request with nearest helicopter and
			// wait for confirmation received from town 
			Date from_town_confirm = closestFlight(reached_town, 0);
			Date reached_camp = new Date(from_town_confirm.getTime() + one_way_flight_duration);
			return reached_camp;			
		}
	}
	
	private Date closestFlight(Date start, int direction)
	{
		SimpleDateFormat ft = new SimpleDateFormat ("HH:mm");		
		ArrayList<TimeTableEntry> flights = _sqlite.getTimeTable();
		try {
			Date only_hh_mm = ft.parse(ft.format(start));
			Date only_dd_mm_yyyy = new Date(start.getTime() - only_hh_mm.getTime());
			for (int i = 0; i < flights.size(); ++i)
			{
				Date tt_value = ft.parse(flights.get(i).getTime());
				if (tt_value.after(only_hh_mm) && flights.get(i).getDirection() == direction) {					
					return new Date(only_dd_mm_yyyy.getTime() + tt_value.getTime());
				}					
			}
		} catch (ParseException e) {			
		}
		
		// to avoid null pointer exception
		return new Date(start.getTime() + 24*60*60*1000);
	}

	private void makeCanceling() {
		System.out.println("*** MAKE A CANCELING *** ");
		System.out.println("REQUESTS IN DATABASE:");
		ArrayList<RequestRecord> bookings = _sqlite.getBookRequests();
		int requests_size = bookings.size();		
		for(int i = 0; i < requests_size; ++i) 
		{
			System.out.println(i + ". " + bookings.get(i).toString());
		}
		
		System.out.println("Please, select the ticket of the booking to cancel: ");
		int choice = readInt();
		if (choice < 0 || choice >= requests_size) {
			System.out.println("Incorrect booking number!");
			return;
		}
		
		RequestRecord record = bookings.get(choice);		
		if (_sqlite.getCancelRequestForTicket( record.getTicket() ) != null) {
			System.out.println("Same cancel request has been already performed.");
			return;
		}
		
		// add canceling request to database
		String ticket = String.valueOf(record.getTicket());
		RequestRecord cancel = new RequestRecord(ticket, 1);
		_sqlite.addRequestRecord(cancel);
		notifyAllDevices(cancel);
	}
	
	private void notifyAllDevices(RequestRecord record) {
		LinkedList<HostInfo> devices = _finder.requestSimilarDeviceList();
		ArrayList<RequestRecord> requests = new ArrayList<RequestRecord>();
		requests.add(record);
		NetworkOperator.sendBroadcast(devices, requests, null);
	}

	private int readInt() {
		int value = Integer.MIN_VALUE;
		try{
			value = Integer.parseInt(readLine());										
		}
		catch(NumberFormatException ex){			
		}
		return value;
	}
	
	/**
	 * 	This function takes system timetable and prints it out
	 * 	in two separated parts: times for flights to CAMP and
	 * 	times for flights to TOWN
	 */
	private void printTimetable(int direction) {
		// check for db connection
		if (_sqlite == null){
			System.err.println("SQLiteConnector is bad. Unable to get the timetable.");
			return;
		}
		
		ArrayList<TimeTableEntry> timetable = _sqlite.getTimeTable();
		if (timetable.isEmpty()) {
			System.out.println("*** Timetable is empty ***");
			return;
		}
		
		String dir = (direction == 0) ? "TO CAMP" : "TO TOWN";		
		System.out.println("*** FLIGHTS TIME TABLE (Flight Number - Time) *** " + dir + " ***");		
		
		int count = 0;
		String line = new String();			 
		for(int i = 0; i < timetable.size(); ++i)
		{
			TimeTableEntry tt = timetable.get(i);
			if (tt.getDirection() == direction) {
				++count;
				line = line + ("[Flight " + tt.getFlightNumber() + " - " + tt.getTime() + "] ");				
				if (count % 4 == 0){
					System.out.println(line);
					line = "";
				}
			}				
		}					
		
		System.out.println(line);
		line = "";
	}	
	
	/** 
	 * Function for reading input line	 
	 */
	public String readLine()
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
