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
import android.widget.Toast;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton startButton = (FloatingActionButton) findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try{
                    startActivity(new Intent(MenuActivity.this, VoiceChatActivity.class));
                } catch(ActivityNotFoundException e) {
                    Toast t = Toast.makeText(getApplicationContext(), "Your Device does not support Speech to Text", Toast.LENGTH_SHORT);

                    t.show();
                }
            }

        });
    }

}
