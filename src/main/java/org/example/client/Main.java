package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.entity.Book;
import org.example.entity.Publisher;
import org.example.lib.ORManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Exception {

        ORManager orManager = ORManager.withDataSource(new HikariDataSource());

//        orManager.register(Book.class, Publisher.class);
        orManager.register(Publisher.class);
        orManager.register(Book.class);

        Book book1 = new Book("Solaris", LocalDate.of(1961, 1, 1));
        Book book2 = new Book("Just Book", LocalDate.now());
        Book book3 = new Book("Just Book 2", LocalDate.of(1961, 1, 1));
        Book book4 = new Book("Just Book 3", LocalDate.of(1961, 1, 1));
        Book book5 = new Book("Just Book 4", LocalDate.of(1961, 1, 1));
        Book book6 = new Book("new book", LocalDate.of(1961, 1, 1));

        Publisher publisher1 = new Publisher("Just Publisher");
        Publisher publisher2 = new Publisher("MyPub1");
        Publisher publisher3 = new Publisher("MyPub2");
        Publisher publisher4 = new Publisher("MyPub3");

//        orManager.persist(book6);
//        orManager.persist(book2);
//        orManager.persist(book3);
//        orManager.persist(book4);
//        orManager.persist(publisher1);
//        orManager.persist(publisher2);
//        orManager.persist(publisher3);
//        orManager.persist(publisher4);

        System.out.println("Find All Books");
        orManager.findAll(Book.class).forEach(System.out::println);
        System.out.println();

        System.out.println("Find All Publishers");
        orManager.findAll(Publisher.class).forEach(System.out::println);
        System.out.println();

        System.out.println("Find Book and Publisher by ID");

        Optional<Book> book = orManager.findById(1, Book.class);
        book.ifPresent(System.out::println);

        System.out.println(orManager.findById(10, Book.class));
        System.out.println(orManager.findById(1, Publisher.class));
    }
}
