package com.joseph.transfer_sdk;

public class ByteUtils {

    /**
     * 字节转换为字符串
     * @param b
     * @return
     */
    public static String byte2String(byte b){
        String str=Integer.toHexString(b&0xff);
        if(str.length()==1){
            str="0"+str;
        }
        str="0x"+str;
        return str;
    }

    /**
     * 字符串转单个字节
     * @param var
     * @return
     */
    public static byte string2Byte(String var)throws Exception{
        if(!var.matches("([a-fA-F0-9]{1,2})")){
            throw new NumberFormatException("转换失败:"+var+"不是16进制字符");
        }
        return Integer.valueOf(var,16).byteValue();
    }


    /**
     * 字节码转换为字符串（十六进制）
     * @param bytes
     * @return
     */
    public static String bytesToString(byte[]bytes){
        StringBuilder byteStr= new StringBuilder();
        for (byte b:bytes){
            byteStr.append(byte2String(b)).append(" ");
        }
        return byteStr.toString().trim();
    }

    /**
     * 字符串强转为字节数组
     * @param var
     * @return
     * @throws Exception
     */
    public static byte[] stringToBytes(String var)throws Exception {
        String[] bytesStr=var.split("\\s+");
        byte[]bytes=new byte[bytesStr.length];
        for (int i=0;i<bytesStr.length;i++){
            bytes[i]=string2Byte(bytesStr[i]);
        }
        return bytes;
    }


    /**
     * 数据转换
     * @param hbyte 高位数据
     * @param lbyte 低位数据
     * @return 数据
     */
    public static int byteToInt(byte hbyte, byte lbyte) {
        int l = lbyte;
        l &= 0xFF;
        l |= ((int) hbyte << 8);
        l &= 0xFFFF;
        return l;
    }

    /**
     * 数据转换
     * @param hbyte1 高位数据
     * @param hbyte2 高位数据
     * @param lbyte1 低位数据
     * @param lbyte2 低位数据
     * @return 数据
     */
    public static int byteToInt(byte hbyte1, byte hbyte2,
                                byte lbyte1, byte lbyte2) {
        int l = lbyte2;
        l &= 0xFF;
        l |= ((int) lbyte1 << 8);
        l &= 0xFFFF;

        int h = hbyte2;
        h &= 0xFF;
        h |= ((int) hbyte1 << 8);
        h &= 0xFFFF;

        l |= ((int) h << 16);
        l &= 0xFFFFFFFF;
        return l;
    }

}
