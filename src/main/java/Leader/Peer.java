package Leader;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * This is the main class for the peer2peer program. It starts a client with a
 * username and port. Next the peer can decide who to listen to. So this
 * peer2peer application is basically a subscriber model, we can "blurt" out to
 * anyone who wants to listen and we can decide who to listen to. We cannot
 * limit in here who can listen to us. So we talk publicly but listen to only
 * the other peers we are interested in.
 * 
 */

public class Peer {
	private String username;
	private int peerNumber;
	private int leaderNumber;
	private BufferedReader bufferedReader;
	private ServerThread serverThread;
	private ArrayList<String> peerLocations = new ArrayList<>();
	private String leaderLocation;
	private int port;

	public Peer(BufferedReader bufReader, String username, ServerThread serverThread) {
		this.username = username;
		this.bufferedReader = bufReader;
		this.serverThread = serverThread;
	}

	/**
	 * Main method saying hi and also starting the Server thread where other peers
	 * can subscribe to listen
	 *
	 * @param args[0] username
	 * @param args[1] port for server
	 */
	public static void main(String[] args) throws Exception {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String username = args[0];
		System.out.println("Hello " + username + " and welcome! Your port will be " + args[1]);

		// starting the Server Thread, which waits for other peers to want to connect
		ServerThread serverThread = new ServerThread(args[1]);
		serverThread.start();
		Peer peer = new Peer(bufferedReader, args[0], serverThread);
		if (args.length > 2) {
			// Initialize string of peers to establish connections
			String peers = "";
			for (int i = 2; i < args.length; i += 2) {
				peers += args[i] + ":" + args[i + 1] + " ";
			}
			System.out.println("You are the leader");
			peer.connectToPeers(peers);
		}
		peer.connectToPeers();
	}

	public void connectToPeers() throws Exception {
		JSONArray peerInfo = serverThread.getPeerInfo();
		System.out.println("You have been connected to a leader");
		System.out.println("Peer Info: " + peerInfo.toString());
		for (var i : peerInfo) {
			peerLocations.add((String) i);
		}
		leaderNumber = peerLocations.size() - 1;
//		for (int i = 0; i < peerLocations.length; i++) {
//			String[] address = peerLocations[i].split(":");
//			Socket socket = null;
//			try {
//				socket = new Socket(address[0], Integer.valueOf(address[1]));
//				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//				out.println();
//				new ClientThread(socket).start();
//				System.out.println("Connected to " + address[0] + " at " + address[1]);
//			} catch (Exception c) {
//				if (socket != null) {
//					socket.close();
//				} else {
//					System.out.println("Cannot connect, wrong input");
//					System.out.println("Exiting: I know really user friendly");
//					System.exit(0);
//				}
//			}
//		}
		askForInput();
	}

	/**
	 * Takes in a string containing host and port information for the leader to
	 * connect to peers specified in command line args
	 * 
	 * @param peers string to be parsed into peer information
	 * @throws Exception
	 */
	public void connectToPeers(String peers) throws Exception {
		serverThread.isLeader = true;
		String[] setupValue = peers.split(" ");
		leaderNumber = setupValue.length;
		for (int i = 0; i < setupValue.length; i++) {
			peerLocations.add(setupValue[i] + ":" + i);
			String[] address = setupValue[i].split(":");
			Socket socket = null;
			try {
				socket = new Socket(address[0], Integer.valueOf(address[1]));
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				JSONObject message = new JSONObject();
				JSONArray peerInfo = new JSONArray();
				for (int j = 0; j < setupValue.length; j++) {
					peerInfo.put(setupValue[j] + ":" + j);
					if (j == i) {
						message.put("peernum", j);
					} 
				}
				peerInfo.put(InetAddress.getLocalHost().getHostAddress() + ":" + serverThread.getPortNum() + ":" + setupValue.length);
				message.put("messagetype", "leaderconnect");
				message.put("data", peerInfo);
				out.println(message);
				String response = in.readLine();
				JSONTokener tokener = new JSONTokener(response);
				JSONObject recieved = new JSONObject(tokener);
				System.out.println("Message recieved from " + address[0] + " at port: " + address[1] + "::"
						+ recieved.getString("data"));
				System.out.println("Connected to " + peerLocations.get(i));
			} catch (SocketException | EOFException e) {
				// client disconnect
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (socket != null)
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
		sendHeartbeats();
	}

	public JSONObject createMessage(String type, JSONObject data) {
		JSONObject message = new JSONObject();
		message.put("messagetype", type);
		message.put("data", data);
		return message;
	}

	/**
	 * User is asked to define who they want to subscribe/listen to Per default we
	 * listen to no one
	 * @throws Exception 
	 *
	 */
//	public void updateListenToPeers() throws Exception {
//		System.out.println("> Who do you want to listen to? Enter host:port");
//		String input = bufferedReader.readLine();
//		String[] setupValue = input.split(" ");
//		for (int i = 0; i < setupValue.length; i++) {
//			String[] address = setupValue[i].split(":");
//			Socket socket = null;
//			try {
//				socket = new Socket(address[0], Integer.valueOf(address[1]));
//				new ClientThread(socket).start();
//			} catch (Exception c) {
//				if (socket != null) {
//					socket.close();
//				} else {
//					System.out.println("Cannot connect, wrong input");
//					System.out.println("Exiting: I know really user friendly");
//					System.exit(0);
//				}
//			}
//		}
//
//		askForInput();
//	}

	
	public void setLeader() throws Exception {
		peerLocations.remove(leaderNumber);
		int max = 0;
		for (int i = 0; i < peerLocations.size(); i++) {
			String[] pInfo = peerLocations.get(i).split(":");
			if (max < Integer.parseInt(pInfo[2])) {
				max = Integer.parseInt(pInfo[2]);
			}
		}
		leaderNumber = max;
		JSONObject data = new JSONObject();
		data.put("leadernumber", leaderNumber);
		JSONObject message = createMessage("newleader", data);
		for (var p : peerLocations) {
			NetworkUtils.send(p, message);
		}
		askForInput();
	}
	
	public void sendHeartbeats() throws Exception {
		JSONObject beat = new JSONObject();
		beat.put("messagetype", "heartbeat");
		while (serverThread.isLeader) {
			for (int i = 0; i < peerLocations.size(); i++) {
				if (i != leaderNumber) {
					NetworkUtils.send(peerLocations.get(i), beat);
				}
			}
			Thread.sleep(2000);
		}
	}

	/**
	 * Client waits for user to input their message or quit
	 *
	 * @param bufReader    bufferedReader to listen for user entries
	 * @param username     name of this peer
	 * @param serverThread server thread that is waiting for peers to sign up
	 */
	public void askForInput() throws Exception {
		try {
			System.out
					.println("> Send a simple calculation with format: calc [num1] [operator] [num2] e.g. calc 3 * 3");
			while (true) {
				int i = 0;
				Thread.sleep(1000);
				while (!serverThread.leaderActive) {
					i++;
					Thread.sleep(10);
					if (i == 100) {
						setLeader();
					}
				}
				String message = bufferedReader.readLine();
				if (!serverThread.isLeader && serverThread.leaderActive) {
					String[] messageParts = message.split(" ");
					if (messageParts[0].equals("calc")) {
						JSONObject data = new JSONObject();
						data.put("op", messageParts[2]);
						data.put("num1", Integer.parseInt(messageParts[1]));
						data.put("num2", Integer.parseInt(messageParts[3]));
						JSONObject calc = createMessage("calc", data);
						JSONObject response = null;
						response = NetworkUtils.send(peerLocations.get(serverThread.getLeaderNum()), calc);
						if (response == null) {
							System.out.println("Null response");
							setLeader();
						}
						System.out.println("Answer : " + response.getInt("ans"));
					} else {
						System.out.println("Invalid input, try again");
					} 
				} else if (serverThread.isLeader){
					sendHeartbeats();
				} else {
					setLeader();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
