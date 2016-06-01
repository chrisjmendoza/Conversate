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
import android.view.animation.AnimationUtils;
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

    // TextView references for sent messages
    private List<TextView> sentMessages;
    private int thisSentMessageId;

    // ChatterBot gets stored here
    ChatterBot conversateBot;
    ChatterBotSession conversateSession;
    private String currentMessage;
    private String currentResponse;
    private boolean isTryingToGetResponse;

    // Chat Labels
    private static final String BOT_LABEL = "Cleverbot";
    private static final String USER_LABEL = "Me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat);
        layout = (RelativeLayout) findViewById(R.id.chat_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // instantiate the sent messages list
        sentMessages = new ArrayList<TextView>();
        thisSentMessageId = 0;

        // instantiate ChatterBot
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

    /**
     * This onClickListener method is triggered when the Type/Send button is clicked
     * @return OnClickListener
     */
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
                    // get rid of that damn keyboard
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textMessage.getWindowToken(), 0);

                    String message = textMessage.getText().toString();

                    // give the string from EditText to the new TextView
                    final TextView userMessage = createNewTextView(message, false);
                    layout.addView(userMessage);
                    userMessage.startAnimation(AnimationUtils.loadAnimation(VoiceChatActivity.this, android.R.anim.slide_in_left));

                    recordButton.setVisibility(View.VISIBLE);
                    typeButton.setText("Type");

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
                        //do nothing
                    }

                    final TextView responseMessage = createNewTextView(currentResponse, true);
                    layout.addView(responseMessage);
                    responseMessage.startAnimation(AnimationUtils.loadAnimation(VoiceChatActivity.this, android.R.anim.slide_in_left));

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

    /**
     * Creates new EditText when you click "Type" to compose a message
     * Sets focus to EditText and brings up keyboard
     * @return EditText
     */
    private EditText createNewEditText() {
        final RelativeLayout.LayoutParams params;

        // create new EditText
        textMessage = new EditText(this);
        params = getTextViewParams(0, false, true);
        textMessage.setLayoutParams(params);
        textMessage.setHint("Type text here!");
        textMessage.requestFocus();
        textMessage.setHintTextColor(getResources().getColor(R.color.text));
        textMessage.setTextColor(getResources().getColor(R.color.text));

        // bring up keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        return textMessage;
    }

    /**
     * Creates new TextView when you click "Send" to send a message
     * @param message to place in TextView
     * @param isChatterBot determines left or right alignment
     * @return message TextView
     */
    private TextView createNewTextView(String message, boolean isChatterBot) {
        final RelativeLayout.LayoutParams params, oldMessageParams;

        // place current message
        thisSentMessageId++;
        TextView sentMessage = new TextView(this);
        sentMessage.setId(thisSentMessageId);
        params = getTextViewParams(typeButton.getId(), isChatterBot, false);
        sentMessage.setLayoutParams(params);
        sentMessage.setTextColor(getResources().getColor(R.color.text));
        sentMessage.setBackgroundColor(getResources().getColor(R.color.messageBackground));
        sentMessage.setPadding(10, 10, 10, 10);

        // set the correct message label
        if (isChatterBot) {
            sentMessage.setText(BOT_LABEL + ":    " + message);
        } else {
            sentMessage.setText(USER_LABEL + ":    " + message);
        }
        sentMessages.add(sentMessage);

        // place previous message (above current message)
        if (sentMessages.size() > 1) {
            TextView oldMessage = sentMessages.get(sentMessages.size() - 2);
            oldMessageParams = getTextViewParams(thisSentMessageId, !isChatterBot, false);
            oldMessage.setLayoutParams(oldMessageParams);
        }

        return sentMessage;
    }

    /**
     * Helper function for createNewTextView
     * @param id to place old message above
     * @param isChatterBot determines left or right alignment
     * @return LayoutParams to attach to a TextView
     */
    private RelativeLayout.LayoutParams getTextViewParams(int id, boolean isChatterBot, boolean isEditText) {
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        // add xml layout rules to this new EditText
        if (isChatterBot || isEditText) {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        }

        if (isEditText) {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            params.addRule(RelativeLayout.ABOVE, id);
        }

        return params;
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
            conversateBot = factory.create(ChatterBotType.CLEVERBOT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CreateChatSessionTask createSessionTask = new CreateChatSessionTask();
        createSessionTask.execute();
    }

    class CreateChatSessionTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            while (conversateSession == null) {
                try {
                    System.out.println("Trying to start session. Silence means success.");
                    conversateSession = conversateBot.createSession();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to start session");
                }
                if (conversateSession == null) {
                    try {
                        Thread.sleep(1000);
                        System.out.println("trying to start session again!! Are we online? "
                                + isOnline());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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

        @Override
        protected Void doInBackground(String... params) {
            while (currentResponse == null && isTryingToGetResponse) {
                try {
                    System.out.println("Trying to get response. Silence means success.");
                    currentResponse = conversateSession.think(currentMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to get response");
                }
                if (currentResponse == null) {
                    System.out.println("Trying for response again..");
                }
            }
            return null;
        }
    }
}
