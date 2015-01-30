import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/*
 * Command Checker
 */

public class CmdChecker {
    final static String CMD_HELP = "help";
    final static String CMD_STATE = "state";
    final static String CMD_CREAT = "create";
    final static String CMD_CONN = "connect";
    final static String CMD_SHOWSERV = "showserv";
    final static String CMD_SHOW = "show";
    final static String CMD_USER = "users";
    final static String CMD_SCHED = "sched";
    final static String CMD_DUMP = "dump";
    final static String CMD_DEL = "del";
    final static String CMD_MOD = "mod";
    final static String CMD_SCHED10 = "sched10";
    final static String CMD_TEST = "test";
    final static String CMD_DISCONN = "disconnect";

    final static String HELP_HELP = "List all commands";
    final static String HELP_STATE = "Show client status";
    final static String HELP_CREAT = "Create calendar for current client";
    final static String HELP_CONN = "Connect to user's calendar";
    final static String HELP_SHOWSERV = "Print status on server side {Test Command}";
    final static String HELP_SHOW = "Print valid events of <username> from <starttime> to <endtime>";
    final static String HELP_USER = "List all users";
    final static String HELP_SCHED = "Schedule events";
    final static String HELP_DUMP = "Let server dump all events {Test Command}";
    final static String HELP_DEL = "Delete event";
    final static String HELP_MOD = "Modify event";
    final static String HELP_SCHED10 = "Schedule 10 fixed events {Test Command}";
    final static String HELP_TEST = "Test";
    final static String HELP_DISCONN = "Disconnect from CalendarManager";

    final static String[] ValidCmd = {CMD_HELP, CMD_STATE, CMD_CREAT, CMD_CONN, CMD_SHOW, CMD_USER, CMD_SCHED, CMD_DEL, CMD_MOD};
    final static String[] HiddenCmd = {CMD_SHOWSERV, CMD_DUMP, CMD_SCHED10, CMD_TEST};
    final static String[] ValidHelp = {HELP_HELP, HELP_STATE, HELP_CREAT, HELP_CONN, HELP_SHOW, HELP_USER, HELP_SCHED, HELP_DEL, HELP_MOD};
    final static String[] HiddenHelp = {HELP_SHOWSERV, HELP_DUMP, HELP_SCHED10, HELP_TEST};

    final static String DatePattern = "yyyy-[m]m-[d]d [h]h:[m]m";
    final static String InvalidCmd = "Invalid Command";
    final static String EmptyCmd = "Empty Command";
    final static String InvalidType = "Invalid Event Type";
    final static String InvalidTime = "Invalid Time Format";
    final static String InvalidIndex = "Invalid Index";

    public HashMap<String, String> Cmds;
    public HashMap<String, String> HiddenCmds;

    public CmdChecker(){
	Cmds = new HashMap<String, String>();
	HiddenCmds = new HashMap<String, String>();
	for(int i = 0; i < ValidCmd.length; i++)
	    Cmds.put(ValidCmd[i], ValidHelp[i]);
	for(int i = 0; i < HiddenCmd.length; i++)
	    HiddenCmds.put(HiddenCmd[i], HiddenHelp[i]);

    }

    /*
     * Check if str resides in valid Commands
     */
    public String checkCmd(String str) throws CmdFormatException{
	str = str.trim();
	if(str.length() == 0)
	    throw new CmdFormatException(EmptyCmd);
	if(!Cmds.containsKey(str) && !HiddenCmds.containsKey(str))
	    throw new CmdFormatException(InvalidCmd + " " + str);
	return str;
    }

    /*
     * Check if str is valid Event type
     */
    public int checkEventType(String str) throws CmdFormatException{
	int priv;

	str = str.trim();
	if(str.matches("private")) 
	    priv = Event.ACC_PRIVATE;
	else if(str.matches("public")) 
	    priv = Event.ACC_PUBLIC;
	else if(str.matches("group")) 
	    priv = Event.ACC_GROUP;
	else if(str.matches("open")) 
	    priv = Event.ACC_OPEN;
	else
	    throw new CmdFormatException(InvalidType+" "+str);

	return priv;
    }

    /*
     * Check if str is valid User(s) format
     * if no user specified, return defUser
     */
    public String[] checkUsers(String defUser, String str){
	str = str.trim();
	if(str.length() == 0)
	    return new String[]{defUser};
	else{
	    String[] us = str.split(",");
	    for(String s : us)
		s = s.trim();
	    return us;
	}
    }

    /*
     * Check if str is valid Time format
     */
    public Date checkTime(String str) throws CmdFormatException{
	if(str.matches("^\\d{4}-\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}\\s*")){
	    String[] date = (str.split("\\s+")[0]).split("-");
	    String[] time = (str.split("\\s+")[1]).split(":");
	    Calendar cal = Calendar.getInstance();
	    cal.clear();
	    cal.set(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1,
		    Integer.parseInt(date[2]), Integer.parseInt(time[0]),
		    Integer.parseInt(time[1]), 0);
	    Date t = cal.getTime();
	    return t;
	}else{
	    throw new CmdFormatException(InvalidTime +" " + str + " ["+DatePattern+"]");
	}
    }

    /*
     * Check if str is valid Event Description format
     */
    String checkDescription(String str){
	str = str.trim();
	return "\"" + str + "\"";
    }

    /*
     * Check if str is valid numeric option
     */
    int checkIndex(int upper,String str) throws CmdFormatException {
	str = str.trim();
	try{
	    int index = Integer.parseInt(str);
	    if(index < 0 || index >= upper)
		throw new CmdFormatException(InvalidIndex+ " " + str);
	    return index;
	}catch(NumberFormatException e){
	    throw new CmdFormatException(InvalidIndex + " " + str);
	}
    }
	    
}
