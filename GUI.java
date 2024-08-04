

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;

public class GUI extends Application {

	private AtomicLong lastShootTime = new AtomicLong(0);

	public static final int size = 32;
	public static final int scene_height = size * 20 + 100;
	public static final int scene_width = size * 20 + 200;

	public static Image image_floor;
	public static Image image_wall;
	public static Image hero_right,hero_left,hero_up,hero_down;

	public static Image image_shoot_horizontal, image_shoot_vertical, image_shoot_wallEast, image_shoot_wallWest, image_shoot_wallNorth, image_shoot_wallSouth;



	public static Player me;
	public static List<Player> players = new ArrayList<Player>();

	private Label[][] fields;
	private TextArea scoreList;

	private  String[] board = {    // 20x20
			"wwwwwwwwwwwwwwwwwwww",
			"w        ww        w",
			"w w  w  www w  w  ww",
			"w w  w   ww w  w  ww",
			"w  w               w",
			"w w w w w w w  w  ww",
			"w w     www w  w  ww",
			"w w     w w w  w  ww",
			"w   w w  w  w  w   w",
			"w     w  w  w  w   w",
			"w ww ww        w  ww",
			"w  w w    w    w  ww",
			"w        ww w  w  ww",
			"w         w w  w  ww",
			"w        w     w  ww",
			"w  w              ww",
			"w  w www  w w  ww ww",
			"w w      ww w     ww",
			"w   w   ww  w      w",
			"wwwwwwwwwwwwwwwwwwww"
	};

	public GUI() throws IOException {
	}


	// -------------------------------------------
	// | Maze: (0,0)              | Score: (1,0) |
	// |-----------------------------------------|
	// | boardGrid (0,1)          | scorelist    |
	// |                          | (1,1)        |
	// -------------------------------------------

	// Connection stuff
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private Socket clientSocket;

	// GUI stuff
	private Button joinBtn = new Button("Join");
	private TextField nameField = new TextField();
	private Button leaveBtn = new Button("Leave");

	@Override
	public void start(Stage primaryStage) {
		try {
			clientSocket = new Socket("localhost", 8888);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());

			Reader readerThread = new Reader(clientSocket, this);
			readerThread.start();

			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(0, 10, 0, 10));

			Text mazeLabel = new Text("Maze:");
			mazeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			Text scoreLabel = new Text("Score:");
			scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			scoreList = new TextArea();

			GridPane boardGrid = new GridPane();

			image_wall  = new Image(getClass().getResourceAsStream("Image/wall4.png"),size,size,false,false);
			image_floor = new Image(getClass().getResourceAsStream("Image/floor1.png"),size,size,false,false);

			hero_right  = new Image(getClass().getResourceAsStream("Image/heroRight.png"),size,size,false,false);
			hero_left   = new Image(getClass().getResourceAsStream("Image/heroLeft.png"),size,size,false,false);
			hero_up     = new Image(getClass().getResourceAsStream("Image/heroUp.png"),size,size,false,false);
			hero_down   = new Image(getClass().getResourceAsStream("Image/heroDown.png"),size,size,false,false);

			image_shoot_horizontal = new Image(getClass().getResourceAsStream("Image/fireHorizontal.png"),size,size,false,false);
			image_shoot_vertical = new Image(getClass().getResourceAsStream("Image/fireVertical.png"),size,size,false,false);
			image_shoot_wallEast = new Image(getClass().getResourceAsStream("Image/fireWallEast.png"),size,size,false,false);
			image_shoot_wallEast = new Image(getClass().getResourceAsStream("Image/fireWallWest.png"),size,size,false,false);
			image_shoot_wallEast = new Image(getClass().getResourceAsStream("Image/fireWallNorth.png"),size,size,false,false);
			image_shoot_wallEast = new Image(getClass().getResourceAsStream("Image/fireWallSouth.png"),size,size,false,false);

			fields = new Label[20][20];
			for (int j=0; j<20; j++) {
				for (int i=0; i<20; i++) {
					switch (board[j].charAt(i)) {
						case 'w':
							fields[i][j] = new Label("", new ImageView(image_wall));
							break;
						case ' ':
							fields[i][j] = new Label("", new ImageView(image_floor));
							break;
						default: throw new Exception("Illegal field value: "+board[j].charAt(i) );
					}
					boardGrid.add(fields[i][j], i, j);
				}
			}
			scoreList.setEditable(false);


			// Leave og join knapper
			HBox btnBox = new HBox();
			leaveBtn.setTranslateX(168);
			leaveBtn.setDisable(true);
			btnBox.getChildren().addAll(nameField, joinBtn, leaveBtn);
			grid.add(btnBox, 0, 2);

			joinBtn.setOnMouseClicked(event -> joinHandler());
			leaveBtn.setOnMouseClicked(event -> {
				try {
					leaveHandler();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			// ---------------------------

			grid.add(mazeLabel,  0, 0);
			grid.add(scoreLabel, 1, 0);
			grid.add(boardGrid,  0, 1);
			grid.add(scoreList,  1, 1);

			Scene scene = new Scene(grid,scene_width,scene_height);
			primaryStage.setScene(scene);
			primaryStage.show();

			scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
				long currentTime = System.currentTimeMillis();
				switch (event.getCode()) {
					case UP:outToServerHandler( "MOVE","up");  break;
					case DOWN:outToServerHandler("MOVE","down");  break;
					case LEFT:outToServerHandler("MOVE","left"); break;
					case RIGHT:outToServerHandler("MOVE","right"); break;
					case SPACE:
						if (currentTime - lastShootTime.get() >= 3000) { // Cooldown of 3 seconds
							outToServerHandler("SHOOT", me.direction);
							lastShootTime.set(currentTime); // Update last shoot time
						}
						break;

					default: break;
				}
			});

			scoreList.setText(getScoreList());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void leaveHandler() throws IOException {
		outToServerHandler("LEAVE", "ud");
		Platform.exit();
	}

	public void leave(String name){
		Player deadPlayer = null;
		for (int i = 0; i < players.size(); i++) {
			Player p = players.get(i);
			if(p.name.equals(name)){
				deadPlayer = p;
			}
		}

		players.remove(deadPlayer);
	}

	private void joinHandler() {
		String name = nameField.getText();
		if (!name.equals("")) {
			createPlayer(name);
			nameField.setText("");
			joinBtn.setDisable(true);
			nameField.setDisable(true);
			leaveBtn.setDisable(false);
		} else {
			Alert al = new Alert(Alert.AlertType.INFORMATION);
			al.setHeaderText("Indtast navn");
			al.show();
		}
	}

	public Player getPlayer(String playName, int X, int Y, String direcktion){
		Player player = null;

		for (int i = 0; i < players.size(); i++) {
			Player p = players.get(i);
			if(p.name.equals(playName)){
				player = p;
			}
		}
		if(player == null){
			player = new Player(playName, X, Y, direcktion);
			players.add(player);
		}
		return player;
	}

 	private void getColor(Player player){
		int index = -1;
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).equals(player)) {
				index = i;
				break;
			}
		}
		hero_right  = new Image(getClass().getResourceAsStream("Image/hero_right"+index+".png"),size,size,false,false);
		hero_left   = new Image(getClass().getResourceAsStream("Image/hero_left"+index+".png"),size,size,false,false);
		hero_up     = new Image(getClass().getResourceAsStream("Image/hero_up"+index+".png"),size,size,false,false);
		hero_down   = new Image(getClass().getResourceAsStream("Image/hero_down"+index+".png"),size,size,false,false);
	}
	public void playerMoved(int delta_x, int delta_y, String direction, Player player) {
		Platform.runLater(() -> {
			int index = -1;
			getColor(player);

			player.direction = direction;
			int x = player.getXpos(), y = player.getYpos();

			// Hvis der er en væg
			if (board[y+delta_y].charAt(x+delta_x)=='w') {
				player.addPoints(-1);
			}
			else {
				// Hvis der er en anden spiller
				Player p = getPlayerAt(x+delta_x,y+delta_y);
				if (p!=null) {
					player.addPoints(10);
					p.addPoints(-10);
				} else {
					// Hvis der er fri plads
					player.addPoints(1);

					fields[x][y].setGraphic(new ImageView(image_floor));
					x+=delta_x;
					y+=delta_y;

					if (direction.equals("right")) {
						fields[x][y].setGraphic(new ImageView(hero_right));
					};
					if (direction.equals("left")) {
						fields[x][y].setGraphic(new ImageView(hero_left));
					};
					if (direction.equals("up")) {
						fields[x][y].setGraphic(new ImageView(hero_up));
					};
					if (direction.equals("down")) {
						fields[x][y].setGraphic(new ImageView(hero_down));
					};
					if (direction.equals("ud")) {
						fields[x][y].setGraphic(new ImageView(image_floor));
					};
					if (direction.equals("ind")) {
						fields[x][y].setGraphic(new ImageView(hero_up));
					};

					player.setXpos(x);
					player.setYpos(y);
				}
			}
			scoreList.setText(getScoreList());
		});
	}

	public String getScoreList() {
		StringBuffer b = new StringBuffer(100);
		for (Player p : players) {
			b.append(p+"\r\n");
		}
		return b.toString();
	}

	public Player getPlayerAt(int x, int y) {
		for (Player p : players) {
			if (p.getXpos()==x && p.getYpos()==y) {
				return p;
			}
		}
		return null;
	}

	public void outToServerHandler(String handling, String direction) {
		try {
			outToServer.writeBytes( handling + " " + me.name + " "+  me.xpos + " " + me.ypos + " " + direction + " " + me.point+ "\n"); // \n HUSK NEWLINE
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}


	public void createPlayer(String name) {
		/*int[] x_posArr = {1, 1, -18, -18};
		int[] y_posArr = {1, 18, 1, -18};

		int i = players.size() % 4; */

		Random rand = new Random();
		int x, y;

		do {
			x = rand.nextInt(20);
			y = rand.nextInt(20);
		} while (board[y].charAt(x) == 'w');


		me = new Player(name,x,y,"up");
		players.add(me);
		getColor(me);
		fields[x][y].setGraphic(new ImageView(hero_up));

		outToServerHandler("MOVE", "ind");
	}


	public void spawn(Player player){
		Random rand = new Random();
		int x, y;

		do {
			x = rand.nextInt(20);
			y = rand.nextInt(20);
		} while (board[y].charAt(x) == 'w');
		fields[player.getXpos()][player.getYpos()].setGraphic(new ImageView(image_floor));
		playerMoved(x, y, "up", player);
		outToServerHandler("MOVE", "ind");

	}

	public void shoot(int delta_x, int delta_y, String direction, Player player){
		Timer timer = new Timer();
		int shootFade = 200;

		Platform.runLater(() -> {
			if (direction.equals("right")) {
				int startX = player.getXpos() + 1;
				int startY = player.getYpos();

				int tempX = player.getXpos() + 1;
				while (board[player.getYpos()].charAt(tempX) != 'w') {
					Player p = getPlayerAt(tempX, startY);
					if (p != null) {
						System.out.println("Hit");
						p.addPoints(-100);
						player.addPoints(100);
//						spawn(p);
					}
					if (board[startY].charAt(tempX + 1) == 'w') {
						fields[tempX][player.getYpos()].setGraphic(new ImageView(image_shoot_wallEast));
					} else {
						fields[tempX][player.getYpos()].setGraphic(new ImageView(image_shoot_horizontal));
					}
					tempX += 1;
				}

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								int tempX = startX;
								while (board[startY].charAt(tempX) != 'w') {
									fields[tempX][startY].setGraphic(new ImageView(image_floor));
									tempX += 1;
								}
							}
						});
					}
				}, shootFade);
			};
			if (direction.equals("left")) {
				int startX = player.getXpos() - 1;
				int startY = player.getYpos();

				int tempX = player.getXpos() -1;
				while (board[player.getYpos()].charAt(tempX) != 'w') {
					Player p = getPlayerAt(tempX, startY);
					if (p != null) {
						System.out.println("Hit");
						p.addPoints(-100);
						player.addPoints(100);
//						spawn(p);
					}
					fields[tempX][player.getYpos()].setGraphic(new ImageView(image_shoot_horizontal));
					tempX -= 1;
				}

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								int tempX = startX;
								while (board[startY].charAt(tempX) != 'w') {
									fields[tempX][startY].setGraphic(new ImageView(image_floor));
									tempX -= 1;
								}
							}
						});
					}
				}, shootFade);
			};
			if (direction.equals("up")) {
				int startX = player.getXpos();
				int startY = player.getYpos() - 1;

				int tempY = player.getYpos() - 1;
				while (board[tempY].charAt(player.getXpos()) != 'w') {
					Player p = getPlayerAt(startX, tempY);
					if (p != null) {
						System.out.println("Hit");
						p.addPoints(-100);
						player.addPoints(100);
//						spawn(p);
					}
					fields[player.getXpos()][tempY].setGraphic(new ImageView(image_shoot_vertical));
					tempY -= 1;
				}

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								int tempY = player.getYpos() - 1;
								while (board[tempY].charAt(player.getXpos()) != 'w') {
									fields[startX][tempY].setGraphic(new ImageView(image_floor));
									tempY -= 1;
								}
							}
						});
					}
				}, shootFade);
			};
			if (direction.equals("down")) {
				int startX = player.getXpos();
				int startY = player.getYpos() + 1;

				int tempY = player.getYpos() + 1;
				while (board[tempY].charAt(player.getXpos()) != 'w') {
					Player p = getPlayerAt(startX, tempY);
					if (p != null) {
						System.out.println("Hit");
						p.addPoints(-100);
						player.addPoints(100);
//						spawn(p);
					}
					fields[player.getXpos()][tempY].setGraphic(new ImageView(image_shoot_vertical));
					tempY += 1;
				}

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								int tempY = player.getYpos() + 1;
								while (board[tempY].charAt(player.getXpos()) != 'w') {
									fields[startX][tempY].setGraphic(new ImageView(image_floor));
									tempY += 1;
								}
							}
						});
					}
				}, shootFade);
			};
		});


	}
}