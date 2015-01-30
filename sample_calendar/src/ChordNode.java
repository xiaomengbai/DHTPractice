import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.io.Serializable;

public class ChordNode extends UnicastRemoteObject implements IChordNode, Runnable{

    // 64 nodes
    public static final int FINGER_SIZE = 5;
    public static final String[] serverName = {"medusa-node1.vsnet.gmu.edu", "medusa-node2.vsnet.gmu.edu", "medusa-node3.vsnet.gmu.edu",
					       "medusa-node4.vsnet.gmu.edu", "medusa-node5.vsnet.gmu.edu", "medusa-node6.vsnet.gmu.edu",
					       "medusa-node7.vsnet.gmu.edu", "medusa-node8.vsnet.gmu.edu", "medusa-node9.vsnet.gmu.edu",
					       "medusa-node10.vsnet.gmu.edu", "medusa-node11.vsnet.gmu.edu", "medusa-node12.vsnet.gmu.edu",
					       "medusa-node13.vsnet.gmu.edu", "medusa-node14.vsnet.gmu.edu", "medusa-node15.vsnet.gmu.edu",
					       "medusa-node16.vsnet.gmu.edu", "medusa-node17.vsnet.gmu.edu", "medusa-node18.vsnet.gmu.edu",
					       "medusa-node19.vsnet.gmu.edu"};

    public static final String[] serverAddr = {"10.1.255.235", "10.1.255.236", "10.1.255.237", "10.1.255.238", "10.1.255.239", 
					       "10.1.255.240", "10.1.255.241", "10.1.255.242", "10.1.255.243", "10.1.255.244", 
					       "10.1.255.245", "10.1.255.246", "10.1.255.247", "10.1.255.248", "10.1.255.249", 
					       "10.1.255.250", "10.1.255.251", "10.1.255.252", "10.1.255.253"};
    public static final String ServName = "ChordNode";
    public static final int Port = 1098;
    


    public int selfid;
    public String selfip;
    public FingerTable ft;

    static public void debug(IChordNode icn, String s) throws RemoteException{
	System.out.println("["+icn.getId()+"]["+icn.getAddr()+"]: " + s);
    }

    /*
     * modular arithmetic operation
     */
    static public boolean inRange(int id, int st, int ed){
	if(st < ed)
	    return (id > st && id < ed);
	else
	    return (id > st || id < ed);
    }

    static public boolean inRangeeQ(int id, int st, int ed){
	if(st < ed)
	    return (id > st && id <= ed);
	else
	    return (id > st || id <= ed);
    }

    static public boolean inRangeEq(int id, int st, int ed){
	if(st < ed)
	    return (id >= st && id < ed);
	else
	    return (id >= st || id < ed);
    }

    static public int pushBack(int id, int i){
	if(id >= i)
	    return id - i;
	else
	    return id + (int)Math.pow(2, ChordNode.FINGER_SIZE) - i;
    }

    /*
     * get ID of this node
     */
    public int getId() throws RemoteException{
	return this.selfid;
    }

    /*
     * get IP address of this node
     */
    public String getAddr() throws RemoteException{
	return this.selfip;
    }

    /*
     * get successor ID of this node
     */
    public int getSuccId() throws RemoteException{
	return this.ft.finger[0];
    }

    /*
     * get successor IP address of this node
     */
    public String getSuccAddr() throws RemoteException{
	return this.ft.ip[0];
    }

    /*
     * get predecessor ID of this node
     */
    public int getPredId() throws RemoteException{
	return this.ft.predecessor;
    }

    /*
     * get predecessor IP address of this node
     */
    public String getPredAddr() throws RemoteException{
	return this.ft.predip;
    }

    /*
     * set predecessor of this node
     */
    public void setPred(int id, String ip) throws RemoteException{
	this.ft.setPred(id, ip);
    }
    

    /*
     * stablization thread
     */
    public void run(){
	while(true){
	    try{
		Thread.sleep(5000);
		// fix successor link if it fails
		validSucc();
		// fix successor link if it has changed
		stablize();
		// fix other finger table entries
		fixFingers();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}
    }

    /*
     * Fix successor if it has changed
     */
    public void stablize() throws RemoteException{
	IChordNode node = this.getSuccessor();
	int succpredid = node.getPredId();
	if(succpredid != this.selfid){
	    if(inRange(succpredid, this.selfid, this.ft.finger[0])){
		this.ft.setFinger(0, node.getPredId(), node.getPredAddr());
		node = this.getSuccessor();
	    }
	    node.notify(this);
	}
	node = node.getSuccessor();
	this.ft.setSuccsucc(node.getId(), node.getAddr());
    }

    /*
     * Notify successor to fix its predecessor
     */
    public void notify(IChordNode n) throws RemoteException{
	if(this.ft.predecessor == -1 || inRange(n.getId(), this.ft.predecessor, this.selfid))
	    this.ft.setPred(n.getId(), n.getAddr());
    }

    /*
     * Fix other finger table entries
     */
    public void fixFingers() throws RemoteException{
	for(int i = 1; i < ChordNode.FINGER_SIZE; i++){
	    IChordNode node = this.findSucc(this.ft.start[i]);
	    this.ft.setFinger(i, node.getId(), node.getAddr());
	}
    }

    /*
     * Join the chord ring
     */
    public void join(IChordNode an) throws RemoteException{
	this.ft.setPred(-1, null);
	IChordNode node = an.findSucc(this.ft.start[0]);
	this.ft.setFinger(0, node.getId(), node.getAddr());
	node = node.findSucc(node.getId());
	this.ft.setSuccsucc(node.getId(), node.getAddr());
    }

    /*
     * Find the closest predecessor of specified node in finger table
     */
    public IChordNode findClosestPred(int id) throws RemoteException{
    	for(int i = ChordNode.FINGER_SIZE - 1; i >= 0 ; i--){
    	    if( inRange(this.ft.finger[i], this.selfid, id) )
		return this.getSuccessor(i);
	}
	return this;
    }
    
    /*
     * Find the predecessor of specified node
     */
    public IChordNode findPred(int id) throws RemoteException{
	IChordNode Icand = this;
	while(!inRangeeQ(id, Icand.getId(), Icand.getSuccId())){
	    Icand = Icand.findClosestPred(id);
	}
	return Icand;
    }

    /*
     * find the sucessor of specified node
     */
    public IChordNode findSucc(int id) throws RemoteException{
    	IChordNode Isucc = this.findPred(id);
	Isucc = Isucc.getSuccessor();
	return Isucc;
    }

    public IChordNode getFingerNode(int m) throws NotBoundException, MalformedURLException, RemoteException{
	if(this.ft.finger[m] == this.selfid)
	    return this;

	return (IChordNode)Naming.lookup(getServName(this.ft.ip[m]));
    }

    public IChordNode getSuccsucc() throws NotBoundException, MalformedURLException, RemoteException{
	if(this.ft.succsucc == this.selfid)
	    return this;

	return (IChordNode)Naming.lookup(getServName(this.ft.succsuccip));
    }

    public IChordNode getSuccessor() throws RemoteException{
	validSucc();
	try{
	    return getFingerNode(0);
	}catch(Exception e){
	    e.printStackTrace();
	    System.exit(1);
	    return null;
	}
    }

    public IChordNode getSuccessor(int i) throws RemoteException{
	try{
	    return getFingerNode(i);
	}catch(Exception e){
	    return getSuccessor();
	}
    }

    public void validSucc() throws RemoteException{
	IChordNode Isucc = null;

	try{
	    Isucc = getFingerNode(0);
	    Isucc.isAlive();
	}catch(Exception e){
	    debug(this, "Successor failed, fixing...");
	    fixSucc();
	}
    }


    public void fixSucc() throws RemoteException{
	try{
	    debug(this, "Try swith to succ-successor...");
	    IChordNode Isucc = this.getSuccsucc();
	    Isucc.isAlive();
	    this.ft.setFinger(0, Isucc.getId(), Isucc.getAddr());
	    Isucc.setPred(this.selfid, this.selfip);
	}catch(Exception e){
	    debug(this, "Failed...");
	    debug(this, e.toString());
	    System.exit(1);
	}
    }		

    public void printFingerTable() throws RemoteException{
	System.out.println(this.ft);
    }

    public int generateId(String ip) throws UnknownHostException{

	int id = -1;
	for(int i = 0; i < serverAddr.length; i++)
	    if(serverAddr[i].compareTo(ip) == 0)
		id = i;
	
	if(id == -1)
	    throw new UnknownHostException(ip);
	else
	    return id;
    }

    public IChordNode findPeer(){
	int cand = 0, failed = 0;
	IChordNode IremChordNode;

	for(; cand < ChordNode.serverAddr.length; cand++){
    	    String servName = this.getServName(ChordNode.serverAddr[cand]);
    	    try{
		if(cand == this.selfid)
		    throw new ConnectException("");
    		IremChordNode = (IChordNode)Naming.lookup(servName);
		IremChordNode.isAlive();
		System.out.println("Server found: " +servName);
		return IremChordNode;
	    }catch(Exception e){
		failed++;
	    }
	}
	System.out.println("Nothing found");
	return null;
    }

    public void isAlive() throws RemoteException {
	;
    }

    public void printMsg(String msg) throws RemoteException{
	System.out.println("From Remote: "+msg);
    }
    
    public String toString(){
	StringBuilder sb = new StringBuilder("");
	sb.append("self["+this.selfid+"]: "+serverAddr[this.selfid]);
	sb.append("\n----------\n");
	return sb.toString();
    }

    public String getServName(String ip){
	return "rmi://"+ ip + ":" + ChordNode.Port + "/"+ChordNode.ServName;
    }

    public ChordNode() throws UnknownHostException, RemoteException, SocketException, MalformedURLException{
	
	this.selfip = InetAddress.getLocalHost().getHostAddress();
	this.selfid = generateId(this.selfip);

	ft = new FingerTable(this);

	IChordNode IartChordNode = findPeer();
	if(IartChordNode != null)
	    this.join(IartChordNode);

	Naming.rebind(this.getServName(selfip), this);

	Thread t = new Thread(this);
	t.start();

	System.out.println(this.ft);
    }

    public static String getHostAddress() throws SocketException{

	Enumeration<NetworkInterface> i = NetworkInterface.getNetworkInterfaces();
	while(i.hasMoreElements()){
	    NetworkInterface n = i.nextElement();
	    System.out.println("Interface: " + n.getName());
	    Enumeration<InetAddress> addrs = n.getInetAddresses();
	    while(addrs.hasMoreElements()){
		InetAddress addr = addrs.nextElement();
		System.out.println("  Address: " + addr.getHostAddress());
	    }
	}
	return "";
    }

    private class FingerTable implements Serializable{
	public int[] start;
	public int[] finger;
	public String[] ip;
	public int succsucc;
	public String succsuccip;
	public int predecessor;
	public String predip;
	private int id;
	private String addr;

	public FingerTable(ChordNode cn){
	    this.start = new int[ChordNode.FINGER_SIZE];
	    this.finger = new int[ChordNode.FINGER_SIZE];
	    this.ip = new String[ChordNode.FINGER_SIZE];

	    /* init finger entries */
	    for(int i = 0; i < finger.length; i++){
		this.start[i] = (int)Math.pow(2, i) + cn.selfid;
		if(this.start[i] >= (int)Math.pow(2, ChordNode.FINGER_SIZE))
		    this.start[i] = this.start[i] - (int)Math.pow(2, ChordNode.FINGER_SIZE);
		this.finger[i] = cn.selfid;
		this.ip[i] = cn.selfip;
	    }
	    
	    this.succsucc = cn.selfid;
	    this.succsuccip = cn.selfip;
	    this.predecessor = cn.selfid;
	    this.predip = cn.selfip;
	    this.id = cn.selfid;
	    this.addr = cn.selfip;
	}

	public void setFinger(int idx, int suc, String ip){
	    this.finger[idx] = suc;
	    this.ip[idx] = ip;
	}

	public void setSuccsucc(int id, String ip){
	    this.succsucc = id;
	    this.succsuccip = ip;
	}

	public void setPred(int pred, String ip){
	    this.predecessor = pred;
	    this.predip = ip;
	}
	
	public String toString(){
	    StringBuilder sb = new StringBuilder("\tFinger Table\t\n");
	    sb.append("-------------------------\n");
	    sb.append("id: " + this.id + "\t" + "addr: " + this.addr + "\n");
	    sb.append("-------------------------\n");
	    for(int i = 0; i < ChordNode.FINGER_SIZE; i++)
		sb.append(i + " : " + this.start[i] + " : " + this.finger[i] + " : " + this.ip[i] + "\n");
	    sb.append("------------------------\n");
	    sb.append("predecessor : " + this.predecessor + " : " + this.predip + "\n");
	    sb.append("succ-successor : " + this.succsucc + " : " + this.succsuccip + "\n");
	    return sb.toString();
	}

    }

}
/* obsolete
    // public IChordNode getPredNode() throws RemoteException{
    // 	if(this.finger.predecessor == this.selfid)
    // 	    return this;

    // 	IChordNode IremChordNode = null;
    // 	try{
    // 	    IremChordNode = (IChordNode)Naming.lookup(getServName(this.ft.predip));
    // 	}catch(Exception e){
    // 	}
    // 	return IremChordNode;
    // }


    public void join(IChordNode an) throws RemoteException{
	// init finger table
	System.out.println("init finger table");
	initFingerTable(an);
	this.printFingerTable();
	IChordNode pred = getPredNode();
	pred.printFingerTable();
	System.out.println("update others");
	// update others
	updateOthers();
	this.printFingerTable();
	pred = getPredNode();
	pred.printFingerTable();
	
    }

    public void initFingerTable(IChordNode an) throws RemoteException{
	IChordNode node;
	System.out.println("find succ of "+this.ft.start[0]);
	node = an.findSucc(this.ft.start[0]);
	this.ft.setFt(0, node.getId(), node.getAddr());
	this.ft.setPred(node.getPredId(), node.getPredAddr());
	node.setPred(this.selfid, this.selfip);
	for(int i = 1; i < ChordNode.FINGER_SIZE; i++){
	    if(inRangeEq(this.ft.start[i], this.selfid, this.ft.finger[i-1]))
		this.ft.setFinger(i, this.ft.finger[i-1], this.ft.ip[i-1]);
	    else{
		node = an.findSucc(this.ft.start[i]);
		this.ft.setFinger(i, node.getId(), node.getAddr());
	    }
	}
    }

    public void updateOthers() throws RemoteException{
	IChordNode p;
	for(int i = 0; i < ChordNode.FINGER_SIZE; i++){
	    int candid = pushBack(this.selfid, (int)Math.pow(2, i));
	    p = findPred(candid);
	    if(p.getSuccId() == candid)
		p = p.getFingerNode(0);
	    p.printMsg("hi");
	    p.updateFingerTable(this.selfid, this.selfip, i);
	}
    }

    public void updateFingerTable(int id, String ip, int i) throws RemoteException{
	System.out.println("update ft table: " + id + " : " + ip + " : " + i);
	if(inRange(id, this.selfid, this.ft.finger[i])){
	    this.ft.finger[i] = id;
	    this.ft.ip[i] = ip;
	    if(this.getPredId() != id){
		IChordNode p = this.getPredNode();
		p.updateFingerTable(id, ip, i);
	    }
	}
    }
*/