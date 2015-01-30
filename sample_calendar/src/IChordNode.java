import java.rmi.*;
// import java.util.ArrayList;
// import java.util.Date;

public interface IChordNode extends Remote {
    void printMsg(String msg) throws RemoteException;
    int getId() throws RemoteException;
    int getSuccId() throws RemoteException;
    String getSuccAddr() throws RemoteException;
    int getPredId() throws RemoteException;
    String getAddr() throws RemoteException;
    String getPredAddr() throws RemoteException;
    void setPred(int id, String ip) throws RemoteException;
    IChordNode getSuccessor() throws RemoteException;
    IChordNode getSuccessor(int m) throws RemoteException;
    IChordNode findClosestPred(int id) throws RemoteException;
    IChordNode findPred(int id) throws RemoteException;
    IChordNode findSucc(int id) throws RemoteException;
    //    void updateFingerTable(int id, String ip, int i) throws RemoteException;
    void notify(IChordNode n) throws RemoteException;
    void isAlive() throws RemoteException;
    void printFingerTable() throws RemoteException;
}