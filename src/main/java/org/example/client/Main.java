package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.ORManager;

import java.time.LocalDate;
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

        orManager.persist(book6);
        orManager.persist(book2);
        orManager.persist(book3);
        orManager.persist(book4);
        orManager.persist(publisher1);
        orManager.persist(publisher2);
        orManager.persist(publisher3);
        orManager.persist(publisher4);

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

        System.out.println("Save new Entity");
        Book newBook = new Book("New Book", LocalDate.now());
        Book newBook1 = new Book("New Book 1", LocalDate.of(2023, 1, 1), publisher1);
        Book newBook2 = new Book("New Book 2", LocalDate.now(), null);
        Publisher newPublisher = new Publisher("newPublisher");
        Optional<Publisher> persistedPublisher = orManager.findById(1, Publisher.class);
        System.out.println("Create book with persisted publisher");
        Book newBook3 = new Book("New Book 3", LocalDate.now(), persistedPublisher.get());
        System.out.println(newBook3);
        System.out.println("----------------------------");

        Book newBookRecord = orManager.save(newBook);
        System.out.println(newBookRecord); // using findById for return from DB
        Book newBook1Record = orManager.save(newBook1);
        System.out.println(newBook1Record);
        Book newBookRecord2 = orManager.save(newBook2);
        System.out.println(newBookRecord2);
        System.out.println("Save book with persisted publisher");
        Book newBookRecord3 = orManager.save(newBook3);
        System.out.println(newBookRecord3);
        System.out.println("----------------------------");

        Publisher newPublisherRecord = orManager.save(newPublisher);
        System.out.println(newPublisherRecord);
        System.out.println("Find All Books");
        orManager.findAll(Book.class).forEach(System.out::println);
        System.out.println("Find All Publishers");
        orManager.findAll(Publisher.class).forEach(System.out::println);
        System.out.println();
    }
}
