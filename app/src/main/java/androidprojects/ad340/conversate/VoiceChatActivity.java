package androidprojects.ad340.conversate;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceChatActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    // layout reference
    private LinearLayout layout;

    // button references
    private FloatingActionButton recordButton;
    private Button typeButton;

    // EditText reference for composing message
    private EditText textMessage;

    // TextView references for all sent messages
    private List<TextView> sentMessages;

    // ChatterBot variables get stored here
    ChatterBot conversateBot;
    ChatterBotSession conversateSession;
    private String currentMessage;
    private String currentResponse;
    private boolean isTryingToGetResponse;

    // Text to Speech and Speech to Text
    private int MY_TTS_DATA_CHECK_CODE = 42;
    private int REQ_CODE_SPEECH_INPUT = 43;
    private TextToSpeech conversateTTS;

    // Chat Labels
    private static final String BOT_LABEL = "Cleverbot";
    private static final String USER_LABEL = "Me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat);
        createScrollView();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        TextView title = (TextView) findViewById(R.id.title);
        title.setText("Talking to Cleverbot");

        // instantiate the sent messages list
        sentMessages = new ArrayList<>();

        // instantiate ChatterBot
        createChatterSession();

        // instantiate text to speech
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_TTS_DATA_CHECK_CODE);

        // handle the buttons
        recordButton = (FloatingActionButton) findViewById(R.id.voiceButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }

        });

        typeButton = (Button) findViewById(R.id.type_button);
        typeButton.setOnClickListener(onTypeClick());
    }

    /**
     * Install Text to Speech data when the application initializes
     * */
    public void onInit(int initStatus) {
        // if text to speech initiallized successfully and English is available on device
        if (initStatus == TextToSpeech.SUCCESS &&
                conversateTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE) {
            conversateTTS.setLanguage(Locale.US);

        } else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Your Device does not support Text to Speech",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show google speech input dialog, handler for the record button
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk!");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Speech to Text is not supported on your device!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the activity results for Speech to Text and Text to Speech
     * */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                //the device has the necessary data - create the TTS
                conversateTTS = new TextToSpeech(this, this);
            }
            else {
                //no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        } else
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {

            ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String message = text.get(0);

            // FACTOR AFTER give the string from EditText to the new TextView
            final TextView userMessage = createNewTextView(message, false);
            layout.addView(userMessage);
            userMessage.startAnimation(AnimationUtils.loadAnimation(VoiceChatActivity.this,
                    android.R.anim.slide_in_left));

            // update global variable because getting a response makes a web request
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
            responseMessage.startAnimation(AnimationUtils.loadAnimation(VoiceChatActivity.this,
                    android.R.anim.slide_in_left));

            // print for debugging the current messages and responses
            System.out.println("Current message: " + currentMessage +
                    ", and current response: " + currentResponse);

            if (conversateTTS != null) {
                conversateTTS.speak(currentResponse, TextToSpeech.QUEUE_ADD, null);
            }

            // now that we have our responses, we have to close the AsyncTask
            isTryingToGetResponse = false;
            currentResponse = null;
            currentMessage = null;
        }
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

                    // FACTOR AFTER give the string from EditText to the new TextView
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

                    if (conversateTTS != null) {
                        conversateTTS.speak(currentResponse, TextToSpeech.QUEUE_ADD, null);
                    }

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
        final LinearLayout.LayoutParams params;

        // create new EditText
        textMessage = new EditText(this);
        params = getTextViewParams(false, true);
        textMessage.setLayoutParams(params);
        textMessage.setHint("Type text here!");
        textMessage.requestFocus();
        textMessage.setHintTextColor(ContextCompat.getColor(this, R.color.text));
        textMessage.setTextColor(ContextCompat.getColor(this, R.color.text));

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
        final LinearLayout.LayoutParams params;

        // place current message
        TextView sentMessage = new TextView(this);
        params = getTextViewParams(isChatterBot, false);
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

        return sentMessage;
    }

    /**
     * Helper function for createNewTextView
     * @param isChatterBot determines left or right alignment
     * @return LayoutParams to attach to a TextView
     */
    private LinearLayout.LayoutParams getTextViewParams(boolean isChatterBot, boolean isEditText) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // add xml layout rules to this new EditText
        if (isChatterBot || isEditText) {
            params.gravity = Gravity.LEFT;
        } else {
            params.gravity = Gravity.RIGHT;
        }

        if (isEditText) {
            params.width = layout.getWidth() - typeButton.getWidth() - 10;
            params.gravity = Gravity.BOTTOM;
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

    @Override
    protected void onStop() {
        super.onStop();
        conversateTTS.shutdown();
    }

    /**
     * Creates a new chatter session using the API and stores it
     * */
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

    /**
     * Programmatically creates a scrollview for the messages to populate,
     * and adds it to the existing RelativeLayout from "content_voice_chat.xml"
     * */
    private void createScrollView() {
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.chat_layout);
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));
        layout = new LinearLayout(this);
        layout.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        sv.addView(layout);
        rl.addView(sv);
    }

    /**
     * Create Chat Session AsyncTask in order to use multiple threads,
     * for the purpose of not making HTTP requests on the UI thread.
     * */
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

    /**
     * Get Chat Response AsyncTask in order to use multiple threads,
     * for the purpose of not making HTTP requests on the UI thread.
     * */
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