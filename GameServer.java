

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private static List<GameServerThread> clientThreads = new ArrayList<>();
    public static void main(String[] args) throws Exception {

        ServerSocket welcomeSocket = new ServerSocket(8888);

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            GameServerThread clientThread = new GameServerThread(connectionSocket);
            clientThreads.add(clientThread);
            clientThread.start();
        }
    }

    public static synchronized void broadcastMessage(String message) {
        for (GameServerThread clientThread : clientThreads) {
            try {
                clientThread.sendMessage(message);
            } catch (IOException e) {
                System.out.println("Fejl ved udsendelse af besked til klient: " + e.getMessage());
            }
        }
    }
    public static void leave(GameServerThread GT){
        clientThreads.remove(GT);

    }

}
