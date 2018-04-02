package fileProcessing;

public enum GlobalProperties {
	
	ABM_DATA_FOLDER_FILE_KEY				( "abm.data.folder", 0 ),
	ABM_TRIP_DATA_FILE_KEY 					( "abm.trip.data.file", 1 ),
	PARAMETER_FILE_KEY  					( "car.allocator.parameters.file", 2 ),
	ABM_HHID_LABEL_ID_KEY					("abm.data.file.hhid",3),
	ABM_HOUSEHOLD_DATA_FILE_KEY				("abm.household.data.file",4),
	ABM_PERSON_DATA_FILE_KEY				("abm.person.data.file",5),
	MIN_ABM_HH_ID_KEY						("min.hh.id",6),
	MAX_ABM_HH_ID_KEY						("max.hh.id",7),
	NUM_HHS_PER_JOB							("num.hhs.per.jppf.job", 8),
	MIN_ACT_DURATION						("min.activity.duration",9),
	CAR_REPOSITION_COST_PER_MILE			("car.repositioning.cost.per.mile",10),
	USUAL_DRIVER_BONUS						("trip.bonus.usual.driver",11),
	HHID_LOG_REPORT_KEY						("log.report.hh.id",12),
	UNSAT_DEMAND_DISTANCE_PENALTY			("unsatisfied.demand.distance.penalty",13),
	PARKING_HOURLY_FIELD					("sed.parking.cost.hourly.field",14),
	PARKING_MONTHLY_FIELD					("sed.parking.cost.monthly.field",15),
	MINUTES_PER_MILE						("minutes.per.mile",16),
	UNUSED_CAR_BONUS						("unused.car.bonus",17),
	ROUND_UP_THRESHOLD						("round.up.threshold",18),
	INTRA_ZONAL_GIK_SAME_PERSON				("intra.zonal.car.repo.same.person.penalty",19),
	RUN_MIXED_INTEGER_LP					("start.with.mixed.integer.programming",20);


	private final String label;
	private final int index;
	
	GlobalProperties( String label, int index ) {
		this.label = label;
		this.index = index;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
}
