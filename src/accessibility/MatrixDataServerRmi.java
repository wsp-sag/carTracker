package accessibility;

import java.io.Serializable;

import com.pb.common.calculator.DataEntry;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import accessibility.UtilRmi;

//import org.apache.log4j.Logger;


/**
 * @author Jim Hicks
 *
 * Class for managing matrix data in a remote process and accessed by UECs using RMI.
 */
public class MatrixDataServerRmi implements MatrixDataServerIf, Serializable {

    //protected static Logger logger = Logger.getLogger(MatrixDataServerRmi.class);

    UtilRmi remote;
    String connectString;


    public MatrixDataServerRmi( String hostname, int port, String className ) {

        connectString = String.format("//%s:%d/%s", hostname, port, className );
        remote = new UtilRmi(connectString);

    }

    public void writeMatrixFile(String fileName, Matrix[] m, String[] names, MatrixType mt) {
        Object[] objArray = { fileName, m, names, mt };
        remote.method("writeMatrixFile", objArray);
    }

    
    public String testRemote( String remoteObjectName ) {
        Object[] objArray = { remoteObjectName };
        return (String)remote.method( "testRemote", objArray);
    }

    public String testRemote() {
        Object[] objArray = {};
        return (String)remote.method( "testRemote", objArray);
    }

    public void listMatrixRegistry() {
        Object[] objArray = {};
        remote.method("listMatrixRegistry", objArray);
    }

    public void registerMatrix ( String matrixIdentifier, String fileName, String matrixType, String tableName ) {
        Object[] objArray = { matrixIdentifier, fileName, matrixType, tableName };
        remote.method("registerMatrix", objArray);
    }
    
    public float[] getMatrixRow( String matrixIdentifier, int tableRow ) {
        Object[] objArray = { matrixIdentifier, tableRow };
        return (float[])remote.method("getMatrixRow", objArray);
    }
    
    public float[] getMatrixColumn( String matrixIdentifier, int tableCol ) {
        Object[] objArray = { matrixIdentifier, tableCol };
        return (float[])remote.method("getMatrixColumn", objArray);
    }
    
    public float[] loadMatrix( String matrixIdentifier, int tableCol ) {
        Object[] objArray = { matrixIdentifier, tableCol };
        return (float[])remote.method("loadMatrix", objArray);
    }
    
    public void clear() {
        Object[] objArray = {};
        remote.method( "clear", objArray);
    }
    
    public Matrix getMatrix( DataEntry dataEntry ) {
        Object[] objArray = { dataEntry };
        return (Matrix)remote.method( "getMatrix", objArray);
    }

    public void start32BitMatrixIoServer( MatrixType mType ) {
        Object[] objArray = { mType };
        remote.method( "start32BitMatrixIoServer", objArray);
    }
    
    public void stop32BitMatrixIoServer() {
        Object[] objArray = {};
        remote.method( "stop32BitMatrixIoServer", objArray);
    }

	@Override
	public void writeMatrixFile(String fileName, Matrix[] m) {
		// TODO Auto-generated method stub
		
	}


}