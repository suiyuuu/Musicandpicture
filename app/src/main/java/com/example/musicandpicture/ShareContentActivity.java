package com.example.musicandpicture;

public class ShareContentActivity {
    private String name;
    private int age;
    private String sex;
    private String number;

    public ShareContentActivity(String name, int age, String sex, String number) {
        this.name = name;
        this.age = age;
        this.sex = sex;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "ShareContentActivity{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", sex='" + sex + '\'' +
                ", number='" + number + '\'' +
                '}';
    }
}

