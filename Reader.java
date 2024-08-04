import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Reader extends Thread {
    private BufferedReader reader;
    private GUI gui;

    public Reader(Socket socket, GUI gui) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.gui = gui;
        System.out.println(gui);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String receivedMessage = reader.readLine();
                System.out.println("Received: " + receivedMessage);

                String[] deler = receivedMessage.split(" ");

                String handling = deler[0];
                String playerName = deler[1];
                int posX = Integer.parseInt(deler[2]);
                int posY = Integer.parseInt(deler[3]);
                String retning = deler[4];
                int points = Integer.parseInt(deler[5]);



                Player player = gui.getPlayer(playerName, posX,posY,retning);
                if(player.point != points) {
                    player.point = points;
                }
                if(handling.equals("SHOOT")){
                    gui.shoot(0,0,retning, player);
                }
                else{
                    switch (retning) {
                        case "up":    gui.playerMoved(0,-1,"up", player); break;
                        case "down":  gui.playerMoved(0,+1,"down", player); break;
                        case "left":  gui.playerMoved(-1,0,"left", player); break;
                        case "right": gui.playerMoved(+1,0,"right", player); break;
                        case "ud": gui.playerMoved(0,0,"ud", player);gui.leave(playerName); break;
                        case "ind": gui.playerMoved(0,0,"Ind", player); break;
                        default: break;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}