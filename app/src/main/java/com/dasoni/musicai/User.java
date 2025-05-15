package com.dasoni.musicai;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class User implements Serializable {
    public String username, email, password, accountStatus, dateSignUp;
    public String dateAccess, timeFirstAccess, timeExit;
    public Map<String, Integer> rhythmsLevel = new HashMap<>();
    public User() {
    }

    public User(String username, String email, String password, String accountStatus) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.accountStatus = accountStatus;
        this.dateSignUp = getCurrentDate();
        this.dateAccess = getCurrentDate();
        this.timeFirstAccess = getCurrentTime();
        this.timeExit = "";
        this.rhythmsLevel.put("four_four", 0);
        this.rhythmsLevel.put("three_four", 0);
        this.rhythmsLevel.put("two_four", 0);
        this.rhythmsLevel.put("etcs", 0);
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public void updateRhythmLevel(String rhythmType, int newScore) {
        int updatedScore = rhythmsLevel.getOrDefault(rhythmType, 0) + newScore;
        rhythmsLevel.put(rhythmType, Math.min(updatedScore, 100));
    }

}
