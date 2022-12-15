import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*This class is extended by LeaderElectionVoteRequestHandler.java,
HeartBeatRequestHandler.java and ClientRequestHandler.java,
This is helper class which helps to broadcast the requests to multiple server. */
public class RaftRequestHandler extends Thread {
    protected List<RequestManager> requestsGenerator;
    protected RaftServer requestInitiatorRaftServer;
    protected Iterator<RaftServer> requestReceiverRaftServers;
    private final int requestInitiatorRaftServerUID;
    protected String requestMessageBody;
    protected ProjectLog projectLog;

    protected RequestManager requesterGenerator(int serverPort) {
        return new RequestManager(this, "localhost", serverPort, this.requestMessageBody);
    }

    //set message for vote request
    protected void setMessageForVote(String voteRequestMessageBody) {
        this.requestMessageBody = voteRequestMessageBody;
    }

    //set message for heartbeat
    protected void setMessageForHeartBeat(String heartbeatRequestBody) {
        this.requestMessageBody = heartbeatRequestBody;
    }

    //set message for client request
    protected void setMessageForClientRequest(String clientRequestBody) {
        this.requestMessageBody = clientRequestBody;
    }

    protected void stopVotingProcess() {
        for (RequestManager rm : this.requestsGenerator) {
            rm.interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (requestReceiverRaftServers.hasNext()) {
                RaftServer raftServer = requestReceiverRaftServers.next();
                if (raftServer.RaftServerUID != this.requestInitiatorRaftServerUID) {
                    RequestManager requesterGenerator = this.requesterGenerator(raftServer.serverPort);
                    this.requestsGenerator.add(requesterGenerator);
                    requesterGenerator.start();
                }
            }
        } catch (Exception e) {
            projectLog.logMessage("Error while sending request: " + e);
        }
    }

    public RaftRequestHandler(RaftServer requestInitiatorRaftServer, Iterator<RaftServer> requestReceiverRaftServers) {
        super("RaftRequestHandler");
        this.projectLog = new ProjectLog("RaftRequestHandler", System.out);
        this.requestReceiverRaftServers = requestReceiverRaftServers;
        this.requestInitiatorRaftServer = requestInitiatorRaftServer;
        this.requestInitiatorRaftServerUID = requestInitiatorRaftServer.RaftServerUID;
        this.requestsGenerator = new ArrayList<>();
    }
}
