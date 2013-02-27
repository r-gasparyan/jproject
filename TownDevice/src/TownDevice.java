import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;

/** 
 *  This class contains main code for town device application behavior
 */

/**
 * @author ruben
 *
 */

public class TownDevice extends Thread {	
	private int 				_port = 0;	
	private ActiveDeviceFinder 	_finder = null;
	private TownNetworkOperator _operator = null;	
	private ArrayList<TimeTableEntry> _timetable = null;

	public void confirm(RequestRecord record)
	{
		System.out.println("Confirmed " + record.toString());
	}
			
	public void run() 
	{		
		initialize();
		
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
			deinitialize();					
		}	
	}	
	
	private void initialize() 
	{				
		System.out.println("Town device application initialization...");
			
		_operator = new TownNetworkOperator();
		_operator.setListener(this);
		_operator.start();
		
		_port = _operator.getPort();
		
		/* find all air companies */		
		_finder = new ActiveDeviceFinder("_town._tcp.local.", NetworkOperator.getDeviceName(), _port);			
		LinkedList<HostInfo> list = _finder.jmdnsRequestDevices("_aircompany._tcp.local.");
		for (int i = 0; i < list.size(); ++i) {
			System.out.println("Found air company: " + list.get(i).toString());
		}	
				
		System.out.println("Initialization finished.");
	}
	
	private void deinitialize() 
	{		
		System.out.println("Closing application...");
		
		/* stop jmdns service */ 
		_finder.closeJmDNS();
				
		/* stop acceptor thread */
		_operator.stopAccepting();
		
		try {		
			_operator.join();
		} catch (InterruptedException e) {
			System.out.println("join() interrupted");
		}
		
		System.out.println("Application closed.");
	}	

	private void printMainMenu()
	{
		System.out.println("");
		System.out.println("Select menu item:");
		System.out.println("1. Make a booking request");
		System.out.println("2. Make a canceling request");
		System.out.println("3. Show all requests");
		System.out.println("4. Show timetable");
		System.out.println("5. See the list of air companies");
		System.out.println("0. Exit");
	}
	
	private void processResponse(String response) {
		switch (response){
		case "1":
			makeBookingFromTown();
			break;
		case "2":
			makeCancelingFromTown();
			break;
		case "3":
			showAllRequests();
			break;
		case "4":
			showTimetable();			
			break;
		case "5":
			showAirCompanies();
			break;		
		default:
			break;
		}
	}
	
	/**
	 * This function is responsible for making a booking.
	 */
	private void makeBookingFromTown() {
		HostInfo info = getAirCompany();		
		if (info == null) {
			System.out.println("Unable to make a booking. No air company available.");
			return;
		}
		
		System.out.println("*** MAKE A BOOKING ***");
		// direction part
		int direction = -1;  
		do{
			System.out.println("Select direction (0 - TO CAMP, 1 - TO TOWN):");
			direction = readInt();
		} while (direction != 0 && direction != 1);
		
		ArrayList<TimeTableEntry> timetable = NetworkOperator.requestTimetable(info);
		if (timetable == null || timetable.isEmpty()) {
			System.out.println("Unable to get a timetable.");
			return;
		}
		
		printTimetable(timetable, direction);
		
		System.out.println("Select flight by number: ");
		TimeTableEntry flight = null;
		int flight_number = -1; 
		do{								
			flight_number = readInt();			
			flight = getTimeTableEntry(timetable, flight_number); 
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
			System.out.println("Closest booking date: not earlier than " + ft.format(closestDate(timetable, direction)));
			System.out.println("Input date of flight like '14/05/2012' or 0 to EXIT and change flight): ");
			date = readLine();
			if (date.equals("0")){
				System.out.println("Exit.");
				return;
			}					
			try {
				expected_flight_time = ft.parse(flight.getTime() + " " + date);
				Date closest_date = closestDate(timetable, direction);
				ok = expected_flight_time.after(closest_date) || expected_flight_time.equals(closest_date);
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
		
		// notify other camp devices
		notifyAirCompany(record);
	}	
	
	private void makeCancelingFromTown() {
		HostInfo info = getAirCompany();	
		if (info == null) {
			System.out.println("Unable to make a cancelling. No air company available.");
			return;
		}
		
		System.out.println("*** MAKE A CANCELING *** ");
		System.out.println("REQUESTS IN DATABASE:");
		ArrayList<RequestRecord> requests = NetworkOperator.requestRecords(info);		
		ArrayList<RequestRecord> bookings = new ArrayList<RequestRecord>();
		ArrayList<RequestRecord> cancellings = new ArrayList<RequestRecord>();
		for(int i = 0; i < requests.size(); ++i) 
		{	
			RequestRecord record = requests.get(i); 
			if (record.getRequestType() == 0) {
				// booking
				bookings.add(record);
				System.out.println(i + ". " + record.toString());
			}
			else {
				// cancelling
				cancellings.add(record);
			}
		}
		
		System.out.println("Please, select the ticket of the booking to cancel: ");
		int choice = readInt();
		if (choice < 0 || choice >= bookings.size()) {
			System.out.println("Incorrect booking number!");
			return;
		}
		
		RequestRecord record = bookings.get(choice);		
		if (getCancelRequestForTicket( cancellings, record.getTicket() ) != null) {
			System.out.println("Same cancel request has been already performed.");
			return;
		}
		
		// add canceling request to database
		String ticket = String.valueOf(record.getTicket());
		RequestRecord cancel = new RequestRecord(ticket, 1);		
		notifyAirCompany(cancel);
	}
	
	/**
	 *  Show air companies by requesting devices list
	 */
	private void showAirCompanies() {
		LinkedList<HostInfo> devices = _finder.jmdnsRequestDevices("_aircompany._tcp.local.");
		if (devices == null || devices.size() == 0) {
			System.out.println("Air companies not found");
		}
		else {
			ListIterator<HostInfo> li = devices.listIterator();
			while (li.hasNext()) {
				HostInfo info = li.next();        	
				System.out.println("Air company " + info.toString());
			}
		}
	}

	/**
	 *  Requesting timetable from air company and show them
	 */
	private void showTimetable() {
		if (refreshTimetable()) {				
			System.out.println("Select direction (0 - TO CAMP, 1 - TO TOWN):");
			int direction = readInt();
			if (direction == 0 || direction == 1) {					
				printTimetable(_timetable, direction);		
			}
			else {
				System.out.println("Incorrect direction.");
			}				
		} else {
			System.out.println("Cannot find any air company in the network");
		}
	}

	/**
	 * Requesting all request records from air company and show them
	 */
	private void showAllRequests() {
		HostInfo info = getAirCompany();
		if (info != null) {
			System.out.println("ALL REQUESTS LIST:");
			ArrayList<RequestRecord> reqs = NetworkOperator.requestRecords(info);
			for (int i = 0; i < reqs.size(); ++i)
			{
				System.out.println(i + ". " + reqs.get(i).toString());
			}			
		} 
		else {
			System.out.println("Cannot find any air company in the network");
		}
	}
	
	
	private TimeTableEntry getTimeTableEntry(ArrayList<TimeTableEntry> timetable, int flight_number) 
	{
		for (int i = 0; i < timetable.size(); ++i)
		{
			TimeTableEntry entry = timetable.get(i);
			if (entry.getFlightNumber() == flight_number) {
				return entry;
			}
		}
		return null;
	}

	private Date closestDate(ArrayList<TimeTableEntry> timetable, int direction) {
		int one_way_flight_duration = 60*60*1000; // one hour in milliseconds 
		Date from_camp_request = closestFlight(timetable, new Date(), 1);
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
			Date from_town_confirm = closestFlight(timetable, reached_town, 0);
			Date reached_camp = new Date(from_town_confirm.getTime() + one_way_flight_duration);
			return reached_camp;			
		}
	}
	
	private Date closestFlight(ArrayList<TimeTableEntry> flights, Date start, int direction)
	{
		SimpleDateFormat ft = new SimpleDateFormat ("HH:mm");		
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


	private RequestRecord getCancelRequestForTicket(ArrayList<RequestRecord> cancellings, int ticket) 
	{
		String name = String.valueOf(ticket);
		for (int i = 0; i < cancellings.size(); ++i) 
		{
			if (name.equals(cancellings.get(i).getPassengerName()))
			{
				return cancellings.get(i);
			}
		}
		return null;
	}
	
	private HostInfo getAirCompany()
	{
		LinkedList<HostInfo> devices = _finder.jmdnsRequestDevices("_aircompany._tcp.local.");
		if (devices != null && !devices.isEmpty()) {
			HostInfo info = devices.getFirst();
			return info;
		}
		
		return null;
	}

	/**
	 * This function notifies first air company considering that replication of
	 * request between companies will be performed by themselves.
	 * @param record freshly created record
	 */
	private boolean notifyAirCompany(RequestRecord record) 
	{
		HostInfo info = getAirCompany();
		return NetworkOperator.sendRecord(info, record);
	}
	
	/**
	 * Reads integer value from input stream
	 */
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
	private void printTimetable(ArrayList<TimeTableEntry> timetable, int direction) 
	{
		if (timetable == null || timetable.isEmpty()) {
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
	
	private boolean refreshTimetable()
	{
		HostInfo info = getAirCompany();
		if (info != null) {
			_timetable = NetworkOperator.requestTimetable(info);
			if (_timetable != null) {
				return true;				
			}
		}		
		return false;		
	}	
	
	/** Function for reading input line	
	 *  
	 * @return string containing user input
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
