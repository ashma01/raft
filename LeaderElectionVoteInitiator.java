import java.util.Map;

/*This class helps to process the vote request received by the servers*/
class LeaderElectionVoteInitiator extends RequestManager {

    @Override
    public void run() {
        this.raftRequestHandler.projectLog.logMessage("Initiating vote request message to port: " + serverPort);
        String response = utilityClass.sendToOtherServer(serverAddress, serverPort, requestBody);
        if (response.equalsIgnoreCase("ERROR")) {
            this.raftRequestHandler.projectLog.logMessage("Error while collecting votes!");
            return;
        }
        Map<String, String> resp = utilityClass.parseMap(response);
        this.raftRequestHandler.projectLog.logMessage("Response to the Vote Request: " + resp);
        if (resp != null && !resp.get("term").isEmpty() && !resp.get("voteGranted").isEmpty())
            ((LeaderElectionVoteRequestHandler) raftRequestHandler).setElectionResult(Integer.valueOf(resp.get("term")),
                    Integer.valueOf(resp.get("voteGranted")) != 0);

    }

    public LeaderElectionVoteInitiator(RaftRequestHandler raftRequestHandler, String requestMessage, String address, int port) {
        super(raftRequestHandler, address, port, requestMessage);
    }
}
