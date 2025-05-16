package com.dasoni.musicai

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import android.content.Intent

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMainPage()

    }
    fun setMainPage() {
        setContentView(R.layout.activity_main)
        var button : Button = findViewById(R.id.button)
        var button2 : Button = findViewById(R.id.button2)

        button.setOnClickListener{
            setRegisterPage()
        }

        button2.setOnClickListener{
            val intent = Intent(this, SoundActivity::class.java)
            startActivity(intent)
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