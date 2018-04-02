package accessibility;


import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;


/**
 * @author Jim Hicks
 * 
 *         Class for managing matrix data in a remote process and accessed by UECs
 *         using RMI.
 */
public class MatrixDataRemoteHandler implements MatrixDataHandlerIf
{

  MatrixDataServerRmi ms;
  MatrixType matrixType;


  public MatrixDataRemoteHandler(MatrixDataServerRmi ms, MatrixType matrixType) {
    this.ms = ms;
    this.matrixType = matrixType;
  }


  public void registerMatrix ( String matrixIdentifier, String fileName, String matrixType, String tableName ) {
    ms.registerMatrix( matrixIdentifier, fileName, matrixType, tableName );
  }

  public MatrixType getMatrixType() {
    return matrixType;
  }

  public float[] getMatrixRow( String matrixIdentifier, int tableRow ) {
    return ms.getMatrixRow( matrixIdentifier, tableRow );
  }

  public float[] getMatrixColumn( String matrixIdentifier, int tableCol ) {
    return ms.getMatrixColumn( matrixIdentifier, tableCol );
  }

  public void writeMatrixFile(String fileName, Matrix[] m, String[] names, MatrixType mt) {
    ms.writeMatrixFile( fileName, m, names, mt );
  }

  public void releaseMatrixData() {
    MatrixDataManager.getInstance().clearData();
  }

}