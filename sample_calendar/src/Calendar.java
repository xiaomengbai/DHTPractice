import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Calendar
 */
public class Calendar extends UnicastRemoteObject implements ICalendar{

    //owner own this calendar
    private String owner;
    private CalendarManager cm;
    public ArrayList<Event> events;
    public ICalendarClient cli;

    public Lock callock;

    public void lockCalendar() throws RemoteException{
	this.callock.lock();
    }

    public void unlockCalendar() throws RemoteException{
	this.callock.unlock();
    }

    public void isAlive() throws RemoteException{
	;
    }

    public void lockCalendars(String[] users) throws UserNotExistException, RemoteException{
	int nextkey = 0;

	// sort users in order of their ids
	Map<Integer, String> map = new HashMap<Integer, String>();
	for(String user : users)
	    map.put(CalendarManager.hashName(user), user);
	TreeSet<Integer> keys = 
	    new TreeSet<Integer>(new Comparator<Integer>(){
		    public int compare(Integer a, Integer b){
			return a.intValue() - b.intValue();
		    }
		});
	keys.addAll(map.keySet());

	try{
	    // lock relevant calendar
	    Iterator<Integer> it = keys.iterator();
	    while(it.hasNext()){
		nextkey = it.next();
		getRemCalendar(map.get(nextkey)).lockCalendar();
	    }
	}catch(Exception e){
	    // lock failed
	    System.out.println("Lock failed...");
	    // unlock locked object;
	    for(Iterator<Integer> it = keys.descendingIterator(); it.hasNext(); ){
		int key = it.next();
		if(key < nextkey)
		    getRemCalendar(map.get(key)).unlockCalendar();
	    }
	    throw e;
	}
    }
		
    public void unlockCalendars(String[] users){
	// sort users in order of their ids
	Map<Integer, String> map = new HashMap<Integer, String>();
	for(String user : users)
	    map.put(CalendarManager.hashName(user), user);
	TreeSet<Integer> keys = 
	    new TreeSet<Integer>(new Comparator<Integer>() {
		    public int compare(Integer a, Integer b){
			return b.intValue() - a.intValue();
		    }
		});
	keys.addAll(map.keySet());

	for(Iterator<Integer> it = keys.iterator(); it.hasNext(); ){
	    try{
		int idx = it.next();
		getRemCalendar(map.get(/*it.next()*/idx)).unlockCalendar();
	    }catch(Exception e){
		// do nothing
	    }
	}
    }


    /* 
     * Notify User event
     */
    public void pushEvent(Event e) throws RemoteException{
	if(cli == null)
	    return;
	cli.notifyEvent(e);
	/* Only notify once */
	e.setNotified();
    }

    public String toString(){
	StringBuilder sb = new StringBuilder("Owner: "+this.owner+"\n");
	sb.append("Client: ");
	if(this.cli == null) 
	    sb.append("connected\n");
	else
	    sb.append("connected\n");
	for(Event e : events)
	    sb.append(e.toString());
	sb.append("--------------------\n");
	return sb.toString();
    }

    public Calendar(String u, CalendarManager cm) throws RemoteException{
	super();
	this.callock = new ReentrantLock();
	this.owner = u;
	this.events = new ArrayList<Event>();
	this.cm = cm;
	this.cli = null;
    }

    public Calendar(String u, CalendarManager cm, ArrayList<Event> ev) throws RemoteException{
	super();
	this.callock = new ReentrantLock();
	this.owner = u;
	this.events = ev;
	this.cm = cm;
	this.cli = null;
    }


    public String getOwner(){
	return owner;
    }

    /*
     * Find one OPEN Event can contain given Event
     */
    public Event getContainer(Event ev) throws RemoteException, EventConflictException{
	for(Event ce : this.events){
	    if(ce.getPrivilege() != Event.ACC_OPEN)
		continue;
	    if(isContain(ce.getStTime(), ce.getEdTime(), ev.getStTime(), ev.getEdTime()))
		return ce;
	}
	throw new EventConflictException("("+this.owner+")Can not find corresponding OPEN event");
    }

    static boolean isContain(Date st1, Date ed1, Date st2, Date ed2){
	return ( !st1.after(st2) && !ed1.before(ed2) );
    }

    /*
     * Find one OPEN Event as left neighbor of given Event
     */
    public Event getLNeighbour(Event e){
	for(Event ev : this.events)
	    if(ev.getPrivilege() == Event.ACC_OPEN &&
	       ev.getEdTime().compareTo(e.getStTime()) == 0)
		return ev;
	return null;
    }

    /*
     * Find one OPEN Event as right neighbor of given Event
     */
    public Event getRNeighbour(Event e){
	for(Event ev : this.events)
	    if(ev.getPrivilege() == Event.ACC_OPEN &&
	       ev.getStTime().compareTo(e.getEdTime()) == 0)
		return ev;
	return null;
    }
	    

    /*
     * Check if there is existing Event conflict with given Event
     */
    public void checkEventConflict(Event e) throws EventConflictException, RemoteException{
	for(Event ev : this.events){
	    if(e.getPrivilege() == Event.ACC_GROUP && ev.getPrivilege() != Event.ACC_GROUP)
		continue;
	    if((e.getStTime().after(ev.getStTime()) && e.getStTime().before(ev.getEdTime())) || 
	       (e.getEdTime().after(ev.getStTime()) && e.getEdTime().before(ev.getEdTime())) ||
	       (e.getStTime().before(ev.getStTime()) && e.getEdTime().after(ev.getEdTime())) ||
	       (e.getStTime().equals(ev.getStTime()) && e.getEdTime().equals(ev.getEdTime())))
		throw new EventConflictException("(" + this.owner + ") Conflict with : " + ev.toString());
	}
    }

    /*
     * Add Event
     */
    public void addEvent(Event e) throws RemoteException{
	synchronized(this.events){
	    this.events.add(e);
	    Collections.sort(this.events);
	}
    }

    /*
     * Insert Event e splitting existing Event c
     */
    public void addEvent(Event e, Event c) throws RemoteException{
	Event tail = c.duplicate();
	c.setEdTime(e.getStTime());
	tail.setStTime(e.getEdTime());
	synchronized(this.events){
	    this.events.add(e);
	}
	if(!c.getEdTime().after(c.getStTime())){
	    synchronized(this.events){
		this.events.remove(c);
	    }
	}
	if(tail.getEdTime().after(tail.getStTime())){
	    synchronized(this.events){
		this.events.add(tail);
	    }
	}
	synchronized(this.events){
	    Collections.sort(this.events);
	}
    }

    /*
     * Modify ith Event into Event me
     */
    public void modifyEvent(Event me, int i) throws RemoteException, EventConflictException, UserNotExistException{
	System.out.println(">>> Server: modify " + this.events.get(i) + " to " + me);
	Event oe = null;
	if(me.getPrivilege() != Event.ACC_GROUP){
	    synchronized(this.events){
		try{	    
		    oe = removeEvent(i);
		    scheduleEvent(me);
		}catch(Exception e){
		    if(oe != null)
			scheduleEvent(oe);
		    throw e;
		}
	    }
	}else{
	    String[] members = me.getMembers();
	    ArrayList<String> successMembers = new ArrayList<String>();
	    
	    lockCalendars(members);
	    try{
		oe = removeEvent(i);
		scheduleEvent(me);
	    }catch(Exception e){
		if(oe != null)
		    scheduleEvent(oe);
		throw e;
	    }finally{
		unlockCalendars(members);
	    }
	}
    }
	    
    /*
     * Delete Event from Event list
     */
    public boolean deleteEvent(Event ev) throws RemoteException{
	if(ev.getPrivilege() == Event.ACC_GROUP){
	    /* For GROUP Event, we need recover original OPEN Event */
	    Event le = this.getLNeighbour(ev);
	    Event re = this.getRNeighbour(ev);
	    if(le != null){
		le.setEdTime(ev.getEdTime());
		if(re != null && 
		   le.getTextDescription().compareTo(re.getTextDescription()) == 0){
		    le.setEdTime(re.getEdTime());
		    synchronized(this.events){
			this.events.remove(re);
		    }
		}
	    }else if(re != null){
		re.setStTime(ev.getStTime());
	    }else{
		Event ne = ev.duplicate();
		ne.setPrivilege(Event.ACC_OPEN);
		ne.setTextDescription(Event.OPEN_DES);
		ne.clearMembers();
		addEvent(ne);
	    }
	}
	synchronized(this.events){
	    for(Iterator<Event> it = this.events.iterator(); it.hasNext(); ){
		if(it.next().isEqual(ev)){
		    it.remove();
		    return true;
		}
	    }
	    return false;
	}
    }


    /*
     * Interface for Calendar Client removing Event
     */
    public Event removeEvent(int i) throws RemoteException, UserNotExistException{
	Event retEv = this.events.get(i);

	if(retEv.getPrivilege() != Event.ACC_GROUP){
	    deleteEvent(retEv);
	    return retEv;
	}else{
	    /* For GROUP Event, remove all copies */
	    String[] members = retEv.getMembers();
	    ArrayList<String> successMembers = new ArrayList<String>();
	    
	    lockCalendars(members);
	    try{
		for(String u : members){
		    getRemCalendar(u).deleteEvent(retEv);
		    successMembers.add(u);
		}
		return retEv;
	    }catch(Exception e){
		for(String u : successMembers){
		    ICalendar remCal = getRemCalendar(u);
		    try{
			remCal.addEvent(retEv.duplicate(), remCal.getContainer(retEv));
		    }catch(Exception de){
			remCal.addEvent(retEv.duplicate());
		    }
		}
		throw e;
	    }finally{
		unlockCalendars(members);
	    }
	}
    }


    public ArrayList<Event> getAllEvents() throws RemoteException{
	return this.events;
    }
	
    /*
     * Get other Calendar Object
     */
    private ICalendar getRemCalendar(String user) throws RemoteException, UserNotExistException{
	if(user.compareTo(this.owner) == 0)
	    return this;
	else
	    return cm.connectCalendar(null, user);
    }

    /*
     * Interface for Calendar Client retrieving valid Events
     */
    public ArrayList<Event> retrieveEvents(String user, Date st1, Date st2) throws RemoteException, UserNotExistException{
	ArrayList<Event> validEvents = new ArrayList<Event>();
	boolean checkTime = true;
	if(user.length() == 0)
	    user = this.owner;
	if(st1 == null || st2 == null)
	    checkTime = false;
	
	ArrayList<Event> remEvents = getRemCalendar(user).getAllEvents();
	for(Event ev : remEvents){
	    boolean isValidEv = true;
	    // Privilege check only for others' events
	    if(user.compareTo(this.owner) != 0){
		if(ev.getPrivilege() == Event.ACC_PRIVATE || 
		   ev.getPrivilege() == Event.ACC_GROUP && !ev.isMember(this.owner))
		    isValidEv = false;
	    }
	    // check time
	    if( checkTime && ( ev.getStTime().before(st1) || ev.getStTime().after(st2) ) )
		isValidEv = false;
	    if(isValidEv){
		validEvents.add(ev);
	    }else if(user.compareTo(this.owner) != 0 && ev.getPrivilege() == Event.ACC_GROUP){
		// user out of group members can only see time
		Event fe = ev.duplicate();
		fe.clearMembers();
		fe.clearTextDescription();
		validEvents.add(fe);
	    }
	}
	return validEvents;
    }

    /*
     * Interface for Calendar Client scheduling Event
     */
    public void scheduleEvent(Event e) throws RemoteException, EventConflictException, UserNotExistException{
	System.out.print(">>>Server: Schedule Event: "+e.toString());
	if(e.getPrivilege() == Event.ACC_GROUP){
	    // lock relevant calendars sequentially
	    String[] members = e.getMembers();
	    ICalendar remCal;
	    int idx = -1;
	    HashMap<String, Event> containers = new HashMap<String, Event>();
	    ArrayList<String> successMembers = new ArrayList<String>();
	    
	    lockCalendars(members);
	    try{
		// We have to check all users first
		for(String u : members){
		    remCal = getRemCalendar(u);
		    containers.put(u, remCal.getContainer(e));
		    remCal.checkEventConflict(e);
		}

		// Everything's OK, then do scheduling

		for(String u : members){
		    getRemCalendar(u).addEvent(e.duplicate(), containers.get(u));
		    successMembers.add(u);
		}

	    }catch(Exception se){
		for(String u : successMembers)
		    getRemCalendar(u).deleteEvent(e);
		throw se;
	    }finally{
		unlockCalendars(members);
	    }
	}else{
	    synchronized(this.events){
		checkEventConflict(e);
		addEvent(e);
	    }
	}
    }
}
