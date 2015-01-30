import java.util.Date;
import java.io.Serializable;

/*
 * Event
 */

public class Event implements Serializable, Comparable<Event>{

    public static final int ACC_PRIVATE = 0;
    public static final int ACC_PUBLIC = 1;
    public static final int ACC_GROUP = 2;
    public static final int ACC_OPEN = 3;
    public static final String[] NOMEMBER = new String[0];
    public static final String OPEN_DES = "\"\"";
    public static final String NO_DES = "\"\"";

    private Date stTime;
    private Date edTime;
    private String textDescription;
    private int privilege;
    private String[] members;
    private boolean slept;

    public Event(Date st, Date ed, String des, int priv, String[] m){
	this.stTime = st;
	this.edTime = ed;
	this.textDescription = des;
	this.privilege = priv;
	this.members = m;
	this.slept = false;
    }

    public Event(Date st, Date ed, String des, int priv){
    	this(st, ed, des, priv, Event.NOMEMBER);
    }

    /*
     * Compare mems with members owned
     */
    public boolean isMemberEqual(String[] mems){
	if(mems.length != this.members.length)
	    return false;
	for(String u : mems)
	    if(!isMember(u))
		return false;
	return true;
    }

    public int compareTo(Event ev){
	/* Events are sorted via Date */
	return getStTime().compareTo(ev.getStTime());
    }

    /*
     * Compare two Events
     */
    public boolean isEqual(Event ev){
	return ( ev.getStTime().compareTo(this.stTime) == 0 &&
		 ev.getEdTime().compareTo(this.edTime) == 0 &&
		 ev.getTextDescription().compareTo(this.textDescription) == 0 &&
		 ev.getPrivilege() == this.privilege &&
		 this.isMemberEqual(ev.getMembers()) );
    }

    public boolean isNotified(){
	return this.slept;
    }
    public void setNotified(){
	this.slept = true;
    }

    /*
     * Check if u is one of members of this Event
     */
    public boolean isMember(String u){
	for(String m : members)
	    if(u.compareTo(m) == 0)
		return true;
	return false;
    }

    public Event duplicate(){
	Event e = new Event(this.stTime, this.edTime, this.textDescription, this.privilege, this.members);
	if(this.isNotified())
	    e.setNotified();
	return e;
    }

    public void clearMembers(){
	this.members = Event.NOMEMBER;
    }
    public void clearTextDescription(){
	this.setTextDescription(Event.NO_DES);
    }

    public String[] getMembers(){
	return this.members;
    }
    public Date getStTime(){
	return this.stTime;
    }
    public Date getEdTime(){
	return this.edTime;
    }
    public String getTextDescription(){
	return this.textDescription;
    }
    public int getPrivilege(){
	return this.privilege;
    }

    public void setStTime(Date st){
	this.stTime = st;
    }
    public void setEdTime(Date ed){
	this.edTime = ed;
    }
    public void setTextDescription(String des){
	this.textDescription = des;
    }
    public void setPrivilege(int priv){
	this.privilege = priv;
    }
    public String toString(){
	StringBuilder sb = new StringBuilder();
	sb.append("[");
	sb.append(stTime);
	sb.append(" -- ");
	sb.append(edTime);
	sb.append("]:[" + textDescription + "]:[");
	if(members.length > 0){
	    for(String m : members)
		sb.append(m+",");
	}
	sb.append("]");
	if(this.slept)
	    sb.append(":[notified]:");
	else
	    sb.append(":[fresh]");
	switch(this.privilege){
	case Event.ACC_PRIVATE: 
	    sb.append(":[private]");
	    break;
	case Event.ACC_PUBLIC:
	    sb.append(":[public]");
	    break;
	case Event.ACC_GROUP:
	    sb.append(":[group]");
	    break;
	case Event.ACC_OPEN:
	    sb.append(":[open]");
	    break;
	}
	sb.append("\n");
	return sb.toString();
    }
}