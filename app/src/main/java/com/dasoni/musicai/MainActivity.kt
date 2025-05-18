package com.dasoni.musicai

import android.content.Intent
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.w3c.dom.Text
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    var username = ""
    var loggedinUser : User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMainPage()
    }
    fun setMainPage() {
        setContentView(R.layout.activity_main)

        var login_btn : Button = findViewById(R.id.Login_btn)
        login_btn.setOnClickListener {
            setLoginPage()
        }

        var soundTest_btn : Button = findViewById(R.id.soundTest_btn)
        soundTest_btn.setOnClickListener {
            val intent = Intent(this, SoundActivity::class.java)
            startActivity(intent)
        }

        var username_display : TextView = findViewById(R.id.username_display)

        if (username != "") {
            username_display.text = username + "님, 안녕하세요!" + loggedinUser.toString()
        }
    }

    fun setPasswordResetPage() {
        setContentView(R.layout.password_reset)

        val auth = FirebaseAuth.getInstance()
        val back: ImageButton = findViewById(R.id.backButton)
        back.setOnClickListener {
            setMainPage()
        }

        val sendEmail: Button = findViewById(R.id.Link)
        val linkTime: Button = findViewById(R.id.link_time)
        val userEmail: EditText = findViewById(R.id.user_email)

        sendEmail.setOnClickListener {
            val email = userEmail.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "유효한 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "비밀번호 재설정 이메일이 전송되었습니다.", Toast.LENGTH_LONG).show()
                        linkTime.visibility = View.VISIBLE
                        sendEmail.visibility = View.VISIBLE

                    } else {
                        Toast.makeText(this, "이메일 전송 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
        linkTime.setOnClickListener {
            setContentView(R.layout.result_message_screen)
            val btn_back : ImageView = findViewById(R.id.btn_back)
            btn_back.setOnClickListener{
                setLoginPage()
            }
        }
    }



    fun setLoginPage() {
        setContentView(R.layout.login)

        val email: EditText = findViewById(R.id.input_email)
        val password: EditText = findViewById(R.id.password)
        val loginBtn: Button = findViewById(R.id.button_sign_in)
        val forgetpassword: TextView = findViewById(R.id.link_forgot_pw)
        val register: TextView = findViewById(R.id.link_signup)

        // ----------------------------------------- Underline --------------------------------------------------------
        val fp_text = getString(R.string.forgotpassword)
        val spannable_ul2 = SpannableString(fp_text)

        spannable_ul2.setSpan(
            UnderlineSpan(),
            0,
            fp_text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        forgetpassword.text = spannable_ul2
        // ----------------------------------------- Underline --------------------------------------------------------
        // ----------------------------------------- Underline --------------------------------------------------------

        val text = getString(R.string.signup)
        val spannable_ul = SpannableString(text)

        spannable_ul.setSpan(
            UnderlineSpan(),
            21,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        register.text = spannable_ul
        // ----------------------------------------- Underline --------------------------------------------------------
        // ------------------------------------------------------------ Color Change -------------------------------------------------------
        val mainText: TextView = findViewById(R.id.main_text1)


        val fullText = getString(R.string.signin_title)
        var spannable = SpannableString(fullText)


        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val blackColor = ContextCompat.getColor(this, android.R.color.black)

        val target = "에\n오신것을"
        val startIndex = fullText.indexOf(target)
        val endIndex = startIndex + target.length

        spannable.setSpan(
            ForegroundColorSpan(primaryColor),
            0,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (startIndex >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(blackColor),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        mainText.text = spannable
        // ------------------------------------------------------------ Color Change -------------------------------------------------------

        register.setOnClickListener {
            setRegisterPage()
        }

        forgetpassword.setOnClickListener {
            setPasswordResetPage()
        }

        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid

                        val ref = FirebaseDatabase.getInstance()
                            .getReference("MUSICAI/users")
                            .child(uid!!)

                        ref.get().addOnSuccessListener { snapshot ->
                            val user = snapshot.getValue(User::class.java)
                            if (user != null) {
                                username = user.username
                                loggedinUser = user
                                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                setMainPage()
                            } else {
                                Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "유저 정보 로딩 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }



    fun setRegisterPage() {
        setContentView(R.layout.registration)

        val username: EditText = findViewById(R.id.username)
        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)
        val passwordConfirm: EditText = findViewById(R.id.password_confirm)
        val registerButton: Button = findViewById(R.id.register)
        val btn_back: ImageView = findViewById(R.id.btn_back)

        val password_unmatch: TextView = findViewById(R.id.static_error_repw)
        val password_short: TextView = findViewById(R.id.static_error_pw)
        val username_exists: TextView = findViewById(R.id.static_error_name)
        val email_exists: TextView = findViewById(R.id.static_error_email)

        val errorEmail: TextView = findViewById(R.id.static_error_email)

        //-----------------------------Underline--------------------------
        val text = getString(R.string.error_email)
        val spannable = SpannableString(text)

        spannable.setSpan(
            UnderlineSpan(),
            29,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        errorEmail.text = spannable

        btn_back.setOnClickListener {
            setLoginPage()
        }
        //-----------------------------Underline--------------------------

        registerButton.setOnClickListener {
            password_unmatch.visibility = View.GONE
            password_short.visibility = View.GONE
            username_exists.visibility = View.GONE
            email_exists.visibility = View.GONE

            val usernameText = username.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val passwordConfirmText = passwordConfirm.text.toString().trim()

            if (usernameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || passwordConfirmText.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var hasError = false

            if (passwordText.length < 8) {
                password_short.visibility = View.VISIBLE
                hasError = true
            }

            if (passwordText != passwordConfirmText) {
                password_unmatch.visibility = View.VISIBLE
                hasError = true
            }

            if (hasError) return@setOnClickListener

            val usersRef = FirebaseDatabase.getInstance().getReference("MUSICAI/users")
            usersRef.get().addOnSuccessListener { snapshot ->
                var usernameTaken = false
                var emailTaken = false

                for (userSnapshot in snapshot.children) {
                    val userEmail = userSnapshot.child("email").value?.toString()
                    val userName = userSnapshot.child("username").value?.toString()

                    if (userName == usernameText) {
                        usernameTaken = true
                    }
                    if (userEmail == emailText) {
                        emailTaken = true
                    }
                }

                if (usernameTaken) {
                    username_exists.visibility = View.VISIBLE
                }

                if (emailTaken) {
                    email_exists.visibility = View.VISIBLE
                }

                if (usernameTaken || emailTaken) return@addOnSuccessListener

                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
                            val hashedPassword = hashPassword(passwordText)
                            val user = User(usernameText, emailText, hashedPassword, "active")

                            usersRef.child(userId)
                                .setValue(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                                    setMainPage()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "데이터 저장 오류: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

            }.addOnFailureListener {
                Toast.makeText(this, "유저 데이터 확인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun logout() {
        username = ""
        loggedinUser = null
        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
        setMainPage()
    }

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

}