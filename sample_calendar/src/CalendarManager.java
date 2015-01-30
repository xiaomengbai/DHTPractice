import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/*
 * Calendar Manager
 */

public class CalendarManager extends UnicastRemoteObject implements ICalendarManager, Runnable{

    public static final String ServName = "CalendarManagerServiceXMB";
    public static final int Port = 1098;
    public static final String DEF_PATH = "CalendarData.dump";
    // public static final String[] usernames = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
    // 					      "m", "n", "o", "p", "q", "r", "s"};

    public ArrayList<Calendar> Calendars;
    public ChordNode chord;

    public void isAlive() throws RemoteException{
	;
    }

    public int getId() throws RemoteException{
	return this.chord.getId();
    }

    public CalendarManager(ChordNode cn) throws RemoteException{
	super();
	this.Calendars = new ArrayList<Calendar>();
	this.chord = cn;

	try{
	    ICalendarManager succ = getCalendarManager(this.chord.getSuccAddr());
	    succ.notifySucc(this);
	}catch(Exception e){
	    e.printStackTrace();
	}

	Thread t = new Thread(this);
	t.start();
    }

    public void run(){
	while(true){
	    try{
		Thread.sleep(15000);
		synchronized(Calendars){
		    for(Iterator<Calendar> it = Calendars.iterator(); it.hasNext();){
			Calendar cal = it.next();
			try{
			    IChordNode node = this.chord.findSucc(hashName(cal.getOwner()));
			    if(node.getId() == this.getId()){
				// propagate
				try{
				    if(this.chord.getSuccId() != this.getId()){
					ICalendarManager cm = getCalendarManager(this.chord.getSuccAddr());
					cm.propagateCalendar(cal.getOwner(), cal.getAllEvents());
				    }
				}catch(Exception e){
				    //wait next time
				    System.out.println("fail...");
				    System.out.println(e.toString());
				    e.printStackTrace();
				}
			    }else if(node.getSuccId() != this.getId()){
				//remove calendar
				System.out.println(">>>Server: Remove invalid copy of " + cal.getOwner());
				it.remove();
			    }
			}catch(RemoteException e){
			    //cant find successor...
			    System.out.println("Finger Table Error...");
			    System.out.println("Wait until next time");
			}
		    }
		}
	    }catch(InterruptedException e){
		e.printStackTrace();
	    }
	}
    }

    public static int hashName(String name){
	return name.hashCode() % 19;
	// int id = 0;
	// for(; id < CalendarManager.usernames.length; id++){
	//     if(name.compareTo(CalendarManager.usernames[id]) == 0)
	// 	return id;
	// }
	// return -1;
    }

    public static String getServName(String addr){
	return "rmi://" + addr + ":" + CalendarManager.Port + "/" + CalendarManager.ServName;
    }
    
    public ICalendarManager getCalendarManager(String addr){
	if(this.chord.selfip.compareTo(addr) == 0)
	    return this;

	try{
	    return (ICalendarManager)Naming.lookup(getServName(addr));
	}catch(Exception e){
	    e.printStackTrace();
	    return null;
	}
    }


    /*
     * Get Calendar by user name
     */
    public Calendar getCalendar(String user) throws UserNotExistException{
	for(Calendar cal : Calendars){
	    if(cal.getOwner().compareTo(user) == 0)
		return cal;
	}
	throw new UserNotExistException(user + " doesn't exist!");
    }

    /*
     * Remove Calendar 
     */
    public void removeCalendar(String user) throws RemoteException, UserNotExistException{
	System.out.println(">>>Server: Remove calendar from "+ user);
	ICalendarManager IremCalendarManager;

	IremCalendarManager = getCalendarManager(this.chord.findSucc(hashName(user)).getAddr());
	if(IremCalendarManager.getId() != this.getId())
	    IremCalendarManager.removeCalendar(user);
	else{
	    synchronized(Calendars){
		Calendars.remove(getCalendar(user));
	    }
	}
	
    }

    /*
     * Abandon Corresponding Client stub
     */
    public void disconnectCalendar(String user) throws RemoteException, UserNotExistException{
	System.out.println(">>>Server: Disconnect calendar from "+ user);
	ICalendarManager IremCalendarManager;

	IremCalendarManager = getCalendarManager(this.chord.findSucc(hashName(user)).getAddr());
	if(IremCalendarManager.getId() != this.getId())
	    IremCalendarManager.disconnectCalendar(user);
	else{
	    Calendar cal = getCalendar(user);
	    cal.cli = null;
	}
    }

    /*
     * Interface for Calendar Client Creating Calendar
     */
    public ICalendar createCalendar(ICalendarClient cli, String user) throws RemoteException, UserExistException {
	System.out.println(">>>Server: Create calendar for "+ user);
	ICalendarManager IremCalendarManager;

	IremCalendarManager = getCalendarManager(this.chord.findSucc(hashName(user)).getAddr());
	if(IremCalendarManager.getId() != this.getId())
	    return IremCalendarManager.createCalendar(cli, user);
	else{
	    for(Calendar cal : Calendars)
		if(cal.getOwner().compareTo(user) == 0)
		    throw new UserExistException();
	
	    Calendar NewCal = new Calendar(user, this); 
	    NewCal.cli = cli;
	    synchronized(Calendars){
		Calendars.add(NewCal);
	    }
	    return NewCal;
	}
    }

    /*
     * Interface for Calendar Manager propagating secondary copy
     */
    public void propagateCalendar(String user, ArrayList<Event> ev) throws RemoteException{
	System.out.println(">>>Server: Propagate calendar of " + user + " from predecessor");
	synchronized(Calendars){
	    for(Iterator<Calendar> it = Calendars.iterator(); it.hasNext();)
		if(it.next().getOwner().compareTo(user) == 0)
		    it.remove();
	}

	Calendar NewCal = new Calendar(user, this, ev); 

	synchronized(Calendars){
	    Calendars.add(NewCal);
	}
    }

    /*
     * Interface for Calendar Client doing connection
     */
    public ICalendar connectCalendar(ICalendarClient cli, String user) throws RemoteException, UserNotExistException{
	System.out.println(">>>Server: Connect calendar for "+ user);
	ICalendarManager IremCalendarManager;

	IremCalendarManager = getCalendarManager(this.chord.findSucc(hashName(user)).getAddr());
	if(IremCalendarManager.getId() != this.getId())
	    return IremCalendarManager.connectCalendar(cli, user);
	else{
	    Calendar cal = getCalendar(user);
	    cal.cli = cli;
	    return cal;
	}
    }

    /*
     * Show state on server side
     */
    public void showCalendarManagerServ() throws RemoteException{
	System.out.println(this.toString());
    }

    public void notifySucc(ICalendarManager cm) throws RemoteException{
	System.out.println(">>>Server: Notification from predecessor ");

	if(cm.getId() == this.getId())
	    return;

	final ICalendarManager icm = cm;
	new Thread(){
	    public void run(){
		try{
		    for(Calendar cal : Calendars)
			if(hashName(cal.getOwner()) <= icm.getId())
			    icm.propagateCalendar(cal.getOwner(), cal.getAllEvents());
		}catch(Exception e){
		    e.printStackTrace();
		}
	    }
	}.start();
    }

    /*
     * Interface for Calendar Client getting users list
     */
    public ArrayList<String> getUsers() throws RemoteException{
	ArrayList<String> users = new ArrayList<String>();
	for(Calendar cal : Calendars)
	    users.add(cal.getOwner());
	return users;
    }

    public String toString(){
	StringBuilder sb = new StringBuilder(">>>   Calendar Manager Status   <<<\n");
	for(Calendar cal : Calendars)
	    sb.append(cal.toString());
	return sb.toString();
    }

    public void dump() throws RemoteException{
	try{
	    String path = new String("./" + this.chord.getId() + "/");
	    File dir = new File(path);
	    if(!dir.exists())
		dir.mkdirs();
	    dumpSelf(path + CalendarManager.DEF_PATH);
	}catch(IOException e){
	    System.out.println(">>> Dump Failed:");
	    e.printStackTrace();
	}
    }

    /*
     * Dump all data to FILE
     */
    public void dumpSelf(String path) throws IOException{
	System.out.println(">>>Server: Dump to " + path);
	ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
	out.writeObject(this);
	out.writeInt(this.Calendars.size());
	for(Calendar cal : Calendars){
	    out.writeObject(cal);
	    out.writeInt(cal.events.size());
	    for(Event ev : cal.events)
		out.writeObject(ev);
	}
	out.close();
    }

    /*
     * Recover data from FILE
     */
    public static CalendarManager retrieveSelf(String path, ChordNode cn) throws IOException, ClassNotFoundException{
	String genpath = "./" + cn.getId() + "/" + path;
	System.out.println(">>>Server: Retrieve from "+genpath);

	ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(genpath)));
	CalendarManager cm = (CalendarManager)in.readObject();
	cm.chord = cn;
	cm.Calendars = new ArrayList<Calendar>();
	for(int i = in.readInt(); i > 0; i--){
	    Calendar cal = (Calendar)in.readObject();
	    cal.cli = null;
	    cm.Calendars.add(cal);
	    cal.events = new ArrayList<Event>();
	    for(int j = in.readInt(); j > 0; j--){
		Event ev = (Event)in.readObject();
		cal.events.add(ev);
	    }
	}
	
	try{
	    ICalendarManager succ = cm.getCalendarManager(cm.chord.getSuccAddr());
	    succ.notifySucc(cm);
	}catch(Exception e){
	    e.printStackTrace();
	}

	return cm;
    }
}
