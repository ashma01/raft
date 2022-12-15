import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


/*Basic server class that open socket; each raft server extends this class to become a server*/
public class Server extends Thread {

    private ServerSocket serverSocket;
    protected Socket socket;
    protected int serverUID;
    protected DataInputStream inputStream;
    protected DataOutputStream outputStream;
    protected int serverPort;
    protected ProjectLog projectLog;

    protected String read() {
        try {
            return this.inputStream.readUTF();
        } catch (IOException e) {
            projectLog.logMessage("read" + e.toString());
            return "ERROR";
        }
    }

    protected void write(String response) {
        try {
            this.outputStream.writeUTF(response);
        } catch (IOException e) {
            projectLog.logMessage(("write" + e.toString()));
        }
    }

    protected void leaderElectionProcess() {
        try {
            String response = this.read();
            this.write(response);
        } catch (Exception e) {
            projectLog.logMessage("process" + e.toString());
        }
    }

    public void run() {
        while (true) {
            try {
                this.socket = serverSocket.accept();
                this.inputStream = new DataInputStream(this.socket.getInputStream());
                this.outputStream = new DataOutputStream(this.socket.getOutputStream());
                this.leaderElectionProcess();// will be overridden
                this.socket.close();
            } catch (SocketTimeoutException s) {
                projectLog.logMessage("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public Server(int number, int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.serverUID = number;
        this.serverPort = port;
        this.projectLog = new ProjectLog("RaftServer", System.out);
        this.projectLog.logMessage("Server created");

    }
}