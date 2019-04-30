package utility;

public interface DistributableIf<T> {
	public T newInstance( int firstTask, int lastTask );
}
