package my.netty.test;

import io.netty.handler.codec.http.HttpRequest;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Dmytro on 02.08.2015.
 */
public class MyHTTPRequest {

    private String ip;
    private Object message;
    private Date date;
    private long recBytes;
    private long sentBytes;
    private int reqCounter = 1;
    private long reqTime;
    private String uri;
    public enum ReqType {HELLO, REDIRECT, STATUS, DEFAULT}
    private ReqType type;

    public MyHTTPRequest(Object message) {
        this.message = message;
        this.date = new Date();
        try {
            this.sentBytes = message.toString().getBytes("utf8").length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        this.uri = ((HttpRequest)message).getUri();

        if (message.toString().contains("hello")){
            this.type = ReqType.HELLO;
        } else if (message.toString().contains("redirect")) {
            this.type = ReqType.REDIRECT;
        } else if (message.toString().contains("status")){
            this.type = ReqType.STATUS;
        } else {
            this.type = ReqType.DEFAULT;
            }
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }


    public Date getDate() {
        return date;
    }


    public long getRecBytes() {
        return recBytes;
    }

    public void setRecBytes(long recBytes) {
        this.recBytes = recBytes;
    }

    public long getSentBytes() {
        return sentBytes;
    }

    public int getReqCounter() {
        return reqCounter;
    }

    public void setReqCounter(int reqCounter) {
        this.reqCounter = reqCounter;
    }

    public long getReqTime() {
        return reqTime;
    }

    public void setReqTime(long reqTime) {
        this.reqTime = reqTime;
    }

    public String getUri() {
        return uri;
    }

    public ReqType getType() {
        return type;
    }

    private float getSecfromReqTime(long reqTime) {
        return reqTime / 1000F;
    }


    public long getBytePerSec() {
        return (getSecfromReqTime(this.reqTime) > 0)
                ?((long)((this.sentBytes + this.recBytes) / getSecfromReqTime(this.reqTime)))
                :(this.sentBytes + this.recBytes);
    }

//    Formatting data for better visual
    public String getFormatDate(){return new SimpleDateFormat("HH:mm:ss MM-dd-yyyy").format(this.date);}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MyHTTPRequest)) return false;

        MyHTTPRequest that = (MyHTTPRequest) o;

        return ip.equals(that.ip) && type == that.type;

    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
