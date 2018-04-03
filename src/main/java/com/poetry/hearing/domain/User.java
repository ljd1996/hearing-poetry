package com.poetry.hearing.domain;

import java.io.Serializable;

public class User implements Serializable {
    private Integer id;

    private String name;

    private String passwd;

    private String email;

    private String autograph;

    public User() {

    }

    public User(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.passwd = user.getPasswd();
        this.email = user.getEmail();
        this.autograph = user.getAutograph();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd == null ? null : passwd.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getAutograph() {
        return autograph;
    }

    public void setAutograph(String autograph) {
        this.autograph = autograph == null ? null : autograph.trim();
    }
}