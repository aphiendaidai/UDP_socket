package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailServerr {
    private static final int PORT = 9999;
    private static final String MAIL_DIR = "mail_storage";
    private DatagramSocket socket;
    
    // Cấu hình SMTP - SỬA THÔNG TIN NÀY
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_USERNAME = "aphien629@gmail.com"; // Email của bạn
    private static final String EMAIL_PASSWORD = "rqjo esgb aqby yhpp";// App password
    
    public MailServerr() {
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
            System.out.println("Mail Server đang chạy trên cổng " + PORT);
            System.out.println("SMTP Server: " + SMTP_HOST);

            byte[] buffer = new byte[4096];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                System.out.println("[" + new Date() + "] Nhận từ " + clientAddress + ":" + clientPort);
                System.out.println("Request: " + message);

                String response = processRequest(message, clientAddress);
                sendResponse(response, clientAddress, clientPort);
                
                System.out.println("Response: " + response);
                System.out.println("-----------------------------------\n");
            }
        } catch (Exception e) {
            System.err.println("Lỗi server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String processRequest(String message, InetAddress clientAddress) {
        String[] parts = message.split("\\|");
        String command = parts[0];

        try {
            switch (command) {
                case "CREATE":
                    return createAccount(parts[1], parts[2]);
                case "LOGIN":
                    return login(parts[1], parts[2]);
                case "SEND":
                    return sendRealEmail(parts[1], parts[2], parts[3], parts[4], clientAddress);
                case "READ":
                    return readEmail(parts[1], parts[2]);
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

        if (!userDir.mkdir()) {
            return "ERROR|Không thể tạo thư mục người dùng";
        }
        
        File passwordFile = new File(userDir, ".password");
        try (PrintWriter writer = new PrintWriter(passwordFile, "UTF-8")) {
            writer.println(password);
        } catch (Exception e) {
            userDir.delete();
            return "ERROR|Không thể lưu mật khẩu";
        }

        File welcomeFile = new File(userDir, "new_email.txt");
        try (PrintWriter writer = new PrintWriter(welcomeFile, "UTF-8")) {
            String creationDate = new Date().toString();
            writer.println("--- THÔNG TIN TÀI KHOẢN ---");
            writer.println("Tên tài khoản: " + username);
            writer.println("Mật khẩu: " + password);
            writer.println("Ngày tạo: " + creationDate);
            writer.println("-----------------------------");
            writer.println();
            writer.println("Welcome! Hệ thống email này giờ có thể gửi email thật!");
            writer.flush();
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không thể tạo file welcome: " + e.getMessage());
        }

        System.out.println("✓ Đã tạo tài khoản: " + username);
        return "SUCCESS|Tạo tài khoản thành công!";
    }

    private String login(String username, String password) {
        File userDir = new File(MAIL_DIR, username);
        
        if (!userDir.exists()) {
            return "ERROR|Tài khoản không tồn tại!";
        }

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
            if (!file.getName().equals(".password")) {
                fileList.append(file.getName()).append(";");
            }
        }

        System.out.println("✓ User " + username + " đăng nhập (" + (files.length - 1) + " email)");
        return fileList.toString();
    }

    /**
     * GỬI EMAIL THẬT QUA SMTP
     */
    private String sendRealEmail(String from, String to, String subject, String content, InetAddress clientAddress) {
        // Kiểm tra định dạng email
        if (!to.contains("@")) {
            return "ERROR|Địa chỉ email người nhận không hợp lệ! Cần có dạng: user@example.com";
        }

        try {
            // Cấu hình properties cho JavaMail
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);

            // Tạo session với authentication
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD);
                }
            });

            // Tạo message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_USERNAME, from + " (via Mail System)"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            
            // Nội dung email (có thể dùng HTML)
            String emailBody = "Từ: " + from + "\n"
                             + "IP: " + clientAddress.getHostAddress() + "\n"
                             + "Ngày: " + new Date() + "\n"
                             + "-------------------\n\n"
                             + content;
            
            message.setText(emailBody);

            // GỬI EMAIL
            Transport.send(message);

            // Lưu lại bản copy vào thư mục người nhận (nếu là user nội bộ)
            saveEmailCopy(from, to, subject, content, clientAddress);

            System.out.println("✓ Đã gửi email thật từ " + from + " đến " + to);
            return "SUCCESS|Email đã được gửi thành công đến " + to;

        } catch (MessagingException e) {
            System.err.println("✗ Lỗi gửi email: " + e.getMessage());
            e.printStackTrace();
            return "ERROR|Không thể gửi email: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR|Lỗi hệ thống: " + e.getMessage();
        }
    }

    /**
     * Lưu bản copy email vào thư mục nếu người nhận là user nội bộ
     */
    private void saveEmailCopy(String from, String to, String subject, String content, InetAddress clientAddress) {
        // Lấy username từ email (phần trước @)
        String toUsername = to.contains("@") ? to.substring(0, to.indexOf("@")) : to;
        File toUserDir = new File(MAIL_DIR, toUsername);

        if (toUserDir.exists()) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = "email_" + from + "_" + timestamp + ".txt";
            File emailFile = new File(toUserDir, filename);

            try (PrintWriter writer = new PrintWriter(emailFile, "UTF-8")) {
                writer.println("From: " + from);
                writer.println("Sender IP: " + clientAddress.getHostAddress());
                writer.println("To: " + to);
                writer.println("Subject: " + subject);
                writer.println("Date: " + new Date());	
                writer.println("-------------------");
                writer.println(content);
                System.out.println("  → Đã lưu copy vào hộp thư nội bộ của " + toUsername);
            } catch (Exception e) {
                System.err.println("  → Không thể lưu copy: " + e.getMessage());
            }
        }
    }

    private String readEmail(String username, String filename) {
        File emailFile = new File(MAIL_DIR, username + "/" + filename);
        
        if (!emailFile.exists()) {
            return "ERROR|File không tồn tại!";
        }
        
        try {
            StringBuilder content = new StringBuilder("SUCCESS|");
            BufferedReader reader = new BufferedReader(new FileReader(emailFile));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            System.out.println("✓ User " + username + " đọc email: " + filename);
            return content.toString();
        } catch (Exception e) {
            return "ERROR|Không thể đọc file: " + e.getMessage();
        }
    }

    private void sendResponse(String response, InetAddress address, int port) {
        try {
            byte[] data = response.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  REAL MAIL SERVER - SMTP ENABLED");
        System.out.println("===========================================");
        System.out.println("QUAN TRỌNG: Cấu hình SMTP trước khi chạy!");
        System.out.println("1. Sửa EMAIL_USERNAME và EMAIL_PASSWORD");
        System.out.println("2. Gmail: Bật 2-Step Verification và tạo App Password");
        System.out.println("   https://myaccount.google.com/apppasswords");
        System.out.println("===========================================\n");
        
        new MailServerr();
    }
}
