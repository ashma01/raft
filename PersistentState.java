import java.util.ArrayList;
import java.util.List;


/*Persistent states log entry created by the leader and other server to manage their log in sync*/
public class PersistentState {

    private ProjectLog projectLog;
    private int commitIndex;

    final List<ReplicatedLogEntry> entries;

    public int getLastLogIndex() {
        return this.entries.size();
    }

    public int getCommitIndex() {
        return this.commitIndex;
    }

    public int getLastLogTerm() {
        int lastTerm = -1;
        if (this.getLastLogIndex() > 0) {
            try {
                lastTerm = this.entries.get(entries.size() - 1).getTermFromRaftServersLogEntry();
            } catch (Exception e) {
                System.out.println("empty log index list");
            }
        }
        return lastTerm;
    }

    /*get the term at the log entry*/
    public int getTermFromReplicatedLogEntry(int i) {
        if (this.getLastLogIndex() <= i) {
            try {
                ReplicatedLogEntry replicatedLogEntry = this.entries.get(i);
                return replicatedLogEntry.getTermFromRaftServersLogEntry();
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    /*Append the log entry*/
    public boolean appendToReplicatedLogEntry(int term, String payLoad, int previousLogTerm, int previousLogIndex) {
        boolean toAppend = false;

        int lastTerm = this.getTermFromReplicatedLogEntry(previousLogIndex);

        if (lastTerm != previousLogTerm) {
            int entrySize = this.entries.size();
            if (entrySize > previousLogIndex) {
                this.entries.subList(previousLogIndex, entrySize).clear();
            }
        } else {
            toAppend = true;
        }

        projectLog.logMessage("New Log entry inserted");
        UtilityClass utilityClass = new UtilityClass();
        String command = utilityClass.parsePayload(payLoad);
        ReplicatedLogEntry ne = new ReplicatedLogEntry(term, command);
        synchronized (this.entries) {
            this.entries.add(ne);
        }
        return toAppend;

    }

    /*Update leader commit index*/
    public void updateLeaderCommitIndex(int i) {
        if (i > this.commitIndex) {
            this.commitIndex = (Math.min(i, this.getLastLogIndex()));
        }
    }

    public PersistentState() {
        this.projectLog = new ProjectLog("PersistenceLog", System.out);
        this.entries = new ArrayList<>();
        this.commitIndex = -1;
        this.projectLog.logMessage("PersistenceLog created");
    }
}


