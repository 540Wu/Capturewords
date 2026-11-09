package com.shilin.cuoci

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var navView: NavigationView

    private var currentTag = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WordStore.init(this)
        EssayStore.init(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        bottomNav = findViewById(R.id.bottomNav)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.app_name, R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_skin -> showSkinPicker()
                R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.menu_debug -> startActivity(Intent(this, DebugActivity::class.java))
                R.id.menu_about -> showAbout()
            }
            drawerLayout.closeDrawers()
            true
        }

        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showTab("home", "错词捕手")
                R.id.nav_study -> showTab("study", "写作干货")
                R.id.nav_review -> showTab("quiz", "复习")
                R.id.nav_history -> showTab("history", "历史记录")
                R.id.nav_profile -> showTab("profile", "我的")
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
            showTab("home", "错词捕手")
        } else {
            val visible = supportFragmentManager.fragments.firstOrNull { !it.isHidden }
            if (visible == null) {
                bottomNav.selectedItemId = R.id.nav_home
                showTab("home", "错词捕手")
            } else {
                currentTag = visible.tag ?: "home"
                supportActionBar?.title = when (currentTag) {
                    "study" -> "写作干货"
                    "history" -> "历史记录"
                    "profile" -> "我的"
                    "quiz" -> "复习"
                    else -> "错词捕手"
                }
            }
        }
    }

    private fun showTab(tag: String, title: String) {
        if (tag == currentTag && supportFragmentManager.findFragmentByTag(tag) != null) {
            supportActionBar?.title = title
            return
        }
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()

        for (f in fm.fragments) {
            val t = f.tag
            if (t != null && t != tag) ft.hide(f)
        }

        val existing = fm.findFragmentByTag(tag)
        if (existing == null) {
            val f: Fragment = when (tag) {
                "study" -> StudyFragment()
                "history" -> HistoryFragment()
                "profile" -> ProfileFragment()
                "quiz" -> QuizFragment()
                else -> HomeFragment()
            }
            ft.add(R.id.fragmentContainer, f, tag)
        } else {
            ft.show(existing)
        }

        ft.commit()
        currentTag = tag
        supportActionBar?.title = title
    }

    private fun showSkinPicker() {
        val names = SkinManager.names()
        val current = Prefs.skin(this)
        AlertDialog.Builder(this)
            .setTitle("外观风格")
            .setSingleChoiceItems(names, current) { d, which ->
                Prefs.setSkin(this, which)
                d.dismiss()
                recreate()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("关于错词捕手")
            .setMessage(
                "v1.0\n\n" +
                    "它只读取「不背单词」的界面内容，用来记录你答错或标记为不认识的单词，" +
                    "再按分组生成包含这些词的小短文，帮助你在语境里记住它们。\n\n" +
                    "所有数据只保存在本机，不会上传到任何服务器（生成短文时会把你当天记错的单词发给 DeepSeek）。"
            )
            .setPositiveButton("知道了", null)
            .show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(navView)) {
            drawerLayout.closeDrawers()
            return
        }
        if (currentTag != "home") {
            bottomNav.selectedItemId = R.id.nav_home
            showTab("home", "错词捕手")
            return
        }
        super.onBackPressed()
    }
}
