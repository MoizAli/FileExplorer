package com.filemanager;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.filemanager.entity.FileEntity;

import java.io.File;
import java.util.ArrayList;

/**
 * This class sits between the Main activity and the FileManager class.
 * To keep the FileManager class modular, this class exists to handle
 * UI events and communicate that information to the FileManger class
 * <p/>
 * This class is responsible for handling the information that is displayed
 * from the list view (the files and folder) with a a nested class TableRow.
 * The TableRow class is responsible for displaying which icon is shown for each
 * entry. For example a folder will display the folder icon, a Word doc will
 * display a word icon and so on. If more icons are to be added, the TableRow
 * class must be updated to display those changes.
 *
 */
public class EventHandler {
    private final Context mContext;
    private final FileManager mFileMang;
    private ThumbnailCreator mThumbnail;
    private TableRow mDelegate;

    private boolean multi_select_flag = false;
    private boolean thumbnail_flag = true;
    private int mColor = Color.WHITE;

    //the list used to feed info into the array adapter and when multi-select is on
    private ArrayList<FileEntity> mDataSource, mMultiSelectData;
    private TextView mPathLabel;
    private TextView mInfoLabel;
    ArrayList<String> arrayFilename = new ArrayList<String>();


    /**
     * Creates an EventHandler object. This object is used to communicate
     * most work from the Main activity to the FileManager class.
     *
     * @param context The context of the main activity e.g  Main
     * @param manager The FileManager object that was instantiated from Main
     */
    public EventHandler(Context context, final FileManager manager) {
        mContext = context;
        mFileMang = manager;

        mDataSource = new ArrayList<FileEntity>(mFileMang.setHomeDir
                (Environment.getExternalStorageDirectory().getPath()));

        setFileArray();

    }

    private void setFileArray() {
        arrayFilename.clear();
        for (FileEntity fileEntity : mDataSource) {
            arrayFilename.add(fileEntity.getFileName());
        }
    }

    /**
     * This constructor is called if the user has changed the screen orientation
     * and does not want the directory to be reset to home.
     *
     * @param context  The context of the main activity e.g  Main
     * @param manager  The FileManager object that was instantiated from Main
     * @param location The first directory to display to the user
     */
    public EventHandler(Context context, final FileManager manager, String location) {
        mContext = context;
        mFileMang = manager;

        mDataSource = new ArrayList<FileEntity>(mFileMang.getNextDir(location, true));
        setFileArray();
    }

    /**
     * This method is called from the Main activity and this has the same
     * reference to the same object so when changes are made here or there
     * they will display in the same way.
     *
     * @param adapter The TableRow object
     */
    public void setListAdapter(TableRow adapter) {
        mDelegate = adapter;
    }

    /**
     * This method is called from the Main activity and is passed
     * the TextView that should be updated as the directory changes
     * so the user knows which folder they are in.
     *
     * @param path  The label to update as the directory changes
     * @param label the label to update information
     */
    public void setUpdateLabels(TextView path, TextView label) {
        mPathLabel = path;
        mInfoLabel = label;
    }

    /**
     * @param color
     */
    public void setTextColor(int color) {
        mColor = color;
    }

    /**
     * Set this true and thumbnails will be used as the icon for image files. False will
     * show a default image.
     *
     * @param show
     */
    public void setShowThumbnails(boolean show) {
        thumbnail_flag = show;
    }

    /**
     * Indicates whether the user wants to select
     * multiple files or folders at a time.
     * <br><br>
     * false by default
     *
     * @return true if the user has turned on multi selection
     */
    public boolean isMultiSelected() {
        return multi_select_flag;
    }

    /**
     * Use this method to determine if the user has selected multiple files/folders
     *
     * @return returns true if the user is holding multiple objects (multi-select)
     */
    public boolean hasMultiSelectData() {
        return (mMultiSelectData != null && mMultiSelectData.size() > 0);
    }

   /**
     * this will stop our background thread that creates thumbnail icons
     * if the thread is running. this should be stopped when ever
     * we leave the folder the image files are in.
     */
    public void stopThumbnailThread() {
        if (mThumbnail != null) {
            mThumbnail.setCancelThumbnails(true);
            mThumbnail = null;
        }
    }

    /**
     * will return the data in the ArrayList that holds the dir contents.
     *
     * @param position the indext of the arraylist holding the dir content
     * @return the data in the arraylist at position (position)
     */
    public FileEntity getData(int position) {

        if (position > mDataSource.size() - 1 || position < 0)
            return null;

        return mDataSource.get(position);
    }

    /**
     * called to update the file contents as the user navigates there
     * phones file system.
     *
     * @param content an ArrayList of the file/folders in the current directory.
     */
    public void updateDirectory(ArrayList<FileEntity> content) {
        if (!mDataSource.isEmpty())
            mDataSource.clear();

        for (FileEntity data : content)
            mDataSource.add(data);

        setFileArray();

        mDelegate.notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView topView;
        TextView bottomView;
        ImageView icon;
        ImageView mSelect;    //multi-select check mark icon
        RelativeLayout relativeBg;
    }


    /**
     * A nested class to handle displaying a custom view in the ListView that
     * is used in the Main activity. If any icons are to be added, they must
     * be implemented in the getView method. This class is instantiated once in Main
     * and has no reason to be instantiated again.
     *
     * @author Joe Berria
     */
    public class TableRow extends ArrayAdapter<String> {
        private final int KB = 1024;
        private final int MG = KB * KB;
        private final int GB = MG * KB;
        private String display_size;
        private ArrayList<Integer> positions;
        private LinearLayout hidden_layout;

        public TableRow() {
            super(mContext, R.layout.tablerow, arrayFilename);
        }

        public void addMultiPosition(int index, String path) {
            if (positions == null)
                positions = new ArrayList<Integer>();

            if (mMultiSelectData == null) {
                positions.add(index);
                add_multiSelect_file(path);

            } else if (mMultiSelectData.contains(path)) {
                if (positions.contains(index))
                    positions.remove(new Integer(index));

                mMultiSelectData.remove(path);

            } else {
                positions.add(index);
                add_multiSelect_file(path);
            }

            notifyDataSetChanged();
        }

        /**
         * This will turn off multi-select and hide the multi-select buttons at the
         * bottom of the view.
         *
         * @param clearData if this is true any files/folders the user selected for multi-select
         *                  will be cleared. If false, the data will be kept for later use. Note:
         *                  multi-select copy and move will usually be the only one to pass false,
         *                  so we can later paste it to another folder.
         */
        public void killMultiSelect(boolean clearData) {
            hidden_layout = (LinearLayout) ((Activity) mContext).findViewById(R.id.hidden_buttons);
            hidden_layout.setVisibility(LinearLayout.GONE);
            multi_select_flag = false;

            if (positions != null && !positions.isEmpty())
                positions.clear();

            if (clearData)
                if (mMultiSelectData != null && !mMultiSelectData.isEmpty())
                    mMultiSelectData.clear();

            notifyDataSetChanged();
        }

        public String getFilePermissions(File file) {
            String per = "-";

            if (file.isDirectory())
                per += "d";
            if (file.canRead())
                per += "r";
            if (file.canWrite())
                per += "w";

            return "";
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder mViewHolder;
            int num_items = 0;
            String temp = mFileMang.getCurrentDir();
            File file = new File(temp + "/" + mDataSource.get(position).getFileName());
            String[] list = file.list();

            if (list != null)
                num_items = list.length;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.tablerow, parent, false);

                mViewHolder = new ViewHolder();
                mViewHolder.topView = (TextView) convertView.findViewById(R.id.top_view);
                mViewHolder.bottomView = (TextView) convertView.findViewById(R.id.bottom_view);
                mViewHolder.icon = (ImageView) convertView.findViewById(R.id.row_image);
                mViewHolder.mSelect = (ImageView) convertView.findViewById(R.id.multiselect_icon);
                mViewHolder.relativeBg = (RelativeLayout) convertView.findViewById(R.id.relativeBg);

                convertView.setTag(mViewHolder);

            } else {
                mViewHolder = (ViewHolder) convertView.getTag();
            }

            if (positions != null && positions.contains(position))
                mViewHolder.mSelect.setVisibility(ImageView.VISIBLE);
            else
                mViewHolder.mSelect.setVisibility(ImageView.GONE);

            mViewHolder.topView.setTextColor(mColor);
            mViewHolder.bottomView.setTextColor(mColor);

            if (mThumbnail == null)
                mThumbnail = new ThumbnailCreator(52, 52);

            if (file != null && file.isFile()) {
                String ext = file.toString();
                String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);

    			/* This series of else if statements will determine which 
    			 * icon is displayed 
    			 */
                if (sub_ext.equalsIgnoreCase("pdf")) {
                    mViewHolder.icon.setImageResource(R.drawable.pdf);

                } else if (sub_ext.equalsIgnoreCase("mp3") ||
                        sub_ext.equalsIgnoreCase("wma") ||
                        sub_ext.equalsIgnoreCase("m4a") ||
                        sub_ext.equalsIgnoreCase("m4p")) {

                    mViewHolder.icon.setImageResource(R.drawable.music);

                } else if (sub_ext.equalsIgnoreCase("png") ||
                        sub_ext.equalsIgnoreCase("jpg") ||
                        sub_ext.equalsIgnoreCase("jpeg") ||
                        sub_ext.equalsIgnoreCase("gif") ||
                        sub_ext.equalsIgnoreCase("tiff")) {

                    if (thumbnail_flag && file.length() != 0) {
                        Bitmap thumb = mThumbnail.isBitmapCached(file.getPath());

                        if (thumb == null) {
                            final Handler handle = new Handler(new Handler.Callback() {
                                public boolean handleMessage(Message msg) {
                                    notifyDataSetChanged();

                                    return true;
                                }
                            });

                            mThumbnail.createNewThumbnail(mDataSource, mFileMang.getCurrentDir(), handle);

                            if (!mThumbnail.isAlive())
                                mThumbnail.start();

                        } else {
                            mViewHolder.icon.setImageBitmap(thumb);
                        }

                    } else {
                        mViewHolder.icon.setImageResource(R.drawable.image);
                    }

                } else if (sub_ext.equalsIgnoreCase("zip") ||
                        sub_ext.equalsIgnoreCase("gzip") ||
                        sub_ext.equalsIgnoreCase("gz")) {

                    mViewHolder.icon.setImageResource(R.drawable.zip);

                } else if (sub_ext.equalsIgnoreCase("m4v") ||
                        sub_ext.equalsIgnoreCase("wmv") ||
                        sub_ext.equalsIgnoreCase("3gp") ||
                        sub_ext.equalsIgnoreCase("mp4")) {

                    mViewHolder.icon.setImageResource(R.drawable.movies);

                } else if (sub_ext.equalsIgnoreCase("doc") ||
                        sub_ext.equalsIgnoreCase("docx")) {

                    mViewHolder.icon.setImageResource(R.drawable.word);

                } else if (sub_ext.equalsIgnoreCase("xls") ||
                        sub_ext.equalsIgnoreCase("xlsx")) {

                    mViewHolder.icon.setImageResource(R.drawable.excel);

                } else if (sub_ext.equalsIgnoreCase("ppt") ||
                        sub_ext.equalsIgnoreCase("pptx")) {

                    mViewHolder.icon.setImageResource(R.drawable.ppt);

                } else if (sub_ext.equalsIgnoreCase("html")) {
                    mViewHolder.icon.setImageResource(R.drawable.html32);

                } else if (sub_ext.equalsIgnoreCase("xml")) {
                    mViewHolder.icon.setImageResource(R.drawable.xml32);

                } else if (sub_ext.equalsIgnoreCase("conf")) {
                    mViewHolder.icon.setImageResource(R.drawable.config32);

                } else if (sub_ext.equalsIgnoreCase("apk")) {
                    mViewHolder.icon.setImageResource(R.drawable.appicon);

                } else if (sub_ext.equalsIgnoreCase("jar")) {
                    mViewHolder.icon.setImageResource(R.drawable.jar32);

                } else {
                    mViewHolder.icon.setImageResource(R.drawable.text);
                }

            } else if (file != null && file.isDirectory()) {
                if (file.canRead() && file.list().length > 0)
                    mViewHolder.icon.setImageResource(R.drawable.folder_full);
                else
                    mViewHolder.icon.setImageResource(R.drawable.folder);
            }

            String permission = getFilePermissions(file);

            if (file.isFile()) {
                double size = file.length();
                if (size > GB)
                    display_size = String.format("%.2f Gb ", (double) size / GB);
                else if (size < GB && size > MG)
                    display_size = String.format("%.2f Mb ", (double) size / MG);
                else if (size < MG && size > KB)
                    display_size = String.format("%.2f Kb ", (double) size / KB);
                else
                    display_size = String.format("%.2f bytes ", (double) size);

                if (file.isHidden())
                    mViewHolder.bottomView.setText("(hidden) | " + display_size + " | " + permission);
                else
                    mViewHolder.bottomView.setText(display_size + " | " + permission);

            } else {
                if (file.isHidden())
                    mViewHolder.bottomView.setText("(hidden) | " + num_items + " items | " + permission);
                else
                    mViewHolder.bottomView.setText(num_items + " items | " + permission);
            }

            mViewHolder.topView.setText(file.getName());

            if (mDataSource.get(position).isHighlighted())
                mViewHolder.relativeBg.setBackgroundColor(mContext.getResources().getColor(R.color.darkBlue));
            else
                mViewHolder.relativeBg.setBackgroundColor(mContext.getResources().getColor(R.color.black));

            return convertView;
        }

        private void add_multiSelect_file(String src) {
            if (mMultiSelectData == null)
                mMultiSelectData = new ArrayList<FileEntity>();

            mMultiSelectData.add(new FileEntity(src, false));
        }
    }
}
