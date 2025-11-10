package Client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class MainClientt extends JFrame {
    private JTextField usernameField, toField, subjectField;
    private JTextArea contentArea;
    private JButton createBtn, loginBtn, sendBtn, refreshBtn;
    private JPasswordField passwordField;
    private DatagramSocket socket;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9999;
    private String currentUser = null;

    public MainClientt() {
        setTitle("Mail Client - Real SMTP");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể tạo socket: " + e.getMessage());
            System.exit(1);
        }

        initComponents();
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Đăng ký/Đăng nhập
        JPanel authPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        authPanel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        authPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        authPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        authPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        createBtn = new JButton("Tạo tài khoản");
        createBtn.addActionListener(e -> createAccount());
        authPanel.add(createBtn, gbc);

        gbc.gridx = 1;
        loginBtn = new JButton("Đăng nhập");
        loginBtn.addActionListener(e -> login());
        authPanel.add(loginBtn, gbc);

        tabbedPane.addTab("Đăng nhập", authPanel);

        // Tab 2: Gửi email THẬT
        JPanel sendPanel = new JPanel(new BorderLayout(5, 5));
        JPanel sendFormPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        sendFormPanel.add(new JLabel("Đến (Email):"), gbc);
        gbc.gridx = 1;
        toField = new JTextField(20);
        toField.setToolTipText("Nhập email thật: example@gmail.com");
        sendFormPanel.add(toField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        sendFormPanel.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1;
        subjectField = new JTextField(20);
        sendFormPanel.add(subjectField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel noteLabel = new JLabel("<html><b>Lưu ý:</b> Email sẽ được gửi thật qua SMTP!</html>");
        noteLabel.setForeground(Color.RED);
        gbc.gridwidth = 2;
        sendFormPanel.add(noteLabel, gbc);

        sendPanel.add(sendFormPanel, BorderLayout.NORTH);

        contentArea = new JTextArea(10, 30);
        sendPanel.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        sendBtn = new JButton("Gửi Email Thật");
        sendBtn.setBackground(new Color(34, 139, 34));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("Arial", Font.BOLD, 14));
        sendBtn.addActionListener(e -> sendEmail());
        sendPanel.add(sendBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Gửi Email", sendPanel);

        // Tab 3: Hộp thư
        JPanel inboxPanel = new JPanel(new BorderLayout(5, 5));
        
        DefaultListModel<String> emailListModel = new DefaultListModel<>();
        JList<String> emailList = new JList<>(emailListModel);
        emailList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inboxPanel.add(new JScrollPane(emailList), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        refreshBtn = new JButton("Làm mới");
        refreshBtn.addActionListener(e -> refreshInbox(emailListModel));
        buttonPanel.add(refreshBtn);
        
        JButton openBtn = new JButton("Mở Email");
        openBtn.addActionListener(e -> openEmail(emailList.getSelectedValue()));
        buttonPanel.add(openBtn);
        
        inboxPanel.add(buttonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Hộp thư", inboxPanel);

        add(tabbedPane);
    }

    private void createAccount() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập username và password!");
            return;
        }

        String response = sendRequest("CREATE|" + username + "|" + password);
        String[] parts = response.split("\\|", 2);
        
        if (parts[0].equals("SUCCESS")) {
            JOptionPane.showMessageDialog(this, parts[1]);
            usernameField.setText("");
            passwordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, parts[1], "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập username và password!");
            return;
        }

        String response = sendRequest("LOGIN|" + username + "|" + password);
        String[] parts = response.split("\\|", 2);
        
        if (parts[0].equals("SUCCESS")) {
            currentUser = username;
            setTitle("Mail Client - " + currentUser + " (SMTP Enabled)");
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công!\n\nBạn có thể gửi email đến bất kỳ địa chỉ nào!");
        } else {
            JOptionPane.showMessageDialog(this, parts[1], "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendEmail() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập trước!");
            return;
        }

        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String content = contentArea.getText().trim();

        if (to.isEmpty() || subject.isEmpty() || content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin!");
            return;
        }

        // Kiểm tra định dạng email
        if (!to.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            JOptionPane.showMessageDialog(this, 
                "Địa chỉ email không hợp lệ!\nVí dụ: user@gmail.com", 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Xác nhận gửi
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn gửi email THẬT đến:\n" + to + "?",
            "Xác nhận gửi email",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Hiển thị loading
        JDialog loadingDialog = new JDialog(this, "Đang gửi...", true);
        JLabel loadingLabel = new JLabel("Đang gửi email, vui lòng đợi...", JLabel.CENTER);
        loadingDialog.add(loadingLabel);
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(this);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                String request = "SEND|" + currentUser + "|" + to + "|" + subject + "|" + content;
                return sendRequest(request);
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    String response = get();
                    String[] parts = response.split("\\|", 2);
                    
                    if (parts[0].equals("SUCCESS")) {
                        JOptionPane.showMessageDialog(MainClientt.this, 
                            parts[1], 
                            "Thành công", 
                            JOptionPane.INFORMATION_MESSAGE);
                        toField.setText("");
                        subjectField.setText("");
                        contentArea.setText("");
                    } else {
                        JOptionPane.showMessageDialog(MainClientt.this, 
                            parts[1], 
                            "Lỗi", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MainClientt.this, 
                        "Lỗi: " + e.getMessage(), 
                        "Lỗi", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }

    private void refreshInbox(DefaultListModel<String> emailListModel) {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập trước!");
            return;
        }
        
        String response = sendRequest("LOGIN|" + currentUser + "|" + new String(passwordField.getPassword()));
        String[] parts = response.split("\\|", 2);
        
        if (parts[0].equals("SUCCESS")) {
            emailListModel.clear();
            if (parts.length > 1 && !parts[1].isEmpty()) {
                String[] files = parts[1].split(";");
                for (String file : files) {
                    emailListModel.addElement(file);
                }
                JOptionPane.showMessageDialog(this, "Đã làm mới! Có " + files.length + " email");
            } else {
                JOptionPane.showMessageDialog(this, "Hộp thư trống");
            }
        }
    }

    private void openEmail(String filename) {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập trước!");
            return;
        }
        
        if (filename == null || filename.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn email để mở!");
            return;
        }
        
        String response = sendRequest("READ|" + currentUser + "|" + filename);
        String[] parts = response.split("\\|", 2);
        
        if (parts[0].equals("SUCCESS")) {
            JTextArea emailContent = new JTextArea(parts[1]);
            emailContent.setEditable(false);
            emailContent.setFont(new Font("Monospaced", Font.PLAIN, 12));
            emailContent.setCaretPosition(0);
            
            JScrollPane scrollPane = new JScrollPane(emailContent);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            
            JOptionPane.showMessageDialog(this, scrollPane, "Email: " + filename, JOptionPane.PLAIN_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, parts[1], "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String sendRequest(String request) {
        try {
            byte[] sendData = request.getBytes();
            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            socket.send(sendPacket);

            byte[] receiveData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (Exception e) {
            return "ERROR|Lỗi kết nối: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainClientt().setVisible(true);
        });
    }
}
