import java.awt.event.*;
import javax.swing.*;
import java.net.*;
public class ChatUDPJFrame extends JFrame implements ActionListener
{
    private String name;
    private int port;
    private JTextArea text_receiver;
    private JTextField text_sender;
	private String prefix;
	private int self;
    public ChatUDPJFrame(String name) throws Exception
    {
        super("聊天室  " + name + "  " + InetAddress.getLocalHost());
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
        this.port = 8080;

        InetAddress localHost;
        String localip = "";
        int NetworkPrefixLength;
        try {
            localHost = Inet4Address.getLocalHost();
            localip = localHost.getHostAddress();
            NetworkInterface networkInterface;
            try {
                networkInterface = NetworkInterface.getByInetAddress(localHost);
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    NetworkPrefixLength = address.getNetworkPrefixLength();
                }
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String[] ipSub = localip.split("\\.");
        prefix = ipSub[0] + "." + ipSub[1] + "." + ipSub[2] + ".";
        self = Integer.parseInt(ipSub[3]);

        byte data[] = new byte[512];
        DatagramPacket pack = new DatagramPacket(data, data.length);
        DatagramSocket socket = new DatagramSocket(port);
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
                for (int i = 1; i < self; ++i) {
                    DatagramPacket pack = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(prefix + i), port);
                    new DatagramSocket().send(pack);
                }
                for (int i = self+1; i < 255; ++i) {
                    DatagramPacket pack = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(prefix + i), port);
                    new DatagramSocket().send(pack);
                }
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
        new ChatUDPJFrame(name);
    }
}
