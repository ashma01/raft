import java.util.Iterator;

/*A heartbeat message it created in this class. This message is further sent by the Leader every time to maintain its leadership.*/
public class HeartBeatRequestHandler extends RaftRequestHandler {
    public HeartBeatRequestHandler(RaftServer raftServer, Iterator<RaftServer> servers) {
        super(raftServer, servers);
        UtilityClass utilityClass = new UtilityClass();
        String heartBeat = utilityClass.createHeartBeat(raftServer.currentTerm, raftServer.RaftServerUID);
        setMessageForHeartBeat(heartBeat);
    }
}
