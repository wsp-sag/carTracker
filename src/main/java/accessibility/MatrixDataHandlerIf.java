package accessibility;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;

public interface MatrixDataHandlerIf {

    public void registerMatrix ( String matrixIdentifier, String fileName, String matrixType, String tableName );
    public float[] getMatrixRow( String matrixIdentifier, int tableRow );
    public float[] getMatrixColumn( String matrixIdentifier, int tableCol );
    public void writeMatrixFile(String fileName, Matrix[] m, String[] names, MatrixType mt);
    public MatrixType getMatrixType();
    public void releaseMatrixData();
    
}
