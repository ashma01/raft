/*This class is a one log entry for each logs*/
class ReplicatedLogEntry {
    private int term;
    private String clientCommandToExecute;

    public int getTermFromRaftServersLogEntry() {
        return this.term;
    }

    public ReplicatedLogEntry(int term, String clientCommandToExecute) {
        this.term = term;
        this.clientCommandToExecute = clientCommandToExecute;
    }
}