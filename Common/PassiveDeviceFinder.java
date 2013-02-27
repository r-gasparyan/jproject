import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.LinkedList;
import java.util.ListIterator;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 *  This class implements the finding other devices mechanism.
 *  It starts, connects to the network and performs finding switched-on devices
 *  using JmDNS library without registering own service
 *  
 * 	@author ruben
 */
public class PassiveDeviceFinder {
	
	// jmdns class instance
	private JmDNS _jmdns = null;	

	// own host information
	private HostInfo _myself = null;
	
	// current service type name (e.g. "_camp._tcp.local.")
	private String _serviceType = null;
	
	// own service name
	private String _deviceName = null;
	
	// list of all devices
	private LinkedList<HostInfo> _devices = null;	

	/* Creating JmDNS instance and registering the local service */
	public PassiveDeviceFinder(String serviceType, String deviceName, int port)
	{		
		_serviceType = serviceType;
		_deviceName = deviceName;		
		_devices = new LinkedList<HostInfo>();
		
		try {
			_myself = new HostInfo(_deviceName, InetAddress.getLocalHost().getHostAddress(), port);
		} catch (UnknownHostException e) {
			_myself = new HostInfo(_deviceName, "127.0.0.1", port);
		}
		
		try {		
			InetAddress Address = InetAddress.getLocalHost();
			System.out.println("Current device: " + _myself.getIp() + ":" + _myself.getPort());			
			_jmdns = JmDNS.create(Address);
			System.out.println("JmDNS instance created.");

			_jmdns.addServiceListener(_serviceType, new SampleListener(this));			                        
		} catch (IOException e) {			
			e.printStackTrace();
		}	
		
		refreshDeviceList();
	}	
	
	private void refreshDeviceList()
	{
		_devices.clear();
		
		LinkedList<HostInfo> devices = jmdnsRequestDevices();
		ListIterator<HostInfo> li = devices.listIterator();
        while (li.hasNext()) {
        	HostInfo info = li.next();        	
        	saveHostInfo(info.getName(), info.getIp(), info.getPort());
        }      
	}

	/* Here we find all network devices of type "CAMP" including _myself
	 * and add them to list "Devices"
	 */
	private synchronized LinkedList<HostInfo> jmdnsRequestDevices()
	{		
		return jmdnsRequestDevices(_serviceType);		
	}	
	
	public synchronized LinkedList<HostInfo> jmdnsRequestDevices(String type)
	{
		LinkedList<HostInfo> devices = new LinkedList<HostInfo>();
		ServiceInfo[] serviceInfos = _jmdns.list(type);			
		for (ServiceInfo info : serviceInfos) 
		{			
			HostInfo device = new HostInfo(info.getName(), info.getInet4Addresses()[0].getHostAddress(), info.getPort());	
			devices.add(device);
		}		
		return devices;
	}

	/**
	 *  Closes down jmdns instance and unregisters all services
	 */
	public synchronized void closeJmDNS() {
		_jmdns.unregisterAllServices();
		System.out.print("Wait for JmDNS instance to be released... ");
		try {
			_jmdns.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		System.out.println("JmDNS down.");
	}
	
	/** 
	 * Here we return all network devices saved for the moment as CAMP devices
	 */
	public synchronized LinkedList<HostInfo> getDevices()
	{
		return _devices;
	}
	
	/** 
	 * Here we save CAMP device if it has not been saved yet 
	 * and if it is not _myself
	 */
	private synchronized void saveHostInfo(String name, String ip, int port)
	{
		// do not save myself into CAMP devices
		if (ip.equals(_myself.getIp()) && port == _myself.getPort()) {
			return;
		}
		
		// check if such info has been saved before
		ListIterator<HostInfo> li = _devices.listIterator();
        while (li.hasNext()) {
        	HostInfo info = li.next();
        	
        	// if this host has been saved - do not save it
        	if (ip.equals(info.getIp()) && info.getPort() == port) {        		
        		return;
        	}
        }
        
        // finally, save host info
        _devices.add(new HostInfo(name, ip, port));
	}
	
	/**
	 *  Here we remove info about CAMP device from our list
	 */
	private synchronized void forgetHostInfo(String ip, int port)
	{		
		ListIterator<HostInfo> li = _devices.listIterator();
        while (li.hasNext()) {
        	HostInfo info = li.next();        	
        	if (ip.equals(info.getIp()) && info.getPort() == port) { 
        		_devices.remove(li.nextIndex() - 1);
        		return;
        	}        	
        }
	}

	/*
	 * 	Sample listener class needs to provide callback functions
	 * 	to notify about new devices appeared in the network 
	 */
	static class SampleListener implements ServiceListener {		
		public void serviceAdded(ServiceEvent event) {
			String name = event.getInfo().getName();
			String ip = event.getInfo().getInet4Addresses()[0].getHostAddress();
			int port = event.getInfo().getPort(); 
			System.out.println("Service added: " + ip);
			if (this.finder != null) {
				this.finder.saveHostInfo(name, ip, port);
			}
		}
		public void serviceRemoved(ServiceEvent event) {			
			String ip = event.getInfo().getInet4Addresses()[0].getHostAddress();
			int port = event.getInfo().getPort();
			System.out.println("Service removed: " + ip);
			if (this.finder != null) {
				this.finder.forgetHostInfo(ip, port);				
			}
		}
		public void serviceResolved(ServiceEvent event) {        	
			String name = event.getInfo().getName();
			String ip = event.getInfo().getInet4Addresses()[0].getHostAddress();
			int port = event.getInfo().getPort();
			System.out.println("Service resolved: " + ip);
			if (this.finder != null) {
				this.finder.saveHostInfo(name, ip, port);
			}
		}
		
		PassiveDeviceFinder finder = null;
		public SampleListener (PassiveDeviceFinder finder) {
			this.finder = finder;
		}
	}	
}
