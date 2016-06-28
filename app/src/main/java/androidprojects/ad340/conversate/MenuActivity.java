package androidprojects.ad340.conversate;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        TextView title = (TextView) findViewById(R.id.title);
        title.setText("Conversate");

        Button startButton = (Button) findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    Intent startChatActivity = new Intent(MenuActivity.this,
                            VoiceChatActivity.class);
                    startActivity(startChatActivity);

                } catch (ActivityNotFoundException e) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Your device couldn't connect to Conversate",
                            Toast.LENGTH_SHORT);

                    t.show();
                }
            }

        });
    }

}
