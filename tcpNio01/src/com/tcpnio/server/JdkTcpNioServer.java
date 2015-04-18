package com.tcpnio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Implement of Tcp-Nio Server with JDK Implement
 * <p/>
 * Created by xiaotian.shi on 2015/4/18.
 */
public class JdkTcpNioServer {

    /**
     * the answer count from the server
     */
    private static long total = 0;
    /**
     * buffered size
     */
    private static int BLOCK_SIZE = 4096;
    /**
     * the buffer to receive what client send
     */
    private static ByteBuffer RECEIVE_BUFF = ByteBuffer.allocate(BLOCK_SIZE);
    /**
     * the buffer to send to the client
     */
    private static ByteBuffer SEND_BUFF = ByteBuffer.allocate(BLOCK_SIZE);

    private Selector selector;

    public JdkTcpNioServer(int port) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(new InetSocketAddress(port));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("=========  JdkTcpNioServer START  =================");
    }

    public void listen() throws IOException {
        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                handleKey(selectionKey);
                iterator.remove();
            }
        }
    }

    private void handleKey(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel;
        SocketChannel socketChannel;
        String receiveText;
        String sendText;
        int count;

        if (selectionKey.isAcceptable()) {
            //返回创建此键的通道
            serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            //接受客户端建立连接的请求，并返回 SocketChannel 对象
            socketChannel = serverSocketChannel.accept();
            //配置为非阻塞
            socketChannel.configureBlocking(false);
            //注册到selector
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("CLIENT IS CONNNECTED " + socketChannel);
        } else if (selectionKey.isReadable()) {
            //返回创建此键的通道
            socketChannel = (SocketChannel) selectionKey.channel();
            //将缓冲区清空，以备下次读取
            RECEIVE_BUFF.clear();
            //将发送来的数据读取到缓冲区
            count = socketChannel.read(RECEIVE_BUFF);
            if (count > 0) {
                receiveText = new String(RECEIVE_BUFF.array(), 0, count);
                System.out.println("CLIENT says { " + receiveText + " }");
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            }
            //如果count==-1说明客户端已经关闭连接，这样可以防止服务端不停地进入读取事件
            else if (count == -1) {
                socketChannel.close();
            }
        } else if (selectionKey.isWritable()) {
            //将缓冲区清空以备下次写入
            SEND_BUFF.clear();
            // 返回为之创建此键的通道。
            socketChannel = (SocketChannel) selectionKey.channel();
            sendText = "hello i am a SERVER. ANSWER " + total++;
            //向缓冲区中输入数据
            SEND_BUFF.put(sendText.getBytes());
            //将缓冲区各标志复位,因为向里面put了数据标志被改变要想从中读取数据发向服务器,就要复位
            SEND_BUFF.flip();
            //输出到通道
            socketChannel.write(SEND_BUFF);
            System.out.println("SERVER says {" + sendText + "}");
            socketChannel.register(selector, SelectionKey.OP_READ);
        } else if (selectionKey.isConnectable()) {
            System.out.println("key is connectable");
        }
    }

    public static void main(String[] args) throws IOException {
        JdkTcpNioServer jdkTcpNioServer = new JdkTcpNioServer(10101);
        jdkTcpNioServer.listen();
    }
}
