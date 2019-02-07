package algorithms;

import objects.Household;
import objects.HouseholdCarAllocation;

@FunctionalInterface
public interface HhCarAllocatorIf {
	public HouseholdCarAllocation getCarAllocationWithSchedulesForHh( Household hh );
}
