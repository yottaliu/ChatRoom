import java.awt.event.*;
import javax.swing.*;
import java.net.*;
import java.util.*;

public class ChatUDPJFrame extends JFrame implements ActionListener
{
    // 网名
    private String name;
    // 端口
    private int port = 8080;
    // 消息接受文本区
    private JTextArea text_receiver;
    // 发送消息的文本域
    private JTextField text_sender;
    // 存放在线主机的集合
    private Set<InetAddress> hosts = new HashSet<InetAddress>();

    /**
     * Convert a IPv4 address from an InetAddress to an integer
     * @param inetAddr is an InetAddress corresponding to the IPv4 address
     * @return the IP address as an integer in network byte order
     */
    public static int inetAddressToInt(InetAddress inetAddr)
            throws IllegalArgumentException {
        byte [] addr = inetAddr.getAddress();
        if (addr.length != 4) {
            throw new IllegalArgumentException("Not an IPv4 address");
        }
        return ((addr[3] & 0xff) << 24) | ((addr[2] & 0xff) << 16) |
                ((addr[1] & 0xff) << 8) | (addr[0] & 0xff);
    }

    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                                (byte)(0xff & (hostAddress >> 8)),
                                (byte)(0xff & (hostAddress >> 16)),
                                (byte)(0xff & (hostAddress >> 24)) };

        try {
           return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
           throw new AssertionError();
        }
    }

    /**
     * 比较两个无符号int数a和b的大小，a小于b返回-1，a等于b返回0，a大于b返回1
     */
    public int unsignedIntCompare(int a, int b)
    {
        long one, two;
        one = a & 0x0FFFFFFFFl;
        two = b & 0x0FFFFFFFFl;
        if (one < two) {
            return -1;
        } else if (one == two) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * 启动时执行。获取本机ip地址和子网掩码，然后给局域网中的所有人推送上线信息，
     * 并且发送请求ip地址信号，以便得到局域网中在线主机的回应，登记它们的ip地址
     */
    public void firstTime()
    {
        // 上线消息字节流缓冲区
        byte alertBuffer[] = ("“" + name + "”上线了！").getBytes();
        // 请求信号字节流缓冲区
        byte signalBuffer[] = ("\b").getBytes();
        // 本机ip地址
        InetAddress localHost;
        // 本机ip地址32位整数表示
        int localip = 0;
        // 网络前缀长度
        int networkPrefixLength = 0;
        try {
            // 获取本机ip地址
            localHost = Inet4Address.getLocalHost();
            // 把本机ip地址转换为32位整数表示
            localip = inetAddressToInt(InetAddress.getByName(localHost.getHostAddress()));
            // 网络接口
            NetworkInterface networkInterface;
            try {
                // 获取网卡信息
                networkInterface = NetworkInterface.getByInetAddress(localHost);
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    // 获取网络前缀长度
                    networkPrefixLength = address.getNetworkPrefixLength();
                }
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // 若无法正常获取网络前缀长度，则在此指定
        networkPrefixLength = 24;
        // 切换局域网中下一台主机需要的变化量的值
        int delta = 1 << networkPrefixLength;
        // 网络前缀
        int networkPrefix = localip & (delta - 1);
        // 局域网中第一台主机的主机号
        int begin = delta;
        // 局域网中最后一台主机之后的主机号，一般为直接广播地址的主机号
        int end = ~(delta - 1);
        // 用此掩码获得主机号
        int mask = end;
        // 本机的主机号
        int self = localip & mask;
        /*
        System.out.println("localip: " + localip);
        System.out.println("networkPrefix: " + networkPrefix);
        System.out.println("self: " + self);
        System.out.println("begin: " + begin);
        System.out.println("end: " + end);
        System.out.println("delta: " + delta);
        */

        try {
            // 用来发包的socket
            DatagramSocket sendSocket = new DatagramSocket();
            // 给小于本机主机号的主机发包
            for (int i = begin; unsignedIntCompare(i, self) == -1; i += delta) {
                // 上线提醒packet
                DatagramPacket alertPack = new DatagramPacket(alertBuffer,
                                                alertBuffer.length,
                                                intToInetAddress(networkPrefix | i),
                                                port);
                // 请求信号packet
                DatagramPacket signalPack = new DatagramPacket(signalBuffer,
                                                signalBuffer.length,
                                                intToInetAddress(networkPrefix | i),
                                                port);
                // 发上线提醒
                sendSocket.send(alertPack);
                // 发请求信号
                sendSocket.send(signalPack);
                // System.out.print("b ");
            }
            //System.out.println("self + delta: " + (self+delta));
            // 给大于本机主机号的主机发包
            for (int i = self+delta; unsignedIntCompare(i, end) == -1; i += delta) {
                // 上线提醒packet
                DatagramPacket alertPack = new DatagramPacket(alertBuffer,
                                                alertBuffer.length,
                                                intToInetAddress(networkPrefix | i),
                                                port);
                // 请求信号packet
                DatagramPacket signalPack = new DatagramPacket(signalBuffer,
                                                signalBuffer.length,
                                                intToInetAddress(networkPrefix | i),
                                                port);
                // 发上线提醒
                sendSocket.send(alertPack);
                // 发请求信号
                sendSocket.send(signalPack);
                // System.out.print("e ");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //text_receiver.append("目前在线的网友有：\n");
    }

    /**
     * 给ip地址为ia的主机发送回应信息
     */
    public void sendResponse(InetAddress ia)
    {
        // 回应信息字节流缓冲区
        byte responseBuffer[] = ("\f").getBytes();
        try {
            // 回应信息packet
            DatagramPacket responsePack = new DatagramPacket(responseBuffer,
                                                            responseBuffer.length,
                                                            ia,
                                                            port);
            // 发送回应信息
            new DatagramSocket().send(responsePack);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 构造函数
     */
    public ChatUDPJFrame(String name) throws Exception
    {
        // 聊天窗口标题栏
        super("聊天室  " + name + "  " + InetAddress.getLocalHost());
        // 设置窗口
        this.setBounds(320, 240, 400, 240);
        // 退出事件
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        // 设置消息接受文本区
        this.text_receiver = new JTextArea();
        this.text_receiver.setEditable(false);
        this.getContentPane().add(new JScrollPane(this.text_receiver));
        JPanel panel = new JPanel();
        this.getContentPane().add(panel, "South");
        this.text_sender = new JTextField(20);
        // 在面板中添加发送文本域
        panel.add(this.text_sender);
        // 发送按钮
        final JButton button_send = new JButton("发送");
        // 在面板中添加发送按钮
        panel.add(button_send);
        // 给发送按钮添加监听事件
        button_send.addActionListener(this);
        // 让发送文本域监听按下回车的事件，让其与点击发送按钮等效
        text_sender.addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e)
            {
                //按回车键执行相应操作
                if(e.getKeyChar()==KeyEvent.VK_ENTER )
                {
                    // 若没有输入内容直接回车，则忽略
                    if (text_sender.getText().charAt(0) == '\n') {
                        text_sender.setText("");
                    } else {
                        button_send.doClick();
                    }
                }
            }
        });
        // 使可见
        this.setVisible(true);
        // 设置网名
        this.name = name;
        // 设置端口
        this.port = 8080;

        firstTime();

        // 接受消息的字节流缓冲区
        byte data[] = new byte[512];
        // 接受消息的packet
        DatagramPacket recvPack = new DatagramPacket(data, data.length);
        // 接受消息的socket
        DatagramSocket recvSocket = new DatagramSocket(port);
        // 接受到的消息的长度
        int length = 0;
        // 随时接受消息
        while (recvSocket != null) {
            // 接受消息
            recvSocket.receive(recvPack);
            // 获取消息长度
            length = recvPack.getLength();
            // 获取到的消息转化为String
            String message = new String(recvPack.getData(), 0, length);
            System.out.println(message);
            // 如果是请求ip地址的请求消息，则登记发送方的ip地址，并且给其回应
            if (message.charAt(0) == '\b') {
                // 获取发送方ip地址
                InetAddress fresh = recvPack.getAddress();
                // 登记发送方ip地址
                hosts.add(fresh);
                System.out.println(fresh);
                // 回应
                sendResponse(fresh);
                System.out.println("enroll");
            // 如果是对方回应的信息，则登记对方ip地址
            } else if (message.charAt(0) == '\f') {
                // 获取回应方的ip地址
                InetAddress fresh = recvPack.getAddress();
                // 登记回应方的ip地址
                hosts.add(fresh);
                System.out.println(fresh);
                System.out.println("echo");
            // 否则，直接显示消息
            } else {
                // 让此消息显示在接受消息的文本区里
                text_receiver.append(message + "\r\n");
            }
        }
    }

    /**
     * 监听事件
     */
    public void actionPerformed(ActionEvent e)
    {
        // 如果点击发送按钮
        if (e.getActionCommand() == "发送") {
            // 要发送消息的字节流缓冲区
            byte buffer[] = ("“" + name + "”说：" + text_sender.getText()).getBytes();
            try {
                // 发送消息的socket
                DatagramSocket sendSocket = new DatagramSocket();
                // 给每个登记的主机发送此消息
                for (InetAddress ia: hosts) {
                    System.out.println(ia);
                    // 发送消息的packet
                    DatagramPacket sendPack = new DatagramPacket(buffer,
                                                                buffer.length,
                                                                ia,
                                                                port);
                    // 发送消息的socket
                    sendSocket.send(sendPack);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            // 在自己的窗口中也要显示自己所发的消息
            text_receiver.append("“我”说：" + text_sender.getText() + "\n");
            // 清空发送消息的文本域
            text_sender.setText("");
        }
    }

    public static void main(String args[]) throws Exception
    {
        String name = JOptionPane.showInputDialog("请输入你的网名：");
        new ChatUDPJFrame(name);
    }
}
