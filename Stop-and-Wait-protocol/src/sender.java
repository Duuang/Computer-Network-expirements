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

public class sender {
	static int IfFilterErrorAndLost = 1;
	static int FilterError = 5; //should >= 3
	static int FilterLost = 5;
	private static Timer timer = new Timer();
	
	public static void main(String[] args) {
		try {
			int sender_port = 55300;
			int receiver_port = 55301;
			InetAddress receiver_address = InetAddress.getLocalHost();
			int max_seq_num = 1; // sequence number from [0, max_seq_num]
			int count = 1;  
			int sequence_number = 0;
			System.out.println("start sending");
			DatagramSocket socket = new DatagramSocket(sender_port);
			
			while (count <= 30) { //maximum count should >= 3
				Thread.sleep(1000);

				//generate byte[] of frame to send via UDPSend()
				byte[] to_send = Frame.Generate(sequence_number, "10101010").getBytes();
				System.out.println("-------sending data #" + count + " ----------");
				System.out.println("sequence number of frame to send: " + sequence_number);
				System.out.println("data: " + "10101010");
				
				//System.out.println("data with CRC: " + Frame.Generate(sequence_number, "10101010"));
				
				UDPSend(sender_port, receiver_port, receiver_address, to_send, socket);
				
				
				byte[] buffer = new byte[1];
				while (true) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					
					String ack = new String(buffer, 0, packet.getLength());
					System.out.println("received ACK: " + ack);
					if (sequence_number == 0) {
						if (ack.equals("1")) {
							System.out.println("ACK correct, sending next frame");
							timer.cancel();
							sequence_number = 1;
							break;
						} else {
							System.out.println("ACK wrong, waiting for correct ACK");
						}
					} else {
						if (ack.equals("0")) {
							System.out.println("ACK correct, sending next frame");
							timer.cancel();
							sequence_number = 0;
							break;
						} else {
							System.out.println("ACK wrong, waiting for correct ACK");
						}
					}
				}			
				count++;
			}
			
			System.out.print("sended\n");
			socket.close();
		} catch (Exception e){
			System.out.println("Exception thrown  :" + e);
		}
	}
	
	public static void UDPSend(int sender_port, int receiver_port, InetAddress receiver_address, 	
			  			byte[] data, DatagramSocket socket) throws Exception {
		timer.cancel();
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					//UDPSend() again
					System.out.println("**ERROR: time out, send again");
					UDPSend(sender_port, receiver_port, receiver_address, data, socket);	
				} catch (Exception e){
					System.out.println("Exception thrown  :" + e);
				}
			}
		}, 3000);
		
		
		//filter to produce lost and error
		//丢帧，未传输到的情况，包括了丢帧且出错
		if (IfFilterErrorAndLost == 1 && (int)(1+Math.random()*(FilterLost)) % FilterLost == 1) {  
			System.out.println("---Filter: frame lost");
			System.out.println("frame data: --");
			System.out.println("sended");
		//出错且未丢帧的情况
		} else if (IfFilterErrorAndLost == 1 && (int)(1+Math.random()*(FilterError)) % FilterError 							== 1) { 
			
			byte[] error_data = new byte[1005];
			System.arraycopy(data, 0, error_data, 0, data.length);
			//模拟出错情况
			// sequence number 部分出错
			if ((int)(1+Math.random()*(2)) <= 1) {	
				System.out.println("---Filter: frame error at position 0");
				if (error_data[0] == '0') {
					error_data[0]++; 
				} else {
					error_data[0]--;
				}
			//数据部分出错		
			} else {
				System.out.println("---Filter: frame error at position 5");
				if (error_data[5] == '0') {
					error_data[5]++; 
				} else {
					error_data[5]--;
				}
			}					
			DatagramPacket error_packet = new DatagramPacket(error_data, data.length, receiver_address, receiver_port);
			socket.send(error_packet);
			System.out.println("frame data: " + new String(error_data));
			System.out.println("sended");
		//正确发送的情况
		} else {
			System.out.println("---Filter: None, 正确发送");
			DatagramPacket packet = new DatagramPacket(data, data.length, receiver_address, receiver_port);
			socket.send(packet);
			System.out.println("frame data: " + new String(data));
			System.out.println("sended");
		}

		
	}

}
