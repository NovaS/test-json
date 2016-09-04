package com.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author novas
 */
public class NioClient extends Thread {
	private static final Logger logger = LoggerFactory.getLogger(NioClient.class);
	private static final int MAX_BYTE = 2048;
	private final ConcurrentLinkedQueue<String> writeQue;
	private InetSocketAddress targetAddress;
	private SocketChannel channel;
	private Selector selector;
	private Timer worker;
	
	public NioClient(String host, int port) {
		writeQue = new ConcurrentLinkedQueue<String>();
		targetAddress = new InetSocketAddress(host, port);
		worker = new Timer();
	}
	
	public void send(String data) {
		this.writeQue.offer(data);
	}
	
	@Override
	public void run() {
		super.run();
		try {
			selector = Selector.open();
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_CONNECT);
			channel.connect(targetAddress);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Failed to connect to server: {}",targetAddress);
			interrupt();
		}
		while(!isInterrupted()){
			try {
				int select = selector.select(1000);
				if(select<1) sleep(1);
				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while(selectedKeys.hasNext()){
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();
					if(!key.isValid()) continue;
					if(key.isConnectable()) connect(key);
					else if(key.isReadable()) read(key);
					else if(key.isWritable()) write(key);
					else logger.warn("Found unhandled key from selector!");
				}
			} catch (IOException e) {
				logger.error("IOException! Message: {}", e.getMessage());
				terminate();
			} catch (InterruptedException e) {
				logger.error("InterruptedException! Message: {}", e.getMessage());
			}
		}
	}
	
	public void terminate() {
		try {
			logger.warn("Terminating connection!");
			worker.cancel();
			interrupt();
			if(selector!=null) selector.close();
			if(channel!=null) channel.close();
		} catch (IOException e) {
			logger.error("IOException! Message: {}", e.getMessage());
		}
	}
	
	private void connect(SelectionKey key) throws IOException {
		try {
			logger.info("Connecting to "+targetAddress.getHostName());
			SocketChannel chan = (SocketChannel) key.channel();
			if(chan.isConnectionPending()) chan.finishConnect();
			chan.configureBlocking(false);
			chan.register(selector, SelectionKey.OP_WRITE);
//			key.interestOps(SelectionKey.OP_WRITE);
		} catch (IOException e) {
			logger.error("Failed in connecting to server {}", targetAddress.getHostName());
			throw e;
		}
	}
	
	private void read(SelectionKey key) throws IOException {
		try {
			SocketChannel chan = (SocketChannel) key.channel();
			ByteBuffer buff = ByteBuffer.allocate(MAX_BYTE);
			buff.clear();
			int length = chan.read(buff);
			if(length>0){
				buff.flip();
				byte[] swap = new byte[length];
				buff.get(swap, 0, length);
				logger.info("Received data: "+new String(swap));
			}
			chan.register(selector, SelectionKey.OP_WRITE);
//			key.interestOps(SelectionKey.OP_WRITE);
		} catch (IOException e) {
			logger.error("Failed to read from server {}", targetAddress.getHostName());
			throw e;
		}
	}
	
	private void write(SelectionKey key) throws IOException {
		try {
			SocketChannel chan = (SocketChannel) key.channel();
			String data = writeQue.poll();
			if(data!=null){
				logger.info("Sending data: {}", data);
				chan.write(ByteBuffer.wrap(data.getBytes()));
				chan.write(ByteBuffer.wrap("\n".getBytes()));
			}
			chan.register(selector, SelectionKey.OP_READ);
//			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			logger.error("Failed to write to server {}",targetAddress.getHostName());
			throw e;
		}
	}
}
