package app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import model.Visitor;
import model.Book;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                System.err.println("Файл database.properties не найден");
                return;
            }
            props.load(input);
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            return;
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Подключение успешно");

            initDatabase(conn);

            task1(conn);

            task2(conn);

            task3(conn);

            task4(conn);

            task5(conn);

            task6(conn);

            task7(conn);

            task8(conn);
        } catch (SQLException e) {
            System.err.println("Ошибка БД: " + e.getMessage());
        }
    }

    private static void initDatabase(Connection conn) throws SQLException {
        try {
            InputStream sqlStream = Main.class.getClassLoader().getResourceAsStream("music-create.sql");
            String script = new String(sqlStream.readAllBytes(), StandardCharsets.UTF_8);

            try (Statement stmt = conn.createStatement()) {
                for (String sql : script.split(";")) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать файл music-create.sql.", e);
        }
    }

    private static void task1(Connection conn) throws SQLException {
        System.out.println("Задание 1: Список музыкальных композиций");
        String sql = "SELECT * FROM study.music";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ": " + rs.getString("name"));
            }
        }
    }

    private static void task2(Connection conn) throws SQLException {
        System.out.println("Задание 2: Фильтрация композиций");
        String sql = """
            SELECT * FROM study.music 
            WHERE name NOT ILIKE '%m%' 
            AND name NOT ILIKE '%t%'
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ": " + rs.getString("name"));
            }
        }
    }

    private static void task3(Connection conn) throws SQLException {
        System.out.println("Задание 3: Добавление композиции");
        String sql = "INSERT INTO study.music (id, name) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";
        String title = "My Favorite Song";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, 21);
            ps.setString(2, title);
            ps.executeUpdate();
            System.out.println("Добавлена композиция: " + title);
        }
    }

    private static void task4(Connection conn) throws SQLException {
        System.out.println("Задание 4: Загрузка данных из JSON");

        createVisitorAndBookTables(conn);

        List<Visitor> visitors;
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("books.json")) {
            if (in == null) {
                throw new RuntimeException("books.json не найден в ресурсах");
            }
            Gson gson = new Gson();
            Type visitorsType = new TypeToken<List<Visitor>>() {}.getType();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                visitors = gson.fromJson(reader, visitorsType);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать файл books.json", e);
        }

        System.out.println("Прочитано посетителей из файла: " + visitors.size());

        String visitorSql = """
        INSERT INTO study.visitors (name, surname, phone) 
        VALUES (?, ?, ?) 
        ON CONFLICT (name, surname) DO NOTHING
    """;

        String bookSql = """
        INSERT INTO study.books (title, author, year, isbn, publisher) 
        VALUES (?, ?, ?, ?, ?) 
        ON CONFLICT (isbn) DO NOTHING
    """;

        try (PreparedStatement visitorPs = conn.prepareStatement(visitorSql);
             PreparedStatement bookPs = conn.prepareStatement(bookSql)) {

            conn.setAutoCommit(false);

            int visitorsInserted = 0;
            int booksInserted = 0;

            for (Visitor visitor : visitors) {
                visitorPs.setString(1, visitor.getName().trim());
                visitorPs.setString(2, visitor.getSurname().trim());
                visitorPs.setString(3, visitor.getPhone().trim());
                visitorsInserted += visitorPs.executeUpdate();

                if (visitor.getFavoriteBooks() != null) {
                    for (Book book : visitor.getFavoriteBooks()) {
                        bookPs.setString(1, book.getName().trim());
                        bookPs.setString(2, book.getAuthor().trim());
                        bookPs.setInt(3, book.getPublishingYear());
                        bookPs.setString(4, book.getIsbn().trim());
                        bookPs.setString(5, book.getPublisher().trim());
                        booksInserted += bookPs.executeUpdate();
                    }
                }
            }

            conn.commit();
            conn.setAutoCommit(true);

            System.out.println("Загрузка завершена");
            System.out.println("Вставлено посетителей: " + visitorsInserted);
            System.out.println("Вставлено книг: " + booksInserted);

        } catch (SQLException e) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw e;
        }
    }

    private static void task5(Connection conn) throws SQLException {
        System.out.println("Задание 5: Книги по году издания");

        String sql = """
        SELECT title, author, year 
        FROM study.books 
        ORDER BY year ASC
    """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println(rs.getInt("year") + " - " +
                rs.getString("title") + " ( " + rs.getString("author") + " ) ");
            }
        }
    }

    private static void task6(Connection conn) throws SQLException {
        System.out.println("Задание 6: Книги до 2000 года");

        String sql = """
        SELECT title, author, year 
        FROM study.books 
        WHERE year < 2000 
        ORDER BY year ASC
    """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println(rs.getInt("year") + " - " +
                rs.getString("title") + " ( " + rs.getString("author") + " ) ");
            }
            System.out.println("Всего книг до 2000 года: " + count);
        }
    }

    private static void task7(Connection conn) throws SQLException {
        System.out.println("Задание 7: Добавление своих данных");

        List<Book> myBooks = List.of(
                new Book("Преступление и наказание", "Фёдор Достоевский", 1866,
                        "978-5-17-012346-3", "Эксмо"),
                new Book("Война и мир", "Лев Толстой", 1869,
                        "978-5-17-012347-0", "Эксмо")
        );

        Visitor me = new Visitor("Саша", "Пустозерова", "1234567899", myBooks);

        String visitorSql = """
                    INSERT INTO study.visitors (name, surname, phone) 
                    VALUES (?, ?, ?) 
                    ON CONFLICT (name, surname) DO NOTHING
                """;

        try (PreparedStatement ps = conn.prepareStatement(visitorSql)) {
            ps.setString(1, me.getName());
            ps.setString(2, me.getSurname());
            ps.setString(3, me.getPhone());
            ps.executeUpdate();
            System.out.println("Добавлена информация о себе");
        }

        String bookSql = """
                    INSERT INTO study.books (title, author, year, isbn, publisher) 
                    VALUES (?, ?, ?, ?, ?) 
                    ON CONFLICT (isbn) DO NOTHING
                """;

        try (PreparedStatement ps = conn.prepareStatement(bookSql)) {
            for (Book book : me.getFavoriteBooks()) {
                ps.setString(1, book.getName());
                ps.setString(2, book.getAuthor());
                ps.setInt(3, book.getPublishingYear());
                ps.setString(4, book.getIsbn());
                ps.setString(5, book.getPublisher());
                ps.executeUpdate();
            }
            System.out.println("Добавлены любимые книги");
        }

        System.out.println("Проверка:");

        String checkVisitorSql = """
                    SELECT id, name, surname, phone 
                    FROM study.visitors 
                    WHERE name = ? AND surname = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(checkVisitorSql)) {
            ps.setString(1, me.getName());
            ps.setString(2, me.getSurname());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Посетитель:");
                    System.out.println("  ID: " + rs.getInt("id"));
                    System.out.println("  Имя: " + rs.getString("name"));
                    System.out.println("  Фамилия: " + rs.getString("surname"));
                    System.out.println("  Телефон: " + rs.getString("phone"));
                }
            }
        }

        System.out.println("Книги:");

        String checkBooksSql = "SELECT * FROM study.books";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkBooksSql)) {
            while (rs.next()) {
                System.out.println(rs.getInt("year") + " - " +
                        rs.getString("title") + " ( " + rs.getString("author") + " ) ");
            }
        }
    }

    private static void task8(Connection conn) throws SQLException {
        System.out.println("Задание 8: Удаление таблиц");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS study.books");
            System.out.println("Таблица study.books удалена");

            stmt.execute("DROP TABLE IF EXISTS study.visitors");
            System.out.println("Таблица study.visitors удалена");
        }
    }

    private static void createVisitorAndBookTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS study.visitors (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                surname VARCHAR(255) NOT NULL,
                phone VARCHAR(50),
                UNIQUE(name, surname)
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS study.books (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                author VARCHAR(255),
                year INT,
                isbn VARCHAR(20) UNIQUE NOT NULL,
                publisher VARCHAR(255)
            )
        """);
        }
    }
}