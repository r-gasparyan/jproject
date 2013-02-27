import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.Serializable;

/**
 * 	This class is for containing request record items.
 *  Fields operated by clients:
 *  	PassengerName
 *  	FlightTime
 *  	Direction
 *  	RequestType
 *  	TicketTime
 *  	BookingCancelingFlag
 *  
 *  Fields operated by system:
 *  	Ticket
 *  	Confirmed
 */

/**
 * @author ruben
 *
 */
public class RequestRecord implements Serializable {
	//------ client operated information --------
	/* Name of the passenger for statistical (or other) purpose */
	private String _passenger_name;	
	
	/* Flight number from timetable */
	private int _flight_number;
	
	/* Only date of flight (time is being kept in timetable by flight number) */
	private Date _flight_date;
	
	/* Direction of flight: TO_TOWN(1) or TO_CAMP(0) */
	private int _direction; // default = 0
	
	/* Type of request: REQ_BOOKING(0) or REQ_CANCELING(1) */
	private int _request_type; // default = 0
	
	/* If the type of ticket is
	 * SPECIFIED_FLIGHT(0) - request will be placed for that flight and could be declined if there is no places
	 * ON_DEMAND(1)        - request will be fulfilled for nearest possible flight just after FlightTime
	 */	
	private int _ticket_type;
	
	//------ system operated information --------
	/* Unique request number within booking system */
	private int _ticket;
	
	/* Keeps confirmation status of request. Can be changed only by air company */
	/* Initially this request is not confirmed, it requires confirmation from air company */
	private boolean _confirmed = false;	
	
	/* Whether or not air company checked this request */
	private boolean _checked = false;
	
	/* for booking requests construction */
	public RequestRecord(String passenger, int flight_number, Date expected_flight_date, 
			int direction, int request_type, int ticket_type)
	{
		_passenger_name = passenger;
		_flight_number = flight_number;
		_flight_date = expected_flight_date;
		_direction = direction;
		_request_type = request_type;
		_ticket_type = ticket_type;		
		
		/* Generating pseudo-unique ticket */
		_ticket = (int) (System.nanoTime() % java.lang.Integer.MAX_VALUE);		
	}	
	
	/* for canceling requests construction */
	public RequestRecord(String passenger, int request_type)
	{
		_passenger_name = passenger;
		_flight_number = 0;
		_flight_date = null;
		_direction = 0;
		_request_type = request_type;
		_ticket_type = 0;	
		
		/* Generating pseudo-unique ticket */
		_ticket = (int) (System.nanoTime() % java.lang.Integer.MAX_VALUE);	
	}
	
	public String toString()
	{
		String req_type = (_request_type == 0) ? "BOOK": "CANCEL";
		String confirmed = (_confirmed) ? "ACK" : "N_ACK";
		String checked = (_checked) ? "CHECK" : "N_CHECK";
		
		if (_request_type == 0) {
			// booking record
			SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yy");
			String dir = (_direction == 0) ? "TO_CAMP" : "TO_TOWN";	 
			return (String.valueOf(_ticket) + " "
					+ _passenger_name + "\t "
					+ req_type + " "
					+ "Flight: " + _flight_number + " "
					+ ft.format(_flight_date) + " " 
					+ dir + " " 
					+ confirmed + " " 
					+ checked);
		}
		else {
			// canceling record
			return (String.valueOf(_ticket) + "\t  "
					+ req_type + " for ticket " 					
					+ _passenger_name + "\t "
					+ confirmed + " "
					+ checked);			
		}	       
	}
	
	/**
	 * This function will change ticket field in RequestRecord
	 * Warning! Should be called only from database code while 
	 * requesting data
	 * @param ticket - ticket number
	 */
	public void setTicket(int ticket)
	{
		this._ticket = ticket;
	}
	
	/**
	 * This function makes the request confirmed. Should be used 
	 * only in database code while requesting data
	 */
	public void confirm()
	{
		this._confirmed = true;
	}
	
	public void markAsChecked() {
		_checked = true;
	}
	
	public boolean isChecked() {
		return _checked;
	}
	
	public String getPassengerName() {
		return _passenger_name;
	}

	public int getFlightNumber() {
		return _flight_number;
	}

	public Date getFlightDate() {
		return _flight_date;
	}	

	public int getDirection() {
		return _direction;
	}

	public int getRequestType() {
		return _request_type;
	}

	public int getTicketType() {
		return _ticket_type;
	}

	public int getTicket() {
		return _ticket;
	}

	public boolean isConfirmed() {		
		return _confirmed;				
	}	
}
