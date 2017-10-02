package kr.neolab.samplecode;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;

public class HttpUtils {
    public static boolean exception_flag = false;
    /*
     * Function  :   Post request to server
     * Param     :   params are request content，encode is encoding format
     */
    public static String submitPostData(String strUrlPath,Map<String, String> params, String encode) {
        exception_flag = false;
        byte[] data = getRequestData(params, encode).toString().getBytes();//get request body
        try {
            URL url = new URL(strUrlPath);

            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            if (httpURLConnection == null){
                return "";
            }
            httpURLConnection.setConnectTimeout(60000);     //set timeout
            httpURLConnection.setDoInput(true);             //input stream, get data from server
            httpURLConnection.setDoOutput(true);            //output stream, submit data to server
            httpURLConnection.setRequestMethod("POST");     //Using post method
            httpURLConnection.setUseCaches(false);          //set no cache
            //set header
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length));
            //get output stream and write data to server
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(data);

            int response = httpURLConnection.getResponseCode();            //get server response code
            if(response == HttpURLConnection.HTTP_OK) {
                InputStream inptStream = httpURLConnection.getInputStream();
                return dealResponseResult(inptStream);                     //deal response result
            }
            exception_flag = true;
        } catch (IOException e) {
            e.printStackTrace();
            exception_flag = true;
            return "";
        }
        return "";
    }

    /*
     * Function  :   encapsulate request body
     * Param     :   params are request content，encode is encoding format
     */
    public static StringBuffer getRequestData(Map<String, String> params, String encode) {
        StringBuffer stringBuffer = new StringBuffer();          //store the encapsulated request body
        try {
            for(Map.Entry<String, String> entry : params.entrySet()) {
                stringBuffer.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), encode))
                        .append("&");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);//remove the last '&'
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    /*
     * Function  :   convert input stream to string
     * Param     :   inputStream
     */
    public static String dealResponseResult(InputStream inputStream) {
        String resultData = null;   //store result
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        try {
            while((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        resultData = new String(byteArrayOutputStream.toByteArray());
        return resultData;
    }


}