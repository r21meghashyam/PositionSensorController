package dev.megh.gyroscopecontroller;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerService extends Service {
    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    private final BlockingQueue<Data> queue = new LinkedBlockingQueue<Data>();
    public String SERVER = "";

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        ServerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ServerService.this;
        }
    }

    public class Data{
        public float x;
        public float y;
        public float z;
        Data(float x,float y,float z){
            this.x=x;
            this.y=y;
            this.z=z;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return binder;
    }

    public void startServer(){
        Thread running = new Thread(() -> {

            SocketAddress socketAddress = new InetSocketAddress(SERVER, 5500);
            Socket sock = new Socket();
            try {
                sock.connect(socketAddress, 150);
                OutputStream stream = sock.getOutputStream();
                while (true) {
                    try {
                        Data data = queue.take();
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("x",data.x);
                        jsonObject.put("y",data.y);
                        jsonObject.put("z",data.z);
                        byte[] bytes = jsonObject.toString().getBytes();
                        if(!sock.isOutputShutdown())
                            stream.write(bytes);

                        //handle the data
                    } catch (InterruptedException  e) {
                        System.err.println("Error occurred:" + e);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        running.start();
    }

    /** method for clients */
    public void findServer(Callback obj) {

        Thread thread = new Thread(){

            private String getLocalIP(){
                String localIp = "";
                try {
                    Enumeration<NetworkInterface> enumNetworkInterfaces =  NetworkInterface.getNetworkInterfaces();

                    while(enumNetworkInterfaces.hasMoreElements()){
                        NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                        Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                        while(enumInetAddress.hasMoreElements()) {
                            InetAddress inetAddress = enumInetAddress.nextElement();
                            String[] ipPart = inetAddress.getHostName().split("\\.");
                            if(ipPart.length>0&&ipPart[0].equals("192")&&ipPart[1].equals("168"))
                                localIp = inetAddress.getHostName();
                        }
                    }

                } catch (SocketException e) {
                    e.printStackTrace();
                }
                return localIp;
            }

            private boolean isReachable(String host){

                boolean isReachableBool = false;
                try {
                    SocketAddress socketAddress = new InetSocketAddress(host, 5500);
                    Socket sock = new Socket();
                    sock.connect(socketAddress, 150);

                    isReachableBool = true;
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return isReachableBool;
            }

            @Override
            public void run() {
                super.run();
                String localIp = getLocalIP();
                if(localIp.equals("")){
                    obj.serverText("Not connected to WIFI");
                    return;
                }
                String[] ipPart = localIp.split("\\.");
                int inc = Integer.valueOf(ipPart[3]);
                int dec = inc;
                String host;
                String server="";
                while(dec>0||inc<255){
                    if(inc<255) {
                        host = ipPart[0] + "." + ipPart[1] + "." + ipPart[2] + "." + inc;
                        inc++;
                        obj.serverText("Checking "+host);
                        if (isReachable(host)) {
                            server = host;
                            break;
                        }
                    }
                    if(dec>0) {
                        host = ipPart[0] + "." + ipPart[1] + "." + ipPart[2] + "." + dec;
                        dec--;
                        obj.serverText("Checking "+host);
                        if (isReachable(host)) {
                            server = host;
                            break;
                        }
                    }
                }
                if(server.equals(""))
                    obj.serverText("No server found");
                else {
                    obj.serverText("Connected to " + server);
                    SERVER = server;
                    startServer();
                }
            }
        };
        thread.start();
    }
    public void postData(float x, float y, float z){
        if(!SERVER.equals("")) {
            queue.offer(new Data(x,y,z));
        }
    }
}