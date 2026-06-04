package com.example.myapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.ui.search.SearchDialogFragment;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    // 用来设置ToolBar
    private AppBarConfiguration appBarConfiguration;

    // 避免手写findViewById。
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // 找到承载 Fragment 页面的 NavHostFragment。
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);


        // NavController 负责根据 nav_graph.xml 做页面跳转。
        NavController navController = navHostFragment.getNavController();

        // 根据导航图创建顶部栏配置。
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();

        // 让 Toolbar 自动跟随导航状态更新标题和返回按钮。
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载右上角菜单。menu_main.xml 里目前是搜索按钮。
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // 返回 true 表示菜单要显示。
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 获取用户点击的菜单项 id。
        int id = item.getItemId();

        // 点击搜索按钮时，弹出对话式搜索窗口。
        if (id == R.id.action_search) {
            new SearchDialogFragment().show(getSupportFragmentManager(), "search");
            return true;
        }

        // 其他菜单事件交给系统默认逻辑处理。
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // 用户点击 Toolbar 左上角返回按钮时，会走到这里。
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        // 重新拿到导航控制器，让 Navigation 处理返回。
        NavController navController = navHostFragment.getNavController();

        // 优先让 Navigation 返回上一页；如果不能处理，再交给系统默认返回。
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
