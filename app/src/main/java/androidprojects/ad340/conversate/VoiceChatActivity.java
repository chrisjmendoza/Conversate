package androidprojects.ad340.conversate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

import java.util.ArrayList;
import java.util.List;

public class VoiceChatActivity extends AppCompatActivity {

    // layout reference
    private RelativeLayout layout;

    // button references
    private FloatingActionButton recordButton;
    private Button typeButton;

    // EditText reference for composing message
    private EditText textMessage;

    // TextView reference for sent message
    private List<TextView> sentMessages;
    private int nextSentMessageId;

    // Chatterbot gets stored here
    ChatterBot conversate_bot;
    ChatterBotSession conversate_session;
    private String currentMessage;
    private String currentResponse;
    private boolean isTryingToGetResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat);
        layout = (RelativeLayout) findViewById(R.id.chat_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // instantiate the sent messages list
        sentMessages = new ArrayList<TextView>();
        nextSentMessageId = 1;

        //instantiate Chatterbot
        createChatterSession();

        // handle the buttons
        recordButton = (FloatingActionButton) findViewById(R.id.voiceButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Should record voice for Conversate", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        typeButton = (Button) findViewById(R.id.type_button);
        typeButton.setOnClickListener(onTypeClick());
    }

    // This onclick listener method is triggered when the type/send button is clicked
    private View.OnClickListener onTypeClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // depending on the button text, we have different behavior
                String buttonText = typeButton.getText().toString();
                if (buttonText.equals("Type")) {
                    layout.addView(createNewEditText());
                    recordButton.setVisibility(View.INVISIBLE);
                    typeButton.setText("Send");

                } else if (buttonText.equals("Send")) {
                    String message = textMessage.getText().toString();

                    // give the string from EditText to the new TextView
                    layout.addView(createNewTextView(message, false));
                    recordButton.setVisibility(View.VISIBLE);
                    typeButton.setText("Type");

                    // get rid of that damn keyboard
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textMessage.getWindowToken(), 0);

                    // remove the edit view when the user sends a message
                    layout.removeView(textMessage);

                    // this is all because getting a response makes a web request
                    // and web requests can't happen on the UI thread
                    currentMessage = message;
                    isTryingToGetResponse = true;
                    ChatResponseTask getResponseTask = new ChatResponseTask();
                    getResponseTask.execute();

                    // wait until we get a response back from cleverbot
                    while (currentResponse == null) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    layout.addView(createNewTextView(currentResponse, true));

                    // print for debugging the current messages and responses
                    System.out.println("Current message: " + currentMessage +
                            ", and current response:" + currentResponse);
                    
                    // now that we have our responses, we have to close the AsyncTask
                    isTryingToGetResponse = false;
                    currentResponse = null;
                    currentMessage = null;
                }
            }
        };
    }

    // Creates a new EditText when you click "type" to compose a message 
    // Should also set focus to EditText and bring up keyboard
    private EditText createNewEditText() {
        final RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

        // add xml layout rules to this new EditText
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        textMessage = new EditText(this);
        textMessage.setLayoutParams(params);
        textMessage.setHint("Type text here!");

        textMessage.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        return textMessage;
    }

    // Creates a new TextView when you click "send" to send a message 
    // Should also set focus to EditText and bring up keyboard
    private TextView createNewTextView(String message, boolean isChatterbot) {
        final RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

        // add xml layout rules to this new EditText
        if (isChatterbot) {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        }

        if (sentMessages.isEmpty()) {
            params.addRule(RelativeLayout.ABOVE, typeButton.getId());
        } else {
            params.addRule(RelativeLayout.ABOVE, sentMessages.get(sentMessages.size() - 1).getId());
        }

        TextView sentMessage = new TextView(this);
        sentMessage.setId(nextSentMessageId);
        nextSentMessageId++;
        sentMessage.setLayoutParams(params);
        sentMessage.setText(message);
        sentMessages.add(sentMessage);
        return sentMessage;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_voice_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createChatterSession() {
        ChatterBotFactory factory = new ChatterBotFactory();

        try {
            conversate_bot = factory.create(ChatterBotType.CLEVERBOT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CreateChatSessionTask createSessionTask = new CreateChatSessionTask();
        createSessionTask.execute();
    }

    class CreateChatSessionTask extends AsyncTask<String, Void, Void> {

        private Exception exception;

        protected void onPostExecute() {
            // TODO: check this.exception
            // TODO: do something with the app
        }

        @Override
        protected Void doInBackground(String... params) {
            while (conversate_session == null) {
                try {
                    System.out.println("Trying to start session");
                    conversate_session = conversate_bot.createSession();
                } catch (Exception e) {
                    this.exception = e;
                    System.out.println("Failed to start session");
                }
                try {
                    Thread.sleep(1000);
                    System.out.println("trying again!! Are we online?" + isOnline());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        public boolean isOnline() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
    }

    class ChatResponseTask extends AsyncTask<String, Void, Void> {

        private Exception exception;

        @Override
        protected Void doInBackground(String... params) {
            while (currentResponse == null && isTryingToGetResponse) {
                try {
                    System.out.println("Trying to get response");
                    currentResponse = conversate_session.think(currentMessage);
                } catch (Exception e) {
                    this.exception = e;
                    System.out.println("Failed to get response");
                }
                try {
                    Thread.sleep(1000);
                    System.out.println("trying for response again!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
