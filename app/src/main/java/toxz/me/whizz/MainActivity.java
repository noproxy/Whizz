package toxz.me.whizz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.Picasso;
import com.viewpagerindicator.UnderlinePageIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.timroes.android.listview.EnhancedListView;
import toxz.me.whizz.application.MyApplication;
import toxz.me.whizz.application.ShortCutCreator;
import toxz.me.whizz.data.DataChangedListener;
import toxz.me.whizz.data.DatabaseHelper;
import toxz.me.whizz.data.Note;
import toxz.me.whizz.monitor.NoticeMonitorService;

import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;


public class MainActivity extends AppCompatActivity implements DataChangedListener, ViewPager.OnPageChangeListener,
        ActionMode.Callback, View.OnClickListener {

    public static final int REQUEST_CODE_IMAGE_PICK = 1;
    public static final int SCROLL_FLAG_FROM_ONE_TO_TWO = 0;
    public static final int SCROLL_FLAG_FROM_TWO_TO_ONE = 1;

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm 创建");
    private LayoutInflater mInflater;
    private Spinner mDaySpinner, mTimeSpinner;
    private ViewPager mViewPager;
    private EditText mNewNoteEditText;
    private View mNoNote;
    private EnhancedListView mMainList;
    private View mNewItemPager;
    private ImageView mNoticeSetButton;
    private TextView mCreatedTimeText;
    private String mTempText = "";
    private LinearLayout mImageContainer;
    private ArrayList<Note> mCheckedItem = new ArrayList<>();
    private ArrayList<String> mImagePaths = new ArrayList<>();
    /**
     * Note mCurrent is used to contain temp data, null when create a new note.
     */
    private Note mCurrentNote = null;
    private ActionMode mActionMode = null;
    private int mImageContainerWidth = 0, mImageHeight = 0, mCurrentPage = 0;
    private boolean isEdit = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * Called when the activity is first created.
     * if not first open ,display XMB.Otherwise, display welcome activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if account is not initialized ,launch welcome activity.
        if (MyApplication.AccountInfo.getAccount() == null) {
            Log.i("vital", "account == null ");
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            this.finish();
        } else {
            init();
            int launchFrom = getIntent().getIntExtra(ShortCutCreator.EXTRA_LAUNCH_METHOD, -1);
            switch (launchFrom) {
                case ShortCutCreator.NEW_TEXT_NOTE_SHORTCUT:
                    if (mViewPager != null) {
                        mViewPager.setCurrentItem(1);
                        isEdit = true;
                    }
                    break;
                default:
                    break;
            }

        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onRestart() {
        int launchFrom = getIntent().getIntExtra(ShortCutCreator.EXTRA_LAUNCH_METHOD, -1);
        switch (launchFrom) {
            case ShortCutCreator.NEW_TEXT_NOTE_SHORTCUT:
                if (mViewPager != null) {
                    mViewPager.setCurrentItem(1);
                    isEdit = true;
                }
                break;
            default:
                break;
        }
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.i("onResume()", "isEdit is " + isEdit + " mCurrentPage is " + mCurrentPage);
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i("onResume()", "isEdit is " + isEdit + " mCurrentPage is " + mCurrentPage);
        super.onPause();
    }

    /**
     * init and display.
     */
    private void init() {
        mInflater = getLayoutInflater();

        setContentView(R.layout.main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

         /* initViewPager */
        List<View> mPagers = new ArrayList<>();
        mPagers.add(initMainPager());
        mPagers.add(initAddNotePager());
        mViewPager = (ViewPager) findViewById(R.id.mainViewpager);
        mViewPager.setAdapter(new MyPagerAdapter(mPagers));


        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();


        /* show indicator on action bar */
        View customActionBarView = mInflater.inflate(R.layout.custom_action_bar, null);
        assert customActionBarView != null;
        UnderlinePageIndicator pagerIndicator = (UnderlinePageIndicator) customActionBarView.findViewById(R.id.pageIndicator);
        pagerIndicator.setSelectedColor(getResources().getColor(R.color.item_background));
        pagerIndicator.setFades(false);
        pagerIndicator.setViewPager(mViewPager);
        pagerIndicator.setOnPageChangeListener(this);


        assert actionBar != null;
        actionBar.setIcon(R.drawable.logo_whizzdo);
        actionBar.setTitle(null);
        actionBar.setCustomView(customActionBarView);
        actionBar.setDisplayShowCustomEnabled(true);


        DatabaseHelper.getDatabaseHelper(getApplicationContext()).setDatabaseChangedListener(this);
    }

    private AdapterView.OnItemClickListener mListItemClickListener;
    private AdapterView.OnItemLongClickListener mListItemLongClickListener;

    /* call by init() */
    private View initMainPager() {
        @SuppressLint("InflateParams")
        View itemListPager = mInflater.inflate(R.layout.item_list_pager, null);
        assert itemListPager != null;

        mMainList = (EnhancedListView) itemListPager.findViewById(R.id.main_list);
        mNoNote = itemListPager.findViewById(R.id.no_note);
        mMainList.setDivider(getResources().getDrawable(R.drawable.line_divider));

        mListItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    mCurrentNote = (Note) view.getTag();
                    mViewPager.setCurrentItem(1, true);
                    onScrollPage(SCROLL_FLAG_FROM_ONE_TO_TWO);
                } else {
                    final Note note = (Note) view.getTag();
                    if (mCheckedItem.contains(note)) {
                        view.setBackgroundResource(R.color.main_background);
                        Log.i("onItemClick()", "item " + position + " is removed from ArrayList");
                        mCheckedItem.remove(note);

                    } else {
                        view.setBackgroundResource(R.color.selected_background);
                        Log.i("onItemClick()", "item " + position + " is added to ArrayList");
                        mCheckedItem.add((Note) view.getTag());
                    }
                }

            }
        };

        mMainList.setOnItemClickListener(mListItemClickListener);
        mListItemLongClickListener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) return true;
                startActionMode(MainActivity.this);
                view.setBackgroundResource(R.color.selected_background);
                Log.i("onItemLongClick()", "start actionMode, item " + position + " is added to ArrayList");
                mCheckedItem.add((Note) view.getTag());
                return true;
            }
        };
        mMainList.setOnItemLongClickListener(mListItemLongClickListener);
        mMainList.setSwipeDirection(EnhancedListView.SwipeDirection.END).setDismissCallback(new EnhancedListView.OnDismissCallback() {
            @Override
            public EnhancedListView.Undoable onDismiss(EnhancedListView listView, int position) {
                if (position < listView.getAdapter().getCount()) {
                    final Note note = (Note) listView.getAdapter().getItem(position);
                    note.delete(DatabaseHelper.getDatabaseHelper(MainActivity.this));
                    return new EnhancedListView.Undoable() {
                        @Override
                        public void undo() {
                            note.resetID().commit(DatabaseHelper.getDatabaseHelper(MainActivity.this));
                        }
                    };
                } else {
                    Log.e("onDismiss()", "return null");
                    return null;
                }
            }
        }).setRequireTouchBeforeDismiss(false).setUndoHideDelay(3000).setUndoStyle(EnhancedListView.UndoStyle.MULTILEVEL_POPUP).enableSwipeToDismiss();
        refreshList();
        return itemListPager;
    }

    /**
     * refresh the list, data will refreshed.
     */
    private void refreshList() {
        //every item show up to 80 Words.Set by xml.
        Log.i("refreshList()", "!!!");
        if (!DatabaseHelper.getDatabaseHelper(getApplicationContext()).hasNotes())
            mNoNote.setVisibility(View.VISIBLE);
            //if contain no items，not display list, and the notice for no item will displayed.
        else
            mNoNote.setVisibility(View.GONE);
        mMainList.setAdapter(new MyListAdapter(this, mInflater));
    }


    /* call by init() */
    private View initAddNotePager() {
        mNewItemPager = mInflater.inflate(R.layout.new_item_pager, null);
        assert mNewItemPager != null;
        mNewNoteEditText = (EditText) mNewItemPager.findViewById(R.id.et_input_note);
        mImageContainer = (LinearLayout) mNewItemPager.findViewById(R.id.image_container);
        mNoticeSetButton = (ImageButton) mNewItemPager.findViewById(R.id.bottom_bar_notice);
        mCreatedTimeText = (TextView) mNewItemPager.findViewById(R.id.tv_create_time);

        mDaySpinner = (Spinner) mNewItemPager.findViewById(R.id.day_spinner);
        mTimeSpinner = (Spinner) mNewItemPager.findViewById(R.id.time_spinner);
        mDaySpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                        new String[]{"今天", "明天", "选择日期..."}));
        mTimeSpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                        new String[]{"早上08：00", "下午13：00", "晚上17：00", "夜间20：00", "选择时刻..."}));

        refreshNotePager();
        return mNewItemPager;
    }

    //TODO BUG 添加图片之后，textView文字消失
    private void refreshNotePager() {
        mImageContainer.removeAllViews();
        /* what need to be refresh: image list , alarm color , time pick */
        if (mCurrentNote == null) {
            mNewNoteEditText.setText("");
            mCreatedTimeText.setText(format.format(System.currentTimeMillis()));
            mNoticeSetButton.setImageResource(R.drawable.bottom_bar_notice_grey);
            mDaySpinner.setVisibility(View.GONE);
            mTimeSpinner.setVisibility(View.GONE);
        } else {
            if (mCurrentNote.getImagesUris().size() > 0) {
                LinearLayout linearLayout;
                Log.i("refreshNotePager()", "image path sizes: " + mCurrentNote.getImagesUris().size());
                for (int i = 0; i < mCurrentNote.getImagesUris().size(); i += 2) {
                    Log.i("load images", "Path is " + mCurrentNote.getImagesUris().get(i));
                    Log.i("refreshNotePager()", "add pic  i = " + i);
                    if (i == mCurrentNote.getImagesUris().size() - 1) {
                        Log.i("refreshNotePager()", "kind 1 was created ! ");
                        linearLayout = (LinearLayout) mInflater.inflate(R.layout.image_layout, null);
                        ImageView imageView = (ImageView) linearLayout.findViewById(R.id.image);
                        imageView.setTag(mCurrentNote.getImagesUris().get(i));
//                        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentNote.getImagesUris().get(i));
//                        imageView.setImageBitmap(bitmap);
                        Picasso.with(MainActivity.this).load(mCurrentNote.getImagesUris().get(i)).error(R.drawable.ic_launcher).into(imageView);
                    } else {
                        Log.i("refreshNotePager()", "kind 2 was created ! i=" + i);
                        linearLayout = (LinearLayout) mInflater.inflate(R.layout.two_image_layout, null);
                        ImageView imageView1 = (ImageView) linearLayout.findViewById(R.id.image1);
                        imageView1.setTag(mCurrentNote.getImagesUris().get(i));

//                        Bitmap bitmap1 = BitmapFactory.decodeFile(mCurrentNote.getImagesUris().get(i));
//                        imageView1.setImageBitmap(bitmap1);
                        Picasso.with(MainActivity.this)
                                .load(mCurrentNote.getImagesUris().get(i))
                                .error(R.drawable.ic_launcher)
                                .into(imageView1);

                        Log.i("refreshNotePager()", "kind 2 was created ! i=" + (i + 1));
                        ImageView imageView2 = (ImageView) linearLayout.findViewById(R.id.image2);
                        imageView2.setTag(mCurrentNote.getImagesUris().get(i));

//                        Bitmap bitmap2 = BitmapFactory.decodeFile(mCurrentNote.getImagesUris().get(i + 1));
//                        imageView2.setImageBitmap(bitmap2);
                        Picasso.with(MainActivity.this)
                                .load(mCurrentNote.getImagesUris().get(i + 1))
                                .error(R.drawable.ic_launcher)
                                .into(imageView2);
                    }
                    mImageContainer.addView(linearLayout);
                }
            }
            mNewNoteEditText.setText(mCurrentNote.getContent());
            mCreatedTimeText.setText(format.format(mCurrentNote.getCreatedTime()));
            mDaySpinner.setVisibility(View.VISIBLE);
            mTimeSpinner.setVisibility(View.VISIBLE);
            switch (mCurrentNote.getImportance()) {
                case Note.NO_IMPORTANCE:
                    mDaySpinner.setVisibility(View.GONE);
                    mTimeSpinner.setVisibility(View.GONE);
                    mNoticeSetButton.setImageResource(R.drawable.bottom_bar_notice_grey);
                    break;
                case Note.LOW_IMPORTANCE:
                    mDaySpinner.setSelected(false);
                    mTimeSpinner.setEnabled(true);
                    mNoticeSetButton.setImageResource(R.drawable.bottom_bar_notice_grey);
                    break;
                case Note.NORMAL_IMPORTANCE:
                    mDaySpinner.setSelection(1);
                    mNoticeSetButton.setImageResource(R.drawable.bottom_bar_notice_blue);
                    break;
                case Note.HIGH_IMPORTANCE:
                    mDaySpinner.setSelection(0);
                    mNoticeSetButton.setImageResource(R.drawable.bottom_bar_notice_red);
                    break;
                //TODO 设定TimeSpinner的显示（显示特殊时刻和日期/读取时间）
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentPage == 1) {
            isEdit = false;
            mCurrentPage = 0;
            onScrollPage(SCROLL_FLAG_FROM_TWO_TO_ONE);
        }
        sendBroadcast(new Intent(NoticeMonitorService.MY_ACTION_MAIN_ACTIVITY_EXIT));
        super.onBackPressed();
    }

    /**
     * handle the data changed event
     */
    @Override
    public void notifyDataChanged() {
        refreshList();
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mCurrentPage = position;
//
//        Log.i("vital", "Viewpager onPageScrolled ! position is :" + position + ", position offset is " + positionOffset + ", pixels is " + positionOffsetPixels);
//        Log.i("vital", "isEdit is  " + isEdit);
//TODO 仿x-plore的滑动


        /*当偏移小于一定值时绘制指示图标，当大于这一值时，触发onScrollPage*/
    }

    @Override
    public void onPageSelected(int position) {
//        Log.i("vital", "onPageSelected : position is " + position);
//        Log.i("vital", "isEdit is  " + isEdit);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // 如果本来是在编辑状态，则不变；如果不在编辑状态，则判断是否进入编辑状态
        boolean sign = isEdit;
        isEdit = isEdit || (state == 0 && mCurrentPage == 1);
        if (!sign && isEdit) {
            onScrollPage(SCROLL_FLAG_FROM_ONE_TO_TWO);
        }

        // 如果不是在编辑状态，则不变；如果是在编辑状态，判断是否需要保存，若需要，则保存，然后退出编辑状态

        if (isEdit && (state == 0 && mCurrentPage == 0)) {
            onScrollPage(SCROLL_FLAG_FROM_TWO_TO_ONE);
            isEdit = false;
        }
//        Log.i("onPageScrollStateChanged()", "isEdit  is " + isEdit + " ,  mCurrentPage is " + mCurrentPage);
//                Log.i("vital", "isEdit is  " + isEdit);

    }


    private void onScrollPage(int flag) {
        if (mActionMode != null) {
            mActionMode.finish();
        }
        switch (flag) {
            case SCROLL_FLAG_FROM_ONE_TO_TWO:
                Log.i("onScrollPage()", "SCROLL_FLAG_FROM_ONE_TO_TWO");
                refreshNotePager();
                if (mNewNoteEditText != null) {
                    mNewNoteEditText.requestFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(mNewNoteEditText, 0);
                }
                break;
            case SCROLL_FLAG_FROM_TWO_TO_ONE:
                Log.i("onScrollPage()", "SCROLL_FLAG_FROM_TWO_TO_ONE");
                mNewNoteEditText.clearFocus();
                mMainList.requestFocus();


                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                try {
                    inputMethodManager.hideSoftInputFromWindow(mMainList.getWindowToken(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                String note = String.valueOf(mNewNoteEditText.getText());
                int importance = mDaySpinner.getSelectedItemPosition();
                if (mCurrentNote == null && (note == null || note.trim().length() == 0))
                    Toast.makeText(MainActivity.this, "空白记事，已舍弃", Toast.LENGTH_SHORT).show();
                else {
                    if (mCurrentNote == null) {
                        mCurrentNote = new Note.Builder()
                                .setNotice(true)
                                .setContent(note)
                                .setCreatedTime(System.currentTimeMillis())
                                .setImportance(importance).create();
                    } else {
                        if (mCurrentNote.getID() == -1) {
                            mCurrentNote.setCreatedTime(System.currentTimeMillis());
                        }
                        mCurrentNote.setContent(note);
                        mCurrentNote.setNotice(true);
                        mCurrentNote.setImportance(importance);
                    }
                    mCurrentNote.commit(DatabaseHelper.getDatabaseHelper(MainActivity.this));
                    Toast.makeText(MainActivity.this, "已保存", Toast.LENGTH_SHORT).show();
                    mCurrentNote = null;
                    refreshList();
                }

                mImagePaths.clear();
                refreshNotePager();
                break;
            default:
        }

    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.action_mode, menu);
        mActionMode = mode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionMode_finish:
                Note note;
                for (int i = 0; i < mCheckedItem.size(); i++) {
                    note = mCheckedItem.get(i);
                    note.setFinished(true);
                    Log.i("onActionItemClicked()", "note " + note.getContent() + " , id " + note.getID() + " is finished.");
                    note.commit(DatabaseHelper.getDatabaseHelper(MainActivity.this));
                }
                mode.finish();
                break;
            case R.id.actionMode_delete:
                Note note2;
                for (int i = 0; i < mCheckedItem.size(); i++) {
                    note2 = mCheckedItem.get(i);
                    note2.delete(DatabaseHelper.getDatabaseHelper(MainActivity.this));
                }
                mode.finish();
                break;
            case R.id.actionMode_select_all:
                break;
        }
        return false;
    }


    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (mCheckedItem.size() != 0) refreshList();
        mCheckedItem.clear();
        mActionMode = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_bar_audio:
                //TODO audio
                break;
            case R.id.bottom_bar_image:
                mTempText = mNewNoteEditText.getText().toString();
                if (mTempText == null)
                    mTempText = "";
                startActivityForResult(new Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI), REQUEST_CODE_IMAGE_PICK);
                break;
            case R.id.bottom_bar_notice:
                //TODO set notice time  and save it into Note instance.
                if (mDaySpinner.getVisibility() == View.VISIBLE) {
                    if (mCurrentNote != null)
                        mCurrentNote.setImportance(Note.NO_IMPORTANCE);
                    mDaySpinner.setVisibility(View.GONE);
                    mTimeSpinner.setVisibility(View.GONE);
                    //TODO 去掉时间
                } else {
                    mDaySpinner.setVisibility(View.VISIBLE);
                    mTimeSpinner.setVisibility(View.VISIBLE);
                }
                //TODO BUG: 选择时间以后，界面上的闹钟图标没有变颜色
                //TODO  BUG： 上次选择时间以后，默认的sinner选项残留
                break;
            case R.id.action_bar_item_add_note:
                mViewPager.setCurrentItem(1, true);
                break;
            case R.id.action_bar_item_list:
                mViewPager.setCurrentItem(0, true);
                break;
            case R.id.no_note:
                mViewPager.setCurrentItem(1);
                break;
            case R.id.image:
            case R.id.image1:
            case R.id.image2:
                Uri uri = (Uri) v.getTag();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_IMAGE_PICK:
                if (resultCode == RESULT_OK && data != null) {
                    Uri image = data.getData();
                    onReceiveImage(image);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onReceiveImage(Uri uri) {
        if (uri != null) {
            if (mCurrentNote == null) {
                mCurrentNote = new Note();
            }
            mCurrentNote.getImagesUris().add(uri);
            mCurrentNote.setContent(mTempText);
            refreshNotePager();
            mTempText = "";
        }
    }

}
