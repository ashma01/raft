import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/*This is a basic client class, it will send request to all server until the leader is found.*/

public class Client {

    public static final int NUMBER_OF_SERVER = 5; // Number of server sets to 5
    public static final int STARTING_PORT = 8001; // initial port number
    public static void main(String[] args) {
        String serverName = "localhost";
        Socket client = new Socket();
        String responseBody;
        try {
            for (int port = STARTING_PORT; port < (STARTING_PORT + NUMBER_OF_SERVER); port++) {
                System.out.println("Client connecting to Server - "+ serverName + ":" + port);
                client = new Socket(serverName, port);
                System.out.println("Connected!");
                DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
                responseBody = createReqForServer();
                dataOutputStream.writeUTF(responseBody);
                System.out.println("Request sent!");
                DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
                String serverResponse = dataInputStream.readUTF();
                System.out.println("Response received from server: " + serverResponse + "!\n");
                if (serverResponse.equalsIgnoreCase("Executed")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                System.out.println("Something went wrong");
            }
        }
    }

    /*This method will create a client request for server*/
    public static String createReqForServer() {
        String payload = "set,a,1@set,b,2@set,c,3@show@remove,b@show";
        Map<String, String> heartBeatMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        heartBeatMap.put("RPC", "CLIENT");
        heartBeatMap.put("payload", payload);
        for (String key : heartBeatMap.keySet()) {
            sb.append(key).append("=").append(heartBeatMap.get(key)).append("&");
        }
        return sb.toString();
    }
}