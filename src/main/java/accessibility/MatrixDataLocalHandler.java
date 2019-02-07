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
public class MatrixDataLocalHandler implements MatrixDataHandlerIf
{

  MatrixDataServer matrixDataServer;
  MatrixType matrixType;


  public MatrixDataLocalHandler(MatrixDataServer matrixDataServer, MatrixType matrixType) {
    this.matrixDataServer = matrixDataServer;
    this.matrixType = matrixType;
  }



  public void start32BitMatrixIoServer() {
    matrixDataServer.start32BitMatrixIoServer(matrixType);
  }

  public void stop32BitMatrixIoServer() {
    matrixDataServer.stop32BitMatrixIoServer();
    releaseMatrixData();
  }

  public void registerMatrix ( String matrixIdentifier, String fileName, String matrixType, String tableName ) {
    matrixDataServer.registerMatrix( matrixIdentifier, fileName, matrixType, tableName );
  }

  public MatrixType getMatrixType() {
    return matrixType;
  }

  public float[] getMatrixRow( String matrixIdentifier, int tableRow ) {
    return matrixDataServer.getMatrixRow( matrixIdentifier, tableRow );
  }

  public float[] getMatrixColumn( String matrixIdentifier, int tableCol ) {
    return matrixDataServer.getMatrixColumn( matrixIdentifier, tableCol );
  }

  public void writeMatrixFile(String fileName, Matrix[] m, String[] names, MatrixType mt) {
    matrixDataServer.writeMatrixFile( fileName, m, names, mt );
  }

  public void releaseMatrixData() {
    matrixDataServer.clear();
    MatrixDataManager.getInstance().clearData();
  }

}