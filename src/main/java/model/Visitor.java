package model;

import java.util.List;

public class Visitor {
    private String name;
    private String surname;
    private String phone;
    private boolean subscribed;
    private List<Book> favoriteBooks;

    public Visitor(String name, String surname, String phone, List<Book> favoriteBooks) {
        this.name = name;
        this.surname = surname;
        this.phone = phone;
        this.subscribed = true;
        this.favoriteBooks = favoriteBooks;
    }

    public Visitor() {}

    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getPhone() { return phone; }
    public boolean isSubscribed() { return subscribed; }
    public List<Book> getFavoriteBooks() { return favoriteBooks; }

    @Override
    public String toString() {
        return name + " " + surname;
    }
}
