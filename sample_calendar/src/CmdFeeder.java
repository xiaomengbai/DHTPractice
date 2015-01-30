import java.io.*;


/*
 * Command Feeder
 */
public class CmdFeeder {

    private BufferedReader br;

    public CmdFeeder(){
	this.br = new BufferedReader(new InputStreamReader(System.in));
    }

    public String feedCmd(String str){
	try{
	    System.out.print(str+"> ");
	    String cmd = br.readLine();
	    if(cmd == null){
		/* Ctrl-D would exit this program */
		System.out.println("\nBye!");
		System.exit(0);
	    }
	    return cmd;
	}catch(Exception e){
	    e.printStackTrace();
	    System.exit(1);
	}
	return "";
    }

    public String feedCmd(String help, String str){
	System.out.println("# "+help);
	return this.feedCmd(str);
    }
}
