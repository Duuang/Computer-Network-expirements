import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class host2 {
	public static int IfFilterErrorAndLost = 1;
	public static int FilterError = 5; //should >= 3
	public static int FilterLost = 5;
	public static int max_seq_num = 7; // sequence number from [0, max_seq_num]
	public static byte[][] send_buffer = new byte[max_seq_num + 1][1024];
	public static Timer timer = new Timer();
	public static int next_frame_to_send = 0;
	public static int ack_expected = 1;
	public static int frame_expected = 0;
	public static int window_low = 0;
	public static int window_high = 7;
	public static DatagramSocket socket;
	public static int count = 1;
	public static boolean if_already_loop = false;
	public static boolean if_waiting = false;
	public static int wait_count;
	public static boolean canstartsending = false;
	
	public static void main(String[] args) {
		try {
			int host1_port = 55300;
			int host2_port = 55301;
			InetAddress receiver_address = InetAddress.getLocalHost();
		
			
			socket = new DatagramSocket(host2_port);
			
			ReceiverThread receiver_thread = new ReceiverThread();
			receiver_thread.start();
			SenderThread sender_thread = new SenderThread();
			sender_thread.start();
		} catch (Exception e){
			System.out.println("Exception thrown  :" + e);
		}
	}
	
	

static class SenderThread extends Thread{
	@Override
    public void run() {
       
		try {
			int host1_port = 55300;
			int host2_port = 55301;
			InetAddress receiver_address = InetAddress.getLocalHost();
			while (true) {
				Thread.sleep(100);
				if (host2.canstartsending == true) {
					break;
				}
			}
			while (true) {
				host2.timer.cancel();
				host2.timer = new Timer();
			
				host2.timer.schedule(new TimerTask() {
					@Override
					public void run() {
						try {
							//UDPSend() again
							System.out.println("<S>**ERROR: time out, send again from sequence number " + (host2.ack_expected - 1 < 0 ? host2.max_seq_num : host2.ack_expected - 1));
							int rowbackcount = host2.next_frame_to_send - (host2.ack_expected - 1);
							if (rowbackcount < 0) {
								rowbackcount += host2.max_seq_num + 1;
							} else if (rowbackcount == 0) {
								if (host2.if_waiting == true) {
									rowbackcount += host2.max_seq_num + 1;
								}
							}
							host2.count -= rowbackcount;
							host2.next_frame_to_send = host2.ack_expected - 1 < 0 ? host2.max_seq_num : host2.ack_expected - 1;
							host2.if_waiting = false;
							host2.wait_count = 0;
							host2.timer.cancel();
							new SenderThread().run();
						} catch (Exception e){
							System.out.println("<S>Exception thrown  :" + e);
							
						}
					}
				}, 3000);

				if (host2.if_already_loop == true) {
					return;
				}
				
				System.out.println("<S>start sending");
				host2.if_waiting = false;
				host2.wait_count = 0;
				while (host2.count <= 40) { //maximum count should >= 3
					Thread.sleep(500);
					if (host2.if_already_loop == false) {
						host2.if_already_loop = true;
					}
					if (host2.next_frame_to_send == (host2.window_high == host2.max_seq_num ? 0 : host2.window_high + 1)) {
						if (host2.wait_count < 1) {
							host2.wait_count++;
						} else {
							host2.if_waiting = true;
							continue;
						}
					}
					
					//generate byte[] of frame to send via UDPSend()
					System.arraycopy(Frame.Generate(host2.next_frame_to_send, "10101010").getBytes(), 0, host2.send_buffer[host2.next_frame_to_send], 0, Frame.Generate(host2.next_frame_to_send, "10101010").getBytes().length);
					//host2.send_buffer[host2.next_frame_to_send] = Frame.Generate(host2.next_frame_to_send, "10101010").getBytes();
					System.out.println("<S>-------sending data #" + host2.count + " ----------");
					System.out.println("<S>next_frame_to_send (frame to send now): " + host2.next_frame_to_send);
					System.out.println("<S>ack_expected: " + host2.ack_expected);
					System.out.println("<S>frame_expected: " + host2.frame_expected);
					System.out.println("<S>original data: " + "10101010");
					
					//System.out.println("data with CRC: " + Frame.Generate(sequence_number, "10101010"));
					
					UDPSend(host2_port, host1_port, receiver_address, host2.send_buffer[host2.next_frame_to_send], host2.socket);
	
					
					host2.next_frame_to_send++;
					if (host2.next_frame_to_send > host2.max_seq_num) {
						host2.next_frame_to_send -= host2.max_seq_num + 1;
					} 
					host2.count++;
					
					if (host2.count > 40)
						return;
					
				}
			}
			
			//System.out.print("sended\n");
			//host2.socket.close();
		} catch (Exception e){
			System.out.println("<S>Exception thrown  :" + e);
		}
		
	}
	
	public  void UDPSend(int sender_port, int receiver_port, InetAddress receiver_address, 	
			  			byte[] data, DatagramSocket socket) throws Exception {
		//filter to produce lost and error
		//丢帧，未传输到的情况，包括了丢帧且出错
		if (host2.IfFilterErrorAndLost == 1 && (int)(1+Math.random()*(host2.FilterLost)) % host2.FilterLost == 1) {  
			System.out.println("<S>---Filter: frame lost");
			System.out.println("<S>frame data: --");
			System.out.println("<S>sended");
		//出错且未丢帧的情况
		} else if (host2.IfFilterErrorAndLost == 1 && (int)(1+Math.random()*(host2.FilterError)) % host2.FilterError == 1) { 
			
			byte[] error_data = new byte[1024];
			System.arraycopy(data, 0, error_data, 0, data.length);
			//模拟出错情况
			// sequence number 部分出错
			if ((int)(1+Math.random()*(2)) <= 1) {
				System.out.println("<S>---Filter: frame error at position 0");
				if (error_data[0] == '0') {
					error_data[0]++; 
				} else {
					error_data[0]--;
				}
			//数据部分出错		
			} else {
				System.out.println("<S>---Filter: frame error at position 5");
				if (error_data[5] == '0') {
					error_data[5]++; 
				} else {
					error_data[5]--;
				}
			}					
			DatagramPacket error_packet = new DatagramPacket(error_data, data.length, receiver_address, receiver_port);
			socket.send(error_packet);
			System.out.println("<S>frame data: " + new String(error_data));
			System.out.println("<S>sended");
		//正确发送的情况
		} else {
			System.out.println("<S>---Filter: None, 正确发送");
			DatagramPacket packet = new DatagramPacket(data, data.length, receiver_address, receiver_port);
			socket.send(packet);
			System.out.println("<S>frame data: " + new String(data));
			System.out.println("<S>sended");
		}

		
	}

	
}



static class ReceiverThread extends Thread{     
    @Override
    public void run() {
	    try {
			int host1_port = 55300;
			int host2_port = 55301;
			InetAddress sender_address = InetAddress.getLocalHost();
			
			System.out.println("<R>receiver starts");
			//DatagramSocket socket = new DatagramSocket(host2_port);
			int count = 1;
			while (true) {
				System.out.println("<R>listening at port");
				String data = UDPReceive(socket);
				System.out.println("<R>------------data #" + count + " received-------------");
				
				host2.canstartsending = true;
				System.out.println("<R>received data: " + data);
				//如果是ACK，改变窗口
				if (data.length() == 3) {
					System.out.println("<R>data type: ACK");
					System.out.println("<R>frame sequence number: " + data.substring(0, 3) + ", that is " + Integer.parseInt(data.substring(0, 3), 2));
					//收到的ACK正确
					if (host2.ack_expected == Integer.parseInt(data.substring(0, 3), 2)) {
						host2.window_high++;
						host2.window_low++;
						host2.ack_expected++;
						if (host2.ack_expected > host2.max_seq_num) {
							host2.ack_expected -= host2.max_seq_num + 1;
						}
						host2.window_low = Math.min(30, host2.window_low);
						host2.timer.cancel();
						host2.timer = new Timer();
						host2.timer.schedule(new TimerTask() {
							@Override
							public void run() {
								//从n 开始重新发送
								try {
									//UDPSend() again
									System.out.println("<R>**ERROR: time out, send again from sequence number " + Integer.parseInt(data.substring(0, 3), 2));
									host2.next_frame_to_send = Integer.parseInt(data.substring(0, 3), 2);
									host2.if_waiting = false;
									host2.wait_count = 0;
								} catch (Exception e){
									System.out.println("<R>Exception thrown  :" + e);
								}
							}
						}, 3000);
					} else {
						System.out.println("<R>wrong ACK received, discarded");
					}
					
				//是数据帧
				} else {
					System.out.println("<R>data type: frame data");
					System.out.println("<R>frame sequence number: " + data.substring(0, 3) + ", that is " + Integer.parseInt(data.substring(0, 3), 2));
					// check CRC
					boolean If_CRC_Legal = (CRCGenerate.GetCRC(data.substring(0, data.length() - 16))).substring(0, data.length()).equals(data.substring(0, data.length()));
					//System.out.println(data.length());
					System.out.println("<R>frame CRC check result: " + If_CRC_Legal);
					String tmpstr = Integer.toBinaryString(host2.frame_expected);
					if (frame_expected <= 1) {
						tmpstr = "00" + tmpstr;
					} else if (frame_expected <= 3) {
						tmpstr = "0" + tmpstr;
					}
					if (data.substring(0, 3).equals(tmpstr) && If_CRC_Legal) {
						System.out.println("<R>frame correct");
						host2.frame_expected++;
						if (host2.frame_expected > host2.max_seq_num) {
							host2.frame_expected -= host2.max_seq_num + 1;
						}
						System.out.println("<R>sending ack of sequence number (frame_expected): " + host2.frame_expected);
						
						byte[] ack_data;
						String tmpstr2 = Integer.toBinaryString(host2.frame_expected);
						if (frame_expected <= 1) {
							tmpstr2 = "00" + tmpstr2;
						} else if (frame_expected <= 3) {
							tmpstr2 = "0" + tmpstr2;
						}
						ack_data = tmpstr2.getBytes();
						
						DatagramPacket packet = new DatagramPacket(ack_data, ack_data.length, sender_address, host1_port);
						socket.send(packet);
						System.out.println("<R>sended");
						
						
					} 
					else if (If_CRC_Legal){
						System.out.println("<R>frame sequence number incorrect, discarded");
					} else {
						System.out.println("<R>frame CRC incorrect, discarded");
					}
				}
				
				
				count++;
			}
			
		} catch (Exception e){
			System.out.println("<R>Exception thrown: " + e);
		}
		System.out.print("<R>ended\n");
	}

	public String UDPReceive(DatagramSocket socket) throws Exception{
		
		
		byte[] buffer = new byte[27];
		
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		String data = new String(buffer, 0, packet.getLength());
		//System.out.println(packet.getLength());
	    
		return data;
	}
}
}
