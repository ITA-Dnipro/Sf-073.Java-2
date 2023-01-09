package org.example.client;

import com.zaxxer.hikari.HikariDataSource;
import org.example.client.entity.Book;
import org.example.client.entity.Publisher;
import org.example.lib.ORManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.lang.System.out;

public class Main {

    static final String SEPARATOR = "---------------------------------------";

    public static void main(String[] args) throws Exception {

        ORManager orManager = ORManager.withDataSource(new HikariDataSource());

        orManager.register(Publisher.class);
        orManager.register(Book.class);

        Book book1 = new Book("To Kill a Mockingbird", LocalDate.now());
        Book book2 = new Book("The Great Gatsby ", LocalDate.of(1961, 1, 1));
        orManager.persist(book1);
        orManager.persist(book2);

        Publisher publisher1 = new Publisher("Just Publisher 1");
        Publisher publisher2 = new Publisher("Just Publisher 2");
        orManager.persist(publisher1);
        orManager.persist(publisher2);

        out.println("Save Method Test Examples");
        Publisher publisher101 = new Publisher("Publisher@OneToMany101");
        Book book101 = new Book("One Hundred Years of Solitude ", LocalDate.now());
        Book book202 = new Book("In Cold Blood ", LocalDate.now());
        Book book303 = new Book("Wide Sargasso Sea", LocalDate.now());
        publisher101.setBooks(List.of(book101, book202, book303));
        Publisher publisherRecord1 = orManager.save(publisher101);
        out.println(publisherRecord1);
        out.println(publisherRecord1.getBooks());
        out.println(SEPARATOR);
        Publisher publisher202 = new Publisher("Publisher@OneToMany202");
        Book book404 = new Book("Brave New World", LocalDate.now(), publisher202);
        Book book505 = new Book(" Capture The Castle", LocalDate.now(), publisher202);
        Book book606 = new Book("Jane Eyre ", LocalDate.now(), publisher202);
        publisher202.setBooks(List.of(book404, book505, book606));
        Publisher publisherRecord2 = orManager.save(publisher202);
        out.println("Print Publisher@OneToMany202:");
        out.println(publisherRecord2);
        out.println("Publisher@OneToMany202 getBooks(): ");
        out.println(publisherRecord2.getBooks());
        out.println(SEPARATOR);
        Publisher publisher303 = orManager.save(new Publisher("Publisher@OneToMany303"));
        Book book7 = new Book("The Call of the Wild ", LocalDate.now(), publisher303);
        Book book8 = new Book("The Chrysalids", LocalDate.now(), publisher303);
        Book book9 = new Book("Persuasion", LocalDate.now(), publisher303);
        publisher303.setBooks(List.of(book7, book8, book9));
        orManager.save(publisher303);
        out.println("Print Publisher@OneToMany303:");
        out.println(SEPARATOR);

        out.println("Find All Books");
        orManager.findAll(Book.class).forEach(out::println);
        out.println("Find All Publishers");
        orManager.findAll(Publisher.class).forEach(out::println);
        out.println(SEPARATOR);

        out.println("Find by id");
        out.println(orManager.findById(100, Book.class));
        out.println(orManager.findById(10, Book.class));
        out.println(orManager.findById(1, Publisher.class));
        out.println(SEPARATOR);

        out.println("Refresh");
        out.println("Book before changes: ");
        Book bookToChange = orManager.findById(8, Book.class).get();
        out.println(bookToChange);
        bookToChange.setTitle("new title");
        bookToChange.setPublishedAt(LocalDate.of(1990, 4, 4));
        out.println("Book after changes: ");
        out.println(bookToChange);
        Book refreshBook = orManager.refresh(bookToChange);
        out.println("Refreshed book: ");
        out.println(refreshBook);
        out.println(SEPARATOR);
        out.println("Publisher before changes: ");
        Publisher publisherToChange = orManager.findById(5, Publisher.class).get();
        out.println(publisherToChange);
        publisherToChange.setName("new name");
        out.println("Publisher after changes: ");
        out.println(publisherToChange);
        Publisher refreshedPublisher = orManager.refresh(publisherToChange);
        out.println("Refreshed Publisher: ");
        out.println(refreshedPublisher);
        out.println(SEPARATOR);

        out.println("Delete book");
        Book book = orManager.findById(1, Book.class).get();
        orManager.delete(book);
        orManager.findAll(Book.class).forEach(out::println);
        out.println(SEPARATOR);

        out.println("Delete publisher");
        Publisher publisher = orManager.findById(5, Publisher.class).get();
        orManager.delete(publisher);
        List<Publisher> all = orManager.findAll(Publisher.class);
        all.forEach(System.out::println);
        out.println("Updated findById: 11, Book.class");
        out.println(orManager.findById(11, Book.class));
        out.println(SEPARATOR);
        out.println("Merge method examples");
        book2.setTitle("Just Book 1");
        orManager.merge(book1);
        out.println("Set New Title To Book9 -> Book909@OneToMany303 ");
        book9.setTitle("Book909@OneToMany303UPDATED");
        orManager.merge(book9);
        out.println("Print Book9:");
        out.println(book9);



    }
}
