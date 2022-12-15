import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/*This class will be called for vote request, and it will broadcast the request vote rpc to all the servers*/
public class LeaderElectionVoteRequestHandler extends RaftRequestHandler {

    private final int majorityVotes;

    /*Atomic for thread-safety*/
    AtomicInteger receivedVotes = new AtomicInteger(-1);

    /*Override LeaderElectionVoteRequester */
    protected RequestManager requesterGenerator(int port) {
        return new LeaderElectionVoteInitiator(this, this.requestMessageBody, "localhost", port);
    }

    /*set vote received from servers and check if won the election*/
    public synchronized void setElectionResult(int term, boolean voteGranted) {
        if (voteGranted) {
            this.receivedVotes.incrementAndGet();
        }
        if (this.receivedVotes.get() == this.majorityVotes) {
            projectLog.logMessage("Total votes received: " + this.receivedVotes);
            projectLog.logMessage("New Leader Elected for Term: " + term);

            // Set himself as a leader
            requestInitiatorRaftServer.setServerState(RaftServerStates.LEADER);
            // immediately sends heartbeat to maintain authority
            requestInitiatorRaftServer.sendHeartBeat();
        }
    }

    public LeaderElectionVoteRequestHandler(RaftServer callerRaftServer, Iterator allServers, int majorityVotes) {
        super(callerRaftServer, allServers);
        this.majorityVotes = majorityVotes;
        UtilityClass utilityClass = new UtilityClass();

        String requestVote = utilityClass.createMessage(callerRaftServer.currentTerm, callerRaftServer.RaftServerUID,
                callerRaftServer.persistentState.getLastLogIndex(), callerRaftServer.persistentState.getLastLogTerm());
        setMessageForVote(requestVote);
        receivedVotes.incrementAndGet();
    }
}
