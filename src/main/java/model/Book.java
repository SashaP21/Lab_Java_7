package model;

public class Book {
    private String name;
    private String author;
    private int publishingYear;
    private String isbn;
    private String publisher;

    public Book(String name, String author, int publishingYear, String isbn, String publisher) {
        this.name = name;
        this.author = author;
        this.publishingYear = publishingYear;
        this.isbn = isbn;
        this.publisher = publisher;
    }

    public Book() {}

    public String getName() { return name; }
    public String getAuthor() { return author; }
    public int getPublishingYear() { return publishingYear; }
    public String getIsbn() { return isbn; }
    public String getPublisher() { return publisher; }

    @Override
    public String toString() {
        return name + " — " + author + " (" + publishingYear + ")";
    }
}
