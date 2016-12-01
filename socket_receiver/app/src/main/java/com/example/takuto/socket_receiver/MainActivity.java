package com.example.takuto.socket_receiver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity  extends Activity {

    // ソケット関連
    private ServerSocket mServer;
    private Socket mSocket;
    int port = 1735;
    String output;

    // スレッド関連
    volatile Thread runner = null;
    Handler mHandler = new Handler();

    // レイアウト関連
    TextView tv;
    Button bt;
    EditText ed;

    // 送受信メッセージを格納
    String r_message;
    String s_message;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // レイアウトのID取得
        tv = (TextView) findViewById(R.id.LogView);
        bt = (Button) findViewById(R.id.button1);
        ed = (EditText) findViewById(R.id.TextSend);

        // ソケット通信開始と受信待ちの状態。
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mServer = new ServerSocket(port);
                    mSocket = mServer.accept();
                    ReceiveTask();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Toast.makeText(this, "Socket: Connected", Toast.LENGTH_SHORT).show();

        // btを押したときの処理を設定する
        //  →メッセージを送信する。
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_message = ed.getText().toString();
                tv.setText(s_message + "\n" + "send:message");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SendTask();
                    }
                }).start();
            }
        });

    }

    /* ソケット送信タスク */
    public void SendTask() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(),"MS932"));
            writer.write(s_message);
            writer.flush();

            Log.d("RESULTSS", "send ok " + s_message);
        } catch (IOException e) {
            Log.d("RESULTSS", "send out");
        }

    }

    /* ソケット受信タスク */
    public void ReceiveTask() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream(), "MS932"));
            final StringBuilder messageBuilder = new StringBuilder();

            while ((r_message = in.readLine()) != null) {
                messageBuilder.append(r_message);

                mHandler.post(new Runnable() {
                    JSONObject date;
                    String url_mes = new String();
                    String uri = new String();

                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {

                                    url_mes = new String(r_message.getBytes("Shift-JIS"), "Shift-JIS");
                                    Log.d("RESULTSS",url_mes);
                                    uri = new URI("https://chatbot-api.userlocal.jp/api/chat?message=" + url_mes + "&key=1459e8f5f23200343636").toASCIIString();
                                    Log.d("RESULTSS",uri);
                                    URL url = new URL(uri);
                                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                                    date = InputStreamToString(con.getInputStream());
                                    tv.setText(date.getString("result"));
                                } catch (Exception ex) {
                                    System.out.println(ex);
                                }
                            }
                        }).start();
                    }
                });
            }

            runner.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static JSONObject InputStreamToString(InputStream is) throws IOException {
        try {
            BufferedInputStream inputStream = new BufferedInputStream(is);
            StringBuilder sb = new StringBuilder();
            String line;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                if (length > 0) {
                    Log.d("JSON", Arrays.toString(buffer));
                    outputStream.write(buffer, 0, length);
                }
            }
            Log.d("JSON", "OK");
            return new JSONObject(new String(outputStream.toByteArray()));
        } catch (IOException exception) {
            // 処理なし
            Log.d("JSON", "ERROR" + exception);
        } catch (JSONException e) {
            Log.d("JSON", "ERROR " + e);
            e.printStackTrace();
        }

        return null;
    }
}