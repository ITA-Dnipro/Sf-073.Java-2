package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.entity.Book;
import org.example.entity.Publisher;
import org.example.lib.ORManager;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        ORManager orManager = ORManager.withDataSource(new HikariDataSource());

        orManager.register(Book.class, Publisher.class);

        Book book1 = new Book("Solaris", LocalDate.of(1961, 1, 1));
        Book book2 = new Book("Just Book", LocalDate.now());
        Book book3 = new Book("aaaaaaaa", LocalDate.of(1961, 1, 1));
        Book book4 = new Book("bbbbbbbb", LocalDate.of(1961, 1, 1));

        orManager.persist(book1);
        orManager.persist(book2);
        orManager.persist(book3);
        orManager.persist(book4);

        Publisher publisher1 = new Publisher("Just Publisher");
        Publisher publisher2 = new Publisher("MyPub1");
        Publisher publisher3 = new Publisher("MyPub2");
        Publisher publisher4 = new Publisher("MyPub3");

        orManager.persist(publisher1);
        orManager.persist(publisher2);
        orManager.persist(publisher3);
        orManager.persist(publisher4);

        orManager.findAll(Book.class).forEach(System.out::println);
        System.out.println();
        orManager.findAll(Publisher.class).forEach(System.out::println);
    }
}
