public class CRCGenerate {
    public static String GetCRC(String args) {

        //10010111011001001000001110001000 10001000000100001
        //1101011111 10011
        // the binary to send
        String InfoString = args;
        
        // reserved polynomial CRC-CCITT=X16+X12+X5+1
        String GenXString = "10001000000100001";
        
        
        // operator
        int GenXlen = GenXString.length();
        //step1 append r zeros to InfoString r = 16
        int cas = GenXlen - 1;
        while(cas-- > 0){
            InfoString = InfoString + '0';
        }

        //step2 mod2 and chufa qiuyu qiushang
        // string to int
        int Infolen = InfoString.length();
        
        int i = 0, ans = 0;
        int inbinary = 0, poly = 0;

        //get the count of calculant
        int calculantNum = 1 + Infolen - GenXlen;
        int j = 0;
        String str1 = "", str2 = "", str3 = "";
        String outputBinary = "";

        for(j = 0; j < calculantNum; j++){
            if(j == 0){
                str1 = InfoString.substring(j, j + GenXlen);
            }
            else{
                str1 = str2.substring(1, GenXlen) + InfoString.charAt(j + GenXlen - 1);//bei chu shu = last yu shu + zai qu yi wei
            }
            str2 = "";
            str3 = "";
            int cas2 = 0;
            if(str1.charAt(0) == '1'){
                str3 = GenXString;
            }
            else{
                for(int cas3 = 0; cas3 < GenXlen; cas3++){
                    str3 = str3 + '0';
                }
            }
            for(cas2 = 0; cas2 < GenXlen; cas2++){
                if(str1.charAt(cas2) == str3.charAt(cas2)){
                    str2 = str2 + '0';
                }
                else{
                    str2 = str2 + '1';
                }
            }
            if (j == calculantNum - 1){
                for (int cas4 = 0; cas4 < Infolen - GenXlen + 1; cas4++){
                    outputBinary = outputBinary + InfoString.substring(cas4, cas4 + 1);
                }
                for(int cas5 = 0; cas5 < GenXlen - 1; cas5++){
                    if(str2.charAt(1+cas5) == '1'){
                        outputBinary = outputBinary + '1';
                    }
                    else{
                        outputBinary = outputBinary + '0';
                    }
                }
            }
            //InfoString = str2.substring(1, GenXlen) + InfoString.substring(GenXlen, Infolen);
        }
        
        //get value of the inputstring and poly
        for (i = Infolen - 1; i >= 0; i--) {
            if (InfoString.charAt(i) == '1') {
                inbinary += 2 ^ ans;
            }
            ans++;
        }
        ans = 0;
        for(i = GenXlen - 1; i >= 0; i--){
            if(GenXString.charAt(i) == '1'){
                poly += 2 ^ ans;
            }
            ans++;
        }
        
        // print the string to send
        //System.out.println("String to send is " + InfoString);
        //System.out.println("transfer to int is " + inbinary);

        // print the generater
        //System.out.println("Generator is " + GenXString);
        //System.out.println("transfer to int is " + poly);

        //print the output
        //System.out.println("String to output is " + outputBinary);
        return outputBinary;
    }
}