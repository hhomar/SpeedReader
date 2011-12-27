package hhomar.speedreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SpeedReader extends Activity {
	private final String APP_KEY = "speed-reader";
	private final String TAG = "SpeedReader";
	
	private TextView textView;
	private SeekBar seekBar;
	private ImageButton playButton;
	private ClipboardManager clipboard;
	
	private int wpm;
	private String[] splitText;
	private int wordsPerSecond = 0;
	private int textLength = 0;
	private int currPosition = 0;
	private boolean running = false;
	
	private Handler updateTextHandler = new Handler();

//	private String debugText = "One baby to another says I'm lucky to have meet you " +
//								"I don't care what you think unless it is about me";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
   
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        textView = (TextView) findViewById(R.id.TextView);
        seekBar = (SeekBar) findViewById(R.id.SeekBar);

        playButton = (ImageButton) findViewById(R.id.PasteButton);
            
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        wpm = settings.getInt("wpm", 300);
        
        if (savedInstanceState == null) {
        	CheckClipboard();
        	//SetText(debugText);
        } else {
        	Bundle map = savedInstanceState.getBundle(APP_KEY);
        	if (map != null)
        		RestoreState(map);
        	else
        		CheckClipboard();
        }

        playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!running) {
			    	playButton.setImageResource(android.R.drawable.ic_media_pause);
			    	updateTextHandler.removeCallbacks(updateText);
			    	updateTextHandler.postDelayed(updateText, 1000 / wordsPerSecond);
					running = true;
				}
				else {
					Pause();
				}
			}
		});
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					//Log.d(TAG, " Before: currPosition: " + currPosition + " progress: " + progress);
					currPosition = progress;
					//Log.d(TAG, " After : currPosition: " + currPosition + " progress: " + progress);
					textView.setText(splitText[currPosition]);
				}
			}

			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}
		});
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	
    	Pause();
    }
    
    // From Emulator, test with Control-F11.
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
    	savedInstanceState.putBundle(APP_KEY, SaveState());
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void onStop() 
    {
    	super.onStop();
    	
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("wpm", wpm);
        editor.commit();    	
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pasteItem:
            	CheckClipboard();
                break;
            case R.id.wpmItem:
            	AlertDialog.Builder wpmDialog = new AlertDialog.Builder(this);
            	wpmDialog.setTitle(R.string.wpm);
            	wpmDialog.setMessage(R.string.wpm_input);
            	final EditText input = new EditText(this);
            	input.setInputType(InputType.TYPE_CLASS_NUMBER);
            	input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });
            	input.setText("" + wpm);
            	wpmDialog.setView(input);
            	wpmDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {  
            		public void onClick(DialogInterface dialog, int whichButton) {
            			if (!TextUtils.isEmpty(input.getText()))
            				SpeedReader.this.SetWPM(input.getText().toString());
            		} 
            		
            	});  	   
            	wpmDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {  
            		   public void onClick(DialogInterface dialog, int whichButton) {  
            		     // Cancelled.  
            		   }  
            	});  
            	wpmDialog.show();

                break;
        }
        return true;
    }
    
    private void CheckClipboard()
    {
    	String text = clipboard.getText().toString();
		SetText(text);
    	if (!text.isEmpty()) {
    		Toast.makeText(this, R.string.begin_speed_reading, Toast.LENGTH_LONG).show();
    	}
    }
    
    private void SetWPM(String w)
    {
    	if (!w.isEmpty()) {
    		wpm = Integer.parseInt(w);
        	wordsPerSecond = wpm / 60;
    		String wpmMessage = getString(R.string.wpm) + " " + getString(R.string.wpm_set_at) + " " + wpm;
   			Toast.makeText(this, wpmMessage, Toast.LENGTH_LONG).show();
    	}
    }
    
    private void SetText(String text)
    {	
    	boolean hasText = !text.isEmpty();
    
    	splitText = text.split("\\s");
    	textLength = splitText.length;
    	wordsPerSecond = wpm / 60;
		
    	seekBar.setMax(textLength);
    	playButton.setEnabled(hasText);
    	seekBar.setEnabled(hasText);
    }

    private void Pause()
    {
	    playButton.setImageResource(android.R.drawable.ic_media_play);
	    updateTextHandler.removeCallbacks(updateText);
	    running = false;
    }
    
    private Bundle SaveState()
    {
    	Bundle map = new Bundle();
    	
    	map.putInt("wps", Integer.valueOf(wordsPerSecond));
    	map.putInt("cp", Integer.valueOf(currPosition));
    	map.putInt("tlen", Integer.valueOf(textLength));
    	map.putStringArray("txt", splitText);
    	
    	Log.d(TAG, "SaveState");
    	
    	return map;
    }
    
    private void RestoreState(Bundle map)
    {
    	wordsPerSecond = map.getInt("wps");
    	currPosition = map.getInt("cp");
    	textLength = map.getInt("tlen");
    	splitText = map.getStringArray("txt");
    	
    	textView.setText(splitText[currPosition]);
    	seekBar.setProgress(currPosition);
    }
    
    private Runnable updateText = new Runnable() {
    	public void run()
    	{
    		textView.setText(splitText[currPosition++]);
    		seekBar.setProgress(currPosition);
    		if (currPosition >= textLength) {
    			playButton.setImageResource(android.R.drawable.ic_media_play);
    			updateTextHandler.removeCallbacks(updateText);
    			currPosition = 0;
    			running = false;
    			return;
    		}
    		updateTextHandler.postDelayed(updateText, 1000 / wordsPerSecond);		
    	}
    };
}