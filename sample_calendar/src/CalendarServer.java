import java.rmi.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.net.*;

/*
 * Calendar Server
 */
public class CalendarServer{

    public static CalendarManager cm;
    public static ChordNode node;

    public static void main(String argv[]){
	try{
	    // System.setSecurityManager(new RMISecurityManager());
	    System.out.println(">>>Server: Registering Calendar Service");

	    //	    ChordNode.getHostAddress();
	    try{
		node = new ChordNode();
	    }catch(Exception e){
		e.printStackTrace();
		System.exit(1);
	    }

	    try{
		cm = CalendarManager.retrieveSelf(CalendarManager.DEF_PATH, node);
		Thread t = new Thread(cm);
		t.start();
	    }catch(Exception e){
		System.out.println(">>>Server: Retrieve from file failed");
		cm = new CalendarManager(node);
	    }
	    Naming.rebind(CalendarManager.getServName(cm.chord.getAddr()), cm);
	    System.out.println(">>>Server: Ready...");

	    /* Timing Task in 30 sec */
	    Timer timer = new Timer();
	    timer.scheduleAtFixedRate(new TimerTask(){
		    public void run(){
			// Inform user at least 30 minutes earlier
			Date now = new Date(System.currentTimeMillis() - 30 * 60 * 1000);
			for(Calendar cal : cm.Calendars){
			    try{
				if(cal.cli != null)
				    cal.cli.isAlive();
			    }catch(RemoteException e){
				cal.cli = null;
				continue;
			    }
			    for(Event ev : cal.events)
				if(ev.getStTime().before(now) && !ev.isNotified()){
				    try{
					cal.pushEvent(ev);
				    }catch(RemoteException e){
					e.printStackTrace();
				    }
				}
			}
			try{
			    // Dump to File 
			    cm.dump();
			}catch(Exception e){
			    e.printStackTrace();
			}
		    }
		}, 10 * 1000, 30 * 1000);
	}catch(Exception e){
	    System.out.println(">>>Server: Failed to register Calendar Service:" + e);
	}
    }
}
