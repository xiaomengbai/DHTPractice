import java.rmi.*;
import java.rmi.server.*;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.io.*;

/*
 * Client
 * The user interface
 */

public class Client{
    /* Used to read user input */
    static CalendarClient cli;
    static CmdChecker checker;
    static CmdFeeder feeder;

    public static void main(String[] argv){
	if(argv.length < 1){
	    System.out.println("Usage: java Client <username>");
	    System.exit(1);
	}

	/* Build Client */
	try{
	    cli = new CalendarClient(argv[0]);
	}catch(RemoteException e){
	    e.printStackTrace();
	}

	/* Command feeder */
	feeder = new CmdFeeder();
	/* Command Checker */
	checker = new CmdChecker();
	
	do{

	    String cmd = feeder.feedCmd(cli.user);

	    handleCmd(cmd);

	}while(true);
    }


    /*
     * Handle Commands
     */
    static void handleCmd(String cmd){
	try{
	    /* Trim and Check if command valid */
	    cmd = checker.checkCmd(cmd);

	    if(cmd.matches(checker.CMD_HELP)){ // help
		
		System.out.println(">>> Valid Commands ");
		for(String c : checker.Cmds.keySet())
		    System.out.format("\t%10s\t\t%s\n", c, checker.Cmds.get(c));

	    }else if(cmd.matches(checker.CMD_STATE)){ //state

		System.out.println(">>> User: " + cli.user);
		System.out.println("    Calendar Manager: " + cli.showCalendarManagerStatus());
		System.out.println("    Calendar: " + cli.showCalendarStatus());

	    }else if(cmd.matches(checker.CMD_CREAT)){ //create

		String address = feeder.feedCmd("Server address");
		cli.connectCalendarManager(address);
		cli.createCalendar();

	    }else if(cmd.matches(checker.CMD_CONN)){ //connect

		String address = feeder.feedCmd("Server Address");
		cli.connectCalendarManager(address);
		cli.connectCalendar();

	    }else if(cmd.matches(checker.CMD_SHOWSERV)){ //showserv [TEST]

		cli.showCalendarManagerServ();

	    }else if(cmd.matches(checker.CMD_SHOW)){ //show

		ArrayList<Event> validEvents;
		Date st1, st2;
		String user = feeder.feedCmd("user("+cli.user+")");

		try{
		    st1 = checker.checkTime(feeder.feedCmd("Time Format: "+checker.DatePattern, "Date 1"));
		    st2 = checker.checkTime(feeder.feedCmd("Date 2"));
		}catch(CmdFormatException e){
		    System.out.println(">>> Client: Time Format Error, Show all events...");
		    st1 = st2 = null;
		}
	    
		/* Check if time is in right order */
		if(st1 != null && st2 != null && st1.after(st2))
		    throw new CmdFormatException("Time order wrong!");

		validEvents = cli.retrieveEvents(user, st1, st2);
		printEvents(validEvents);
	
	    }else if(cmd.matches(checker.CMD_USER)){ //users

		ArrayList<String> users = cli.getUsers();
		System.out.println(users.toString());

	    }else if(cmd.matches(checker.CMD_SCHED)){ //sched
	    
		int type = checker.checkEventType(feeder.feedCmd("Available Types: private public group open", "type"));
		String[] users = checker.checkUsers(cli.user, feeder.feedCmd("Users Pattern: <user1>[,<user2>[,<user3>...]]", "users("+cli.user+")"));
		Date st = checker.checkTime(feeder.feedCmd("Time Format: " + checker.DatePattern, "Beginning Time"));
		Date ed = checker.checkTime(feeder.feedCmd("Ending Time"));
		if(st.after(ed))
		    throw new CmdFormatException("Time order wrong!");
		String des = checker.checkDescription(feeder.feedCmd("Description"));

		Event ev = cli.getEvent(type, users, st, ed, des);
		cli.scheduleEvent(ev);

		System.out.print(">>>Add Event: "+ev.toString());

	    }else if(cmd.matches(checker.CMD_DUMP)){ //dump
	    
		cli.dump();

	    }else if(cmd.matches(checker.CMD_DEL)){ //del

		ArrayList<Event> uEvents = cli.retrieveEvents("", null, null);
		/* List events */
		for(int i = 0; i < uEvents.size(); i++)
		    System.out.print(i+". "+uEvents.get(i));
		int idx = checker.checkIndex(uEvents.size(), feeder.feedCmd("[0-"+(uEvents.size()-1)+"]"));
		System.out.print(">>>Client: Remove Event: " + uEvents.get(idx));
		cli.removeEvent(idx);

	    }else if(cmd.matches(checker.CMD_MOD)){ //mod

		ArrayList<Event> uEvents = cli.retrieveEvents("", null, null);
		for(int i = 0; i < uEvents.size(); i++)
		    System.out.print(i+". "+uEvents.get(i));
		int idx = checker.checkIndex(uEvents.size(), feeder.feedCmd("[0-"+(uEvents.size()-1)+"]"));

		/* User cannot modify the Event Type */
		int type = uEvents.get(idx).getPrivilege();
		String[] users = checker.checkUsers(cli.user, feeder.feedCmd("Users Pattern: <user1>[,<user2>[,<user3>...]]", "users("+cli.user+")"));
		Date st = checker.checkTime(feeder.feedCmd("Time Format: " + checker.DatePattern, "Beginning Time"));
		Date ed = checker.checkTime(feeder.feedCmd("Ending Time"));
		if(st.after(ed))
		    throw new CmdFormatException("Time order wrong!");
		String des = checker.checkDescription(feeder.feedCmd("Description"));
		Event me = cli.getEvent(type, users, st, ed, des);
		cli.modifyEvent(me, idx);

	    }else if(cmd.matches(checker.CMD_SCHED10)){ //sched10 [TEST]
		
		int[] types = {checker.checkEventType("private"), checker.checkEventType("private"), 
			       checker.checkEventType("private"), checker.checkEventType("public"), 
			       checker.checkEventType("public"), checker.checkEventType("public"), 
			       checker.checkEventType("open"), checker.checkEventType("open"), 
			       checker.checkEventType("open"), checker.checkEventType("public")};
		// int[] types = {checker.checkEventType("open")};
		String[] thisUser = new String[]{cli.user};
		Date[] stDates = new Date[]{ checker.checkTime("2010-1-1 10:00"), checker.checkTime("2010-1-2 10:00"), checker.checkTime("2010-1-3 10:00"),
					     checker.checkTime("2010-1-4 10:00"), checker.checkTime("2010-1-5 10:00"), checker.checkTime("2010-1-6 10:00"),
					     checker.checkTime("2010-1-7 10:00"), checker.checkTime("2010-1-8 10:00"), checker.checkTime("2010-1-9 10:00"),
					     checker.checkTime("2010-1-10 10:00")};
		// Date[] stDates = new Date[]{ checker.checkTime("2010-1-9 10:00") };
		// Date[] edDates = new Date[]{ checker.checkTime("2010-1-9 22:00") };
		
		Date[] edDates = new Date[]{ checker.checkTime("2010-1-1 22:00"), checker.checkTime("2010-1-2 22:00"), checker.checkTime("2010-1-3 22:00"),
					     checker.checkTime("2010-1-4 22:00"), checker.checkTime("2010-1-5 22:00"), checker.checkTime("2010-1-6 22:00"),
					     checker.checkTime("2010-1-7 22:00"), checker.checkTime("2010-1-8 22:00"), checker.checkTime("2010-1-9 22:00"),
					     checker.checkTime("2010-1-10 22:00")};
		// String[] dess = new String[]{ checker.checkDescription("des9") };
		String[] dess = new String[]{"des1","des2","des3","des4","des5","des6","des7","des8","des9","des10"};

		for(int i = 0; i < types.length; i++){
		    Event ev = cli.getEvent(types[i], thisUser, stDates[i], edDates[i], dess[i]);
		    cli.scheduleEvent(ev);
		}

	    }else if(cmd.matches(checker.CMD_TEST)){ //test [TEST]

		System.out.println("test...");

	    }
	}catch(RemoteException e){
	    e.printStackTrace();
	}catch(UserNotExistException e){
	    System.out.println(e);
	}catch(UserExistException e){
	    System.out.println(e);
	}catch(NoConnectionException e){
	    System.out.println(e);
	}catch(EventConflictException e){
	    System.out.println(e);
	}catch(CmdFormatException e){
	    /* Do nothing for empty command */
	    if(!e.getMessage().matches(checker.EmptyCmd))
		System.out.println(e.getMessage());
	}
    }

    public static void printEvents(ArrayList<Event> events){
	System.out.println(">>> Client: Events List");
	for(Event ev : events)
	    System.out.print(ev.toString());
    }
	    
}