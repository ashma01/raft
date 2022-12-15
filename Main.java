import java.util.Timer;


/*Main runnable class to start the program/algorithm*/
public class Main {

    public static final int NUMBER_OF_SERVER = 5; // Number of server sets to 5
    public static final int STARTING_PORT = 8001; // initial port number

    public static void main(String[] args) {
        int numberOfServers = NUMBER_OF_SERVER;
        Timer timer = new Timer();
        ConsensusModule consensusModule = new ConsensusModule(numberOfServers, STARTING_PORT);
        timer.schedule(consensusModule, ConsensusModule.RAFT_POOL_STATUS_PING_TIME, ConsensusModule.RAFT_POOL_STATUS_PING_TIME);
        consensusModule.startServerPool();
    }
}
