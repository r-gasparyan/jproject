import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * 	This class is designed to operate with single file SQLite database using 
 * 	JDBC driver for connection between java and SQLite
 */

/**
 * @author ruben
 *
 */
public class DatabaseConnector {
	
	Connection connection = null;     
	
	/**
	 *	Connecting to the database with provided file path.
	 *  @param databasePath - full file name for SQLite database
	 */
	public DatabaseConnector(String databasePath)
	{
		try{
			Class.forName("org.sqlite.JDBC"); 
			connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
			
			createTablesIfNotExist();
		}
		catch (ClassNotFoundException e){
			System.err.println("Class not found");		
		} catch (SQLException e) {
			System.err.println("SQL caused exception");
		}
	}

	/**
	 * Creating timetable and requests tables if they are absent
	 */
	private void createTablesIfNotExist() 
	{		 
		String timetableQuery = "CREATE TABLE IF NOT EXISTS Timetable " 
					+ "(FlightNumber integer NOT NULL PRIMARY KEY UNIQUE,FlightTime time NOT NULL,"
					+ "Direction integer NOT NULL,AirCompany text NOT NULL);";
		executeQuery(timetableQuery);
		
		String requestsQuery = "CREATE TABLE IF NOT EXISTS Requests " 
					+ "(Ticket integer NOT NULL PRIMARY KEY UNIQUE,PassengerName text NOT NULL," 
					+ "FlightNumber integer,FlightDate text,Direction integer,RequestType integer NOT NULL," 
					+ "TicketType integer,Confirmed boolean NOT NULL, Checked boolean NOT NULL);";
		executeQuery(requestsQuery);
	}
	
	private void executeQuery(String query)
	{
		try {
			Statement statement = connection.createStatement();
			statement.execute(query);
			statement.close();
		} catch (SQLException e) {
			System.err.println("Could not execute query: " + query);
			e.printStackTrace();
		}
	}
	
//-------------------------   REQUESTS  ----------------------------
	
	public synchronized ArrayList<RequestRecord> getAllRequests()
	{
		ArrayList<RequestRecord> requests = new ArrayList<RequestRecord>();
		requests.addAll(getBookRequests());
		requests.addAll(getCancelRequests());
		return requests;
	}
	
	public synchronized ArrayList<RequestRecord> getBookRequests()
	{
		ArrayList<RequestRecord> bookings = new ArrayList<RequestRecord>();

		Statement statement = null;
		ResultSet table = null;		
		String query = "SELECT * FROM Requests WHERE RequestType = '0'";
		try {			
			statement = connection.createStatement();			
			table = statement.executeQuery(query);
			while (table.next()) {								
				RequestRecord record = getBookingFrom(table);
				if (record != null) {
					bookings.add(record);
				}
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in timetable request code");			
		}
		return bookings;		
	}
	
	public synchronized ArrayList<RequestRecord> getCancelRequests()
	{		
		ArrayList<RequestRecord> cancelings = new ArrayList<RequestRecord>();

		Statement statement = null;
		ResultSet table = null;		
		String query = "SELECT * FROM Requests WHERE RequestType = '1'";
		try {			
			statement = connection.createStatement();			
			table = statement.executeQuery(query);
			while (table.next()) 
			{
				RequestRecord record = getCancelingFrom(table);
				if (record != null) {
					cancelings.add(record);	
				}
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in timetable request code");			
		}
		return cancelings;		
	}
	
	public synchronized void addRequestRecord(RequestRecord record)
	{
		if (isRecordInDatabase(record.getTicket())) {
			return;
		}
		
		if (record.getRequestType() == 0) {
			addBookRecord(record);
		}
		else {
			addCancelRecord(record);
		}
	}
	
	private synchronized void addBookRecord(RequestRecord book)
	{
		if (book.getRequestType() != 0) {
			System.out.println("SQLiteConnector. Unable to add canceling record as booking.");
			return;
		}		
		
		String confirmed = (book.isConfirmed()) ? "1" : "0";
		String checked = (book.isChecked()) ? "1" : "0";		

		SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy");
		String date = ft.format(book.getFlightDate());
		String query = "INSERT INTO Requests (Ticket,PassengerName,FlightNumber,FlightDate,Direction," 
				+ "RequestType,TicketType,Confirmed,Checked) VALUES (" 
				+ book.getTicket() + ","
				+ "\"" + book.getPassengerName() + "\","
				+ book.getFlightNumber() + ","
				+ "\"" + date + "\","
				+ book.getDirection() + ","
				+ book.getRequestType() + ","
				+ book.getTicketType() + ","
				+ confirmed + ","
				+ checked + ")";
		//System.out.println(query);
		executeQuery(query);
	}
	
	private synchronized void addCancelRecord(RequestRecord cancel)
	{
		if (cancel.getRequestType() != 1) {
			System.out.println("SQLiteConnector. Unable to add booking record as canceling.");
			return;
		}
		
		String confirmed = (cancel.isConfirmed()) ? "1" : "0";
		String checked = (cancel.isChecked()) ? "1" : "0";
		String query = "INSERT INTO Requests (Ticket,PassengerName,FlightNumber,FlightDate,Direction," 
				+ "RequestType,TicketType,Confirmed,Checked) VALUES (" 
				+ cancel.getTicket() + ","
				+ "\"" + cancel.getPassengerName() + "\","
				+ "NULL" + ","
				+ "NULL" + ","
				+ "NULL" + ","
				+ cancel.getRequestType() + ","
				+ "NULL" + ","
				+ confirmed + ","
				+ checked + ")";
		//System.out.println(query);
		executeQuery(query);
	}
	
	public synchronized RequestRecord getCancelRequestByTicket(int ticket)
	{
		String query = "SELECT * FROM REQUESTS WHERE RequestType = '1' and Ticket=" + ticket;
		RequestRecord record = null;
		
		Statement statement = null;
		ResultSet table = null;	
		try {			
			statement = connection.createStatement();
			table = statement.executeQuery(query);
			while (table.next()) {				
				record = getCancelingFrom(table);				
				// considering the unique tickets for each request
				break;			
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in cancel request by ticket code");			
		}
		return record;
		
	}
	
	public synchronized RequestRecord getCancelRequestForTicket(int ticket)
	{
		String query = "SELECT * FROM REQUESTS WHERE RequestType = '1' and PassengerName=\"" + String.valueOf(ticket) + "\"";
		RequestRecord record = null;
		
		Statement statement = null;
		ResultSet table = null;	
		try {			
			statement = connection.createStatement();
			table = statement.executeQuery(query);
			while (table.next()) {				
				record = getCancelingFrom(table);				
				// considering the unique tickets for each request
				break;			
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in cancel request by ticket code");			
		}
		return record;
	}
	
	private RequestRecord getCancelingFrom(ResultSet table) throws SQLException
	{
		if (table == null || table.getInt("RequestType") != 1) {
			return null;
		}
		
		RequestRecord result = new RequestRecord(table.getString("PassengerName"), table.getInt("RequestType"));
		result.setTicket(table.getInt("Ticket"));
		if (table.getBoolean("Confirmed")){
			result.confirm();
		}
		if (table.getBoolean("Checked")){
			result.markAsChecked();
		}	
		return result;
	}
	
	public synchronized ArrayList<RequestRecord> getBookRecordsByName(String name)
	{
		String query = "SELECT * FROM REQUESTS WHERE PassengerName=" + name;
		return getBookRecordsByQuery(query);
	}

	/**
	 * This function is used to simplify getting bookings from database
	 * by using SELECT-like query, specified with WHERE field,
	 * for example: SELECT * FROM REQUESTS WHERE FlightDate="11/12/2012"
	 * 
	 * @param query - string with SELECT query
	 * @return first item from results
	 */
	private ArrayList<RequestRecord> getBookRecordsByQuery(String query) {
		ArrayList<RequestRecord> bookings = new ArrayList<RequestRecord>();
		
		Statement statement = null;
		ResultSet table = null;	
		RequestRecord record = null;
		try {			
			statement = connection.createStatement();
			table = statement.executeQuery(query);
			while (table.next()) {
				if (table.getInt("RequestType") != 0) {
					continue;
				}
				record = getBookingFrom(table);
				if (record != null) {
					bookings.add(record);
				}
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in timetable request code");			
		}
		return bookings;
	}	
	
	private RequestRecord getBookingFrom(ResultSet table) throws SQLException
	{	
		if (table == null || table.getInt("RequestType") != 0) {
			return null;
		}
		
		RequestRecord result = null;
		SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy");
		Date date = null;
		try{
			date = ft.parse(table.getString("FlightDate"));
		} catch(ParseException e) {
			System.err.println("SQLiteConnector. Failed to parse string: " + table.getString("FlightDate") + ".");
			return null;
		}
		
		result = new RequestRecord(table.getString("PassengerName"), 
				table.getInt("FlightNumber"), date, table.getInt("Direction"),
				table.getInt("RequestType"), table.getInt("TicketType"));
		result.setTicket(table.getInt("Ticket"));
		if (table.getBoolean("Confirmed")){
			result.confirm();
		}
		if (table.getBoolean("Checked")){
			result.markAsChecked();
		}	
		return result;
	}
	
//------------------------------------   TIMETABLE ----------------------------------------------
	
	public synchronized ArrayList<TimeTableEntry> getTimeTable()
	{
		String query = "SELECT * FROM Timetable ORDER BY FlightTime";
		
		ArrayList<TimeTableEntry> timetable = new ArrayList<TimeTableEntry>();		
		Statement statement = null;
		ResultSet table = null;		
		try {			
			statement = connection.createStatement();
			table = statement.executeQuery(query);
			while (table.next()) {			
				TimeTableEntry entry = new TimeTableEntry(table.getInt("FlightNumber"), table.getString("FlightTime"), 
						table.getInt("Direction"), table.getString("AirCompany"));
				timetable.add(entry);				
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in timetable request code");			
		}
		return timetable;		
	}
	
	public synchronized void addTimeTableEntry(TimeTableEntry entry)
	{
		String query = "INSERT INTO Timetable (FlightTime, Direction, AirCompany) VALUES (\"" 
				+ entry.getTime() +"\", "+ entry.getDirection() +", \"" + entry.getAirCompany() + "\")";
		//System.out.println(query);
		executeQuery(query);
	}
	
	/**
	 * 	This function requests timetable entry for corresponding flight 
	 * number from the database (considering that flight numbers are unique)
	 * 
	 * @param flight_number number of flight in our timetable
	 * @return timetable entry for specified flight (CHECK FOR null!)
	 */
	public synchronized TimeTableEntry getTimeTableEntry(int flight_number)
	{
		String query = "SELECT * FROM TIMETABLE WHERE FlightNumber=" + flight_number;				
		Statement statement = null;
		ResultSet table = null;	
		TimeTableEntry entry = null;
		try {			
			statement = connection.createStatement();
			table = statement.executeQuery(query);
			while (table.next()) {	
				entry = new TimeTableEntry(table.getInt("FlightNumber"), table.getString("FlightTime"), 
						table.getInt("Direction"), table.getString("AirCompany"));								
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in timetable request code");			
		}
		return entry;	
	}
	
	public void closeConnection()
	{
		try {
			connection.close();
		} catch (SQLException e) {
			System.out.println("Failed to close connection to SQLite");
		}
	}
	
//-----------------------------   MERGE PROCEDURES ----------------------------------
	/*
	 * Assume that we will need to either add requests or update them in the database.
	 * Assume that fresh data is more important and up-to-date for us.
	 */	
	public void mergeRequests(ArrayList<RequestRecord> requests) 
	{
		if (requests == null) {
			System.out.println("no requests to merge");
			return;
		}
		
		for (int i = 0; i < requests.size(); ++i)
		{
			RequestRecord record = requests.get(i);
			if (isRecordInDatabase(record.getTicket()))	{
				updateRequest(record);
			}
			else if (record.getRequestType() == 0) {
				addBookRecord(record);
			}
			else {
				addCancelRecord(record);
			}
		}		
	}
	
	/**
	 * Assume that only 'confirmed' and 'checked' fields can be updated
	 * @param record
	 */
	private void updateRequest(RequestRecord record) {		
		// update request record (checked and confirmed fields)			
		String confirmed = (record.isConfirmed()) ? "1" : "0";
		String checked = (record.isChecked()) ? "1" : "0";					
		String query = "UPDATE Requests SET " + "Confirmed=" + confirmed + ","
				+"Checked=" + checked + " WHERE Ticket=" + record.getTicket() + ";";			
		executeQuery(query);			
	}

	private boolean isRecordInDatabase(int ticket)
	{
		boolean inDatabase = false;
		String query = "SELECT * FROM Requests WHERE Ticket = '" + ticket + "'";
		ResultSet resultSet = null;		
		try {			
			Statement statement = connection.createStatement();
			resultSet = statement.executeQuery(query);	
			if (resultSet.next()){
				inDatabase = true;				
			}
			statement.close();
		} catch (SQLException e) {
			System.err.println("Could not execute query: " + query);
			e.printStackTrace();
		} 
		return inDatabase;
	}

	public void mergeTimetable(ArrayList<TimeTableEntry> timetable) 
	{
		if (timetable == null) {
			System.out.println("no timetable to merge");
			return;
		}
		
		for (int i = 0; i < timetable.size(); ++i)
		{
			TimeTableEntry entry = timetable.get(i);
			if (isFlightInDatabase(entry.getFlightNumber())) {
				updateFlight(entry);
			}
			else {
				addTimeTableEntry(entry);
			}
		}		
	}
	
	private void updateFlight(TimeTableEntry entry) {
		// update timetable entry (FlightTime, Direction and AirCompany fields)			
		String direction = (entry.getDirection() == 1) ? "1" : "0";		
		String query = "UPDATE Timetable SET " + "FlightTime='" + entry.getTime() + "', "
				+"Direction=" + direction +", AirCompany='" + entry.getAirCompany() 
				+ "' WHERE FlightNumber=" + entry.getFlightNumber() + ";";			
		executeQuery(query);		
	}

	private boolean isFlightInDatabase(int flightNumber) {
		boolean inDatabase = false;
		String query = "SELECT * FROM Timetable WHERE FlightNumber=" + flightNumber;
		ResultSet resultSet = null;		
		try {			
			Statement statement = connection.createStatement();
			resultSet = statement.executeQuery(query);	
			if (resultSet.next()){
				inDatabase = true;				
			}
			statement.close();
		} catch (SQLException e) {
			System.err.println("Could not execute query: " + query);
			e.printStackTrace();
		} 
		return inDatabase;
	}	
	
	private void cleanUp()
	{
		// TODO implement - remove checked and not confirmed items
	}

	
	/**
	 * Check in the database if flight is present by flight number.
	 * Used by AirCompanyDevice::addTimetableEntry()
	 * @param flightNumber
	 * @return whether flight with such number is present or not
	 */
	public boolean hasFlight(int flightNumber) 
	{
		boolean inDatabase = false;
		String query = "SELECT * FROM Timetable WHERE FlightNumber = '" + flightNumber + "'";
		ResultSet resultSet = null;		
		try {			
			Statement statement = connection.createStatement();
			resultSet = statement.executeQuery(query);	
			if (resultSet.next()){
				inDatabase = true;				
			}
			statement.close();
		} catch (SQLException e) {
			System.err.println("Could not execute query: " + query);
			e.printStackTrace();
		} 
		return inDatabase;
	}

	/**
	 * Returns all not checked requests to date.
	 * Used by AirCompanyDevice::getNotCheckedRequests()
	 * @return not yet checked requests
	 */
	public ArrayList<RequestRecord> getNotCheckedRequests() 
	{
		ArrayList<RequestRecord> notChecked = new ArrayList<RequestRecord>();
		String query = "SELECT * FROM Requests WHERE Checked =  '0'";
		Statement statement = null;
		ResultSet table = null;	
		try {			
			statement = connection.createStatement();
			table = statement.executeQuery(query);
			while (table.next()) 
			{
				RequestRecord record = (table.getInt("RequestType") == 0) ? getBookingFrom(table) : getCancelingFrom(table);				
				if (record != null) {
					notChecked.add(record);
				}
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in notchecked() request code");			
		}
		return notChecked;
	}

	public void removeRequest(Integer req_number) 
	{
		String query = "DELETE FROM Requests WHERE Ticket = " + String.valueOf(req_number);
		executeQuery(query);
	}

	public boolean flightHasFreeSeats(int flight_number, Date date) 
	{
		SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy");		
		
		String query = "SELECT * FROM Requests WHERE FlightNumber =  '" 
						+ flight_number +"' and FlightDate = \"" + ft.format(date) + "\"";
		
		Statement statement = null;
		ResultSet table = null;
		int count = 0;
		try {			
			statement = connection.createStatement();
			table = statement.executeQuery(query);
			while (table.next()) 
			{
				++count;
			}			
			statement.close();				
			table.close();
		} catch (SQLException e) {
			System.out.println("Problem with SQL in flightHasFreeSeats() code");			
		}
		return count <= 6;
	}
}
