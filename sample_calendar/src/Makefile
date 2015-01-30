JFLAGS = #-g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	CalendarServer.java \
	Client.java \
	Event.java \
	ChordNode.java \
	CalendarClient.java \
	Calendar.java \
	CalendarManager.java \
	CmdFeeder.java \
	CmdChecker.java \
	ICalendarManager.java \
	ICalendarClient.java \
	ICalendar.java \
	UserExistException.java \
	CmdFormatException.java \
	UserNotExistException.java \
	EventConflictException.java \
	NoConnectionException.java

DUMPDIR = $(shell ls -d [0-9]*)

default: classes

classes: $(CLASSES:.java=.class)

clean:
	-rm -rf *.class *.dump $(DUMPDIR)