import java.util.Arrays;

public class Frame {
	static int sequence_num;
	static String str_data;
	static String to_send;
	public static String Generate(int sequence_number, String data) {
		
		sequence_num = sequence_number;	
		str_data = data;
		String tmpstr = Integer.toBinaryString(sequence_number) + data;
		if (sequence_number <= 1) {
			tmpstr = "00" + tmpstr;
		} else if (sequence_number <= 3) {
			tmpstr = "0" + tmpstr;
		}
		to_send =  CRCGenerate.GetCRC(tmpstr);	
		
		return to_send;
	}
	public static void main(String args[]) {
		String s = CRCGenerate.GetCRC("10101010");
		System.out.print("10101010\n");
		System.out.print(s);
	}
}
