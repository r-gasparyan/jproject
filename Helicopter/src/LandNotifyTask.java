import java.util.TimerTask;

/**
 * This class will be used for implementing timeout notification 
 * when helicopter lands.
 */

/**
 * @author ruben
 *
 */
public class LandNotifyTask extends TimerTask {
	private HelicopterDevice _helicopterDevice = null;	
	
	public LandNotifyTask(HelicopterDevice helicopterDevice)
	{
		_helicopterDevice = helicopterDevice;
	}
	
	public void run()
	{
		_helicopterDevice.landed();
	}
}
