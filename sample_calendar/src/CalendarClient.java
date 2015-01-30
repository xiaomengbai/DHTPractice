import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.io.*;

/*
 * Calendar Client Class
 */
public class CalendarClient extends UnicastRemoteObject implements ICalendarClient, Runnable{
    
    public String user;
    public String cmip;
    private ICalendarManager IremCalendarManager;
    private ICalendar IremCalendar;
    static final String NoCalendarEx = "Connect to Calendar first";
    static final String NoCalendarManagerEx = "Connect to CalendarManager first";


    public CalendarClient(String u) throws RemoteException{
	this.user = u;
	this.IremCalendarManager = null;
	this.IremCalendar = null;
	this.cmip = null;

	Thread t = new Thread(this);
	t.start();
    }

    public void run(){
	while(true){
	    try{
		Thread.sleep(5000);
		try{
		    if(this.IremCalendarManager != null)
			this.IremCalendarManager.isAlive();
		}catch(Exception e){
		    System.out.println("CalendarManager down...");
		    this.IremCalendarManager = null;
		}
		try{
		    if(this.IremCalendar != null)
			this.IremCalendar.isAlive();
		}catch(Exception e){
		    System.out.println("Calendar down...");
		    this.IremCalendar = null;
		    //wait until chord stable
		    Thread.sleep(6000);
		    connectCalendar();
		}
	    }catch(Exception e){
		e.printStackTrace();
		this.IremCalendarManager = null;
		this.IremCalendar = null;
		continue;
	    }
	}
    }



    public Event getEvent(int type, String[] users, Date st, Date ed, String des){
	// Unify the description of OPEN event
	if(type == Event.ACC_OPEN)
	    des = Event.OPEN_DES;
	if(type != Event.ACC_GROUP){
	    return new Event(st, ed, des, type);
	}else{
	    return new Event(st, ed, des, type, users);
	}
    }

    public String showCalendarStatus(){
	return IremCalendar == null ? "Disconnected" : "Connected";
    }

    public String showCalendarManagerStatus(){
	return IremCalendarManager == null ? "Disconnected" : "Connected";
    }

    /*
     * Show CalendarManger Status on Serve Side 
     * Only for TEST
     */
    public void showCalendarManagerServ() throws RemoteException, NoConnectionException{
	if(this.IremCalendarManager == null)
	    throw new NoConnectionException(NoCalendarManagerEx);

	this.IremCalendarManager.showCalendarManagerServ();
    }

    /* 
     * Dump the CalendarManager immediately
     * Only for TEST
     */
    public void dump() throws RemoteException, NoConnectionException{
	if(this.IremCalendarManager == null)
	    throw new NoConnectionException(NoCalendarManagerEx);

	this.IremCalendarManager.dump();
    }

    /*
     * Get list of Users who hold own Calendars
     */
    public ArrayList<String> getUsers() throws RemoteException, NoConnectionException{
	if(this.IremCalendarManager == null)
	    throw new NoConnectionException(NoCalendarManagerEx);

	return this.IremCalendarManager.getUsers();
    }

    /*
     * Retrieve valid Events list
     */
    public ArrayList<Event> retrieveEvents(String user, Date st1, Date st2) throws RemoteException, UserNotExistException, NoConnectionException{
	if(this.IremCalendar == null)
	    throw new NoConnectionException(NoCalendarEx);

	return this.IremCalendar.retrieveEvents(user, st1, st2);
    }

    /*
     * Schedule Event
     */
    public void scheduleEvent(Event e) throws RemoteException, EventConflictException, UserNotExistException, NoConnectionException{
	if(this.IremCalendar == null)
	    throw new NoConnectionException(NoCalendarEx);

	this.IremCalendar.scheduleEvent(e);
    }

    /*
     * Remove Event
     */
    public Event removeEvent(int i) throws RemoteException, NoConnectionException, UserNotExistException{
	if(this.IremCalendar == null)
	    throw new NoConnectionException(NoCalendarEx);

	return this.IremCalendar.removeEvent(i);
    }

    /*
     * Modify Event
     */
    public void modifyEvent(Event e, int i) throws RemoteException, EventConflictException, NoConnectionException, UserNotExistException{
	if(this.IremCalendar == null)
	    throw new NoConnectionException(NoCalendarEx);

	this.IremCalendar.modifyEvent(e, i);
    }

    /*
     * Connect to CalendarManager
     */
    public void connectCalendarManager(String address){
	String servName = "rmi://" + address + ":" + CalendarManager.Port +"/" + CalendarManager.ServName;
	if(this.IremCalendarManager != null)
	    return;
	
	System.out.println(">>> Client: Looking up " + servName + "...");
	try{
	    this.IremCalendarManager = (ICalendarManager)Naming.lookup(servName);
	}catch(Exception e){
	    e.printStackTrace();
	}
    }

    /*
     * Connect to corresponding Calendar
     */
    public void connectCalendar() throws NoConnectionException, RemoteException, UserNotExistException{
	System.out.println("connect to Calendar...");
	if(this.IremCalendarManager == null)
	    throw new NoConnectionException(NoCalendarManagerEx);
	if(this.IremCalendar != null)
	    return;

	this.IremCalendar = (ICalendar)this.IremCalendarManager.connectCalendar(this, this.user);
	System.out.println("done!");
    }

    /*
     * Create corresponding Calendar
     */
    public void createCalendar() throws NoConnectionException, RemoteException, UserExistException{
	if(this.IremCalendarManager == null)
	    throw new NoConnectionException(NoCalendarManagerEx);
	if(this.IremCalendar != null)
	    return;

	this.IremCalendar = (ICalendar)this.IremCalendarManager.createCalendar(this, this.user);
    }

    /*
     * Remove Corresponding Calendar
     * Only for TEST
     */
    public void removeCalendar() throws NoConnectionException, RemoteException, UserNotExistException{
	if(this.IremCalendarManager == null)
	    throw new NoConnectionException(NoCalendarManagerEx);

	this.IremCalendarManager.removeCalendar(this.user);
	this.IremCalendar = null;
    }

    /*
     * Disconnect from Calendar
     */
    public void disconnectCalendar() throws NoConnectionException, RemoteException, UserNotExistException{
	if(this.IremCalendarManager == null)
	    throw new NoConnectionException(NoCalendarManagerEx);

	this.IremCalendarManager.disconnectCalendar(this.user);
	this.IremCalendar = null;
    }

    /*
     * Interface for CalendarServer notify Events which is about
     * to expire
     */
    public void notifyEvent(Event e) throws RemoteException{
	System.out.print(">>>Notification:"+e.toString());
    }
    
    /*
     * Interface for CalendarServer detect if corresponding client
     * is alive
     */
    public void isAlive() throws RemoteException{
	;
    }
}