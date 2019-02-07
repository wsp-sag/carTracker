package objects;

import java.io.Serializable;

public class HouseholdCarAllocation implements Serializable {

	private static final long serialVersionUID = 6525413672198243121L;
	
	private final Household hh;
	private final double[][][] scheduleResult;
	private final double[] unsatisfiedDemandResult;
	private final double[][][] allocationResult;
	private final double[][][][] linkingResult;
	private final int optimalSolutionIterations;
	private final int iterNumForIntegerizing;
	
	public HouseholdCarAllocation( Household hh,double[] unsatisfiedDemandResult, double[][][] scheduleResult, double[][][] allocationResult,double[][][][] linkingResult ,int optimalSolutionIterations, int iterNumForIntegerizing ) {
		this.hh = hh;
		this.unsatisfiedDemandResult = unsatisfiedDemandResult;
		this.scheduleResult = scheduleResult;
		this.allocationResult = allocationResult;
		this.linkingResult = linkingResult;
		this.optimalSolutionIterations = optimalSolutionIterations;
		this.iterNumForIntegerizing = iterNumForIntegerizing;
	}
	
	public Household getHousehold() {
		return hh;
	}
	
	public int getOptimalSolutionIterations() {
		return optimalSolutionIterations;
	}
	
	public double[][][]  getAllocationResult() {
		return allocationResult;
	}
	public double[][][][]  getCarLinkingResult() {
		return linkingResult;
	}
	
	public double[][][]  getScheduleAdjustmentResults() {
		return scheduleResult;
	}
	
	public double[]  getUnsatisfiedDemandResults() {
		return unsatisfiedDemandResult;
	}
	
	public int getNumIterationsForIntegerizing() {
		return iterNumForIntegerizing;
	}
	
	
	
}

