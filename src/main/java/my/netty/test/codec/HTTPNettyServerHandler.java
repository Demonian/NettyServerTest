package my.netty.test.codec;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import my.netty.test.MyHTTPRequest;


import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Created by Dmytro on 02.08.2015.
 */
public class HTTPNettyServerHandler extends ChannelInboundHandlerAdapter{

    private static AtomicInteger overallCount = new AtomicInteger();
    private static Set<MyHTTPRequest> uniqReqCount = new HashSet<MyHTTPRequest>();
    private static ConcurrentHashMap<String, MyHTTPRequest> reqInfo = new ConcurrentHashMap<String, MyHTTPRequest>();
    private static ConcurrentHashMap<String, Integer> redirectReqCount = new ConcurrentHashMap<String, Integer>();
    private static AtomicInteger opennedConnect = new AtomicInteger();
    private static ConcurrentLinkedDeque<MyHTTPRequest> sixteenProcessedConnect = new ConcurrentLinkedDeque<MyHTTPRequest>();
    private static InternalLogger logger = InternalLoggerFactory.getInstance("log");

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        opennedConnect.incrementAndGet();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        opennedConnect.decrementAndGet();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            if(msg.toString().contains("favicon")){return;}
            MyHTTPRequest request = new MyHTTPRequest(msg);

            overallCount.incrementAndGet();
            request.setReqTime(request.getDate().getTime());
            request.setIp(ctx.channel().remoteAddress().toString().split(":")[0].substring(1));

            if (uniqReqCount.add(request)) {
                logger.info("Unique request added: " + request.getType()
                        + "\nRequest from Ip: " + request.getIp());
            }

            if (sixteenProcessedConnect.size() < 16) {
                sixteenProcessedConnect.add(request);
            } else {
                sixteenProcessedConnect.pollFirst();
                sixteenProcessedConnect.add(request);
            }

            uniqReqCount = Collections.synchronizedSet(uniqReqCount);

            if (reqInfo.containsKey(request.getIp())) {
                request.setReqCounter(reqInfo.get(request.getIp()).getReqCounter() + 1);
                reqInfo.replace(request.getIp(), request);
            } else {
                reqInfo.put(request.getIp(),request);
            }

//            Choose the action from my request type

            switch (request.getType()) {
                case HELLO:
                    helloRequestHandler(ctx, request);
                    break;
                case REDIRECT: redirectRequestHandler(ctx, request);
                    break;
                case STATUS: statusRequestHandler(ctx, request);
                    break;
                default: defRequestHandler(ctx, request);
            }
        }
    }

    private void defRequestHandler(ChannelHandlerContext ctx, MyHTTPRequest request) {
        logger.info("Request type: " + request.getType() + "\n" +
                "Default request!");
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                Unpooled.wrappedBuffer("Response by default!".getBytes()));
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        try {
            request.setRecBytes(response.toString().getBytes("utf8").length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        request.setReqTime((new Date().getTime() - request.getReqTime()));
        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void helloRequestHandler(ChannelHandlerContext ctx, MyHTTPRequest request) {
        logger.info("Request type: " + request.getType() + "\n" +
                "Wait for 10 sec!");
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                Unpooled.wrappedBuffer("Hello World".getBytes()));
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        try {
            request.setRecBytes(response.toString().getBytes("utf8").length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        request.setReqTime((new Date().getTime() - request.getReqTime()));
        System.out.println(request.getReqTime());
        System.out.println(request.getBytePerSec());
        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void redirectRequestHandler(ChannelHandlerContext ctx, MyHTTPRequest request) {
        if (request.getUri().contains("=")) {
            String redirectURL = request.getUri().split("=")[1];
            logger.info("Request type: " + request.getType() + "\n" +
                    "URL to redirect: " + redirectURL);
            if (redirectReqCount.containsKey(redirectURL)) {
                redirectReqCount.replace(redirectURL, redirectReqCount.get(redirectURL) + 1);
            } else {
                redirectReqCount.put(redirectURL, 1);
            }

            redirectURL = redirectURL.toLowerCase().startsWith("http://") ? redirectURL : "http://" + redirectURL;

            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, TEMPORARY_REDIRECT);
            response.headers().set(CONTENT_TYPE, "text/html");
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(LOCATION, redirectURL);

            try {
                request.setRecBytes(response.toString().getBytes("utf8").length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            request.setReqTime((new Date().getTime() - request.getReqTime()));
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void statusRequestHandler(ChannelHandlerContext ctx, MyHTTPRequest request) {
        logger.info("Request type: " + request.getType() + "\n" +
                "Generating the table...");
        StringBuilder htmlPage = new StringBuilder();

//        Generating the tables of statistic

        String bodyTables = bodyTablesGenerator();
        htmlPage.append("<!DOCTYPE html>" +
                "<html lang=\"en\">"+
                "<head><title>Status</title></head>"+
                "<body>"+
                "<center><h3>Statistic of Interactions:</h3></center>\n"+
                "<p style=\"text-align:left;\">" +
                bodyTables + "</p>" +
                "</body>" +
                "</html>");
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                Unpooled.wrappedBuffer(htmlPage.toString().getBytes()));
        response.headers().set(CONTENT_TYPE, "text/html");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        try {
            request.setRecBytes(response.toString().getBytes("utf8").length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        request.setReqTime((new Date().getTime() - request.getReqTime()));
        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private String bodyTablesGenerator() {
        StringBuilder tables = new StringBuilder();
//        Overall request count and uniq requests html generator
        String overallStat = "<span style=\"font-family:Arial;font-size:16px;font-style:normal;font-weight:bold;" +
                "text-decoration:underline;text-transform:none;color:000000;\">" +
                "Overall requests: " + overallCount +
                "</span></p>\n" +
                "<span style=\"font-family:Arial;font-size:16px;font-style:normal;font-weight:bold;" +
                "text-decoration:underline;text-transform:none;color:000000;\">" +
                "Unique requests of every IP: " + uniqReqCount.size() +
                "</span></p>\n"
                ;

//        First Table generator

        StringBuilder dataForFirstTable = new StringBuilder();
        for (Map.Entry<String, MyHTTPRequest> reqList : reqInfo.entrySet()) {
            dataForFirstTable.append("<tr><td>")
                    .append(reqList.getKey()).append("</td><td>").append(reqList.getValue().getReqCounter())
                    .append("</td><td>").append(reqList.getValue().getFormatDate()).append("</td></tr>\n");
        }
        String firstTable = "<center><h4>Uniq IP requests statistic:</h4></center>\n" +
                "<style type=\"text/css\">\n" +
                ".tftable {font-size:12px;color:#333333;width:100%;border-width: 1px;border-color: #729ea5;border-collapse: collapse;}\n" +
                ".tftable th {font-size:12px;background-color:#acc8cc;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;text-align:left;}\n" +
                ".tftable tr {background-color:#ffffff;}\n" +
                ".tftable td {font-size:12px;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;}\n" +
                ".tftable tr:hover {background-color:#ffff99;}\n" +
                "</style>\n" +
                "\n" +
                "<table class=\"tftable\" border=\"1\">\n" +
                "<tr><th>IP</th><th>Overall Requests Count</th><th>Last Request Time</th></tr>\n" +
                dataForFirstTable.toString() +
                "</table>\n";

//        Second Table generator

        StringBuilder dataForSecondTable = new StringBuilder();

        for (Map.Entry<String, Integer> urlList : redirectReqCount.entrySet()) {
            dataForSecondTable.append("<tr><td>")
                    .append(urlList.getKey()).append("</td><td>")
                    .append(urlList.getValue()).append("</td></tr>\n");
        }

        String secondTable = "<center><h4>URL redirects count:</h4></center>\n" +
                "<style type=\"text/css\">\n" +
                ".tftable {font-size:12px;color:#333333;width:100%;border-width: 1px;border-color: #729ea5;" +
                "border-collapse: collapse;}\n" +
                ".tftable th {font-size:12px;background-color:#acc8cc;border-width: 1px;padding: 8px;" +
                "border-style: solid;border-color: #729ea5;text-align:left;}\n" +
                ".tftable tr {background-color:#ffffff;}\n" +
                ".tftable td {font-size:12px;border-width: 1px;padding: 8px;border-style: solid;" +
                "border-color: #729ea5;}\n" +
                ".tftable tr:hover {background-color:#ffff99;}\n" +
                "</style>\n" +
                "\n" +
                "<table class=\"tftable\" border=\"1\">\n" +
                "<tr><th>URL to Redirect</th><th>Redirect Count</th></tr>\n" +
                dataForSecondTable.toString() +
                "</table>\n";

//          Open connections html generator

        String openConnections = "<span style=\"font-family:Arial;font-size:16px;font-style:normal;" +
                "font-weight:bold;text-decoration:underline;text-transform:none;color:000000;\">" +
                "Open connections: " + opennedConnect +
                "</span></p>\n"
                ;

//         Third Table generator

        StringBuilder dataForThirdTable = new StringBuilder();

        for (MyHTTPRequest reqList : sixteenProcessedConnect) {
            dataForThirdTable.append("<tr><td>")
                    .append(reqList.getIp()).append("</td><td>").append(reqList.getUri())
                    .append("</td><td>").append(reqList.getFormatDate())
                    .append("</td><td>").append(reqList.getSentBytes())
                    .append("</td><td>").append(reqList.getRecBytes())
                    .append("</td><td>").append(reqList.getBytePerSec())
                    .append("</td></tr>\n");
        }

        String thirdTable = "<center><h4>Request full statistic by IP:</h4></center>\n" +
                "<style type=\"text/css\">\n" +
                ".tftable {font-size:12px;color:#333333;width:100%;border-width: 1px;border-color: #729ea5;border-collapse: collapse;}\n" +
                ".tftable th {font-size:12px;background-color:#acc8cc;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;text-align:left;}\n" +
                ".tftable tr {background-color:#ffffff;}\n" +
                ".tftable td {font-size:12px;border-width: 1px;padding: 8px;border-style: solid;border-color: #729ea5;}\n" +
                ".tftable tr:hover {background-color:#ffff99;}\n" +
                "</style>\n" +
                "\n" +
                "<table class=\"tftable\" border=\"1\">\n" +
                "<tr><th>SRC_IP</th><th>URI</th><th>timestamp</th>" +
                "<th>SENT_BYTES</th><th>RECEIVED_BYTES</th><th>SPEED (BYTES/SEC)</th></tr>\n" +
                dataForThirdTable.toString() +
                "</table>";

        tables.append(overallStat).append(firstTable).append(secondTable).append(openConnections).append(thirdTable);
        return tables.toString();
    }
}
