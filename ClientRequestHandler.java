import java.util.Iterator;

/*This class will be called by the leader when client request is received, leader will call this class and send Append Entries RPC to all the server in the cluster*/
public class ClientRequestHandler extends RaftRequestHandler {
    public ClientRequestHandler(RaftServer raftServer, Iterator servers, String payload) {
        super(raftServer, servers);
        UtilityClass utilityClass = new UtilityClass();
        String clientAppendEntries = utilityClass.createClientAppendEntries(raftServer.currentTerm, raftServer.RaftServerUID, payload,
                raftServer.persistentState.getLastLogIndex(), raftServer.persistentState.getLastLogTerm(), raftServer.persistentState.getCommitIndex());
        setMessageForClientRequest(clientAppendEntries);
    }

}
