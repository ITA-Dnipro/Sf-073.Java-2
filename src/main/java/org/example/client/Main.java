package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.ORManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.lang.System.*;

public class Main {

    static final String SEPARATOR = "---------------------------------------";

    public static void main(String[] args) throws Exception {


        ORManager orManager = ORManager.withDataSource(new HikariDataSource());
        orManager.register(Publisher.class);
        orManager.register(Book.class);

        Book book1 = new Book("Just Book 1", LocalDate.now());
        Book book2 = new Book("Just Book 2", LocalDate.of(1961, 1, 1));

        Publisher publisher1 = new Publisher("Just Publisher 1");
        Publisher publisher2 = new Publisher("Just Publisher 2");

        orManager.persist(book1);
        orManager.persist(book2);
        orManager.persist(publisher1);
        orManager.persist(publisher2);

        out.println("Find All Books");
        orManager.findAll(Book.class).forEach(out::println);
        out.println(SEPARATOR);
        out.println("Find All Publishers");
        orManager.findAll(Publisher.class).forEach(out::println);
        out.println(SEPARATOR);
        out.println("Find Book and Publisher by ID = 1");
        Optional<Book> book = orManager.findById(1, Book.class);
        book.ifPresent(out::println);
        out.println(SEPARATOR);

        out.println(orManager.findById(10, Book.class));
        out.println(orManager.findById(1, Publisher.class));

        out.println("Save Method Test Examples");
        Publisher publisher101 = new Publisher("Publisher@OneToMany101");
        Book book101 = new Book("Book101@OneToMany101", LocalDate.now());
        Book book202 = new Book("Book202@OneToMany101", LocalDate.now());
        Book book303 = new Book("Book303@OneToMany101", LocalDate.now());
        publisher101.setBooks(List.of(book101, book202, book303));
        Publisher publisherRecord1 = orManager.save(publisher101);
        out.println(publisherRecord1);
        out.println(publisherRecord1.getBooks());
        out.println(SEPARATOR);
        Publisher publisher202 = new Publisher("Publisher@OneToMany202");
        Book book404 = new Book("Book404@OneToMany202", LocalDate.now(), publisher202);
        Book book505 = new Book("Book505@OneToMany202", LocalDate.now(), publisher202);
        Book book606 = new Book("Book606@OneToMany202", LocalDate.now(), publisher202);
        publisher202.setBooks(List.of(book404, book505, book606));
        Publisher publisherRecord2 = orManager.save(publisher202);
        out.println(publisherRecord2);
        out.println(publisherRecord2.getBooks());
        out.println(SEPARATOR);
        Publisher publisher303 = orManager.save(new Publisher("Publisher@OneToMany303"));
        Book book7 = new Book("Book707@OneToMany303", LocalDate.now(), publisher303);
        Book book8 = new Book("Book808@OneToMany303", LocalDate.now(), publisher303);
        Book book9 = new Book("Book909@OneToMany303", LocalDate.now(), publisher303);
        publisher303.setBooks(List.of(book7, book8, book9));
        Publisher publisher404 = new Publisher(publisher303.getName().replace('3', '4'));
        publisher404.setBooks(publisher303.getBooks());
        Publisher publisher505 = orManager.save(publisher404);
        out.println(publisher505);
        out.println(publisher505.getBooks());
        out.println(SEPARATOR);
        out.println("Find All With Saved Books");
        orManager.findAll(Book.class).forEach(out::println);
        out.println("Find All With Saved Publishers");
        orManager.findAll(Publisher.class).forEach(out::println);
        out.println(SEPARATOR);
    }
}
