import java.rmi.*;
import java.util.ArrayList;
import java.util.Date;

public interface ICalendar extends Remote {
    ArrayList<Event> getAllEvents() throws RemoteException;
    void isAlive() throws RemoteException;
    void lockCalendar() throws RemoteException;
    void unlockCalendar() throws RemoteException;
    boolean deleteEvent(Event ev) throws RemoteException;
    void addEvent(Event e) throws RemoteException;
    void addEvent(Event e, Event c) throws RemoteException;
    void checkEventConflict(Event e) throws RemoteException, EventConflictException;
    Event getContainer(Event ev) throws RemoteException, EventConflictException;
    ArrayList<Event> retrieveEvents(String user, Date stTime1, Date stTime2) throws RemoteException, UserNotExistException;
    void scheduleEvent(Event e) throws RemoteException, EventConflictException, UserNotExistException;
    Event removeEvent(int i) throws RemoteException, UserNotExistException;
    void modifyEvent(Event me, int i) throws RemoteException, EventConflictException, UserNotExistException;
}