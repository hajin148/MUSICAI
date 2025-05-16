package com.dasoni.musicai

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase


class MainActivity : AppCompatActivity() {
    var username = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMainPage()
    }
    fun setMainPage() {
        setContentView(R.layout.activity_main)
        var register_btn : Button = findViewById(R.id.Register_btn)
        register_btn.setOnClickListener{
            setRegisterPage()
        }
        var login_btn : Button = findViewById(R.id.Login_btn)
        login_btn.setOnClickListener {
            setLoginPage()
        }

        var username_display : TextView = findViewById(R.id.username_display)

        if (username != "") {
            username_display.text = username
        }
    }

    fun setLoginPage() {
        setContentView(R.layout.login)

        var email: EditText = findViewById(R.id.login_email)
        var password: EditText = findViewById(R.id.login_password)
        var login_btn: Button = findViewById(R.id.login_btn)

        login_btn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("MUSICAI/users")

            ref.get().addOnSuccessListener { snapshot ->
                var foundUser = false
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null && user.email == emailText && user.password == passwordText) {
                        foundUser = true
                        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        username = user.username
                        setMainPage()
                        break
                    }
                }

                if (!foundUser) {
                    Toast.makeText(this, "이메일 또는 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener {
                Toast.makeText(this, "로그인 오류: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun setRegisterPage() {
        setContentView(R.layout.registration)

        var username: EditText = findViewById(R.id.username)
        var email: EditText = findViewById(R.id.email)
        var password: EditText = findViewById(R.id.password)
        var password_confirm: EditText = findViewById(R.id.password_confirm)
        var registerButton: Button = findViewById(R.id.register)

        registerButton.setOnClickListener {
            val usernameText = username.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val passwordConfirmText = password_confirm.text.toString().trim()

            if (usernameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || passwordConfirmText.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText != passwordConfirmText) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = User(usernameText, emailText, passwordText, "active")

            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("MUSICAI/users").push()
            ref.setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "이제 회원가입을 완료했어요!", Toast.LENGTH_SHORT).show()
                    setMainPage()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "오류: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}