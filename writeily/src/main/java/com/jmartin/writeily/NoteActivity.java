package com.jmartin.writeily;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.commonsware.cwac.anddown.AndDown;
import com.jmartin.writeily.dialog.ShareDialog;
import com.jmartin.writeily.model.Constants;
import com.jmartin.writeily.model.Note;

/**
 * Created by jeff on 2014-04-11.
 */
public class NoteActivity extends Activity {

    private Note note;
    private Context context;
    private EditText content;

    private String loadedFilename;


    public NoteActivity() {
    }

    public NoteActivity(Context context) {
        this.context = context;
        this.note = new Note();
    }

    public NoteActivity(Context context, Note note) {
        this.context = context;
        this.note = note;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_note);

        context = getApplicationContext();
        content = (EditText) findViewById(R.id.note_content);

        Intent receivingIntent = getIntent();
        String intentAction = receivingIntent.getAction();
        String type = receivingIntent.getType();

        if (Intent.ACTION_SEND.equals(intentAction) && type != null) {
            if ("text/plain".equals(type)) {
                note = new Note();
                note.setContent(receivingIntent.getStringExtra(Intent.EXTRA_TEXT));
            }
        } else {
            note = (Note) getIntent().getSerializableExtra(Constants.NOTE_KEY);
        }

        if (note == null) {
            note = new Note();
        } else {
            content.setText(note.getContent());
            loadedFilename = note.getRawFilename();
            getActionBar().setTitle(note.getTitle());
        }

        // Set up the font and background preferences
        setupAppearancePreferences();

        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_share:
                showShareDialog();
                return true;
            case R.id.action_preview:
                previewNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_slide_out_right, R.anim.anim_slide_in_right);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Constants.SHARE_BROADCAST_TAG);
        registerReceiver(shareBroadcastReceiver, ifilter);

        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(shareBroadcastReceiver);
        saveNote(note.update(content.getText().toString()));
        super.onPause();
    }

    private void setupAppearancePreferences() {
        String fontType = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_font_choice_key), "");
        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_theme_key), "");

        String fontSize = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_font_size_key), "");

        if (!fontSize.equals("")) {
            content.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(fontSize));
        }

        if (!fontType.equals("")) {
            content.setTypeface(Typeface.create(fontType, Typeface.NORMAL));
        }

        if (!theme.equals("")) {
            if (theme.equals(getString(R.string.theme_dark))) {
                content.setBackgroundColor(getResources().getColor(R.color.dark_grey));
                content.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                content.setBackgroundColor(getResources().getColor(android.R.color.white));
                content.setTextColor(getResources().getColor(R.color.dark_grey));
            }
        }
    }

    private void previewNote() {
        saveNote(note.update(content.getText().toString()));

        Intent intent = new Intent(this, PreviewActivity.class);

        // .replace is a workaround for Markdown lists requiring two \n characters
        intent.putExtra(Constants.MD_PREVIEW_KEY, note.getContent().replace("\n-", "\n\n-"));

        startActivity(intent);
    }

    private void shareNote(int type) {
        saveNote(note.update(content.getText().toString()));

        String shareContent = "";

        if (type == Constants.SHARE_TXT_TYPE) {
            shareContent = note.getContent();
        } else if (type == Constants.SHARE_HTML_TYPE) {
            AndDown andDown = new AndDown();
            shareContent = Constants.UNSTYLED_HTML_PREFIX +
                           andDown.markdownToHtml(note.getContent()) +
                           Constants.MD_HTML_SUFFIX;
        }

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_string)));
    }

    private void showShareDialog() {
        FragmentManager fragManager = getFragmentManager();
        ShareDialog shareDialog = new ShareDialog(this);
        shareDialog.show(fragManager, Constants.SHARE_DIALOG_TAG);
    }

    private void saveNote(boolean requiresOverwrite) {
        loadedFilename = note.save(context, loadedFilename, requiresOverwrite);
    }

    private BroadcastReceiver shareBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.SHARE_BROADCAST_TAG)) {
                shareNote(intent.getIntExtra(Constants.SHARE_TYPE_TAG, 0));
            }
        }
    };
}
