package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

public class MailServer{
    private static final int PORT = 9999;
    private static final String MAIL_DIR = "mail_storage";
    private DatagramSocket socket;
    
    public MailServer() {
        createMailDirectory();
        startServer();
    }

    private void createMailDirectory() {
        File dir = new File(MAIL_DIR);
        if (!dir.exists()) {
            dir.mkdir();
            System.out.println("Đã tạo thư mục lưu trữ: " + MAIL_DIR);
        }
    }

    private void startServer() {
        try {
            socket = new DatagramSocket(PORT);
            System.out.println("===================================");
            System.out.println("Mail Server đang chạy trên cổng " + PORT);
            System.out.println("===================================\n");

            byte[] buffer = new byte[4096];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                System.out.println("[" + new Date() + "] Nhận từ " + clientAddress + ":" + clientPort);
                System.out.println("Request: " + message);

                String response = processRequest(message);
                sendResponse(response, clientAddress, clientPort);

                System.out.println("Response: " + response);
                System.out.println("-----------------------------------\n");
            }
        } catch (Exception e) {
            System.err.println("Lỗi server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private String processRequest(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];

        try {
            switch (command) {
                case "CREATE":
                    return createAccount(parts[1], parts[2]);
                case "LOGIN":
                    return login(parts[1], parts[2]);
                case "SEND":
                    return sendEmail(parts[1], parts[2], parts[3], parts[4]);
                default:
                    return "ERROR|Lệnh không hợp lệ";
            }
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String createAccount(String username, String password) {
        File userDir = new File(MAIL_DIR, username);
        
        if (userDir.exists()) {
            return "ERROR|Tài khoản đã tồn tại!";
        }

        userDir.mkdir();
        
        // Lưu password vào file
        File passwordFile = new File(userDir, ".password");
        try (PrintWriter writer = new PrintWriter(passwordFile, "UTF-8")) {
            writer.println(password);
        } catch (Exception e) {
            return "ERROR|Không thể lưu mật khẩu";
        }
        
        // Tạo file new_email.txt với nội dung chào mừng
        File welcomeFile = new File(userDir, "new_email.txt");
        try (PrintWriter writer = new PrintWriter(welcomeFile, "UTF-8")) {
            writer.println("Thank you for using this service. We hope that you will feel comfortable........");
        } catch (Exception e) {
            return "ERROR|Không thể tạo file chào mừng";
        }

        System.out.println("✓ Đã tạo tài khoản: " + username);
        return "SUCCESS|Tạo tài khoản thành công!";
    }
    private String login(String username, String password) {
        File userDir = new File(MAIL_DIR, username);
        
        if (!userDir.exists()) {
            return "ERROR|Tài khoản không tồn tại!";
        }

        // Kiểm tra password
        File passwordFile = new File(userDir, ".password");
        try (BufferedReader reader = new BufferedReader(new FileReader(passwordFile))) {
            String savedPassword = reader.readLine();
            if (!password.equals(savedPassword)) {
                return "ERROR|Mật khẩu không đúng!";
            }
        } catch (Exception e) {
            return "ERROR|Không thể đọc mật khẩu";
        }

        File[] files = userDir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("✓ User " + username + " đăng nhập (hộp thư trống)");
            return "SUCCESS|";
        }

        StringBuilder fileList = new StringBuilder("SUCCESS|");
        for (File file : files) {
            // Không gửi file .password về client
            if (!file.getName().equals(".password")) {
                fileList.append(file.getName()).append(";");
            }
        }

        System.out.println("✓ User " + username + " đăng nhập (" + (files.length - 1) + " email)");
        return fileList.toString();
    }

    private String sendEmail(String from, String to, String subject, String content) {
        File toUserDir = new File(MAIL_DIR, to);
        
        if (!toUserDir.exists()) {
            return "ERROR|Người nhận không tồn tại!";
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "email_" + from + "_" + timestamp + ".txt";
        File emailFile = new File(toUserDir, filename);

        try (PrintWriter writer = new PrintWriter(emailFile, "UTF-8")) {
            writer.println("From: " + from);
            writer.println("To: " + to);
            writer.println("Subject: " + subject);
            writer.println("Date: " + new Date());	
            writer.println("-------------------");
            writer.println(content);
        } catch (Exception e) {
            return "ERROR|Không thể gửi email";
        }

        return "SUCCESS|Gửi email thành công!";
    }

    private void sendResponse(String response, InetAddress address, int port) {
        try {
            byte[] data = response.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) {
     new MailServer();
     
    }
}
