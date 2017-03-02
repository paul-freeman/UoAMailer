package nz.ac.auckland.lablet.mailer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DebugLaunch extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    Intent startMain = new Intent(Intent.ACTION_MAIN);
    startMain.addCategory(Intent.CATEGORY_HOME);
    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(startMain);
  }
}
