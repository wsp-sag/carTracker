package utility;

// This class contains the constants which are used by several models

public class ConstantsOhio implements ConstantsIf {
	
    private static final int TOD_INTERVAL_IN_MINUTES = 15;    
    private static final int NUM_TOD_INTERVALS_PER_HOUR = 4;    
    private static final int NUM_TOD_INTERVALS = 96;       
    private static final int DEFALUT_LAST_DEPART_MINUTE = 1430;
    
    public int getTodIntervalMinutes() {
        return TOD_INTERVAL_IN_MINUTES;
    }

    public int getNumTodIntervalsPerHour() {
        return NUM_TOD_INTERVALS_PER_HOUR;
    }

    public int getNumTodIntervals() {
        return NUM_TOD_INTERVALS;
    }

    public int getDefaultLastDepartMinute() {
        return DEFALUT_LAST_DEPART_MINUTE;
    }

}