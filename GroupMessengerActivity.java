package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int[] REMOTE_PORT = {11108,11112,11116,11120,11124};
    static final int SERVER_PORT = 10000;
    static int num = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        // Reference from PA1

        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portString = telephonyManager.getLine1Number().substring(telephonyManager.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portString) * 2 ));

        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.e(TAG, "onCreate: Server Socket Created");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch(IOException e)
        {
            Log.e(TAG, "onCreate: Socket Creation Failed ");
        }


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */



        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Button OnClick Listener Instantiated ");
                String message = editText.getText().toString() + "\n";
                editText.setText("");
                TextView textView = (TextView) findViewById(R.id.textView1);
                textView.append("\t" + message);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message,myPort);


            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{

        // https://developer.android.com/reference/java/io/DataInputStream.html
        // https://developer.android.com/reference/java/io/DataOutputStream.html


        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {

            ServerSocket serverSocket = serverSockets[0];

            try{
                while(true)
                {
                    Log.e(TAG, "doInBackground: ServerTask Instantiated");
                    Socket socket = serverSocket.accept();
                    String message;
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    message = inputStream.readUTF();
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    if(message != null)
                    {
                        outputStream.writeUTF("OK");
                        publishProgress(message);
                        inputStream.close();
                        socket.close();
                        outputStream.flush();
                        outputStream.close();
                    }

                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Input Exceptions");
            }



            return  null;

        }

        protected void onProgressUpdate(String... values) {

            String received = values[0].trim();
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(received + "\t\n");

            //https://developer.android.com/reference/android/net/Uri

            Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
            ContentValues contentValues = new ContentValues();
            contentValues.put("key",Integer.toString(num));
            contentValues.put("value",received);
            getContentResolver().insert(uri,contentValues);
            num++;
            Log.e(TAG, "onProgressUpdate: KEY,VALUE Pair appended");

            return;

        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void>{


        @Override
        protected Void doInBackground(String... strings) {

            // https://developer.android.com/reference/java/io/DataInputStream.html
            // https://developer.android.com/reference/java/io/DataOutputStream.html

            try{
                for (int remotePort: REMOTE_PORT)
                {
                    Log.e(TAG, "doInBackground: ClientTask Instantiated");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
                    String msgToSend = strings[0];

                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                    dataOutputStream.writeUTF(msgToSend);
                    String acknowledgement = dataInputStream.readUTF();

                    if(acknowledgement.equals("OK"))
                    {
                        socket.close();

                    }

                    dataOutputStream.close();
                    dataInputStream.close();


                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "UnKnown Host Exception");
            } catch (IOException e) {
                Log.e(TAG, "Input Exceptions");
            }


            return null;
        }
    }

}
