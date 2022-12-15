import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/*This ia utility class which contains message parsing code and creating request and response rpc*/
public class UtilityClass {

    private void set(String key, String value, Map<String, Integer> clientsLog) {
        clientsLog.put(key, Integer.valueOf(value));
    }

    private void remove(String key, Map<String, Integer> clientsLog) {
        clientsLog.remove(key);
    }

    private void show(Map<String, Integer> clientsLog) {
        System.out.println(clientsLog.toString());
    }

    /*Create message for request vote RPC*/
    public String createMessage(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
        Map<String, String> requestVoteMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        requestVoteMap.put("RPC", RaftServer.RPC_VOTE);
        requestVoteMap.put("term", String.valueOf(term));
        requestVoteMap.put("candidateId", String.valueOf(candidateId));
        requestVoteMap.put("lastLogIndex", String.valueOf(lastLogIndex));
        requestVoteMap.put("lastLogTerm", String.valueOf(lastLogTerm));
        for (String key : requestVoteMap.keySet()) {
            sb.append(key).append("=").append(requestVoteMap.get(key)).append("&");
        }
        return sb.toString();
    }


    /*Create message for Hartbeat*/
    public String createHeartBeat(int term, int leaderId) {
        Map<String, String> heartBeatMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        heartBeatMap.put("RPC", RaftServer.RPC_APPEND); // append rpc for heartbeat
        heartBeatMap.put("term", String.valueOf(term));
        heartBeatMap.put("leaderId", String.valueOf(leaderId));
        heartBeatMap.put("prevLogIndex", String.valueOf(0));
        heartBeatMap.put("prevLogTerm", String.valueOf(0));
        heartBeatMap.put("payload", "0");// payload is 0 so servers can determine the heartbeat message
        heartBeatMap.put("leaderCommit", String.valueOf(0));
        for (String key : heartBeatMap.keySet()) {
            sb.append(key).append("=").append(heartBeatMap.get(key)).append("&");
        }
        return sb.toString();
    }

    /*Create message for client appendEntries RPC*/
    public String createClientAppendEntries(int term, int leaderId, String payload, int prevLogIndex, int prevLogTerm, int leaderCommit) {
        Map<String, String> appendEntriesMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        appendEntriesMap.put("RPC", RaftServer.RPC_APPEND); // client append rpc
        appendEntriesMap.put("term", String.valueOf(term));
        appendEntriesMap.put("leaderId", String.valueOf(leaderId));
        appendEntriesMap.put("prevLogIndex", String.valueOf(prevLogIndex));
        appendEntriesMap.put("prevLogTerm", String.valueOf(prevLogTerm));
        appendEntriesMap.put("payload", payload);
        appendEntriesMap.put("leaderCommit", String.valueOf(leaderCommit));
        for (String key : appendEntriesMap.keySet()) {
            sb.append(key).append("=").append(appendEntriesMap.get(key)).append("&");
        }
        return sb.toString();
    }

    public Map<String, String> parseMap(final String input) {
        final Map<String, String> map = new HashMap<String, String>();
        for (String pair : input.split("&")) {
            String[] kv = pair.split("=");
            map.put(kv[0], kv[1]);
        }
        return map;
    }

    public int generateRandomInteger(int min, int max) {
        Random r = new Random(System.currentTimeMillis());
        return r.ints(min, (max + 1)).findFirst().getAsInt();
    }

    public String parsePayload(String payload) {
        Map<String, Integer> raftServerLogs = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder();
        String[] payloadFirstSplit = payload.split("@");
        for (String entries : payloadFirstSplit) {
            System.out.println(entries);
            String[] tokens = entries.split(",");
            if (tokens[0].equals("set")) {
                set(tokens[1], tokens[2], raftServerLogs);
                stringBuilder.append("set");
            }
            if (tokens[0].equals("remove")) {
                remove(tokens[1], raftServerLogs);
                stringBuilder.append("remove");
            }
            if (tokens[0].equals("show")) {
                show(raftServerLogs);
                stringBuilder.append("show");
            }
        }
        return stringBuilder.toString();
    }

    /*This method will be called from when a server needs to send message to other server*/
    public String sendToOtherServer(String address, int port, String requestBody) {
        try {
            Socket serverSocket = new Socket();
            serverSocket.connect(new InetSocketAddress(address, port), RaftServer.CONNECTION_TIMEOUT);
            DataOutputStream dataOutputStream = new DataOutputStream(serverSocket.getOutputStream());
            dataOutputStream.writeUTF(requestBody);
            DataInputStream dataInputStream = new DataInputStream(serverSocket.getInputStream());
            String serverResponse = dataInputStream.readUTF();
            serverSocket.close();
            return serverResponse;
        } catch (IOException e) {
            return "ERROR";
        }
    }

    public UtilityClass() {
    }
}
