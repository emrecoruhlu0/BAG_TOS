package com.bag_tos;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    public static void main(String[] args) throws IOException {
        int PORT = 1234;
        ServerSocket serverSocket = new ServerSocket(PORT);
        List<ClientHandler> clients = new ArrayList<>();
        RoomHandler roomHandler = new RoomHandler();
        //Lobby lobby = new Lobby();

        System.out.println("Sunucu basladi. Port: " + PORT);

        while (true) {
            ClientHandler clientHandler = new ClientHandler(serverSocket.accept(), roomHandler);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }
}


