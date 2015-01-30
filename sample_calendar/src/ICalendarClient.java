import java.rmi.*;

public interface ICalendarClient extends Remote {
    void notifyEvent(Event e) throws RemoteException;
    void isAlive() throws RemoteException;
}