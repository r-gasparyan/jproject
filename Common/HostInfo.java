/**
 * 	This class presents host information required in order to 
 * 	connect to this host {ip, port}
 */

/**
 * @author ruben
 *
 */
public class HostInfo {
	private String name;
	private String ip;
	private int port;
	
	public HostInfo(String name, String ipAddress, int portNumber)
	{		
		this.name = name;
		this.ip = ipAddress;
		this.port = portNumber;
	}
	
	public String getName() {
		return name;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
	
	public String toString() {
		return new String(name + " <-> " + ip + ":" + String.valueOf(port));
	}
}
