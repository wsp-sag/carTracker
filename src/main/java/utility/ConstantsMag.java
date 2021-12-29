package utility;

// This class contains the constants which are used by several models

public class ConstantsMag implements ConstantsIf {
	
    private static final int TOD_INTERVAL_IN_MINUTES = 30;    
    private static final int NUM_TOD_INTERVALS_PER_HOUR = 2;    
    private static final int NUM_TOD_INTERVALS = 40; 
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