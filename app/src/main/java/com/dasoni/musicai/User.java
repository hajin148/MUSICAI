package com.dasoni.musicai;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class User implements Serializable {
    public String nickname, email, password, accountStatus, dateSignUp;

    private String accessTime, exitTime;

    public Map<String, Integer> rhythmsLevel;

    public User(){

    }

    public User(String nickname, String email, String password, String accountStatus) {
        this.nickname = nickname;
        this.email = email;
        this.password = password; // hash
        this.accountStatus = accountStatus;
        this.dateSignUp = getCurrentDate();
        this.accessTime = "";
        this.exitTime = "";

        this.rhythmsLevel.put("4/4", 0);
        this.rhythmsLevel.put("3/4", 0);
        this.rhythmsLevel.put("2/4", 0);
        this.rhythmsLevel.put("etcs", 0);
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public String getEmail() {
        return email;
    }
    public String getNickName() {
        return nickname;
    }
    public String getAccountStatus() { return accountStatus;}

    public void setEmail(String email) {
        this.email = email;
    }
    public void setAdmin(boolean admin) {
        this.accountStatus = "Admin";
    }

    public void updateRhythmLevel(String rhythmType, int newScore) {
        int updatedScore = (rhythmsLevel.getOrDefault(rhythmType, 0) + newScore);
        if (updatedScore > 100) {
            updatedScore = updatedScore / 2;
        }
        rhythmsLevel.put(rhythmType, updatedScore);
    }

}
