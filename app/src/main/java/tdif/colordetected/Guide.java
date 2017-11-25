package tdif.colordetected;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Guide extends AppCompatActivity implements View.OnClickListener{
    Button btnCamera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(this);

    }
    public void onClick(View v) {
        Intent i = new Intent(this, MainActivity.class);

        startActivity(i);
    }
}
