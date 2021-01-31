package Leader;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * SERVER This is the ServerThread class that has a socket where we accept
 * clients contacting us. We save the clients ports connecting to the server
 * into a List in this class. When we wand to send a message we send it to all
 * the listening ports
 */

public class ServerThread extends Thread {
	boolean isLeader;
	boolean leaderActive;
	private String portNum;
	private ServerSocket serverSocket;
	private int leaderNumber;
	private int peerNum;
	private JSONArray peerInfo;
	private String leaderInfo;
	private ArrayList<String> peerLocations = new ArrayList<>();
	private Set<Socket> listeningSockets = new HashSet<Socket>();

	public ServerThread(String portNum) throws IOException {
		this.portNum = portNum;
		serverSocket = new ServerSocket(Integer.valueOf(portNum));
	}

	/**
	 * Starting the thread, we are waiting for clients wanting to talk to us, then
	 * save the socket in a list
	 */
	public void run() {
		Socket sock = null;
		try {
			while (true) {
				leaderActive = false;
				sock = serverSocket.accept();
				JSONObject message = read(sock);
				String messageType = message.getString("messagetype");
				switch (messageType ) {
					case "leaderconnect":
						peerInfo = message.getJSONArray("data");
						leaderNumber = message.getJSONArray("data").length() - 1;
						peerNum = message.getInt("peernum");
						respond(sock, respondToLeader());
						break;
					case "calc":
						JSONObject response = calcResponse(message.getJSONObject("data"));
						System.out.println("Recieved calc request");
						System.out.println("Data: " + message.getJSONObject("data").toString());
						respond(sock, response);
						break;
					case "newleader":
						JSONObject leader = message.getJSONObject("data");
						leaderNumber = leader.getInt("leadernumber");
						if (leaderNumber == peerNum) {
							isLeader = true;
							System.out.println("You are the new leader, type l to accept your role");
						} else {
							isLeader = false;
						}
						respond(sock, respondToLeader());
					case "heartbeat":
						JSONObject beatResponse = new JSONObject();
						beatResponse.put("messagetype", "beatresponse");
						respond(sock, beatResponse);
						leaderActive = true;
						Thread.sleep(3000);
				}
				sock.close();
			}
		} catch (SocketException | EOFException e) {
			// client disconnect
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (sock != null)
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	public static JSONObject calcResponse(JSONObject data) {
		JSONObject response = new JSONObject();
		response.put("messagetype", "calcresponse");
		int num1 = data.getInt("num1");
		int num2 = data.getInt("num2");
		int ans;
		switch (data.getString("op")) {
			case "+":
				ans = num1 + num2;
				response.put("ans", ans);
				break;
			case "-":
				ans = num1 - num2;
				response.put("ans", ans);
				break;
			case "*":
				ans = num1 * num2;
				response.put("ans", ans);
				break;
			case "/":
				ans = num1 / num2;
				response.put("ans", ans);
				break;
		}
		return response;
	}
	
	public static JSONObject respondToLeader() {
		JSONObject response = new JSONObject();
		response.put("messagetype", "ack");
		response.put("data", "got it");
		return response;
	}

	public static JSONObject read(Socket conn) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = in.readLine();
		JSONTokener tokener = new JSONTokener(line);
		JSONObject root = new JSONObject(tokener);
		return root;
	}

	public static void respond(Socket conn, JSONObject message) throws IOException {
		PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
		out.println(message.toString());
	}

	/**
	 * Sending the message to the OutputStream for each socket that we saved
	 */
	void sendMessage(String message) {
		try {
			for (Socket s : listeningSockets) {
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				out.println(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String getPortNum() {
		return portNum;
	}
	
	int getLeaderNum() {
		return leaderNumber;
	}
	
	
	JSONArray getPeerInfo() throws InterruptedException {
		while (peerInfo == null) {
			Thread.sleep(1);
		}
		return peerInfo;
	}
	
}
