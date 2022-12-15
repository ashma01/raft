import java.io.IOException;
import java.util.*;

/*This class initializes a cluster of 5 servers*/
public class ConsensusModule extends TimerTask {
    private int totalRaftServers;
    private List<RaftServer> raftCluster;

    private static ProjectLog projectLog = new ProjectLog("ConsensusModule", System.out);
    public static final int RAFT_POOL_STATUS_PING_TIME = 300;

    public Iterator<RaftServer> getTotalServers() {
        return this.raftCluster.iterator();
    }

    /*Current state of all the servers, this method will be called after every 300ms (RAFT_POOL_STATUS_PING_TIME)*/
    private void clusterHealth() {
        projectLog.logMessage("************** RAFT CLUSTER CURRENT STATUS START **************");
        for (RaftServer raftServer : this.raftCluster) {
            projectLog.logMessage("Raft Server " + raftServer.getServerNumber() + " in the cluster is a " + raftServer.getCurrentStatus());
        }
        projectLog.logMessage("*************** RAFT CLUSTER CURRENT STATUS END ***************");
    }

    /*Method will create required number of raft servers with respective port numbers*/
    private void createCluster(int initialPort) {
        projectLog.logMessage("Creating " + this.totalRaftServers + " servers");
        for (int i = 0; i < this.totalRaftServers; i++) {
            try {
                RaftServer raftServerThread = new RaftServer(i + 1, initialPort++, this);
                this.raftCluster.add(raftServerThread);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*Calculate majority to win the election*/
    public int calculateRequiredMajority() {
        int majorityVote = ((this.totalRaftServers) / 2) + 1;
        projectLog.logMessage("Required number of votes for majority is: " + majorityVote);
        return majorityVote;
    }

    /*Calling raft server class to start the election process*/
    public void startServerPool() {
        for (RaftServer raftServer : this.raftCluster) {
            raftServer.start();
            projectLog.logMessage("Server number " + raftServer.getServerNumber() + " started");
        }
        this.clusterHealth();
    }

    @Override
    public void run() {
        this.clusterHealth();
    }

    public ConsensusModule(int totalRaftServers, int initialPort) {
        this.raftCluster = new ArrayList<>();
        this.totalRaftServers = totalRaftServers;
        createCluster(initialPort);
        projectLog.logMessage("Server cluster is created");
        calculateRequiredMajority();
    }
}
