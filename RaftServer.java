import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/*This class is responsible to initialize all the raft server and handle RPCs as well as
process leader election and election timeout*/
public class RaftServer extends Server {

    public static final int HEARTBEAT_TIMEOUT = 50;

    public static final int CONNECTION_TIMEOUT = 2000;

    public static final int ELECTION_RANGE_RANDOM_MIN = 300;
    public static final int ELECTION_RANGE_RANDOM_MAX = 1000;

    public static final String RPC_VOTE = "RequestVote";

    public static final String RPC_APPEND = "AppendEntries";

    public static final String RPC_CLIENT = "CLIENT";

    public int RaftServerUID; // unique id of server

    private final ConsensusModule RaftServerCluster;

    private RaftServerStates raftServerState;

    private Timer heartbeatTimer = new Timer();

    private Timer electionTimeoutTimer;

    private LeaderElectionVoteRequestHandler leaderElectionVoteRequestHandler;

    AtomicInteger votedFor = new AtomicInteger(-1);

    public int currentTerm = 0;

    public PersistentState persistentState;

    String responseBody;

    /*Make yourself follower*/
    private boolean changeToFollower(int newTerm) {
        if (newTerm > this.currentTerm) {
            this.currentTerm = newTerm;
            this.votedFor.set(-1);
            if (this.raftServerState == RaftServerStates.LEADER || this.raftServerState == RaftServerStates.CANDIDATE) {
                this.setServerState(RaftServerStates.FOLLOWER);
            }
            return true;
        }
        return false;
    }

    private String executeClientRequest(String payload) {
        boolean isSuccess = this.appendEntriesRPC(this.currentTerm, this.persistentState.getLastLogIndex(), this.persistentState.getLastLogTerm(),
                payload, this.persistentState.getCommitIndex() + 1
        );
        responseBody = handleAppendEntries(this.currentTerm, isSuccess ? 1 : 0);
        UtilityClass utilityClass = new UtilityClass();
        Map<String, String> message = utilityClass.parseMap(responseBody);
        return message.toString();
    }

    /*This method is executed when a candidate or follower receives a heartbeat from the leader*/
    private String manageHeartBeat(Map<String, String> message) {
        this.disconnectElectionTimeout();
        this.startElectionTimeout();
        /*Stop any ongoing vote request*/
        if (leaderElectionVoteRequestHandler != null) {
            leaderElectionVoteRequestHandler.stopVotingProcess();
            leaderElectionVoteRequestHandler.interrupt();
            leaderElectionVoteRequestHandler = null;
        }

        int leaderTerm = Integer.parseInt(message.get("term"));
        if (currentTerm < leaderTerm) {
            currentTerm = leaderTerm;
        }

        return handleAppendEntries(this.currentTerm, 1);
    }

    /*Return vote if voted yes or no for the candidate*/
    protected synchronized boolean requestVoteRPC(int candidateTerm, int candidateId, int lastLogIndex, int lastLogTerm) {
        int persistentStateLastLogTerm = this.persistentState.getLastLogTerm();
        int persistentStateLastLogIndex = this.persistentState.getLastLogIndex();

        if (this.changeToFollower(candidateTerm) || candidateTerm < this.currentTerm) {
            this.votedFor.set(-1); // don't grant vote to the server
        } else if (candidateTerm == this.currentTerm) {
            if (this.votedFor.get() == -1 || this.votedFor.get() == candidateId) {
                if ((persistentStateLastLogTerm > lastLogTerm) ||
                        (persistentStateLastLogTerm == lastLogTerm &&
                                persistentStateLastLogIndex > lastLogIndex)) {
                    this.votedFor.set(-1); // candidate's logs are not up-to-date, so reset vote
                    return false; // don't grant vote to the server
                } else {
                    this.votedFor.set(candidateId); // set who you voted for
                    this.disconnectElectionTimeout(); // stop your election timeout
                    return true; // grant vote to the server
                }
            }
        }
        return false;
    }

    /*Append entries RPC handler; handles the request received from the client and the leader will send
    this message to all the server and ask them to update log*/
    protected boolean appendEntriesRPC(int term, int prevLogIndex, int prevLogTerm, String entries, int leaderCommit) {

        if (term < this.currentTerm) {
            return false;
        }

        /*Check if logs need to be appended*/
        boolean isAppended = this.persistentState.appendToReplicatedLogEntry(term, entries, prevLogTerm, prevLogIndex);
        this.persistentState.updateLeaderCommitIndex(leaderCommit); //update commit index
        return isAppended;
    }

    /*Process request from any server or client accordingly*/
    protected void leaderElectionProcess() {

        String messageFromServers = this.read();
        UtilityClass utilityClass = new UtilityClass();
        Map<String, String> message = utilityClass.parseMap(messageFromServers);
        if (message == null || message.get("RPC").isEmpty()) {
            return;
        }

        String command = message.get("RPC");

        if (raftServerState == RaftServerStates.FOLLOWER || raftServerState == RaftServerStates.CANDIDATE) {

            switch (command) {
                case RPC_VOTE:
                    /*Request vote RPC for follower and candidates*/
                    boolean isVoted = this.requestVoteRPC(Integer.parseInt(message.get("term")), Integer.parseInt(message.get("candidateId")),
                            Integer.parseInt(message.get("lastLogIndex")), Integer.parseInt(message.get("lastLogTerm"))); // get the vote decision

                    responseBody = handleResultVote(this.currentTerm, isVoted ? 1 : 0); // return vote decision back
                    this.write(responseBody);
                    break;

                case RPC_APPEND:
                    /*Identify if this Append-RPC is a heartbeat or an append entries request*/
                    int currentTerm = Integer.parseInt(message.get("term"));
                    changeToFollower(currentTerm); // step down to become a follower if your term is smaller than the received

                    if ("0".equals(message.get("payload"))) {
                        /*This is a heartbeat message because payload = 0*/
                        responseBody = manageHeartBeat(message); // manage heartbeat , reset election timeout
                        this.write(responseBody);
                    } else {
                        System.out.println(message);
                        boolean isSuccess = this.appendEntriesRPC(Integer.parseInt(message.get("term")), Integer.parseInt(message.get("prevLogIndex")),
                                Integer.parseInt(message.get("prevLogTerm")), message.get("payload"), Integer.parseInt(message.get("leaderCommit")));
                        responseBody = handleAppendEntries(this.currentTerm, isSuccess ? 1 : 0);
                        this.write(responseBody);
                    }
                    break;

                case RPC_CLIENT:
                    /*Client request to a server who is not a leader for this term*/
                    responseBody = "I am not the leader"; // simply returning response
                    this.write(responseBody);
                    break;

                default:
                    break;
            }
        }

        if ((raftServerState == RaftServerStates.LEADER) && command.equals(RPC_CLIENT)) {
            /*Client request to the leader*/
            String payload = message.get("payload");
            sendClientRequest(payload);// send payload to other client for append entry
            executeClientRequest(payload);// after sending client request to other servers for log replication, leader will execute command itself
            responseBody = "Executed";// return executed
            this.write(responseBody);
        }
    }

    /*Request vote response*/
    public String handleResultVote(int term, int voteGranted) {
        Map<String, String> requestVoteMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        requestVoteMap.put("term", String.valueOf(term));
        requestVoteMap.put("voteGranted", String.valueOf(voteGranted));
        for (String key : requestVoteMap.keySet()) {
            sb.append(key).append("=").append(requestVoteMap.get(key)).append("&");
        }
        return sb.toString();
    }

    /*Append entry response same as given in the raft paper*/
    public String handleAppendEntries(int term, int success) {
        Map<String, String> resultAppendEntriesMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        resultAppendEntriesMap.put("term", String.valueOf(term));
        resultAppendEntriesMap.put("success", String.valueOf(success));
        for (String key : resultAppendEntriesMap.keySet()) {
            sb.append(key).append("=").append(resultAppendEntriesMap.get(key)).append("&");
        }
        return sb.toString();
    }

    /*Election timeout timer task, this initializes timer task to keep the timeout running to random amount of time*/
    public void startElectionTimeout() {
        UtilityClass utilityClass = new UtilityClass();
        long rndTimeout = utilityClass.generateRandomInteger(ELECTION_RANGE_RANDOM_MIN, ELECTION_RANGE_RANDOM_MAX);
        electionTimeoutTimer = new Timer();
        electionTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startLeaderElection(); // call leader election
            }
        }, rndTimeout, rndTimeout);
    }

    /*To stop the timer task*/
    public void disconnectElectionTimeout() {
        electionTimeoutTimer.cancel();
    }


    /*Leader election method*/
    public void startLeaderElection() {
        try {
            setServerState(RaftServerStates.CANDIDATE);// set as a candidate
            projectLog.logMessage("Changed state of raft server " + this.RaftServerUID + " to CANDIDATE");
            ++this.currentTerm; // increment current term
            leaderElectionVoteRequestHandler = new LeaderElectionVoteRequestHandler(this, RaftServerCluster.getTotalServers(), RaftServerCluster.calculateRequiredMajority());// initialize vote request
            this.disconnectElectionTimeout(); // reset election timeout
            this.startElectionTimeout(); // reset election timeout
            leaderElectionVoteRequestHandler.start(); // start vote request for leader election
        } finally {
            setServerState(RaftServerStates.FOLLOWER);
        }
    }


    public void setServerState(RaftServerStates state) {
        synchronized (this) {
            this.raftServerState = state;
            this.votedFor.set(-1);
        }
    }

    public int getServerNumber() {
        return this.serverUID;
    }

    public String getCurrentStatus() {
        switch (this.raftServerState) {
            case LEADER:
                return "LEADER";
            case FOLLOWER:
                return "FOLLOWER";
            case CANDIDATE:
                return "CANDIDATE";
            default:
                return "ERROR";
        }
    }

    /*Elected leader sends heartbeat*/
    public void sendHeartBeat() {
        if (raftServerState == RaftServerStates.LEADER) {
            projectLog.logMessage("Sending HeartBeat to other servers in the cluster");
            new HeartBeatRequestHandler(this, RaftServerCluster.getTotalServers()).start();
        }
    }

    public void sendClientRequest(String payload) {

        projectLog.logMessage("Sending ClientRequest to other server in the cluster");
        new ClientRequestHandler(this, RaftServerCluster.getTotalServers(), payload).start();
    }

    public RaftServer(int number, int port, ConsensusModule RaftServerCluster) throws IOException {
        super(number, port);
        this.raftServerState = RaftServerStates.FOLLOWER;
        this.RaftServerUID = number;
        this.RaftServerCluster = RaftServerCluster;
        this.persistentState = new PersistentState();
        this.startElectionTimeout();
        this.heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendHeartBeat(); // call heartbeat
            }
        }, 0, HEARTBEAT_TIMEOUT);
    }
}
