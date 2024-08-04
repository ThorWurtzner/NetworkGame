import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServerThread extends Thread {

 private Socket connectionSocket;
    private DataOutputStream writer;

    public GameServerThread(Socket connectionSocket)throws IOException {
        this.connectionSocket = connectionSocket;
        this.writer = new DataOutputStream(connectionSocket.getOutputStream());
    }


    @Override
    public void run() {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            System.out.println("Spiller forbundet til Server");

            while (true) {
                String clientMsg = inFromClient.readLine();
                System.out.println(clientMsg);

                String[] deler = clientMsg.split(" ");
                String handling = deler[0];

                GameServer.broadcastMessage(clientMsg);
                if(deler[0].equals("LEAVE")){
                    GameServer.leave(this);
                    connectionSocket.close();
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    public void sendMessage(String message) throws IOException {
        writer.writeBytes(message + '\n');
    }
}
