import java.awt.event.*;
import javax.swing.*;
import java.net.*;
public class ChatUDPJFrame extends JFrame implements ActionListener
{
    private String name;
    private InetAddress destip;
    private int destport;
    private JTextArea text_receiver;
    private JTextField text_sender;
    public ChatUDPJFrame(String name, String host, int destport, int receiveport) throws Exception
    {
        super("聊天室  " + name + "  " + InetAddress.getLocalHost() + ":" + receiveport);
        this.setBounds(320, 240, 400, 240);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.text_receiver = new JTextArea();
        this.text_receiver.setEditable(false);
        this.getContentPane().add(new JScrollPane(this.text_receiver));
        JPanel panel = new JPanel();
        this.getContentPane().add(panel, "South");
        this.text_sender = new JTextField(20);
        panel.add(this.text_sender);
        JButton button_send = new JButton("发送");
        panel.add(button_send);
        button_send.addActionListener(this);
        this.setVisible(true);
        this.name = name;
        this.destip = InetAddress.getByName(host);
        this.destport = destport;
        byte data[] = new byte[512];
        DatagramPacket pack = new DatagramPacket(data, data.length);
        DatagramSocket socket = new DatagramSocket(receiveport);
        while (socket != null) {
            socket.receive(pack);
            int length = pack.getLength();
            String message = new String(pack.getData(), 0, length);
            text_receiver.append(message + "\r\n");
        }
    }
    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "发送") {
            byte buffer[] = (name + "说：" + text_sender.getText()).getBytes();
            try {
                DatagramPacket pack = new DatagramPacket(buffer, buffer.length, destip, destport);
                new DatagramSocket().send(pack);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            text_receiver.append("我说：" + text_sender.getText() + "\n");
            text_sender.setText("");
        }
    }
    public static void main(String args[]) throws Exception
    {
        String name = JOptionPane.showInputDialog("请输入你的网名：");
        String IPHost = JOptionPane.showInputDialog("请输入对方的IP地址：");
        int destport = Integer.parseInt(JOptionPane.showInputDialog("请输入你使用的端口号："));
        int receiveport = Integer.parseInt(JOptionPane.showInputDialog("请输入接收方的端口号："));
        new ChatUDPJFrame(name, IPHost, destport, receiveport);
    }
}
