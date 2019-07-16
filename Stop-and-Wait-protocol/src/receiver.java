import java.io.InputStream;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Timer;
import java.util.Calendar;
import java.util.TimerTask;

public class receiver {

	
	public static void main(String[] args) {
		try {
			int sender_port = 55300;
			int receiver_port = 55301;
			InetAddress sender_address = InetAddress.getLocalHost();
			int sequence_number = 0;
			System.out.println("receiver starts");
			DatagramSocket socket = new DatagramSocket(receiver_port);
			int count = 1;
			while (true) {
				System.out.println("listening at port");
				String data = UDPReceive(socket);
				System.out.println("--------frame data #" + count + " received-------------");
				System.out.println("frame data: " + data);
				System.out.println("frame sequence number: " + data.charAt(0));
				//----------------------TODO
				// check CRC
				boolean If_CRC_Legal = CRCGenerate.GetCRC(data.substring(0, data.length() - 16)).equals(data);
				
				System.out.println("frame CRC check result: " + If_CRC_Legal);
				if (data.charAt(0) == ("" + sequence_number).charAt(0) && If_CRC_Legal) {
					System.out.println("frame correct");
					if (sequence_number == 0) {
						sequence_number = 1;
					} else {
						sequence_number = 0;
					}
					System.out.println("sending ack of sequence number " + sequence_number);
					
					byte[] ack_data;
					if (sequence_number == 0) {
						ack_data = "0".getBytes();
					} else {
						ack_data = "1".getBytes();
					}
					DatagramPacket packet = new DatagramPacket(ack_data, ack_data.length, sender_address, sender_port);
					socket.send(packet);
					System.out.println("sended");
					
				} 
				else if (If_CRC_Legal){
					System.out.println("frame sequence number incorrect, discarded");
				} else {
					System.out.println("frame CRC incorrect, discarded");
				}
				count++;
			}
			
		} catch (Exception e){
			System.out.println("Exception thrown  :" + e);
		}
		System.out.print("ended\n");
	}
	
	public static String UDPReceive(DatagramSocket socket) throws Exception{
		
		
		byte[] buffer = new byte[1024];
		
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		String data = new String(buffer, 0, packet.getLength());
		//System.out.println(packet.getLength());
        
		return data;
		
		
	}
	
}
