package com.filemanager;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.filemanager.entity.FileEntity;

import java.io.File;

/**
 * This is the main activity. The activity that is presented to the user
 * as the application launches. This class is, and expected not to be, instantiated.
 * <br>
 * <p/>
 * This class handles creating the list.
 * This class relies on the class EventHandler to handle all button
 * press logic and to control the data displayed on its ListView. This class
 * also relies on the FileManager class to handle all file operations.
 * However most interaction with the FileManager class
 * is done via the EventHandler class. Also the SettingsMangager class to load
 * and save user settings.
 */
public final class Main extends ListActivity {
    private static final String PREFS_NAME = "ManagerPrefsFile";    //user preference file name
    private static final String PREFS_HIDDEN = "hidden";
    private static final String PREFS_COLOR = "color";
    private static final String PREFS_THUMBNAIL = "thumbnail";
    private static final String PREFS_SORT = "sort";
    private static final String PREFS_STORAGE = "sdcard space";

    private static final int SEARCH_B = 0x09;

    private FileManager mFileMag;
    private EventHandler mHandler;
    private EventHandler.TableRow mTable;

    private SharedPreferences mSettings;
    private boolean mReturnIntent = false;
    private boolean mUseBackKey = true;
    private TextView mPathLabel, mDetailLabel, mStorageLabel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        /*read settings*/
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        boolean hide = mSettings.getBoolean(PREFS_HIDDEN, false);
        boolean thumb = mSettings.getBoolean(PREFS_THUMBNAIL, true);
        int space = mSettings.getInt(PREFS_STORAGE, View.VISIBLE);
        int color = mSettings.getInt(PREFS_COLOR, -1);
        int sort = mSettings.getInt(PREFS_SORT, 3);

        mFileMag = new FileManager();
        mFileMag.setShowHiddenFiles(hide);
        mFileMag.setSortType(sort);

        if (savedInstanceState != null)
            mHandler = new EventHandler(Main.this, mFileMag, savedInstanceState.getString("location"));
        else
            mHandler = new EventHandler(Main.this, mFileMag);

        mHandler.setTextColor(color);
        mHandler.setShowThumbnails(thumb);
        mTable = mHandler.new TableRow();
        
        /*sets the ListAdapter for our ListActivity and
         *gives our EventHandler class the same adapter
         */
        mHandler.setListAdapter(mTable);
        setListAdapter(mTable);
        
        /* register context menu for our list view */
        registerForContextMenu(getListView());

        mStorageLabel = (TextView) findViewById(R.id.storage_label);
        mDetailLabel = (TextView) findViewById(R.id.detail_label);
        mPathLabel = (TextView) findViewById(R.id.path_label);
        mPathLabel.setText("path: /sdcard");

        updateStorageLabel();
        mStorageLabel.setVisibility(space);

        mHandler.setUpdateLabels(mPathLabel, mDetailLabel);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("location", mFileMag.getCurrentDir());
    }

    private void updateStorageLabel() {
        long total, aval;
        int kb = 1024;

        StatFs fs = new StatFs(Environment.
                getExternalStorageDirectory().getPath());

        total = fs.getBlockCount() * (fs.getBlockSize() / kb);
        aval = fs.getAvailableBlocks() * (fs.getBlockSize() / kb);

        mStorageLabel.setText(String.format("sdcard: Total %.2f GB " +
                        "\t\tAvailable %.2f GB",
                (double) total / (kb * kb), (double) aval / (kb * kb)));
    }

    /**
     * To add more functionality and let the user interact with more
     * file types, this is the function to add the ability.
     * <p/>
     * (note): this method can be done more efficiently
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        final FileEntity item = (FileEntity) mHandler.getData(position);
        boolean multiSelect = mHandler.isMultiSelected();
        File file = new File(mFileMag.getCurrentDir() + "/" + item.getFileName());
        String item_ext = null;

        try {
            item_ext = item.getFileName().substring(item.getFileName().lastIndexOf("."), item.getFileName().length());

        } catch (IndexOutOfBoundsException e) {
            item_ext = "";
        }

    	/*
    	 * If the user has multi-select on, we just need to record the file
    	 * not make an intent for it.
    	 */
        if (multiSelect) {
            mTable.addMultiPosition(position, file.getPath());

        } else {
            if (file.isDirectory()) {
                if (file.canRead()) {
                    mHandler.stopThumbnailThread();
                    mHandler.updateDirectory(mFileMag.getNextDir(item.getFileName(), false));
                    mPathLabel.setText(mFileMag.getCurrentDir());
		    		
		    		/*set back button switch to true 
		    		 * (this will be better implemented later)
		    		 */
                    if (!mUseBackKey)
                        mUseBackKey = true;

                } else {
                    Toast.makeText(this, "Can't read folder due to permissions",
                            Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    /*
     * (non-Javadoc)
     * This will check if the user is at root directory. If so, if they press back
     * again, it will close the application. 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        String current = mFileMag.getCurrentDir();

        if (keycode == KeyEvent.KEYCODE_SEARCH) {
            showDialog(SEARCH_B);

            return true;

        } else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && !current.equals("/")) {
            if (mHandler.isMultiSelected()) {
                mTable.killMultiSelect(true);
                Toast.makeText(Main.this, "Multi-select is now off", Toast.LENGTH_SHORT).show();

            } else {
                //stop updating thumbnail icons if its running
                mHandler.stopThumbnailThread();
                mHandler.updateDirectory(mFileMag.getPreviousDir());
                mPathLabel.setText(mFileMag.getCurrentDir());
            }
            return true;

        } else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && current.equals("/")) {
            Toast.makeText(Main.this, "Press back again to quit.", Toast.LENGTH_SHORT).show();

            if (mHandler.isMultiSelected()) {
                mTable.killMultiSelect(true);
                Toast.makeText(Main.this, "Multi-select is now off", Toast.LENGTH_SHORT).show();
            }

            mUseBackKey = false;
            mPathLabel.setText(mFileMag.getCurrentDir());

            return false;

        } else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey && current.equals("/")) {
            finish();

            return false;
        }
        return false;
    }
}
