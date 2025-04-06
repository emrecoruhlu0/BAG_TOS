package com.bag_tos;

import java.io.*;
import java.net.Socket;

import static com.bag_tos.MessageUtils.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    //private Lobby lobby;
    private RoomHandler roomHandler;
    private Game game;
    private boolean isAlive = true;

    public ClientHandler(Socket socket, RoomHandler roomHandler) throws IOException {
        this.socket = socket;
        this.roomHandler = roomHandler;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        try {
            out.println("HOŞ GELDINIZ");
            String proposedUsername;
            boolean isUsernameValid;

            do {
                isUsernameValid = true; // Varsayılan olarak geçerli kabul et
                out.println("KULLANICI_ADI:"); // Kullanıcıdan giriş iste
                out.flush(); // Mesajın hemen gitmesini sağla
                proposedUsername = in.readLine();

                // Bağlantı kopması kontrolü
                if (proposedUsername == null) {
                    throw new IOException("Bağlantı kesildi.");
                }

                // Boşluk veya boş giriş kontrolü
                if (proposedUsername.trim().isEmpty()) {
                    out.println(MessageUtils.formatWarning("Kullanıcı adı boş olamaz!"));
                    isUsernameValid = false; // Döngüyü yeniden başlat
                }
                // Kullanıcı adı alınmış mı kontrolü
                else if (roomHandler.isUsernameTaken(proposedUsername)) {
                    out.println(MessageUtils.formatWarning("Bu kullanıcı adı zaten alındı!"));
                    isUsernameValid = false;
                }

            } while (!isUsernameValid); // Geçerli bir isim alana kadar döngü

            username = proposedUsername;
            roomHandler.addUsername(username);
            roomHandler.addToRoom("LOBBY", this);
            game = roomHandler.game;

            // Mesajları dinle
            String message;
            while ((message = in.readLine()) != null) {
                if (!isAlive) {
                    out.println("[HATA] Ölüsünüz, işlem yapamazsınız!");
                    continue;
                }
                if (message.startsWith("/")) {
                    //System.out.println("[DEBUG] Alınan komut: " + message);
                    if (message.startsWith("/mafya ")) {
                        handleMafiaCommand(message);
                    } else if (message.startsWith("/ready") || message.startsWith("/start")) {
                        if (message.startsWith("/ready")) {
                            roomHandler.readyCount++;
                            System.out.println("ready istegi: " + roomHandler.readyCount);
                        } else if (message.startsWith("/start")) {
                            roomHandler.startCount++;
                            System.out.println("start istegi: " + roomHandler.readyCount);
                        }
                        roomHandler.readyCountHandle();
                    } else if (game != null) {
                        if (game.getCurrentPhase() == Game.Phase.NIGHT) {
                            handleNightCommand(message);
                        } else {
                            handleDayCommand(message);
                        }
                    }
                } else {
                    handleGeneralMessage(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Hata: " + username + " baglantisi kesildi.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleDayCommand(String message) {
        if (message.startsWith("/oyla ")) {
            String target = message.split(" ")[1];
            game.handleVote(username, target);
        }
    }

    private void handleNightCommand(String message) {
        if (message.startsWith("/oldur ") || message.startsWith("/iyilestir ")) {
            game.handleAction(username, message);
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    private void handleMafiaCommand(String message) {
        if (!isMafia()) {
            sendMessage("Bu komutu kullanma yetkiniz yok!");
            return;
        }
        String cleanMessage = message.replaceFirst("/mafya ", "");
        // Özel mesajı MAFYA odasına gönder
        roomHandler.broadcastToRoom("MAFYA", "🔮 [MAFYA] " + getUsername() + ": " + cleanMessage);
    }

    private void handleGeneralMessage(String message) {
        // Genel mesajı LOBBY'e gönder

        if (game.getCurrentPhase() == Game.Phase.DAY) {
            roomHandler.broadcastToRoom("LOBBY", getUsername() + ": " + message);
        } else {
            sendMessage(formatError("gece mesaj gönderemezsin"));
        }
    }

    private boolean isMafia() {
        return game.getRole(getUsername()) instanceof Mafya;
    }

    public Game getGame() {
        return game;
    }
}
