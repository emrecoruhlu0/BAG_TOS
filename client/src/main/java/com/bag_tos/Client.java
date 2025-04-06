package com.example.tos_client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 1234);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Scanner scanner = new Scanner(System.in);

        if (System.getProperty("os.name").startsWith("Windows")) {
            enableANSIConsole();
        }

        new Thread(() -> {
            try {
                String gelenMesaj;
                while ((gelenMesaj = in.readLine()) != null) {
                    System.out.println(gelenMesaj);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        //System.out.print("Kullanici adi: ");
        //String kullaniciAdi = scanner.nextLine();
        //out.println(kullaniciAdi);

        while (true) {
            String mesaj = scanner.nextLine();
            out.println(mesaj);
        }
    }

    private static void enableANSIConsole() {
        try {
            // CMD/PowerShell'de ANSI desteğini zorla etkinleştir
            new ProcessBuilder("cmd", "/c", "REG ADD HKCU\\CONSOLE /f /v VirtualTerminalLevel /t REG_DWORD /d 1")
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (Exception e) {
            System.out.println("ANSI renk desteği etkinleştirilemedi!");
        }
    }
}