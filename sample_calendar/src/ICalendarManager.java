import java.rmi.*;
import java.util.ArrayList;
import java.util.Date;

public interface ICalendarManager extends Remote {
    void isAlive() throws RemoteException;
    int getId() throws RemoteException;
    void propagateCalendar(String user, ArrayList<Event> ev) throws RemoteException;
    void notifySucc(ICalendarManager cm) throws RemoteException;
    void removeCalendar(String user) throws RemoteException, UserNotExistException;
    void disconnectCalendar(String user) throws RemoteException, UserNotExistException;
    ICalendar createCalendar(ICalendarClient cli, String user) throws RemoteException, UserExistException;
    ICalendar connectCalendar(ICalendarClient cli, String user) throws RemoteException, UserNotExistException;
    ArrayList<String> getUsers() throws RemoteException;
    void showCalendarManagerServ() throws RemoteException;
    void dump() throws RemoteException;
}