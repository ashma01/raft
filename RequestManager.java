
/*Helper class to manage parallel request broadcasting for RPCs*/
class RequestManager extends Thread {

    protected String serverAddress;
    protected int serverPort;
    protected RaftRequestHandler raftRequestHandler;

    String requestBody;

    UtilityClass utilityClass ;

    @Override
    public void run() {
        utilityClass.sendToOtherServer(serverAddress, serverPort, requestBody);
    }

    public RequestManager(RaftRequestHandler raftRequestHandler, String serverAddress, int serverPort, String requestBody) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.raftRequestHandler = raftRequestHandler;
        this.requestBody = requestBody;
        this.utilityClass = new UtilityClass();
    }
}
