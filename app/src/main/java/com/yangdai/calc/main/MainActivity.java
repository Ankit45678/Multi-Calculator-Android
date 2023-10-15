package com.yangdai.calc.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;
import com.yangdai.calc.R;
import com.yangdai.calc.floating.FloatingWindow;
import com.yangdai.calc.other.AboutActivity;
import com.yangdai.calc.other.SettingsActivity;
import com.yangdai.calc.toolbox.ToolBoxFragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 30415
 */
public class MainActivity extends AppCompatActivity {
    private Menu menu;
    private ViewPager2 viewPager;
    private int currentPosition = 0;
    private ImageView pageIcon;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.menu = menu;
        if (currentPosition == 1) {
            menu.findItem(R.id.historys).setVisible(false);
            menu.findItem(R.id.view_layout).setVisible(true);
        } else {
            menu.findItem(R.id.historys).setVisible(true);
            menu.findItem(R.id.view_layout).setVisible(false);
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isGrid = settings.getBoolean("GridLayout", true);
        if (isGrid) {
            menu.findItem(R.id.view_layout).setIcon(getDrawable(R.drawable.grid_on));
        } else {
            menu.findItem(R.id.view_layout).setIcon(getDrawable(R.drawable.table_rows));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.historys) {
            ViewPager2 viewPager2;
            try {
                viewPager2 = findViewById(R.id.view_pager);
                if (viewPager2.getCurrentItem() == 0) {
                    viewPager2.setCurrentItem(1);
                } else {
                    viewPager2.setCurrentItem(0);
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        } else if (item.getItemId() == R.id.resize) {
            if (isMyServiceRunning()) {
                // 调用 FloatingWindow 中 onDestroy()方法
                stopService(new Intent(MainActivity.this, FloatingWindow.class));
            }
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayDisplayPermission();
            } else {
                startService(new Intent(MainActivity.this, FloatingWindow.class));
                finish();
            }
        } else if (item.getItemId() == R.id.setting) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (item.getItemId() == R.id.about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (item.getItemId() == R.id.view_layout) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isGrid = settings.getBoolean("GridLayout", true);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("GridLayout", !isGrid);
            editor.apply();
            if (!isGrid) {
                item.setIcon(getDrawable(R.drawable.grid_on));
            } else {
                item.setIcon(getDrawable(R.drawable.table_rows));
            }
            Bundle result = new Bundle();
            result.putBoolean("GridLayout", !isGrid);
            getSupportFragmentManager().setFragmentResult("ChangeLayout", result);
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestOverlayDisplayPermission() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setTitle(getString(R.string.Screen_Overlay_Permission_Needed))
                .setMessage(getString(R.string.Permission_Dialog_Messege))
                .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    // 处理权限未授予的情况
                    Toast.makeText(MainActivity.this, getString(R.string.permission), Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton(getString(R.string.Open_Settings), (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    overlayPermissionLauncher.launch(intent);
                }).show();
    }

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // 处理权限已授予的情况
                    startService(new Intent(MainActivity.this, FloatingWindow.class));
                    finish();
                }
            }
    );

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // 需要使用循环来获取当前正在运行的服务的信息。
        // 使用 ActivityManager.RunningServiceInfo 来检索特定服务的信息，这里是指当前的服务。
        // getRunningServices() 方法返回一个当前正在运行的服务列表，
        // Integer.MAX_VALUE 是 2147483647，所以最多可以返回这么多个服务。
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

            // 如果找到了这个服务正在运行，就返回 true，否则返回 false。
            if (FloatingWindow.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(SurfaceColors.SURFACE_0.getColor(this));
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupSharedPreferences();
        setupViewPager();
        pageIcon.setOnClickListener(v -> {
            if (currentPosition == 0) {
                currentPosition = 1;
                viewPager.setCurrentItem(1, true);
            } else {
                currentPosition = 0;
                viewPager.setCurrentItem(0, true);
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(SurfaceColors.SURFACE_0.getColor(this)));
        getSupportActionBar().setElevation(0f);
        pageIcon = findViewById(R.id.view_pager_icon);
    }

    private void setupSharedPreferences() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean("screen", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setupViewPager() {
        WormDotsIndicator dotsIndicator = findViewById(R.id.dotsIndicator);
        viewPager = findViewById(R.id.view_pager_main);
        if (viewPager != null) {
            reduceDragSensitivity();
            List<Fragment> fragments = new ArrayList<>();
            fragments.add(MainFragment.newInstance());
            fragments.add(ToolBoxFragment.newInstance());
            MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), getLifecycle(), fragments);
            viewPager.setAdapter(pagerAdapter);
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @SuppressLint("UseCompatLoadingForDrawables")
                @Override
                public void onPageSelected(int position) {
                    currentPosition = position;
                    if (currentPosition == 0) {
                        if (menu != null) {
                            menu.findItem(R.id.historys).setVisible(true);
                            menu.findItem(R.id.view_layout).setVisible(false);
                        }
                        pageIcon.setImageDrawable(getDrawable(R.drawable.calculate_icon));
                    } else {
                        if (menu != null) {
                            menu.findItem(R.id.historys).setVisible(false);
                            menu.findItem(R.id.view_layout).setVisible(true);
                        }
                        pageIcon.setImageDrawable(getDrawable(R.drawable.grid_view_more));
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        if (currentPosition == 0) {
                            if (menu != null) {
                                menu.findItem(R.id.historys).setVisible(true);
                                menu.findItem(R.id.view_layout).setVisible(false);
                            }
                        } else {
                            if (menu != null) {
                                menu.findItem(R.id.historys).setVisible(false);
                                menu.findItem(R.id.view_layout).setVisible(true);
                            }
                        }
                    }
                }
            });
            dotsIndicator.attachTo(viewPager);
        }
    }

    private void reduceDragSensitivity() {
        try {
            Field ff = ViewPager2.class.getDeclaredField("mRecyclerView");
            ff.setAccessible(true);
            RecyclerView recyclerView = (RecyclerView) ff.get(viewPager);
            Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
            touchSlopField.setAccessible(true);
            int touchSlop = (int) touchSlopField.get(recyclerView);
            touchSlopField.set(recyclerView, touchSlop * 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}