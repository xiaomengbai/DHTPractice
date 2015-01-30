import java.rmi.*;
import java.rmi.server.*;

public class ChordClient{
    
    static CmdFeeder feeder;
    static IChordNode node;

    public static void main(String[] argv){

	feeder = new CmdFeeder();
	node = null;

	while(true){
	    String cmd = feeder.feedCmd("chord");
	    cmd = cmd.trim();
	    if(cmd.length() > 0)
		handleCmd(cmd);
	}
    }

    public static void handleCmd(String cmd){

	if(cmd.compareTo("list") == 0){
	    for(int i = 0; i < ChordNode.serverAddr.length; i++){
		try{
		    node = (IChordNode)Naming.lookup("rmi://"+ChordNode.serverAddr[i]+":"+ChordNode.Port+"/"+ChordNode.ServName);
		    node.isAlive();
		    System.out.println(i+". "+ChordNode.serverAddr[i]);
		}catch(Exception e){
		}
	    }
	    int option = Integer.parseInt(feeder.feedCmd("?"));
	    System.out.println("Node " + option + " chosen.");
	    try{
		node = (IChordNode)Naming.lookup("rmi://"+ChordNode.serverAddr[option]+":"+ChordNode.Port+"/"+ChordNode.ServName);
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}else if(cmd.compareTo("status") == 0){
	    if(node == null){
		System.out.println("null");
	    }else{
		try{
		    System.out.println("Node "+node.getId());
		}catch(Exception e){
		    e.printStackTrace();
		}
	    }
	}else if(cmd.compareTo("finger") == 0){
	    if(node == null){
		System.out.println("Connect first");
	    }else{
		try{
		    node.printFingerTable();
		}catch(Exception e){
		    e.printStackTrace();
		}
	    }
	}else if(cmd.compareTo("help") == 0){
	    System.out.println("list");
	    System.out.println("status");
	    System.out.println("finger");
	}else{
	    System.out.println("Illegal Command "+cmd);
	}

    }
}