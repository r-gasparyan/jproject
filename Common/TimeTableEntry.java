import java.io.Serializable;

/**
 * 	This class implements the entry of flights timetable.
 * 	It has 2 data members:
 * 		Flight Time
 *		Flight Direction
 */

/**
 * @author ruben
 *
 */
public class TimeTableEntry implements Serializable{
	/* Flight number */
	int _flight_number;
	
	/* Planned departure time */
	private String _flight_time;
	
	/* Direction of flight: TO_TOWN(1) or TO_CAMP(0)*/
	private int _direction; // default = 0
	
	String _air_company;
	
	public TimeTableEntry(int flight_number, String flight_time, int direction, String air_company)
	{
		_flight_number = flight_number;
		_flight_time = flight_time;
		_direction = direction;
		_air_company = air_company;
	}
	
	public String toString() 
	{
		return new String("[Flight " + getFlightNumber() + " - " + getTime() + "|" + getAirCompany() +"] ");
	}
	
	public boolean isValid() {		
		return (_air_company != null);
	}
	
	public int getFlightNumber() {
		return _flight_number;
	}
	
	public int getDirection() {
		return _direction;
	}
	
	public boolean toCamp() {
		return _direction == 0;
	}
	
	public boolean toTown() {
		return _direction == 1;
	}	
	
	public String getTime() {			
		return _flight_time;
	}
	
	public String getAirCompany() {
		return _air_company;
	}
}
