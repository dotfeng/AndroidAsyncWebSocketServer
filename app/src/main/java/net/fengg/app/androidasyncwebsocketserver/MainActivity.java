package net.fengg.app.androidasyncwebsocketserver;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.AsyncNetworkSocket;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.WebSocket.PingCallback;
import com.koushikdutta.async.http.WebSocket.PongCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    EditText et_port;
    EditText et_send;
    EditText et_ip;
    Button btn_start;
    Button btn_stop;
    Button btn_send;
    TextView txt_text;
    AsyncHttpServer server;
    List<WebSocket> _sockets = new ArrayList<WebSocket>();
    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_port = (EditText) findViewById(R.id.et_port);
        et_send = (EditText) findViewById(R.id.et_send);
        et_ip = (EditText) findViewById(R.id.et_ip);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_send = (Button) findViewById(R.id.btn_send);
        txt_text = (TextView) findViewById(R.id.txt_text);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webserver(Integer.parseInt(et_port.getEditableText().toString()), "/.*");
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(et_ip.getEditableText().toString(), et_send.getEditableText().toString());
            }
        });
    }

    private void webserver(int port, String uri) {
        server = new AsyncHttpServer();

        server.get("/live/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request,
                                  AsyncHttpServerResponse response) {
                String address = ((AsyncNetworkSocket)request.getSocket()).getRemoteAddress().toString();
                response.send("welcome " + address + ", " + request.getPath());
            }
        });

        // listen on port
        AsyncServerSocket socket = server.listen(port);
        if(null == socket) {
            showToast("start failed");
            return;
        }
        showToast("start successfully");
        server.websocket("/ws/.*", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                String address = ((AsyncNetworkSocket)request.getSocket()).getRemoteAddress().toString();
                Log.i(TAG, address);
                _sockets.add(webSocket);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listClient();
                        showToast("new connect");
                    }
                });

                webSocket.send("welcome " + address + ", " + request.getPath());

                //Use this to clean up any references to your websocket
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        Log.i(TAG, "close");
                        try {
                            if (ex != null)
                                Log.e(TAG, "Error");
                        } finally {
                            _sockets.remove(webSocket);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listClient();
                                    showToast("new disconnect");
                                }
                            });
                        }
                    }
                });

                webSocket.setStringCallback(new StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        Log.i(TAG, "string:" + s);
                        webSocket.send(s);
                    }
                });
                webSocket.setPingCallback(new PingCallback() {

                    @Override
                    public void onPingReceived(String s) {
                        Log.i(TAG, "ping:" + s);
                        webSocket.ping("ping");
                    }
                });
                webSocket.setPongCallback(new PongCallback() {

                    @Override
                    public void onPongReceived(String s) {
                        Log.i(TAG, "pong:" + s);
                        webSocket.pong("pong");
                    }
                });

            }
        });
    }

    private void stop() {
        server.stop();
    }

    private void send(String ip, String message) {
    	for (WebSocket webSocket : _sockets) {
            if(null == ip || "".equals(ip) ||
                    ((AsyncNetworkSocket)webSocket.getSocket())
                            .getRemoteAddress().toString().contains(ip)) {
                webSocket.send(message);
            }
        }
    }

    private synchronized void listClient() {
        StringBuilder sb = new StringBuilder();
        for (WebSocket webSocket : _sockets) {
            sb.append(((AsyncNetworkSocket)webSocket.getSocket())
                    .getRemoteAddress().toString());
            sb.append("\n");
        }
        txt_text.setText(sb.toString());
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
